package test5;

import edu.princeton.cs.algs4.BinaryStdIn;
import edu.princeton.cs.algs4.BinaryStdOut;

public class RunLength {
    // 压缩：原始比特流 → 游程编码二进制文件
    public static void compress() {
        char cnt = 0;
        boolean b, old = false;
        while (!BinaryStdIn.isEmpty()) {
            b = BinaryStdIn.readBoolean();
            // 当前比特和上一个不同，写入计数值，重置计数器
            if (b != old) {
                BinaryStdOut.write(cnt);
                cnt = 0;
                old = !old;
            } else {
                // 连续相同比特达到255上限，分段存储
                if (cnt == 255) {
                    BinaryStdOut.write(cnt);
                    cnt = 0;
                    BinaryStdOut.write(cnt);
                }
            }
            cnt++;
        }
        // 写入最后一段游程长度
        BinaryStdOut.write(cnt);
        BinaryStdOut.close();
    }

    // 解压：游程编码二进制 → 还原原始比特流
    public static void expand() {
        boolean b = false;
        while (!BinaryStdIn.isEmpty()) {
            char cnt = BinaryStdIn.readChar();
            // 循环打印cnt个当前比特
            for (int i = 0; i < cnt; i++) {
                BinaryStdOut.write(b);
            }
            b = !b;
        }
        BinaryStdOut.close();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("使用说明：");
            System.out.println("压缩：java RunLength - input.bin output.rle");
            System.out.println("解压：java RunLength + input.rle restore.bin");
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
                System.out.println("游程压缩完成！");
            } else if ("+".equals(op)) {
                expand();
                System.out.println("游程解压完成！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            BinaryStdIn.close();
            BinaryStdOut.close();
        }
    }
}