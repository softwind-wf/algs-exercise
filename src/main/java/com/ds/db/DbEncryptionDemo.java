package com.ds.db;

import com.ds.crypto.Hex;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 应用层字段加密演示：AES-256-GCM + 信封加密（KEK 包 DEK）+ 盲索引（HMAC-SHA256）。
 *
 * <p>演示把"加密"放在应用层，数据库里只存密文、随机数（nonce）、认证标签（tag）、
 * 被 KEK 包起来的 DEK，以及盲索引。对比 {@code db_encryption_mysql.sql} 里
 * 数据库内置 AES 函数那条路，这里补上了内置函数给不了的三件事：</p>
 *
 * <ol>
 *   <li><b>认证加密（AEAD）</b>：GCM 带 128 位 tag，密文被改一个字节解密就失败；</li>
 *   <li><b>信封加密</b>：每行一个随机 DEK，KEK 只用来包/解 DEK——轮换 KEK 时
 *       只重写 wrapped_dek，不用重新加密全表数据；</li>
 *   <li><b>盲索引</b>：HMAC-SHA256(手机号)，等值查询走普通索引，不必全表解密。</li>
 * </ol>
 *
 * <p>演示步骤：建表 → 逐行加密入库 → 以 DBA 视角看库里长什么样 → 应用解密
 * → 盲索引等值查询（EXPLAIN）→ 篡改密文触发 AEAD 失败 → KEK 轮换。</p>
 *
 * <p>运行：{@code scripts\run-db-encryption-demo.ps1}（会先跑 MySQL/SQL 演示，
 * 再编译运行本类）。依赖 {@code target\lib\mysql-connector-j-8.0.33.jar}。</p>
 *
 * <p>注意：演示为了自包含，KEK 和盲索引密钥每次运行随机生成、不落盘；
 * 真实系统应放在 KMS / HSM / Java KeyStore 里，并做访问审计。</p>
 */
public final class DbEncryptionDemo {

    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_TAG_BYTES = GCM_TAG_BITS / 8;
    private static final int NONCE_BYTES = 12;
    private static final int KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private DbEncryptionDemo() {
    }

