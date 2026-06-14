package exercise1.exercise1_1;

public class Log2Floor {
    // 静态方法 lg()
    public static int lg(int N) {
        if (N <= 0) {
            throw new IllegalArgumentException("N 必须是正整数");
        }
        
        int result = 0;
        // 用位运算右移来模拟除以 2，直到 N 变为 1
        while (N > 1) {
            N = N >> 1; // 等价于 N = N / 2
            result++;
        }
        return result;
    }

    public static void main(String[] args) {
        // 测试几个例子
        System.out.println(lg(1));   // 0 （因为 2⁰ = 1）
        System.out.println(lg(2));   // 1 （因为 2¹ = 2）
        System.out.println(lg(5));   // 2 （因为 2² = 4 ≤ 5 < 8 = 2³）
        System.out.println(lg(8));   // 3 （因为 2³ = 8）
        System.out.println(lg(10));  // 3 （因为 2³ = 8 ≤ 10 < 16 = 2⁴）
    }
}