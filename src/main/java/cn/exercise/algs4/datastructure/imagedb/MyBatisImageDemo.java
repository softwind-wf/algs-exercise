package cn.exercise.algs4.datastructure.imagedb;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis 图片大对象（BLOB）写入 → 读回 实战演示
 *
 * <p>与 {@code cn.exercise.algs4.datastructure.huffman.MyBatisHuffmanPersistenceDemo}
 * 同一套「纯 MyBatis」套路：JDBC 建库建表，数据读写全部走 MyBatis。</p>
 * <pre>
 *   内存生成一张 PNG 图片
 *        │
 *        ▼  MyBatis Mapper（ImageMapper，LONGBLOB 列）
 *   MySQL image_demo 库（images 表）
 *        │
 *        ▼  MyBatis selectById 读回 BLOB
 *   ✅ 逐字节比对 + 落盘到 target/ 可直接打开查看
 * </pre>
 *
 * <p>DDL（建库建表）用原生 JDBC 完成；数据读写全部走 MyBatis。</p>
 * <p>连接信息从 {@code src/main/resources/db.properties} 读取。</p>
 * <p>MyBatis 配置独立于 huffman 演示：{@code src/main/resources/mybatis-image-config.xml}。</p>
 */
public class MyBatisImageDemo {

    private static final String DB_NAME = "image_demo";
    private static final int IMG_WIDTH = 480;
    private static final int IMG_HEIGHT = 240;

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   MyBatis 图片大对象(BLOB) 写入 → 读出 实战演示  ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        // 1. 读取连接配置 + 用原生 JDBC 建库建表（DDL 交给 JDBC，MyBatis 专注数据读写）
        Properties props = loadDbConfig();
        initDatabase(props);

        // 2. 构建 SqlSessionFactory（解析 mybatis-image-config.xml）
        SqlSessionFactory factory = buildSessionFactory();

        // 3. 内存中生成一张示例 PNG 图（不依赖任何外部图片文件）
        byte[] png = generateSamplePng();
        writeToDisk(png, "target/original_demo-gradient.png");
        System.out.println("🖼 已生成示例 PNG: " + png.length + " 字节（原图已落盘 target/original_demo-gradient.png）");

        // 4. MyBatis INSERT 把图片字节写入数据库（LONGBLOB 列）
        int imageId = saveImageViaMyBatis(factory, "demo-gradient.png", "image/png", png);

        // 5. MyBatis SELECT 读回图片：逐字节校验 + 落盘成可打开的图片
        loadAndVerify(factory, imageId, png);

