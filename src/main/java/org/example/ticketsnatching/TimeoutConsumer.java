package org.example.ticketsnatching;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.example.ticketsnatching.Config.ORDER_TIMEOUT_DEAD_LETTER_QUEUE;

public class TimeoutConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(TimeoutConsumer.class);

    @Override
    public void run() {
        try {
            Channel channel = MQManager.getChannel();
            log.info("超时订单消费者启动，等待消息...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String orderId = new String(delivery.getBody(), StandardCharsets.UTF_8);
                log.info("[超时消费者] 收到超时检查消息，订单ID: {}", orderId);

                DatabaseManager.Order order = DatabaseManager.getOrder(orderId);
                if (order != null && "AwaitingPayment".equals(order.status)) {
                    // 订单仍是待支付状态，执行取消操作
                    DatabaseManager.updateOrderStatusInDB(orderId, "TimeoutCancelled");
                    DatabaseManager.increaseStockInDB(order.productId);
                    log.warn("[超时消费者] 订单 {} 已超时，自动取消并归还库存。", orderId);
                    String notification = String.format("您的订单 %s 因超时未支付已被自动取消。", orderId);
                    WebSocketManager.sendMessageToUser(order.userId, notification);
                } else {
                    // 订单已支付或已手动取消，无需处理
                    log.info("[超时消费者] 订单 {} 状态已改变，无需处理。当前状态: {}", orderId, order != null ? order.status : "不存在");
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            channel.basicConsume(ORDER_TIMEOUT_DEAD_LETTER_QUEUE, false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            log.error("超时订单消费者异常", e);
        }
    }
}