package exercise1.exercise1_1;

import edu.princeton.cs.algs4.StdDraw;
import edu.princeton.cs.algs4.StdIn;

public class Histogram {
    public static void main(String[] args) {
        // 解析命令行参数
        int N = Integer.parseInt(args[0]);
        double l = Double.parseDouble(args[1]);
        double r = Double.parseDouble(args[2]);
        double interval = (r - l) / N;

        // 初始化计数数组
        int[] counts = new int[N];

        // 读取输入并统计
        while (!StdIn.isEmpty()) {
            double val = StdIn.readDouble();
            if (val >= l && val < r) {
                int bin = (int) ((val - l) / interval);
                counts[bin]++;
            }
        }

        // 找到最大计数，用于缩放Y轴
        int maxCount = 0;
        for (int count : counts) {
            if (count > maxCount) maxCount = count;
        }

        // 设置画布与坐标
        StdDraw.setCanvasSize(800, 600);
        StdDraw.setXscale(l, r);
        StdDraw.setYscale(0, maxCount * 1.1);

        // 绘制直方图
        for (int i = 0; i < N; i++) {
            double x = l + i * interval;
            double width = interval * 0.9;
            double height = counts[i];
            StdDraw.filledRectangle(x + width / 2, height / 2, width / 2, height / 2);
        }
    }
}