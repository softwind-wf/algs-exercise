package test5;

public class BruteForceSearchV1 {
    /**
     * 暴力子字符串查找（双重循环实现）
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

        // 遍历文本中所有可能的起始位置
        for (int i = 0; i <= N - M; i++) {
            int j;
            // 从当前位置开始，逐个字符比较
            for (j = 0; j < M; j++) {
                if (txt.charAt(i + j) != pat.charAt(j)) {
                    break; // 不匹配，提前退出
                }
            }
            // 整个模式串匹配成功，返回起始索引
            if (j == M) {
                return i;
            }
        }
        // 未找到匹配
        return N;
    }

    // 测试方法
    public static void main(String[] args) {
        String text = "ABRACADABRA";
        String pattern1 = "CAD";
        String pattern2 = "XYZ";
        String pattern3 = "ABRA";

        System.out.println("文本：" + text);
        System.out.println("查找模式 \"" + pattern1 + "\"，结果索引：" + search(pattern1, text));
        System.out.println("查找模式 \"" + pattern2 + "\"，结果索引：" + search(pattern2, text));
        System.out.println("查找模式 \"" + pattern3 + "\"，结果索引：" + search(pattern3, text));
    }
}