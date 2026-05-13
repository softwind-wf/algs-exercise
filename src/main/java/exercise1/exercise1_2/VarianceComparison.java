package exercise1.exercise1_2;

public class VarianceComparison {
    public static void main(String[] args) {
        // 测试1：普通数据
        testWithData(new double[]{1, 2, 3, 4, 5});
        // 测试2：大数数据（最能体现稳定性差异）
        testWithData(new double[]{1000000, 1000001, 1000002, 1000003, 1000004});
    }

    private static void testWithData(double[] data) {
        // 递推法（Accumulator）
        Accumulator acc = new Accumulator();
        for (double x : data) acc.addDataValue(x);

        // 传统法
        int n = data.length;
        double sum = 0.0, sumSq = 0.0;
        for (double x : data) {
            sum += x;
            sumSq += x * x;
        }
        double meanTraditional = sum / n;
        double varTraditional = (sumSq / n) - (meanTraditional * meanTraditional);

        // 输出对比
        System.out.println("数据: " + java.util.Arrays.toString(data));
        System.out.println("递推法均值: " + acc.mean());
        System.out.println("递推法方差: " + acc.var());
        System.out.println("传统法均值: " + meanTraditional);
        System.out.println("传统法方差: " + varTraditional);
        System.out.println("------------------------------");
    }
}