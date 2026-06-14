package exercise1.exercise1_4;

import java.util.*;

public class EggDropping {
    
    /**
     * 鸡蛋掉落实验的结果
     */
    static class Result {
        int attempts;      // 总尝试次数
        int brokenEggs;    // 打碎的鸡蛋数
        int foundF;        // 找到的F层
        
        Result(int attempts, int brokenEggs, int foundF) {
            this.attempts = attempts;
            this.brokenEggs = brokenEggs;
            this.foundF = foundF;
        }
        
        @Override
        public String toString() {
            return String.format("F=%d, 尝试%d次, 打碎%d个蛋", 
                foundF, attempts, brokenEggs);
        }
    }
    
    /**
     * 鸡蛋掉落模拟器
     */
    static class EggSimulator {
        private final int N;        // 总层数
        private final int F;        // 真实的临界层
        private int eggs;           // 当前剩余鸡蛋
        private int attempts;       // 尝试次数
        private int brokenEggs;     // 打碎鸡蛋数
        
        public EggSimulator(int N, int F, int initialEggs) {
            this.N = N;
            this.F = F;
            this.eggs = initialEggs;
            this.attempts = 0;
            this.brokenEggs = 0;
        }
        
        /**
         * 在floor层扔鸡蛋
         * @return true 如果鸡蛋碎了（floor >= F），false 如果没碎
         */
        public boolean drop(int floor) {
            if (floor < 1 || floor > N) {
                throw new IllegalArgumentException("楼层必须在1到N之间");
            }
            
            attempts++;
            
            boolean broken = floor >= F;
            if (broken) {
                brokenEggs++;
                eggs--;
                if (eggs < 0) {
                    throw new RuntimeException("鸡蛋用完了！");
                }
            }
            
            return broken;
        }
        
        public int getAttempts() { return attempts; }
        public int getBrokenEggs() { return brokenEggs; }
        public int getRemainingEggs() { return eggs; }
    }



        /**
         * 策略1：二分查找
         * 需要 ~lgN 个鸡蛋，尝试 ~lgN 次，打碎 ~lgN 个蛋
         */
        public static Result binarySearchStrategy(int N, int F) {
            System.out.println("\n=== 策略1：二分查找 ===");
            EggSimulator sim = new EggSimulator(N, F, (int)(Math.log(N)/Math.log(2)) + 5);

            int low = 1;
            int high = N;

            while (low <= high) {
                int mid = low + (high - low) / 2;

                boolean broken = sim.drop(mid);

                if (broken) {
                    // 碎了，F <= mid
                    high = mid - 1;
                    // 如果high < low，说明找到了
                    if (high < low) {
                        break;
                    }
                } else {
                    // 没碎，F > mid
                    low = mid + 1;
                }
            }

            // F 是第一个会碎掉的楼层，所以是 low
            int foundF = low;

            return new Result(sim.getAttempts(), sim.getBrokenEggs(), foundF);
        }



        /**
         * 策略2：两阶段法
         * 第一阶段：指数探测确定范围
         * 第二阶段：在范围内二分查找
         * 总成本 ~2lgF
         */
        public static Result twoPhaseStrategy(int N, int F) {
            System.out.println("\n=== 策略2：两阶段法（~2lgF） ===");

            // 需要足够多的鸡蛋（lgN个就够了）
            EggSimulator sim = new EggSimulator(N, F, (int)(Math.log(N)/Math.log(2)) + 5);

            // 第一阶段：指数探测
            System.out.println("第一阶段：指数探测");
            int floor = 1;
            int step = 1;
            boolean broken = false;

            while (floor <= N && !broken) {
                broken = sim.drop(floor);

                if (!broken) {
                    // 没碎，继续指数增长
                    step *= 2;
                    floor = Math.min(floor + step, N);
                    System.out.printf("  安全到达 %d 层%n", floor);
                } else {
                    System.out.printf("  在 %d 层碎了！%n", floor);
                }
            }

            // 确定搜索范围
            int low, high;
            if (broken) {
                // 在 floor 层碎了，F 在上一个安全楼层和 floor 之间
                high = floor;
                low = Math.max(1, floor - step);
            } else {
                // 到了顶层都没碎，F = N+1（题目假设F <= N）
                return new Result(sim.getAttempts(), sim.getBrokenEggs(), N + 1);
            }

            System.out.printf("搜索范围：[%d, %d]%n", low, high);

            // 第二阶段：在范围内二分查找
            System.out.println("第二阶段：二分查找");

            while (low < high) {
                int mid = low + (high - low) / 2;

                broken = sim.drop(mid);

                if (broken) {
                    // 碎了，F <= mid
                    high = mid;
                } else {
                    // 没碎，F > mid
                    low = mid + 1;
                }
            }

            int foundF = low;
            return new Result(sim.getAttempts(), sim.getBrokenEggs(), foundF);
        }




