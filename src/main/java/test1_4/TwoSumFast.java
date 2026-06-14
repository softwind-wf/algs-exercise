package test1_4;

import java.util.Arrays;

public class TwoSumFast {

    // 二分查找：在有序数组 a 中查找 key，返回其索引，未找到则返回 -1
    public static int rank(int key, int[] a) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if      (key < a[mid]) hi = mid - 1;
            else if (key > a[mid]) lo = mid + 1;
            else return mid;
        }
        return -1;
    }

    // 计算和为 0 的整数对数目
    public static int count(int[] a) {
        Arrays.sort(a); // 先对数组进行排序
        int N = a.length;
        int cnt = 0;
        for (int i = 0; i < N; i++) {
            // 查找 -a[i] 是否存在，且其索引 j > i，避免重复计数
            if (rank(-a[i], a) > i) {
                cnt++;
            }
        }
        return cnt;
    }

    public static void main(String[] args) {
        // 测试用例 1
        int[] test1 = {1, -1, 2, 3, -2};
        System.out.println("Test 1: " + count(test1)); // 预期输出: 2 (1和-1, 2和-2)

        // 测试用例 2
        int[] test2 = {0, 0, 0};
        System.out.println("Test 2: " + count(test2)); // 预期输出: 1 (注意：此算法对重复元素处理有限)

        // 测试用例 3
        int[] test3 = {1, 2, 3, 4};
        System.out.println("Test 3: " + count(test3)); // 预期输出: 0
    }
}