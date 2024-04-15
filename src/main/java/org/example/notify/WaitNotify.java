package org.example.notify;

/**
 * <a href="https://javadoop.com/">示例</a>
 */
public class WaitNotify {
    volatile int a = 0;

    public static void main(String[] args) {
        notifyAndInterrupted();
    }

    public static void waitAndNotify() {
        Object object = new Object();

        new Thread(() -> {
            synchronized (object) {
                System.out.println("线程1 获取到监视器锁");
                try {
                    object.wait();
                    System.out.println("线程1 恢复啦。我为什么这么久才恢复，因为notify方法虽然早就发生了，可是我还要获取锁才能继续执行。");
                } catch (InterruptedException e) {
                    System.out.println("线程1 wait方法抛出了InterruptedException异常");
                }
            }
        }, "线程1").start();

        new Thread(() -> {
            synchronized (object) {
                System.out.println("线程2 拿到了监视器锁。为什么呢，因为线程1 在 wait 方法的时候会自动释放锁");
                System.out.println("线程2 执行 notify 操作");
                object.notify();
                System.out.println("线程2 执行完了 notify，先休息3秒再说。");
                try {
                    Thread.sleep(3000);
                    System.out.println("线程2 休息完啦。注意了，调sleep方法和wait方法不一样，不会释放监视器锁");
                } catch (InterruptedException e) {
                    System.out.println("线程2 wait方法抛出了InterruptedException异常");
                }
                System.out.println("线程2 休息够了，结束操作");
            }
        }, "线程2").start();
    }


    public static void waitAndInterrupted() {
        Object object = new Object();

        Thread thread1 = new Thread(() -> {
            synchronized (object) {
                System.out.println("线程1 获取到监视器锁");
                try {
                    object.wait();
                    System.out.println("线程1 恢复啦。我为什么这么久才恢复，因为notify方法虽然早就发生了，可是我还要获取锁才能继续执行。");
                } catch (InterruptedException e) {
                    System.out.println("线程1 wait方法抛出了InterruptedException异常，即使是异常，我也是要获取到监视器锁了才会抛出");
                }
            }
        }, "线程1");
        thread1.start();

        new Thread(() -> {
            synchronized (object) {
                System.out.println("线程2 拿到了监视器锁。为什么呢，因为线程1 在 wait 方法的时候会自动释放锁");
                System.out.println("线程2 设置线程1 中断");
                thread1.interrupt();
                System.out.println("线程2 执行完了 中断，先休息3秒再说。");
                try {
                    Thread.sleep(3000);
                    System.out.println("线程2 休息完啦。注意了，调sleep方法和wait方法不一样，不会释放监视器锁");
                } catch (InterruptedException e) {
                    System.out.println("线程2 sleep方法抛出了InterruptedException异常");
                }
                System.out.println("线程2 休息够了，结束操作");
            }
        }, "线程2").start();
    }

    public static void notifyAndInterrupted() {
        Object object = new Object();

        WaitNotify waitNotify = new WaitNotify();

        Thread thread1 = new Thread(() -> {
            synchronized (object) {
                System.out.println("线程1 获取到监视器锁");
                try {
                    object.wait();
                    System.out.println("线程1 正常恢复啦。");
                } catch (InterruptedException e) {
                    System.out.println("线程1 wait方法抛出了InterruptedException异常");
                }
            }
        }, "线程1");
        thread1.start();

        Thread thread2 = new Thread(() -> {
            synchronized (object) {
                System.out.println("线程2 获取到监视器锁");
                try {
                    object.wait();
                    System.out.println("线程2 正常恢复啦。");
                } catch (InterruptedException e) {
                    System.out.println("线程2 wait方法抛出了InterruptedException异常");
                }
            }
        }, "线程2");
        thread2.start();

        // 这里让 thread1 和 thread2 先起来，然后再起后面的 thread3
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            synchronized (object) {
                System.out.println("线程3 拿到了监视器锁。");
                System.out.println("线程3 设置线程1中断");
                thread1.interrupt(); // 1
                waitNotify.a = 1; // 这行是为了禁止上下的两行中断和notify代码重排序
                System.out.println("线程3 调用notify");
                object.notify(); //2
                System.out.println("线程3 调用完notify后，休息一会");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println("线程2 sleep方法抛出了InterruptedException异常");
                }
                System.out.println("线程3 休息够了，结束同步代码块");
            }
        }, "线程3").start();
    }
}