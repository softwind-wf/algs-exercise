package com.ds.matching;

/**
 * Sunday + BM 坏字符混合算法（BlockSkipMatch 的修正版）。
 *
 * 保留原思路中的"多信息跳跃"，但使用正确的数学依据：
 *   失配后同时用 Sunday 向前看 + BM 向后看，取最大跳跃。
 *
 * Sunday（向右看）: checkPos 的字符不在模式中 → 跳 m+1
 * BM 坏字符（向左看）: 失配的字符在模式中最右位置 → 跳 j - lastPos[c]
 */
public class SundayBMHybrid {

    public int search(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        if (m == 0) return 0;
        if (n < m) return -1;

        int[] lastPos = new int[65536];
        java.util.Arrays.fill(lastPos, -1);
        for (int i = 0; i < m; i++) lastPos[pattern.charAt(i)] = i;

        int start = 0;
        while (start <= n - m) {
            int j = 0;
            while (j < m && text.charAt(start + j) == pattern.charAt(j)) j++;
            if (j == m) return start;

            // BM 坏字符：失配位置 text[start+j] 在模式中最右出现
            int shiftBM = 1;
            if (j < m) {
                int idx = lastPos[text.charAt(start + j)];
                shiftBM = idx < 0 ? j + 1 : j - idx;
            }

            // Sunday：窗口后第一个字符
            int shiftSunday = 1;
            int checkPos = start + m;
            if (checkPos < n) {
                int idx = lastPos[text.charAt(checkPos)];
                shiftSunday = idx < 0 ? m + 1 : m - idx;
            }

            start += Math.max(1, Math.max(shiftBM, shiftSunday));
        }
        return -1;
    }

    public static void main(String[] args) {
        SundayBMHybrid sbm = new SundayBMHybrid();

        String[][] cases = {
                {"abacababxabcabcabx", "abcabc"},
                {"hello world", "world"},
                {"mississippi", "issip"},
                {"this is a test", "test"},
                {"aaaaa", "bba"},
                {"xabYcde", "abY"},
                {"aXbcYdefgh", "bcY"},
                {"ababcabcacbab", "abcac"},
                {"abcabcabc", "abc"},
                {"", "a"},
                {"abc", ""},
                {"a", "a"},
                {"GGGGGGGX", "GGGGGGGY"},
        };

        System.out.println("Sunday + BM 混合算法 (BlockSkipMatch 修正版)");
        System.out.println("======================================");
        int passed = 0;
        for (String[] c : cases) {
            int r = sbm.search(c[0], c[1]);
            int expected = (c[0].isEmpty() || c[1].isEmpty())
                    ? (c[1].isEmpty() ? 0 : -1)
                    : c[0].indexOf(c[1]);
            boolean ok = r == expected;
            System.out.printf("%s search(\"%s\", \"%s\") = %d, expected=%d%n",
                    ok ? "✓" : "✗", c[0], c[1], r, expected);
            if (ok) passed++;
        }
        System.out.println("\n通过: " + passed + " / " + cases.length);
    }
}
