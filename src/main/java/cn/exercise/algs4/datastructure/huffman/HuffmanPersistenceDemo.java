package cn.exercise.algs4.datastructure.huffman;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

/**
 * 玩法 A：哈夫曼压缩结果的数据持久化演示
 *
 * <p>演示链路：</p>
 * <pre>
 *   明文文件(data.txt)
 *        │  HuffmanCoding.fullEncode() 压缩
 *        ▼
 *   { 哈夫曼树, 编码表, 编码bit串 }
 *        │  JDBC 持久化（含事务、批量插入）
 *        ▼
 *   MySQL 数据库 huffman_demo
 *        │  JDBC 读回（BLOB → 重建哈夫曼树）
 *        ▼
 *   字节还原 → decode() 译码 → 与原文明文比对
 * </pre>
 *
 * <p>数据库连接信息从 {@code src/main/resources/db.properties} 读取。</p>
 */
public class HuffmanPersistenceDemo {

    private static final String DB_NAME = "huffman_demo";

    public static void main(String[] args) throws Exception {
        // 1. 读取数据库连接配置
        Properties props = loadDbConfig();
        String user = props.getProperty("user", "root");
        String password = props.getProperty("password", "");
        String port = props.getProperty("port", "3306");
        String url = "jdbc:mysql://localhost:" + port
                + "?useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     哈夫曼 × MySQL 数据持久化 演示        ║");
        System.out.println("╚══════════════════════════════════════════╝");

        // 2. 连接 MySQL 并初始化库表
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            initDatabase(conn);
            conn.setCatalog(DB_NAME);

            // 3. 读取明文件并做哈夫曼压缩
            String text = readPlainText();
            System.out.println("📖 明文: \"" + text + "\" (" + text.length() + " 字符)");

            Object[] result = HuffmanCoding.fullEncode(text);
            @SuppressWarnings("unchecked")
            Map<Character, String> codes = (Map<Character, String>) result[1];
            String encoded = (String) result[2];
            byte[] treeBytes = HuffmanCoding.treeToBytes((HuffmanCoding.HuffmanNode) result[0]);

            System.out.println("🗜 Huffman 压缩完成: " + encoded.length() + " bit ("
                    + (encoded.length() + 7) / 8 + " 字节)");
            System.out.println("🌳 哈夫曼树序列化: " + treeBytes.length + " 字节");

            // 4. 持久化到 MySQL（文档 + 编码表，同一事务）
            int docId = persistToDb(conn, text, encoded, treeBytes, codes);

            // 5. 从 MySQL 读回并还原校验
            restoreAndVerify(conn, docId, text);

