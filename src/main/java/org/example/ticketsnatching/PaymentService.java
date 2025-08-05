package org.example.ticketsnatching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static void payForOrder(String userId, String orderId) {
        log.info("[支付服务] 用户正在支付订单 {}", orderId);
        try {
            MQManager.sendMessage(Config.ORDER_EXCHANGE, Config.PAYMENT_ROUTING_KEY, userId + "," + orderId);
        } catch (IOException e) {
            log.error("[支付服务] 发送支付消息失败", e);
        }
    }
}