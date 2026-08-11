package cn.exercise.algs4.datastructure.search;

/**
 * 斐波那契查找：在有序数组中使用斐波那契数列划分区间。
 * 优势：只涉及加减运算，无需乘除；分割比趋近黄金比例 0.618。
 * 平均复杂度 O(log n)，最坏情况比二分查找稍差。
 */
public class FibonacciSearch {

    /**
     * 迭代版斐波那契查找
     * @param a      有序数组（升序）
     * @param target 要查找的目标值
     * @return 目标值的索引，若不存在返回 -1
     */
    public int search(int[] a, int target) {
        int n = a.length;

        // 1. 找到最小的斐波那契数 fibM >= n+1
        int fibMm2 = 0;   // F(k-2)
        int fibMm1 = 1;   // F(k-1)
        int fibM = 1;     // F(k)

        while (fibM < n + 1) {
            fibMm2 = fibMm1;
            fibMm1 = fibM;
            fibM = fibMm2 + fibMm1;
        }

        // 2. 标记被淘汰的左半部分末尾
        int offset = -1;

        // 3. 当还有元素可查时
        while (fibM > 1) {
            // 检查位置：offset + F(k-2)
            int i = Math.min(offset + fibMm2, n - 1);

            if (a[i] < target) {
                // 目标在右边，砍掉 F(k-2) 个元素
                // 剩余区间大小变为 F(k-1)，k = k-1
                fibM = fibMm1;
                fibMm1 = fibMm2;
                fibMm2 = fibM - fibMm1;
                offset = i;
            } else if (a[i] > target) {
                // 目标在左边，砍掉 F(k-1) 个元素
                // 剩余区间大小变为 F(k-2)，k = k-2
                fibM = fibMm2;
                fibMm1 = fibMm1 - fibMm2;
                fibMm2 = fibM - fibMm1;
            } else {
                return i;
            }
        }

        // 4. 还剩一个元素：F(k-1) == 1 时检查 offset+1
        if (fibMm1 == 1 && offset + 1 < n && a[offset + 1] == target) {
            return offset + 1;
        }

        return -1;
    }

    /**
     * 递归版斐波那契查找
     */
    public int searchRecursive(int[] a, int target) {
        int n = a.length;

        // 构造斐波那契数列，找到 F(k) >= n
        int fibMm2 = 0, fibMm1 = 1, fibM = 1;
        while (fibM < n) {
            fibMm2 = fibMm1;
            fibMm1 = fibM;
            fibM = fibMm2 + fibMm1;
        }

        return fibSearch(a, target, -1, n - 1, fibM, fibMm1, fibMm2);
    }

    private int fibSearch(int[] a, int target, int offset, int hi,
                          int fibM, int fibMm1, int fibMm2) {
        if (fibM <= 0) {
            return -1;
        }

        // 还剩一个元素
        if (fibM == 1) {
            int i = Math.min(offset + 1, hi);
            return a[i] == target ? i : -1;
        }

        int i = Math.min(offset + fibMm2, a.length - 1);

        if (a[i] < target) {
            // 去右边：区间缩小为 F(k-1)，k = k-1
            return fibSearch(a, target, i, hi, fibMm1, fibMm2, fibMm1 - fibMm2);
        } else if (a[i] > target) {
            // 去左边：区间缩小为 F(k-2)，k = k-2
            return fibSearch(a, target, offset, i - 1, fibMm2, fibMm1 - fibMm2, fibMm2 - (fibMm1 - fibMm2));
        } else {
            return i;
        }
    }

    public static void main(String[] args) {
        FibonacciSearch fs = new FibonacciSearch();

        int[] arr = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

        System.out.println("=== 迭代版 ===");
        System.out.println("找 70 的索引: " + fs.search(arr, 70));   // 6
        System.out.println("找 10 的索引: " + fs.search(arr, 10));   // 0
        System.out.println("找 100 的索引: " + fs.search(arr, 100)); // 9
        System.out.println("找 55 的索引: " + fs.search(arr, 55));   // -1
        System.out.println("找 20 的索引: " + fs.search(arr, 20));   // 1

        System.out.println("\n=== 递归版 ===");
        System.out.println("找 70 的索引: " + fs.searchRecursive(arr, 70));   // 6
        System.out.println("找 55 的索引: " + fs.searchRecursive(arr, 55));   // -1
        System.out.println("找 100 的索引: " + fs.searchRecursive(arr, 100)); // 9

        // 边界测试
        int[] single = {42};
        System.out.println("\n=== 单元素数组 ===");
        System.out.println("找 42: " + fs.search(single, 42));  // 0
        System.out.println("找 99: " + fs.search(single, 99));  // -1
    }
}
