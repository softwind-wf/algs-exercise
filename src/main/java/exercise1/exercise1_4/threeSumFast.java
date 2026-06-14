package exercise1.exercise1_4;

import java.util.Arrays;

public class threeSumFast {


    public static int threeSumFaster(int[] a) {
        Arrays.sort(a);  // 确保排序
        int N = a.length;
        int cnt = 0;

        for (int i = 0; i < N - 2; i++) {
            // 跳过重复的 i，避免重复统计（不跳也不影响正确性，但会多算）
            if (i > 0 && a[i] == a[i-1]) continue;

            int j = i + 1;
            int k = N - 1;
            int target = -a[i];

            while (j < k) {
                int sum = a[j] + a[k];
                if (sum == target) {
                    // 统计相同值
                    if (a[j] == a[k]) {
                        // 所有 j..k 都相等，组合数 C(len, 2)
                        int len = k - j + 1;
                        cnt += len * (len - 1) / 2;
                        break;
                    } else {
                        int leftVal = a[j];
                        int rightVal = a[k];
                        int cntLeft = 0, cntRight = 0;

                        while (j <= k && a[j] == leftVal) {
                            cntLeft++;
                            j++;
                        }
                        while (j <= k && a[k] == rightVal) {
                            cntRight++;
                            k--;
                        }
                        cnt += cntLeft * cntRight;
                    }
                } else if (sum < target) {
                    j++;
                } else {
                    k--;
                }
            }
        }
        return cnt;
    }
}
