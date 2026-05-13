package exercise1.exercise1_2;

public class Rational {
    private final long numerator;   // 分子
    private final long denominator; // 分母

    // 构造函数1：支持 int 类型参数
    public Rational(int numerator, int denominator) {
        this((long) numerator, (long) denominator);
    }

    // 构造函数2：支持 long 类型参数（核心构造）
    public Rational(long numerator, long denominator) {
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
    public Rational plus(Rational b) {
        // 断言：检查乘法是否会溢出
        assert !willOverflow(this.numerator, b.denominator) : "加法中 this.numerator * b.denominator 溢出";
        assert !willOverflow(b.numerator, this.denominator) : "加法中 b.numerator * this.denominator 溢出";
        assert !willOverflow(this.denominator, b.denominator) : "加法中 this.denominator * b.denominator 溢出";

        long newNumerator = this.numerator * b.denominator + b.numerator * this.denominator;
        long newDenominator = this.denominator * b.denominator;
        return new Rational(newNumerator, newDenominator);
    }

    // 减法
    public Rational minus(Rational b) {
        assert !willOverflow(this.numerator, b.denominator) : "减法中 this.numerator * b.denominator 溢出";
        assert !willOverflow(b.numerator, this.denominator) : "减法中 b.numerator * this.denominator 溢出";
        assert !willOverflow(this.denominator, b.denominator) : "减法中 this.denominator * b.denominator 溢出";

        long newNumerator = this.numerator * b.denominator - b.numerator * this.denominator;
        long newDenominator = this.denominator * b.denominator;
        return new Rational(newNumerator, newDenominator);
    }

    // 乘法
    public Rational times(Rational b) {
        assert !willOverflow(this.numerator, b.numerator) : "乘法中 this.numerator * b.numerator 溢出";
        assert !willOverflow(this.denominator, b.denominator) : "乘法中 this.denominator * b.denominator 溢出";

        long newNumerator = this.numerator * b.numerator;
        long newDenominator = this.denominator * b.denominator;
        return new Rational(newNumerator, newDenominator);
    }

    // 除法
    public Rational divides(Rational b) {
        assert !willOverflow(this.numerator, b.denominator) : "除法中 this.numerator * b.denominator 溢出";
        assert !willOverflow(this.denominator, b.numerator) : "除法中 this.denominator * b.numerator 溢出";

        long newNumerator = this.numerator * b.denominator;
        long newDenominator = this.denominator * b.numerator;
        return new Rational(newNumerator, newDenominator);
    }

    // 转换为浮点数
    public double doubleValue() {
        return (double) numerator / denominator;
    }

    // 判断两个 long 相乘是否会溢出
    private boolean willOverflow(long a, long b) {
        if (a == 0 || b == 0) {
            return false;
        }
        // 检查溢出：如果 a * b 超过 Long.MAX_VALUE 或小于 Long.MIN_VALUE
        return (b > 0 && a > Long.MAX_VALUE / b) || (b > 0 && a < Long.MIN_VALUE / b)
                || (b < 0 && a > Long.MIN_VALUE / b) || (b < 0 && a < Long.MAX_VALUE / b);
    }

    // 相等性判断
    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        Rational rational = (Rational) that;
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
        // 开启断言（运行时需要加参数 -ea）
        Rational a = new Rational(1, 2);
        Rational b = new Rational(1, 3);
        System.out.println("a = " + a);
        System.out.println("b = " + b);
        System.out.println("a + b = " + a.plus(b));

        // 测试溢出断言（仅在开启断言时生效）
        try {
            Rational big = new Rational(Long.MAX_VALUE, 1);
            big.times(big); // 这里会触发断言失败
        } catch (AssertionError e) {
            System.out.println("断言生效：" + e.getMessage());
        }
    }
}