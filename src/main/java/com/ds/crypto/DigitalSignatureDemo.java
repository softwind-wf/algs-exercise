package com.ds.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 数字签名：从一条转账指令出发，把"签名是什么、为什么这样设计、哪里会翻车"全部跑一遍。
 *
 * <p>共 7 段，每段都以自检断言收尾（结尾会打印"自检通过 N / 失败 M"，失败时进程退出码为 1）：
 * <ol>
 *   <li>最小闭环：一条消息 -> 签名 -> 验签；改消息 / 改签名 / 换公钥，三种篡改都被拒绝；</li>
 *   <li>数学原形：用小素数 RSA 手算 σ = m^d mod n，并现场演示教科书签名的两种伪造
 *       （可乘性伪造、先选签名再反推消息），说明"为什么必须先 hash 再签"；</li>
 *   <li>四种算法实测：RSA PKCS#1 v1.5、RSA-PSS、ECDSA P-256、Ed25519 的签名长度、
 *       确定性（同一消息签两次是否相同）与耗时；</li>
 *   <li>"用私钥加密"到底算不算签名：JCE 实测 + 为什么不提供机密性；</li>
 *   <li>签名 vs MAC（HMAC）：对称密钥能证明"有密钥"，不能证明"是谁"；</li>
 *   <li>攻击面：ECDSA/DSA 随机数复用 -> 私钥被算出来（用玩具数字完整推导）；
 *       公钥被替换 -> 攻击者用自己的私钥签名照样"验签通过"；重放 -> 同一签名提交两次都通过；</li>
 *   <li>小结。</li>
 * </ol>
 *
 * <p>本类不实现任何密码算法，全部调用 JDK 自带 JCA/JCE（{@link Signature}、{@link MessageDigest}、
 * {@link Mac}、{@link Cipher}、{@link KeyPairGenerator}）。第 2、6 段里的"手算"是为了教学，
 * 用 {@link BigInteger} 复现教科书公式，不要用于任何真实场景。
 *
 * <p>运行：{@code java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp target\sig-classes
 * com.ds.crypto.DigitalSignatureDemo}
 */
public final class DigitalSignatureDemo {

    /** 演示用消息：一条带金额的交易指令，改一个数字就应该验签失败。 */
    private static final String MESSAGE =
            "转账指令：2026-10-28 从 A 账户转 100.00 元 给 B 账户（流水号 T-0001）";

    /** PSS 参数：SHA-256 + MGF1-SHA256 + 32 字节盐 + trailer=1（对应 RFC 8017 的 EMSA-PSS）。 */
    private static final PSSParameterSpec PSS_SHA256 = new PSSParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getEncoder();

    private static int passed;
    private static int failed;

