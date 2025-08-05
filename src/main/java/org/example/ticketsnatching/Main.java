package org.example.ticketsnatching;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Main {
    private static final String PRODUCT_ID = "notebook";
    private static final int INITIAL_STOCK = 3;
    private static final int CONCURRENT_USERS = 4;

    public static void main(String[] args) throws Exception {
        // 1. 初始化所有组件
        init();

        // 2. 启动所有后台消费者线程
        startConsumer();

        // 3. 模拟秒杀场景
        ticketSnatchingSimulation();

        // 等待一段时间，让订单创建和超时逻辑有时间执行
        log.info("等待10秒，让订单消费者处理消息...");
        Thread.sleep(10000);

        // 4. 打印秒杀后的数据库状态
        DatabaseManager.printStatus();

        // 5. 模拟后续操作：支付、手动取消、超时
        log.info("--- 开始模拟后续操作 ---");
        // 假设我们知道生成的订单ID (实际应用中需要通过查询获得)
        // 场景A: 成功支付 (选择第一个待支付订单)
        DatabaseManager.getOrderDB().values().stream()
                .filter(o -> "AwaitingPayment".equals(o.status)).findFirst()
                .ifPresent(order -> {
                    log.info("模拟场景A：用户支付订单 {}", order.orderId);
                    PaymentService.payForOrder(order.userId, order.orderId);
                });
        Thread.sleep(2000);

        // 场景B: 手动取消 (选择第二个待支付订单)
        DatabaseManager.getOrderDB().values().stream()
                .filter(o -> "AwaitingPayment".equals(o.status)).findFirst()
                .ifPresent(order -> {
                    log.info("模拟场景B：用户手动取消订单 {}", order.orderId);
                    CancellationService.cancelOrder(order.orderId);
                });
        Thread.sleep(2000);

        // 场景C: 等待剩余订单超时
        log.info("模拟场景C：等待剩余订单超时 (等待 {} 秒)...", Config.ORDER_TIMEOUT_MS / 1000);
        Thread.sleep(Config.ORDER_TIMEOUT_MS + 5000); // 等待足够长的时间确保超时消息被处理

        // 6. 打印最终的数据库状态
        log.info("--- 所有操作执行完毕，打印最终状态 ---");
        DatabaseManager.printStatus();

        // 7. 关闭资源
        closeResources();
    }

    private static void init() throws IOException, TimeoutException {
        log.info("[INIT]--- 系统初始化开始 ---");
        RedisManager.init();
        MQManager.init();
        WebSocketManagerHandle.start(Config.WEBSOCKET_PORT);
        DatabaseManager.initProduct(PRODUCT_ID, INITIAL_STOCK);
        RedisManager.setStock(PRODUCT_ID, INITIAL_STOCK);
        log.info("[INIT]--- 系统初始化完成 ---");
    }

    private static void startConsumer() throws InterruptedException {
        log.info("[INIT]--- 启动后台消费者 ---");
        new Thread(new OrderConsumer()).start();
        new Thread(new PaymentConsumer()).start();
        new Thread(new TimeoutConsumer()).start();
        new Thread(new CancellationConsumer()).start();
        Thread.sleep(2000); // 等待消费者启动
    }

    private static void ticketSnatchingSimulation() throws InterruptedException {
        log.info("--- 开始模拟秒杀，{}个用户并发请求，库存{} ---", CONCURRENT_USERS, INITIAL_STOCK);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final String userId = "user-" + i;
            WebSocketManager.registerWebSocket(userId);
            log.info("提交用户 {} 的秒杀请求", userId);
            executor.submit(() -> {
                try {
                    SeckillService.placeOrder(userId, PRODUCT_ID);
                    log.info("用户 {} 的秒杀请求处理完成", userId);
                } catch (Exception e) {
                    log.error("用户 {} 的秒杀请求处理异常", userId, e);
                }
            });
        }
        log.info("所有秒杀请求已提交到线程池");
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        log.info("--- 秒杀请求发送完毕 ---");
    }

    private static void closeResources() throws IOException, TimeoutException, InterruptedException {
        MQManager.close();
        // 关闭Redis连接池
        RedisManager.close();
        WebSocketManagerHandle.stop();
        log.info("--- 系统关闭 ---");
    }
}