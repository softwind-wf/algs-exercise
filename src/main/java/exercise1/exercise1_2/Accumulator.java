package exercise1.exercise1_2;

public class Accumulator {
    private double m; // 当前均值
    private double s; // 累积的方差中间值
    private int N;    // 数据点数量

    // 添加一个新的数据点
    public void addDataValue(double x) {
        N++;
        s = s + 1.0 * (N - 1) / N * (x - m) * (x - m);
        m = m + (x - m) / N;
    }

    // 返回均值
    public double mean() {
        return m;
    }

    // 返回样本方差（除以 N-1）
    public double var() {
        return s / (N - 1);
    }

    // 返回样本标准差
    public double stddev() {
        return Math.sqrt(this.var());
    }

    // 测试用例
    public static void main(String[] args) {
        Accumulator acc = new Accumulator();
        // 测试数据：1, 2, 3, 4, 5
        acc.addDataValue(1);
        acc.addDataValue(2);
        acc.addDataValue(3);
        acc.addDataValue(4);
        acc.addDataValue(5);

        System.out.println("均值: " + acc.mean());
        System.out.println("方差: " + acc.var());
        System.out.println("标准差: " + acc.stddev());
    }
}