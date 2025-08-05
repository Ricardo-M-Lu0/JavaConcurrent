package org.example.ticketsnatching;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.example.ticketsnatching.Config.MANUAL_CANCEL_QUEUE;

/**
 * 手动取消消费者
 */
public class CancellationConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(CancellationConsumer.class);

    @Override
    public void run() {
        try {
            Channel channel = MQManager.getChannel();
            log.info("手动取消消费者启动，等待消息...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String orderId = new String(delivery.getBody(), StandardCharsets.UTF_8);
                log.info("[取消消费者] 收到手动取消请求，订单ID: {}", orderId);

                DatabaseManager.Order order = DatabaseManager.getOrder(orderId);
                if (order != null && "AwaitingPayment".equals(order.status)) {
                    DatabaseManager.updateOrderStatusInDB(orderId, "Cancelled");
                    DatabaseManager.increaseStockInDB(order.productId);
                    log.info("[取消消费者] 订单 {} 已成功取消并归还库存", orderId);
                } else {
                    log.warn("[取消消费者] 订单 {} 无法取消，当前状态: {}", orderId, order != null ? order.status : "不存在");
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            channel.basicConsume(MANUAL_CANCEL_QUEUE, false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            log.error("手动取消消费者异常", e);
        }
    }
}