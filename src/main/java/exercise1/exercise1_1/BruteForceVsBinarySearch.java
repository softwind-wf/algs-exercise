package exercise1.exercise1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class BruteForceVsBinarySearch {

    // 暴力查找
    public static int bruteForceSearch(int key, int[] a) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == key) {
                return i;
            }
        }
        return -1;
    }

    // 二分查找
    public static int binarySearch(int key, int[] a) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (key < a[mid]) {
                hi = mid - 1;
            } else if (key > a[mid]) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    // 从文件读取整数数组
    public static int[] readInts(String filename) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(filename));
        ArrayList<Integer> list = new ArrayList<>();
        while (scanner.hasNextInt()) {
            list.add(scanner.nextInt());
        }
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = list.get(i);
        }
        scanner.close();
        return a;
    }

    public static void main(String[] args) throws FileNotFoundException {
        // 读取数据
        int[] whiteList = readInts("largeW.txt");
        int[] testList = readInts("largeT.txt");

        // 测试暴力查找
        long startTime = System.currentTimeMillis();
        int bruteCount = 0;
        for (int key : testList) {
            if (bruteForceSearch(key, whiteList) != -1) {
                bruteCount++;
            }
        }
        long bruteTime = System.currentTimeMillis() - startTime;

        // 测试二分查找
        startTime = System.currentTimeMillis();
        int binaryCount = 0;
        for (int key : testList) {
            if (binarySearch(key, whiteList) != -1) {
                binaryCount++;
            }
        }
        long binaryTime = System.currentTimeMillis() - startTime;

        // 输出结果
        System.out.println("暴力查找找到 " + bruteCount + " 个匹配项，耗时 " + bruteTime + " ms");
        System.out.println("二分查找找到 " + binaryCount + " 个匹配项，耗时 " + binaryTime + " ms");
    }
}