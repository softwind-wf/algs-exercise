package test5;

import edu.princeton.cs.algs4.BinaryStdOut;

public class GenTestBitmap {
    public static void main(String[] args) {
        // 输出原始位图二进制文件
        BinaryStdOut.setFile("q32x48.bin");
        // 模拟书本32×48位图：17个0、7个1交替循环（简化示例）
        int[][] runList = {
                {17, 0}, {7, 1}, {10, 0}, {5, 1}, {9, 0}, {5, 1},
                {12, 0}, {5, 1}, {13, 0}, {5, 1}, {14, 0}, {5, 1},
                {15, 0}, {5, 1}, {15, 0}, {9, 1}, {15, 0}, {5, 1},
                {15, 0}, {5, 1}, {15, 0}, {5, 1}, {14, 0}, {6, 1},
                {13, 0}, {7, 1}, {12, 0}, {5, 1}, {10, 0}, {5, 1},
                {20, 0}, {2, 1}, {5, 0}, {5, 1}, {5, 0}, {5, 1},
                {5, 0}, {5, 1}, {5, 0}, {5, 1}, {5, 0}, {5, 1},
                {5, 0}, {5, 1}, {5, 0}, {5, 1}, {5, 0}, {5, 1},
                {5, 0}, {5, 1}, {5, 0}, {5, 1}, {7, 0}, {4, 1},
                {14, 0}, {1, 1}, {32, 0}
        };
        boolean curBit = false;
        for (int[] run : runList) {
            int len = run[0];
            int bitVal = run[1];
            for (int i = 0; i < len; i++) {
                BinaryStdOut.write(bitVal == 1);
            }
        }
        BinaryStdOut.close();
        System.out.println("原始位图 q32x48.bin 生成完成！");
    }
}