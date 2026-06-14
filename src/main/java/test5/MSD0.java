package test5;

public class MSD0 {
    private static int R = 26;        // 基数
    private static final int M = 0;   // 小数组的切换阈值
    private static String[] aux;       // 数据分类的辅助数组

    private static int charAt(String s, int d) {
        if (d < s.length()) return s.charAt(d)-'a';
        else return -1;
    }

    public static void sort(String[] a) {
        int N = a.length;
        aux = new String[N];
        sort(a, 0, N - 1, 0);
    }

    private static void sort(String[] a, int lo, int hi, int d) {
        // 以第d个字符为键将a[lo]至a[hi]排序
        if (hi <= lo + M) {
            Insertion.sort(a, lo, hi, d);
            return;
        }

        int[] count = new int[R + 2];
        for (int i = lo; i <= hi; i++)
            count[charAt(a[i], d) + 2]++;              // 计算频率

        for (int r = 0; r < R + 1; r++)
            count[r + 1] += count[r];                    // 将频率转换为索引

        for (int i = lo; i <= hi; i++)
            aux[count[charAt(a[i], d) + 1]++] = a[i];  // 数据分类

        for (int i = lo; i <= hi; i++)
            a[i] = aux[i - lo];                        // 回写

        // 递归地以每个字符为键进行排序
        for (int r = 0; r < R; r++)
            sort(a, lo + count[r], lo + count[r + 1] - 1, d + 1);
    }

    // 配套的插入排序类，和MSD配合使用
    public static class Insertion {
        // 对a[lo..hi]的字符串，从第d个字符开始做插入排序
        public static void sort(String[] a, int lo, int hi, int d) {
            for (int i = lo + 1; i <= hi; i++) {
                for (int j = i; j > lo && less(a[j], a[j - 1], d); j--) {
                    exch(a, j, j - 1);
                }
            }
        }

        // 从第d个字符开始比较两个字符串
        private static boolean less(String v, String w, int d) {
            // 逐个字符比较，直到出现不同字符或其中一个字符串结束
            for (int i = d; i < Math.min(v.length(), w.length()); i++) {
                if (v.charAt(i) < w.charAt(i)) return true;
                if (v.charAt(i) > w.charAt(i)) return false;
            }
            // 前面字符都一样，则短的更小
            return v.length() < w.length();
        }

        // 交换数组中的两个元素
        private static void exch(String[] a, int i, int j) {
            String temp = a[i];
            a[i] = a[j];
            a[j] = temp;
        }
    }

    // 简单测试
    public static void main(String[] args) {
        String[] arr = {"she", "sells", "seashells", "by", "the", "seashore",
                        "the", "shells", "she", "sells", "are", "surely", "seashells"};
        MSD0.sort(arr);
        for (String s : arr) {
            System.out.println(s);
        }
    }
}