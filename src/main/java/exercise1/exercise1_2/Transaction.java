package exercise1.exercise1_2;

import java.util.Comparator;
import java.util.Objects;

// 实现Comparable接口，默认按金额降序排序
public class Transaction implements Comparable<Transaction> {
    // 交易核心属性
    private final String who;
    private final SmartDate when;
    private final double amount;

    // 构造器1：直接传入参数构造
    public Transaction(String who, SmartDate when, double amount) {
        if (who == null || when == null) {
            throw new IllegalArgumentException("交易人/日期不能为空");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("交易金额必须为正数");
        }
        this.who = who;
        this.when = when;
        this.amount = amount;
    }

    // 构造器2：从格式化字符串解析（格式：姓名 年 月 日 金额）
    public Transaction(String transactionString) {
        String[] fields = transactionString.split("\\s+");
        if (fields.length != 5) { // 修正之前的字段数错误，姓名+年+月+日+金额共5个
            throw new IllegalArgumentException("交易字符串格式错误，需为：姓名 年 月 日 金额");
        }
        this.who = fields[0];
        this.when = new SmartDate(Integer.parseInt(fields[1]),
                Integer.parseInt(fields[2]),
                Integer.parseInt(fields[3]));
        this.amount = Double.parseDouble(fields[4]);
        if (this.amount <= 0) {
            throw new IllegalArgumentException("交易金额必须为正数");
        }
    }

    // 各属性的getter方法
    public String who() {
        return who;
    }

    public SmartDate when() {
        return when;
    }

    public double amount() {
        return amount;
    }

    // 重写equals：全属性相等才相等
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Transaction that = (Transaction) other;
        return Double.compare(that.amount, amount) == 0 &&
                Objects.equals(who, that.who) &&
                Objects.equals(when, that.when);
    }

    // 重写hashCode：和equals保持一致
    @Override
    public int hashCode() {
        return Objects.hash(who, when, amount);
    }

    // 重写toString：格式化输出，整洁易读
    @Override
    public String toString() {
        return String.format("%-10s %10s %8.2f", who, when, amount);
    }

    // 实现Comparable接口：默认按金额**降序**排序
    @Override
    public int compareTo(Transaction that) {
        return Double.compare(that.amount, this.amount);
    }

    // 内部静态比较器1：按交易日期**升序**排序（年→月→日）
    public static class DateOrder implements Comparator<Transaction> {
        @Override
        public int compare(Transaction v, Transaction w) {
            // 先比年份
            if (v.when().getYear() != w.when().getYear()) {
                return Integer.compare(v.when().getYear(), w.when().getYear());
            }
            // 再比月份
            if (v.when().getMonth() != w.when().getMonth()) {
                return Integer.compare(v.when().getMonth(), w.when().getMonth());
            }
            // 最后比日期
            return Integer.compare(v.when().getDay(), w.when().getDay());
        }
    }

    // 内部静态比较器2：按交易人姓名**字典序**排序
    public static class WhoOrder implements Comparator<Transaction> {
        @Override
        public int compare(Transaction v, Transaction w) {
            return v.who().compareTo(w.who());
        }
    }

    // 测试主方法：三种排序方式都演示
    public static void main(String[] args) {
        // 初始化3条交易记录
        SmartDate d1 = new SmartDate(2024, 10, 5);
        SmartDate d2 = new SmartDate(2024, 10, 3);
        SmartDate d3 = new SmartDate(2024, 9, 20);
        Transaction t1 = new Transaction("Alice", d1, 199.99);
        Transaction t2 = new Transaction("Bob", d2, 299.50);
        Transaction t3 = new Transaction("Charlie", d3, 99.99);
        Transaction[] transactions = {t1, t2, t3};

        // 1. 默认按金额降序排序
        java.util.Arrays.sort(transactions);
        System.out.println("=== 按金额降序 ===");
        for (Transaction t : transactions) System.out.println(t);

        // 2. 按交易日期升序排序
        java.util.Arrays.sort(transactions, new Transaction.DateOrder());
        System.out.println("\n=== 按日期升序 ===");
        for (Transaction t : transactions) System.out.println(t);

        // 3. 按姓名字典序排序
        java.util.Arrays.sort(transactions, new Transaction.WhoOrder());
        System.out.println("\n=== 按姓名排序 ===");
        for (Transaction t : transactions) System.out.println(t);
    }
}