package com.ds.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC 元数据（Metadata）演示 —— PostgreSQL 版
 *
 * <p>对应教材 Database System Concepts 中「Metadata」一节：
 * JDBC 提供三层元数据接口，让我们在运行时"内省"数据库结构和查询结果：</p>
 *
 * <pre>
 *   ① DatabaseMetaData    —— 数据库整体信息（产品名/版本/表/列/主键/函数…）
 *   ② ResultSetMetaData   —— 查询结果集的列信息（列名/类型/个数…）
 *   ③ ParameterMetaData   —— 预编译语句的参数信息（个数/类型…）
 * </pre>
 */
public class PgMetadataDemo {

    public static void main(String[] args) throws Exception {
        Properties props = loadDbConfig();
        String url = "jdbc:postgresql://localhost:" + props.getProperty("port", "5432") + "/callable_demo";

        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"))) {

            demoDatabaseMetaData(conn);
            demoResultSetMetaData(conn);
            demoParameterMetaData(conn);
        }
    }

    // ====================== ① DatabaseMetaData ======================

    private static void demoDatabaseMetaData(Connection conn) throws SQLException {
        System.out.println("══════════ ① DatabaseMetaData：数据库级元数据 ══════════");

        DatabaseMetaData meta = conn.getMetaData();

        // 基本信息
        System.out.println("  产品名       : " + meta.getDatabaseProductName());
        System.out.println("  产品版本     : " + meta.getDatabaseProductVersion());
        System.out.println("  驱动名       : " + meta.getDriverName());
        System.out.println("  驱动版本     : " + meta.getDriverVersion());
        System.out.println("  JDBC 版本    : " + meta.getJDBCMajorVersion() + "." + meta.getJDBCMinorVersion());
        System.out.println("  最大连接数   : " + meta.getMaxConnections());
        System.out.println("  支持事务     : " + meta.supportsTransactions());
        System.out.println("  支持保存点   : " + meta.supportsSavepoints());

        // 列出 public schema 的表
        System.out.println("\n  ── public schema 下的表 ──");
        try (ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                System.out.println("  表: " + rs.getString("TABLE_NAME")
                        + "  类型: " + rs.getString("TABLE_TYPE"));
            }
        }

        // employee 表的列
        System.out.println("\n  ── employee 表的列 ──");
        try (ResultSet rs = meta.getColumns(null, "public", "employee", "%")) {
            while (rs.next()) {
                System.out.println("  列: " + rs.getString("COLUMN_NAME")
                        + "  类型: " + rs.getString("TYPE_NAME")
                        + "  可空: " + rs.getString("IS_NULLABLE")
                        + "  默认: " + rs.getString("COLUMN_DEF"));
            }
        }

        // employee 表的主键
        System.out.println("\n  ── employee 表的主键 ──");
        try (ResultSet rs = meta.getPrimaryKeys(null, "public", "employee")) {
            while (rs.next()) {
                System.out.println("  主键: " + rs.getString("COLUMN_NAME")
                        + "  序号: " + rs.getShort("KEY_SEQ"));
            }
        }

        // callable_demo 库中的函数/存储过程
        System.out.println("\n  ── 可调用的函数/过程 ──");
        try (ResultSet rs = meta.getProcedures(null, "public", "%")) {
            while (rs.next()) {
                System.out.println("  " + rs.getString("PROCEDURE_NAME")
                        + "  类型: " + rs.getShort("PROCEDURE_TYPE"));
            }
        }
    }

    // ====================== ② ResultSetMetaData ======================

    private static void demoResultSetMetaData(Connection conn) throws SQLException {
        System.out.println("\n══════════ ② ResultSetMetaData：结果集列元数据 ══════════");

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT emp_id, emp_name, dept, salary FROM employee ORDER BY emp_id");
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            System.out.println("  列数: " + colCount);

            for (int i = 1; i <= colCount; i++) {
                System.out.printf("  列 %d: 名称=%-12s JDBC类型=%-10s SQL类型=%-12s 精度=%s%n",
                        i,
                        meta.getColumnLabel(i),
                        meta.getColumnTypeName(i),
                        meta.getColumnClassName(i),
                        meta.getPrecision(i));
            }

            // 打印表头 + 数据
            System.out.println("\n  ── 用元数据动态打印结果集 ──");
            StringBuilder header = new StringBuilder("  ");
            for (int i = 1; i <= colCount; i++) {
                header.append(meta.getColumnLabel(i)).append("\t");
            }
            System.out.println(header);

            while (rs.next()) {
                StringBuilder row = new StringBuilder("  ");
                for (int i = 1; i <= colCount; i++) {
                    row.append(rs.getObject(i)).append("\t");
                }
                System.out.println(row);
            }
        }
    }

    // ====================== ③ ParameterMetaData ======================

    private static void demoParameterMetaData(Connection conn) throws SQLException {
        System.out.println("\n══════════ ③ ParameterMetaData：参数元数据 ══════════");

        String sql = "SELECT emp_name FROM employee WHERE dept = ? AND salary > ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ParameterMetaData meta = ps.getParameterMetaData();
            int paramCount = meta.getParameterCount();

            System.out.println("  SQL : " + sql);
            System.out.println("  参数个数: " + paramCount);

            for (int i = 1; i <= paramCount; i++) {
                System.out.printf("  参数 %d: JDBC类型=%s  模式=%s  精度=%s%n",
                        i,
                        meta.getParameterTypeName(i),
                        meta.getParameterMode(i),
                        meta.getPrecision(i));
            }
        }

        // CallableStatement 的参数元数据（OUT 参数也能查）
        System.out.println("\n  ── CallableStatement 的参数元数据 ──");
        try {
            try (CallableStatement cs = conn.prepareCall("{? = call dept_count(?)}")) {
                ParameterMetaData meta = cs.getParameterMetaData();
                System.out.println("  {call dept_count(?)}  参数个数: " + meta.getParameterCount());
                for (int i = 1; i <= meta.getParameterCount(); i++) {
                    System.out.printf("  参数 %d: JDBC类型=%s  模式=%s%n",
                            i, meta.getParameterTypeName(i), meta.getParameterMode(i));
                }
            }
        } catch (SQLException e) {
            System.out.println("  ⚠ PostgreSQL JDBC 驱动不支持对 CallableStatement 的 OUT 参数做元数据内省。");
            System.out.println("    这是 PG 驱动的已知限制，MySQL/Oracle 对此支持更好。");
        }

        System.out.println("\n  ✅ 结论: 元数据让我们在运行时动态感知数据库结构，无需硬编码列名/类型。");
    }

    // ====================== 工具方法 ======================

    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/pg.properties", "src/main/resources/pg.properties", "pg.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = PgMetadataDemo.class.getResourceAsStream(path);
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
        throw new IOException("找不到 pg.properties");
    }
}
