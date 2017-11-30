//package com.forsrc.utils;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.util.Arrays;
//import java.util.concurrent.TimeUnit;
//
//import org.apache.sshd.client.SshClient;
//import org.apache.sshd.client.channel.ChannelExec;
//import org.apache.sshd.client.channel.ChannelShell;
//import org.apache.sshd.client.channel.ClientChannel;
//import org.apache.sshd.client.channel.ClientChannelEvent;
//import org.apache.sshd.client.future.ConnectFuture;
//import org.apache.sshd.client.session.ClientSession;
//import org.apache.sshd.common.util.GenericUtils;
//import org.apache.sshd.common.util.io.NoCloseOutputStream;
//import org.slf4j.LoggerFactory;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//
//public class SshdUtils {
//
//    public static void main(String[] args) throws IOException {
//        String hostname = args[0];
//        String username = args[1];
//        String password = args[2];
//
//        SshdUtils utils = new SshdUtils(hostname, username, password);
//        System.out.println("-----");
//        utils.handle(new ClientSessionHandler() {
//
//            @Override
//            public void handle(ClientSession session) throws IOException {
//                ChannelExec exec = session.createExecChannel("whoami");
//                exec.setOut(new NoCloseOutputStream(System.out));
//                exec.open().await();
//                exec.waitFor(Arrays.asList(ClientChannelEvent.CLOSED), 0);
//                exec.close();
//            }
//        });
//        System.out.println("-----");
//        utils.handle(new ClientSessionHandler() {
//
//            @Override
//            public void handle(ClientSession session) throws IOException {
//                ChannelShell channel = session.createShellChannel();
//                channel.setOut(new NoCloseOutputStream(System.out));
//
//                channel.open().await();
//                channel.getInvertedIn().write("whoami¥n".getBytes());
//                channel.getInvertedIn().flush();
//                //channel.waitFor(Arrays.asList(ClientChannelEvent.CLOSED), 0);
//                try {
//                    TimeUnit.SECONDS.sleep(2);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                channel.close();
//            }
//        });
//        System.out.println("-----");
//        utils.handle(new ClientSessionHandler() {
//
//            @Override
//            public void handle(ClientSession session) throws IOException {
//                String response = session.executeRemoteCommand("pwd");
//                String[] lines = GenericUtils.split(response, '\n');
//                for (String l : lines) {
//                    System.out.println(l);
//                }
//                response = session.executeRemoteCommand("whoami");
//                lines = GenericUtils.split(response, '\n');
//                for (String l : lines) {
//                    System.out.println(l);
//                }
//            }
//        });
//    }
//
//    private String username;
//    private String hostname;
//    private String password;
//
//    public SshdUtils(String hostname, String username, String password) {
//        super();
//        this.username = username;
//        this.hostname = hostname;
//        this.password = password;
//    }
//
//    public void handle(ClientSessionHandler handler) throws IOException {
//        Logger minaLogger = (Logger) LoggerFactory.getLogger("org.apache.sshd");
//        if (minaLogger != null) {
//            minaLogger.setLevel(Level.INFO);
//        }
//        SshClient client = SshClient.setUpDefaultClient();
//        client.start();
//        ClientSession session = null;
//        try {
//            ConnectFuture connectFuture = client.connect(username, hostname, 22);
//            connectFuture.await(5 * 1000);
//            session = connectFuture.getSession();
//            session.addPasswordIdentity(password);
//            session.auth().await(5 * 1000);
//
//            if (!session.isAuthenticated()) {
//                throw new IllegalArgumentException(String.format("Auth failed: %s@%s", username, hostname));
//            }
//            handler.handle(session);
//        } finally {
//            if (session != null && !session.isClosed()) {
//                session.close(false).await();;
//            }
//            if (client != null) {
//                client.stop();
//            }
//        }
//
//    }
//
//    public void handle(ChannelExecHandler handler) throws IOException {
//
//    }
//
//    public static interface ClientSessionHandler {
//        public void handle(ClientSession session) throws IOException;
//    }
//
//    public static interface ChannelExecHandler {
//        public void handle(ChannelExec exec) throws IOException;
//    }
//}
