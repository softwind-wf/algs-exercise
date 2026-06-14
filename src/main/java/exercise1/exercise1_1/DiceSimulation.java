package exercise1.exercise1_1;

public class DiceSimulation {
    public static void main(String[] args) {
        int SIDES = 6;
        int N = 1000000; // 可以调整这个值来测试不同次数的效果
        int[] count = new int[2 * SIDES + 1];

        // 模拟 N 次掷骰子
        for (int i = 0; i < N; i++) {
            int die1 = 1 + (int) (Math.random() * SIDES);
            int die2 = 1 + (int) (Math.random() * SIDES);
            count[die1 + die2]++;
        }

        // 输出理论概率和经验频率
        System.out.println("和为k\t理论概率\t经验频率");
        for (int k = 2; k <= 2 * SIDES; k++) {
            double theoretical = (k - 1 < SIDES ? k - 1 : 2 * SIDES - k + 1) / 36.0;
            double empirical = count[k] / (double) N;
            System.out.printf("%d\t%.4f\t%.4f\n", k, theoretical, empirical);
        }
    }
}