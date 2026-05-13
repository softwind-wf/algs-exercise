package exercise1.exercise1_1;

public class Fibonacci2 {
    public static long F(int N) {
        if (N == 0) return 0;
        if (N == 1) return 1;
        // 用数组保存中间结果
        long[] fib = new long[N + 1];
        fib[0] = 0;
        fib[1] = 1;
        for (int i = 2; i <= N; i++) {
            fib[i] = fib[i - 1] + fib[i - 2];
        }
        return fib[N];
    }

    public static void main(String[] args) {
        for (int N = 0; N < 60; N++) {
            System.out.println(N + " " + F(N));
        }
    }
}