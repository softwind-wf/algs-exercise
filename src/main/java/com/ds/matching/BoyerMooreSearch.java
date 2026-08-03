package com.ds.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BM 算法（Boyer-Moore）：最实用的字符串匹配算法之一。
 * 从右向左比较模式串，失配时利用坏字符规则 + 好后缀规则跳过尽可能多的字符。
 *
 * 平均时间复杂度 O(n/m)，最坏 O(n*m)（但实践中很少出现），
 * 空间 O(m + sigma)，sigma 为字符集大小。
 *
 * 和 KMP 的根本区别：
 *   KMP 从左到右比，i 不回溯，j 通过 next 回退
 *   BM  从右到左比，模式串往前跳，跳过尽可能多的字符
 */
public class BoyerMooreSearch {

    private static final int ALPHABET_SIZE = 256;  // ASCII

    /**
     * 首次匹配位置。仅使用坏字符规则（简化版）。
     */
    public int indexOf(String text, String pattern) {
        return indexOf(text.toCharArray(), pattern.toCharArray());
    }

    public int indexOf(char[] text, char[] pattern) {
        int n = text.length;
        int m = pattern.length;
        if (m == 0) return 0;
        if (n < m) return -1;

        // 坏字符表
        int[] bc = buildBadChar(pattern);

        // 匹配
        int i = 0;  // 模式串相对于文本的起始位置
        while (i <= n - m) {
            int j = m - 1;  // 从右向左比

            while (j >= 0 && text[i + j] == pattern[j]) {
                j--;
            }

            if (j < 0) {
                return i;  // 完全匹配
            }

            // 坏字符规则：text[i+j] 是坏字符
            // 模式串中该字符最后出现的索引 = bc[text[i+j]]
            // 将模式串右移 j - bc[text[i+j]]
            int shift = j - bc[text[i + j]];
            i += Math.max(1, shift);
        }
        return -1;
    }

    /**
     * 完整版 BM：坏字符规则 + 好后缀规则。
     */
    public int indexOfFull(String text, String pattern) {
        return indexOfFull(text.toCharArray(), pattern.toCharArray());
    }

    public int indexOfFull(char[] text, char[] pattern) {
        int n = text.length;
        int m = pattern.length;
        if (m == 0) return 0;
        if (n < m) return -1;

        int[] bc = buildBadChar(pattern);
        int[] gs = buildGoodSuffix(pattern);

        int i = 0;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && text[i + j] == pattern[j]) {
                j--;
            }
            if (j < 0) {
                return i;
            }

            // 两个规则取最大偏移量
            int shiftByBC = j - bc[text[i + j]];  // 可能为负
            int shiftByGS = gs[j];

