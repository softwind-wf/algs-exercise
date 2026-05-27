package test5;

import java.util.Arrays;
import java.util.Random;

public class SortPerformanceTest {
    // 切换为插入排序的阈值（和《算法》书中一致）
    private static final int M = 15;

    // ========== 三向字符串快速排序 ==========
    private static int charAt(String s, int d) {
        return (d < s.length()) ? s.charAt(d) : -1;
    }

    private static void exch(String[] a, int i, int j) {
        String temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    // 从第d位开始的插入排序（小数组优化）
    private static void insertionSort(String[] a, int lo, int hi, int d) {
        for (int i = lo; i <= hi; i++) {
            for (int j = i; j > lo && less(a[j], a[j - 1], d); j--) {
                exch(a, j, j - 1);
            }
        }
    }

    private static boolean less(String v, String w, int d) {
        return v.substring(d).compareTo(w.substring(d)) < 0;
    }

    public static void quick3Sort(String[] a) {
        quick3Sort(a, 0, a.length - 1, 0);
    }

    private static void quick3Sort(String[] a, int lo, int hi, int d) {
        if (hi <= lo + M) {
            insertionSort(a, lo, hi, d);
            return;
        }

        int lt = lo, gt = hi;
        int v = charAt(a[lo], d);
        int i = lo + 1;

        while (i <= gt) {
            int t = charAt(a[i], d);
            if (t < v) exch(a, lt++, i++);
            else if (t > v) exch(a, i, gt--);
            else i++;
        }

        quick3Sort(a, lo, lt - 1, d);
        if (v >= 0) quick3Sort(a, lt, gt, d + 1);
        quick3Sort(a, gt + 1, hi, d);
    }

    // ========== 生成测试数据 ==========
    private static String[] generateRandomStrings(int n, int maxLen) {
        Random r = new Random();
        String[] arr = new String[n];
        for (int i = 0; i < n; i++) {
            int len = r.nextInt(maxLen) + 1;
            StringBuilder sb = new StringBuilder(len);
            for (int j = 0; j < len; j++) {
                sb.append((char) ('a' + r.nextInt(26)));
            }
            arr[i] = sb.toString();
        }
        return arr;
    }

    // ========== 性能测试 ==========
    public static void main(String[] args) {
        int n = 100_000;    // 字符串数量
        int maxLen = 20;    // 每个字符串最大长度
        int trials = 5;     // 测试轮数，取平均时间

        long totalQuick3 = 0;
        long totalArrays = 0;

        for (int t = 0; t < trials; t++) {
            String[] arr1 = generateRandomStrings(n, maxLen);
            String[] arr2 = Arrays.copyOf(arr1, arr1.length);

            // 测试三向字符串快速排序
            long start = System.nanoTime();
            quick3Sort(arr1);
            long time = System.nanoTime() - start;
            totalQuick3 += time;
            System.out.printf("第%d轮 - 三向快排耗时: %.2f ms\n", t + 1, time / 1e6);

            // 测试 Arrays.sort
            start = System.nanoTime();
            Arrays.sort(arr2);
            time = System.nanoTime() - start;
            totalArrays += time;
            System.out.printf("第%d轮 - Arrays.sort耗时: %.2f ms\n", t + 1, time / 1e6);

            // 验证排序结果一致
            if (!Arrays.equals(arr1, arr2)) {
                System.err.println("排序结果不一致！");
                return;
            }
        }

        double avgQuick3 = totalQuick3 / (trials * 1e6);
        double avgArrays = totalArrays / (trials * 1e6);

        System.out.println("\n===== 平均耗时对比 =====");
        System.out.printf("三向字符串快速排序: %.2f ms\n", avgQuick3);
        System.out.printf("Arrays.sort:        %.2f ms\n", avgArrays);

        if (avgQuick3 < avgArrays) {
            System.out.printf("三向字符串快速排序比 Arrays.sort 快了 %.2f%%\n",
                    (avgArrays - avgQuick3) / avgArrays * 100);
        } else {
            System.out.printf("Arrays.sort 比三向字符串快速排序快了 %.2f%%\n",
                    (avgQuick3 - avgArrays) / avgQuick3 * 100);
        }
    }
}