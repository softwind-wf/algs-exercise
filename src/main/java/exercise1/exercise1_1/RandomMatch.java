package exercise1.exercise1_1;

import java.util.Arrays;
import java.util.Random;

public class RandomMatch {

    // 二分查找
    public static int binarySearch(int key, int[] a) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (key < a[mid]) hi = mid - 1;
            else if (key > a[mid]) lo = mid + 1;
            else return mid;
        }
        return -1;
    }

    // 生成大小为 N 的随机 6 位正整数数组
    public static int[] generateRandomArray(int N) {
        Random random = new Random();
        int[] arr = new int[N];
        for (int i = 0; i < N; i++) {
            // 生成 100000 到 999999 之间的随机整数
            arr[i] = 100000 + random.nextInt(900000);
        }
        return arr;
    }

    // 计算两个数组的交集数量
    public static int countCommonElements(int[] a, int[] b) {
        Arrays.sort(a);
        int count = 0;
        for (int num : b) {
            if (binarySearch(num, a) != -1) {
                count++;
            }
        }
        return count;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("请输入实验次数 T");
            return;
        }
        int T = Integer.parseInt(args[0]);
        int[] NValues = {1000, 10000, 100000, 1000000};

        // 打印表头
        System.out.printf("%8s %15s\n", "N", "平均交集数量");

        for (int N : NValues) {
            long total = 0;
            for (int t = 0; t < T; t++) {
                int[] a = generateRandomArray(N);
                int[] b = generateRandomArray(N);
                total += countCommonElements(a, b);
            }
            double average = (double) total / T;
            System.out.printf("%8d %15.2f\n", N, average);
        }
    }
}