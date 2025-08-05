package org.example.ticketsnatching;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.nio.charset.Charset;
import java.time.Duration;

@Slf4j
public class RedisManager {
    private static JedisPool jedisPool;
    private static BloomFilter<String> bloomFilter;

    public static void init() {
        // 初始化Jedis连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);

        jedisPool = new JedisPool(poolConfig, Config.REDIS_HOST, Config.REDIS_PORT, 2000, Config.REDIS_PASSWORD);

        // 测试连接
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(Config.REDIS_DATABASE);
            log.info("Redis 连接成功: {}", jedis.ping());
        }

        // 初始化布隆过滤器 (预计1000个商品，期望误判率0.01)
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 1000, 0.01);
    }

    // 设置商品初始库存
    public static void setStock(String productId, int stock) {
        String key = Config.PRODUCT_STOCK_KEY_PREFIX + productId;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(Config.REDIS_DATABASE);
            String result = jedis.set(key, String.valueOf(stock));
            log.info("设置商品 {} 库存为 {}，结果: {}", productId, stock, result);
        }
    }

    // Lua脚本实现原子预扣减库存
    public static boolean preDeductStock(String productId) {
        String script = "local stock = redis.call('get', KEYS[1]) " +
                        "if tonumber(stock) > 0 then " +
                        "    redis.call('decr', KEYS[1]) " +
                        "    return 1 " +
                        "else " +
                        "    return 0 " +
                        "end";
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(Config.REDIS_DATABASE);
            Object result = jedis.eval(script, 1, Config.PRODUCT_STOCK_KEY_PREFIX + productId);
            return Long.valueOf(1).equals(result);
        }
    }

    // --- 布隆过滤器相关 ---
    public static void addProductToBloomFilter(String productId) {
        bloomFilter.put(productId);
    }

    public static boolean mightContainInBloomFilter(String productId) {
        return bloomFilter.mightContain(productId);
    }

    public static void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    public static void increaseStockInDB(String productId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(Config.REDIS_DATABASE);
            jedis.incr(Config.PRODUCT_STOCK_KEY_PREFIX + productId);
        }
    }
}