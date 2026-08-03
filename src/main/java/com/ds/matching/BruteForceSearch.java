package com.ds.matching;

import java.util.ArrayList;
import java.util.List;

/**
 * BF 算法（Brute Force，暴力匹配）：字符串匹配中最朴素的算法。
 * 将模式串在文本串上滑动，每个位置逐字符比较。
 * 时间复杂度 O(n*m)，空间 O(1)。n=文本长度，m=模式长度。
 */
public class BruteForceSearch {

    /**
     * 在文本串 text 中查找模式串 pattern 首次出现的位置。
     *
     * @param text    文本串
     * @param pattern 模式串
     * @return 首次匹配的起始索引，未找到返回 -1
     */
    public int indexOf(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();

        // i 是模式串在文本串上的起始位置
        for (int i = 0; i <= n - m; i++) {
            int j;
            // 逐字符比较
            for (j = 0; j < m; j++) {
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    break;
                }
            }
            // j 走到末尾，说明完全匹配
            if (j == m) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 找出所有匹配位置的索引。
     */
    public List<Integer> findAll(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        for (int i = 0; i <= n - m; i++) {
            int j;
            for (j = 0; j < m; j++) {
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    break;
                }
            }
            if (j == m) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * 字符数组版本，直接操作 char[] 更高效。
     */
    public int indexOf(char[] text, char[] pattern) {
        int n = text.length;
        int m = pattern.length;

        for (int i = 0; i <= n - m; i++) {
            int j;
            for (j = 0; j < m; j++) {
                if (text[i + j] != pattern[j]) {
                    break;
                }
            }
            if (j == m) {
                return i;
            }
        }
        return -1;
    }

    /**
     * BF 算法匹配过程演示：打印每一步的比较过程。
     */
    public void demo(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();

        System.out.println("文本: \"" + text + "\"");
        System.out.println("模式: \"" + pattern + "\"");
        System.out.println();

        for (int i = 0; i <= n - m; i++) {
            // 构建可视化字符串
            StringBuilder match = new StringBuilder();
            for (int k = 0; k < i; k++) match.append(' ');

            int j;
            for (j = 0; j < m; j++) {
                char tc = text.charAt(i + j);
                char pc = pattern.charAt(j);
                if (tc == pc) {
                    match.append(tc);
                } else {
                    match.append('!');
                    break;
                }
            }

            System.out.printf("%s%n", text);
            System.out.printf("%s", "                                              ");
            System.out.println(pattern);

            if (j == m) {
                System.out.println("            ↑ 匹配成功！索引=" + i);
                return;
            }
            System.out.printf("%s%n%n", match);
        }
        System.out.println("未找到匹配");
    }

    public static void main(String[] args) {
        BruteForceSearch bf = new BruteForceSearch();

        // 基本测试
        System.out.println("=== 基本测试 ===");
        int idx1 = bf.indexOf("hello world", "world");
        System.out.println("'world' in 'hello world': 索引=" + idx1);  // 6

        int idx2 = bf.indexOf("ababcabcacbab", "abcac");
        System.out.println("'abcac' in 'ababcabcacbab': 索引=" + idx2); // 5

        int idx3 = bf.indexOf("aaaaa", "bba");
        System.out.println("'bba' in 'aaaaa': 索引=" + idx3);           // -1

        // 边界测试
        System.out.println("\n=== 边界测试 ===");
        System.out.println("空模式:  " + bf.indexOf("abc", ""));         // 0
        System.out.println("空文本:  " + bf.indexOf("", "abc"));         // -1
        System.out.println("完全相同:" + bf.indexOf("abc", "abc"));      // 0
        System.out.println("首字符:  " + bf.indexOf("abc", "a"));        // 0
        System.out.println("尾字符:  " + bf.indexOf("abc", "c"));        // 2

        // 查找所有匹配
        System.out.println("\n=== 查找所有匹配 ===");
        List<Integer> all = bf.findAll("abababab", "ab");
        System.out.println("'ab' in 'abababab': " + all); // [0, 2, 4, 6]

        // char[] 版本
        System.out.println("\n=== char[] 版本 ===");
        int idx4 = bf.indexOf("algorithm".toCharArray(), "rith".toCharArray());
        System.out.println("'rith' in 'algorithm': 索引=" + idx4); // 4

        // 演示匹配过程
        System.out.println("\n=== 匹配过程演示 ===");
        bf.demo("ababcabcacbab", "abcac");
    }
}
