package com.ds.sort;

import java.util.Arrays;

/**
 * 冒泡排序：反复遍历数组，相邻元素两两比较，将较大值逐步"冒泡"到末尾。
 * 稳定排序，原地排序，时间复杂度 O(n²)。
 */
public class BubbleSort {

    /**
     * 基础版冒泡排序
     * 每轮把当前未排序部分的最大值冒泡到最后。
     */
    public void sort(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            // 第 i 轮：将 [0, n-1-i] 中的最大值沉到 n-1-i 位置
            for (int j = 0; j < n - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    swap(a, j, j + 1);
                }
            }
        }
    }

    /**
     * 优化版冒泡排序
     * 如果某一轮没有发生交换，说明已经有序，提前结束。
     */
    public void sortOptimized(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    swap(a, j, j + 1);
                    swapped = true;
                }
            }
            if (!swapped) {
                break;  // 没有交换，已有序
            }
        }
    }

    /**
     * 进一步优化：记录最后一轮交换的位置，该位置之后已经有序。
     */
    public void sortOptimized2(int[] a) {
        int n = a.length;
        int lastSwap = n - 1;  // 最后一轮发生交换的位置
        while (lastSwap > 0) {
            int bound = lastSwap;
            lastSwap = 0;
            for (int j = 0; j < bound; j++) {
                if (a[j] > a[j + 1]) {
                    swap(a, j, j + 1);
                    lastSwap = j;  // 记录最后一次交换
                }
            }
        }
    }

    /**
     * 鸡尾酒排序（双向冒泡）
     * 从左到右冒泡一次，再从右到左冒泡一次，轮流进行。
     */
    public void cocktailSort(int[] a) {
        int lo = 0;
        int hi = a.length - 1;
        boolean swapped = true;

        while (lo < hi && swapped) {
            swapped = false;

            // 从左到右：最大值沉底
            for (int i = lo; i < hi; i++) {
                if (a[i] > a[i + 1]) {
                    swap(a, i, i + 1);
                    swapped = true;
                }
            }
            hi--;

            // 从右到左：最小值上浮
            for (int i = hi; i > lo; i--) {
                if (a[i] < a[i - 1]) {
                    swap(a, i, i - 1);
                    swapped = true;
                }
            }
            lo++;
        }
    }

    private void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void main(String[] args) {
        BubbleSort bs = new BubbleSort();

        int[] arr1 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        bs.sort(arr1);
        System.out.println("基础版: " + Arrays.toString(arr1));

        int[] arr2 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        bs.sortOptimized(arr2);
        System.out.println("优化版: " + Arrays.toString(arr2));

        int[] arr3 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        bs.sortOptimized2(arr3);
        System.out.println("优化版2: " + Arrays.toString(arr3));

        int[] arr4 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        bs.cocktailSort(arr4);
        System.out.println("鸡尾酒: " + Arrays.toString(arr4));

        // 边界测试
        int[] empty = {};
        bs.sort(empty);
        System.out.println("空数组: " + Arrays.toString(empty));

        int[] single = {42};
        bs.sort(single);
        System.out.println("单元素: " + Arrays.toString(single));

        // 最优情况：已有序，优化版一轮就退出
        int[] sorted = {1, 2, 3, 4, 5};
        bs.sortOptimized(sorted);
        System.out.println("已有序: " + Arrays.toString(sorted));
    }
}
