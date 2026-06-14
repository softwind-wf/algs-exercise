package exercise1.exercise1_1;

public class BetweenZeroAndOne {
    public static void main(String[] args) {
        // 检查命令行参数是否正确传入
        if (args.length != 2) {
            System.out.println("用法: java BetweenZeroAndOne <x> <y>");
            System.out.println("请输入两个 double 类型的参数");
            return;
        }

        try {
            // 将命令行参数转换为 double 类型
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);

            // 核心判断逻辑
            boolean isInRange = (x > 0 && x < 1) && (y > 0 && y < 1);

            // 输出结果
            System.out.println(isInRange);
        } catch (NumberFormatException e) {
            System.out.println("错误: 请输入有效的数字");
        }
    }
}