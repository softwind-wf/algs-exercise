package test5;

public class MSD {
    private static int R = 26;        // 基数（保留，但未使用）
    private static final int M = 0;   // 小数组的切换阈值
    private static String[] aux;       // 数据分类的辅助数组

    private static int charAt(String s, int d) {
        if (d < s.length()) return s.charAt(d) - 'a';
        else return -1;
    }

    public static void sort(String[] a) {
        int N = a.length;
        aux = new String[N];
        sort(a, 0, N - 1, 0);
    }

    // 按第 d 个字符对 a[lo..hi] 进行排序（使用归并排序 + 递归分组）
    private static void sort(String[] a, int lo, int hi, int d) {
        // 小数组使用插入排序
        if (hi <= lo + M) {
            Insertion.sort(a, lo, hi, d);
            return;
        }

        // 按第 d 个字符对 a[lo..hi] 进行归并排序
        mergeSortByD(a, lo, hi, d);

        // 扫描分组，对每个相同字符的组递归排序下一个字符
        int start = lo;
        int curChar = charAt(a[start], d);
        for (int i = lo + 1; i <= hi; i++) {
            int nextChar = charAt(a[i], d);
            if (nextChar != curChar) {
                // 当前组为 [start, i-1]，若字符不是 -1 则递归排序下一层
                if (curChar != -1) {
                    sort(a, start, i - 1, d + 1);
                }
                start = i;
                curChar = nextChar;
            }
        }
        // 处理最后一组
        if (curChar != -1) {
            sort(a, start, hi, d + 1);
        }
    }

    // 归并排序（基于第 d 个字符）
    private static void mergeSortByD(String[] a, int lo, int hi, int d) {
        if (hi <= lo) return;
        int mid = lo + (hi - lo) / 2;
        mergeSortByD(a, lo, mid, d);
        mergeSortByD(a, mid + 1, hi, d);
        merge(a, lo, mid, hi, d);
    }

    // 归并两个已排序的子数组（基于第 d 个字符）
    private static void merge(String[] a, int lo, int mid, int hi, int d) {
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
        }
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a[k] = aux[j++];
            else if (j > hi) a[k] = aux[i++];
            else if (lessByChar(aux[i], aux[j], d)) a[k] = aux[i++];
            else a[k] = aux[j++];
        }
    }

    // 比较两个字符串的第 d 个字符（返回 v 的字符是否小于 w 的字符）
    private static boolean lessByChar(String v, String w, int d) {
        int cv = charAt(v, d);
        int cw = charAt(w, d);
        return cv < cw;
    }

    // 配套的插入排序类，和 MSD 配合使用
    public static class Insertion {
        // 对 a[lo..hi] 的字符串，从第 d 个字符开始做插入排序
        public static void sort(String[] a, int lo, int hi, int d) {
            for (int i = lo + 1; i <= hi; i++) {
                for (int j = i; j > lo && less(a[j], a[j - 1], d); j--) {
                    exch(a, j, j - 1);
                }
            }
        }

        // 从第 d 个字符开始比较两个字符串
        private static boolean less(String v, String w, int d) {
            for (int i = d; i < Math.min(v.length(), w.length()); i++) {
                if (v.charAt(i) < w.charAt(i)) return true;
                if (v.charAt(i) > w.charAt(i)) return false;
            }
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
        MSD.sort(arr);
        for (String s : arr) {
            System.out.println(s);
        }
    }
}