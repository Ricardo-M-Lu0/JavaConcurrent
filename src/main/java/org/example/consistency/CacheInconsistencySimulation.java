package org.example.consistency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 缓存不一致模拟</br>
 * 方案一：先删除缓存再更新数据库</br>
 * 方案二：先更新数据库再删除缓存</br>
 * 推荐使用方案二（两种方案都会出现缓存不一致问题，但是概率不同）</br>
 * 方案一的问题，发生的条件是“一个读请求插在了写请求的删除缓存和更新数据库之间”，这种情况是很常见的。</br>
 * 方案二的问题，发生的条件是“一个读请求（A）必须跨越一个完整的写请求（B）”，即A读DB的旧值，
 * 然后B完成了更新DB+删除缓存两步操作，然后A才把旧值写回缓存。这要求写操作（更新DB+删缓存）的速度，要比读操作（读DB+写缓存）的速度还要快
 * </br>
 * 在实际系统中，读操作的速度通常远远快于写操作。因此，方案二出现问题的概率极低，低到在大多数工程实践中可以容忍。
 *
 *
 * <br><b>注意：</b></br>
 * 更新策略相对于删除策略的缺点
 * 1.更新策略不够懒，每次都会更新缓存数据，而删除策略是在读请求需要的时候才读取数据放入缓存（删除操作比更新操作要轻量级）
 * 2.更新策略的数据一致性较差，如果线程A和线程B都更新同一个key，线程A更新数据库为99，线程B更新数据为98，然后线程B更新缓存为98，接着线程A更新缓存为99，那么该数据不会再被更新正确了
 * 虽然删除策略也会出现不一致的情况但是概率要比
 * </br>
 * </br>
 *
 * 更新策略的风险：</br>
 * 它的操作目的是“将我认为正确的值写入缓存”。但这个“我认为”是基于它自己那个时间点从数据库读到的数据。</br>
 * 在并发环境下，当你去写缓存时，你手里的数据很可能已经不是最新鲜的了。</br>
 * 你正在用一个“可能过时”的数据去覆盖缓存，这本质上是在传播一个潜在的错误状态。我们之前模拟的脏数据场景，根源就在于此。</br>
 * </br>
 *
 * 删除策略的安全性：</br>
 * 它的操作目的不是写入具体的值，而是“通知缓存，你现在的数据已经不可信了”。</br>
 * 它不产生任何新的数据，只负责让旧数据失效。这个操作本身不携带“状态”，因此不会引入“传播错误状态”的风险。</br>
 * 它相信“源头”（数据库）永远是正确的，并把“重建缓存”这个光荣而艰巨的任务，交给了下一次真正需要数据的读请求。</br>
 * </br>
 * 小结：删除策略从设计上就规避了“用旧数据污染缓存”的风险，因为它从不主动写数据，只做“作废”声明。
 */
public class CacheInconsistencySimulation {
    // 使用线程安全的ConcurrentHashMap模拟缓存和数据库
    private static final Map<String, String> cache = new ConcurrentHashMap<>();
    private static final Map<String, String> database = new ConcurrentHashMap<>();
    private static final String KEY = "product:123";

    public static void main(String[] args) throws InterruptedException {
        // 初始化数据
        database.put(KEY, "10");
        System.out.println("---------- 初始状态 ----------");
        System.out.println("数据库值: " + database.get(KEY));
        System.out.println("缓存值: " + cache.get(KEY));
        System.out.println("--------------------------------\n");
        simulateDeleteFirst();

        // 重置环境，准备场景二
        System.out.println("\n---------- 重置环境 ----------");
        database.put(KEY, "10");
        cache.clear();
        System.out.println("数据库值: " + database.get(KEY));
        System.out.println("缓存值: " + cache.get(KEY));
        System.out.println("--------------------------------\n");
        simulateUpdateFirst();
    }

