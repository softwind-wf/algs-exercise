package test;

import edu.princeton.cs.algs4.StdDraw;

public class VisualAccumulator {
    private double total;
    private int N;

    // 构造函数：初始化绘图区域
    public VisualAccumulator(int trials, double max) {
        StdDraw.setXscale(0, trials);
        StdDraw.setYscale(0, max);
        StdDraw.setPenRadius(0.005);
    }

    // 添加数据并更新绘图
    public void addDataValue(double val) {
        N++;
        total += val;
        // 绘制灰色原始数据点
        StdDraw.setPenColor(StdDraw.DARK_GRAY);
        StdDraw.point(N, val);
        // 绘制红色平均值点
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.point(N, total / N);
    }

    // 计算平均值
    public double mean() {
        return N == 0 ? 0.0 : total / N;
    }

    // 返回对象的字符串表示
    @Override
    public String toString() {
        return String.format("Mean (%d values): %.5f", N, mean());
    }
}