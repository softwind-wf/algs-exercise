package exercise2.exercise2_5;

import java.util.PriorityQueue;
import java.util.Comparator;

// 定义可积函数接口
interface DoubleFunction {
    double apply(double x);
}

// 代表一个积分区间，包含当前近似值和误差估计
class Interval {
    double a, b;      // 区间端点
    double s;         // 当前区间的积分近似值
    double error;     // 误差估计

    public Interval(double a, double b, double s, double error) {
        this.a = a;
        this.b = b;
        this.s = s;
        this.error = error;
    }
}

public class AdaptiveIntegrator {

    // 辛普森公式：计算区间[a,b]上f(x)的辛普森近似积分
    private static double simpson(DoubleFunction f, double a, double b) {
        double c = (a + b) / 2.0;
        double h = b - a;
        return (h / 6.0) * (f.apply(a) + 4 * f.apply(c) + f.apply(b));
    }

    // 自适应辛普森积分（使用优先队列控制精度）
    public static double adaptiveIntegrate(
            DoubleFunction f,
            double a,
            double b,
            double tolerance,
            int maxIterations
    ) {
        // 优先队列：按误差降序排列，每次取出误差最大的区间拆分
        PriorityQueue<Interval> pq = new PriorityQueue<>(
                Comparator.comparingDouble((Interval iv) -> -iv.error)
        );

        // 初始化整个区间
        double s0 = simpson(f, a, b);
        double s1 = simpson(f, a, (a + b) / 2.0);
        double s2 = simpson(f, (a + b) / 2.0, b);
        double error0 = Math.abs(s0 - s1 - s2) / 15.0; // 辛普森误差估计
        pq.add(new Interval(a, b, s0, error0));

        double totalIntegral = 0.0;
        double totalError = 0.0;
        int iterations = 0;

        while (!pq.isEmpty() && iterations < maxIterations) {
            Interval current = pq.poll();

            // 如果当前区间误差足够小，直接计入结果
            if (current.error < tolerance) {
                totalIntegral += current.s;
                totalError += current.error;
                continue;
            }

            // 拆分区间为两半，分别计算辛普森近似和误差
            double mid = (current.a + current.b) / 2.0;
            double sLeft = simpson(f, current.a, mid);
            double sRight = simpson(f, mid, current.b);
            double sTotal = sLeft + sRight;
            double errorLeft = Math.abs(current.s - sLeft - simpson(f, current.a, (current.a + mid)/2.0) - simpson(f, (current.a + mid)/2.0, mid)) / 15.0;
            double errorRight = Math.abs(current.s - sRight - simpson(f, mid, (mid + current.b)/2.0) - simpson(f, (mid + current.b)/2.0, current.b)) / 15.0;

            // 将两个子区间加入优先队列
            pq.add(new Interval(current.a, mid, sLeft, errorLeft));
            pq.add(new Interval(mid, current.b, sRight, errorRight));

            iterations++;
        }

        // 处理剩余未处理的区间（达到最大迭代次数时）
        while (!pq.isEmpty()) {
            Interval iv = pq.poll();
            totalIntegral += iv.s;
            totalError += iv.error;
        }

        System.out.printf("迭代次数: %d, 估计总误差: %.2e%n", iterations, totalError);
        return totalIntegral;
    }

    // 测试示例
    public static void main(String[] args) {
        // 示例1：积分 ∫₀¹ x² dx = 1/3 ≈ 0.3333333
        DoubleFunction f1 = x -> x * x;
        double result1 = adaptiveIntegrate(f1, 0, 1, 1e-6, 1000);
        System.out.printf("∫₀¹ x² dx = %.8f%n", result1);

        // 示例2：积分 ∫₀^π sin(x) dx = 2
        DoubleFunction f2 = Math::sin;
        double result2 = adaptiveIntegrate(f2, 0, Math.PI, 1e-6, 1000);
        System.out.printf("∫₀^π sin(x) dx = %.8f%n", result2);
    }
}