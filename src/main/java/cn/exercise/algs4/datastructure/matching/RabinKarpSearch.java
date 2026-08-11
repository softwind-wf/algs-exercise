package cn.exercise.algs4.datastructure.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RK 算法（Rabin-Karp，滚动哈希）：用哈希值代替逐字符比较。
 * 核心思路：子串哈希值相等时才做精确匹配，大幅减少字符比较次数。
 *
 * 时间复杂度：平均 O(n+m)，最坏 O(n*m)（哈希碰撞严重时）。
 * 空间复杂度：O(1)。
 * n = 文本长度，m = 模式长度。
 */
public class RabinKarpSearch {

    // 大质数，减少哈希冲突
    private static final long MOD = 1_000_000_007L;
    // 基数，256 覆盖所有 ASCII 字符
    private static final int BASE = 256;

    /**
     * 返回模式串在文本串中首次出现的位置。
     */
    public int indexOf(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return 0;
        if (n < m) return -1;

        long patternHash = 0;  // 模式串的哈希值
        long textHash = 0;     // 当前窗口的哈希值
        long h = 1;            // h = BASE^(m-1) % MOD，用于滚动时去头

        // 预计算 BASE^(m-1) % MOD
        for (int i = 0; i < m - 1; i++) {
            h = (h * BASE) % MOD;
        }

        // 计算初始哈希
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MOD;
            textHash = (textHash * BASE + text.charAt(i)) % MOD;
        }

        // 滑动窗口
        for (int i = 0; i <= n - m; i++) {
            // 哈希匹配 → 逐字符验证（防碰撞）
            if (textHash == patternHash) {
                if (text.regionMatches(i, pattern, 0, m)) {
                    return i;
                }
            }

            // 计算下一个窗口的哈希值（滚动哈希核心）
            if (i < n - m) {
                // 去头 + MOD（防负数），乘 BASE，加尾
                textHash = (textHash - text.charAt(i) * h % MOD + MOD) % MOD;
                textHash = (textHash * BASE + text.charAt(i + m)) % MOD;
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

        long patternHash = 0, textHash = 0;
        long h = 1;

        for (int i = 0; i < m - 1; i++) {
            h = (h * BASE) % MOD;
        }

        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MOD;
            textHash = (textHash * BASE + text.charAt(i)) % MOD;
        }

        for (int i = 0; i <= n - m; i++) {
            if (textHash == patternHash) {
                if (text.regionMatches(i, pattern, 0, m)) {
                    result.add(i);
                }
            }
            if (i < n - m) {
                textHash = (textHash - text.charAt(i) * h % MOD + MOD) % MOD;
                textHash = (textHash * BASE + text.charAt(i + m)) % MOD;
            }
        }
        return result;
    }

    /**
     * 返回所有匹配位置（int[] 版本）。
     */
    public int[] search(String text, String pattern) {
        List<Integer> list = findAll(text, pattern);
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    // ==================== 多模式匹配 ====================

    /**
     * 在一段文本中同时匹配多个模式串。
     * 返回每个模式串首次出现的位置（未找到为 -1）。
     */
    public int[] matchMulti(String text, String... patterns) {
        int k = patterns.length;
        int n = text.length();

        // 1. 统一预处理：找到所有模式的最大长度 M
        int maxLen = 0;
        for (String p : patterns) {
            if (p.length() > maxLen) maxLen = p.length();
        }
        if (maxLen == 0) return new int[k];

        // 2. 按模式长度分组，每组独立计算哈希
        int[] result = new int[k];
        Arrays.fill(result, -1);

        // 简单策略：对每个模式串独立走一遍 RK
        // （真正高效多模式匹配用 AC 自动机，这里演示基本思路）
        for (int i = 0; i < k; i++) {
            if (result[i] == -1) {
                result[i] = indexOf(text, patterns[i]);
            }
        }
        return result;
    }

    // ==================== 演示 ====================

    /**
     * 打印滚动哈希的每一步，帮助理解算法。
     */
    public void demo(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || n < m) return;

        long h = 1;
        for (int i = 0; i < m - 1; i++) {
            h = (h * BASE) % MOD;
        }

        // 初始哈希
        long pHash = 0, tHash = 0;
        for (int i = 0; i < m; i++) {
            pHash = (pHash * BASE + pattern.charAt(i)) % MOD;
            tHash = (tHash * BASE + text.charAt(i)) % MOD;
        }

        System.out.println("文本: \"" + text + "\", 模式: \"" + pattern + "\"");
        System.out.println("模式哈希: " + pHash);
        System.out.println("BASE^(m-1) mod MOD (h): " + h);
        System.out.println();

        for (int i = 0; i <= n - m; i++) {
            String sub = text.substring(i, i + m);
            long directHash = computeHash(sub); // 直接计算用于验证
            System.out.printf("i=%d 窗口 [%s] 哈希=%d (直接算=%d)%s%n",
                    i, sub, tHash, directHash,
                    tHash == pHash ? " ← 命中!" : "");

            if (tHash == pHash && text.regionMatches(i, pattern, 0, m)) {
                System.out.println("          精确匹配成功！索引=" + i);
                return;
            }

            // 滚动
            if (i < n - m) {
                long old = tHash;
                tHash = (tHash - text.charAt(i) * h % MOD + MOD) % MOD;
                tHash = (tHash * BASE + text.charAt(i + m)) % MOD;
                System.out.printf("          滚动: (%d - '%c'*%d) * %d + '%c' = %d %n%n",
                        old, text.charAt(i), h, BASE, text.charAt(i + m), tHash);
            }
        }
    }

    /** 直接计算字符串哈希（用于演示对比） */
    private long computeHash(String s) {
        long hash = 0;
        for (int i = 0; i < s.length(); i++) {
            hash = (hash * BASE + s.charAt(i)) % MOD;
        }
        return hash;
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        RabinKarpSearch rk = new RabinKarpSearch();

        System.out.println("=== 基本测试 ===");
        System.out.println("'world' in 'hello world': " + rk.indexOf("hello world", "world"));  // 6
        System.out.println("'abcac' in 'ababcabcacbab': " + rk.indexOf("ababcabcacbab", "abcac")); // 5
        System.out.println("'bba' in 'aaaaa': " + rk.indexOf("aaaaa", "bba"));                    // -1

        System.out.println("\n=== 边界测试 ===");
        System.out.println("空模式:   " + rk.indexOf("abc", ""));      // 0
        System.out.println("空文本:   " + rk.indexOf("", "abc"));      // -1
        System.out.println("完全相同: " + rk.indexOf("abc", "abc"));   // 0
        System.out.println("模式更长: " + rk.indexOf("ab", "abc"));    // -1

        System.out.println("\n=== 查找所有匹配 ===");
        System.out.println("'ab' in 'abababab': " + Arrays.toString(rk.search("abababab", "ab")));

        System.out.println("\n=== 多模式匹配 ===");
        int[] results = rk.matchMulti("hello world, welcome to java world",
                "hello", "world", "java", "python");
        System.out.println("[hello,world,java,python]: " + Arrays.toString(results)); // [0,6,22,-1]

        System.out.println("\n=== 滚动哈希演示 ===");
        rk.demo("ababcabcacbab", "abcac");
    }
}
