package exercise1.exercise1_1;

import java.util.Arrays;

public class BinarySearch3 {

    // 二分查找（递归实现）
    public static int rank(int key, int[] a) {
        return rank(key, a, 0, a.length - 1);
    }

    private static int rank(int key, int[] a, int lo, int hi) {
        if (lo > hi) return -1;
        int mid = lo + (hi - lo) / 2;
        if (key < a[mid]) {
            return rank(key, a, lo, mid - 1);
        } else if (key > a[mid]) {
            return rank(key, a, mid + 1, hi);
        } else {
            return mid;
        }
    }

    // 去重方法：删除排序数组中的重复元素
    public static int[] removeDuplicates(int[] sortedArray) {
        if (sortedArray == null || sortedArray.length == 0) {
            return new int[0];
        }

        // 用一个指针记录不重复元素的位置
        int uniqueIndex = 0;
        for (int i = 1; i < sortedArray.length; i++) {
            if (sortedArray[i] != sortedArray[uniqueIndex]) {
                uniqueIndex++;
                sortedArray[uniqueIndex] = sortedArray[i];
            }
        }

        // 截取不重复的部分
        return Arrays.copyOf(sortedArray, uniqueIndex + 1);
    }

    public static void main(String[] args) {
        // 原始白名单（已排序，但有重复）
        int[] whitelist = {1, 3, 3, 5, 7, 7, 7, 9, 11, 11, 13};
        System.out.println("原始白名单：" + Arrays.toString(whitelist));

        // 去重
        int[] uniqueWhitelist = removeDuplicates(whitelist);
        System.out.println("去重后白名单：" + Arrays.toString(uniqueWhitelist));

        // 测试二分查找
        int key = 7;
        int index = rank(key, uniqueWhitelist);
        if (index != -1) {
            System.out.println("找到 key = " + key + "，索引为：" + index);
        } else {
            System.out.println("未找到 key = " + key);
        }
    }
}