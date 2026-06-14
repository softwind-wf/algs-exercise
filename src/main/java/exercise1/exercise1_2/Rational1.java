package exercise1.exercise1_2;

public class Rational1 {
    private final long numerator;   // 分子
    private final long denominator; // 分母

    // 构造函数1：支持 int 类型参数
    public Rational1(int numerator, int denominator) {
        this((long) numerator, (long) denominator);
    }

    // 构造函数2：支持 long 类型参数（核心构造）
    public Rational1(long numerator, long denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("分母不能为0");
        }

        // 处理符号，确保分母为正
        long g = gcd(Math.abs(numerator), Math.abs(denominator));
        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }

        this.numerator = numerator / g;
        this.denominator = denominator / g;
    }

    // 欧几里得算法求最大公约数
    private long gcd(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    // 加法
    public Rational1 plus(Rational1 b) {
        long newNumerator = this.numerator * b.denominator + b.numerator * this.denominator;
        long newDenominator = this.denominator * b.denominator;
        return new Rational1(newNumerator, newDenominator);
    }

    // 减法
    public Rational1 minus(Rational1 b) {
        long newNumerator = this.numerator * b.denominator - b.numerator * this.denominator;
        long newDenominator = this.denominator * b.denominator;
        return new Rational1(newNumerator, newDenominator);
    }

    // 乘法
    public Rational1 times(Rational1 b) {
        long newNumerator = this.numerator * b.numerator;
        long newDenominator = this.denominator * b.denominator;
        return new Rational1(newNumerator, newDenominator);
    }

    // 除法
    public Rational1 divides(Rational1 b) {
        long newNumerator = this.numerator * b.denominator;
        long newDenominator = this.denominator * b.numerator;
        return new Rational1(newNumerator, newDenominator);
    }

    // 转换为浮点数
    public double doubleValue() {
        return (double) numerator / denominator;
    }

    // 相等性判断
    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        Rational1 rational = (Rational1) that;
        return numerator == rational.numerator && denominator == rational.denominator;
    }

    // 字符串表示
    @Override
    public String toString() {
        if (denominator == 1) {
            return numerator + "";
        }
        return numerator + "/" + denominator;
    }

    // 测试用例
    public static void main(String[] args) {
        // 测试 int 构造
        Rational1 a = new Rational1(1, 2);
        Rational1 b = new Rational1(1, 3);
        System.out.println("a = " + a);
        System.out.println("b = " + b);
        System.out.println("a + b = " + a.plus(b));
        System.out.println("a - b = " + a.minus(b));
        System.out.println("a * b = " + a.times(b));
        System.out.println("a / b = " + a.divides(b));
        System.out.println("a == b ? " + a.equals(b));
        System.out.println("a 的浮点值 = " + a.doubleValue());

        // 测试 long 构造
        Rational1 c = new Rational1(1000000000L, 2000000000L);
        System.out.println("\nc = " + c);
        System.out.println("c 的浮点值 = " + c.doubleValue());
    }
}