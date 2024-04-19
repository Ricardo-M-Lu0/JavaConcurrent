package org.example.classicquestion;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 死锁示例
 * <p>
 * 产生死锁的四个必要条件是：互斥条件、持有并等待条件、不可剥夺条件、环路等待条件
 * <p>
 * 最常见的并且可行的就是使用资源有序分配法，来破环环路等待条件。即线程 A 和 线程 B 总是以相同的顺序申请自己想要的资源
 *
 * @author liugang
 * @version 1.0
 * @date 2024/4/16 14:33
 * @since 1.0
 */
public class Deadlock {
    static ReentrantLock aLock = new ReentrantLock();
    static ReentrantLock bLock = new ReentrantLock();

    public static void main(String[] args) {
        new Thread(Deadlock::aThread, "threadA").start();
        new Thread(Deadlock::bThread, "threadB").start();
    }

    private static void aThread() {
        System.out.println("thread A waiting get ResourceA");
        aLock.lock();
        System.out.println("thread A got ResourceA");
        sleep(1);
        System.out.println("thread A waiting get ResourceB");
        bLock.lock();
        System.out.println("thread A got ResourceB");
        bLock.unlock();
        aLock.unlock();
    }

    private static void bThread() {
        System.out.println("thread B waiting get ResourceB");
        bLock.lock();
        System.out.println("thread B got ResourceB");
        sleep(1);
        System.out.println("thread B waiting get ResourceA");
        aLock.lock();
        System.out.println("thread B got ResourceA");
        aLock.unlock();
        bLock.unlock();
    }

    private static void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
