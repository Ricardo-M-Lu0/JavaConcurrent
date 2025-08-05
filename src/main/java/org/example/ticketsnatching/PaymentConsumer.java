package org.example.ticketsnatching;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.example.ticketsnatching.Config.PAYMENT_QUEUE;

public class PaymentConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);

    @Override
    public void run() {
        try {
            Channel channel = MQManager.getChannel();
            log.info("支付消费者启动，等待消息...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String orderId = new String(delivery.getBody(), StandardCharsets.UTF_8);
                log.info("[支付消费者] 收到支付消息，订单ID: {}", orderId);
                DatabaseManager.updateOrderStatusInDB(orderId, "Paid");
                log.info("[支付消费者] 订单 {} 状态已更新为 'Paid'", orderId);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String notification = String.format("您的订单 %s 已支付成功。", orderId);
                WebSocketManager.sendMessageToUser(order.userId, notification);
            };
            channel.basicConsume(PAYMENT_QUEUE, false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            log.error("支付消费者异常", e);
        }
    }
}