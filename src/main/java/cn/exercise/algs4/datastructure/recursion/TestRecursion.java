package cn.exercise.algs4.datastructure.recursion;

import java.util.HashMap;
import java.util.Map;

public class TestRecursion {
    private Map<Long, Long> fibCache = new HashMap<>();

    public int addRecursive(int n) {
        if (n == 1) {
            return 1;
        }
        return n + addRecursive(n - 1);
    }

    public long fibonacci(long n) {
        if (n == 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }
        if (fibCache.containsKey(n)) {
            return fibCache.get(n);
        }
        long result = fibonacci(n - 1) + fibonacci(n - 2);
        fibCache.put(n, result);
        return result;
    }

    public static void main(String[] args) {
        TestRecursion test = new TestRecursion();
        System.out.println(test.addRecursive(5000));
        System.out.println(test.fibonacci(100));
    }


}
