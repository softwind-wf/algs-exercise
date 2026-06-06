package test5;

/**
 * Boyer-Moore 字符串匹配算法（坏字符规则实现，启发式向后跳跃）
 * 核心：预处理生成right数组：保存每个字符在模式串中最靠右下标，不在模式则为-1
 * 匹配从模式末尾向前比对，失配时根据坏字符计算文本指针i需要跳跃的距离
 */
public class BoyerMoore {
    // right[字符ASCII] = 该字符在模式串中最右侧索引，无则-1
    private int[] right;
    // 模式串
    private String pattern;

    /**
     * 构造函数：预处理模式串，生成坏字符跳跃表right
     * @param pat 待匹配的模式字符串
     */
    public BoyerMoore(String pat) {
        this.pattern = pat;
        int m = pat.length();
        final int ASCII_RANGE = 256; // 基础ASCII字符总数
        right = new int[ASCII_RANGE];

        // 1. 全部字符默认初始化为-1：代表字符不在模式串中
        for (int c = 0; c < ASCII_RANGE; c++) {
            right[c] = -1;
        }
        // 2. 遍历模式，记录每个字符最后出现的下标（靠右优先）
        for (int j = 0; j < m; j++) {
            char ch = pat.charAt(j);
            right[ch] = j;
        }
    }

    /**
     * 在文本串中查找模式串首次出现下标
     * @param text 待检索的文本字符串
     * @return 匹配起始下标；找不到返回text.length()
     */
    public int search(String text) {
        int n = text.length();
        int m = pattern.length();
        // 模式串空串特殊处理
        if (m == 0) return 0;
        if (n < m) return n;

        int skipLen; // 本轮匹配失败需要向后跳跃的步数
        // i：文本串和模式串左端对齐的起始位置
        for (int i = 0; i <= n - m; i += skipLen) {
            skipLen = 0;
            // 从模式末尾【从右往左】逐字符比对
            for (int j = m - 1; j >= 0; j--) {
                char txtCh = text.charAt(i + j);
                char patCh = pattern.charAt(j);
                if (patCh != txtCh) {
                    // 坏字符公式：skip = 当前失配位置j - 坏字符在模式最右位置
                    skipLen = j - right[txtCh];
                    // 保证至少右移1位，避免原地死循环
                    if (skipLen < 1) skipLen = 1;
                    break;
                }
            }
            // skip=0：全部字符匹配成功，返回起始下标i
            if (skipLen == 0) {
                return i;
            }
        }
        // 全文无匹配，返回文本长度
        return n;
    }

    // 查找所有匹配位置（拓展方法：找出全部匹配下标）
    public java.util.List<Integer> searchAll(String text) {
        java.util.List<Integer> res = new java.util.ArrayList<>();
        int idx = 0;
        int n = text.length();
        int m = pattern.length();
        while (idx <= n - m) {
            int pos = search(text.substring(idx));
            if (pos == n - idx) break;
            res.add(idx + pos);
            idx += pos + 1; // 向后移动一位继续查找下一个
        }
        return res;
    }

    /**
     * 从start位置开始往后搜索第一个匹配
     * @param text 完整文本
     * @param start 起始查找下标
     * @return 匹配下标，无匹配返回text.length()
     */
    private int search(String text, int start) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return start;
        // 剩余字符不够匹配
        if (start > n - m) return n;

        int skipLen;
        for (int i = start; i <= n - m; i += skipLen) {
            skipLen = 0;
            for (int j = m - 1; j >= 0; j--) {
                char txtCh = text.charAt(i + j);
                char patCh = pattern.charAt(j);
                if (patCh != txtCh) {
                    skipLen = j - right[txtCh];
                    if (skipLen < 1) skipLen = 1;
                    break;
                }
            }
            if (skipLen == 0) {
                return i;
            }
        }
        return n;
    }

    // 原有search对外方法不变，兼容老调用
    public int search1(String text) {
        return search(text, 0);
    }

    // 优化后的searchAll，不再substring
    public java.util.List<Integer> searchAll1(String text) {
        java.util.List<Integer> res = new java.util.ArrayList<>();
        int idx = 0;
        int n = text.length();
        int m = pattern.length();

        while (idx <= n - m) {
            // 直接从idx开始查找，不截取子串
            int pos = search(text, idx);
            // pos等于文本长度=找不到了
            if (pos == n) break;
            res.add(pos);
            // 重叠查找：下次从pos+1开始
            idx = pos + 1;
            // 不重叠查找换成：idx = pos + m;
        }
        return res;
    }

    public static void main(String[] args) {
        // 测试用例1：常规匹配
        String txt1 = "ABABACAABABACX";
        String pat1 = "ABABAC";
        BoyerMoore bm1 = new BoyerMoore(pat1);
        int findIdx = bm1.search(txt1);
        System.out.println("首次匹配下标：" + findIdx);
        System.out.println("全部匹配下标：" + bm1.searchAll(txt1));

        // 测试用例2：无匹配场景
        String txt2 = "ABCDEFG";
        String pat2 = "XYZ";
        BoyerMoore bm2 = new BoyerMoore(pat2);
        System.out.println("无匹配返回值：" + bm2.search(txt2));

        // 测试用例3：首字符就失配大幅跳跃
        String txt3 = "EEEEEEEEA";
        String pat3 = "A";
        BoyerMoore bm3 = new BoyerMoore(pat3);
        System.out.println("单字符匹配下标：" + bm3.search(txt3));
    }
}