        /**
         * 策略3：线性搜索（只有一个鸡蛋时的最优策略）
         * 成本 = F 次尝试，1个碎蛋
         */
        public static Result linearSearchStrategy(int N, int F) {
            System.out.println("\n=== 策略3：线性搜索（一个鸡蛋） ===");
            EggSimulator sim = new EggSimulator(N, F, 1);

            for (int floor = 1; floor <= N; floor++) {
                boolean broken = sim.drop(floor);

                if (broken) {
                    // 碎了，F 就是当前楼层
                    return new Result(sim.getAttempts(), sim.getBrokenEggs(), floor);
                }
            }

            // 所有楼层都没碎，F = N+1
            return new Result(sim.getAttempts(), sim.getBrokenEggs(), N + 1);
        }

    

        public static void main(String[] args) {
            int N = 1000;  // 大楼层数
            int[] testF = {1, 10, 50, 100, 500, 999};

            System.out.println("大楼层数 N = " + N);
            System.out.println("====================================================================");

            for (int F : testF) {
                System.out.printf("\n【测试】真实F = %d%n", F);
                System.out.println("---------------------------------------------------------------------");

                // 策略1：二分查找
                Result r1 = binarySearchStrategy(N, F);
                System.out.println("二分查找: " + r1);

                // 策略2：两阶段法
                Result r2 = twoPhaseStrategy(N, F);
                System.out.println("两阶段法: " + r2);

                // 策略3：线性搜索
                Result r3 = linearSearchStrategy(N, F);
                System.out.println("线性搜索: " + r3);

                System.out.println("-------------------------------------------------------------------------------");
            }

            // 性能对比测试
            System.out.println("\n【性能对比】");
            performanceTest(10000, 5000);
        }

        /**
         * 性能测试：统计不同策略的平均尝试次数
         */
        public static void performanceTest(int N, int maxF) {
            Random rand = new Random(42);
            int testCount = 100;

            double avgAttempts1 = 0;
            double avgAttempts2 = 0;
            double avgAttempts3 = 0;

            double avgBroken1 = 0;
            double avgBroken2 = 0;
            double avgBroken3 = 0;

            for (int i = 0; i < testCount; i++) {
                int F = rand.nextInt(maxF) + 1;

                Result r1 = binarySearchStrategy(N, F);
                Result r2 = twoPhaseStrategy(N, F);
                Result r3 = linearSearchStrategy(N, F);

                avgAttempts1 += r1.attempts;
                avgAttempts2 += r2.attempts;
                avgAttempts3 += r3.attempts;

                avgBroken1 += r1.brokenEggs;
                avgBroken2 += r2.brokenEggs;
                avgBroken3 += r3.brokenEggs;
            }

            avgAttempts1 /= testCount;
            avgAttempts2 /= testCount;
            avgAttempts3 /= testCount;

            avgBroken1 /= testCount;
            avgBroken2 /= testCount;
            avgBroken3 /= testCount;

            System.out.printf("二分查找: 平均尝试 %.2f 次, 平均碎蛋 %.2f 个%n",
                    avgAttempts1, avgBroken1);
            System.out.printf("两阶段法: 平均尝试 %.2f 次, 平均碎蛋 %.2f 个 (理论 ~2lgF ≈ %.2f)%n",
                    avgAttempts2, avgBroken2, 2 * Math.log(maxF/2) / Math.log(2));
            System.out.printf("线性搜索: 平均尝试 %.2f 次, 平均碎蛋 %.2f 个 (理论 ~F/2 ≈ %.2f)%n",
                    avgAttempts3, avgBroken3, maxF/2.0);
        }
    



















}