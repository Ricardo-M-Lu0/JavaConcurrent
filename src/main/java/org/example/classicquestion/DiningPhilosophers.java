package org.example.classicquestion;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PV操作经典例题-哲学家就餐问题
 * <p>
 * 对于互斥访问有限的竞争问题（如 I/O 设备）一类的建模过程十分有用
 * <p>
 * <a href="https://leetcode.cn/problems/the-dining-philosophers/">1226. 哲学家进餐</a>
 * tag：操作系统、信号量、同步、死锁
 *
 * @author Ricardo
 * @version 1.0
 * @date 2024/4/15 17:11
 * @see <a href="https://xiaolincoding.com/os/4_process/multithread_sync.html#%E5%93%B2%E5%AD%A6%E5%AE%B6%E5%B0%B1%E9%A4%90%E9%97%AE%E9%A2%98">哲学家就餐</a>
 * @since 1.0
 */
public class DiningPhilosophers {

    public static void main(String[] args) {
        DiningPhilosophers3 diningPhilosophers3 = new DiningPhilosophers3();
        DiningPhilosophers4 diningPhilosophers4 = new DiningPhilosophers4();
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            new Thread(() -> diningPhilosophers3.smartPerson(finalI), "[" + finalI + "]").start();
        }
    }

    static class DiningPhilosophers3 {
        final int N = 5;

        // 每个哲学家一个信号量初始值为0
        final Semaphore[] fork = new Semaphore[N];

        public DiningPhilosophers3() {
            for (int i = 0; i < fork.length; i++) {
                fork[i] = new Semaphore(1);
            }
        }

        public void smartPerson(int i) {
            try {
                while (true) {
                    think(i); //思考
                    Semaphore leftFork = fork[i];
                    Semaphore rightFork = fork[right(i)];
                    if (i % 2 == 0) {
                        // 偶数编号的哲学家先拿左边的叉子，后拿右边的叉子
                        leftFork.acquire();
                        rightFork.acquire();
                    } else {
                        // 奇数编号的哲学家先拿右边的叉子，后拿左边的叉子
                        rightFork.acquire();
                        leftFork.acquire();
                    }
                    eat(i); //就餐
                    leftFork.release();
                    rightFork.release();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 就餐，拿到两把叉子后执行
        private void eat(int i) {
            System.out.printf("[eat] 哲学家 %d 正在就餐%n", i);
        }

        // 思考
        private void think(int i) {
            System.out.printf("[think] 哲学家 %d 正在思考%n", i);
        }

        private int left(int i) {
            return (i + N - 1) % N;
        }

        private int right(int i) {
            return (i + 1) % N;
        }
    }

    static class DiningPhilosophers4 {
        // 思考状态
        final int THINKING = 0;
        // 饥饿状态
        final int HUNGRY = 1;
        // 进餐状态
        final int EATING = 2;

        final int N = 5;

        int[] state = new int[N];

        // 每个哲学家一个信号量初始值为0
        final Semaphore[] s = new Semaphore[5];
        // 互斥信号量
        final Semaphore mutex = new Semaphore(1);

        public DiningPhilosophers4() {
            for (int i = 0; i < s.length; i++) {
                s[i] = new Semaphore(0);
            }
        }

        public void smartPerson(int i) {
            while (true) {
                think(i); //思考
                takeForks(i); //准备拿去叉子吃饭
                eat(i); //就餐
                putForks(i); //吃完放回叉子
            }
        }

        /**
         * 标记哲学家状态
         *
         * @param i 哲学家编号 0~4
         */
        void test(int i) {
            // 如果i号的左边右边哲学家都不是进餐状态，把i号哲学家标记为进餐状态
            if (state[i] == HUNGRY & state[left(i)] != EATING
                    & state[right(i)] != EATING) {
                state[i] = EATING; //两把叉子到手，进餐状态
                s[i].release(); //通知第哲学家可以进餐了
            }
        }

        /**
         * 拿叉子
         * 要么拿到两把叉子，要么被阻塞
         *
         * @param i 哲学家编号 0~4
         */
        private void takeForks(int i) {
            try {
                // 进入临界区
                mutex.acquire();
                // 标记为饥饿状态
                state[i] = HUNGRY;
                // 尝试获取叉子
                test(i);
                // 离开临界区
                mutex.release();
                // 没有叉子就阻塞，有叉子就继续运行
                s[i].acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 放叉子
         * 把两把叉子放回原处，并在需要的时候，去唤醒左邻右舍
         *
         * @param i 哲学家编号 0~4
         */
        private void putForks(int i) {
            try {
                // 进入临界区
                mutex.acquire();
                // 标记为思考状态
                state[i] = THINKING;
                // 检查左边的哲学家是否在进餐，没则唤醒
                test(left(i));
                // 检查右边的哲学家是否在进餐，没则唤醒
                test(right(i));
                // 离开临界区
                mutex.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 就餐，拿到两把叉子后执行
        private void eat(int i) {
            System.out.printf("[eat] 哲学家 %d 正在就餐%n", i);
        }

        // 思考
        private void think(int i) {
            System.out.printf("[think] 哲学家 %d 正在思考%n", i);
        }

        private int left(int i) {
            return (i + N - 1) % N;
        }

        private int right(int i) {
            return (i + 1) % N;
        }
    }

    /**
     * leetcode题解
     * <a href="https://leetcode.cn/problems/the-dining-philosophers/solutions/36049/1ge-semaphore-1ge-reentrantlockshu-zu-by-gfu/">leetcode题解</a>
     */
    static class DiningPhilosophers5 {
        // 初始化为0, 二进制表示则为00000, 说明当前所有叉子都未被使用
        private final AtomicInteger fork = new AtomicInteger(0);
        // 每个叉子的int值(即二进制的00001, 00010, 00100, 01000, 10000)
        private final int[] forkMask = new int[]{1, 2, 4, 8, 16};
        // 限制 最多只有4个哲学家去持有叉子
        private final Semaphore eatLimit = new Semaphore(4);

        // call the run() method of any runnable to execute its code
        public void wantsToEat(int philosopher,
                               Runnable pickLeftFork,
                               Runnable pickRightFork,
                               Runnable eat,
                               Runnable putLeftFork,
                               Runnable putRightFork) throws InterruptedException {

            int leftMask = forkMask[(philosopher + 1) % 5], rightMask = forkMask[philosopher];
            eatLimit.acquire();    //限制的人数 -1

            while (!pickFork(leftMask)) Thread.sleep(1);    //拿起左边的叉子
            while (!pickFork(rightMask)) Thread.sleep(1);   //拿起右边的叉子

            pickLeftFork.run();    //拿起左边的叉子 的具体执行
            pickRightFork.run();    //拿起右边的叉子 的具体执行

            eat.run();    //吃意大利面 的具体执行

            putLeftFork.run();    //放下左边的叉子 的具体执行
            putRightFork.run();    //放下右边的叉子 的具体执行

            while (!putFork(leftMask)) Thread.sleep(1);     //放下左边的叉子
            while (!putFork(rightMask)) Thread.sleep(1);    //放下右边的叉子

            eatLimit.release(); //限制的人数 +1
        }

        private boolean pickFork(int mask) {
            int expect = fork.get();
            return (expect & mask) <= 0 && fork.compareAndSet(expect, expect ^ mask);
        }

        private boolean putFork(int mask) {
            int expect = fork.get();
            return fork.compareAndSet(expect, expect ^ mask);
        }
    }

}
