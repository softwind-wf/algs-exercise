package exercise1.exercise1_2;

import edu.princeton.cs.algs4.Interval1D;
import edu.princeton.cs.algs4.Interval2D;
import edu.princeton.cs.algs4.StdDraw;
import edu.princeton.cs.algs4.StdRandom;

public class Interval2DClient {
    public static void main(String[] args) {
        // 从命令行参数读取 N, min, max
        int N = Integer.parseInt(args[0]);
        double min = Double.parseDouble(args[1]);
        double max = Double.parseDouble(args[2]);

        // 初始化绘图
        StdDraw.setCanvasSize(800, 800);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
        StdDraw.enableDoubleBuffering();

        Interval2D[] intervals = new Interval2D[N];
        Interval1D[] xIntervals = new Interval1D[N];
        Interval1D[] yIntervals = new Interval1D[N];

        // 生成 N 个随机 2D 间隔
        for (int i = 0; i < N; i++) {
            double x1 = StdRandom.uniformDouble(0, 1 - max);
            double x2 = x1 + StdRandom.uniformDouble(min, max);
            double y1 = StdRandom.uniformDouble(0, 1 - max);
            double y2 = y1 + StdRandom.uniformDouble(min, max);
            xIntervals[i] = new Interval1D(x1, x2);
            yIntervals[i] = new Interval1D(y1, y2);
            intervals[i] = new Interval2D(xIntervals[i], yIntervals[i]);
            intervals[i].draw();
        }

        StdDraw.show();

        // 统计相交和包含的区间对数量
        int intersectCount = 0;
        int containCount = 0;

        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                if (intervals[i].intersects(intervals[j])) {
                    intersectCount++;
                }
                if (intervals[i].contains(intervals[j]) || intervals[j].contains(intervals[i])) {
                    containCount++;
                }
            }
        }

        // 输出结果
        System.out.println("相交的区间对数量: " + intersectCount);
        System.out.println("有包含关系的区间对数量: " + containCount);
    }
}