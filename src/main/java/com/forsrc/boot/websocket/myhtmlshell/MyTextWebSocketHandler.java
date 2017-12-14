package com.forsrc.boot.websocket.myhtmlshell;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MyTextWebSocketHandler extends TextWebSocketHandler {

    private List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private boolean isLogOpen = false;

    private Tailer tailer;

    public MyTextWebSocketHandler() {

    }

    public void tailer() {
        TailerListener listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                super.handle(line);
                System.out.println(line);
                try {
                    String msg = String.format("\r\n%s\r\n$", line);
                    Iterator<WebSocketSession> it = sessions.iterator();
                    while (it.hasNext()) {
                        WebSocketSession webSocketSession = it.next();
                        webSocketSession.sendMessage(new TextMessage(msg));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        tailer = new Tailer(new File("/tmp/test.txt"), listener, 500, true);

        CompletableFuture.runAsync(new Runnable() {

            @Override
            public void run() {
                tailer.run();
            }
        });

    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws InterruptedException, IOException, ExecutionException {
        System.out.println("--> " + message);

        String cmd = message.getPayload().replaceAll("&nbsp;", " ").substring(1);
        String msg = String.format("\r\n%s\r\n$", cmd);
        Iterator<WebSocketSession> it = sessions.iterator();
        while (it.hasNext()) {
            WebSocketSession webSocketSession = it.next();
            if (!webSocketSession.isOpen()) {
                sessions.remove(webSocketSession);
                continue;
            }
            if ("/log start".equals(cmd) && !isLogOpen) {
                webSocketSession.sendMessage(new TextMessage("\r\nstart log\r\n$"));
                tailer();
                isLogOpen = true;
            } else if ("/log stop".equals(cmd) && isLogOpen) {
                webSocketSession.sendMessage(new TextMessage("\r\nstop log\r\n$"));
                tailer.stop();
                isLogOpen = false;
            } else {
                webSocketSession.sendMessage(new TextMessage(msg));
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // the messages will be broadcasted to all users.
        sessions.add(session);
        session.sendMessage(new TextMessage("$"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }
}