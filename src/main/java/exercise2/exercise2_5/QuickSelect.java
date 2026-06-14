package exercise2.exercise2_5;

import java.util.Random;

public class QuickSelect {

    // 交换数组中两个元素的位置
    private static void exch(Comparable[] a, int i, int j) {
        Comparable temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    // 快速排序的切分方法
    private static int partition(Comparable[] a, int lo, int hi) {
        int i = lo;
        int j = hi + 1;
        Comparable v = a[lo]; // 切分元素

        while (true) {
            // 从左向右找 >= v 的元素
            while (a[++i].compareTo(v) < 0) {
                if (i == hi) break;
            }
            // 从右向左找 <= v 的元素
            while (v.compareTo(a[--j]) < 0) {
                if (j == lo) break;
            }
            // 指针相遇，切分结束
            if (i >= j) break;
            exch(a, i, j);
        }
        // 将切分元素放到正确位置
        exch(a, lo, j);
        return j;
    }

    // 随机打乱数组，防止最坏情况
    private static void shuffle(Comparable[] a) {
        Random random = new Random();
        for (int i = 0; i < a.length; i++) {
            int r = i + random.nextInt(a.length - i);
            exch(a, i, r);
        }
    }

    // 核心方法：找到数组中第k小的元素（k从0开始）
    public static Comparable select(Comparable[] a, int k) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("数组不能为空");
        }
        if (k < 0 || k >= a.length) {
            throw new IllegalArgumentException("k 超出数组范围");
        }

        // 复制原数组，避免修改输入
        Comparable[] copy = a.clone();
        shuffle(copy); // 随机打乱，防止最坏情况

        int lo = 0;
        int hi = copy.length - 1;
        while (lo < hi) {
            int j = partition(copy, lo, hi);
            if (j == k) {
                return copy[j];
            } else if (j > k) {
                hi = j - 1; // 第k小元素在左子数组
            } else {
                lo = j + 1; // 第k小元素在右子数组
            }
        }
        return copy[lo];
    }

    // 测试
    public static void main(String[] args) {
        Integer[] arr = {3, 1, 4, 2, 1, 5, 9, 2, 6};
        int k = 4; // 找第3小的元素（0开始计数，即第4小的数）
        Comparable result = select(arr, k);
        System.out.println("数组中第" + k + "小的元素是: " + result);
        // 预期输出: 3（排序后数组: [1, 1, 2, 2, 3, 4, 5, 6, 9]）
    }
}