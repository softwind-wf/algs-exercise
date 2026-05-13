package exercise1.exercise1_1;

public class LnFactorial {

    // 递归实现：计算 ln(N!)
    public static double lnFactorialRecursive(int N) {
        // 递归终止条件：0! = 1! = 1，ln(1) = 0
        if (N <= 1) {
            return 0.0;
        }
        // 递归核心逻辑：ln(N!) = ln(N) + ln((N-1)!)
        return Math.log(N) + lnFactorialRecursive(N - 1);
    }

    // 迭代实现：计算 ln(N!)（避免递归栈溢出，空间效率更高）
    public static double lnFactorialIterative(int N) {
        double result = 0.0;
        // 从2开始累加（因为ln(1)=0，不影响结果）
        for (int i = 2; i <= N; i++) {
            result += Math.log(i);
        }
        return result;
    }

    // 主方法：测试两种实现并对比结果
    public static void main(String[] args) {
        // 测试用例：可以修改这个值测试不同N的结果
        int testN = 10;

        // 调用递归版并输出
        double recursiveResult = lnFactorialRecursive(testN);
        System.out.println("递归版计算 ln(" + testN + "!) = " + recursiveResult);

        // 调用迭代版并输出
        double iterativeResult = lnFactorialIterative(testN);
        System.out.println("迭代版计算 ln(" + testN + "!) = " + iterativeResult);

        // 验证两种方法结果是否一致（浮点误差可忽略）
        System.out.println("两种方法结果是否一致：" + (Math.abs(recursiveResult - iterativeResult) < 1e-10));
    }
}