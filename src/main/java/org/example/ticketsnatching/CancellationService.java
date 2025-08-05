package org.example.ticketsnatching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CancellationService {
    private static final Logger log = LoggerFactory.getLogger(CancellationService.class);

    public static void cancelOrder(String orderId) {
        log.info("[取消服务] 用户正在手动取消订单 {}", orderId);
        try {
            MQManager.sendMessage(Config.ORDER_EXCHANGE, Config.MANUAL_CANCEL_ROUTING_KEY, orderId);
        } catch (IOException e) {
            log.error("发送手动取消消息失败", e);
        }
    }
}