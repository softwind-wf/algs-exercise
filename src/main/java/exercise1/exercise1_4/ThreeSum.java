package exercise1.exercise1_4;

import edu.princeton.cs.algs4.StdOut;

public class ThreeSum {
    public static int count(int[] a) {
        // 统计和为0的三元组的数量，避免溢出
        int N = a.length;
        int cnt = 0;
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                for (int k = j + 1; k < N; k++) {
                    long sum = (long) a[i] + (long) a[j] + (long) a[k];
                    if (sum == 0) {
                        cnt++;
                    }
                }
            }
        }
        return cnt;
    }

    public static void main(String[] args) {
        int[] a = new int[]{3, 2, 4, 5, 6};
        StdOut.println(count(a));  // 应该输出 0，因为没有三个数和为 0
    }
}