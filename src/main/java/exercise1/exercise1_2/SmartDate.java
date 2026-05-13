package exercise1.exercise1_2;

import java.util.Calendar;

public class SmartDate {
    private final int year;
    private final int month;
    private final int day;

    // 构造函数
    public SmartDate(int year, int month, int day) {
        if (!isValidDate(year, month, day)) {
            throw new IllegalArgumentException("非法日期: " + year + "-" + month + "-" + day);
        }
        this.year = year;
        this.month = month;
        this.day = day;
    }

    // 在 SmartDate 类中添加
    public SmartDate(String dateStr) {
        String[] fields = dateStr.split("/");
        if (fields.length != 3) {
            throw new IllegalArgumentException("日期格式错误，应为 月/日/年，例如：5/22/1939");
        }
        int month = Integer.parseInt(fields[0]);
        int day = Integer.parseInt(fields[1]);
        int year = Integer.parseInt(fields[2]);

        // 复用已有的日期合法性验证
        if (!isValidDate(year, month, day)) {
            throw new IllegalArgumentException("非法日期: " + dateStr);
        }
        this.year = year;
        this.month = month;
        this.day = day;
    }



    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    // 日期合法性验证
    private boolean isValidDate(int year, int month, int day) {
        if (month < 1 || month > 12) return false;
        if (day < 1) return false;
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        return day <= maxDay;
    }

    // 新增：返回星期几
    public String dayOfTheWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(this.year, this.month - 1, this.day);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // Calendar 中，1=周日，2=周一，...，7=周六
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            case Calendar.SUNDAY:
                return "Sunday";
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        return year + "-" + month + "-" + day;
    }

    // 测试用例
    public static void main(String[] args) {
        SmartDate date = new SmartDate(2026, 2, 1);
        System.out.println(date + " 是 " + date.dayOfTheWeek());
    }
}