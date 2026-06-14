package test;

public class Accumulator {
    // 维护两个状态：数据数量和数据总和
    private int count;
    private double sum;

    // 构造函数：初始化累加器
    public Accumulator() {
        this.count = 0;
        this.sum = 0.0;
    }

    // 添加一个新的数据值
    public void addDataValue(double val) {
        count++;
        sum += val;
    }

    // 计算并返回平均值
    public double mean() {
        return count == 0 ? 0.0 : sum / count;
    }

    // 返回对象的字符串表示
    @Override
    public String toString() {
        return String.format("Mean (%d values): %.5f", count, mean());
    }
}