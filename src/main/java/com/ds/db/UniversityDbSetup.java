package com.ds.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * 大学数据库建库工具：执行 {@code src/main/resources/sql/university.sql}
 * 创建 university 库的 9 张表（实体 5 + 联系 4），并验证表结构与外键。
 *
 * <p>连接信息从 {@code src/main/resources/db.properties} 读取。</p>
 */
public class UniversityDbSetup {

    private static final String DB_NAME = "university";
    private static final String SQL_PATH = "src/main/resources/sql/university_auth.sql";

    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   大学数据库建库（9 张表）                 ║");
        System.out.println("╚════════════════════════════════════════════╝");

        Properties props = loadDbConfig();
        String sql = loadSqlScript();

        // 1. 执行建库建表脚本（allowMultiQueries 一次执行整段 DDL）
        String url = "jdbc:mysql://localhost:" + props.getProperty("port", "3306")
                + "?useUnicode=true&characterEncoding=UTF-8"
                + "&useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true";
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement()) {
            st.execute(sql);
            System.out.println("✅ SQL 脚本执行完成");
        }

        // 2. 验证：打印每张表的结构
        verifyTables(props);

        // 3. 统计：每张表的行数
        printRowCounts(props);
    }

    /** 从 db.properties 读取连接配置：优先 classpath，其次相对路径兜底 */
    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/db.properties", "src/main/resources/db.properties", "db.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = UniversityDbSetup.class.getResourceAsStream(path);
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

    /** 读取 university.sql 脚本内容 */
    private static String loadSqlScript() throws IOException {
        File f = new File(SQL_PATH);
        if (!f.exists()) {
            throw new IOException("找不到 SQL 脚本: " + SQL_PATH);
        }
        return new String(Files.readAllBytes(Paths.get(SQL_PATH)), StandardCharsets.UTF_8);
    }

    /** 查询 information_schema 打印每张表的列定义（主键/外键标注） */
    private static void verifyTables(Properties props) throws SQLException {
        String url = "jdbc:mysql://localhost:" + props.getProperty("port", "3306")
                + "?useSSL=false&allowPublicKeyRetrieval=true";
        String sql = "SELECT table_name, column_name, column_type, is_nullable, column_key "
                + "FROM information_schema.columns "
                + "WHERE table_schema = '" + DB_NAME + "' "
                + "ORDER BY table_name, ordinal_position";

        TreeMap<String, StringBuilder> map = new TreeMap<>();
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String table = rs.getString("table_name");
                StringBuilder sb = map.computeIfAbsent(table, k -> new StringBuilder());
                sb.append(String.format("    %-14s %-12s  %s%s%s%n",
                        rs.getString("column_name"),
                        rs.getString("column_type"),
                        "NO".equals(rs.getString("is_nullable")) ? "NOT NULL " : "",
                        "PRI".equals(rs.getString("column_key")) ? "[PK] " : "",
                        "MUL".equals(rs.getString("column_key")) ? "[FK] " : ""));
            }
        }

        System.out.println("\n──────────────── 建表结果（" + map.size() + " 张表） ────────────────");
        for (Map.Entry<String, StringBuilder> e : map.entrySet()) {
            System.out.println("📋 表 " + e.getKey() + ":");
            System.out.print(e.getValue());
        }

        // 外键数量统计
        int fkCount = 0;
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.table_constraints "
                             + "WHERE constraint_schema = '" + DB_NAME + "' AND constraint_type = 'FOREIGN KEY'")) {
            rs.next();
            fkCount = rs.getInt(1);
        }
        System.out.println("🔗 外键总数: " + fkCount);
    }

    /** 统计每张表的行数，验证示例数据插入成功 */
    private static void printRowCounts(Properties props) throws SQLException {
        String url = "jdbc:mysql://localhost:" + props.getProperty("port", "3306")
                + "?useSSL=false&allowPublicKeyRetrieval=true";
        String[] tables = {"department", "classroom", "course", "instructor", "student",
                "section", "teaches", "takes", "prereq"};
        System.out.println("\n──────────────── 示例数据行数统计 ────────────────");
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement()) {
            for (String t : tables) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + DB_NAME + "." + t)) {
                    rs.next();
                    System.out.printf("   %-12s %d 行%n", t, rs.getInt(1));
                }
            }
        }
    }
}
