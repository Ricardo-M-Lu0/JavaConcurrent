package org.example.ticketsnatching;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class SeckillService {
    public static void placeOrder(String userId, String productId) {
        try {
            log.info("[用户 {}] 开始处理商品 {} 的秒杀请求", userId, productId);
            // 1. 布隆过滤器快速失败：检查商品是否已明确售罄
            boolean mightContain = RedisManager.mightContainInBloomFilter(productId);
            if (mightContain) {
                log.warn("[用户 {}] 请求商品 {} 失败：布隆过滤器判定已售罄 (快速失败)", userId, productId);
                return;
            }

            // 2. Redis原子预扣减库存
            boolean success = RedisManager.preDeductStock(productId);
            log.info("[用户 {}] 预扣减商品 {} 结果: {}", userId, productId, success);
            if (success) {
                // 3. 预扣减成功，发送消息到MQ进行后续处理
                log.info("[用户 {}] 预扣减商品 {} 库存成功，发送消息到MQ...", userId, productId);
                try {
                    String message = userId + "," + productId;
                    MQManager.sendMessage(Config.ORDER_EXCHANGE, Config.ORDER_ROUTING_KEY, message);
                } catch (IOException e) {
                    log.error("发送订单消息失败", e);
                    // 注意：这里需要补偿机制，将Redis库存加回去
                    RedisManager.increaseStockInDB(productId);
                }
            } else {
                // 4. 预扣减失败，说明库存已空，将商品ID加入布隆过滤器
                log.info("[用户 {}] 请求商品 {} 失败：Redis库存不足", userId, productId);
                RedisManager.addProductToBloomFilter(productId);
            }
        } catch (Exception e) {
            log.error("[用户 {}] 处理商品 {} 的秒杀请求时发生异常", userId, productId, e);
        }
    }
}