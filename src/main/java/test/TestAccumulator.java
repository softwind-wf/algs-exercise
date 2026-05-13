package test;

public class TestAccumulator {
    public static void main(String[] args) {
        // 从命令行参数获取数据点数量
        if (args.length != 1) {
            System.out.println("Usage: java TestAccumulator <number_of_values>");
            return;
        }

        int T = Integer.parseInt(args[0]);
        Accumulator a = new Accumulator();

        // 生成 T 个 0~1 之间的随机数并累加
        for (int t = 0; t < T; t++) {
            // 生成 [0,1) 之间的随机数
            double randomValue = Math.random();
            a.addDataValue(randomValue);
        }

        // 输出结果
        System.out.println(a);
    }
}