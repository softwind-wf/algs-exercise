package test;

import edu.princeton.cs.algs4.Counter;
import edu.princeton.cs.algs4.Interval1D;
import edu.princeton.cs.algs4.Interval2D;
import edu.princeton.cs.algs4.Point2D;
import edu.princeton.cs.algs4.StdDraw;
import edu.princeton.cs.algs4.StdOut;

public class Interval2DTest {
    public static void main(String[] args) {
        // 从命令行参数读取输入
        double xlo = Double.parseDouble(args[0]);
        double xhi = Double.parseDouble(args[1]);
        double ylo = Double.parseDouble(args[2]);
        double yhi = Double.parseDouble(args[3]);
        int T = Integer.parseInt(args[4]);

        // 创建区间和二维矩形
        Interval1D xinterval = new Interval1D(xlo, xhi);
        Interval1D yinterval = new Interval1D(ylo, yhi);
        Interval2D box = new Interval2D(xinterval, yinterval);
        
        // 绘制矩形
        box.draw();

        // 初始化计数器
        Counter c = new Counter("hits");

        // 随机生成点并检测是否在矩形内
        for (int t = 0; t < T; t++) {
            double x = Math.random();
            double y = Math.random();
            Point2D p = new Point2D(x, y);
            if (box.contains(p)) {
                c.increment();
            } else {
                // 绘制不在矩形内的点
                p.draw();
            }
        }

        // 输出结果
        StdOut.println(c);
        StdOut.println(box.area());
    }
}