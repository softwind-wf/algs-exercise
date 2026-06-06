package test5;

import java.util.ArrayList;
import java.util.List;

/**
 * 正确的完整 Boyer-Moore 字符串匹配算法
 */
public final class CorrectBoyerMoore {
    private final String pattern;
    private final int[] badChar;   // 坏字符表
    private final int[] goodSuffix; // 好后缀表（移动距离）

    public CorrectBoyerMoore(String pattern) {
        this.pattern = pattern;
        int m = pattern.length();
        // 坏字符表
        badChar = new int[256];
        for (int i = 0; i < 256; i++) badChar[i] = -1;
        for (int i = 0; i < m; i++) badChar[pattern.charAt(i)] = i;
        
        if (m == 0) {
            goodSuffix = new int[1];
            goodSuffix[0] = 0;
        } else {
            goodSuffix = new int[m + 1];
            buildGoodSuffix();
        }
    }

    /** 构建好后缀移动表 */
    private void buildGoodSuffix() {
        int m = pattern.length();
        // 1. 计算 suffix 数组：suffix[i] = 以 i 为结尾的最长后缀子串的长度（该子串也是模式串的后缀）
        int[] suffix = new int[m];
        suffix[m - 1] = m;
        for (int i = m - 2; i >= 0; i--) {
            int j = i;
            while (j >= 0 && pattern.charAt(j) == pattern.charAt(m - 1 - (i - j))) {
                j--;
            }
            suffix[i] = i - j;
        }

        for (int i = 0; i <= m; i++) {
            goodSuffix[i] = m;
        }

        int lastPrefixPosition = m;
        for (int i = m - 1; i >= 0; i--) {
            if (suffix[i] == i + 1) {
                lastPrefixPosition = i + 1;
            }
            goodSuffix[i] = lastPrefixPosition + (m - 1 - i);
        }

        for (int i = 0; i < m - 1; i++) {
            int slen = suffix[i];
            goodSuffix[m - slen] = m - 1 - i;
        }
    }

    /** 返回首次匹配下标，未找到返回 -1 */
    public int search(String text) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return 0;
        if (n < m) return -1;

        int i = 0;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(i + j)) {
                j--;
            }
            if (j < 0) return i;   // 完全匹配

            // 坏字符移动
            int bcShift = j - badChar[text.charAt(i + j)];
            if (bcShift < 1) bcShift = 1;
            // 好后缀移动
            int gsShift = goodSuffix[j + 1];  // 注意: 我们用 j+1 作为后缀长度
            // 取最大值移动
            i += Math.max(bcShift, gsShift);
        }
        return -1;
    }

    /** 返回所有匹配下标（允许重叠） */
    public List<Integer> searchAll(String text) {
        List<Integer> result = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return result;
        int i = 0;
        while (i <= n - m) {
            int pos = searchFrom(text, i);
            if (pos == -1) break;
            result.add(pos);
            i = pos + 1;  // 重叠匹配；若不重叠改为 pos + m
        }
        return result;
    }

    private int searchFrom(String text, int start) {
        int n = text.length();
        int m = pattern.length();
        if (start > n - m) return -1;
        int i = start;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(i + j)) j--;
            if (j < 0) return i;
            int bcShift = j - badChar[text.charAt(i + j)];
            if (bcShift < 1) bcShift = 1;
            int gsShift = goodSuffix[j + 1];
            i += Math.max(bcShift, gsShift);
        }
        return -1;
    }

    public static void main(String[] args) {
        // 测试1
        String txt1 = "ABABACAABABACX";
        String pat1 = "ABABAC";
        CorrectBoyerMoore bm1 = new CorrectBoyerMoore(pat1);
        System.out.println("首次匹配下标：" + bm1.search(txt1));
        System.out.println("全部匹配下标：" + bm1.searchAll(txt1));

        // 测试2
        String txt2 = "GCATCGCAGAGAGTATAGCAGAGAGTACG";
        String pat2 = "GCAGAGAG";
        CorrectBoyerMoore bm2 = new CorrectBoyerMoore(pat2);
        System.out.println("匹配位置（期望 5）：" + bm2.search(txt2));
        System.out.println("全部匹配：" + bm2.searchAll(txt2));
    }
}