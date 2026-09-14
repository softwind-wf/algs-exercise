package com.ds.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;

/**
 * JDBC 可调用语句（CallableStatement）演示 —— PostgreSQL 版
 *
 * <p>对应教材 Database System Concepts 中「Callable Statement」一节：
 * 用 {@code {call ...}} 调用数据库里的存储过程 / 函数。</p>
 *
 * <pre>
 *   PostgreSQL callable_demo 库
 *     ├─ dept_count(dept)       标量函数（IN → 返回单个值）   {? = call ...}
 *     ├─ emp_info(id, OUT...)   OUT 参数函数（一次返回多值）   {call ...}
 *     ├─ it_employees()         表返回函数（返回结果集）        {call ...}
 *     └─ apply_raise(INOUT)     存储过程（改数据 + 回传新值）   {call ...}
 * </pre>
 *
 * <p>对象定义见 {@code src/main/resources/sql/callable_demo_pg.sql}，本类直接读脚本建库建对象，
 * 保证一条命令跑完。连接信息从 {@code src/main/resources/pg.properties} 读取。</p>
 */
public class PgCallableDemo {

    private static final String DB_NAME = "callable_demo";

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║     JDBC 可调用语句（CallableStatement）演示    ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        // 1. 读取连接配置 + 确保数据库存在
        Properties props = loadDbConfig();
        ensureDatabase(props, DB_NAME);

        // 2. 连接演示库，执行脚本建表 + 建函数/过程
        String url = "jdbc:postgresql://localhost:" + props.getProperty("port", "5432") + "/" + DB_NAME;
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"))) {
            initObjects(conn);
            System.out.println();

