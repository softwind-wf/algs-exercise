package com.ds.matching;

public class KMPDemo {

    // 1. 暴力法计算 next 数组（完全对应手工枚举前后缀的思路）
    private static int[] bruteForceNext(String pattern) {
        int n = pattern.length();
        int[] next = new int[n];
        next[0] = -1; // 约定：第0位失配时无法再回退

        // 依次计算 next[1] ~ next[n-1]
        for (int k = 1; k < n; k++) {
            int maxCommonLen = 0;
            // 从最长的可能长度倒着试，第一个匹配的就是最长公共前后缀
            for (int len = k - 1; len >= 1; len--) {
                boolean match = true;
                // 逐字符对比：前缀前len位  vs  后缀后len位
                for (int i = 0; i < len; i++) {
                    if (pattern.charAt(i) != pattern.charAt(k - len + i)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    maxCommonLen = len;
                    break; // 倒序找到的第一个就是最长的，直接退出
                }
            }
            next[k] = maxCommonLen;
        }
        return next;
    }

    // 2. 标准递推法计算 next 数组（教材原版，O(n) 高效实现）
    private static int[] standardNext(String pattern) {
        int n = pattern.length();
        int[] next = new int[n];
        next[0] = -1;
        int i = 1; // 后缀指针
        int j = 0; // 前缀指针

        while (i < n - 1) {
            if (j == -1 || pattern.charAt(i) == pattern.charAt(j)) {
                next[i + 1] = j + 1;
                i++;
                j++;
            } else {
                j = next[j]; // 失配回退，复用已算出的结果
            }
        }
        return next;
    }

    // 3. KMP 字符串匹配主方法
    private static int kmpSearch(String mainStr, String pattern, int[] next) {
        int i = 0; // 主串遍历指针
        int j = 0; // 模式串遍历指针

        while (i < mainStr.length() && j < pattern.length()) {
            if (j == -1 || mainStr.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
            } else {
                j = next[j]; // 失配时按 next 数组回退，不用从头开始
            }
        }

        // 模式串走完说明匹配成功，返回在主串中的起始下标
        if (j == pattern.length()) {
            return i - j;
        }
        return -1; // 匹配失败
    }

    public static void main(String[] args) {
        String pattern = "aaaabcabd";
        String mainStr = "eabaaaabcabdxyz";

        // 分别用两种方法计算 next 数组
        int[] nextBrute = bruteForceNext(pattern);
        int[] nextStandard = standardNext(pattern);

        // 打印对比结果
        System.out.println("模式串：" + pattern);
        System.out.print("暴力版 next 数组：");
        for (int num : nextBrute) {
            System.out.print(num + " ");
        }
        System.out.println();

        System.out.print("标准版 next 数组：");
        for (int num : nextStandard) {
            System.out.print(num + " ");
        }
        System.out.println("\n");

        // 测试 KMP 匹配
        int result = kmpSearch(mainStr, pattern, nextStandard);
        if (result != -1) {
            System.out.println("匹配成功，模式串在主串中的起始下标：" + result);
        } else {
            System.out.println("主串中未找到该模式串");
        }
    }
}