    public static void main(String[] args) throws Exception {
        Properties config = loadDbConfig();
        String url = "jdbc:mysql://127.0.0.1:" + config.getProperty("port", "3306")
                + "/?useSSL=true&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
        try (Connection conn = DriverManager.getConnection(
                url, config.getProperty("user", "root"), config.getProperty("password", ""))) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE DATABASE IF NOT EXISTS db_encryption_demo "
                        + "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
                st.execute("USE db_encryption_demo");
            }
            runDemo(conn);
        }
    }

    // ============================================================
    // 演示主体
    // ============================================================

    private static void runDemo(Connection conn) throws Exception {
        section("① 建表：数据库只保存密文/随机数/标签/盲索引/包好的 DEK");
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS student_envelope");
            st.execute("CREATE TABLE student_envelope ("
                    + " sid          VARCHAR(8)  PRIMARY KEY,"
                    + " name_ct      VARBINARY(64) NOT NULL,"
                    + " name_nonce   VARBINARY(12) NOT NULL,"
                    + " name_tag     VARBINARY(16) NOT NULL,"
                    + " phone_ct     VARBINARY(64) NOT NULL,"
                    + " phone_nonce  VARBINARY(12) NOT NULL,"
                    + " phone_tag    VARBINARY(16) NOT NULL,"
                    + " phone_bidx   BINARY(32)    NOT NULL,"
                    + " wrapped_dek  VARBINARY(64) NOT NULL,"
                    + " KEY idx_phone_bidx (phone_bidx)"
                    + ") ENGINE=InnoDB");
        }
        System.out.println("  表结构: sid | name_ct name_nonce name_tag | phone_ct phone_nonce phone_tag"
                + " | phone_bidx | wrapped_dek");

        // ---- 密钥：KEK 与盲索引密钥（演示用，随机生成，不落盘） ----
        byte[] kek = randomBytes(KEY_BYTES);
        byte[] blindIndexKey = randomBytes(KEY_BYTES);
        section("② 应用侧密钥（演示：每次运行随机生成，真实系统放 KMS/KeyStore）");
        System.out.println("  KEK            SHA-256 指纹: " + fingerprint(kek));
        System.out.println("  blindIndexKey  SHA-256 指纹: " + fingerprint(blindIndexKey));
        System.out.println("  每条记录另有独立 DEK（随机 256 位），数据库只存 AESWrap(KEK, DEK)");

        // ---- 逐行加密入库 ----
        List<Student> students = Arrays.asList(
                new Student("S001", "张伟", "13900001111"),
                new Student("S002", "李娜", "13800138000"),
                new Student("S003", "王强", "13900001111"),   // 与 S001 同号，用于看盲索引泄漏面
                new Student("S004", "刘洋", "13700137000"),
                new Student("S005", "陈静", "13600136000"));

        section("③ 应用加密后入库：随机 DEK + 随机 nonce + GCM tag + 盲索引");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO student_envelope"
                        + " (sid, name_ct, name_nonce, name_tag,"
                        + "  phone_ct, phone_nonce, phone_tag, phone_bidx, wrapped_dek)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Student s : students) {
                byte[] dek = randomBytes(KEY_BYTES);
                byte[] wrappedDek = aesWrap(kek, dek);

                Sealed name = seal(dek, s.name.getBytes(StandardCharsets.UTF_8),
                        aad(s.sid, "name"));
                Sealed phone = seal(dek, s.phone.getBytes(StandardCharsets.UTF_8),
                        aad(s.sid, "phone"));
                byte[] bidx = hmacSha256(blindIndexKey, s.phone);

                ps.setString(1, s.sid);
                ps.setBytes(2, name.ciphertext);
                ps.setBytes(3, name.nonce);
                ps.setBytes(4, name.tag);
                ps.setBytes(5, phone.ciphertext);
                ps.setBytes(6, phone.nonce);
                ps.setBytes(7, phone.tag);
                ps.setBytes(8, bidx);
                ps.setBytes(9, wrappedDek);
                ps.executeUpdate();
            }
        }

        // ---- DBA 视角 ----
        section("④ DBA 视角：SELECT 出来全是字节；没有 KEK 拼不回明文");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT sid, HEX(name_ct) name_ct, HEX(phone_ct) phone_ct,"
                             + " HEX(phone_nonce) phone_nonce, HEX(phone_tag) phone_tag,"
                             + " HEX(phone_bidx) phone_bidx, HEX(wrapped_dek) wrapped_dek"
                             + " FROM student_envelope ORDER BY sid")) {
            while (rs.next()) {
                System.out.println("  " + rs.getString("sid"));
                System.out.println("    name_ct   = " + rs.getString("name_ct"));
                System.out.println("    phone_ct  = " + rs.getString("phone_ct")
                        + "   (nonce=" + rs.getString("phone_nonce")
                        + ", tag=" + rs.getString("phone_tag") + ")");
                System.out.println("    phone_bidx= " + rs.getString("phone_bidx"));
                System.out.println("    wrapped_dek=" + rs.getString("wrapped_dek"));
            }
        }
        System.out.println("  —— 库里没有 name/phone 明文列，也没有 DEK 明文。");

        // ---- 应用解密 ----
        section("⑤ 应用视角：用 KEK 解出 DEK，再解字段（GCM 校验 tag）");
        List<Row> rows = readRows(conn);
        for (Row r : rows) {
            byte[] dek = aesUnwrap(kek, r.wrappedDek);
            String name = new String(open(dek, r.name, r.nameNonce, r.nameTag, aad(r.sid, "name")),
                    StandardCharsets.UTF_8);
            String phone = new String(open(dek, r.phone, r.phoneNonce, r.phoneTag, aad(r.sid, "phone")),
                    StandardCharsets.UTF_8);
            System.out.println("  " + r.sid + "  " + name + "  " + phone);
        }
        check(rows.size() == 5, "应读到 5 行");

        // ---- 盲索引等值查询 ----
        section("⑥ 盲索引：不存明文也能等值查询，而且走索引");
        byte[] needle = hmacSha256(blindIndexKey, "13900001111");
        System.out.println("  HMAC(13900001111) = " + Hex.toHex(needle));
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("EXPLAIN SELECT sid FROM student_envelope"
                     + " WHERE phone_bidx = X'" + Hex.toHex(needle) + "'")) {
            if (rs.next()) {
                System.out.printf("  EXPLAIN: type=%s key=%s rows=%s Extra=%s%n",
                        rs.getString("type"), rs.getString("key"),
                        rs.getString("rows"), rs.getString("Extra"));
            }
        }
        List<String> hits = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sid FROM student_envelope WHERE phone_bidx = ? ORDER BY sid")) {
            ps.setBytes(1, needle);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hits.add(rs.getString(1));
                }
            }
        }
        System.out.println("  命中: " + hits + "（S001 与 S003 同号）");
        check(hits.equals(Arrays.asList("S001", "S003")), "盲索引应命中 S001/S003");

        // 对照：没有盲索引时，只能全表解密
        List<String> byDecrypt = new ArrayList<>();
        for (Row r : rows) {
            byte[] dek = aesUnwrap(kek, r.wrappedDek);
            String phone = new String(open(dek, r.phone, r.phoneNonce, r.phoneTag, aad(r.sid, "phone")),
                    StandardCharsets.UTF_8);
            if ("13900001111".equals(phone)) {
                byDecrypt.add(r.sid);
            }
        }
        System.out.println("  对照：逐行解密 " + rows.size() + " 行才能过滤，命中 " + byDecrypt
                + "；数据量越大差距越大。");

        // 密文列上直接搜明文子串
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM student_envelope WHERE phone_ct LIKE '%138%'")) {
            rs.next();
            System.out.println("  WHERE phone_ct LIKE '%138%' → " + rs.getInt(1)
                    + " 行（密文里搜不到明文）。");
        }

        // ---- 篡改检测 ----
        section("⑦ 篡改密文：GCM 认证失败，应用拿不到「垃圾明文」");
        Row tampered = rows.get(1);   // S002
        byte[] badCt = tampered.phone.clone();
        badCt[0] ^= 0x01;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE student_envelope SET phone_ct = ? WHERE sid = ?")) {
            ps.setBytes(1, badCt);
            ps.setString(2, tampered.sid);
            ps.executeUpdate();
        }
        byte[] dek = aesUnwrap(kek, tampered.wrappedDek);
        byte[] bad = new byte[badCt.length + tampered.phoneTag.length];
        System.arraycopy(badCt, 0, bad, 0, badCt.length);
        System.arraycopy(tampered.phoneTag, 0, bad, badCt.length, tampered.phoneTag.length);
        try {
            byte[] plain = decryptGcm(dek, tampered.phoneNonce, bad, aad(tampered.sid, "phone"));
            System.out.println("  [异常] 篡改后居然解出: " + new String(plain, StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            System.out.println("  解密失败: " + e.getClass().getSimpleName()
                    + "（tag 校验不通过，密文被视为损坏）");
        } finally {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE student_envelope SET phone_ct = ? WHERE sid = ?")) {
                ps.setBytes(1, tampered.phone);
                ps.setString(2, tampered.sid);
                ps.executeUpdate();
            }
        }

        // ---- KEK 轮换 ----
        section("⑧ 信封加密的好处：轮换 KEK 只重写 wrapped_dek，不动一行业务密文");
        byte[] newKek = randomBytes(KEY_BYTES);
        Map<String, byte[]> rewrapped = new LinkedHashMap<>();
        for (Row r : rows) {
            byte[] rowDek = aesUnwrap(kek, r.wrappedDek);
            rewrapped.put(r.sid, aesWrap(newKek, rowDek));
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE student_envelope SET wrapped_dek = ? WHERE sid = ?")) {
            for (Map.Entry<String, byte[]> e : rewrapped.entrySet()) {
                ps.setBytes(1, e.getValue());
                ps.setString(2, e.getKey());
                ps.executeUpdate();
            }
        }
        Row after = readOne(conn, "S001");
        System.out.println("  S001 wrapped_dek 前: " + Hex.toHex(rows.get(0).wrappedDek));
        System.out.println("  S001 wrapped_dek 后: " + Hex.toHex(after.wrappedDek));
        System.out.println("  S001 phone_ct 是否变化: "
                + !Arrays.equals(rows.get(0).phone, after.phone) + "（应为 false）");
        byte[] rotatedDek = aesUnwrap(newKek, after.wrappedDek);
        String rotatedPhone = new String(
                open(rotatedDek, after.phone, after.phoneNonce, after.phoneTag, aad("S001", "phone")),
                StandardCharsets.UTF_8);
        System.out.println("  用新 KEK 解 S001: " + rotatedPhone + "（旧密文直接复用）");
        check("13900001111".equals(rotatedPhone), "轮换后仍能解密");

        boolean oldKekRejected;
        try {
            aesUnwrap(kek, after.wrappedDek);
            oldKekRejected = false;
        } catch (GeneralSecurityException e) {
            oldKekRejected = true;
        }
        System.out.println("  旧 KEK 解新 wrapped_dek: "
                + (oldKekRejected ? "失败（AESWrap 完整性校验拒绝）" : "[异常] 竟然成功"));

        section("完成");
        System.out.println("  结论：数据库只拿到密文 + 随机数 + 标签 + 盲索引 + 包好的 DEK；");
        System.out.println("        明文、DEK、KEK 都不在库里。代价是应用要自己做加解密与密钥管理。");
    }

    // ============================================================
    // 加解密原语
    // ============================================================

    /** GCM 加密：返回 ciphertext / nonce / tag 三段，AAD 绑定字段位置。 */
    private static Sealed seal(byte[] key, byte[] plaintext, byte[] aad)
            throws GeneralSecurityException {
        byte[] nonce = randomBytes(NONCE_BYTES);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        byte[] out = cipher.doFinal(plaintext);
        byte[] ct = Arrays.copyOfRange(out, 0, out.length - GCM_TAG_BYTES);
        byte[] tag = Arrays.copyOfRange(out, out.length - GCM_TAG_BYTES, out.length);
        return new Sealed(ct, nonce, tag);
    }

    /** GCM 解密：ct 与 tag 分开存放，这里拼回 GCM 要求的 ciphertext||tag。 */
    private static byte[] open(byte[] dek, byte[] ct, byte[] nonce, byte[] tag, byte[] aad)
            throws GeneralSecurityException {
        byte[] ctAndTag = new byte[ct.length + tag.length];
        System.arraycopy(ct, 0, ctAndTag, 0, ct.length);
        System.arraycopy(tag, 0, ctAndTag, ct.length, tag.length);
        return decryptGcm(dek, nonce, ctAndTag, aad);
    }

    private static byte[] decryptGcm(byte[] dek, byte[] nonce, byte[] ctAndTag, byte[] aad)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dek, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(ctAndTag);
    }

    /** RFC 3394 AES Key Wrap：用 KEK 包住每行的 DEK。 */
    private static byte[] aesWrap(byte[] kek, byte[] dek) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AESWrap");
        cipher.init(Cipher.WRAP_MODE, new SecretKeySpec(kek, "AES"));
        return cipher.wrap(new SecretKeySpec(dek, "AES"));
    }

    private static byte[] aesUnwrap(byte[] kek, byte[] wrapped) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AESWrap");
        cipher.init(Cipher.UNWRAP_MODE, new SecretKeySpec(kek, "AES"));
        return cipher.unwrap(wrapped, "AES", Cipher.SECRET_KEY).getEncoded();
    }

    private static byte[] hmacSha256(byte[] key, String value) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] aad(String sid, String field) {
        return (sid + ":" + field).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    private static String fingerprint(byte[] key) throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(key);
        return Hex.toHex(Arrays.copyOf(digest, 8)).toUpperCase();
    }

    // ============================================================
    // JDBC 读取
    // ============================================================

    private static List<Row> readRows(Connection conn) throws Exception {
        List<Row> out = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT sid, name_ct, name_nonce, name_tag,"
                     + " phone_ct, phone_nonce, phone_tag, phone_bidx, wrapped_dek"
                     + " FROM student_envelope ORDER BY sid")) {
            while (rs.next()) {
                out.add(new Row(rs.getString("sid"),
                        rs.getBytes("name_ct"), rs.getBytes("name_nonce"), rs.getBytes("name_tag"),
                        rs.getBytes("phone_ct"), rs.getBytes("phone_nonce"), rs.getBytes("phone_tag"),
                        rs.getBytes("wrapped_dek")));
            }
        }
        return out;
    }

    private static Row readOne(Connection conn, String sid) throws Exception {
        for (Row r : readRows(conn)) {
            if (r.sid.equals(sid)) {
                return r;
            }
        }
        throw new IllegalStateException("找不到 " + sid);
    }

    private static Properties loadDbConfig() throws Exception {
        Properties p = new Properties();
        Path path = Paths.get("src", "main", "resources", "db.properties");
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
            }
        }
        return p;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("===== " + title + " =====");
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new IllegalStateException("校验失败: " + what);
        }
    }

    // ============================================================
    // 数据结构
    // ============================================================

    private static final class Student {
        final String sid;
        final String name;
        final String phone;

        Student(String sid, String name, String phone) {
            this.sid = sid;
            this.name = name;
            this.phone = phone;
        }
    }

    private static final class Sealed {
        final byte[] ciphertext;
        final byte[] nonce;
        final byte[] tag;

        Sealed(byte[] ciphertext, byte[] nonce, byte[] tag) {
            this.ciphertext = ciphertext;
            this.nonce = nonce;
            this.tag = tag;
        }
    }

    private static final class Row {
        final String sid;
        final byte[] name;
        final byte[] nameNonce;
        final byte[] nameTag;
        final byte[] phone;
        final byte[] phoneNonce;
        final byte[] phoneTag;
        final byte[] wrappedDek;

        Row(String sid, byte[] name, byte[] nameNonce, byte[] nameTag,
            byte[] phone, byte[] phoneNonce, byte[] phoneTag, byte[] wrappedDek) {
            this.sid = sid;
            this.name = name;
            this.nameNonce = nameNonce;
            this.nameTag = nameTag;
            this.phone = phone;
            this.phoneNonce = phoneNonce;
            this.phoneTag = phoneTag;
            this.wrappedDek = wrappedDek;
        }
    }
}
