package cn.exercise.algs4.datastructure.sort;

import java.util.Arrays;

/**
 * 插入排序：将数组分为有序区和无序区，每次从无序区取一个元素，
 * 在有序区中找到正确位置并插入。
 * 稳定排序，原地排序，时间复杂度 O(n²)，最好情况 O(n)。
 */
public class InsertionSort {

    /**
     * 基础版插入排序
     * 从第二个元素开始，依次向前比较并插入到正确位置。
     */
    public void sort(int[] a) {
        int n = a.length;
        for (int i = 1; i < n; i++) {
            int key = a[i];
            int j = i - 1;
            // 将比 key 大的元素依次后移
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            // 插入到正确位置
            a[j + 1] = key;
        }
    }

    /**
     * 交换版：通过相邻元素交换实现插入，代码更简洁。
     */
    public void sortSwap(int[] a) {
        int n = a.length;
        for (int i = 1; i < n; i++) {
            for (int j = i; j > 0 && a[j] < a[j - 1]; j--) {
                swap(a, j, j - 1);
            }
        }
    }

    /**
     * 二分插入排序：在有序区用二分查找定位插入位置，减少比较次数。
     * 比较次数降为 O(n log n)，元素移动次数仍为 O(n²)。
     */
    public void binarySort(int[] a) {
        int n = a.length;
        for (int i = 1; i < n; i++) {
            int key = a[i];
            // 二分查找插入位置
            int lo = 0, hi = i;
            while (lo < hi) {
                int mid = lo + (hi - lo) / 2;
                if (a[mid] <= key) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            // 将 [lo, i-1] 的元素后移一位
            for (int j = i; j > lo; j--) {
                a[j] = a[j - 1];
            }
            a[lo] = key;
        }
    }

    /**
     * 希尔排序：插入排序的改进版。
     * 通过递减的 gap 分组进行插入排序，让元素可以大步移动。
     * 不稳定排序，时间复杂度取决于 gap 序列（此处使用 Knuth 序列），
     * 平均 O(n^{1.3})。
     */
    public void shellSort(int[] a) {
        int n = a.length;
        // Knuth 增量序列: 1, 4, 13, 40, 121, ...
        int gap = 1;
        while (gap < n / 3) {
            gap = gap * 3 + 1;
        }
        while (gap >= 1) {
            for (int i = gap; i < n; i++) {
                int key = a[i];
                int j = i - gap;
                while (j >= 0 && a[j] > key) {
                    a[j + gap] = a[j];
                    j -= gap;
                }
                a[j + gap] = key;
            }
            gap /= 3;
        }
    }

    /**
     * 对数组指定区间 [lo, hi] 进行插入排序。
     * 常作为归并排序/快速排序在小区间上的优化手段。
     */
    public void sort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= lo && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    private void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void main(String[] args) {
        InsertionSort is = new InsertionSort();

        int[] arr1 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        is.sort(arr1);
        System.out.println("基础版:     " + Arrays.toString(arr1));

        int[] arr2 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        is.sortSwap(arr2);
        System.out.println("交换版:     " + Arrays.toString(arr2));

        int[] arr3 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        is.binarySort(arr3);
        System.out.println("二分插入:   " + Arrays.toString(arr3));

        int[] arr4 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        is.shellSort(arr4);
        System.out.println("希尔排序:   " + Arrays.toString(arr4));

        int[] arr5 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        is.sort(arr5, 2, 6);
        System.out.println("区间排序:   " + Arrays.toString(arr5));

        // 边界测试
        int[] empty = {};
        is.sort(empty);
        System.out.println("空数组:     " + Arrays.toString(empty));

        int[] single = {42};
        is.sort(single);
        System.out.println("单元素:     " + Arrays.toString(single));

        int[] sorted = {1, 2, 3, 4, 5};
        is.sort(sorted);
        System.out.println("已有序:     " + Arrays.toString(sorted));

        int[] reversed = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        is.sort(reversed);
        System.out.println("逆序排序:   " + Arrays.toString(reversed));

        int[] dup = {4, 2, 4, 1, 2, 3, 1, 3};
        is.sort(dup);
        System.out.println("含重复元素: " + Arrays.toString(dup));
    }
}
