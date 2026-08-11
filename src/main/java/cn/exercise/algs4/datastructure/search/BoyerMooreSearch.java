package cn.exercise.algs4.datastructure.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * BM 算法（Boyer-Moore）：从右向左比较，利用坏字符+好后缀规则跳过字符。
 * 平均 O(n/m)，最坏 O(n*m)，空间 O(m+sigma)。
 *
 * 和 KMP 的根本区别：KMP 从左到右比，i 不回溯；BM 从右到左比，模式串往前跳。
 */
public class BoyerMooreSearch {

    // ==================== 坏字符规则版 ====================

    public int indexOf(String text, String pattern) {
        return indexOf(text.toCharArray(), pattern.toCharArray());
    }

    public int indexOf(char[] text, char[] pattern) {
        int n = text.length, m = pattern.length;
        if (m == 0) return 0;
        if (n < m) return -1;

        Map<Character, Integer> bc = buildBadChar(pattern);
        int i = 0;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && text[i + j] == pattern[j]) j--;
            if (j < 0) return i;
            int shift = j - bc.getOrDefault(text[i + j], -1);
            i += Math.max(1, shift);
        }
        return -1;
    }

    // ==================== 完整版（坏字符+好后缀） ====================

    public int indexOfFull(String text, String pattern) {
        return indexOfFull(text.toCharArray(), pattern.toCharArray());
    }

    public int indexOfFull(char[] text, char[] pattern) {
        int n = text.length, m = pattern.length;
        if (m == 0) return 0;
        if (n < m) return -1;

        Map<Character, Integer> bc = buildBadChar(pattern);
        int[] gs = buildGoodSuffix(pattern);

        int i = 0;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && text[i + j] == pattern[j]) j--;
            if (j < 0) return i;
            int shift = Math.max(
                    j - bc.getOrDefault(text[i + j], -1),
                    gs[j]
            );
            i += Math.max(1, shift);
        }
        return -1;
    }

    public List<Integer> findAll(String text, String pattern) {
        return findAll(text.toCharArray(), pattern.toCharArray());
    }

    public List<Integer> findAll(char[] text, char[] pattern) {
        List<Integer> result = new ArrayList<>();
        int n = text.length, m = pattern.length;
        if (m == 0 || n < m) return result;

        Map<Character, Integer> bc = buildBadChar(pattern);
        int[] gs = buildGoodSuffix(pattern);

        int i = 0;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && text[i + j] == pattern[j]) j--;
            if (j < 0) {
                result.add(i++);
                continue;
            }
            int shift = Math.max(
                    j - bc.getOrDefault(text[i + j], -1),
                    gs[j]
            );
            i += Math.max(1, shift);
        }
        return result;
    }

    // ==================== 坏字符表 ====================

    /**
     * 使用 HashMap 支持全 Unicode 字符集。
     * 记录每个字符在模式串中最后出现的索引。
     * 不出现的字符后面再查时返回 -1。
     */
    private Map<Character, Integer> buildBadChar(char[] pattern) {
        Map<Character, Integer> bc = new HashMap<>();
        for (int i = 0; i < pattern.length; i++) {
            bc.put(pattern[i], i);
        }
        return bc;
    }

    // ==================== 好后缀表 ====================

    private int[] buildGoodSuffix(char[] pattern) {
        int m = pattern.length;
        int[] gs = new int[m];
        int[] suffix = computeSuffix(pattern);

        // 默认：m（无好后缀匹配）
        // gs[m-1]=1：最后一个字符失配时，好后缀规则退化为最小右移
        for (int j = 0; j < m; j++) gs[j] = m;
        if (m > 0) gs[m - 1] = 1;

        // Case 2：好后缀的某段后缀 = 模式串前缀
        for (int i = 0; i < m - 1; i++) {
            if (suffix[i] == i + 1) {
                for (int j = 0; j < m - 1 - suffix[i]; j++) {
                    if (gs[j] == m) gs[j] = m - 1 - i;
                }
            }
        }

        // Case 1：好后缀在模式串左侧完整出现（覆盖 Case 2，更优）
        for (int i = 0; i < m - 1; i++) {
            int len = suffix[i];
            if (len > 0) {
                int pos = m - 1 - len;
                if (pos >= 0) gs[pos] = m - 1 - i;
            }
        }

        return gs;
    }

    /** suffix[i] = 以 i 结尾的最长公共后缀长度 */
    private int[] computeSuffix(char[] pattern) {
        int m = pattern.length;
        int[] suffix = new int[m];
        suffix[m - 1] = m;
        for (int i = m - 2; i >= 0; i--) {
            int j = i;
            while (j >= 0 && pattern[j] == pattern[m - 1 - (i - j)]) j--;
            suffix[i] = i - j;
        }
        return suffix;
    }

    // ==================== 演示 ====================

    public void demo(String text, String pattern) {
        char[] t = text.toCharArray(), p = pattern.toCharArray();
        int n = t.length, m = p.length;

        System.out.println("文本: \"" + text + "\"");
        System.out.println("模式: \"" + pattern + "\"");

        Map<Character, Integer> bc = buildBadChar(p);
        int[] gs = buildGoodSuffix(p);
        System.out.println("坏字符表: " + bc);
        System.out.println("好后缀表: " + Arrays.toString(gs));

        System.out.println("\n匹配过程:");
        int i = 0;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && t[i + j] == p[j]) j--;

            if (j < 0) {
                System.out.printf("i=%2d: \"%s\" → ★ 完全匹配！%n", i, text.substring(i, i + m));
                i++;
                continue;
            }

            int badShift = j - bc.getOrDefault(t[i + j], -1);
            int goodShift = gs[j];
            int shift = Math.max(1, Math.max(badShift, goodShift));

            StringBuilder line = new StringBuilder();
            for (int k = 0; k < i; k++) line.append(' ');
            line.append(pattern);

            System.out.printf("i=%2d: %s%n", i, text);
            System.out.printf("      %s  (j=%d, 坏字符='%c'→%d, 好后缀[%d]=%d, 右移=%d)%n",
                    line, j, t[i + j], badShift, j, goodShift, shift);

            i += shift;
        }
    }

    // ==================== 性能测试 ====================

    interface Searcher {
        int indexOf(String text, String pattern);
    }

    public static void main(String[] args) {
        BoyerMooreSearch bm = new BoyerMooreSearch();

        // ===== 1. 正确性测试 =====
        System.out.println("======================================");
        System.out.println("【1. 正确性测试】");
        System.out.println("======================================");

        String[][] cases = {
                {"hello world", "world"},
                {"hello world", "hello"},
                {"hello world", " "},
                {"ababcabcacbab", "abcac"},
                {"aaaaa", "bba"},
                {"aaaaa", "aaa"},
                {"abcabcabc", "abc"},
                {"this is a test", "test"},
                {"a", "a"},
                {"a", "b"},
                {"", ""},
                {"", "a"},
                {"abc", ""},
                {"abc", "abcd"},
                {"here is a simple example", "example"},
                {"mississippi", "issip"},
        };

        for (String[] c : cases) {
            int r1 = bm.indexOf(c[0], c[1]);
            int r2 = bm.indexOfFull(c[0], c[1]);
            int expected = c[0].indexOf(c[1]);
            String ok = (r1 == expected && r2 == expected) ? "✓" : "✗";
            System.out.printf("%s indexOf(\"%s\", \"%s\") = %d full=%d expected=%d%n",
                    ok, c[0], c[1], r1, r2, expected);
        }

        System.out.println();
        System.out.println("findAll 测试:");
        String[][] findAllCases = {
                {"abcabcabc", "abc"},
                {"aaaaa", "aa"},
                {"ababab", "ab"},
        };
        for (String[] c : findAllCases) {
            List<Integer> res = bm.findAll(c[0], c[1]);
            System.out.printf("  findAll(\"%s\", \"%s\") = %s%n", c[0], c[1], res);
        }

        // ===== 2. 边界测试 =====
        System.out.println();
        System.out.println("======================================");
        System.out.println("【2. 边界测试】");
        System.out.println("======================================");

        System.out.println("中文:      " + bm.indexOf("你好世界", "世界"));
        System.out.println("特殊字符:  " + bm.indexOf("a*b?c[d]e", "c[d]"));
        System.out.println("重复模式:  " + bm.indexOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaab", "aaaaaaaab"));
        System.out.println("假匹配:    " + bm.indexOf("GGGGGGGX", "GGGGGGGY"));

        String longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
        String longPattern = "consectetur";
        int r1 = bm.indexOf(longText, longPattern);
        int r2 = bm.indexOfFull(longText, longPattern);
        System.out.printf("长文本:    indexOf=%d full=%d%n", r1, r2);

        // ===== 3. 好后缀表验证 =====
        System.out.println();
        System.out.println("======================================");
        System.out.println("【3. 好后缀表验证】");
        System.out.println("======================================");

        bm.demo("ababcabcacbab", "abcac");

        // ===== 4. 性能对比 =====
        System.out.println();
        System.out.println("======================================");
        System.out.println("【4. 性能对比】");
        System.out.println("======================================");

        int N = 100000;
        StringBuilder sb = new StringBuilder(N);
        Random rand = new Random(42);
        for (int i = 0; i < N; i++) {
            sb.append((char) ('A' + rand.nextInt(26)));
        }
        String perfPattern = "ABCDEFGHIJ";
        sb.replace(N - 20, N - 10, perfPattern);
        String perfText = sb.toString();

        Searcher bmSimple = bm::indexOf;
        Searcher bmFull = bm::indexOfFull;
        Searcher jdk = String::indexOf;

        for (int i = 0; i < 100; i++) {
            bmSimple.indexOf(perfText, perfPattern);
            bmFull.indexOf(perfText, perfPattern);
            jdk.indexOf(perfText, perfPattern);
        }

        long t1 = System.nanoTime();
        int rBM = bmSimple.indexOf(perfText, perfPattern);
        long t2 = System.nanoTime();
        int rFull = bmFull.indexOf(perfText, perfPattern);
        long t3 = System.nanoTime();
        int rJDK = jdk.indexOf(perfText, perfPattern);
        long t4 = System.nanoTime();

        System.out.printf("10万字符查找 \"%s\":%n", perfPattern);
        System.out.printf("  BM坏字符: %d  %.2fms%n", rBM, (t2 - t1) / 1e6);
        System.out.printf("  BM完整版: %d  %.2fms%n", rFull, (t3 - t2) / 1e6);
        System.out.printf("  JDK朴素:  %d  %.2fms%n", rJDK, (t4 - t3) / 1e6);

        // ===== 5. 最坏情况测试 =====
        System.out.println();
        System.out.println("======================================");
        System.out.println("【5. 最坏情况】");
        System.out.println("======================================");

        String worstText = "AAAAAAAB";
        String worstPat = "AAAB";
        System.out.println("文本: \"" + worstText + "\", 模式: \"" + worstPat + "\"");

        t1 = System.nanoTime();
        rBM = bm.indexOf(worstText, worstPat);
        t2 = System.nanoTime();
        rJDK = worstText.indexOf(worstPat);
        t3 = System.nanoTime();

        System.out.printf("  BM:  %d  %.2fms%n", rBM, (t2 - t1) / 1e6);
        System.out.printf("  JDK: %d  %.2fms%n", rJDK, (t3 - t2) / 1e6);

        System.out.println("\n测试完成！");
    }
}
