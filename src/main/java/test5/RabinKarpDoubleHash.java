package test5;

import java.util.ArrayList;
import java.util.List;

public class RabinKarpDoubleHash {
    private static final int R = 256;
    private static final long Q1 = 1_000_000_007L;
    private static final long Q2 = 1_000_000_009L;

    private final char[] patChars;
    private final int M;
    private final long patHash1;
    private final long patHash2;
    private final long RM1;
    private final long RM2;

    public RabinKarpDoubleHash(String pattern) {
        this.patChars = pattern.toCharArray();
        this.M = patChars.length;

        // 合并计算 RM1 和 RM2
        long rm1 = 1, rm2 = 1;
        for (int i = 1; i < M; i++) {
            rm1 = (rm1 * R) % Q1;
            rm2 = (rm2 * R) % Q2;
        }
        this.RM1 = rm1;
        this.RM2 = rm2;

        this.patHash1 = hash(patChars, 0, M, Q1);
        this.patHash2 = hash(patChars, 0, M, Q2);
    }

    // 计算 char[] 指定范围的哈希值
    private static long hash(char[] s, int start, int len, long Q) {
        long h = 0;
        for (int i = 0; i < len; i++) {
            h = (h * R + s[start + i]) % Q;
        }
        return h;
    }

    // 验证匹配（拉斯维加斯）
    private boolean check(int start, char[] txtChars) {
        for (int i = 0; i < M; i++) {
            if (txtChars[start + i] != patChars[i]) return false;
        }
        return true;
    }

    // 滑动更新哈希（原地更新两个模数的哈希值）
    private static void updateHash(long[] curH, long RM, long Q, char outChar, char inChar) {
        long h = curH[0];
        h = (h + Q - (RM * outChar) % Q) % Q;
        h = (h * R + inChar) % Q;
        curH[0] = h;
    }

    // 查找第一个匹配位置
    public int search(String text) {
        char[] txtChars = text.toCharArray();
        int N = txtChars.length;
        if (M > N) return N;

        // 初始窗口双哈希
        long curH1 = hash(txtChars, 0, M, Q1);
        long curH2 = hash(txtChars, 0, M, Q2);

        if (curH1 == patHash1 && curH2 == patHash2 && check(0, txtChars)) {
            return 0;
        }

        // 滑动窗口
        for (int i = M; i < N; i++) {
            int start = i - M + 1;
            // 同时更新两个哈希（用数组传递引用）
            long[] h1 = {curH1}, h2 = {curH2};
            updateHash(h1, RM1, Q1, txtChars[i - M], txtChars[i]);
            updateHash(h2, RM2, Q2, txtChars[i - M], txtChars[i]);
            curH1 = h1[0];
            curH2 = h2[0];

            if (curH1 == patHash1 && curH2 == patHash2 && check(start, txtChars)) {
                return start;
            }
        }
        return N;
    }

    // 查找全部匹配下标
    public List<Integer> searchAll(String text) {
        List<Integer> result = new ArrayList<>();
        char[] txtChars = text.toCharArray();
        int N = txtChars.length;
        if (M > N) return result;

        long curH1 = hash(txtChars, 0, M, Q1);
        long curH2 = hash(txtChars, 0, M, Q2);

        if (curH1 == patHash1 && curH2 == patHash2 && check(0, txtChars)) {
            result.add(0);
        }

        for (int i = M; i < N; i++) {
            int start = i - M + 1;
            long[] h1 = {curH1}, h2 = {curH2};
            updateHash(h1, RM1, Q1, txtChars[i - M], txtChars[i]);
            updateHash(h2, RM2, Q2, txtChars[i - M], txtChars[i]);
            curH1 = h1[0];
            curH2 = h2[0];

            if (curH1 == patHash1 && curH2 == patHash2 && check(start, txtChars)) {
                result.add(start);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        String text = "ABCABCABCAXABCABCABC";
        String pattern = "ABC";
        RabinKarpDoubleHash rk = new RabinKarpDoubleHash(pattern);

        int first = rk.search(text);
        List<Integer> all = rk.searchAll(text);

        System.out.println("首个匹配下标：" + first);
        System.out.println("全部匹配下标：" + all);
    }
}