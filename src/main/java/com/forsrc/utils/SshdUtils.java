package com.forsrc.utils;

import java.io.IOException;
import java.util.Arrays;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class SshdUtils {

    public static void main(String[] args) throws IOException {
        String hostname = args[0];
        String username = args[1];
        String password = args[2];

        SshdUtils utils = new SshdUtils(hostname, username, password);
        utils.handle(new ClientSessionHandler() {

            @Override
            public void handle(ClientSession session) throws IOException {
                try (ChannelExec exec = session.createExecChannel("whoami")) {
                    exec.setOut(System.out);
                    exec.open();
                    exec.waitFor(Arrays.asList(ClientChannelEvent.CLOSED), 0);
                }
            }
        });
    }

    private String username;
    private String hostname;
    private String password;

    public SshdUtils(String hostname, String username, String password) {
        super();
        this.username = username;
        this.hostname = hostname;
        this.password = password;
    }

    public void handle(ClientSessionHandler handler) throws IOException {
        Logger minaLogger = (Logger) LoggerFactory.getLogger("org.apache.sshd");
        if (minaLogger != null) {
            minaLogger.setLevel(Level.INFO);
        }
        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        ClientSession session = null;
        try {
            ConnectFuture connectFuture = client.connect(username, hostname, 22);
            connectFuture.await(5 * 1000);
            session = connectFuture.getSession();
            session.addPasswordIdentity(password);
            session.auth().await(5 * 1000);

            if (!session.isAuthenticated()) {
                throw new IllegalArgumentException(String.format("Auth failed: %s@%s", username, hostname));
            }
            handler.handle(session);
        } finally {
            if (session != null && !session.isClosed()) {
                session.close();
            }
            if (client != null && !client.isClosed()) {
                client.close();
            }
            if (client != null) {
                client.stop();
            }
        }

    }

    public void handle(ChannelExecHandler handler) throws IOException {

    }

    public static interface ClientSessionHandler {
        public void handle(ClientSession session) throws IOException;
    }

    public static interface ChannelExecHandler {
        public void handle(ChannelExec exec) throws IOException;
    }
}
