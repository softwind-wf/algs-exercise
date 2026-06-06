package test5;

import java.util.ArrayList;
import java.util.List;

public class KMP0 {
    private String pat;      // 模式串
    private int[][] dfa;     // DFA状态转移数组：dfa[字符][状态] = 下一状态
    private int R = 256;// ASCII字符集大小
    private int borderLen;

    // 构造函数：由模式串构建DFA
    public KMP0(String pat) {
        this.pat = pat;
        int M = pat.length();
        dfa = new int[R][M];
        // 初始：第0个字符匹配成功跳转到状态1
        dfa[pat.charAt(0)][0] = 1;

        // X：重启状态（最长前后缀匹配的状态）
        int X = 0;
        for (int j = 1; j < M; j++) {
            // 1. 所有字符：失配时继承重启状态X的转移（复制失败值）
            for (int c = 0; c < R; c++) {
                dfa[c][j] = dfa[c][X];
            }
            // 2. 当前字符匹配成功，状态+1
            dfa[pat.charAt(j)][j] = j + 1;
            // 3. 更新重启状态X = 当前字符在旧X上的跳转状态
            X = dfa[pat.charAt(j)][X];
        }

        borderLen=X;
    }

    // 在txt中搜索模式pat，返回起始下标；无匹配返回txt长度
    public int search(String txt) {
        int i, j, N = txt.length(), M = pat.length();
        for (i = 0, j = 0; i < N && j < M; i++) {
            // 根据当前字符+当前状态，跳转下一状态
            j = dfa[txt.charAt(i)][j];
        }
        // j走到模式末尾代表匹配成功，返回起始索引；否则没找到
        if (j == M) return i - M;
        else return N;
    }

    // 查找全部匹配下标
    public List<Integer> searchAll(String txt) {
        List<Integer> res = new ArrayList<>();
        int N = txt.length(), M = pat.length();
        int j = 0;
        for (int i = 0; i < N; i++) {
            j = dfa[txt.charAt(i)][j];
            if (j == M) {
                res.add(i - M + 1);
                // 找到一个后，利用重启状态继续向后找下一个
                j = borderLen;
            }
        }
        return res;
    }

    // 测试入口
    public static void main(String[] args) {
        // 书本示例用例：pattern=AACAA，text=AABRAACADABRAACAADABRA
        String pattern = "AACAA";
        String text = "AABRAACAACAABRAACAADABRA";

        KMP0 kmp = new KMP0(pattern);
        int index = kmp.search(text);

        if (index != text.length()) {
            System.out.println("匹配成功！起始下标：" + index);
            System.out.println("文本截取：" + text.substring(index, index + pattern.length()));
        } else {
            System.out.println("未找到匹配子串");
        }

        List<Integer> allPos=kmp.searchAll(text);
        System.out.println("所有匹配："+allPos);

        // 额外测试书本DFA样例：ABABAC
        String pat2 = "ABABAC";
        String txt2 = "XXABABABACXXX";
        KMP0 kmp2 = new KMP0(pat2);
        int idx2 = kmp2.search(txt2);
        System.out.println("\nABABAC匹配位置：" + idx2);
    }
}