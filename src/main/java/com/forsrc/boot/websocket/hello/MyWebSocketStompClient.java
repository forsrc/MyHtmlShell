package com.forsrc.boot.websocket.hello;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MyWebSocketStompClient {

    static class DefaultStompFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            System.out.println("--> getPayloadType(): " + stompHeaders);
            System.out.println("--> getPayloadType(): " + stompHeaders.getAck());
            return Greeting.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            System.out.println("--> handleFrame(): " + stompHeaders);
            System.out.println("--> " + o);
        }
    }

    static BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>();

    public static void main(String[] args) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(
                new SockJsClient(Arrays.asList(new WebSocketTransport(new StandardWebSocketClient()))));
        StompSession session = stompClient
                .connect("ws://127.0.0.1:8088/gs-guide-websocket", new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);

        System.out.println(session.isConnected());
        session.subscribe("/topic/greetings", new DefaultStompFrameHandler());

        HelloMessage message = new HelloMessage("test");
        ObjectMapper objectMapper = new ObjectMapper();

        session.send("/app/hello", objectMapper.writeValueAsBytes(message));

        TimeUnit.SECONDS.sleep(3);
        session.disconnect();

    }
}
