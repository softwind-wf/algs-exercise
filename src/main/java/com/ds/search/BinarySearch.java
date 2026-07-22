package com.ds.search;

/**
 * 二分查找：在有序数组中查找目标值。
 * 同时提供递归和迭代两种实现。
 */
public class BinarySearch {

    /**
     * 递归版二分查找
     * @param a      有序数组（升序）
     * @param target 要查找的目标值
     * @param lo     查找区间的左边界（含）
     * @param hi     查找区间的右边界（含）
     * @return 目标值的索引，若不存在返回 -1
     */
    public int searchRecursive(int[] a, int target, int lo, int hi) {
        if (lo > hi) {
            return -1;
        }
        int mid = lo + (hi - lo) / 2;  // 防止溢出，等价于 (lo + hi) / 2
        if (target < a[mid]) {
            return searchRecursive(a, target, lo, mid - 1);
        } else if (target > a[mid]) {
            return searchRecursive(a, target, mid + 1, hi);
        } else {
            return mid;
        }
    }

    /**
     * 递归版二分查找（便捷入口）
     */
    public int searchRecursive(int[] a, int target) {
        return searchRecursive(a, target, 0, a.length - 1);
    }

    /**
     * 迭代版二分查找
     * @param a      有序数组（升序）
     * @param target 要查找的目标值
     * @return 目标值的索引，若不存在返回 -1
     */
    public int searchIterative(int[] a, int target) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (target < a[mid]) {
                hi = mid - 1;
            } else if (target > a[mid]) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    /**
     * 查找第一个等于 target 的位置（左边界二分查找）
     * 当数组中存在重复元素时，返回最左边的那个。
     */
    public int searchLeftmost(int[] a, int target) {
        int lo = 0;
        int hi = a.length - 1;
        int result = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) {
                result = mid;
                hi = mid - 1;  // 继续往左找
            } else if (a[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        BinarySearch bs = new BinarySearch();

        int[] arr = {1, 3, 5, 7, 9, 11, 13, 15};

        System.out.println("=== 递归版 ===");
        System.out.println("找 7 的索引: " + bs.searchRecursive(arr, 7));   // 3
        System.out.println("找 1 的索引: " + bs.searchRecursive(arr, 1));   // 0
        System.out.println("找 15 的索引: " + bs.searchRecursive(arr, 15)); // 7
        System.out.println("找 6 的索引: " + bs.searchRecursive(arr, 6));   // -1

        System.out.println("\n=== 迭代版 ===");
        System.out.println("找 7 的索引: " + bs.searchIterative(arr, 7));   // 3
        System.out.println("找 6 的索引: " + bs.searchIterative(arr, 6));   // -1
        System.out.println(bs.searchLeftmost(arr,7));

        int[] arr2 = {1, 2, 2, 2, 3, 4, 5};
        System.out.println("\n=== 左边界版（有重复元素） ===");
        System.out.println("找第一个 2 的索引: " + bs.searchLeftmost(arr2, 2)); // 1
    }
}
