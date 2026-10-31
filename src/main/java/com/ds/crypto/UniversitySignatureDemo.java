package com.ds.crypto;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 把数字签名落到 university 项目的 PoC：成绩单签名 + 审计日志哈希链。
 *
 * <p>不依赖 Spring、不改动现有工程，只用 JDK 自带 JCA，演示两个最合适的落点：
 * <ol>
 *   <li><b>成绩单/证明文件签名</b>：学校用私钥签一份规范化成绩单，任何人拿公钥都能验，
 *       对应项目里的 {@code TranscriptVO} / {@code TranscriptService}；</li>
 *   <li><b>审计日志防篡改</b>：每条审计记录带 prevHash，链式哈希 + 链尾签名，
 *       对应 {@code AuditService} / {@code AuditLog}。</li>
 * </ol>
 *
 * <p>密钥来自 {@code -Dkeystore=...}（PKCS12），由 scripts/run-university-signature-demo.ps1
 * 用 keytool 生成；另加载 TLS 证书（{@code -Dtlsp12=...}）做"用错公钥"的对照。
 *
 * <p>运行：{@code java -Dkeystore=target/university-signing.p12 -Dstorepass=changeit
 * -Dalias=transcript-signer -Dtlsp12=keystore/university.p12 -cp target/sig-classes
 * com.ds.crypto.UniversitySignatureDemo}
 */
