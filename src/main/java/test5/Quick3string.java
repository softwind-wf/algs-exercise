package test5;

import java.util.Objects;

/**
 * 三向字符串快速排序（3-way string quicksort）。
 * <p>
 * 该算法是快速排序的变种，适用于对字符串数组进行字典序排序。它通过将字符串按当前字符分为小于、等于、
 * 大于三组，并递归地对等于组的下一个字符以及小于/大于组继续排序。当子数组长度小于阈值时自动切换到插入排序，
 * 以平衡递归开销和常数因子。
 * </p>
 * <p>
 * 时间复杂度：平均 O(N log N)，最坏 O(N²)（但实际发生概率极低），空间复杂度 O(log N)（递归栈）。
 * </p>
 * <p>
 * 注意：输入数组及其元素均不允许为 {@code null}，否则将抛出 {@link NullPointerException}。
 * </p>
 *
 * @author Your Name
 * @version 1.0
 * @see <a href="https://en.wikipedia.org/wiki/Quicksort#Three-way_radix_quicksort">
 * Three-way radix quicksort</a>
 */
public final class Quick3string {

    // 切换为插入排序的阈值（原书建议 10~15）
    private static final int INSERTION_THRESHOLD = 15;

    // 禁止实例化工具类
    private Quick3string() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 对字符串数组进行字典序排序（原地、不稳定）。
     *
     * @param a 待排序的字符串数组，不能为 {@code null}，且所有元素不能为 {@code null}
     * @throws NullPointerException 如果数组为 {@code null}，或任何元素为 {@code null}
     */
    public static void sort(String[] a) {
        Objects.requireNonNull(a, "Input array cannot be null");
        // 提前检查所有元素非 null，避免排序过程中抛出 NPE 导致不一致状态
        for (int i = 0; i < a.length; i++) {
            Objects.requireNonNull(a[i], "Array element at index " + i + " is null");
        }
        sort(a, 0, a.length - 1, 0);
    }

    /**
     * 递归排序闭区间 [lo, hi] 内的字符串，从第 d 个字符开始比较。
     *
     * @param a  字符串数组（元素非空）
     * @param lo 左边界（包含）
     * @param hi 右边界（包含）
     * @param d  比较的起始字符索引（从 0 开始）
     */
    private static void sort(String[] a, int lo, int hi, int d) {
        // 小数组切换为插入排序，避免递归开销
        if (hi - lo + 1 <= INSERTION_THRESHOLD) {
            insertionSort(a, lo, hi, d);
            return;
        }

        int lt = lo;          // 小于基准字符的右边界
        int gt = hi;          // 大于基准字符的左边界
        int v = charAt(a[lo], d); // 基准字符（-1 表示字符串已结束）
        int i = lo + 1;

        while (i <= gt) {
            int t = charAt(a[i], d);
            if (t < v) {
                exch(a, lt++, i++);
            } else if (t > v) {
                exch(a, i, gt--);
            } else {
                i++;
            }
        }

        // 递归处理三个分区
        sort(a, lo, lt - 1, d);          // 小于基准字符的部分
        if (v >= 0) {                    // 基准字符不是终止符（-1）
            sort(a, lt, gt, d + 1);      // 等于基准字符的部分，继续比较下一位
        }
        sort(a, gt + 1, hi, d);          // 大于基准字符的部分
    }

    /**
     * 对子数组 a[lo..hi] 从第 d 位开始执行插入排序。
     *
     * @param a  字符串数组（元素非空）
     * @param lo 左边界（包含）
     * @param hi 右边界（包含）
     * @param d  比较起始字符索引
     */
    private static void insertionSort(String[] a, int lo, int hi, int d) {
        for (int i = lo; i <= hi; i++) {
            for (int j = i; j > lo && compareFrom(a[j], a[j - 1], d) < 0; j--) {
                exch(a, j, j - 1);
            }
        }
    }

    /**
     * 比较两个字符串从第 d 个字符开始的字典序。
     *
     * @param v 第一个字符串（非空）
     * @param w 第二个字符串（非空）
     * @param d 起始字符索引（从 0 开始）
     * @return 负数表示 v < w，0 表示相等，正数表示 v > w
     */
    private static int compareFrom(String v, String w, int d) {
        int vLen = v.length();
        int wLen = w.length();
        int minLen = Math.min(vLen, wLen);
        // 逐个字符比较，直到到达起始位置 d 或任一字符串结束
        for (int i = d; i < minLen; i++) {
            char c1 = v.charAt(i);
            char c2 = w.charAt(i);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        // 前缀相同，较短的字符串视为更小
        return vLen - wLen;
    }

    /**
     * 安全获取字符串第 d 位的字符编码（超出长度返回 -1，表示字符串结束）。
     *
     * @param s 非空字符串
     * @param d 字符索引
     * @return 字符的 Unicode 码点，若超出长度则返回 -1
     */
    private static int charAt(String s, int d) {
        return d < s.length() ? s.charAt(d) : -1;
    }

    /**
     * 交换数组中两个位置的元素。
     *
     * @param a 数组
     * @param i 索引 i
     * @param j 索引 j
     */
    private static void exch(String[] a, int i, int j) {
        String temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    /**
     * 简单测试用例。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        String[] a = {
            "she", "sells", "seashells", "by", "the", "sea", "shore",
            "the", "shells", "she", "sells", "are", "surely", "seashells"
        };
        sort(a);
        for (String s : a) {
            System.out.println(s);
        }

        // 额外测试：空数组、单元素、重复元素
        String[] empty = {};
        sort(empty); // 无输出，不应抛异常

        String[] single = {"hello"};
        sort(single);
        assert single[0].equals("hello");

        String[] duplicates = {"a", "a", "a"};
        sort(duplicates);
        System.out.println("\nDuplicates sorted:");
        for (String s : duplicates) {
            System.out.println(s);
        }
    }
}