package test5;

import edu.princeton.cs.algs4.BinaryStdIn;
import edu.princeton.cs.algs4.BinaryStdOut;

public class LZW {
    // 字符集大小 8bit ASCII
    private static final int R = 256;
    // 编码总数上限 2^12
    private static final int L = 4096;
    // 单个编码比特宽度 12位
    private static final int W = 12;

    // 【图片1：压缩方法 compress()】
    public static void compress() {
        String input = BinaryStdIn.readString();
        TST<Integer> st = new TST<>();

        // 初始化：单个字符存入符号表，编码0~255
        for (int i = 0; i < R; i++) {
            st.put("" + (char) i, i);
        }
        int code = R + 1; // R 是EOF结束标记编码

        while (input.length() > 0) {
            // 找到输入中最长匹配前缀
            String s = st.longestPrefixOf(input);
            // 输出s对应的12位编码
            BinaryStdOut.write(st.get(s), W);

            int t = s.length();
            // 还有剩余字符，且编码未达上限，把s+下一个字符加入符号表
            if (t < input.length() && code < L) {
                st.put(input.substring(0, t + 1), code++);
            }
            // 输入截去已匹配前缀
            input = input.substring(t);
        }
        // 写入文件结束标记EOF编码R
        BinaryStdOut.write(R, W);
        BinaryStdOut.close();
    }

    // 【图片2：解压方法 expand()】
    public static void expand() {
        String[] st = new String[L];
        int i; // 下一个待分配的编码

        // 初始化单字符编码 0~255
        for (i = 0; i < R; i++) {
            st[i] = "" + (char) i;
        }
        st[i++] = ""; // EOF占位（未使用）

        int codeword = BinaryStdIn.readInt(W);
        String val = st[codeword];

        while (true) {
            BinaryStdOut.write(val);
            codeword = BinaryStdIn.readInt(W);
            if (codeword == R) break; // 读到结束标记，退出循环

            String s = st[codeword];
            // 经典LZW异常：前瞻编码不存在，特殊构造字符串
            if (i == codeword) {
                s = val + val.charAt(0);
            }
            // 编码表未满，新增条目
            if (i < L) {
                st[i++] = val + s.charAt(0);
            }
            val = s;
        }
        BinaryStdOut.close();
    }

    // 程序入口，命令行参数区分压缩/解压
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("使用方法：");
            System.out.println("压缩：java LZW - 输入文本.txt 压缩文件.lzw");
            System.out.println("解压：java LZW + 压缩文件.lzw 还原文本.txt");
            return;
        }
        String op = args[0];
        String inPath = args[1];
        String outPath = args[2];
        try {
            BinaryStdIn.setFile(inPath);
            BinaryStdOut.setFile(outPath);
            if ("-".equals(op)) {
                compress();
                System.out.println("LZW压缩完成！");
            } else if ("+".equals(op)) {
                expand();
                System.out.println("LZW解压完成！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            BinaryStdIn.close();
            BinaryStdOut.close();
        }
    }
}