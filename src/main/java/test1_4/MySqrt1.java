package test1_4;

public class MySqrt1 {
    public static void main(String[] args) {
        double number = 8;      // 要开方的数
        double x = number / 2;  // 先随便猜一个数：一半
        double step = 0.1;      // 每次调整一点点

        // 循环100次，足够精确了
        for (int i = 0; i < 1000; i++) {
            double square = x * x;

            // 已经很接近，就退出
            if (Math.abs(square - number) < 1e-10) {
                break;
            }

            // 你的核心逻辑！
            if (square > number) {
                x = x - step;   // 大了，减一点
            } else {
                x = x + step;   // 小了，加一点
            }

            step = step * 0.9;  // 越往后，调整越小
        }

        System.out.println("我算的 √" + number + " = " + x);
        System.out.println("真实的 √" + number + " ≈ " +Math.sqrt(8));
    }
}