            i += Math.max(1, Math.max(shiftByBC, shiftByGS));
        }
        return -1;
    }

    // ==================== 坏字符表 ====================

    /**
     * 坏字符表：记录每个字符在模式串中最后出现的索引。
     * 如果字符不在模式串中，记为 -1。
     */
    private int[] buildBadChar(char[] pattern) {
        int m = pattern.length;
        int[] bc = new int[ALPHABET_SIZE];
        Arrays.fill(bc, -1);
        for (int i = 0; i < m; i++) {
            bc[pattern[i]] = i;
        }
        return bc;
    }

    // ==================== 好后缀表 ====================

    /**
     * 好后缀表：当失配发生在模式串索引 j 时，模式串可以安全右移的最小距离。
     *
     * gs[j] = 模式串从 j 处失配时，右移多少。
     * 模式串 [j+1, m-1] 是已匹配的好后缀，我们要在模式串左边找相同子串。
     *
     * 三种情况（优先级递减）：
     * 1. 好后缀在模式串左侧完整出现 → 对齐
     * 2. 好后缀的某段后缀等于模式串的某段前缀 → 对齐
     * 3. 以上都不满足 → 右移 m 位（跨过整个模式串）
     */
    private int[] buildGoodSuffix(char[] pattern) {
        int m = pattern.length;
        int[] gs = new int[m];
        int[] suffix = computeSuffix(pattern);

        // 默认：没有好后缀匹配 → 右移 m 位
        // 但 gs[m-1] 设为 1（最后一个字符失配时没有好后缀信息，退化为最小右移）
        for (int j = 0; j < m; j++) {
            gs[j] = m;
        }
        if (m > 0) {
            gs[m - 1] = 1;
        }

        // 情况 1：好后缀在模式串左侧完整出现
        // i 是好后缀内部的某个位置，suffix[i] 是以 i 结尾的公共后缀长度
        // j = m-1-suffix[i] 是这个好后缀的起始位置
        // 发生失配的位置是 j-1，gs[j-1] = m-1-i
        for (int i = 0; i < m - 1; i++) {
            int len = suffix[i];
            if (len > 0) {
                int mismatchPos = m - 1 - len;  // 失配发生在第 mismatchPos 位
                if (mismatchPos >= 0) {
                    gs[mismatchPos] = m - 1 - i;
                }
            }
        }

        // 情况 2：好后缀的部分后缀是模式串的前缀
        // 对于失配位置 j，如果模式串[j+1..m-1] 的某后缀 = 模式串前缀
        // 对应 suffix 中满足 suffix[i] = i+1 的位置
        for (int i = 0; i < m - 1; i++) {
            if (suffix[i] == i + 1) {  // 前 i+1 个字符等于后缀
                // 所有失配位置 j < m-1-suffix[i] 都适用
                for (int j = 0; j < m - 1 - suffix[i]; j++) {
                    if (gs[j] == m) {  // 只更新尚未设值的
                        gs[j] = m - 1 - i;
                    }
                }
            }
        }

        // gs[m-1] 设为 1（最后一个字符失配，没有好后缀可言）
        // 但上面的情况 2 可能已给它赋值，确保最小值是 1
        return gs;
    }

    /**
     * suffix[i] = 匹配了模式串中区间 [i, m-1] 的后缀后，
     * 模式串前缀能与该后缀匹配的最大长度。
     *
     * 简单说：以 i 为右边界的最长公共后缀长度。
     */
    private int[] computeSuffix(char[] pattern) {
        int m = pattern.length;
        int[] suffix = new int[m];

        // suffix[m-1] = m，整个串自然是自己的后缀
        suffix[m - 1] = m;

        for (int i = m - 2; i >= 0; i--) {
            int j = i;
            while (j >= 0 && pattern[j] == pattern[m - 1 - (i - j)]) {
                j--;
            }
            suffix[i] = i - j;
        }
        return suffix;
    }

    // ==================== 所有匹配 ====================

    public List<Integer> findAll(String text, String pattern) {
        return findAll(text.toCharArray(), pattern.toCharArray());
    }

    public List<Integer> findAll(char[] text, char[] pattern) {
        List<Integer> result = new ArrayList<>();
        int n = text.length, m = pattern.length;
        if (m == 0 || n < m) return result;

        int[] bc = buildBadChar(pattern);
        int[] gs = buildGoodSuffix(pattern);

        int i = 0;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && text[i + j] == pattern[j]) {
                j--;
            }
            if (j < 0) {
                result.add(i);
                // 找到一个匹配后，右移一位继续找
                i += 1;
                continue;
            }
            i += Math.max(1, Math.max(j - bc[text[i + j]], gs[j]));
        }
        return result;
    }

    // ==================== 演示 ====================

    public void demo(String text, String pattern) {
        char[] t = text.toCharArray();
        char[] p = pattern.toCharArray();
        int n = t.length, m = p.length;

        System.out.println("文本: \"" + text + "\"  长度=" + n);
        System.out.println("模式: \"" + pattern + "\"  长度=" + m);
        System.out.println();

        // 坏字符表
        int[] bc = buildBadChar(p);
        System.out.println("坏字符表 (选列):");
        for (char c : pattern.toCharArray()) {
            System.out.printf("  '%c'=%d", c, bc[c]);
        }
        System.out.println("\n");

        // 好后缀表
        int[] gs = buildGoodSuffix(p);
        System.out.println("好后缀表 gs[j]: " + Arrays.toString(gs));
        System.out.println("  j:        " + Arrays.toString(range(m)));
        System.out.println("  pattern:  " + Arrays.toString(p));
        System.out.println();

        // 匹配过程
        System.out.println("匹配过程:");
        int i = 0;
        while (i <= n - m) {
            int j = m - 1;

            // 显示当前对齐
            System.out.printf("对齐到 i=%d:%n", i);
            System.out.println("  " + text);

            StringBuilder line = new StringBuilder();
            for (int k = 0; k < i; k++) line.append(' ');
            line.append(pattern);
            System.out.println("  " + line);

            // 从右向左比
            while (j >= 0 && t[i + j] == p[j]) {
                j--;
            }

            if (j < 0) {
                System.out.println("  ★ 完全匹配！索引=" + i);
                i += 1;
                continue;
            }

            int badShift = j - bc[t[i + j]];
            int goodShift = gs[j];
            int finalShift = Math.max(1, Math.max(badShift, goodShift));

            System.out.printf("  坏字符: text[%d]='%c' vs pattern[%d]='%c', 右移=%d%n",
                    i + j, t[i + j], j, p[j], badShift);
            System.out.printf("  好后缀: gs[%d]=%d%n", j, goodShift);
            System.out.printf("  实际右移: %d%n%n", finalShift);

            i += finalShift;
        }
    }

    private int[] range(int n) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) r[i] = i;
        return r;
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        BoyerMooreSearch bm = new BoyerMooreSearch();

        System.out.println("=== 坏字符版 ===");
        System.out.println("'world' in 'hello world': " + bm.indexOf("hello world", "world"));
        System.out.println("'abcac' in 'ababcabcacbab': " + bm.indexOf("ababcabcacbab", "abcac"));
        System.out.println("'bba' in 'aaaaa': " + bm.indexOf("aaaaa", "bba"));

        System.out.println("\n=== 完整版（坏字符+好后缀）===");
        System.out.println("'example' in 'here is a simple example': "
                + bm.indexOfFull("here is a simple example", "example"));
        System.out.println("'abc' in 'abcabcabc': " + bm.findAll("abcabcabc", "abc"));

        System.out.println("\n=== 边界测试 ===");
        System.out.println("空模式:     " + bm.indexOf("abc", ""));
        System.out.println("空文本:     " + bm.indexOf("", "abc"));
        System.out.println("完全相同:   " + bm.indexOf("abc", "abc"));
        System.out.println("模式更长:   " + bm.indexOf("ab", "abc"));
        System.out.println("单字符:     " + bm.indexOf("hello", "o"));

        System.out.println("\n=== BM vs KMP 优势场景 ===");
        // BM 真正优势：模式串长，字符集大，右侧就失配
        String text = "this is a test where the pattern appears at the end";
        String pat = "end";
        System.out.println("文本: \"" + text + "\"");
        System.out.println("模式: \"" + pat + "\"");
        System.out.println("BM 找到: " + bm.indexOf(text, pat));
        System.out.println("(BM 从右向左比较，'n' vs ' ' 立即失配，跳过多个字符)");

        System.out.println("\n=== 好后缀表构建演示 ===");
        bm.demo("ababcabcacbab", "abcac");
    }
}
