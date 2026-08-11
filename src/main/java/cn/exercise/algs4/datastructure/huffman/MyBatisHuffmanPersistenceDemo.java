package cn.exercise.algs4.datastructure.huffman;

import cn.exercise.algs4.datastructure.huffman.db.CodeEntry;
import cn.exercise.algs4.datastructure.huffman.db.CodeEntryMapper;
import cn.exercise.algs4.datastructure.huffman.db.Document;
import cn.exercise.algs4.datastructure.huffman.db.DocumentMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 玩法 A（MyBatis 版）：哈夫曼压缩结果的数据持久化演示
 *
 * <p>与 {@link HuffmanPersistenceDemo}（纯 JDBC 版）做同一件事，换成 MyBatis 实现：</p>
 * <pre>
 *   明文文件(data.txt) ──HuffmanCoding.fullEncode()──▶ 压缩结果
 *        │
 *        ▼  MyBatis Mapper（DocumentMapper / CodeEntryMapper，同一 SqlSession 事务）
 *   MySQL huffman_demo 库（documents + code_table 两表）
 *        │
 *        ▼  MyBatis selectById 读回 BLOB → bytesToTree() → decode()
 *   ✅ 译码结果与原文明文比对
 * </pre>
 *
 * <p>DDL（建库建表）用原生 JDBC 完成；数据读写全部走 MyBatis。</p>
 * <p>连接信息从 {@code src/main/resources/db.properties} 读取。</p>
 */
public class MyBatisHuffmanPersistenceDemo {

    private static final String DB_NAME = "huffman_demo";

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║     哈夫曼 × MyBatis 数据持久化 演示        ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // 1. 读取连接配置 + 用原生 JDBC 建库建表（DDL 交给 JDBC，MyBatis 专注数据读写）
        Properties props = loadDbConfig();
        initDatabase(props);

        // 2. 构建 SqlSessionFactory（解析 mybatis-config.xml）
        SqlSessionFactory factory = buildSessionFactory();

        // 3. 读明文并做哈夫曼压缩
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

        // 4. MyBatis 存库（documents + code_table，同一事务）
        int docId = saveViaMyBatis(factory, text, encoded, treeBytes, codes);

        // 5. 从 MySQL 读回并还原校验
        restoreAndVerify(factory, docId, text);

        // 6. 查询展示：编码表 + 统计
        printCodeTable(factory, docId);
        printStats(factory);

