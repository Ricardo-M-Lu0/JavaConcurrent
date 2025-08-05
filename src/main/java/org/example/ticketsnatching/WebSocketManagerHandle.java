package org.example.ticketsnatching;


public class WebSocketManagerHandle {
    public static WebSocketManager webSocketManager;

    public static void start(int port) {
        webSocketManager = new WebSocketManager(port);
        webSocketManager.start();
    }

    public static void stop() throws InterruptedException {
        webSocketManager.stop();
    }
}