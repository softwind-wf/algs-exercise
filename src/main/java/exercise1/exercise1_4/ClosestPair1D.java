package exercise1.exercise1_4;

import java.util.Arrays;

public class ClosestPair1D {

    public static void findClosestPair(double[] a) {
        if (a == null || a.length < 2) {
            System.out.println("数组长度必须 ≥ 2");
            return;
        }

        // 1. 排序
        Arrays.sort(a);

        // 2. 找相邻最小差
        double minDiff = Double.MAX_VALUE;
        double num1 = a[0];
        double num2 = a[1];

        for (int i = 0; i < a.length - 1; i++) {
            double diff = Math.abs(a[i + 1] - a[i]);
            if (diff < minDiff) {
                minDiff = diff;
                num1 = a[i];
                num2 = a[i + 1];
            }
        }

        // 输出
        System.out.printf("最接近的一对数是 %.6f 和 %.6f，差值为 %.6f\n", num1, num2, minDiff);
    }

    public static void main(String[] args) {
        double[] test = {5.3, 9.8, 1.2, 3.5, 7.1, 4.2, 2.8};
        findClosestPair(test);
    }
}