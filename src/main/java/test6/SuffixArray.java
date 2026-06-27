package test6;

public class SuffixArray {
    private final int N;
    private final int[] sa;
    private final int[] lcpArr;
    private final String str;

    public SuffixArray(String s) {
        this.str = s;
        this.N = s.length();
        if (N == 0) {
            sa = new int[0];
            lcpArr = new int[0];
            return;
        }
        this.sa = buildSA(s);
        this.lcpArr = buildLCP(s, this.sa);
    }

    private static int[] buildSA(String s) {
        int n = s.length();
        int[] sa = new int[n];
        int[] rank = new int[n];
        int[] tmp = new int[n];

        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = s.charAt(i);
        }

        for (int k = 1; k < n; k <<= 1) {
            final int kk = k;
            final int[] rk = rank.clone();
            countingSort(sa, rk, kk, n);
            countingSort(sa, rk, 0, n);

            tmp[sa[0]] = 0;
            for (int i = 1; i < n; i++) {
                int prev = sa[i - 1], cur = sa[i];
                int rp1 = rk[prev], rp2 = prev + kk < n ? rk[prev + kk] : -1;
                int rc1 = rk[cur], rc2 = cur + kk < n ? rk[cur + kk] : -1;
                tmp[cur] = tmp[prev] + ((rc1 == rp1 && rc2 == rp2) ? 0 : 1);
            }
            System.arraycopy(tmp, 0, rank, 0, n);
            if (rank[sa[n - 1]] == n - 1) break;
        }
        return sa;
    }

    private static void countingSort(int[] sa, int[] rank, int offset, int n) {
        int maxVal = 256;
        for (int i = 0; i < n; i++) {
            int pos = i + offset;
            int r = pos < n ? rank[pos] + 1 : 0;
            if (r > maxVal) maxVal = r;
        }
        int[] cnt = new int[maxVal + 1];
        for (int i = 0; i < n; i++) {
            int pos = i + offset;
            cnt[pos < n ? rank[pos] + 1 : 0]++;
        }
        for (int i = 1; i <= maxVal; i++) cnt[i] += cnt[i - 1];
        int[] res = new int[n];
        for (int i = n - 1; i >= 0; i--) {
            int pos = sa[i] + offset;
            res[--cnt[pos < n ? rank[pos] + 1 : 0]] = sa[i];
        }
        System.arraycopy(res, 0, sa, 0, n);
    }

    // ---------- Kasai 计算 LCP（线性） ----------
    private int[] buildLCP(String s, int[] sa) {
        int n = s.length();
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        int[] lcp = new int[n];
        int h = 0;
        for (int i = 0; i < n; i++) {
            int r = rank[i];
            if (r == 0) continue;
            int j = sa[r - 1];
            while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
            lcp[r] = h;
            if (h > 0) h--;
        }
        return lcp;
    }

    // ---------- 公共方法 ----------
    public int length() { return N; }
    public String select(int i) { return str.substring(sa[i]); }
    public int index(int i) { return sa[i]; }
    public int lcp(int i) { return (i <= 0 || i >= N) ? 0 : lcpArr[i]; }

    public int rank(String key) {
        int lo = 0, hi = N - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int cmp = key.compareTo(str.substring(sa[mid]));
            if (cmp < 0) hi = mid - 1;
            else if (cmp > 0) lo = mid + 1;
            else return mid;
        }
        return lo;
    }

    public String longestRepeatedSubstring() {
        int maxLen = 0, idx = 0;
        for (int i = 1; i < N; i++) {
            if (lcpArr[i] > maxLen) {
                maxLen = lcpArr[i];
                idx = i;
            }
        }
        return maxLen == 0 ? "" : str.substring(sa[idx], sa[idx] + maxLen);
    }

    public static void main(String[] args) {
        String text = "aacaagtttacaagc";
        SuffixArray sa = new SuffixArray(text);
        System.out.println("最长重复子串：" + sa.longestRepeatedSubstring()); // 输出 "acaag"
    }
}