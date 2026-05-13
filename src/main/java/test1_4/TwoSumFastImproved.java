package test1_4;

import java.util.Arrays;

public class TwoSumFastImproved {

    // 在有序数组 a 的 [lo, hi] 区间内二分查找 key
    public static int rank(int key, int[] a, int lo, int hi) {
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if      (key < a[mid]) hi = mid - 1;
            else if (key > a[mid]) lo = mid + 1;
            else return mid;
        }
        return -1;
    }

    public static int count(int[] a) {
        Arrays.sort(a);
        int N = a.length;
        int cnt = 0;
        for (int i = 0; i < N; i++) {
            // 只在 i+1 到末尾之间查找 -a[i]
            if (rank(-a[i], a, i+1, N-1) != -1) {
                cnt++;
            }
        }
        return cnt;
    }

    public static void main(String[] args) {
        int[] test1 = {1, -1, 2, 3, -2};
        System.out.println("Test 1: " + count(test1)); // 预期输出: 2

        int[] test2 = {0, 0, 0};
        System.out.println("Test 2: " + count(test2)); // 预期输出: 1

        int[] test3 = {1, 2, 3, 4};
        System.out.println("Test 3: " + count(test3)); // 预期输出: 0
    }
}