    private DigitalSignatureDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("JDK 版本: " + System.getProperty("java.version")
                + "，提供者: " + java.security.Security.getProvider("SunRsaSign").getName());
        section1MinimalLoop();
        section2TextbookRsa();
        section3AlgorithmComparison();
        section4IsThisEncryption();
        section5SignatureVsMac();
        section6Attacks();
        section7Summary();
        System.out.println();
        System.out.println("自检：通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed > 0) {
            System.out.println("有自检失败，说明演示代码本身或环境不符合预期，退出码 1。");
            System.exit(1);
        }
    }

    // ==================================================================
    // 1. 最小闭环
    // ==================================================================
    private static void section1MinimalLoop() throws Exception {
        title(1, "最小闭环：签名、验签、三种篡改");
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);
        KeyPair keyPair = rsaKeyPair(2048);

        System.out.println("消息 M（" + message.length + " 字节）: " + MESSAGE);
        System.out.println("SHA-256(M)      : " + sha256Hex(message));
        System.out.println("签名算法        : RSASSA-PSS（SHA-256 + MGF1-SHA256，盐 32 字节）");
        System.out.println("公钥            : RSA 2048 位，指纹 " + fingerprint(keyPair.getPublic()));

        byte[] signature = signPss(keyPair.getPrivate(), message);
        System.out.println("签名长度        : " + signature.length + " 字节（等于 RSA 模数长度，与消息长度无关）");
        System.out.println("签名（Base64）  :");
        for (String line : wrap(B64.encodeToString(signature), 76)) {
            System.out.println("  " + line);
        }

        System.out.println();
        System.out.println("检查：");
        check(verifyPss(keyPair.getPublic(), message, signature),
                "原始消息 + 原始签名 -> 验签通过");
        check(!verifyPss(keyPair.getPublic(),
                        MESSAGE.replace("100.00", "900.00").getBytes(StandardCharsets.UTF_8), signature),
                "只把金额 100.00 改成 900.00 -> 验签拒绝");
        check(!verifyPss(keyPair.getPublic(), message, tamperLastByte(signature)),
                "只改签名的最后一个 bit -> 验签拒绝");
        check(!verifyPss(rsaKeyPair(2048).getPublic(), message, signature),
                "换成另一对密钥的公钥 -> 验签拒绝");

        System.out.println();
        System.out.println("验证者需要什么：消息 + 签名 + 签名者的公钥。三者缺一不可；");
        System.out.println("消息本身是明文传输的，签名只证明\"谁发的、有没有被改\"，不提供机密性。");
    }

    // ==================================================================
    // 2. 数学原形与教科书伪造
    // ==================================================================
    private static void section2TextbookRsa() {
        title(2, "数学原形：教科书 RSA 的签名就是一次指数运算");
        BigInteger p = BigInteger.valueOf(61);
        BigInteger q = BigInteger.valueOf(53);
        BigInteger n = p.multiply(q);                        // 3233
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)); // 3120
        BigInteger e = BigInteger.valueOf(17);
        BigInteger d = e.modInverse(phi);                    // 2753

        System.out.println("p = 61, q = 53");
        System.out.println("n = p*q = " + n);
        System.out.println("φ(n) = (p-1)(q-1) = " + phi);
        System.out.println("e = 17（公钥指数）");
        System.out.println("d = e^-1 mod φ(n) = " + d + "（私钥指数）");
        System.out.println("e*d mod φ(n) = " + e.multiply(d).mod(phi) + "（验证：确实等于 1）");
        System.out.println("公钥 (n, e) = (" + n + ", " + e + ")；私钥 (n, d) = (" + n + ", " + d + ")");

        BigInteger m = BigInteger.valueOf(65);               // 'A'
        BigInteger sigma = m.modPow(d, n);
        System.out.println();
        System.out.println("消息整数 m = 65（'A'，必须小于 n=" + n + "，所以 RSA 一次只能处理很短的数据）");
        System.out.println("签名 σ = m^d mod n = " + sigma);
        System.out.println("验签（任何人都能做）：σ^e mod n = " + sigma.modPow(e, n));
        check(sigma.modPow(e, n).equals(m), "教科书验证公式 σ^e mod n == m 成立");

        System.out.println();
        System.out.println("—— 漏洞 1：可乘性伪造（教科书 RSA / 无填充签名的经典攻击）——");
        BigInteger m1 = BigInteger.valueOf(65);
        BigInteger m2 = BigInteger.valueOf(66);
        BigInteger s1 = m1.modPow(d, n);
        BigInteger s2 = m2.modPow(d, n);
        BigInteger forged = s1.multiply(s2).mod(n);
        BigInteger m3 = m1.multiply(m2).mod(n);
        System.out.println("截获两条合法签名：σ(65) = " + s1 + "，σ(66) = " + s2);
        System.out.println("攻击者把两个签名相乘：σ(65)*σ(66) mod n = " + forged);
        System.out.println("它对应的消息：m3 = 65*66 mod n = " + m3);
        System.out.println("用公钥验伪造签名：σ^e mod n = " + forged.modPow(e, n));
        check(forged.modPow(e, n).equals(m3),
                "不需要私钥，伪造的签名被验证器接受（这就是漏洞）");

        System.out.println();
        System.out.println("—— 漏洞 2：存在性伪造（先选签名，再反推消息）——");
        BigInteger chosen = BigInteger.valueOf(7);
        BigInteger derivedMessage = chosen.modPow(e, n);
        System.out.println("攻击者任意挑一个数当签名：s = 7");
        System.out.println("反推消息：m = s^e mod n = " + derivedMessage);
        System.out.println("于是 (m=" + derivedMessage + ", 签名 s=7) 天然通过验证");
        check(chosen.modPow(e, n).equals(derivedMessage),
                "(m, s=7) 通过验证，但这条 m 是攻击者算出来的乱码，冒充不了真实内容");

        System.out.println();
        System.out.println("—— 修正：签名对象是 hash，并且要先过带结构的填充 ——");
        System.out.println("1) 消息先 hash 成固定长度（SHA-256 -> 32 字节）再当成整数，");
        System.out.println("   攻击者无法控制 hash 的取值，可乘性伪造和目标 hash 对不上；");
        System.out.println("2) PKCS#1 v1.5 把 hash 装进 00 01 FF...FF 00 || DigestInfo 的固定结构，");
        System.out.println("   多乘一个 σ 就会把填充结构弄乱，验签端直接拒绝；DigestInfo 还防跨算法替换；");
        System.out.println("3) RSA-PSS 更进一步：随机盐 + MGF1 掩码，同一消息每次签名都不同；");
        System.out.println("4) hash 必须抗碰撞、抗第二原像 —— SHA-1 已被实际碰撞（2017 SHAttered），");
        System.out.println("   今天签名一律 SHA-256 起步。");
    }

    // ==================================================================
    // 3. 四种算法实测
    // ==================================================================
    private static void section3AlgorithmComparison() throws Exception {
        title(3, "四种签名算法实测：长度、确定性、耗时");
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);

        KeyPair rsa = rsaKeyPair(2048);
        KeyPair ec = ecKeyPair();
        KeyPair ed25519 = ed25519KeyPairOrNull();

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(new String[]{"算法", "密钥长度", "签名长度", "同一消息签两次", "签名 ms", "验签 ms"});

        final byte[] msg = message;
        byte[] pkcs1a = sign("SHA256withRSA", rsa.getPrivate(), msg);
        byte[] pkcs1b = sign("SHA256withRSA", rsa.getPrivate(), msg);
        rows.add(row("RSA-2048 / PKCS#1 v1.5", "2048 位模数", pkcs1a.length,
                sameBytes(pkcs1a, pkcs1b) ? "完全相同" : "不同",
                m -> sign("SHA256withRSA", rsa.getPrivate(), m),
                (m, sg) -> verify("SHA256withRSA", rsa.getPublic(), m, sg)));

        byte[] pssa = signPss(rsa.getPrivate(), msg);
        byte[] pssb = signPss(rsa.getPrivate(), msg);
        rows.add(row("RSA-2048 / RSASSA-PSS", "2048 位模数", pssa.length,
                sameBytes(pssa, pssb) ? "完全相同" : "不同（随机盐）",
                m -> signPss(rsa.getPrivate(), m),
                (m, sg) -> verifyPss(rsa.getPublic(), m, sg)));

        byte[] eca = sign("SHA256withECDSA", ec.getPrivate(), msg);
        byte[] ecb = sign("SHA256withECDSA", ec.getPrivate(), msg);
        rows.add(row("EC P-256 / SHA256withECDSA", "256 位曲线", eca.length,
                sameBytes(eca, ecb) ? "完全相同" : "不同（随机 k）",
                m -> sign("SHA256withECDSA", ec.getPrivate(), m),
                (m, sg) -> verify("SHA256withECDSA", ec.getPublic(), m, sg)));

        if (ed25519 == null) {
            rows.add(new String[]{"Ed25519", "本机 JDK 不支持（需要 15+）", "-", "-", "-", "-"});
        } else {
            final KeyPair ed = ed25519;
            byte[] eda = sign("Ed25519", ed.getPrivate(), msg);
            byte[] edb = sign("Ed25519", ed.getPrivate(), msg);
            rows.add(row("Ed25519", "256 位", eda.length,
                    sameBytes(eda, edb) ? "完全相同" : "不同",
                    m -> sign("Ed25519", ed.getPrivate(), m),
                    (m, sg) -> verify("Ed25519", ed.getPublic(), m, sg)));
        }
        printTable(rows);
        System.out.println("（毫秒数是每次新建 Signature 对象后测的 20 次均值，只看量级，不要当基准测试。）");

        System.out.println();
        System.out.println("表中三个要点：");
        System.out.println("· RSA 签名长度 = 模数长度（2048 位 -> 256 字节），ECDSA/Ed25519 短得多；");
        System.out.println("  ECDSA 的 DER 编码长度在 70~72 字节间浮动，验签端不能假定固定长度。");
        System.out.println("· \"同一消息签两次是否相同\"不能当安全性判据：");
        System.out.println("  PKCS#1 v1.5 和 Ed25519 都是确定性的（Ed25519 的随机量由私钥+消息派生，");
        System.out.println("  所以永不复用、也不会撞车），ECDSA/PSS 才需要随机数。");
        System.out.println("  真正危险的是\"随机数被复用\"，不是\"签名确定性\"（见第 6 段）。");
        System.out.println("· 速度方向不一样：RSA 验签极快（验签只做一次 e=65537 的幂运算，签名要算私钥指数），");
        System.out.println("  ECDSA 验签要做两次标量乘、比签名贵；Ed25519 两者接近。");
        System.out.println("  这里的绝对值主要反映 JCA 每次新建 Signature 的开销，判断趋势就好。");

        System.out.println();
        System.out.println("—— 签名长度与消息长度无关 ——");
        byte[] tiny = "1".getBytes(StandardCharsets.UTF_8);
        byte[] big = new byte[1_000_000];
        RANDOM.nextBytes(big);
        long t0 = System.nanoTime();
        byte[] tinySig = signPss(rsa.getPrivate(), tiny);
        long tinyMs = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        byte[] bigSig = signPss(rsa.getPrivate(), big);
        long bigMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("签 1 字节的消息：签名 " + tinySig.length + " 字节，耗时 " + tinyMs + " ms");
        System.out.println("签 1 MB 的消息：签名 " + bigSig.length + " 字节，耗时 " + bigMs + " ms");
        check(tinySig.length == bigSig.length && verifyPss(rsa.getPublic(), big, bigSig),
                "1 字节与 1 MB 的签名长度相同，且 1 MB 的签名仍可验签");
    }

    // ==================================================================
    // 4. "用私钥加密" 算不算签名
    // ==================================================================
    private static void section4IsThisEncryption() throws Exception {
        title(4, "\"用私钥加密\"到底算不算签名？");
        KeyPair keyPair = rsaKeyPair(2048);
        String text = "这条消息用私钥\"加密\"，任何人都能用公钥解开。";
        byte[] plain = text.getBytes(StandardCharsets.UTF_8);

        System.out.println("尝试：Cipher.getInstance(\"RSA/ECB/PKCS1Padding\").init(ENCRYPT_MODE, 私钥)");
        try {
            Cipher encrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encrypt.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
            byte[] cipherText = encrypt.doFinal(plain);
            Cipher decrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decrypt.init(Cipher.DECRYPT_MODE, keyPair.getPublic());
            byte[] recovered = decrypt.doFinal(cipherText);
            System.out.println("结果：JCE 允许，密文 " + cipherText.length + " 字节");
            System.out.println("公钥还原出的明文: " + new String(recovered, StandardCharsets.UTF_8));
            check(new String(recovered, StandardCharsets.UTF_8).equals(text),
                    "用私钥做的这步\"加密\"，公钥可以完整还原（所以它不提供机密性）");
            System.out.println("→ 结论：方向虽然反过来，但这只是\"私钥做幂运算\"，不是签名：");
        } catch (GeneralSecurityException e) {
            System.out.println("结果：JCE 拒绝——" + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("→ 现代 JCE 不允许把私钥当加密密钥，这本身就说明\"签名 = 私钥加密\"只是历史说法。");
        }

        System.out.println();
        System.out.println("签名和\"私钥加密\"差在四处，缺一不可：");
        System.out.println("1) 签名对象是 hash：σ 作用在 H(M) 上，不是作用在 M 上；");
        System.out.println("2) 结构化填充：PKCS#1 v1.5 的类型 1 填充 / PSS 的随机盐，让\"乘法\"彻底失效；");
        System.out.println("3) 验证是布尔判定：验签要么 true 要么抛异常，不存在\"解出乱码\"的中间态；");
        System.out.println("4) 幂运算本身在数学上双向可逆，安全性全靠\"构造不出合法填充\"这一点，");
        System.out.println("   所以绝不能自己拼填充，必须用标准库/标准算法名。");
    }

    // ==================================================================
    // 5. 签名 vs MAC
    // ==================================================================
    private static void section5SignatureVsMac() throws Exception {
        title(5, "签名 vs MAC：能证明\"有密钥\"，还是能证明\"是谁\"");
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = new byte[16];
        RANDOM.nextBytes(keyBytes);

        byte[] mac1 = hmac(keyBytes, message);
        byte[] mac2 = hmac(keyBytes, message);   // 模拟\"持有同一把密钥的第三方\"自己算一遍

        System.out.println("HMAC-SHA256(K, M) = " + hex(mac1));
        System.out.println("检查：");
        check(MessageDigest.isEqual(mac1, mac2), "持有 K 的人算出来的 MAC 完全相同");
        check(mac1.length == 32 && mac2.length == 32, "MAC 长度固定 32 字节，与消息长度无关");
        System.out.println();
        System.out.println("→ 同一把对称密钥，签名者和\"任何一个拿到 K 的人\"（服务器、运维、攻击者）");
        System.out.println("  产生的结果一模一样，事后无法向法官证明\"这条是 A 发的\"。");
        System.out.println("→ 数字签名用私钥：只有私钥持有者能生成，公钥只能验证，");
        System.out.println("  所以才有不可否认性（Non-repudiation）。");
        System.out.println("→ 现实里两者都在用：TLS 1.2 记录用 HMAC、TLS 1.3 用 HMAC 构造的 HKDF 派生密钥；");
        System.out.println("  JAR 签名、代码签名、电子合同用公私钥签名（需要第三方可验证）。");
    }

    // ==================================================================
    // 6. 攻击面
    // ==================================================================
    private static void section6Attacks() throws Exception {
        title(6, "攻击面：随机数复用、公钥替换、重放");

        // ---------- 6.1 ----------
        System.out.println("—— 6.1 DSA/ECDSA 随机数复用 -> 私钥被完整算出来 ——");
        BigInteger p = BigInteger.valueOf(23);
        BigInteger q = BigInteger.valueOf(11);
        BigInteger g = BigInteger.valueOf(2);
        BigInteger d = BigInteger.valueOf(7);                       // 私钥
        BigInteger y = g.modPow(d, p);                              // 公钥部分
        BigInteger k = BigInteger.valueOf(6);                       // 两次签名复用了同一个 k
        BigInteger z1 = BigInteger.valueOf(3);                      // 第一条消息的摘要 mod q
        BigInteger z2 = BigInteger.valueOf(5);                      // 第二条消息的摘要 mod q
        BigInteger r = g.modPow(k, p).mod(q);
        BigInteger kInv = k.modInverse(q);
        BigInteger s1 = kInv.multiply(z1.add(r.multiply(d))).mod(q);
        BigInteger s2 = kInv.multiply(z2.add(r.multiply(d))).mod(q);

        System.out.println("玩具参数：p=23, q=11, g=2（真实 ECDSA 用 256 位曲线）");
        System.out.println("签名者私钥 d = " + d + "，公钥 y = g^d mod p = " + y);
        System.out.println("两次签名用了同一个随机数 k = " + k + "（复用！）");
        System.out.println("签名 1：(r, s1) = (" + r + ", " + s1 + ")，消息摘要 z1 = " + z1);
        System.out.println("签名 2：(r, s2) = (" + r + ", " + s2 + ")，消息摘要 z2 = " + z2);
        check(verifyToyDsa(p, q, g, y, z1, r, s1), "签名 1 本身是合法的（不是伪造，因此泄漏是真的）");

        BigInteger kRecovered = z1.subtract(z2)
                .multiply(s1.subtract(s2).modInverse(q)).mod(q);
        BigInteger dRecovered = s1.multiply(kRecovered).subtract(z1)
                .multiply(r.modInverse(q)).mod(q);

        System.out.println("攻击者只看到 (z1, r, s1) 和 (z2, r, s2)，因为 r 相同，直接解方程：");
        System.out.println("  k = (z1 - z2) / (s1 - s2) mod q = " + kRecovered);
        System.out.println("  d = (s1*k - z1) / r mod q = " + dRecovered);
        System.out.println("检查：");
        check(kRecovered.equals(k), "复用掉的随机数 k 被还原");
        check(dRecovered.equals(d), "私钥 d 被完整还原 -> 从此可以伪造任何签名");
        System.out.println("→ 现实案例：索尼 PS3（2010）因 ECDSA 的 k 恒定被破解；Android 比特币钱包（2013）");
        System.out.println("  因 SecureRandom 缺陷导致 k 重复，私钥批量泄漏。");
        System.out.println("→ 修复：每个签名都用新的随机 k（RFC 6979 用确定性方式从 私钥+消息 派生 k，");
        System.out.println("  既不复用也不依赖随机数质量）。Ed25519 天生这样做。");

        // ---------- 6.2 ----------
        System.out.println();
        System.out.println("—— 6.2 公钥替换：签名对不对，取决于你手上是谁的公钥 ——");
        KeyPair real = rsaKeyPair(2048);
        KeyPair attacker = rsaKeyPair(2048);
        byte[] forgedMessage = "转账指令：2026-10-28 从 A 账户转 900.00 元 给 C 账户（流水号 T-6666）"
                .getBytes(StandardCharsets.UTF_8);
        byte[] forgedSignature = signPss(attacker.getPrivate(), forgedMessage);

        System.out.println("真实公钥指纹: " + fingerprint(real.getPublic()));
        System.out.println("攻击者公钥指纹: " + fingerprint(attacker.getPublic()));
        System.out.println("攻击者用自己的私钥签了一条\"转 900 元给 C\"的指令。");
        check(verifyPss(attacker.getPublic(), forgedMessage, forgedSignature),
                "用攻击者的公钥验：通过（这条签名确实是攻击者的）");
        check(!verifyPss(real.getPublic(), forgedMessage, forgedSignature),
                "用真实公钥验：拒绝");
        System.out.println("→ 签名本身没被攻破，被替换的是\"公钥\"。所以公钥来源必须可信：");
        System.out.println("  证书链（CA）、预置指纹、公钥固定（pinning）、密钥透明日志（CT）。");

        // ---------- 6.3 ----------
        System.out.println();
        System.out.println("—— 6.3 重放：签名只证明\"这条消息没被改\"，不证明\"只发了一次\" ——");
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);
        byte[] signature = signPss(real.getPrivate(), message);
        check(verifyPss(real.getPublic(), message, signature)
                        && verifyPss(real.getPublic(), message, signature),
                "同一条 (消息, 签名) 提交两次，两次都验签通过");
        System.out.println("→ 转账、下单这类动作必须在协议层加一次性 nonce / 时间戳 / 流水号去重；");
        System.out.println("  例如消息里带 \"流水号 T-0001\"，服务端记录已处理的流水号。");
    }

    // ==================================================================
    // 7. 小结
    // ==================================================================
    private static void section7Summary() {
        title(7, "小结");
        System.out.println("· 数字签名 = 用私钥对\"消息摘要 + 结构化填充\"做运算，公钥验算，结果只有 是/否。");
        System.out.println("· 它同时给出三件事：完整性（改一个 bit 就验不过）、身份认证（私钥持有者）、");
        System.out.println("  不可否认性（只有私钥能生成）——但不提供机密性，也不防重放。");
        System.out.println("· 必须先 hash 再签：不然可乘性伪造、存在性伪造都能绕开私钥。");
        System.out.println("· 算法选择：Ed25519 / ECDSA P-256 / RSA-PSS 优先，RSA 至少 2048 位；");
        System.out.println("  PKCS#1 v1.5 只用于兼容旧系统；SHA-1 已死。");
        System.out.println("· 实现上只用标准库的标准算法名；随机数复用会直接泄漏私钥。");
        System.out.println("· 工程上还要解决：公钥来源（证书）、消息覆盖范围（所有可变量）、");
        System.out.println("  重放（nonce/时间戳）、密钥轮换与吊销。");
    }

    // ==================================================================
    // 签名/验签封装
    // ==================================================================

    private static byte[] signPss(PrivateKey key, byte[] message) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("RSASSA-PSS");
        signature.setParameter(PSS_SHA256);
        signature.initSign(key, RANDOM);
        signature.update(message);
        return signature.sign();
    }

    private static boolean verifyPss(PublicKey key, byte[] message, byte[] signature) {
        try {
            Signature verifier = Signature.getInstance("RSASSA-PSS");
            verifier.setParameter(PSS_SHA256);
            verifier.initVerify(key);
            verifier.update(message);
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            // 验签阶段抛异常与"验不过"等价：都表示拒绝
            return false;
        }
    }

    private static byte[] sign(String algorithm, PrivateKey key, byte[] message)
            throws GeneralSecurityException {
        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(key, RANDOM);
        signature.update(message);
        return signature.sign();
    }

    private static boolean verify(String algorithm, PublicKey key, byte[] message, byte[] signature) {
        try {
            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(key);
            verifier.update(message);
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static KeyPair rsaKeyPair(int bits) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits, RANDOM);
        return generator.generateKeyPair();
    }

    private static KeyPair ecKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        return generator.generateKeyPair();
    }

    private static KeyPair ed25519KeyPairOrNull() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    private static boolean verifyToyDsa(BigInteger p, BigInteger q, BigInteger g, BigInteger y,
                                        BigInteger z, BigInteger r, BigInteger s) {
        if (r.signum() == 0 || s.signum() == 0) {
            return false;
        }
        BigInteger w = s.modInverse(q);
        BigInteger u1 = z.multiply(w).mod(q);
        BigInteger u2 = r.multiply(w).mod(q);
        BigInteger v = g.modPow(u1, p).multiply(y.modPow(u2, p)).mod(p).mod(q);
        return v.equals(r);
    }

    private static byte[] hmac(byte[] key, byte[] message) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(message);
    }

    // ==================================================================
    // 输出与工具
    // ==================================================================

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

    private interface Signer {
        byte[] sign(byte[] message) throws Exception;
    }

    private interface Verifier {
        boolean verify(byte[] message, byte[] signature);
    }

    private static String[] row(String algorithm, String key, int signatureLength, String determinism,
                                Signer signer, Verifier verifier) throws Exception {
        for (int i = 0; i < 5; i++) {   // 预热
            verifier.verify(MESSAGE.getBytes(StandardCharsets.UTF_8),
                    signer.sign(MESSAGE.getBytes(StandardCharsets.UTF_8)));
        }
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);
        int iterations = 20;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            signer.sign(message);
        }
        double signMs = (System.nanoTime() - start) / 1e6 / iterations;
        byte[] signature = signer.sign(message);
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            verifier.verify(message, signature);
        }
        double verifyMs = (System.nanoTime() - start) / 1e6 / iterations;
        return new String[]{algorithm, key, signatureLength + " 字节", determinism,
                String.format("%.3f", signMs), String.format("%.3f", verifyMs)};
    }

    private static void printTable(List<String[]> rows) {
        int[] widths = new int[rows.get(0).length];
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], displayWidth(row[i]));
            }
        }
        for (int r = 0; r < rows.size(); r++) {
            StringBuilder line = new StringBuilder();
            String[] row = rows.get(r);
            for (int i = 0; i < row.length; i++) {
                line.append(padRight(row[i], widths[i]));
                if (i < row.length - 1) {
                    line.append("  ");
                }
            }
            System.out.println(line);
            if (r == 0) {
                StringBuilder divider = new StringBuilder();
                for (int i = 0; i < widths.length; i++) {
                    for (int j = 0; j < widths[i]; j++) {
                        divider.append('-');
                    }
                    divider.append("  ");
                }
                System.out.println(divider);
            }
        }
    }

    /** 中文按 2 列宽度计算，保证终端里表格不歪。 */
    private static int displayWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += text.charAt(i) < 0x2E80 ? 1 : 2;
        }
        return width;
    }

    private static String padRight(String text, int width) {
        StringBuilder builder = new StringBuilder(text);
        int current = displayWidth(text);
        while (current < width) {
            builder.append(' ');
            current++;
        }
        return builder.toString();
    }

    private static String sha256Hex(byte[] data) throws GeneralSecurityException {
        return hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String hex(byte[] data) {
        return Hex.toHex(data);
    }

    /** 公钥指纹：对 X.509 编码取 SHA-256，取前 8 字节。 */
    private static String fingerprint(PublicKey key) throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
        byte[] head = new byte[8];
        System.arraycopy(digest, 0, head, 0, head.length);
        return Hex.toHex(head) + "...";
    }

    private static byte[] tamperLastByte(byte[] signature) {
        byte[] copy = signature.clone();
        copy[copy.length - 1] ^= 0x01;
        return copy;
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < text.length(); i += width) {
            lines.add(text.substring(i, Math.min(text.length(), i + width)));
        }
        return lines;
    }
}
