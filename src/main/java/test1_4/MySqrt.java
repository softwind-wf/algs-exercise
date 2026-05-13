package test1_4;



public class MySqrt {
    public static void main(String[] args) {
        double number = 10;      // 要开方的数
        double epsilon = 1e-15; // 精度要求
        int maxIterations = 1000; // 最大迭代次数
        int iterations = 0;

        // 输入合法性检查
        if (number < 0) {
            System.out.println("错误：不能计算负数的平方根！");
            return;
        }

        // 合理的初始猜测值
        double x = number > 1 ? number : 1;

        // 牛顿迭代法
        while (Math.abs(x * x - number) > epsilon && iterations < maxIterations) {
            x = (x + number / x) / 2;
            iterations++;
        }

        // 检查是否达到最大迭代次数
        if (iterations == maxIterations) {
            System.out.println("警告：达到最大迭代次数，结果可能不准确！");
        }

        System.out.println("我算的 √" + number + " = " + x);
        System.out.println("真实的 √" + number + " ≈ " + Math.sqrt(number));
    }
}