            // 3. 五种典型调用方式
            demoScalarFunction(conn);   // ① 标量函数：{? = call fn(?)} 取返回值
            demoOutParams(conn);        // ② OUT 参数：{call fn(?, ?, ?)} 取多个值
            demoTableFunction(conn);    // ③ 表返回函数：{call fn()} 得到 ResultSet
            demoProcedure(conn);        // ④ 存储过程：{call proc(?, ?)} 修改数据 + INOUT 回传
            demoPlainSqlCall(conn);     // ⑤ 对照：同一函数用普通 SELECT 也能调
        }
    }

    // ====================== 环境准备 ======================

    /** 从 pg.properties 读取连接配置：优先 classpath，其次相对路径兜底 */
    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/pg.properties", "src/main/resources/pg.properties", "pg.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = PgCallableDemo.class.getResourceAsStream(path);
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
        throw new IOException("找不到 pg.properties，请确认文件存在于 src/main/resources/");
    }

    /** 连到 postgres 库检查并创建演示库（CREATE DATABASE 不能在事务里，用独立连接） */
    private static void ensureDatabase(Properties props, String db) throws SQLException {
        String adminUrl = "jdbc:postgresql://localhost:" + props.getProperty("port", "5432") + "/postgres";
        try (Connection conn = DriverManager.getConnection(adminUrl,
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + db + "'")) {
            if (rs.next()) {
                System.out.println("🛠 数据库已存在: " + db);
            } else {
                st.execute("CREATE DATABASE " + db);
                System.out.println("🛠 已创建数据库: " + db);
            }
        }
    }

    /** 读取 SQL 脚本并在演示库执行（建表 + 建函数/过程，可重复运行） */
    private static void initObjects(Connection conn) throws Exception {
        String script = readScript();
        try (Statement st = conn.createStatement()) {
            boolean hasResult = st.execute(script);
            // 多语句脚本：逐个消费结果，确保全部执行完
            while (true) {
                if (hasResult) {
                    try (ResultSet rs = st.getResultSet()) {
                        // 无 SELECT，结果集为空即可
                    }
                } else if (st.getUpdateCount() == -1) {
                    break;
                }
                hasResult = st.getMoreResults();
            }
        }
        System.out.println("✅ 对象就绪: employee 表 + dept_count / emp_info / it_employees / apply_raise");
    }

    private static String readScript() throws IOException {
        String[] candidates = {"/sql/callable_demo_pg.sql",
                "src/main/resources/sql/callable_demo_pg.sql", "sql/callable_demo_pg.sql"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = PgCallableDemo.class.getResourceAsStream(path);
            }
            if (in == null) {
                File f = new File(path);
                if (f.exists()) {
                    in = new FileInputStream(f);
                }
            }
            if (in != null) {
                try (InputStream is = in) {
                    byte[] bytes = new byte[is.available()];
                    int off = 0;
                    int n;
                    while ((n = is.read(bytes, off, bytes.length - off)) > 0) {
                        off += n;
                    }
                    return new String(bytes, 0, off, StandardCharsets.UTF_8);
                }
            }
        }
        throw new IOException("找不到 callable_demo_pg.sql");
    }

    // ====================== 五种可调用方式 ======================

    /** ① 标量函数：{? = call fn(?)} —— 用 registerOutParameter + getXxx 取返回值 */
    private static void demoScalarFunction(Connection conn) throws SQLException {
        System.out.println("───────────────── ① 标量函数 dept_count(dept) ─────────────────");
        try (CallableStatement cs = conn.prepareCall("{? = call dept_count(?)}")) {
            cs.registerOutParameter(1, Types.INTEGER);   // 返回值占第 1 个问号
            cs.setString(2, "IT");                       // IN 参数
            cs.execute();
            System.out.println("   dept_count('IT') = " + cs.getInt(1));
            System.out.println("   语法: {? = call dept_count(?)}  ← 问号里第一个是返回值");
        }
    }

    /** ② OUT 参数函数：{call fn(?, ?, ?, ?)} —— 一次回传多个值 */
    private static void demoOutParams(Connection conn) throws SQLException {
        System.out.println("\n───────────────── ② OUT 参数函数 emp_info(id, OUT...) ─────────────────");
        try (CallableStatement cs = conn.prepareCall("{call emp_info(?, ?, ?, ?)}")) {
            cs.setInt(1, 1);                             // IN: emp_id
            cs.registerOutParameter(2, Types.VARCHAR);   // OUT: emp_name
            cs.registerOutParameter(3, Types.VARCHAR);   // OUT: dept
            cs.registerOutParameter(4, Types.NUMERIC);   // OUT: salary
            cs.execute();
            System.out.println("   emp_id=1 → 姓名=" + cs.getString(2)
                    + "  部门=" + cs.getString(3)
                    + "  工资=" + cs.getBigDecimal(4));
            System.out.println("   语法: {call emp_info(?, ?, ?, ?)}  ← OUT 参数用 registerOutParameter 声明");
        }
    }

    /** ③ 表返回函数：{call fn()} —— 函数返回的是结果集，直接当 ResultSet 用 */
    private static void demoTableFunction(Connection conn) throws SQLException {
        System.out.println("\n───────────────── ③ 表返回函数 it_employees() ─────────────────");
        try (CallableStatement cs = conn.prepareCall("{call it_employees()}")) {
            boolean hasResult = cs.execute();
            if (hasResult) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        System.out.println("   #" + rs.getInt("emp_id") + "  "
                                + rs.getString("emp_name") + "  "
                                + rs.getBigDecimal("salary"));
                    }
                }
            }
            System.out.println("   语法: {call it_employees()}  ← 返回的是多行结果集，像 SELECT 一样遍历");
        }
    }

    /** ④ 存储过程：{call proc(?, ?, ?)} —— 修改数据，INOUT 参数把新值回传 */
    private static void demoProcedure(Connection conn) throws SQLException {
        System.out.println("\n───────────────── ④ 存储过程 apply_raise(INOUT) ─────────────────");
        System.out.println("   调用前 Bob(id=2) 工资: " + querySalary(conn, 2));
        // 注意: PG 的 {call ...} 转义默认按"函数"翻译成 SELECT * FROM fn(...)，
        // 存储过程只能 CALL，所以这里用原生 CALL 语句，不用花括号转义
        try (CallableStatement cs = conn.prepareCall("CALL apply_raise(?, ?, ?)")) {
            cs.setInt(1, 2);                             // IN: emp_id
            cs.setBigDecimal(2, new BigDecimal("10"));   // IN: 涨薪 10%
            cs.setBigDecimal(3, BigDecimal.ZERO);        // INOUT 的"入口值"（本过程会覆盖它）
            cs.registerOutParameter(3, Types.NUMERIC);   // INOUT 的"出口值"
            cs.execute();
            System.out.println("   调用后 Bob 工资: " + cs.getBigDecimal(3) + "  ← INOUT 回传的新值");
        }
        System.out.println("   语法: CALL apply_raise(?, ?, ?)  ← 存储过程只能 CALL，不能嵌在 SELECT 里");
    }

    /** ⑤ 对照：函数除了 CallableStatement，用普通 SELECT 也能直接调 */
    private static void demoPlainSqlCall(Connection conn) throws SQLException {
        System.out.println("\n───────────────── ⑤ 对照：普通 SQL 也能调函数 ─────────────────");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT dept_count('IT') AS cnt")) {
            rs.next();
            System.out.println("   SELECT dept_count('IT') → " + rs.getInt("cnt")
                    + "  （和 ① 用 CallableStatement 调的是同一个函数）");
        }
        System.out.println("   ✅ 结论: 函数可嵌在 SQL 表达式里; 存储过程只能 CALL。");
    }

    private static BigDecimal querySalary(Connection conn, int empId) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT salary FROM employee WHERE emp_id = " + empId)) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }
}
