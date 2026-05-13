package test;

public class DateTest {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java DateTest <month> <day> <year>");
            return;
        }

        int m = Integer.parseInt(args[0]);
        int d = Integer.parseInt(args[1]);
        int y = Integer.parseInt(args[2]);

        // 测试实现一
        Date date1 = new Date1(m, d, y);
        System.out.println("Date1: " + date1);

        // 测试实现二
        Date date2 = new Date2(m, d, y);
        System.out.println("Date2: " + date2);
    }
}