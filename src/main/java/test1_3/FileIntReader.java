package test1_3;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.Queue;
import edu.princeton.cs.algs4.StdOut;

public class FileIntReader {

    // 与算法4原书风格一致，但增加了错误处理
    public static int[] readInts(String fileName) {
        In in = new In(fileName);
        Queue<Integer> queue = new Queue<>();

        // 读取所有整数到队列
        while (!in.isEmpty()) {
            queue.enqueue(in.readInt());
        }

        // 处理空文件的情况
        if (queue.isEmpty()) {
            throw new IllegalArgumentException("文件中没有整数：" + fileName);
        }

        // 转换为数组
        int N = queue.size();
        int[] a = new int[N];
        for (int i = 0; i < N; i++) {
            a[i] = queue.dequeue();
        }

        return a;
    }

    // 主函数测试（算法4风格）
    public static void main(String[] args) {
        if (args.length != 1) {
            StdOut.println("用法：java FileIntReader <文件名>");
            return;
        }

        try {
            int[] numbers = readInts(args[0]);
            StdOut.println("读取到的整数：");
            for (int num : numbers) {
                StdOut.print(num + " ");
            }
            StdOut.println();
        } catch (Exception e) {
            StdOut.println("错误：" + e.getMessage());
        }
    }
}