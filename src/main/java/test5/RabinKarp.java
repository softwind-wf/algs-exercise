package test5;

import java.util.ArrayList;
import java.util.List;

public class RabinKarp {
    private final String pat;
    private final int M;
    private final int R = 256;

    // 【关键优化：预定义现成超大质数，删除动态找素数逻辑，初始化瞬间完成】
    private static final long Q = 8999999963L; // 固定大素数，密码学常用
    private long patHash;
    private long RM;

    public RabinKarp(String pat) {
        this.pat = pat;
        this.M = pat.length();
        // 预计算 RM = R^(M-1) mod Q
        RM = 1;
        for (int i = 1; i <= M - 1; i++) {
            RM = (R * RM) % Q;
        }
        patHash = hash(pat, M);
    }

    // 拉斯维加斯校验，哈希命中才比对
    private boolean check(int pos, String txt) {
        for (int j = 0; j < M; j++) {
            if (txt.charAt(pos + j) != pat.charAt(j))
                return false;
        }
        return true;
    }

    // Horner哈希
    private long hash(String key, int len) {
        long h = 0;
        for (int j = 0; j < len; j++) {
            h = (h * R + key.charAt(j)) % Q;
        }
        return h;
    }

    // 查找第一个匹配
    public int search(String txt) {
        int N = txt.length();
        if (M > N) return N;
        long curHash = hash(txt, M);

        if (curHash == patHash && check(0, txt)) return 0;

        for (int i = M; i < N; i++) {
            // 滑动哈希，+Q防止负数
            curHash = (curHash + Q - RM * txt.charAt(i - M) % Q) % Q;
            curHash = (curHash * R + txt.charAt(i)) % Q;
            int start = i - M + 1;
            if (curHash == patHash && check(start, txt)) {
                return start;
            }
        }
        return N;
    }

    // 查找全部匹配
    public List<Integer> searchAll(String txt) {
        List<Integer> res = new ArrayList<>();
        int N = txt.length();
        if (M > N) return res;
        long curHash = hash(txt, M);

        if (curHash == patHash && check(0, txt)) {
            res.add(0);
        }

        for (int i = M; i < N; i++) {
            curHash = (curHash + Q - RM * txt.charAt(i - M) % Q) % Q;
            curHash = (curHash * R + txt.charAt(i)) % Q;
            int start = i - M + 1;
            if (curHash == patHash && check(start, txt)) {
                res.add(start);
            }
        }
        return res;
    }

    public static void main(String[] args) {
        String text = "ABCABCABCABXABCABC";
        String p = "ABC";
        RabinKarp rk = new RabinKarp(p);

        List<Integer> all = rk.searchAll(text);
        System.out.println("全部匹配下标：" + all);
    }
}