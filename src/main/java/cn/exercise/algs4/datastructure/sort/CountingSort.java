package cn.exercise.algs4.datastructure.sort;

import java.util.Arrays;

/**
 * 计数排序：非比较排序，统计每个值的出现次数，再按顺序回填。
 * 稳定排序（从后往前遍历保证），非原地（需要额外空间），
 * 时间复杂度 O(n + k)，k 为值的范围。
 *
 * 适用于：整数范围不大且已知上下界的场景。
 * 常作为基数排序的子程序。
 */
public class CountingSort {

    /**
     * 基础版计数排序（不稳定）
     * 统计频次后直接按值顺序回填，不保留相同值的原始顺序。
     *
     * @param a    待排序数组（元素必须 >= 0）
     * @param max  数组中的最大值（包含）
     */
    public void sort(int[] a, int max) {
        int n = a.length;
        if (n <= 1) return;

        // 1. 计数
        int[] count = new int[max + 1];
        for (int v : a) {
            count[v]++;
        }

        // 2. 回填（按值从小到大覆盖原数组）
        int idx = 0;
        for (int v = 0; v <= max; v++) {
            for (int c = 0; c < count[v]; c++) {
                a[idx++] = v;
            }
        }
    }

    /**
     * 自动探测范围的版本
     */
    public void sort(int[] a) {
        if (a.length <= 1) return;
        int max = a[0], min = a[0];
        for (int v : a) {
            if (v > max) max = v;
            if (v < min) min = v;
        }
        sort(a, min, max);
    }

    /**
     * 支持负数范围的版本
     *
     * @param a   待排序数组
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     */
    public void sort(int[] a, int min, int max) {
        int n = a.length;
        if (n <= 1) return;

        int range = max - min + 1;
        int[] count = new int[range];

        for (int v : a) {
            count[v - min]++;
        }

        int idx = 0;
        for (int v = 0; v < range; v++) {
            int realVal = v + min;
            for (int c = 0; c < count[v]; c++) {
                a[idx++] = realVal;
            }
        }
    }

    /**
     * 稳定版计数排序
     * 通过累计计数 + 从后往前遍历，保证相等元素的相对顺序不变。
     *
     * 步骤：
     * 1. 统计频次
     * 2. count[i] += count[i-1] → 变成"前缀和"，表示值 i 在输出数组中的尾位置+1
     * 3. 从后往前遍历原数组，放入正确位置，count 递减
     * 4. 拷贝回原数组
     */
    public void sortStable(int[] a) {
        int n = a.length;
        if (n <= 1) return;

        // 找 min / max
        int max = a[0], min = a[0];
        for (int v : a) {
            if (v > max) max = v;
            if (v < min) min = v;
        }

        int range = max - min + 1;
        int[] count = new int[range];

        // 1. 计数
        for (int v : a) {
            count[v - min]++;
        }

        // 2. 前缀和：count[i] = 值 i 在输出数组中的"尾下标 + 1"
        for (int i = 1; i < range; i++) {
            count[i] += count[i - 1];
        }

        // 3. 从后往前遍历，放入输出数组
        int[] output = new int[n];
        for (int i = n - 1; i >= 0; i--) {
            int v = a[i];
            int pos = --count[v - min];    // 先减后取，得到写入位置
            output[pos] = v;
        }

        // 4. 拷贝回原数组
        System.arraycopy(output, 0, a, 0, n);
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        CountingSort cs = new CountingSort();

        // 基础版：指定 max
        int[] arr1 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        cs.sort(arr1, 9);
        System.out.println("基础版:     " + Arrays.toString(arr1));

        // 自动探范围
        int[] arr2 = {15, 3, 21, 8, 15, 3, 7, 8};
        cs.sort(arr2);
        System.out.println("自动范围:   " + Arrays.toString(arr2));

        // 支持负数
        int[] arr3 = {-3, 5, -1, 2, -3, 0, -5, 4};
        cs.sort(arr3, -5, 5);
        System.out.println("含负数:     " + Arrays.toString(arr3));

        // 稳定版
        int[] arr4 = {3, 1, 3, 2, 1, 4, 2, 3};
        cs.sortStable(arr4);
        System.out.println("稳定版:     " + Arrays.toString(arr4));

        // 稳定性验证
        verifyStability(cs);

        // 边界测试
        int[] empty = {};
        cs.sort(empty, 0);
        System.out.println("空数组:     " + Arrays.toString(empty));

        int[] single = {42};
        cs.sort(single);
        System.out.println("单元素:     " + Arrays.toString(single));

        int[] sorted = {1, 2, 3, 4, 5};
        cs.sort(sorted, 5);
        System.out.println("已有序:     " + Arrays.toString(sorted));

        // 大量重复
        int[] dup = {7, 7, 3, 3, 5, 5, 3, 7, 5, 3};
        cs.sortStable(dup);
        System.out.println("大量重复:   " + Arrays.toString(dup));

        // 稳定版排序过程演示
        System.out.println("\n=== 稳定版计数排序过程 ===");
        demoStable(cs);
    }

    static void verifyStability(CountingSort cs) {
        // 用带标记的元素验证：排序后相同值的原始索引应递增
        int n = 10;
        int[] a = new int[n];
        int[] expectedOrder = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i % 3;  // 值只有 0, 1, 2
            expectedOrder[i] = i % 3;
        }
        cs.sortStable(a);

        // 检查：相同值第一次出现的原始索引应小于第二次出现的
        int lastIdx0 = -1, lastIdx1 = -1, lastIdx2 = -1;
        boolean stable = true;
        for (int i = 0; i < n; i++) {
            if (a[i] == 0) {
                if (i < lastIdx0) { stable = false; break; }
                lastIdx0 = i;
            }
        }

        System.out.println("稳定性验证: " + (stable ? "稳定" : "不稳定"));
    }

    static void demoStable(CountingSort cs) {
        int[] a = {3, 1, 3, 2, 1};
        int n = a.length, min = 1, max = 3, range = max - min + 1;

        System.out.println("原始: " + Arrays.toString(a));

        int[] count = new int[range];
        for (int v : a) count[v - min]++;
        System.out.println("计数: " + Arrays.toString(count));

        for (int i = 1; i < range; i++) count[i] += count[i - 1];
        System.out.println("前缀和(尾位置+1): " + Arrays.toString(count));

        int[] output = new int[n];
        System.out.println("\n从后往前填入：");
        for (int i = n - 1; i >= 0; i--) {
            int v = a[i];
            int pos = --count[v - min];
            output[pos] = v;
            System.out.printf("  i=%d, v=%d → 位置 %d → %s%n",
                    i, v, pos, Arrays.toString(output));
        }

        System.arraycopy(output, 0, a, 0, n);
        System.out.println("\n结果: " + Arrays.toString(a));
    }
}
