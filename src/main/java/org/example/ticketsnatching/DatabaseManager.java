package org.example.ticketsnatching;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DatabaseManager {
    // 模拟产品表
    private static final Map<String, AtomicInteger> productStockDB = new ConcurrentHashMap<>();
    // 模拟订单表
    private static final Map<String, Order> orderDB = new ConcurrentHashMap<>();

    public static class Order {
        public String orderId;
        public String productId;
        public String userId;
        public String status; // AwaitingPayment, Paid, Cancelled, TimeoutCancelled

        public Order(String orderId, String productId, String userId) {
            this.orderId = orderId;
            this.productId = productId;
            this.userId = userId;
            this.status = "AwaitingPayment";
        }
        @Override
        public String toString() {
            return String.format("Order{orderId='%s', productId='%s', userId='%s', status='%s'}", orderId, productId, userId, status);
        }
    }

    public static void initProduct(String productId, int stock) {
        productStockDB.put(productId, new AtomicInteger(stock));
    }

    // 在数据库层面创建订单并扣减库存
    public static synchronized boolean createOrderInDB(String orderId, String productId, String userId) {
        AtomicInteger stock = productStockDB.get(productId);
        if (stock != null && stock.get() > 0) {
            stock.decrementAndGet();
            orderDB.put(orderId, new Order(orderId, productId, userId));
            return true;
        }
        return false;
    }
    
    public static synchronized void updateOrderStatusInDB(String orderId, String newStatus) {
        Order order = orderDB.get(orderId);
        if (order != null) {
            order.status = newStatus;
        }
    }
    
    public static synchronized Order getOrder(String orderId) {
        return orderDB.get(orderId);
    }

    public static synchronized void increaseStockInDB(String productId) {
        productStockDB.get(productId).incrementAndGet();
    }

    public static void printAllOrders() {
        log.info("--- 当前所有订单状态 ---");
        orderDB.values().forEach(e -> log.info(e.toString()));
        log.info("------------------------");
    }

    public static void printAllStock() {
        log.info("--- 当前数据库库存 ---");
        productStockDB.forEach((k, v) -> log.info("商品ID: {}, 库存: {}", k, v));
        log.info("------------------------");
    }

    public static void printStatus() {
        DatabaseManager.printAllOrders();
        DatabaseManager.printAllStock();
    }

    public static Map<String, Order> getOrderDB() {
        return orderDB;
    }
}