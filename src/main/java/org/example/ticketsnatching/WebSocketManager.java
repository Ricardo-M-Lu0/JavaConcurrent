package org.example.ticketsnatching;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketManager extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketManager.class);
    private static final Map<String, WebSocket> userSocketMap = new ConcurrentHashMap<>();
    private static final List<WebSocketClient> clients = new ArrayList<>();

    public WebSocketManager(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // 在实际应用中，连接打开时会进行身份验证，然后将userId和conn关联起来
        // 这里我们简化处理，在客户端连接时，由客户端发送自己的userId
        log.info("[WebSocket] 新的WebSocket连接: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String userId = getUserIdBySocket(conn);
        if (userId != null) {
            userSocketMap.remove(userId);
            log.info("[WebSocket] 用户 {} 的WebSocket连接已关闭", userId);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // 假设客户端发送的第一条消息是它的userId，用于注册
        // 消息格式: "register:userId"
        if (message.startsWith("register:")) {
            String userId = message.split(":")[1];
            userSocketMap.put(userId, conn);
            log.info("[WebSocket] 用户 {} 注册到WebSocket, 地址: {}", userId, conn.getRemoteSocketAddress());
            conn.send("注册成功！");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("[WebSocket] 发生错误", ex);
    }

    @Override
    public void onStart() {
        log.info("[WebSocket] WebSocket服务器已在端口 {} 启动", getPort());
    }

    public static void sendMessageToUser(String userId, String message) {
        WebSocket conn = userSocketMap.get(userId);
        if (conn != null && conn.isOpen()) {
            log.info("[WebSocket] 向用户 {} 推送消息: {}", userId, message);
            conn.send(message);
        } else {
            log.warn("[WebSocket] 无法向用户 {} 推送消息，连接不存在或已关闭", userId);
        }
    }

    private String getUserIdBySocket(WebSocket conn) {
        return userSocketMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(conn))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    // 创建WebSocket客户端
    public static WebSocketClient createWebSocketClient(String userId) throws Exception {
        URI uri = new URI("ws://localhost:" + Config.WEBSOCKET_PORT);
        return new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                log.info("[WebSocket] 客户端-{} 已连接到WebSocket服务器", userId);
                // 连接成功后，发送自己的userId进行注册
                this.send("register:" + userId);
            }

            @Override
            public void onMessage(String message) {
                log.info("[WebSocket] 客户端-{} 收到实时通知: {}", userId, message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.info("[WebSocket] 客户端-{} 连接已关闭", userId);
            }

            @Override
            public void onError(Exception ex) {
                log.error("[WebSocket] 客户端-{} 发生错误", userId, ex);
            }
        };
    }


    public static void registerWebSocket(String userId) {
        // 为每个用户创建模拟的WebSocket客户端并连接
        log.info("[WebSocket] --- 模拟客户端连接WebSocket服务器 ---");
        WebSocketClient client;
        try {
            client = createWebSocketClient(userId);
            client.connectBlocking(); // 等待连接成功
            clients.add(client);
            Thread.sleep(200); // 等待客户端注册完成
        } catch (Exception e) {
            log.error("[WebSocket] 创建WebSocket客户端失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws InterruptedException {
        clients.forEach(WebSocketClient::close);
        super.stop();
    }
}