public final class UniversitySignatureDemo {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getEncoder();
    /** PSS 参数；运行在 JDK 8 时会回退到 SHA256withRSA（PKCS#1 v1.5）。 */
    private static final PSSParameterSpec PSS_SHA256 = new PSSParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);

    private static int passed;
    private static int failed;
    private static String signAlgorithm;

    private UniversitySignatureDemo() {
    }

    public static void main(String[] args) throws Exception {
        String keystore = System.getProperty("keystore", "target/university-signing.p12");
        String storepass = System.getProperty("storepass", "changeit");
        String alias = System.getProperty("alias", "transcript-signer");
        String tlsP12 = System.getProperty("tlsp12", "keystore/university.p12");

        System.out.println("=== 数字签名在 university 项目中的 PoC ===");
        System.out.println("说明：不依赖 Spring，只演示「签什么、用什么钥、怎么验、怎么防篡改」。");
        System.out.println();

        KeyStore ks = loadKeystore(keystore, storepass);
        PrivateKey signingKey = (PrivateKey) ks.getKey(alias, storepass.toCharArray());
        X509Certificate signingCert = (X509Certificate) ks.getCertificate(alias);
        PublicKey signingPublicKey = signingCert.getPublicKey();

        section1Key(signingCert);
        String transcriptJson = buildTranscriptJson();
        byte[] payload = transcriptJson.getBytes(StandardCharsets.UTF_8);
        section2Payload(transcriptJson, payload);

        byte[] signature = sign(signingKey, payload);
        section3Sign(signature, payload);
        section4Verify(signingPublicKey, payload, signature);
        section5WrongKey(tlsP12, storepass, payload, signature);
        section6AuditChain(signingKey, signingPublicKey);
        section7WhereToIntegrate();

        System.out.println();
        System.out.println("自检：通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ==================================================================
    // 1. 签名密钥
    // ==================================================================
    private static void section1Key(X509Certificate cert) throws Exception {
        title(1, "签名密钥：专钥专用，不能复用 TLS 证书");
        System.out.println("证书主题: " + cert.getSubjectX500Principal().getName());
        System.out.println("证书有效期: " + cert.getNotBefore() + " ~ " + cert.getNotAfter());
        System.out.println("公钥算法: " + cert.getPublicKey().getAlgorithm() + " "
                + ((RSAPublicKey) cert.getPublicKey()).getModulus().bitLength() + " 位");
        boolean[] keyUsage = cert.getKeyUsage();
        System.out.println("用途扩展 KeyUsage: " + (keyUsage == null ? "未设置" : Arrays.toString(keyUsage))
                + "（第 1 位 digitalSignature 应为 true）");
        System.out.println("指纹(SHA-256): " + fingerprint(cert));
        System.out.println();
        System.out.println("要点：项目里 keystore/university.p12 是 HTTPS 用的（CN=localhost），");
        System.out.println("      签名成绩单必须另发一张「数字签名用途」的证书，不能共用同一把私钥。");
    }

    // ==================================================================
    // 2. 被签的内容
    // ==================================================================
    private static String buildTranscriptJson() {
        // 模拟 TranscriptVO 的字段；固定字段顺序 = 规范化编码（生产建议用 canonical JSON 库）
        return "{"
                + "\"type\":\"TRANSCRIPT\","                     // 域分离：签名用途
                + "\"version\":\"v1\","
                + "\"studentId\":\"2021001\","
                + "\"studentName\":\"张伟\","
                + "\"deptName\":\"计算机科学与技术\","
                + "\"term\":\"2026-FALL\","
                + "\"rows\":["
                + "{\"courseNo\":\"CS301\",\"courseName\":\"数据库系统\",\"credits\":4.0,\"grade\":\"B+\",\"point\":3.3},"
                + "{\"courseNo\":\"CS302\",\"courseName\":\"操作系统\",\"credits\":4.0,\"grade\":\"A-\",\"point\":3.7},"
                + "{\"courseNo\":\"MA201\",\"courseName\":\"线性代数\",\"credits\":3.0,\"grade\":\"B\",\"point\":3.0}"
                + "],"
                + "\"courseCount\":3,"
                + "\"earnedCredits\":11,"
                + "\"gpa\":3.37,"
                + "\"issuedAt\":\"2026-10-31T21:00:00+08:00\","
                + "\"serialNo\":\"TR-20261031-0001\""
                + "}";
    }

    private static void section2Payload(String json, byte[] payload) throws Exception {
        title(2, "被签的内容：成绩单 + 用途 + 流水号");
        System.out.println("长度: " + payload.length + " 字节");
        System.out.println(json);
        System.out.println();
        System.out.println("SHA-256: " + Hex.toHex(MessageDigest.getInstance("SHA-256").digest(payload)));
        System.out.println("被签内容里必须带：type（防跨用途）、studentId/term（防张冠李戴）、");
        System.out.println("serialNo 与 issuedAt（防重放、可追溯）。");
    }

    // ==================================================================
    // 3. 签名
    // ==================================================================
    private static void section3Sign(byte[] signature, byte[] payload) {
        title(3, "学校用私钥签名");
        System.out.println("算法: " + signAlgorithm + "（JDK 17 上优先 RSA-PSS，JDK 8 回退 SHA256withRSA）");
        System.out.println("签名长度: " + signature.length + " 字节");
        System.out.println("签名(Base64): " + B64.encodeToString(signature));
    }

    // ==================================================================
    // 4. 验证
    // ==================================================================
    private static void section4Verify(PublicKey publicKey, byte[] payload, byte[] signature) throws Exception {
        title(4, "谁都能验：文件 + 签名 + 学校公钥");
        check(verify(publicKey, payload, signature), "原始成绩单 + 签名 -> 通过");

        byte[] tamperedScore = new String(payload, StandardCharsets.UTF_8)
                .replace("\"gpa\":3.37", "\"gpa\":4.00").getBytes(StandardCharsets.UTF_8);
        check(!verify(publicKey, tamperedScore, signature), "把 GPA 3.37 改成 4.00 -> 拒绝");

        byte[] tamperedName = new String(payload, StandardCharsets.UTF_8)
                .replace("张伟", "李四").getBytes(StandardCharsets.UTF_8);
        check(!verify(publicKey, tamperedName, signature), "把姓名换成别人 -> 拒绝");

        byte[] badSig = signature.clone();
        badSig[badSig.length / 2] ^= 0x01;
        check(!verify(publicKey, payload, badSig), "改签名中的 1 个 bit -> 拒绝");
    }

    // ==================================================================
    // 5. 用错公钥（TLS 证书）也不能通过
    // ==================================================================
    private static void section5WrongKey(String tlsP12, String storepass, byte[] payload, byte[] signature)
            throws Exception {
        title(5, "换成 HTTPS 证书的公钥：验不过");
        try {
            KeyStore tlsStore = loadKeystore(tlsP12, storepass);
            Certificate tlsCert = tlsStore.getCertificate(tlsStore.aliases().nextElement());
            System.out.println("拿到的 TLS 证书主题: " + ((X509Certificate) tlsCert).getSubjectX500Principal().getName());
            check(!verify(tlsCert.getPublicKey(), payload, signature),
                    "用 TLS 证书的公钥验证成绩单签名 -> 拒绝（不是同一把密钥；生产还应校验 KeyUsage 与证书链）");
        } catch (Exception e) {
            System.out.println("（未找到 TLS keystore，跳过本条：" + e.getMessage() + "）");
        }
    }

    // ==================================================================
    // 6. 审计日志哈希链
    // ==================================================================
    private static void section6AuditChain(PrivateKey signingKey, PublicKey publicKey) throws Exception {
        title(6, "审计日志防篡改：链式哈希 + 链尾签名");
        String[] actions = {"GRADE_UPDATE: 2021001 CS301 B+ -> A-", "ACCOUNT_CREATE: teacher_li",
                "DELETE: section 2026-FALL-CS301-01"};
        List<String> entryHashes = new ArrayList<String>();
        String prevHash = "GENESIS";
        System.out.println("每条记录: hash = SHA256(prevHash || 记录内容)");
        for (int i = 0; i < actions.length; i++) {
            String entryHash = sha256Hex(prevHash + "|" + actions[i]);
            entryHashes.add(entryHash);
            System.out.println("  #" + (i + 1) + " " + actions[i]);
            System.out.println("      prev=" + shortHash(prevHash) + "  hash=" + shortHash(entryHash));
            prevHash = entryHash;
        }
        byte[] anchor = sign(signingKey, prevHash.getBytes(StandardCharsets.UTF_8));
        System.out.println("  链尾签名（每天/每次批量后签一次，作为「锚点」）: "
                + shortHash(B64.encodeToString(anchor)) + "...");

        check(verify(publicKey, prevHash.getBytes(StandardCharsets.UTF_8), anchor), "原链的锚点验签通过");
        String recomputed = recomputeChain(actions);
        check(recomputed.equals(prevHash), "从头重算哈希链，与锚点一致");

        String[] tampered = actions.clone();
        tampered[1] = "ACCOUNT_CREATE: teacher_li  <- 偷偷改成 admin_li";
        String tamperedHash = recomputeChain(tampered);
        System.out.println("  篡改第 2 条后重算: " + shortHash(tamperedHash) + "（与原锚点不一致）");
        check(!tamperedHash.equals(prevHash), "改任意一条历史记录 -> 重算哈希链对不上锚点");
        check(!verify(publicKey, tamperedHash.getBytes(StandardCharsets.UTF_8), anchor), "篡改后的链过不了锚点验签");
    }

    private static String recomputeChain(String[] actions) throws Exception {
        String prevHash = "GENESIS";
        for (String action : actions) {
            prevHash = sha256Hex(prevHash + "|" + action);
        }
        return prevHash;
    }

    // ==================================================================
    // 7. 落点
    // ==================================================================
    private static void section7WhereToIntegrate() {
        title(7, "对应到 university 项目的落点");
        System.out.println("1) 成绩单签名：TranscriptVO / StudentService / 前端成绩单页");
        System.out.println("   新增表 transcript_signature(student_id, term, serial_no, payload_hash,");
        System.out.println("                        signature, cert_serial, issued_at)");
        System.out.println("   新增接口 GET /student/transcript/signed（返回 JSON + 签名 + 证书链）");
        System.out.println("   新增公开验证页 /verify（无需登录，上传文件或粘贴 JSON，只返回 通过/不通过）");
        System.out.println("2) 审计链：AuditLog 增 prev_hash / entry_hash 两列，AuditService 写入时计算；");
        System.out.println("   每日定时把链尾签名存到单独的 audit_anchor 表。");
        System.out.println("3) 不该用签名的地方：登录会话（用 session/JWT）、服务间调用（HMAC 更合适）、");
        System.out.println("   学生提交的作业（学生没有可信密钥，用「上传 + 服务端归档校验」）。");
    }

    // ==================================================================
    // 签名/验证/载入
    // ==================================================================
    private static byte[] sign(PrivateKey key, byte[] data) throws Exception {
        try {
            Signature signature = Signature.getInstance("RSASSA-PSS");
            signature.setParameter(PSS_SHA256);
            signature.initSign(key, RANDOM);
            signature.update(data);
            if (signAlgorithm == null) {
                signAlgorithm = "RSASSA-PSS + SHA-256";
            }
            return signature.sign();
        } catch (java.security.NoSuchAlgorithmException e) {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key, RANDOM);
            signature.update(data);
            if (signAlgorithm == null) {
                signAlgorithm = "SHA256withRSA（PKCS#1 v1.5，JDK 8 回退）";
            }
            return signature.sign();
        }
    }

    private static boolean verify(PublicKey key, byte[] data, byte[] signature) {
        try {
            Signature verifier = Signature.getInstance("RSASSA-PSS");
            verifier.setParameter(PSS_SHA256);
            verifier.initVerify(key);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (Exception pssFailed) {
            try {
                Signature verifier = Signature.getInstance("SHA256withRSA");
                verifier.initVerify(key);
                verifier.update(data);
                return verifier.verify(signature);
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static KeyStore loadKeystore(String path, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(path)) {
            ks.load(in, password.toCharArray());
        }
        return ks;
    }

    // ==================================================================
    // 工具
    // ==================================================================
    private static String sha256Hex(String text) throws Exception {
        return Hex.toHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String fingerprint(X509Certificate cert) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
        return Hex.toHex(Arrays.copyOf(digest, 16)) + "...";
    }

    private static String shortHash(String hex) {
        return hex.length() > 16 ? hex.substring(0, 16) + "..." : hex;
    }

    private static void title(int step, String text) {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println("【" + step + "】" + text);
        System.out.println("==================================================================");
    }

    private static void check(boolean condition, String what) {
        if (condition) {
            passed++;
            System.out.println("  [通过] " + what);
        } else {
            failed++;
            System.out.println("  [失败] " + what);
        }
    }
}
