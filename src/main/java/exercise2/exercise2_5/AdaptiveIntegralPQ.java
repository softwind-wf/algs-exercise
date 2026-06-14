package exercise2.exercise2_5;

import java.util.PriorityQueue;

/**
 * 基于优先队列的自适应数值积分
 * 使用 Simpson 法则计算积分值，并用区间 Simpson 值与两子区间 Simpson 和之差的绝对值作为误差估计。
 */
public class AdaptiveIntegralPQ {

    /** 函数接口 */
    @FunctionalInterface
    interface MathFunction {
        double evaluate(double x);
    }

    /** 区间类：存储区间端点、积分近似值和误差估计 */
    static class Interval implements Comparable<Interval> {
        double left;
        double right;
        double integral;   // 当前区间的 Simpson 积分值
        double error;      // 误差估计：|S(a,b) - (S(a,m)+S(m,b))|

        Interval(double left, double right, MathFunction f) {
            this.left = left;
            this.right = right;
            double mid = (left + right) * 0.5;
            double whole = simpson(f, left, right);
            double leftPart = simpson(f, left, mid);
            double rightPart = simpson(f, mid, right);
            this.integral = whole;
            this.error = Math.abs(whole - (leftPart + rightPart));
        }

        // Simpson 法则：对于区间 [a, b] 上的积分近似
        private static double simpson(MathFunction f, double a, double b) {
            double m = (a + b) * 0.5;
            return (b - a) / 6.0 * (f.evaluate(a) + 4.0 * f.evaluate(m) + f.evaluate(b));
        }

        @Override
        public int compareTo(Interval other) {
            // 按误差降序，误差大的优先级高
            return Double.compare(other.error, this.error);
        }

        @Override
        public String toString() {
            return String.format("[%.6f, %.6f] 积分=%.8f 误差=%.8e", left, right, integral, error);
        }
    }

    /**
     * 自适应积分主方法
     * @param f          被积函数
     * @param a          积分下限
     * @param b          积分上限
     * @param tol        全局容许误差
     * @param maxIter    最大迭代次数（最多分割次数）
     * @param initParts  初始划分份数
     * @return 积分近似值
     */
    public static double integrate(MathFunction f, double a, double b,
                                   double tol, int maxIter, int initParts) {
        // 1. 初始划分，创建区间对象并入队
        PriorityQueue<Interval> pq = new PriorityQueue<>();
        double step = (b - a) / initParts;
        double totalIntegral = 0.0;
        double totalError = 0.0;
        for (int i = 0; i < initParts; i++) {
            double l = a + i * step;
            double r = l + step;
            Interval interval = new Interval(l, r, f);
            pq.offer(interval);
            totalIntegral += interval.integral;
            totalError += interval.error;
        }

        System.out.println("初始总积分 = " + totalIntegral);
        System.out.println("初始总误差估计 = " + totalError);
        System.out.println("初始区间数 = " + initParts);
        System.out.println("开始迭代...\n");

        int iter = 0;
        while (totalError > tol && iter < maxIter) {
            // 取出当前误差最大的区间
            Interval worst = pq.poll();
            if (worst == null) break;

            // 分割成两个子区间
            double mid = (worst.left + worst.right) * 0.5;
            Interval leftInt = new Interval(worst.left, mid, f);
            Interval rightInt = new Interval(mid, worst.right, f);

            // 更新总积分和总误差：减去原区间，加上两个新区间
            totalIntegral = totalIntegral - worst.integral + leftInt.integral + rightInt.integral;
            totalError = totalError - worst.error + leftInt.error + rightInt.error;

            // 将两个子区间入队
            pq.offer(leftInt);
            pq.offer(rightInt);

            iter++;

            // 可选：每 100 次迭代打印一次进度
            if (iter % 100 == 0) {
                System.out.printf("迭代 %d 次: 当前积分 = %.12f, 总误差 = %.8e, 区间数 = %d\n",
                        iter, totalIntegral, totalError, pq.size());
            }
        }

        System.out.printf("\n迭代结束，共分割 %d 次\n", iter);
        System.out.printf("最终区间数 = %d\n", pq.size());
        System.out.printf("最终总误差估计 = %.8e\n", totalError);
        return totalIntegral;
    }

    public static void main(String[] args) {
        // 测试函数1: f(x) = sin(x) 在 [0, PI] 上，真实积分 = 2.0
        MathFunction sinFunc = x -> Math.sin(x);
        double a1 = 0.0, b1 = Math.PI;
        double exact1 = 2.0;
        double result1 = integrate(sinFunc, a1, b1, 1e-6, 5000, 8);
        System.out.printf("\n【sin(x)】积分结果 = %.12f, 真实值 = %.12f, 绝对误差 = %.8e\n",
                result1, exact1, Math.abs(result1 - exact1));

        System.out.println("\n" + "===========================================" + "\n");

        // 测试函数2: f(x) = x^2 在 [0, 1] 上，真实积分 = 1/3 ≈ 0.333333333333
        MathFunction squareFunc = x -> x * x;
        double a2 = 0.0, b2 = 1.0;
        double exact2 = 1.0 / 3.0;
        double result2 = integrate(squareFunc, a2, b2, 1e-8, 5000, 4);
        System.out.printf("\n【x²】积分结果 = %.12f, 真实值 = %.12f, 绝对误差 = %.8e\n",
                result2, exact2, Math.abs(result2 - exact2));
    }
}