        // 7. 事务回滚演示
        demoRollback(factory);
    }

    // ====================== 数据库初始化 ======================

    /** 从 db.properties 读取连接配置：优先 classpath，其次相对路径兜底 */
    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/db.properties", "src/main/resources/db.properties", "db.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = MyBatisHuffmanPersistenceDemo.class.getResourceAsStream(path);
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

    /** 原生 JDBC 建库建表（表结构与 JDBC 演示一致） */
    private static void initDatabase(Properties props) throws Exception {
        String url = "jdbc:mysql://localhost:" + props.getProperty("port", "3306")
                + "?useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement()) {
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

    /** 解析 mybatis-config.xml 构建 SqlSessionFactory */
    private static SqlSessionFactory buildSessionFactory() throws IOException {
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) {
            return new SqlSessionFactoryBuilder().build(in);
        }
    }

    // ====================== 数据读写（MyBatis） ======================

    /**
     * 通过 MyBatis 保存压缩结果：
     * 插入 documents 单条 + 批量插入 code_table，同一 SqlSession 手动事务
     *
     * @return 生成的 documents.id
     */
    private static int saveViaMyBatis(SqlSessionFactory factory, String text, String encoded,
                                      byte[] treeBytes, Map<Character, String> codes) throws Exception {
        // openSession(false) = 手动管理事务，不自动提交
        try (SqlSession session = factory.openSession(false)) {
            DocumentMapper docMapper = session.getMapper(DocumentMapper.class);
            CodeEntryMapper codeMapper = session.getMapper(CodeEntryMapper.class);

            int docId;
            try {
                // 1) 插入文档（useGeneratedKeys 自动回填 id）
                Document doc = new Document();
                doc.setDocName("data.txt");
                doc.setOriginalText(text);
                doc.setEncodedBits(encoded);
                doc.setTreeBlob(treeBytes);
                docMapper.insert(doc);
                docId = doc.getId();

                // 2) 批量插入编码表（一个字符一行）
                List<CodeEntry> entries = new ArrayList<>(codes.size());
                for (Map.Entry<Character, String> e : codes.entrySet()) {
                    CodeEntry ce = new CodeEntry();
                    ce.setDocId(docId);
                    ce.setCh(String.valueOf(e.getKey()));
                    ce.setCode(e.getValue());
                    entries.add(ce);
                }
                int rows = codeMapper.batchInsert(entries);

                session.commit();   // 两个操作一起提交
                System.out.println("💾 [MyBatis] 已持久化: documents(id=" + docId + ") + code_table("
                        + rows + " 行)，事务已提交");
                return docId;
            } catch (Exception ex) {
                session.rollback(); // 任一失败整体回滚
                throw ex;
            }
        }
    }

    /** 从 MySQL 读回压缩数据，重建哈夫曼树译码，与原文明文比对 */
    private static void restoreAndVerify(SqlSessionFactory factory, int docId, String original) {
        System.out.println("\n──────────────── 从数据库读回还原（MyBatis） ────────────────");
        try (SqlSession session = factory.openSession()) {
            Document doc = session.getMapper(DocumentMapper.class).selectById(docId);
            if (doc == null) {
                System.out.println("❌ 数据库中没有 id=" + docId + " 的记录");
                return;
            }
            System.out.println("📤 [MyBatis] 读回: 明文=" + doc.getOriginalText().length() + " 字符, "
                    + "bit串=" + doc.getEncodedBits().length() + " bit, 树BLOB="
                    + doc.getTreeBlob().length + " 字节");

            String decoded = null;
            try {
                HuffmanCoding.HuffmanNode root = HuffmanCoding.bytesToTree(doc.getTreeBlob());
                System.out.println("🌳 已从 BLOB 重建哈夫曼树");
                decoded = HuffmanCoding.decode(doc.getEncodedBits(), root);
            } catch (IOException e) {
                System.err.println("❌ 树反序列化失败: " + e.getMessage());
            }

            boolean match = decoded != null && decoded.equals(original);
            System.out.println("📊 一致性校验: 译码结果 " + (match ? "✅ 与原文完全一致！" : "❌ 不一致！"));
            if (decoded != null && !decoded.isEmpty()) {
                System.out.println("   译码还原: \"" + decoded + "\"");
            }
        }
    }

    /** 查询演示：打印某文档的哈夫曼编码表 */
    private static void printCodeTable(SqlSessionFactory factory, int docId) {
        System.out.println("\n──────────────── 编码表查询 (code_table) ────────────────");
        try (SqlSession session = factory.openSession()) {
            List<CodeEntry> entries = session.getMapper(CodeEntryMapper.class).selectByDocId(docId);
            for (CodeEntry e : entries) {
                String display = " ".equals(e.getCh()) ? "' '" : e.getCh();
                System.out.printf("   字符 %-4s → %s%n", display, e.getCode());
            }
        }
    }

    /** 统计演示：库中已持久化的文档概况 */
    private static void printStats(SqlSessionFactory factory) {
        System.out.println("\n──────────────── 持久化统计 ────────────────");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (SqlSession session = factory.openSession()) {
            List<Document> docs = session.getMapper(DocumentMapper.class).selectAll();
            for (Document d : docs) {
                System.out.printf("   #%d %-10s 编码表=%d 行, bit串=%d bit, 入库时间=%s%n",
                        d.getId(), d.getDocName(),
                        session.getMapper(CodeEntryMapper.class).selectByDocId(d.getId()).size(),
                        d.getEncodedBits().length(),
                        d.getCreatedAt() == null ? "-" : fmt.format(d.getCreatedAt()));
            }
        }
    }

    /**
     * 事务回滚演示：先插入一条合法文档，再故意插入超长字符触发 SQL 异常，
     * 验证 MyBatis 事务把两次操作一起回滚（数据库里不会残留半截数据）。
     */
    private static void demoRollback(SqlSessionFactory factory) {
        System.out.println("\n──────────────── 事务回滚演示 ────────────────");
        System.out.println("  故意写入一个超长字符('verylongchar_overflow')，触发 Data too long 异常...");

        try (SqlSession session = factory.openSession(false)) {
            DocumentMapper docMapper = session.getMapper(DocumentMapper.class);
            CodeEntryMapper codeMapper = session.getMapper(CodeEntryMapper.class);
            try {
                Document doc = new Document();
                doc.setDocName("rollback-demo");
                doc.setOriginalText("should not persist");
                doc.setEncodedBits("0");
                doc.setTreeBlob(new byte[]{1, 2, 3});
                docMapper.insert(doc);                 // 第一步会成功
                System.out.println("   ① 文档已插入(id=" + doc.getId() + ")，继续插入编码表...");

                // ch 列是 VARCHAR(8)，超长字符串必然触发严格模式报错
                CodeEntry bad = new CodeEntry();
                bad.setDocId(doc.getId());
                bad.setCh("verylongchar_overflow");
                bad.setCode("0");
                List<CodeEntry> badList = new ArrayList<>();
                badList.add(bad);
                codeMapper.batchInsert(badList);       // 这一步抛 SQLException

                session.commit();                      // 不会执行到这里
            } catch (Exception ex) {
                session.rollback();
                System.out.println("   ② 编码表插入触发异常: " + ex.getMessage());
            }
        }

        // 回滚后用独立查询验证：rollback-demo 文档确实不存在
        try (SqlSession session = factory.openSession()) {
            List<Document> remain = new ArrayList<>();
            for (Document d : session.getMapper(DocumentMapper.class).selectAll()) {
                if ("rollback-demo".equals(d.getDocName())) {
                    remain.add(d);
                }
            }
            System.out.println("   ③ 回滚后查询 rollback-demo 文档: " + (remain.isEmpty()
                    ? "✅ 不存在（事务已整体回滚，无半截数据残留）"
                    : "❌ 残留了 " + remain.size() + " 条"));
        }
        System.out.println("   ✅ 回滚演示完成: 事务保证『要么全部成功，要么全部不写』");
    }
}
