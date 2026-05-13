package exercise1.exercise1_1;

import java.util.Arrays;

public class BinarySearch1 {
    // 对外接口，调用递归方法并初始化深度为 0
    public static int rank(int key, int[] a) {
        return rank(key, a, 0, a.length - 1, 0);
    }

    // 递归核心方法，增加 depth 参数跟踪递归深度
    public static int rank(int key, int[] a, int lo, int hi, int depth) {
        // 打印当前调用的参数 lo、hi，并用 depth 控制缩进
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }
        System.out.printf("lo = %d, hi = %d\n", lo, hi);

        // 二分查找核心逻辑
        if (lo > hi) return -1;
        int mid = lo + (hi - lo) / 2;
        if (key < a[mid]) {
            return rank(key, a, lo, mid - 1, depth + 1);
        } else if (key > a[mid]) {
            return rank(key, a, mid + 1, hi, depth + 1);
        } else {
            return mid;
        }
    }

    public static void main(String[] args) {
        // 测试用例：有序数组
        int[] a = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19};
        int key = 7;

        System.out.println("数组：" + Arrays.toString(a));
        System.out.println("查找 key = " + key);
        System.out.println("递归调用跟踪：");

        int result = rank(key, a);

        if (result != -1) {
            System.out.println("找到 key，索引为：" + result);
        } else {
            System.out.println("未找到 key");
        }
    }
}