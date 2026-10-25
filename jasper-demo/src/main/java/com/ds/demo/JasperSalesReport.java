package com.ds.demo;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

/**
 * 用 JasperReports 报表引擎为 PostgreSQL university 库的 sales 表出报表：
 * 编译 sales_report.jrxml → 用 JDBC 连接填数据 → 导出 PDF / XLSX / HTML。
 *
 * 运行（在 jasper-demo 目录）：
 *   mvn -q compile exec:java
 * 产物在 jasper-demo/target/report/。
 */
public class JasperSalesReport {

    public static void main(String[] args) throws Exception {
        Properties pg = loadPgConfig();
        String url = "jdbc:postgresql://localhost:" + pg.getProperty("port", "5432")
                + "/university";

        // 1) 模板编译：.jrxml → JasperReport（顺带落一份 .jasper 二进制模板）
        Path dir = Paths.get("target/report");
        Files.createDirectories(dir);
        Path jrxml = Paths.get("src/main/resources/sales_report.jrxml");
        JasperReport report = JasperCompileManager.compileReport(jrxml.toString());
        JasperCompileManager.compileReportToFile(jrxml.toString(),
                dir.resolve("sales_report.jasper").toString());

        // 2) 填数据：引擎自己跑模板里的 SQL（含柱状图的子数据集）
        Map<String, Object> params = new HashMap<>();
        params.put("JDBC_URL", url);
        JasperPrint print;
        try (Connection conn = DriverManager.getConnection(url,
                pg.getProperty("user"), pg.getProperty("password"))) {
            print = JasperFillManager.fillReport(report, params, conn);
        }

        // 3) 三种导出
        JasperExportManager.exportReportToPdfFile(print, dir.resolve("sales_report.pdf").toString());
        JasperExportManager.exportReportToHtmlFile(print, dir.resolve("sales_report.html").toString());
        exportXlsx(print, dir.resolve("sales_report.xlsx").toString());

        System.out.println("pages = " + print.getPages().size());
        for (Path p : new Path[] {dir.resolve("sales_report.pdf"), dir.resolve("sales_report.xlsx"),
                dir.resolve("sales_report.html"), dir.resolve("sales_report.jasper")}) {
            File f = p.toFile();
            System.out.println(String.format("%-28s %8d bytes", f.getName(), f.length()));
        }
    }

    private static void exportXlsx(JasperPrint print, String out) throws Exception {
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimpleXlsxReportConfiguration cfg = new SimpleXlsxReportConfiguration();
        cfg.setDetectCellType(true);
        cfg.setCollapseRowSpan(false);
        exporter.setConfiguration(cfg);
        exporter.exportReport();
    }

    /** 复用主工程的 PostgreSQL 连接配置 */
    private static Properties loadPgConfig() throws Exception {
        for (String candidate : new String[] {"../src/main/resources/pg.properties",
                "src/main/resources/pg.properties"}) {
            InputStream in = new File(candidate).exists()
                    ? Files.newInputStream(Paths.get(candidate)) : null;
            if (in == null) {
                continue;
            }
            try (InputStream is = in) {
                Properties props = new Properties();
                props.load(is);
                return props;
            }
        }
        throw new IllegalStateException("找不到 pg.properties（主工程 src/main/resources 下）");
    }
}
