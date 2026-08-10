package com.ds.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 通用 JDBC SQL 执行工具 —— 让命令行 / Claude Code 直接操作 MySQL。
 *
 * <p>用法：</p>
 * <pre>
 *   SqlRunner "SELECT * FROM university.course"
 *   SqlRunner -db university "SELECT name FROM student WHERE tot_cred > 100"
 *   SqlRunner -f script.sql
 *   echo "SELECT 1" | SqlRunner
 * </pre>
 *
 * <p>特性：</p>
 * <ul>
 *   <li>SELECT / SHOW / DESC → 打印对齐表格 + 行数；</li>
 *   <li>INSERT / UPDATE / DELETE → 打印影响行数；</li>
 *   <li>DDL → 打印执行成功；</li>
 *   <li>一条命令可传多条 SQL，按分号分割（忽略引号内的分号）。</li>
 * </ul>
 *
 * <p>连接信息从 {@code src/main/resources/db.properties} 读取。</p>
 */
public class SqlRunner {

    public static void main(String[] args) throws Exception {
        Properties props = loadDbConfig();
        String url = "jdbc:mysql://localhost:" + props.getProperty("port", "3306")
                + "?useUnicode=true&characterEncoding=UTF-8"
                + "&useSSL=false&allowPublicKeyRetrieval=true";

        // ---- 解析参数 ----
        String db = null;
        String filePath = null;
        List<String> rest = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if ("-db".equals(args[i])) {
                db = args[++i];
            } else if ("-f".equals(args[i])) {
                filePath = args[++i];
            } else {
                rest.add(args[i]);
            }
        }

        String sql;
        if (filePath != null) {
            sql = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } else if (!rest.isEmpty()) {
            sql = String.join(" ", rest);
        } else {
            sql = readStdin();
        }
        if (sql == null || sql.trim().isEmpty()) {
            usage();
            return;
        }

        // ---- 连接并执行 ----
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"))) {
            if (db != null) {
                conn.setCatalog(db);   // 等价于 USE db
            }
            for (String statement : splitSql(sql)) {
                execute(conn, statement);
            }
        }
    }

    /** 执行单条 SQL：SELECT 打印表格，其余打印影响行数 */
    private static void execute(Connection conn, String statement) {
        String trimmed = statement.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            boolean hasResult = st.execute(trimmed);
            if (hasResult) {
                try (ResultSet rs = st.getResultSet()) {
                    printResultSet(rs);
                }
            } else {
                int affected = st.getUpdateCount();
                System.out.println(affected >= 0
                        ? "✅ 影响行数: " + affected
                        : "✅ 执行成功");
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL 错误: " + e.getMessage());
        }
    }

    /** 将查询结果打印为对齐的表格 */
    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();

        List<String> headers = new ArrayList<>(cols);
        int[] widths = new int[cols];
        for (int i = 1; i <= cols; i++) {
            String label = md.getColumnLabel(i);
            headers.add(label);
            widths[i - 1] = label.length();
        }

        List<List<String>> rows = new ArrayList<>();
        while (rs.next()) {
            List<String> row = new ArrayList<>(cols);
            for (int i = 1; i <= cols; i++) {
                String val = rs.getString(i);
                if (val == null) {
                    val = "NULL";
                }
                row.add(val);
                widths[i - 1] = Math.max(widths[i - 1], val.length());
            }
            rows.add(row);
        }

        String sep = sepLine(widths);
        System.out.println(sep);
        StringBuilder head = new StringBuilder("|");
        for (int i = 0; i < cols; i++) {
            head.append(' ').append(pad(headers.get(i), widths[i])).append(" |");
        }
        System.out.println(head);
        System.out.println(sep);
        for (List<String> row : rows) {
            StringBuilder line = new StringBuilder("|");
            for (int i = 0; i < cols; i++) {
                line.append(' ').append(pad(row.get(i), widths[i])).append(" |");
            }
            System.out.println(line);
        }
        System.out.println(sep);
        System.out.println("  → 共 " + rows.size() + " 行");
    }

    private static String sepLine(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) {
                sb.append('-');
            }
            sb.append('+');
        }
        return sb.toString();
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** 按分号分割 SQL（忽略单引号/双引号内的分号） */
    private static List<String> splitSql(String sql) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (quote != 0) {
                cur.append(c);
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
                cur.append(c);
            } else if (c == ';') {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.toString().trim().length() > 0) {
            parts.add(cur.toString());
        }
        return parts;
    }

    private static String readStdin() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static void usage() {
        System.out.println("用法:");
        System.out.println("  SqlRunner \"SQL\"                 执行一条或多条 SQL（分号分隔）");
        System.out.println("  SqlRunner -db <库名> \"SQL\"      先切换数据库再执行");
        System.out.println("  SqlRunner -f <脚本.sql>          执行 SQL 文件");
        System.out.println("  echo \"SQL\" | SqlRunner         从标准输入读取");
    }

    /** 从 db.properties 读取连接配置：优先 classpath，其次相对路径兜底 */
    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/db.properties", "src/main/resources/db.properties", "db.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = SqlRunner.class.getResourceAsStream(path);
            }
            if (in == null) {
                File f = new File(path);
                if (f.exists()) {
                    in = new FileInputStream(f);
                }
            }
            if (in != null) {
                try (InputStream is = in) {
                    props.load(is);
                }
                return props;
            }
        }
        throw new IOException("找不到 db.properties，请确认文件存在于 src/main/resources/");
    }
}
