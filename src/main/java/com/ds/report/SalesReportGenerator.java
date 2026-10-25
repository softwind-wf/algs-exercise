package com.ds.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 报表生成器：把 PostgreSQL university 库的 sales 表（教材图 5-16 的 OLAP 事实表）
 * 渲染成一份自包含的 HTML 报表——概览、单维汇总、交叉表、明细四段。
 *
 * <p>用法（连接信息取自 src/main/resources/pg.properties）：</p>
 * <pre>
 *   java -cp "target/classes;target/lib/postgresql-42.7.4.jar" \
 *        com.ds.report.SalesReportGenerator
 *   java ... com.ds.report.SalesReportGenerator -d university -o target/report/sales-report.html
 * </pre>
 */
public class SalesReportGenerator {

    /** 尺码的展示顺序；库中若出现其他取值，按字母序追加在后面 */
    private static final List<String> SIZE_ORDER = Arrays.asList("small", "medium", "large");

    private static final String CSS =
            "body{font-family:-apple-system,'Segoe UI','Microsoft YaHei',sans-serif;"
            + "margin:0;padding:32px;background:#f5f6f8;color:#1f2328}"
            + "h1{font-size:22px;margin:0 0 4px}h2{font-size:16px;margin:32px 0 10px;"
            + "padding-left:9px;border-left:3px solid #2f6feb}"
            + ".meta{color:#656d76;font-size:12px;margin-bottom:20px}"
            + ".cards{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:8px}"
            + ".card{background:#fff;border:1px solid #d8dee4;border-radius:8px;"
            + "padding:12px 16px;min-width:130px}"
            + ".card .k{font-size:12px;color:#656d76}.card .v{font-size:20px;font-weight:600}"
            + "table{border-collapse:collapse;background:#fff;width:100%;"
            + "border:1px solid #d8dee4;border-radius:8px;overflow:hidden;font-size:13px}"
            + "th,td{padding:7px 12px;border-bottom:1px solid #eaeef2;text-align:left}"
            + "th{background:#f6f8fa;font-weight:600}"
            + "td.n,th.n{text-align:right;font-variant-numeric:tabular-nums}"
            + "tr.total td{font-weight:600;background:#f6f8fa}"
            + ".bar{display:inline-block;height:9px;background:#2f6feb;border-radius:2px;"
            + "vertical-align:middle;margin-right:6px;opacity:.8}"
            + ".foot{color:#656d76;font-size:12px;margin-top:28px;border-top:1px solid #d8dee4;"
            + "padding-top:10px}code{background:#eff1f3;padding:1px 5px;border-radius:4px}";

