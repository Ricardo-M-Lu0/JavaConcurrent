package org.example.ticketsnatching;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP.BasicProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
public class MQManager {
    private static Connection connection;
    private static Channel channel;

    public static void init() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Config.MQ_HOST);
        factory.setPort(Config.MQ_PORT);
        factory.setUsername("guest");
        factory.setPassword("guest");
        connection = factory.newConnection();
        channel = connection.createChannel();

        // --- 声明处理订单的交换机和队列 ---
        channel.exchangeDeclare(Config.ORDER_EXCHANGE, "direct", true);
        channel.queueDeclare(Config.ORDER_QUEUE, true, false, false, null);
        channel.queueBind(Config.ORDER_QUEUE, Config.ORDER_EXCHANGE, Config.ORDER_ROUTING_KEY);

        // --- 声明处理支付的队列 ---
        channel.queueDeclare(Config.PAYMENT_QUEUE, true, false, false, null);
        channel.queueBind(Config.PAYMENT_QUEUE, Config.ORDER_EXCHANGE, Config.PAYMENT_ROUTING_KEY);

        // --- 声明处理手动取消的队列 ---
        channel.queueDeclare(Config.MANUAL_CANCEL_QUEUE, true, false, false, null);
        channel.queueBind(Config.MANUAL_CANCEL_QUEUE, Config.ORDER_EXCHANGE, Config.MANUAL_CANCEL_ROUTING_KEY);

        // --- 声明实现订单超时的死信队列机制 ---
        // 1. 正常接收延迟消息的交换机和队列
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", Config.ORDER_TIMEOUT_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", Config.ORDER_TIMEOUT_DEAD_LETTER_ROUTING_KEY);
        channel.exchangeDeclare(Config.ORDER_TIMEOUT_DELAY_EXCHANGE, "direct", true);
        channel.queueDeclare(Config.ORDER_TIMEOUT_DELAY_QUEUE, true, false, false, args);
        channel.queueBind(Config.ORDER_TIMEOUT_DELAY_QUEUE, Config.ORDER_TIMEOUT_DELAY_EXCHANGE, Config.ORDER_TIMEOUT_DELAY_ROUTING_KEY);

        // 2. 实际消费超时消息的死信交换机和队列
        channel.exchangeDeclare(Config.ORDER_TIMEOUT_DEAD_LETTER_EXCHANGE, "direct", true);
        channel.queueDeclare(Config.ORDER_TIMEOUT_DEAD_LETTER_QUEUE, true, false, false, null);
        channel.queueBind(Config.ORDER_TIMEOUT_DEAD_LETTER_QUEUE, Config.ORDER_TIMEOUT_DEAD_LETTER_EXCHANGE, Config.ORDER_TIMEOUT_DEAD_LETTER_ROUTING_KEY);
        
        log.info("RabbitMQ 初始化成功");
    }

    public static void sendMessage(String exchange, String routingKey, String message) throws IOException {
        channel.basicPublish(exchange, routingKey, null, message.getBytes());
    }

    public static void sendDelayedTimeoutMessage(String message) throws IOException {
        BasicProperties properties = new BasicProperties.Builder()
                .expiration(String.valueOf(Config.ORDER_TIMEOUT_MS))
                .build();
        channel.basicPublish(Config.ORDER_TIMEOUT_DELAY_EXCHANGE, Config.ORDER_TIMEOUT_DELAY_ROUTING_KEY, properties, message.getBytes());
    }

    public static Channel getChannel() {
        return channel;
    }

    public static void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}