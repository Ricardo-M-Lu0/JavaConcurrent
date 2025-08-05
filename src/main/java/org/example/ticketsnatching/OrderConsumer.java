package org.example.ticketsnatching;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.example.ticketsnatching.Config.ORDER_QUEUE;

@Slf4j
public class OrderConsumer implements Runnable {

    @Override
    public void run() {
        try {
            Channel channel = MQManager.getChannel();
            log.info("订单消费者启动，等待消息...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                // 消息格式: "userId,productId"
                String[] parts = message.split(",");
                String userId = parts[0];
                String productId = parts[1];
                String orderId = UUID.randomUUID().toString().substring(0, 8);

                log.info("[订单消费者] 收到创建订单请求: userId={}, productId={}", userId, productId);

                // 核心逻辑: 在数据库创建订单
                boolean success = DatabaseManager.createOrderInDB(orderId, productId, userId);

                if (success) {
                    log.info("[订单消费者] 订单 {} 创建成功 (待支付)，发送超时取消延迟消息...", orderId);
                    // 发送延迟消息，用于15秒后检查订单是否支付
                    MQManager.sendDelayedTimeoutMessage(orderId);
                    // 通过WebSocket通知用户下单成功
                    String notification = String.format("下单成功！您的订单号是: %s，请在15秒内支付。", orderId);
                    WebSocketManager.sendMessageToUser(userId, notification);
                } else {
                    log.warn("[订单消费者] 数据库库存不足，订单创建失败。productId={}", productId);
                    // 注意：这里可能需要一个补偿机制，将Redis中预扣减的库存加回去。为简化原型，暂不实现。
                    // 通过WebSocket通知用户下单失败
                    WebSocketManager.sendMessageToUser(userId, "非常抱歉，下单失败，商品已被抢光。");
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            channel.basicConsume(ORDER_QUEUE, false, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            log.error("订单消费者异常", e);
        }
    }
}