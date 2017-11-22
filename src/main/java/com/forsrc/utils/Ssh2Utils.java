package com.forsrc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Ssh2Utils {

    public static void main(String[] args) throws Exception {
        String hostname = "localhost";
        String login = "";
        String password = "";

        Ssh2Utils ssh = new Ssh2Utils(login, hostname, password);
        ssh.handle(new ChannelSftpHandler() {

            @Override
            public void handle(ChannelSftp sftp) throws Exception {
                sftp.cd("/db");
                Vector<ChannelSftp.LsEntry> files = sftp.ls("*");
                System.out.printf("--> Found %d files in dir %s%n", files.size(), "/db");

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
            if (session != null) {
                session.disconnect();
            }
        }
    }

    public void handle(final String type, final ChannelHandler handler) throws Exception {
        handle(new SessionHandler() {
            @Override
            public void handle(Session session) throws Exception {
                Channel channel = session.openChannel(type);
                try {
                    channel.connect();
                    handler.handle(channel);
                } finally {
                    if (channel != null) {
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

    public Session getSession() throws JSchException {
        JSch ssh = new JSch();
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        Session session = ssh.getSession(login, hostname, 22);
        session.setConfig(config);
        session.setPassword(password);
        return session;
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
}
