package cn.exercise.algs4.datastructure.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * KMP 算法（Knuth-Morris-Pratt）：利用已匹配部分的信息避免文本指针回溯。
 *
 * 核心数据结构：next 数组
 *   next[j] = 模式串前 j 个字符构成的子串中，
 *             最长相等前后缀的长度。
 *
 * 匹配时出现失配 → j 回退到 next[j]，文本指针 i 不动。
 *
 * 时间复杂度 O(n+m)，空间 O(m)。
 */
public class KMPSearch {

    /**
     * 首次匹配位置。
     */
    public int indexOf(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return 0;
        if (n < m) return -1;

        int[] next = computeNext(pattern);
        int j = 0;  // 模式串指针

        for (int i = 0; i < n; i++) {
            // 失配时，利用 next 回退 j（不回退 i）
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            if (j == m) {
                return i - m + 1;
            }
        }
        return -1;
    }

    /**
     * 查找所有匹配位置。
     */
    public List<Integer> findAll(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || n < m) return result;

        int[] next = computeNext(pattern);
        int j = 0;

        for (int i = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            if (j == m) {
                result.add(i - m + 1);
                j = next[j - 1];  // 继续找
            }
        }
        return result;
    }

    /**
     * 计算 next 数组。
     *
     * next[j] 含义（以 pattern = "ababa" 为例）：
     *
     *   下标(j): 0  1  2  3  4
     *   字符:    a  b  a  b  a
     *   next:    0  0  1  2  3
     *
     *   next[0]=0  → 单个字符无前后缀
     *   next[1]=0  → "ab" 无公共前后缀
     *   next[2]=1  → "aba" 的最长相等前后缀是 "a"（长度1）
     *   next[3]=2  → "abab" 的最长相等前后缀是 "ab"（长度2）
     *   next[4]=3  → "ababa" 的最长相等前后缀是 "aba"（长度3）
     *
     * 递推构造：已知 next[j-1]=k，即前 j 个字符有长度为 k 的公共前后缀。
     * 看 pattern[k] 和 pattern[j] 是否相等。
     *
     *   匹配：next[j] = k+1
     *   失配：k = next[k-1]，递归回退
     */
    public int[] computeNext(String pattern) {
        int m = pattern.length();
        int[] next = new int[m];
        // next[0] 默认为 0

        int k = 0;  // 当前最长相等前后缀的长度
        for (int j = 1; j < m; j++) {
            // 失配时回退
            while (k > 0 && pattern.charAt(k) != pattern.charAt(j)) {
                k = next[k - 1];
            }
            // 匹配时扩展
            if (pattern.charAt(k) == pattern.charAt(j)) {
                k++;
            }
            next[j] = k;
        }
        return next;
    }

    // ==================== 演示 ====================

    /**
     * 打印 next 数组的每一步构建过程。
     */
    public void demoNext(String pattern) {
        int m = pattern.length();
        int[] next = new int[m];
        int k = 0;

        System.out.println("构建 next 数组: \"" + pattern + "\"");
        System.out.println("-------------------------------");

        for (int j = 1; j < m; j++) {
            System.out.printf("j=%d 字符='%c', 前%d个字符=\"%s\", k=%d%n",
                    j, pattern.charAt(j), j,
                    pattern.substring(0, j), k);

            while (k > 0 && pattern.charAt(k) != pattern.charAt(j)) {
                System.out.printf("  失配: '%c'≠'%c' → k=%d%n",
                        pattern.charAt(k), pattern.charAt(j), next[k - 1]);
                k = next[k - 1];
            }
            if (pattern.charAt(k) == pattern.charAt(j)) {
                System.out.printf("  匹配: '%c'='%c' → k=%d%n",
                        pattern.charAt(k), pattern.charAt(j), k + 1);
                k++;
            } else {
                System.out.printf("  不匹配，k 保持 %d%n", k);
            }
            next[j] = k;
        }
        System.out.println("\nnext: " + Arrays.toString(next));
    }

    /**
     * 打印匹配过程。
     */
    public void demoMatch(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        int[] next = computeNext(pattern);
        int j = 0;

        System.out.println("\n匹配过程: 文本=\"" + text + "\" 模式=\"" + pattern + "\"");
        System.out.println("next: " + Arrays.toString(next));
        System.out.println("-------------------------------");

        for (int i = 0; i < n; i++) {
            System.out.printf("i=%d text[%d]='%c', j=%d", i, i, text.charAt(i), j);

            if (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                System.out.printf(" → 失配, j 回退到 %d", next[j - 1]);
                while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                    j = next[j - 1];
                }
            }

            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
                System.out.printf(" → 匹配, j=%d", j);
            }

            System.out.println();

            if (j == m) {
                System.out.println("  ★ 完全匹配！索引=" + (i - m + 1));
                return;
            }
        }
        System.out.println("  未找到匹配");
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        KMPSearch kmp = new KMPSearch();

        System.out.println("=== 基本测试 ===");
        System.out.println("'world' in 'hello world': " + kmp.indexOf("hello world", "world"));  // 6
        System.out.println("'abcac' in 'ababcabcacbab': " + kmp.indexOf("ababcabcacbab", "abcac")); // 5
        System.out.println("'bba' in 'aaaaa': " + kmp.indexOf("aaaaa", "bba"));                    // -1
        System.out.println("'aaa' in 'aaaaa': " + kmp.indexOf("aaaaa", "aaa"));                    // 0

        System.out.println("\n=== 边界测试 ===");
        System.out.println("空模式:     " + kmp.indexOf("abc", ""));       // 0
        System.out.println("空文本:     " + kmp.indexOf("", "abc"));       // -1
        System.out.println("完全相同:   " + kmp.indexOf("abc", "abc"));    // 0
        System.out.println("模式更长:   " + kmp.indexOf("ab", "abc"));     // -1
        System.out.println("单字符:     " + kmp.indexOf("hello", "o"));    // 4

        System.out.println("\n=== 查找所有匹配 ===");
        System.out.println("'ab' in 'abababab': " + kmp.findAll("abababab", "ab"));
        System.out.println("'aa' in 'aaaaa': " + kmp.findAll("aaaaa", "aa"));

        System.out.println("\n=== next 数组构建演示 ===");
        kmp.demoNext("ababa");

        System.out.println();
        kmp.demoNext("abcab");

        System.out.println("\n=== 匹配过程演示 ===");
        kmp.demoMatch("ababcabcacbab", "abcac");

        // 对比 BF vs KMP 在某些场景下的差异
        System.out.println("\n=== KMP 优势场景 ===");
        String text = "aaaaaaaab";
        String pat = "aaaab";
        System.out.println("文本: \"" + text + "\", 模式: \"" + pat + "\"");
        System.out.println("next: " + Arrays.toString(kmp.computeNext(pat)));
        System.out.println("KMP 找到位置: " + kmp.indexOf(text, pat));
        System.out.println("(BF 在此场景会大量回溯，KMP 则不会)");
    }
}
