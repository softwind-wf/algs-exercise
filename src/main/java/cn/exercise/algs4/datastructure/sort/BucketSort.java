package cn.exercise.algs4.datastructure.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 桶排序：将数据分散到多个有序桶中，每个桶内部排序，最后合并。
 * 稳定排序（取决于桶内排序算法），非原地，时间复杂度 O(n + k) 平均，
 * k 为桶数。适用于数据分布均匀的场景。
 *
 * 与计数排序的关系：计数排序是桶大小=1 的特殊桶排序。
 */
public class BucketSort {

    // ==================== 整数版 ====================

    /**
     * 整数桶排序
     * 将 [min, max] 区间等分到 numBuckets 个桶中，
     * 每个桶内用插入排序。
     */
    public void sort(int[] a, int numBuckets) {
        int n = a.length;
        if (n <= 1) return;

        // 找范围
        int min = a[0], max = a[0];
        for (int v : a) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (min == max) return;

        // 创建桶
        List<List<Integer>> buckets = new ArrayList<>(numBuckets);
        for (int i = 0; i < numBuckets; i++) {
            buckets.add(new ArrayList<>());
        }

        // 分配：计算元素落入哪个桶
        // 桶索引 = (value - min) / bucketSize，最后一个桶兜底
        double range = (double) (max - min) + 1;  // +1 让 max 不越界
        for (int v : a) {
            int idx = (int) ((v - min) * numBuckets / range);
            buckets.get(idx).add(v);
        }

        // 桶内排序 + 合并
        int idx = 0;
        for (List<Integer> bucket : buckets) {
            if (bucket.isEmpty()) continue;
            insertionSort(bucket);
            for (int v : bucket) {
                a[idx++] = v;
            }
        }
    }

    /** 默认桶数 = n（经验值，数据均匀时最优） */
    public void sort(int[] a) {
        sort(a, Math.max(1, a.length));
    }

    // ==================== 浮点数版（最经典的桶排序场景） ====================

    /**
     * 浮点数桶排序，适用于 [0, 1) 均匀分布的数据。
     * 和 JDK 对 0~1 浮点的桶排序思路一致。
     */
    public void sort(double[] a) {
        int n = a.length;
        if (n <= 1) return;

        // 创建 n 个桶
        List<List<Double>> buckets = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            buckets.add(new ArrayList<>());
        }

        // 分配：桶索引 = floor(value * n)
        for (double v : a) {
            int idx = (int) (v * n);
            if (idx >= n) idx = n - 1;  // 边界保护
            buckets.get(idx).add(v);
        }

        // 桶内排序 + 合并
        int idx = 0;
        for (List<Double> bucket : buckets) {
            Collections.sort(bucket);  // JDK 的 TimSort，高效稳定
            for (double v : bucket) {
                a[idx++] = v;
            }
        }
    }

    // ==================== 辅助 ====================

    private void insertionSort(List<Integer> list) {
        for (int i = 1; i < list.size(); i++) {
            int key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j) > key) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        BucketSort bs = new BucketSort();

        // 整数桶排序
        int[] arr1 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        bs.sort(arr1);
        System.out.println("整数默认桶: " + Arrays.toString(arr1));

        int[] arr2 = {29, 25, 3, 49, 9, 37, 21, 43, 15};
        bs.sort(arr2, 5);
        System.out.println("整数5桶:    " + Arrays.toString(arr2));

        // 浮点数桶排序 [0, 1)
        double[] darr1 = {0.78, 0.17, 0.39, 0.26, 0.72, 0.94, 0.21, 0.12, 0.23, 0.68};
        bs.sort(darr1);
        System.out.println("浮点桶排序: " + Arrays.toString(darr1));

        // 边界测试
        int[] empty = {};
        bs.sort(empty);
        System.out.println("空数组:     " + Arrays.toString(empty));

        int[] single = {42};
        bs.sort(single);
        System.out.println("单元素:     " + Arrays.toString(single));

        int[] sorted = {1, 2, 3, 4, 5};
        bs.sort(sorted);
        System.out.println("已有序:     " + Arrays.toString(sorted));

        int[] reversed = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        bs.sort(reversed);
        System.out.println("逆序排序:   " + Arrays.toString(reversed));

        int[] dup = {4, 2, 4, 1, 2, 3, 1, 3};
        bs.sort(dup);
        System.out.println("含重复元素: " + Arrays.toString(dup));

        // 大量均匀分布数据
        int n = 100;
        int[] big = new int[n];
        for (int i = 0; i < n; i++) {
            big[i] = (int) (Math.random() * 1000);
        }
        bs.sort(big);
        boolean sortedOk = true;
        for (int i = 1; i < n; i++) {
            if (big[i - 1] > big[i]) { sortedOk = false; break; }
        }
        System.out.println("100个随机数: " + (sortedOk ? "正确" : "错误"));

        // 桶内排序过程演示
        System.out.println("\n=== 分桶过程 ===");
        demoBucket(bs);
    }

    static void demoBucket(BucketSort bs) {
        int[] a = {29, 25, 3, 49, 9, 37, 21, 43, 15};
        int numBuckets = 5;
        int min = 3, max = 49;
        double range = (double) (max - min) + 1;

        List<List<Integer>> buckets = new ArrayList<>(numBuckets);
        for (int i = 0; i < numBuckets; i++) buckets.add(new ArrayList<>());

        for (int v : a) {
            int idx = (int) ((v - min) * numBuckets / range);
            buckets.get(idx).add(v);
        }

        for (int i = 0; i < buckets.size(); i++) {
            System.out.printf("桶 %d [%.0f, %.0f): %s%n",
                    i,
                    min + i * range / numBuckets,
                    min + (i + 1) * range / numBuckets - 1,
                    buckets.get(i));
        }
    }
}
