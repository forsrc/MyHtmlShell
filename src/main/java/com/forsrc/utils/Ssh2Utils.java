package com.forsrc.utils;

import static net.sf.expectit.filter.Filters.*;
import static net.sf.expectit.matcher.Matchers.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;

public class Ssh2Utils {

    public static void main(String[] args) throws Exception {
        String hostname = args[0];
        String login = args[1];
        String password = args[2];

        Ssh2Utils ssh = new Ssh2Utils(login, hostname, password);
        ssh.handle(new ChannelSftpHandler() {

            @Override
            public void handle(ChannelSftp sftp) throws Exception {
                sftp.cd("./");
                Vector<ChannelSftp.LsEntry> files = sftp.ls("*");
                System.out.printf("--> Found %d files in dir %s%n", files.size(), "./");

                for (ChannelSftp.LsEntry file : files) {
                    if (file.getAttrs().isDir()) {
                        System.out.printf("--> Reading dir : %s%n", file);
                        continue;
                    }
                    System.out.printf("--> Reading file : %s%n", file);
                    BufferedReader bis = new BufferedReader(new InputStreamReader(sftp.get(file.getFilename())));
                    String line = null;
                    while ((line = bis.readLine()) != null) {
                        System.out.println(line);
                    }
                    bis.close();
                }
            }
        });
        System.out.println("--------");
        ssh.handle(new ChannelShellHandler() {
            @Override
            public void handle(ChannelShell shell) throws Exception {
                OutputStream out = shell.getOutputStream();
                ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
                ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                shell.setOutputStream(stdOut);
                shell.setExtOutputStream(stdErr);
                out.write("ls\n".getBytes());
                out.flush();
                TimeUnit.SECONDS.sleep(2);
                System.out.println(new String(stdOut.toByteArray()));
            }
        });
        System.out.println("--------");
        ssh.handle(new ChannelExecHandler() {
            @Override
            public void handle(ChannelExec exec) throws Exception {
                exec.setOutputStream(System.out);
                exec.setExtOutputStream(System.out);
                exec.setInputStream(null);
                exec.setCommand("ls");
                exec.connect();
                TimeUnit.SECONDS.sleep(1);
            }
        });
        final CompletableFuture<String> future = new CompletableFuture<>();
        System.out.println("--------");
        new Thread() {
            public void run() {
                try {
                    ssh.handle(new ExpectHandler() {
                        @Override
                        public void handle(Expect expect) throws Exception {
                            expect.sendLine("echo hellowrld");
                            expect.expect(contains("hellowrld"));
                            expect.sendLine();
                            String str = expect.expect(regexp("(.*):(.*)")).group(1);
                            System.err.println("--> user: " + str);
                            expect.expect(regexp("(.*)$"));
                            expect.sendLine("exit");
                            future.complete("exit");
                            expect.expect(eof());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        }.start();

        new Thread() {
            public void run() {
                try {
                    ssh.handle(new ExpectHandler() {
                        @Override
                        public void handle(Expect expect) throws Exception {
                            expect.sendLine("echo hellowrld");
                            expect.expect(contains("hellowrld"));
                            String flg = future.get();
                            if ("exit".equals(flg)) {
                                expect.sendLine("echo OK");
                                expect.sendLine("exit");
                                expect.expect(eof());
                                expect.close();
                                return;
                            }
                            expect.sendLine();
                            String str = expect.expect(regexp("(.*):(.*)")).group(1);
                            System.err.println("--> user: " + str);
                            expect.expect(regexp("(.*)$"));
                            expect.sendLine("exit");
                            expect.expect(eof());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        }.start();

        System.err.println("--> " + future.get());

    }

    private String login;
    private String hostname;
    private String password;

    public Ssh2Utils(String login, String hostname, String password) {
        super();
        this.login = login;
        this.hostname = hostname;
        this.password = password;
    }

    public void handle(final SessionHandler handler) throws Exception {
        Session session = getSession();
        try {
            session.connect();
            handler.handle(session);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public void handle(final String type, final ChannelHandler handler) throws Exception {
        handle(type, handler, true);
    }

    public void handle(final String type, final ChannelHandler handler, boolean autoConnect) throws Exception {
        handle(new SessionHandler() {
            @Override
            public void handle(Session session) throws Exception {
                Channel channel = session.openChannel(type);
                try {
                    if (autoConnect) {
                        channel.connect();
                    }
                    handler.handle(channel);
                } finally {
                    if (channel != null && !channel.isClosed()) {
                        channel.disconnect();
                    }
                }
            }
        });

    }

    public void handle(final ChannelSftpHandler handler) throws Exception {
        handle("sftp", new ChannelHandler() {

            @Override
            public void handle(Channel channel) throws Exception {
                ChannelSftp sftp = (ChannelSftp) channel;
                handler.handle(sftp);
            }

        });
    }

    public void handle(final ChannelShellHandler handler) throws Exception {
        handle("shell", new ChannelHandler() {

            @Override
            public void handle(Channel channel) throws Exception {
                ChannelShell shell = (ChannelShell) channel;
                handler.handle(shell);
            }

        });
    }

    public void handle(final ChannelExecHandler handler) throws Exception {
        handle("exec", new ChannelHandler() {

            @Override
            public void handle(Channel channel) throws Exception {
                ChannelExec shell = (ChannelExec) channel;
                handler.handle(shell);
            }

        }, false);
    }

    public void handle(final ExpectHandler handler) throws Exception {
        handle("shell", new ChannelHandler() {

            @Override
            public void handle(Channel channel) throws Exception {
                Expect expect = new ExpectBuilder()
                        .withOutput(channel.getOutputStream())
                        .withInputs(channel.getInputStream(), channel.getExtInputStream())
                        .withEchoOutput(System.out)
                        .withEchoInput(System.err)
                        .withInputFilters(removeColors(), removeNonPrintable())
                        .withExceptionOnFailure()
                        .build();
                try {
                    handler.handle(expect);
                } finally {
                    expect.close();
                }
            }

        });
    }

    public Session getSession() throws JSchException {
        JSch ssh = new JSch();
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        Session session = ssh.getSession(login, hostname, 22);
        session.setConfig(config);
        session.setPassword(password);
        return session;
    }

    private static String getLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println("--> " + line);
            sb.append(line).append("¥n");
        }

        return sb.toString();
    }

    public static interface SessionHandler {
        public void handle(Session session) throws Exception;
    }

    public static interface ChannelHandler {
        public void handle(Channel channel) throws Exception;
    }

    public static interface ChannelSftpHandler {
        public void handle(ChannelSftp sftp) throws Exception;
    }

    public static interface ChannelShellHandler {
        public void handle(ChannelShell shell) throws Exception;
    }

    public static interface ChannelExecHandler {
        public void handle(ChannelExec exec) throws Exception;
    }

    public static interface ExpectHandler {
        public void handle(Expect expect) throws Exception;
    }
}
