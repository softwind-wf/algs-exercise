package dsh.demo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * PostgreSQL PL/Java 外部语言函数演示。
 * 每个 public 静态方法 = 一个可被 SQL 调用的外部函数（LANGUAGE java）。
 *
 * 关联到 university 场景：
 *  - greet          标量函数（String 处理）
 *  - avgOf          int[] 入参（Java 处理整列）
 *  - mostFrequentWord  集合/泛型（HashMap），SQL/plpgsql 写起来很绕
 *  - countStudents  通过 JDBC 回查数据库（复用当前事务）
 */
public class UniversityFuncs {

    public static String greet(String name, String title) {
        if (name == null || name.isBlank()) {
            return "Hello!";
        }
        String t = (title == null || title.isBlank()) ? "" : title + " ";
        return "Dear " + t + name + ", welcome to the university!";
    }

    public static double avgOf(int[] xs) {
        if (xs == null || xs.length == 0) {
            return 0.0;
        }
        return Arrays.stream(xs).average().orElse(0.0);
    }

    public static String mostFrequentWord(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Map<String, Integer> freq = new HashMap<>();
        for (String w : text.toLowerCase().split("\\W+")) {
            if (w.isEmpty()) {
                continue;
            }
            freq.merge(w, 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    /**
     * 入参为 java.sql.Connection 时，PL/Java 自动把当前事务连接传进来。
     * 这样外部函数内部还能执行 SQL，且与调用方处于同一事务。
     */
    public static int countStudents(Connection conn) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM student")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
