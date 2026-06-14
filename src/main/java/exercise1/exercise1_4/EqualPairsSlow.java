package exercise1.exercise1_4;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class EqualPairsSlow {
    public static int countEqualPairs(int[] a) {
        int N = a.length;
        int cnt = 0;
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                if (a[i] == a[j]) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    public static void main(String[] args) throws FileNotFoundException {
        Scanner in = new Scanner(new File(args[0]));
        int[] a = new int[in.nextInt()]; // 假设文件第一行是数组长度
        for (int i = 0; i < a.length; i++) {
            a[i] = in.nextInt();
        }
        in.close();
        System.out.println(countEqualPairs(a));
    }
}