package exercise1.exercise1_1;

import java.util.Arrays;
import java.util.Scanner;

public class BinarySearch2 {

    // 二分查找的递归实现（带深度跟踪）
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

    public static void main(String[] args) {
        // 1. 读取白名单数组
        Scanner scanner = new Scanner(System.in);
        int[] whitelist = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19};
        Arrays.sort(whitelist);

        // 2. 解析命令行参数（+ 或 -）
        if (args.length != 1) {
            System.err.println("用法：java BinarySearch [+|-]");
            System.exit(1);
        }
        String option = args[0];

        // 3. 处理输入并根据参数输出结果
        System.out.println("请输入要检查的数字（每行一个，输入空行结束）：");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) break;
            int key = Integer.parseInt(line);
            int index = rank(key, whitelist);

            if ("+".equals(option)) {
                // +：打印不在白名单中的值
                if (index == -1) {
                    System.out.println(key);
                }
            } else if ("-".equals(option)) {
                // -：打印在白名单中的值
                if (index != -1) {
                    System.out.println(key);
                }
            } else {
                System.err.println("无效参数：请使用 + 或 -");
                System.exit(1);
            }
        }
        scanner.close();
    }
}