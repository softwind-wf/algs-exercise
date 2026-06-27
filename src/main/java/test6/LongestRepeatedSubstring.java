package test6;

public class LongestRepeatedSubstring {

    // 两个字符串的最长公共前缀（原图代码）
    private static int lcp(String s, String t) {
        int N = Math.min(s.length(), t.length());
        for (int i = 0; i < N; i++) {
            if (s.charAt(i) != t.charAt(i)) {
                return i;
            }
        }
        return N;
    }

    // 寻找最长重复子串
    public static String lrs(String text) {
        int n = text.length();
        // 1. 构建所有后缀字符串
        String[] suffixes = new String[n];
        for (int i = 0; i < n; i++) {
            suffixes[i] = text.substring(i);
        }

        // 2. 后缀数组字典序排序
        java.util.Arrays.sort(suffixes);

        int maxLen = 0;
        String result = "";

        // 3. 遍历相邻后缀，计算公共前缀
        for (int i = 1; i < n; i++) {
            int len = lcp(suffixes[i], suffixes[i-1]);
            if (len > maxLen) {
                maxLen = len;
                result = suffixes[i].substring(0, len);
            }
        }
        return result;
    }

    // 测试主方法
    public static void main(String[] args) {
        String str = "to be or not to be";
        String res = lrs(str);
        System.out.println("最长重复子串：" + res);
    }
}