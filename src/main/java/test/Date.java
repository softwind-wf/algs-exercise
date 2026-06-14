package test;

public abstract class Date {
    // 构造函数：创建一个日期对象
    public Date(int month, int day, int year) {}

    public Date() {

    }

    // 获取月份
    public abstract int month();

    // 获取日期
    public abstract int day();

    // 获取年份
    public abstract int year();

    // 返回日期的字符串表示
    public abstract String toString();
}