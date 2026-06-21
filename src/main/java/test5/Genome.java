package test5;

import edu.princeton.cs.algs4.BinaryStdIn;
import edu.princeton.cs.algs4.BinaryStdOut;

public class Genome {
    private static Alphabet DNA = new Alphabet("ACTG");

    // 压缩：读取文本文件 → 输出二进制压缩文件
    public static void compress(String inFile, String outFile) {
        // 绑定输入输出文件
        BinaryStdIn.setFile(inFile);
        BinaryStdOut.setFile(outFile);

        String s = BinaryStdIn.readAllString(); // 一次性读完文件，不会阻塞
        int N = s.length();
        BinaryStdOut.write(N); // 写入原始长度

        for (int i = 0; i < N; i++) {
            int d = DNA.toIndex(s.charAt(i));
            BinaryStdOut.write(d, DNA.lgR());
        }
        // 必须关闭，刷新缓冲区释放文件
        BinaryStdOut.close();
        BinaryStdIn.close();
    }

    // 解压：读取压缩二进制 → 还原文本文件
    public static void expand(String inFile, String outFile) {
        BinaryStdIn.setFile(inFile);
        BinaryStdOut.setFile(outFile);

        int w = DNA.lgR();
        int N = BinaryStdIn.readInt();
        for (int i = 0; i < N; i++) {
            int c = BinaryStdIn.readChar(w);
            BinaryStdOut.write(DNA.toChar(c));
        }
        BinaryStdOut.close();
        BinaryStdIn.close();
    }

    public static void main(String[] args) {
        // 参数格式：
        // 压缩：- input.txt output.bin
        // 解压：+ input.bin output.txt
        if (args.length != 3) {
            System.out.println("参数错误！");
            System.out.println("压缩用法：java Genome - 原始DNA文本.txt 压缩文件.bin");
            System.out.println("解压用法：java Genome + 压缩文件.bin 还原文本.txt");
            return;
        }
        String op = args[0];
        String inPath = args[1];
        String outPath = args[2];

        try {
            if ("-".equals(op)) {
                compress(inPath, outPath);
                System.out.println("压缩完成！");
            } else if ("+".equals(op)) {
                expand(inPath, outPath);
                System.out.println("解压完成！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}