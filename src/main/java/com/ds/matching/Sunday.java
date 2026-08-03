package com.ds.matching;

import java.util.Arrays;

public class Sunday {

    // 用于保存模式串T中的任意字符在模式串T中最后出现下标的数组
    private int[] lastIndex;

    /**
     * 统计模式串T中的任意字符在模式串T中最后出现下标的方法
     * @param T 模式串
     */
    private void initLastIndex(String T) {
        lastIndex = new int[65536];
        Arrays.fill(lastIndex, -1);
        for (int i = 0; i < T.length(); i++) {
            lastIndex[T.charAt(i)] = i;
        }
    }

    /**
     * Sunday算法主方法
     * @param S 主串
     * @param T 模式串
     * @return 匹配成功返回起始下标，失败返回-1
     */
    public int sunday(String S, String T) {
        if (S == null || T == null || S.length() < T.length()) {
            return -1;
        }
        if (T.length() == 0) {
            return 0;
        }

        // 初始化lastIndex数组
        initLastIndex(T);

        int start = 0;
        int i = 0;
        int j = 0;
        int m = T.length();
        int n = S.length();

        while (start <= n - m) {
            // 从模式串起点开始逐位向后比较
            while (j < m && S.charAt(i) == T.charAt(j)) {
                i++;
                j++;
            }

            // 匹配成功
            if (j == m) {
                return start;
            }

            // 计算下一跳位置
            if (start + m < n) {
                start += m - lastIndex[S.charAt(start + m)];
            } else {
                return -1;
            }

            // 重置下标
            i = start;
            j = 0;
        }

        return -1;
    }

    // 测试入口
    public static void main(String[] args) {
        Sunday sunday = new Sunday();
        String s = "abcdefabc123";
        String t = "abc123";
        int pos = sunday.sunday(s, t);
        System.out.println("匹配位置：" + pos);
    }
}
