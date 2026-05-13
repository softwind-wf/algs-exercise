package exercise1.exercise1_4;

import java.util.Arrays;

public class StaticSETofInts1 {
    private int[] a;

    // 构造函数：排序并复制数组
    public StaticSETofInts1(int[] keys) {
        a = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            a[i] = keys[i]; // 防御性复制
        }
        Arrays.sort(a);
    }

    // 判断键是否存在（原来的方法）
    public boolean contains(int key) {
        return rank(key) != -1;
    }

    // 标准二分查找（任意位置）
    private int rank(int key) {
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

    // 新增：返回键出现的次数，O(log N)
    public int howMany(int key) {
        int first = rankLeftmost(key);
        if (first == -1) return 0;
        int last = rankRightmost(key);
        return last - first + 1;
    }

    // 二分查找最左边的匹配索引
    private int rankLeftmost(int key) {
        int lo = 0;
        int hi = a.length - 1;
        int result = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (key < a[mid]) {
                hi = mid - 1;
            } else if (key > a[mid]) {
                lo = mid + 1;
            } else {
                result = mid;
                hi = mid - 1; // 继续向左
            }
        }
        return result;
    }

    // 二分查找最右边的匹配索引
    private int rankRightmost(int key) {
        int lo = 0;
        int hi = a.length - 1;
        int result = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (key < a[mid]) {
                hi = mid - 1;
            } else if (key > a[mid]) {
                lo = mid + 1;
            } else {
                result = mid;
                lo = mid + 1; // 继续向右
            }
        }
        return result;
    }

    // 简单测试
    public static void main(String[] args) {
        int[] keys = {1, 2, 2, 2, 3, 4, 5, 5, 6, 6, 6, 6};
        StaticSETofInts1 set = new StaticSETofInts1(keys);
        
        System.out.println(set.howMany(2));  // 输出 3
        System.out.println(set.howMany(5));  // 输出 2
        System.out.println(set.howMany(6));  // 输出 4
        System.out.println(set.howMany(7));  // 输出 0
    }
}