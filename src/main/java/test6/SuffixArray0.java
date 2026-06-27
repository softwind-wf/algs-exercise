package test6;

import java.util.Arrays;

public class SuffixArray0 {
    private final String[] suffixes;
    private final int N;

    public SuffixArray0(String s) {
        N = s.length();
        suffixes = new String[N];
        for (int i = 0; i < N; i++) {
            suffixes[i] = s.substring(i);
        }
        // 替换Quick3way，原生数组排序
        Arrays.sort(suffixes);
    }

    // 返回后缀数组长度
    public int length() {
        return N;
    }

    // 选中第i个后缀字符串
    public String select(int i) {
        return suffixes[i];
    }

    // 获取该后缀在原字符串中的起始下标
    public int index(int i) {
        return N - suffixes[i].length();
    }

    // 两个字符串最长公共前缀
    private static int lcp(String s, String t) {
        int minLen = Math.min(s.length(), t.length());
        for (int i = 0; i < minLen; i++) {
            if (s.charAt(i) != t.charAt(i)) {
                return i;
            }
        }
        return minLen;
    }

    // 第i项和前一项后缀的公共前缀长度
    public int lcp(int i) {
        if (i <= 0) return 0;
        return lcp(suffixes[i], suffixes[i - 1]);
    }

    // 二分查找：key在后缀数组中的排名
    public int rank(String key) {
        int lo = 0;
        int hi = N - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int cmp = key.compareTo(suffixes[mid]);
            if (cmp < 0) {
                hi = mid - 1;
            } else if (cmp > 0) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        return lo;
    }

    // 额外方法：查找全局最长重复子串
    public String longestRepeatedSubstring() {
        int maxLen = 0;
        String result = "";
        for (int i = 1; i < N; i++) {
            int len = lcp(i);
            if (len > maxLen) {
                maxLen = len;
                result = suffixes[i].substring(0, len);
            }
        }
        return result;
    }

    // 测试主函数
    public static void main(String[] args) {
        String text = "aacaagtttacaagc";
        SuffixArray0 sa = new SuffixArray0(text);
        System.out.println("最长重复子串：" + sa.longestRepeatedSubstring());
    }
}