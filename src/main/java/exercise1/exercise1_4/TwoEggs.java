package exercise1.exercise1_4;

public class TwoEggs {

    // 模拟扔鸡蛋：true 表示碎
    static boolean drop(int floor, int F) {
        return floor >= F;
    }

    // 策略1：等间隔递减跳，最坏 ~2√N 次
    public static int findFWithTwoEggs(int N, int F) {
        int step = (int) Math.sqrt(2 * N);
        int prev = 0;
        int attempts = 0;
        int eggs = 2;

        // 第一个鸡蛋做间隔跳
        while (eggs >0 && prev < N) {
            int next = Math.min(prev + step, N);
            attempts++;
            if (drop(next, F)) {  // 碎了
                eggs--;
                // 用第二个鸡蛋线性扫描 [prev+1, next-1]
                for (int floor = prev + 1; floor < next; floor++) {
                    attempts++;
                    if (drop(floor, F)) {
                        eggs--;
                        System.out.printf("尝试次数: %d, 找到 F=%d%n", attempts, floor);
                        return floor;
                    }
                }
                System.out.printf("尝试次数: %d, 找到 F=%d%n", attempts, next);
                return next;
            }
            prev = next;
            step--;  // 步长递减
        }

        // 如果一直没碎，F = N+1（题目可能定义F<=N，这里安全返回）
        System.out.printf("尝试次数: %d, F=%d%n", attempts, N + 1);
        return N + 1;
    }

    public static int findFWithTwoEggsOptimized(int N, int F) {
        int low = 1;
        int high = 1;
        int attempts = 0;
        int eggs = 2;

        // 阶段1：指数探测，找到上界
        while (high < N && eggs >0) {
            attempts++;
            if (drop(high, F)) {  // 碎了
                eggs--;
                // 在 [low, high] 内线性扫描
                for (int floor = low; floor < high; floor++) {
                    attempts++;
                    if (drop(floor, F)) {
                        eggs--;
                        System.out.printf("尝试次数: %d, 找到 F=%d%n", attempts, floor);
                        return floor;
                    }
                }
                System.out.printf("尝试次数: %d, 找到 F=%d%n", attempts, high);
                return high;
            }
            low = high;
            high = Math.min(high * 2, N);
        }

        // 如果一直没碎
        System.out.printf("尝试次数: %d, F=%d%n", attempts, N + 1);
        return N + 1;
    }

    public static void main(String[] args) {
        int N = 100;
        int F = 57;

        findFWithTwoEggs(N, F);
        findFWithTwoEggsOptimized(N,F);
    }
}