    /**
     * 先删除缓存再更新数据库
     * 可能出现数据不一致的情况:
     * 假设现在有一个写请求A（想把值从10改成20）和一个读请求B，并发执行
     * 1.写请求A先删除缓存
     * 2.A更新数据库之前线程切换到读请求B
     * 3.B发现缓存被删除了，此时B会从数据库中获取数据（旧值10）
     * 4.B将旧值更新到缓存中（10）
     * 5.线程切换到写请求A，A完成对数据库的更新，数据库中数据被更新为20但是缓存中的数据为10，数据不一致
     */
    private static void simulateDeleteFirst() throws InterruptedException {
        System.out.println(">>> 开始模拟场景一：先删除缓存，再更新数据库 <<<");

        // 使用CountDownLatch来精确控制线程执行顺序
        CountDownLatch latch = new CountDownLatch(1);

        Thread write = new Thread(() -> {
            try {
                System.out.println("[写线程A] 启动，准备更新值为 20");

                // 1. 先删除缓存
                cache.remove(KEY);
                System.out.println("[写线程A] 第1步: 删除缓存成功");

                // 2. 释放锁，让读线程B可以开始执行
                latch.countDown();
                System.out.println("[写线程A] 释放latch，让读线程B执行...");

                // 3. 模拟耗时的数据库操作
                System.out.println("[写线程A] 模拟写数据库的耗时操作...");
                Thread.sleep(500);

                // 4. 更新数据库
                database.put(KEY, "20");
                System.out.println("[写线程A] 第2步: 更新数据库成功，值为 20");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread read = new Thread(() -> {
            try {
                // 等待写线程A删除缓存后开始
                latch.await();
                System.out.println("[读线程B] 被唤醒，开始读取数据");

                // 1. 读缓存，发现为空
                String cachedValue = cache.get(KEY);
                System.out.println("[读线程B] 第1步: 读缓存，值: " + cachedValue);

                if (cachedValue == null) {
                    // 2. 缓存未命中，从数据库读取
                    String dbValue = database.get(KEY);
                    System.out.println("[读线程B] 第2步: 缓存未命中，读数据库，值: " + dbValue + " (这是旧值！)");

                    // 3. 将从数据库读到的旧值写回缓存
                    cache.put(KEY, dbValue);
                    System.out.println("[读线程B] 第3步: 将旧值写回缓存");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        read.start();
        write.start();
        // 等待两个线程都执行完毕
        write.join();
        read.join();

        System.out.println("\n---------- 场景一最终结果 ----------");
        System.out.println("数据库值: " + database.get(KEY) + " (期望值: 20)");
        System.out.println("缓存值:   " + cache.get(KEY) + " (期望值: 20)");
        System.out.println(">>> 结论：发生数据不一致！缓存中是脏数据。<<<");
    }

    /**
     * 先更新数据库再删除缓存(推荐使用这种方案)
     * 1.读线程A查询缓存发现为null，从数据库中查询出值为10
     * 2.切换到写线程B更新数据库值为20，并将删除缓存中的值
     * 3.读线程A将查询出的旧值10写入缓存中
     * 4.数据库中值为20，缓存中未10，数据不一致
     */
    private static void simulateUpdateFirst() throws InterruptedException {
        System.out.println(">>> 开始模拟场景二：先更新数据库，再删除缓存 <<<");

        CountDownLatch readDbLatch = new CountDownLatch(1);
        CountDownLatch writeCacheLatch = new CountDownLatch(1);

        // 读线程A：一个比较慢的读操作
        Thread readerThread = new Thread(() -> {
            try {
                System.out.println("[读线程A] 启动，开始读取数据");
                // 1. 读数据库，读到旧值
                String dbValue = database.get(KEY);
                System.out.println("[读线程A] 第1步: 读数据库，值: " + dbValue);

                // 2. 释放锁，让写线程B可以开始执行
                readDbLatch.countDown();

                // 3. 等待写线程B执行完“更新DB”和“删除缓存”两步操作
                System.out.println("[读线程A] 等待写线程B完成...");
                writeCacheLatch.await();

                // 4. 将之前读到的旧值写入缓存（模拟网络延迟等原因导致此操作变慢）
                System.out.println("[读线程A] 被唤醒，将旧值 " + dbValue + " 写入缓存");
                cache.put(KEY, dbValue);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 写线程B：一个比较快的写操作
        Thread writerThread = new Thread(() -> {
            try {
                // 等待读线程A读完数据库
                readDbLatch.await();
                System.out.println("[写线程B] 被唤醒，准备更新值为 20");

                // 1. 更新数据库
                database.put(KEY, "20");
                System.out.println("[写线程B] 第1步: 更新数据库成功，值为 20");

                // 2. 删除缓存
                cache.remove(KEY);
                System.out.println("[写线程B] 第2步: 删除缓存成功");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 3. 释放锁，让读线程A可以将旧值写入缓存
                writeCacheLatch.countDown();
            }
        });

        readerThread.start();
        writerThread.start();

        readerThread.join();
        writerThread.join();

        System.out.println("\n---------- 场景二最终结果 ----------");
        System.out.println("数据库值: " + database.get(KEY) + " (期望值: 20)");
        System.out.println("缓存值:   " + cache.get(KEY) + " (期望值: 20)");
        System.out.println(">>> 结论：在极端罕见情况下，也发生了数据不一致！<<<");
    }
}
