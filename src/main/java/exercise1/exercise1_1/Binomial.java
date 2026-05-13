package exercise1.exercise1_1;

public class Binomial {

    // ======================
    // 1. 记忆化递归版（带缓存）
    // ======================
    private static double[][] memo;

    public static double binomialRecursive(int N, int k, double p) {
        memo = new double[N + 1][k + 1];
        // 初始化缓存为 -1，表示未计算
        for (int i = 0; i <= N; i++) {
            for (int j = 0; j <= k; j++) {
                memo[i][j] = -1.0;
            }
        }
        return binomialRecursiveCore(N, k, p);
    }

    private static double binomialRecursiveCore(int N, int k, double p) {
        if (N == 0 && k == 0) return 1.0;
        if (N < 0 || k < 0) return 0.0;
        if (memo[N][k] != -1.0) {
            return memo[N][k];
        }
        double result = (1.0 - p) * binomialRecursiveCore(N - 1, k, p) + p * binomialRecursiveCore(N - 1, k - 1, p);
        memo[N][k] = result;
        return result;
    }

    // ======================
    // 2. 迭代动态规划版
    // ======================
    public static double binomialIterative(int N, int k, double p) {
        double[][] dp = new double[N + 1][k + 1];
        dp[0][0] = 1.0;

        for (int n = 1; n <= N; n++) {
            dp[n][0] = Math.pow(1 - p, n);
            for (int j = 1; j <= Math.min(n, k); j++) {
                dp[n][j] = (1 - p) * dp[n - 1][j] + p * dp[n - 1][j - 1];
            }
        }
        return dp[N][k];
    }

    // ======================
    // 3. 主方法：测试两种实现
    // ======================
    public static void main(String[] args) {
        int N = 100;
        int k = 50;
        double p = 0.25;

        // 测试记忆化递归版
        long startTimeRecursive = System.currentTimeMillis();
        double resultRecursive = binomialRecursive(N, k, p);
        long endTimeRecursive = System.currentTimeMillis();
        System.out.println("记忆化递归版结果：" + resultRecursive);
        System.out.println("耗时：" + (endTimeRecursive - startTimeRecursive) + " ms");

        // 测试迭代动态规划版
        long startTimeIterative = System.currentTimeMillis();
        double resultIterative = binomialIterative(N, k, p);
        long endTimeIterative = System.currentTimeMillis();
        System.out.println("迭代动态规划版结果：" + resultIterative);
        System.out.println("耗时：" + (endTimeIterative - startTimeIterative) + " ms");

        // 验证结果一致性
        System.out.println("结果是否一致：" + (Math.abs(resultRecursive - resultIterative) < 1e-10));
    }
}