package exercise1.exercise1_2;

import java.util.Calendar;

public class SmartDate1 {
    private final int year;
    private final int month;
    private final int day;

    // 构造函数
    public SmartDate1(int year, int month, int day) {
        // 验证日期合法性
        if (!isValidDate(year, month, day)) {
            throw new IllegalArgumentException("非法日期: " + year + "-" + month + "-" + day);
        }
        this.year = year;
        this.month = month;
        this.day = day;
    }

    // 日期合法性验证
    private boolean isValidDate(int year, int month, int day) {
        // 基础范围检查
        if (month < 1 || month > 12) return false;
        if (day < 1) return false;

        // 每月天数检查
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        return day <= maxDay;
    }

    // 覆盖 toString 方法
    @Override
    public String toString() {
        return year + "-" + month + "-" + day;
    }

    // 测试用例
    public static void main(String[] args) {
        try {
            SmartDate1 validDate = new SmartDate1(2024, 2, 29);
            System.out.println("合法日期: " + validDate);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        try {
            SmartDate1 invalidDate = new SmartDate1(2023, 2, 29);
            //System.out.println("非法日期: " + invalidDate);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}