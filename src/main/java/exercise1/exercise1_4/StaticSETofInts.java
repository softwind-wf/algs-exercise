package exercise1.exercise1_4;

import java.util.Arrays;

public class StaticSETofInts {
    private final int[] a;

    // 构造函数：对传入的数组进行排序
    public StaticSETofInts(int[] keys) {
        // 保护性拷贝，防止外部修改内部数组
        a = new int[keys.length];
        System.arraycopy(keys, 0, a, 0, keys.length);
        // 对数组进行排序，为二分查找做准备
        Arrays.sort(a);
    }

    // 使用二分查找判断 key 是否在集合中
    public boolean contains(int key) {
        return rank(key) != -1;
    }

    // 二分查找的核心实现
    private int rank(int key) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2; // 避免整数溢出
            if (key < a[mid]) {
                hi = mid - 1;
            } else if (key > a[mid]) {
                lo = mid + 1;
            } else {
                return mid; // 找到 key，返回其索引
            }
        }
        return -1; // 未找到 key
    }

    // 测试程序
    public static void main(String[] args) {
        int[] testData = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5,4,5,6,8,9};
        StaticSETofInts set = new StaticSETofInts(testData);

        // 测试包含的元素
        System.out.println("set.contains(3): " + set.contains(3)); // 应该返回 true
        System.out.println("set.contains(9): " + set.contains(9)); // 应该返回 true

        // 测试不包含的元素
        System.out.println("set.contains(0): " + set.contains(0)); // 应该返回 false
        System.out.println("set.contains(7): " + set.contains(7)); // 应该返回 false
    }
}