package exercise1.exercise1_4;

import java.util.Arrays;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class EqualPairsFast {
    public static int countEqualPairs(int[] a) {
        Arrays.sort(a);  // O(N log N)
        int cnt = 0;
        int i = 0;
        int N = a.length;
        
        while (i < N) {
            int j = i + 1;
            while (j < N && a[j] == a[i]) {
                j++;
            }
            int k = j - i;  // 相同值的个数
            cnt += k * (k - 1) / 2;
            i = j;
        }
        return cnt;
    }

    public static void main(String[] args) throws FileNotFoundException {
        Scanner in = new Scanner(new File(args[0]));
        int[] a = new int[in.nextInt()];
        for (int i = 0; i < a.length; i++) {
            a[i] = in.nextInt();
        }
        in.close();
        System.out.println(countEqualPairs(a));
    }
}