        // 6. 展示库中全部图片
        printAllImages(factory);
    }

    // ====================== 数据库初始化 ======================

    /** 从 db.properties 读取连接配置：优先 classpath，其次相对路径兜底 */
    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/db.properties", "src/main/resources/db.properties", "db.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = MyBatisImageDemo.class.getResourceAsStream(path);
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

    /** 原生 JDBC 建库建表（data 列用 LONGBLOB 存图片字节） */
    private static void initDatabase(Properties props) throws Exception {
        String url = "jdbc:mysql://localhost:" + props.getProperty("port", "3306")
                + "?useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url,
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME
                    + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            st.execute("CREATE TABLE IF NOT EXISTS " + DB_NAME + ".images ("
                    + "  id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "  name VARCHAR(64) NOT NULL COMMENT '图片文件名',"
                    + "  mime_type VARCHAR(32) NOT NULL COMMENT 'MIME 类型',"
                    + "  width INT NOT NULL COMMENT '图片宽(px)',"
                    + "  height INT NOT NULL COMMENT '图片高(px)',"
                    + "  data LONGBLOB NOT NULL COMMENT '图片二进制数据',"
                    + "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间'"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片大对象演示'");
        }
        System.out.println("🛠 库表就绪: " + DB_NAME + ".images（data 列为 LONGBLOB）");
    }

    /** 解析 mybatis-image-config.xml 构建 SqlSessionFactory */
    private static SqlSessionFactory buildSessionFactory() throws IOException {
        try (InputStream in = Resources.getResourceAsStream("mybatis-image-config.xml")) {
            return new SqlSessionFactoryBuilder().build(in);
        }
    }

    // ====================== 示例图片生成 ======================

    /** 内存中绘制一张渐变 PNG（蓝→橙、带两行说明文字），转成字节数组返回 */
    private static byte[] generateSamplePng() throws IOException {
        BufferedImage img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < IMG_HEIGHT; y++) {
            for (int x = 0; x < IMG_WIDTH; x++) {
                int r = 20 + (x * 200) / IMG_WIDTH;
                int g = 60 + (y * 150) / IMG_HEIGHT;
                int b = 220 - (x * 120) / IMG_WIDTH;
                img.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("MyBatis <-> MySQL BLOB", 42, 96);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("write image into LONGBLOB, read it back", 42, 136);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    /** 字节数组落盘，用于和读回的图片做肉眼对比 */
    private static void writeToDisk(byte[] bytes, String path) throws IOException {
        File f = new File(path);
        Files.createDirectories(f.toPath().getParent());
        Files.write(f.toPath(), bytes);
    }

    // ====================== 数据读写（MyBatis） ======================

    /** 通过 MyBatis 把图片字节 INSERT 进 images 表，返回自增主键 */
    private static int saveImageViaMyBatis(SqlSessionFactory factory, String name, String mimeType,
                                           byte[] png) {
        try (SqlSession session = factory.openSession(false)) {   // 手动事务，提交后关闭
            ImageMapper mapper = session.getMapper(ImageMapper.class);
            ImageRecord img = new ImageRecord();
            img.setName(name);
            img.setMimeType(mimeType);
            img.setWidth(IMG_WIDTH);
            img.setHeight(IMG_HEIGHT);
            img.setData(png);

            int rows = mapper.insert(img);   // useGeneratedKeys 回填 id
            session.commit();
            System.out.println("💾 [MyBatis] INSERT 成功: images(id=" + img.getId() + ", " + name
                    + ", " + png.length + " 字节)，影响 " + rows + " 行，事务已提交");
            return img.getId();
        }
    }

    /** 从 MySQL 读回图片，逐字节比对 + 落盘成可打开的 PNG */
    private static void loadAndVerify(SqlSessionFactory factory, int id, byte[] original)
            throws IOException {
        System.out.println("\n──────────── 从数据库读回图片（MyBatis SELECT） ────────────");
        try (SqlSession session = factory.openSession()) {
            ImageRecord img = session.getMapper(ImageMapper.class).selectById(id);
            if (img == null) {
                System.out.println("❌ 数据库中没有 id=" + id + " 的图片");
                return;
            }
            byte[] data = img.getData();
            System.out.println("📤 [MyBatis] 读回: " + img.getName() + " (" + img.getMimeType()
                    + "), " + img.getWidth() + "x" + img.getHeight() + ", " + data.length + " 字节");

            boolean identical = Arrays.equals(original, data);
            System.out.println("🔍 逐字节校验: 写入 " + original.length + " 字节 vs 读回 " + data.length
                    + " 字节 → " + (identical ? "✅ 完全一致！" : "❌ 不一致！"));

            // 落盘成真实图片文件，可直接打开对比原图
            String path = "target/readback_" + img.getId() + "_" + img.getName();
            writeToDisk(data, path);
            System.out.println("🗂 已把读回的图片写到磁盘（可打开肉眼对比）: " + new File(path).getAbsolutePath());
        }
    }

    /** 查询演示：打印库中所有图片记录 */
    private static void printAllImages(SqlSessionFactory factory) {
        System.out.println("\n──────────── 库中所有图片（SELECT * FROM images） ────────────");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (SqlSession session = factory.openSession()) {
            List<ImageRecord> list = session.getMapper(ImageMapper.class).selectAll();
            for (ImageRecord img : list) {
                System.out.printf("   #%d  %-20s %-10s %dx%d  %8d 字节  %s%n",
                        img.getId(), img.getName(), img.getMimeType(),
                        img.getWidth(), img.getHeight(), img.getData().length,
                        img.getCreatedAt() == null ? "-" : fmt.format(img.getCreatedAt()));
            }
        }
        System.out.println("\n✅ 演示结束：图片已通过 MyBatis 写入 LONGBLOB，并完整读回。");
        System.out.println("   （可用 sql.bat 查询验证: sql.bat \"SELECT id, name, LENGTH(data) FROM image_demo.images\"）");
    }
}
