package cn.exercise.algs4.datastructure.matching;

import java.util.Arrays;

public class BoyerMoore {

    private int[] badChar;
    private int[] goodSuffix;
    private boolean[] isPrefix;

    // ===================== 初始化坏字符数组 badChar =====================
    private void initBadChar(String T) {
        badChar = new int[65536];
        Arrays.fill(badChar, -1);
        for (int i = 0; i < T.length(); i++) {
            badChar[T.charAt(i)] = i;
        }
    }

    // ===================== 初始化好后缀 + isPrefix数组 =====================
    private void initGoodSuffixAndIsPrefix(String T) {
        int m = T.length();
        goodSuffix = new int[m];
        isPrefix = new boolean[m];
        Arrays.fill(goodSuffix, -1);
        Arrays.fill(isPrefix, false);

        for (int i = 0; i < m - 1; i++) {
            int j = i;
            int k = m - 1;

            while (j >= 0 && T.charAt(k) == T.charAt(j)) {
                goodSuffix[k] = j;
                j--;
                k--;
            }
            // 前缀标记
            if (j == -1) {
                isPrefix[i + 1] = true;
            }
        }
    }

    // ===================== 坏字符规则：计算移动距离 =====================
    private int badCharLength(String S, int i, int j) {
        return j - badChar[S.charAt(i)];
    }

    // ===================== 好后缀规则：计算移动距离 =====================
    private int goodSuffixLength(String T, int j) {
        int m = T.length();
        if (j < m - 1) {
            // 好后缀在别处出现过
            if (goodSuffix[j + 1] != -1) {
                return (j + 1) - goodSuffix[j + 1];
            } else {
                // 向后找，看后缀是否是模式串前缀
                for (int k = 1; k < m - j; k++) {
                    if (isPrefix[k]) {
                        return m - k;
                    }
                }
            }
        } else {
            // 没有好后缀
            return 0;
        }
        // 既无匹配后缀，也无前缀匹配
        return m;
    }

    // ===================== BM算法主匹配函数 =====================
    public int boyerMoore(String S, String T) {
        if (S == null || T == null || S.length() < T.length()) {
            return -1;
        }
        if (T.length() == 0) {
            return 0;
        }

        int m = T.length();
        // 初始化三个辅助数组
        initBadChar(T);
        initGoodSuffixAndIsPrefix(T);

        int start = 0;
        int i = m - 1;
        int j = m - 1;

        while (start + m <= S.length()) {
            // 从后往前匹配字符
            if (S.charAt(i) == T.charAt(j)) {
                i--;
                j--;
                // 全部匹配成功
                if (j == -1) {
                    return start;
                }
            } else {
                // 匹配失败，分别计算两种规则的移动步数
                int bcLength = badCharLength(S, i, j);
                int gsLength = goodSuffixLength(T, j);
                // 取更大的步数向后滑动
                start += Math.max(bcLength, gsLength);
                // 重置比较下标
                i = start + m - 1;
                j = m - 1;
            }
        }
        // 匹配失败
        return -1;
    }

    // 测试入口
    public static void main(String[] args) {
        BoyerMoore bm = new BoyerMoore();
        String mainStr = "abcabcabxabc";
        String pattern = "abcabx";
        int index = bm.boyerMoore(mainStr, pattern);
        if (index != -1) {
            System.out.println("匹配下标：" + index);
        } else {
            System.out.println("未找到模式串");
        }
    }
}
