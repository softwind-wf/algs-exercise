package test5;

public class BruteForceSearchV2 {
    /**
     * 暴力子字符串查找（单循环显式回退实现）
     * @param pat 模式字符串（要查找的子串）
     * @param txt 文本字符串（被查找的文本）
     * @return 模式串在文本中第一次出现的起始索引；未找到返回文本长度
     */
    public static int search(String pat, String txt) {
        int M = pat.length();
        int N = txt.length();
        
        // 边界处理：模式串为空或长度大于文本串，直接返回N
        if (M == 0 || M > N) {
            return N;
        }

        int i, j;
        for (i = 0, j = 0; i < N && j < M; i++) {
            if (txt.charAt(i) == pat.charAt(j)) {
                j++; // 匹配成功，模式串指针后移
            } else {
                // 匹配失败，文本指针回退，模式串指针重置
                i = i - j;
                j = 0;
            }
        }

        // 匹配完成判断
        if (j == M) {
            return i - M; // 找到匹配，返回起始索引
        } else {
            return N; // 未找到匹配
        }
    }

    // 测试方法
    public static void main(String[] args) {
        String text = "AAAAAB";
        String pattern1 = "AAAB";
        String pattern2 = "AABX";
        String pattern3 = "B";

        System.out.println("文本：" + text);
        System.out.println("查找模式 \"" + pattern1 + "\"，结果索引：" + search(pattern1, text));
        System.out.println("查找模式 \"" + pattern2 + "\"，结果索引：" + search(pattern2, text));
        System.out.println("查找模式 \"" + pattern3 + "\"，结果索引：" + search(pattern3, text));
    }
}