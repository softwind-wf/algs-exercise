package exercise1.exercise1_4;

public class twoSumFast {
    public static int twoSumFaster(int[] a) {
        int i = 0, j = a.length - 1;
        int cnt = 0;
        while (i < j) {
            int sum = a[i] + a[j];
            if (sum == 0) {
                cnt++;
                i++;
                j--;
            } else if (sum < 0) {
                i++;
            } else {
                j--;
            }
        }
        return cnt;
    }

    public static int twoSumFasterWithDuplicates(int[] a) {
        int i = 0, j = a.length - 1;
        int cnt = 0;
        while (i < j) {
            int sum = a[i] + a[j];
            if (sum == 0) {
                int leftVal = a[i];
                int rightVal = a[j];

                // 统计左边相同个数
                int cntLeft = 0;
                while (i <= j && a[i] == leftVal) {
                    cntLeft++;
                    i++;
                }
                // 统计右边相同个数
                int cntRight = 0;
                while (j >= i && a[j] == rightVal) {
                    cntRight++;
                    j--;
                }
                cnt += cntLeft * cntRight;
            } else if (sum < 0) {
                i++;
            } else {
                j--;
            }
        }
        return cnt;
    }






}
