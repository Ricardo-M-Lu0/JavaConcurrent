package org.example.ticketsnatching;

public class Config {
    // --- Redis 配置 ---
    public static final String REDIS_HOST = "10.44.87.83";
    public static final int REDIS_PORT = 6379;
    public static final int REDIS_DATABASE = 11;
    public static final String REDIS_PASSWORD = "Codesafe";
    public static final String PRODUCT_STOCK_KEY_PREFIX = "seckill:stock:";
    public static final String PRODUCT_SOLD_OUT_BLOOM_FILTER = "seckill:bloom_filter";

    // --- RabbitMQ 配置 ---
    public static final String MQ_HOST = "localhost";
    public static final int MQ_PORT = 5672;
    // 订单处理相关
    public static final String ORDER_EXCHANGE = "order_exchange";
    public static final String ORDER_QUEUE = "order_queue";
    public static final String ORDER_ROUTING_KEY = "order.create";
    // 支付处理相关
    public static final String PAYMENT_QUEUE = "payment_queue";
    public static final String PAYMENT_ROUTING_KEY = "order.payment";
    // 手动取消相关
    public static final String MANUAL_CANCEL_QUEUE = "manual_cancel_queue";
    public static final String MANUAL_CANCEL_ROUTING_KEY = "order.cancel.manual";
    // 订单超时相关 (死信队列实现)
    public static final String ORDER_TIMEOUT_DELAY_EXCHANGE = "order_timeout_delay_exchange";
    public static final String ORDER_TIMEOUT_DELAY_QUEUE = "order_timeout_delay_queue";
    public static final String ORDER_TIMEOUT_DELAY_ROUTING_KEY = "order.timeout.delay";
    public static final String ORDER_TIMEOUT_DEAD_LETTER_EXCHANGE = "order_timeout_dead_letter_exchange";
    public static final String ORDER_TIMEOUT_DEAD_LETTER_QUEUE = "order_timeout_dead_letter_queue";
    public static final String ORDER_TIMEOUT_DEAD_LETTER_ROUTING_KEY = "order.timeout.dead";
    public static final int ORDER_TIMEOUT_MS = 15000; // 订单超时时间：15秒

    // --- WebSocket 配置 ---
    public static final int WEBSOCKET_PORT = 8887;
}