            // 6. 查询演示：编码表 + 压缩统计
            printCodeTable(conn, docId);
            printStats(conn);
        }
    }

    /** 从 db.properties 读取连接配置：优先 classpath，其次相对路径兜底 */
    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/db.properties", "src/main/resources/db.properties", "db.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = HuffmanPersistenceDemo.class.getResourceAsStream(path);
            }
            if (in == null) {
                java.io.File f = new java.io.File(path);
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

    /** 读取待压缩的明文文件 data.txt */
    private static String readPlainText() throws IOException {
        String[] candidates = {"data.txt", "../data.txt"};
        for (String path : candidates) {
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            }
        }
        throw new IOException("找不到 data.txt");
    }

    /** 建库建表：huffman_demo.documents + code_table */
    private static void initDatabase(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME
                    + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            st.execute("CREATE TABLE IF NOT EXISTS " + DB_NAME + ".documents ("
                    + "  id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "  doc_name VARCHAR(64) NOT NULL COMMENT '文档名',"
                    + "  original_text LONGTEXT NOT NULL COMMENT '原文明文',"
                    + "  encoded_bits LONGTEXT NOT NULL COMMENT '哈夫曼编码bit串',"
                    + "  tree_blob LONGBLOB NOT NULL COMMENT '哈夫曼树序列化字节',"
                    + "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间'"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='哈夫曼压缩文档'");
            st.execute("CREATE TABLE IF NOT EXISTS " + DB_NAME + ".code_table ("
                    + "  doc_id INT NOT NULL COMMENT '关联文档',"
                    + "  ch VARCHAR(8) NOT NULL COMMENT '字符',"
                    + "  code VARCHAR(32) NOT NULL COMMENT '哈夫曼编码',"
                    + "  PRIMARY KEY (doc_id, ch),"
                    + "  CONSTRAINT fk_code_doc FOREIGN KEY (doc_id) REFERENCES "
                    + DB_NAME + ".documents(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='哈夫曼编码表'");
        }
        System.out.println("🛠 库表就绪: " + DB_NAME + ".documents / .code_table");
    }

    /** 将压缩结果写入 MySQL：documents 单条 + code_table 批量，整体一个事务 */
    private static int persistToDb(Connection conn, String text, String encoded,
                                   byte[] treeBytes, Map<Character, String> codes) throws SQLException {
        int docId;
        conn.setAutoCommit(false);   // 开启事务
        try {
            // 插入文档（含压缩数据与序列化树）
            String insertDoc = "INSERT INTO documents (doc_name, original_text, encoded_bits, tree_blob) "
                    + "VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertDoc,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "data.txt");
                ps.setString(2, text);
                ps.setString(3, encoded);
                ps.setBytes(4, treeBytes);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    docId = keys.getInt(1);
                }
            }

            // 批量插入编码表（一行一个字符的编码）
            String insertCode = "INSERT INTO code_table (doc_id, ch, code) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertCode)) {
                for (Map.Entry<Character, String> e : codes.entrySet()) {
                    ps.setInt(1, docId);
                    ps.setString(2, String.valueOf(e.getKey()));
                    ps.setString(3, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();          // 提交事务
        } catch (SQLException ex) {
            conn.rollback();        // 任一步失败则整体回滚
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
        System.out.println("💾 已持久化到 MySQL: documents(id=" + docId + ") + code_table("
                + codes.size() + " 行)");
        return docId;
    }

    /** 从 MySQL 读回压缩数据，重建哈夫曼树并译码，与原文明文比对 */
    private static void restoreAndVerify(Connection conn, int docId, String original) throws SQLException {
        System.out.println("\n──────────────── 从数据库读回还原 ────────────────");
        String sql = "SELECT original_text, encoded_bits, tree_blob FROM documents WHERE id = ?";
        String readBackText, encoded;
        byte[] treeBytes;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                readBackText = rs.getString("original_text");
                encoded = rs.getString("encoded_bits");
                treeBytes = rs.getBytes("tree_blob");
            }
        }
        System.out.println("📤 从 documents 读回: 明文=" + readBackText.length() + " 字符, "
                + "bit串=" + encoded.length() + " bit, 树BLOB=" + treeBytes.length + " 字节");

        // BLOB → 重建哈夫曼树 → 译码
        String decoded = null;
        try {
            HuffmanCoding.HuffmanNode root = HuffmanCoding.bytesToTree(treeBytes);
            System.out.println("🌳 已从 BLOB 重建哈夫曼树");
            decoded = HuffmanCoding.decode(encoded, root);
        } catch (IOException e) {
            System.err.println("❌ 树反序列化失败: " + e.getMessage());
        }

        // 一致性校验：入库文本 == 译码文本
        boolean match = decoded != null && decoded.equals(original);
        System.out.println("📊 一致性校验: 译码结果 " + (match ? "✅ 与原文完全一致！" : "❌ 不一致！"));
        if (decoded != null && !decoded.isEmpty()) {
            System.out.println("   译码还原: \"" + decoded + "\"");
        }
    }

    /** 查询演示：打印该文档的哈夫曼编码表 */
    private static void printCodeTable(Connection conn, int docId) throws SQLException {
        System.out.println("\n──────────────── 编码表查询 (code_table) ────────────────");
        String sql = "SELECT ch, code, CHAR_LENGTH(code) AS len FROM code_table "
                + "WHERE doc_id = ? ORDER BY len, ch";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ch = rs.getString("ch");
                    String display = " ".equals(ch) ? "' '" : ch;
                    System.out.printf("   字符 %-4s → %s%n", display, rs.getString("code"));
                }
            }
        }
    }

    /** 统计演示：库中已持久化的文档概况 */
    private static void printStats(Connection conn) throws SQLException {
        System.out.println("\n──────────────── 持久化统计 ────────────────");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT d.id, d.doc_name, d.created_at, "
                             + "(SELECT COUNT(*) FROM code_table c WHERE c.doc_id = d.id) AS code_count, "
                             + "LENGTH(d.encoded_bits) AS bit_len "
                             + "FROM documents d ORDER BY d.id")) {
            while (rs.next()) {
                System.out.printf("   #%d %-10s 编码表=%d 行, bit串=%d bit, 入库时间=%s%n",
                        rs.getInt("id"), rs.getString("doc_name"),
                        rs.getInt("code_count"), rs.getInt("bit_len"),
                        rs.getTimestamp("created_at").toLocalDateTime().withNano(0));
            }
        }
    }
}
