package test;

import java.util.Arrays;

public class StaticSETofInts {
    private int[] a;

    // 构造函数：接收一个数组，进行保护性复制并排序
    public StaticSETofInts(int[] keys) {
        // 保护性复制，避免外部修改内部数组
        a = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            a[i] = keys[i];
        }
        // 排序数组，为二分查找做准备
        Arrays.sort(a);
    }

    // 判断 key 是否存在于集合中
    public boolean contains(int key) {
        return rank(key) != -1;
    }

    // 二分查找核心实现
    private int rank(int key) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (key < a[mid]) {
                hi = mid - 1;
            } else if (key > a[mid]) {
                lo = mid + 1;
            } else {
                return mid; // 找到，返回索引
            }
        }
        return -1; // 未找到
    }
}