package exercise1.exercise1_2;

import edu.princeton.cs.algs4.StdDraw;

public class VisualCounter {
    private final int maxOperations;  // 最大操作次数
    private final int maxAbsoluteValue;  // 计数器的最大绝对值
    private int count;  // 当前计数值
    private int operations;  // 已执行的操作次数

    // 构造函数
    public VisualCounter(int N, int max) {
        this.maxOperations = N;
        this.maxAbsoluteValue = max;
        this.count = 0;
        this.operations = 0;

        // 初始化绘图窗口
        StdDraw.setXscale(0, N);
        StdDraw.setYscale(-max - 1, max + 1);
        StdDraw.setPenRadius(0.005);
    }

    // 加一操作
    public void increment() {
        if (operations >= maxOperations) return;
        if (Math.abs(count + 1) > maxAbsoluteValue) return;
        count++;
        operations++;
        draw();
    }

    // 减一操作
    public void decrement() {
        if (operations >= maxOperations) return;
        if (Math.abs(count - 1) > maxAbsoluteValue) return;
        count--;
        operations++;
        draw();
    }

    // 绘图辅助方法
    private void draw() {
        // 画当前点
        StdDraw.point(operations, count);
        // 画一条从上一个点到当前点的连线
//        if (operations > 1) {
//            StdDraw.line(operations - 1, count - (count - (this.count - (this.count > 0 ? 1 : -1))), operations, count);
//        }
    }

    // 获取当前计数值
    public int tally() {
        return count;
    }

    // 测试用例
    public static void main(String[] args) {
        int N = 100;
        int max = 10;
        VisualCounter counter = new VisualCounter(N, max);

        // 模拟一些随机操作
        for (int i = 0; i < N; i++) {
            if (Math.random() > 0.5) {
                counter.increment();
            } else {
                counter.decrement();
            }
        }

        System.out.println("最终计数值: " + counter.tally());
    }
}