package com.ds.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * JDBC 大对象（LOB）演示 —— PostgreSQL 版。
 *
 * <p>TEXT 列按 CLOB 读取，BYTEA 列按 BLOB 读取；写入和读取都使用流，
 * 避免把整个大对象一次性放进 Java 堆内存。
 */
public class PgLobDemo {
    private static final String DB_NAME = "lob_demo";

    public static void main(String[] args) throws Exception {
        Properties props = loadDbConfig();
        ensureDatabase(props);

        String url = "jdbc:postgresql://localhost:" + props.getProperty("port", "5432") + "/" + DB_NAME;
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"))) {
            initTable(conn);

            String text = repeat("Hello, JDBC LOB! ", 20_000);
            byte[] image = pseudoRandomBytes(1_000_000);
            insert(conn, "demo.txt", text, image);
            read(conn, "demo.txt");
        }
    }

    private static void initTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS documents");
            st.execute("CREATE TABLE documents ("
                    + "name VARCHAR(100) PRIMARY KEY, "
                    + "content TEXT, "
                    + "attachment BYTEA)");
        }
        System.out.println("✅ 表就绪: documents(content TEXT, attachment BYTEA)");
    }

    private static void insert(Connection conn, String name, String text, byte[] image)
            throws SQLException, IOException {
        String sql = "INSERT INTO documents(name, content, attachment) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setCharacterStream(2, new java.io.StringReader(text), text.length());
            try (InputStream in = new java.io.ByteArrayInputStream(image)) {
                ps.setBinaryStream(3, in, image.length);
            }
            ps.executeUpdate();
        }
        System.out.println("✅ 插入完成: CLOB=" + text.length() + " chars, BLOB=" + image.length + " bytes");
    }

    private static void read(Connection conn, String name) throws SQLException, IOException {
        String sql = "SELECT content, attachment FROM documents WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("记录不存在: " + name);
                }

                // PostgreSQL TEXT 列直接按字符流读取，不要调用 getClob
//                String content = rs.getString("content");
//                String first = content.length() > 20 ? content.substring(0, 20) : content;
//                System.out.println("📖 TEXT: length=" + content.length() + ", prefix=" + first + "...");

                try (Reader reader = rs.getCharacterStream("content")) {
                    String first = readFirst(reader, 20);
                    System.out.println("📖 TEXT: length=" + reader.toString().length() + ", prefix=" + first + "...");
                }





                // PostgreSQL BYTEA 列直接按字节数组读取，不要调用 getBlob
//                byte[] attachment = rs.getBytes("attachment");
//                byte[] header = java.util.Arrays.copyOf(attachment, 16);
//                System.out.println("📖 BYTEA: length=" + attachment.length
//                        + ", header[0..15]=" + java.util.Arrays.toString(header));


                try (InputStream in = rs.getBinaryStream("attachment")) {
                    byte[] header = readBytes(in, 16);
                    System.out.println("📖 BYTEA: length=" + in.available() + ", header[0..15]=" + java.util.Arrays.toString(header));
                }
            }
        }
    }

    private static String readFirst(Reader reader, int limit) throws IOException {
        char[] chars = new char[limit];
        int length = 0;
        while (length < limit) {
            int count = reader.read(chars, length, limit - length);
            if (count < 0) {
                break;
            }
            length += count;
        }
        return new String(chars, 0, length);
    }

    private static byte[] readBytes(InputStream in, int limit) throws IOException {
        byte[] bytes = new byte[limit];
        int length = 0;
        while (length < limit) {
            int count = in.read(bytes, length, limit - length);
            if (count < 0) {
                break;
            }
            length += count;
        }
        if (length != limit) {
            throw new IOException("二进制流提前结束: expected=" + limit + ", actual=" + length);
        }
        return bytes;
    }

    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/pg.properties", "src/main/resources/pg.properties", "pg.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = PgLobDemo.class.getResourceAsStream(path);
            }
            if (in == null) {
                File file = new File(path);
                if (file.exists()) {
                    in = new FileInputStream(file);
                }
            }
            if (in != null) {
                try (InputStream stream = in) {
                    props.load(stream);
                }
                return props;
            }
        }
        throw new IOException("找不到 pg.properties，请确认文件存在于 src/main/resources/");
    }

    private static void ensureDatabase(Properties props) throws SQLException {
        String adminUrl = "jdbc:postgresql://localhost:" + props.getProperty("port", "5432") + "/postgres";
        try (Connection conn = DriverManager.getConnection(adminUrl,
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'")) {
            if (!rs.next()) {
                st.execute("CREATE DATABASE " + DB_NAME);
                System.out.println("🛠 已创建数据库: " + DB_NAME);
            }
        }
    }

    private static String repeat(String text, int count) {
        StringBuilder sb = new StringBuilder(text.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(text);
        }
        return sb.toString();
    }

    private static byte[] pseudoRandomBytes(int length) {
        byte[] bytes = new byte[length];
        int seed = 20260828;
        for (int i = 0; i < length; i++) {
            seed = seed * 1103515245 + 12345;
            bytes[i] = (byte) (seed >>> 16);
        }
        return bytes;
    }
}
