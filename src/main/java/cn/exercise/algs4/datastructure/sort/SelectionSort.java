
package cn.exercise.algs4.datastructure.sort;

import java.util.Arrays;

/**
 * 选择排序：每轮从未排序部分选择最小元素，放到已排序部分的末尾。
 * 不稳定排序，原地排序，时间复杂度 O(n²)。
 */
public class SelectionSort {

    /**
     * 基础版选择排序
     * 每轮找到最小元素，与当前位置交换。
     */
    public void sort(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            // 第 i 轮：在 [i, n-1] 中找到最小值的索引
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (a[j] < a[minIdx]) {
                    minIdx = j;
                }
            }
            // 将最小值交换到位置 i
            swap(a, i, minIdx);
        }
    }

    /**
     * 优化版：同时找最小值和最大值
     * 减少一半的遍历次数。
     */
    public void sortOptimized(int[] a) {
        int n = a.length;
        int left = 0;
        int right = n - 1;

        while (left < right) {
            int minIdx = left;
            int maxIdx = right;

            // 在 [left, right] 中同时找最小和最大
            for (int i = left; i <= right; i++) {
                if (a[i] < a[minIdx]) {
                    minIdx = i;
                }
                if (a[i] > a[maxIdx]) {
                    maxIdx = i;
                }
            }

            // 将最小值交换到左边
            swap(a, left, minIdx);

            // 如果最大值在左边（已被交换），更新 maxIdx
            if (maxIdx == left) {
                maxIdx = minIdx;
            }

            // 将最大值交换到右边
            swap(a, right, maxIdx);

            left++;
            right--;
        }
    }

    private void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void main(String[] args) {
        SelectionSort ss = new SelectionSort();

        int[] arr1 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        ss.sort(arr1);
        System.out.println("基础版: " + Arrays.toString(arr1));

        int[] arr2 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        ss.sortOptimized(arr2);
        System.out.println("优化版: " + Arrays.toString(arr2));

        // 边界测试
        int[] empty = {};
        ss.sort(empty);
        System.out.println("空数组: " + Arrays.toString(empty));

        int[] single = {42};
        ss.sort(single);
        System.out.println("单元素: " + Arrays.toString(single));

        int[] sorted = {1, 2, 3, 4, 5};
        ss.sort(sorted);
        System.out.println("已有序: " + Arrays.toString(sorted));
    }
}