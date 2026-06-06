package test5;

import java.util.ArrayList;
import java.util.List;

public class KMP {
    private String pat;
    private int[] lps;   // 部分匹配表：lps[i] = 最长公共前后缀长度（不含自身）

    // 构造部分匹配表（标准 π 函数，无优化）
    public KMP(String pat) {
        this.pat = pat;
        int m = pat.length();
        lps = new int[m];
        int len = 0;      // 当前最长公共前后缀长度
        int i = 1;
        while (i < m) {
            if (pat.charAt(i) == pat.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
    }

    // 搜索第一个匹配位置
    public int search(String txt) {
        int i = 0, j = 0;
        int n = txt.length(), m = pat.length();
        while (i < n && j < m) {
            if (txt.charAt(i) == pat.charAt(j)) {
                i++;
                j++;
            } else {
                if (j != 0) j = lps[j - 1];
                else i++;
            }
        }
        return j == m ? i - m : n;
    }

    // 搜索所有匹配位置（支持重叠匹配）
    public List<Integer> searchAll(String txt) {
        List<Integer> res = new ArrayList<>();
        int i = 0, j = 0;
        int n = txt.length(), m = pat.length();
        while (i < n) {
            if (txt.charAt(i) == pat.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    res.add(i - m);
                    // 关键：重叠匹配，回退到最长公共前后缀的长度
                    j = lps[j - 1];
                }
            } else {
                if (j != 0) j = lps[j - 1];
                else i++;
            }
        }
        return res;
    }

    // 测试
    public static void main(String[] args) {
        String pattern = "AACAA";
        String text = "AABRAACAACAADABRAACAADABRA";
        KMP kmp = new KMP(pattern);
        System.out.println("第一个匹配位置：" + kmp.search(text));
        System.out.println("所有匹配位置：" + kmp.searchAll(text));

        // 重叠匹配测试
        String pat2 = "AA";
        String txt2 = "AAAAA";
        KMP kmp2 = new KMP(pat2);
        System.out.println("\n重叠匹配 'AA' 在 'AAAAA' 中：" + kmp2.searchAll(txt2));

        // 另一个例子：ABABAC
        String pat3 = "ABABAC";
        String txt3 = "XXABABABACXXX";
        KMP kmp3 = new KMP(pat3);
        System.out.println("ABABAC 首次匹配位置：" + kmp3.search(txt3));
        System.out.println("ABABAC 所有匹配位置：" + kmp3.searchAll(txt3));
    }
}