    public static void main(String[] args) throws Exception {
        String db = "university";
        String out = "target/report/sales-report.html";
        for (int i = 0; i < args.length - 1; i++) {
            if ("-d".equals(args[i])) {
                db = args[++i];
            } else if ("-o".equals(args[i])) {
                out = args[++i];
            }
        }

        Properties props = loadDbConfig();
        String url = "jdbc:postgresql://localhost:" + props.getProperty("port", "5432") + "/" + db;
        long started = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(
                url, props.getProperty("user"), props.getProperty("password"))) {
            ReportData data = read(conn);
            Path written = writeHtml(Paths.get(out), data, url);
            printSummary(data, written);
        }
        System.out.println("耗时 " + (System.currentTimeMillis() - started) + " ms");
    }

    // ---------- 数据抽取 ----------

    /** 一次报表所需的全部聚合结果 */
    static final class ReportData {
        final List<String[]> detail = new ArrayList<>();
        final List<Agg> byItem = new ArrayList<>();
        final List<Agg> byColor = new ArrayList<>();
        final List<Agg> bySize = new ArrayList<>();
        final Map<String, Map<String, Long>> itemSize = new LinkedHashMap<>();
        long totalQuantity;
        int maxQuantity;
        String maxRow = "";
    }

    static final class Agg {
        final String name;
        final long quantity;
        Agg(String name, long quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }

    private static ReportData read(Connection conn) throws SQLException {
        ReportData d = new ReportData();
        try (Statement st = conn.createStatement()) {
            // 明细：按教材原表顺序（服装 → 颜色 → 尺码）
            try (ResultSet rs = st.executeQuery(
                    "SELECT item_name, color, clothes_size, quantity FROM sales"
                    + " ORDER BY item_name, color, clothes_size")) {
                while (rs.next()) {
                    int qty = rs.getInt("quantity");
                    d.detail.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3),
                            Integer.toString(qty)});
                    if (qty > d.maxQuantity) {
                        d.maxQuantity = qty;
                        d.maxRow = rs.getString(1) + " / " + rs.getString(2) + " / " + rs.getString(3);
                    }
                }
            }
            d.byItem.addAll(groupBy(st, "item_name"));
            d.byColor.addAll(groupBy(st, "color"));
            d.bySize.addAll(groupBy(st, "clothes_size"));

            // 交叉表：item × size 在 Java 侧透视，避免拼接动态列 SQL
            try (ResultSet rs = st.executeQuery(
                    "SELECT item_name, clothes_size, sum(quantity) AS qty FROM sales"
                    + " GROUP BY item_name, clothes_size")) {
                while (rs.next()) {
                    d.itemSize.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>())
                            .put(rs.getString(2), rs.getLong("qty"));
                }
            }
        }
        d.totalQuantity = d.byItem.stream().mapToLong(a -> a.quantity).sum();
        return d;
    }

    /** 按单个维属性汇总，结果按销量降序 */
    private static List<Agg> groupBy(Statement st, String column) throws SQLException {
        List<Agg> list = new ArrayList<>();
        try (ResultSet rs = st.executeQuery("SELECT " + column + " AS name, sum(quantity) AS qty"
                + " FROM sales GROUP BY " + column + " ORDER BY qty DESC, name")) {
            while (rs.next()) {
                list.add(new Agg(rs.getString("name"), rs.getLong("qty")));
            }
        }
        return list;
    }

    /** 交叉表的列顺序：先固定语义顺序，再补充库中出现的其他尺码 */
    private static List<String> sizeColumns(ReportData d) {
        List<String> sizes = new ArrayList<>();
        for (Agg a : d.bySize) {
            if (SIZE_ORDER.contains(a.name)) {
                sizes.add(a.name);
            }
        }
        sizes.sort((a, b) -> SIZE_ORDER.indexOf(a) - SIZE_ORDER.indexOf(b));
        for (Agg a : d.bySize) {
            if (!sizes.contains(a.name)) {
                sizes.add(a.name);
            }
        }
        return sizes;
    }

    // ---------- HTML 渲染 ----------

    private static Path writeHtml(Path out, ReportData d, String url) throws IOException {
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        List<String> sizes = sizeColumns(d);
        long maxDim = 0;
        for (Agg a : d.byItem) {
            maxDim = Math.max(maxDim, a.quantity);
        }
        for (List<Agg> dim : Arrays.asList(d.byColor, d.bySize)) {
            for (Agg a : dim) {
                maxDim = Math.max(maxDim, a.quantity);
            }
        }

        try (PrintWriter w = new PrintWriter(
                Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            w.println("<!DOCTYPE html>");
            w.println("<html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">");
            w.println("<title>sales 表报表 · university 库</title>");
            w.println("<style>" + CSS + "</style></head><body>");
            w.println("<h1>sales 表报表</h1>");
            w.println("<div class=\"meta\">数据源 " + esc(url) + " · 生成时间 "
                    + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                    + " · SalesReportGenerator</div>");

            w.println("<div class=\"cards\">");
            card(w, "明细行数", String.valueOf(d.detail.size()));
            card(w, "总销量 quantity", String.valueOf(d.totalQuantity));
            card(w, "维属性", "item_name / color / clothes_size");
            card(w, "单笔最大", d.maxQuantity + "（" + esc(d.maxRow) + "）");
            w.println("</div>");

            w.println("<h2>1. 单维汇总</h2>");
            dimTable(w, "按服装", "item_name", d.byItem, d.totalQuantity, maxDim);
            dimTable(w, "按颜色", "color", d.byColor, d.totalQuantity, maxDim);
            dimTable(w, "按尺码", "clothes_size", d.bySize, d.totalQuantity, maxDim);

            w.println("<h2>2. 交叉表：item_name × clothes_size</h2>");
            crossTable(w, d, sizes);

            w.println("<h2>3. 明细（" + d.detail.size() + " 行）</h2>");
            detailTable(w, d);

            w.println("<div class=\"foot\">合计校验：sum(quantity) = " + d.totalQuantity
                    + "，行数 = " + d.detail.size()
                    + "。重跑命令：<code>java -cp \"target/classes;target/lib/postgresql-42.7.4.jar\""
                    + " com.ds.report.SalesReportGenerator</code></div>");
            w.println("</body></html>");
        }
        return out;
    }

    private static void card(PrintWriter w, String key, String value) {
        w.println("<div class=\"card\"><div class=\"k\">" + esc(key) + "</div><div class=\"v\">"
                + esc(value) + "</div></div>");
    }

    private static void dimTable(PrintWriter w, String label, String column, List<Agg> rows,
                                 long total, long max) {
        w.println("<p style=\"font-size:13px;color:#656d76;margin:14px 0 6px\"><b>"
                + esc(label) + " " + esc(column) + "</b></p>");
        w.println("<table><tr><th>" + esc(column) + "</th><th class=\"n\">销量</th>"
                + "<th class=\"n\">占比</th><th>分布</th></tr>");
        for (Agg a : rows) {
            int pct = total == 0 ? 0 : (int) Math.round(a.quantity * 100.0 / total);
            int bar = max == 0 ? 0 : (int) Math.round(a.quantity * 120.0 / max);
            w.println("<tr><td>" + esc(a.name) + "</td><td class=\"n\">" + a.quantity
                    + "</td><td class=\"n\">" + pct + "%</td><td><span class=\"bar\" style=\"width:"
                    + bar + "px\"></span></td></tr>");
        }
        w.println("<tr class=\"total\"><td>合计</td><td class=\"n\">" + total
                + "</td><td class=\"n\">100%</td><td></td></tr></table>");
    }

    private static void crossTable(PrintWriter w, ReportData d, List<String> sizes) {
        w.println("<table><tr><th>item_name</th>");
        for (String s : sizes) {
            w.println("<th class=\"n\">" + esc(s) + "</th>");
        }
        w.println("<th class=\"n\">合计</th><th class=\"n\">占比</th></tr>");
        for (Agg item : d.byItem) {
            Map<String, Long> row = d.itemSize.getOrDefault(item.name, new LinkedHashMap<>());
            w.print("<tr><td>" + esc(item.name) + "</td>");
            for (String s : sizes) {
                Long v = row.get(s);
                w.print("<td class=\"n\">" + (v == null ? "-" : v) + "</td>");
            }
            int pct = d.totalQuantity == 0 ? 0
                    : (int) Math.round(item.quantity * 100.0 / d.totalQuantity);
            w.println("<td class=\"n\">" + item.quantity + "</td><td class=\"n\">" + pct + "%</td></tr>");
        }
        w.print("<tr class=\"total\"><td>合计</td>");
        for (String s : sizes) {
            long colSum = d.bySize.stream().filter(a -> a.name.equals(s))
                    .mapToLong(a -> a.quantity).sum();
            w.print("<td class=\"n\">" + colSum + "</td>");
        }
        w.println("<td class=\"n\">" + d.totalQuantity + "</td><td class=\"n\">100%</td></tr></table>");
    }

    private static void detailTable(PrintWriter w, ReportData d) {
        w.println("<table><tr><th>item_name</th><th>color</th><th>clothes_size</th>"
                + "<th class=\"n\">quantity</th></tr>");
        for (String[] r : d.detail) {
            w.println("<tr><td>" + esc(r[0]) + "</td><td>" + esc(r[1]) + "</td><td>" + esc(r[2])
                    + "</td><td class=\"n\">" + esc(r[3]) + "</td></tr>");
        }
        w.println("<tr class=\"total\"><td colspan=\"3\">合计（" + d.detail.size() + " 行）</td>"
                + "<td class=\"n\">" + d.totalQuantity + "</td></tr></table>");
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void printSummary(ReportData d, Path out) {
        System.out.println("报表已生成: " + out.toAbsolutePath());
        System.out.println("  明细行数 = " + d.detail.size() + ", sum(quantity) = " + d.totalQuantity);
        System.out.print("  按服装: ");
        for (Agg a : d.byItem) {
            System.out.print(a.name + "=" + a.quantity + "  ");
        }
        System.out.println();
        System.out.print("  按颜色: ");
        for (Agg a : d.byColor) {
            System.out.print(a.name + "=" + a.quantity + "  ");
        }
        System.out.println();
        System.out.print("  按尺码: ");
        for (Agg a : d.bySize) {
            System.out.print(a.name + "=" + a.quantity + "  ");
        }
        System.out.println();
    }

    /** 从 pg.properties 读取连接配置：优先 classpath，其次相对路径兜底 */
    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        for (String path : new String[] {"/pg.properties", "src/main/resources/pg.properties",
                "pg.properties"}) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = SalesReportGenerator.class.getResourceAsStream(path);
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
}
