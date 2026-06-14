package exercise1.exercise1_2;

import edu.princeton.cs.algs4.Interval1D;
import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;

public class Interval1DClient {
    public static void main(String[] args) {
        // 从命令行参数读取区间数量 N
        int N = Integer.parseInt(args[0]);
        Interval1D[] intervals = new Interval1D[N];

        // 从标准输入读取 N 个区间
        for (int i = 0; i < N; i++) {
            double left = StdIn.readDouble();
            double right = StdIn.readDouble();
            intervals[i] = new Interval1D(left, right);
        }

        // 检查所有区间对是否相交
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                if (intervals[i].intersects(intervals[j])) {
                    StdOut.println(intervals[i] + " intersects " + intervals[j]);
                }
            }
        }
    }
}