package com.ds.crypto;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 数字证书：从"公钥怎么信"这个缺口出发，把一张 X.509 证书拆到字节，再逐条验证它的每道防线。
 *
 * <p>共 9 段，每段以自检收尾（结尾打印"自检通过 N / 失败 M"，失败时进程退出码 1）：
 * <ol>
 *   <li>信任缺口：签名机制本身没坏，坏的是"这把公钥到底属于谁"——中间人换公钥后签名照样验通过；</li>
 *   <li>手搓一张证书：用本包的 {@link Der} 手工拼 DER，tbsCertificate + 签名，交给
 *       {@link CertificateFactory} 解析，证明"证书就是一段被签名的结构化数据"；</li>
 *   <li>字段解剖：version / serial / issuer / validity / subject / SPKI / extensions 各防什么；</li>
 *   <li>篡改：改 subject、换公钥、改一个 bit、换签发的私钥，全部被拒绝；</li>
 *   <li>有效期：过期 / 未生效 / UTCTime 两位年份陷阱（2050 写成 "50" 被当成 1950）；</li>
 *   <li>CA 约束：普通证书的私钥去签别的证书，PKIX 校验直接拒绝；</li>
 *   <li>主机名：SAN 与通配符的匹配规则（*.example.com 只顶一层，且不匹配 example.com）；</li>
 *   <li>信任锚：打开 JDK 自带的 cacerts，看"你默认信任的一百多个组织"长什么样；</li>
 *   <li>小结：客户端拿到一张证书后要过的六道检查。</li>
 * </ol>
 *
 * <p>本类不实现任何密码算法，签名与验签用 JDK 自带 JCA；第 2 段的 DER 拼装是教学用的手搓，
 * 不要用于生产。真实证书请用 {@code keytool} / 商业 CA / Let's Encrypt 签发。
 *
 * <p>运行：{@code java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp target\cert-classes
 * com.ds.crypto.CertificateDemo}
 */
public final class CertificateDemo {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 演示消息：一条转账指令，证书要保护的是"这把公钥属于 Bob"这件事。 */
    private static final String MESSAGE = "转账指令：2026-11-01 从 A 账户转 100.00 元 给 B 账户";

    private static int passed;
    private static int failed;

    private CertificateDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("JDK 版本: " + System.getProperty("java.version")
                + "，证书解析器: " + CertificateFactory.getInstance("X.509").getProvider().getName());
        System.out.println("本演示全程离线，不访问任何网络。");
        section1TrustGap();
        section2BuildByHand();
        section3Anatomy();
        section4Tamper();
        section5Validity();
        section6CaConstraints();
        section7Hostname();
        section8TrustAnchors();
        section9Summary();
        System.out.println();
        System.out.println("自检：通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed > 0) {
            System.out.println("有自检失败，说明演示代码本身或环境不符合预期，退出码 1。");
            System.exit(1);
        }
    }

    // ==================================================================
    // 1. 信任缺口：签名没坏，坏的是"公钥属于谁"
    // ==================================================================
    private static void section1TrustGap() throws Exception {
        title(1, "信任缺口：为什么签名还不够");

        KeyPair ca = rsaKeyPair();
        KeyPair bob = rsaKeyPair();
        KeyPair mallory = rsaKeyPair();
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);

        System.out.println("场景：Bob 要在网上证明\"这条转账指令是我发的\"。");
        System.out.println("Alice 需要 Bob 的公钥才能验签，而公钥要么随消息一起传、要么去某个地方下载——");
        System.out.println("这条路上的任何一跳都能把公钥换掉。");
        System.out.println();

        byte[] sigByMallory = sign(mallory.getPrivate(), message);
        System.out.println("Mallory 截获了 Bob 发布的公钥，换成自己的公钥，再用自己的私钥签一条指令。");
        check(!verify(bob.getPublic(), message, sigByMallory),
                "用真正的 Bob 公钥验 Mallory 的签名 -> 拒绝（签名算法工作正常）");
        check(verify(mallory.getPublic(), message, sigByMallory),
                "用\"被换进来的那把公钥\"验签 -> 通过（Alice 以为这是 Bob 的公钥）");
        System.out.println();
        System.out.println("所以：签名能证明\"持有某私钥的人发的、内容没被改\"，但它回答不了");
        System.out.println("      \"这把公钥属于谁\"。这一步必须由第三方担保，这就是 CA 与证书的由来。");

        System.out.println();
        System.out.println("CA 的做法：把\"身份 + 公钥\"写成一段结构化数据，用 CA 自己的私钥签个名。");
        X509Certificate bobCert = parse(new CertSpec()
                .subject("Bob 的签名密钥", "Alg4 Demo", null, "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(bob.getPublic())
                .build(ca.getPrivate()));
        System.out.println("  Bob 的证书 subject = " + bobCert.getSubjectX500Principal().getName());
        check(verify(ca.getPublic(), tbsOf(bobCert.getEncoded()), signatureOf(bobCert.getEncoded())),
                "用事先装好的 CA 公钥验证书 -> 通过：CA 担保这把公钥属于 Bob");

        X509Certificate fakeBob = parse(new CertSpec()
                .subject("Bob 的签名密钥", "Alg4 Demo", null, "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")   // 冒用 CA 的名字
                .key(mallory.getPublic())
                .build(mallory.getPrivate()));                          // 但只签得起自己的名字
        checkThrows("Mallory 自己签一张写着 CA 名字的证书 -> 用 CA 公钥验签被拒绝",
                new Action() {
                    public void run() throws Exception {
                        fakeBob.verify(ca.getPublic());
                    }
                });
        System.out.println();
        System.out.println("结论：证书把\"公钥属于谁\"变成了一个签名问题；而验签只需要一把");
        System.out.println("      事先就信任的公钥（CA 的公钥）。链条到此为止——那把根公钥怎么来的，");
        System.out.println("      见第 8 段（预装在操作系统 / 浏览器 / JDK 里）。");
    }

    // ==================================================================
    // 2. 手搓一张证书
    // ==================================================================
    private static void section2BuildByHand() throws Exception {
        title(2, "手搓一张 X.509 证书：它只是\"一段被签名的 DER 字节\"");

        KeyPair ca = rsaKeyPair();
        KeyPair server = rsaKeyPair();

        CertSpec spec = new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic())
                .san("server.example.com", "www.example.com")
                .ekuServerAuth();
        byte[] tbs = spec.tbs();
        byte[] certDer = spec.build(ca.getPrivate());

        System.out.println("X.509 v3 证书的三段结构：");
        System.out.println("  Certificate ::= SEQUENCE {");
        System.out.println("      tbsCertificate      -- 待签名数据：版本、序列号、签发者、有效期、主体、公钥、扩展");
        System.out.println("      signatureAlgorithm  -- 用什么算法签的（必须与 tbs 里那个字段一致）");
        System.out.println("      signatureValue      -- 对 tbsCertificate 整个字节串的签名");
        System.out.println("  }");
        System.out.println();
        System.out.println("tbsCertificate 共 " + tbs.length + " 字节，开头 32 字节："
                + Hex.toHex(Arrays.copyOf(tbs, 32)) + " ...");
        System.out.println();
        System.out.println("把它按 TLV 一层层拆开（缩进 = 嵌套层级）：");
        System.out.print(Der.dump(tbs));

        List<byte[]> top = Der.children(certDer);
        System.out.println();
        System.out.println("整张证书刚好三段：tbsCertificate " + top.get(0).length + " 字节 / signatureAlgorithm "
                + top.get(1).length + " 字节 / signatureValue " + top.get(2).length + " 字节。");
        System.out.println("注意扩展的 extnValue 是 OCTET STRING，里面**又套了一段 DER**（扩展值本身也是 ASN.1 结构）：");
        byte[] sanValue = extensionValue(tbs, "2.5.29.17");
        if (sanValue != null) {
            System.out.print(Der.dump(sanValue));
        }

        X509Certificate cert = parse(certDer);
        System.out.println();
        System.out.println("CertificateFactory 解析结果：" + cert.getType()
                + "，版本 v" + cert.getVersion()
                + "，签名算法 " + cert.getSigAlgName());
        check(cert.getVersion() == 3, "手拼的字节被 JDK 当成合法的 v3 证书");
        check(cert.getSubjectX500Principal().getName().contains("CN=server.example.com"),
                "subject 解析回 CN=server.example.com");
        check(verify(ca.getPublic(), tbsOf(certDer), signatureOf(certDer)),
                "用 CA 公钥验签通过：签名覆盖的正是 tbsCertificate 那一整段字节");
        check(Arrays.equals(cert.getEncoded(), certDer),
                "证书重新编码后与手拼的字节逐字节相同 -> 我们造的就是标准 X.509");
        System.out.println();
        System.out.println("所以\"证书\"没有任何魔法：它就是一段按 ASN.1 排好版的字节，");
        System.out.println("被 CA 的私钥用 SHA256withRSA 签了名。X.509 只是大家约好的字段排布。");
    }

    // ==================================================================
    // 3. 字段解剖
    // ==================================================================
    private static void section3Anatomy() throws Exception {
        title(3, "字段解剖：每个字段各防什么");

        KeyPair ca = rsaKeyPair();
        KeyPair server = rsaKeyPair();
        X509Certificate cert = parse(new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic())
                .san("server.example.com", "www.example.com")
                .ekuServerAuth()
                .build(ca.getPrivate()));

        System.out.println(pad("字段", 20) + pad("值", 44) + "作用（防什么）");
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println(pad("version", 20) + pad("v" + cert.getVersion(), 44)
                + "扩展只有 v3 才能带，现代证书一律 v3");
        System.out.println(pad("serialNumber", 20) + pad("0x" + cert.getSerialNumber().toString(16), 44)
                + "证书的唯一编号，吊销时按它指名（CRL/OCSP 都用它）");
        System.out.println(pad("signature", 20) + pad(cert.getSigAlgName(), 44)
                + "签发用的算法，篡改它会让验签失败");
        System.out.println(pad("issuer", 20) + pad(abbreviate(cert.getIssuerX500Principal().getName(), 42), 44)
                + "谁签的；验签时用它去找上级公钥");
        System.out.println(pad("validity", 20)
                + pad(fmtShort(cert.getNotBefore()) + " ~ " + fmtShort(cert.getNotAfter()), 44)
                + "有效期：过期/未生效一律拒绝（见第 5 段）");
        System.out.println(pad("subject", 20) + pad(abbreviate(cert.getSubjectX500Principal().getName(), 42), 44)
                + "证书给谁；TLS 里还要和你要连的主机名核对（见第 7 段）");
        System.out.println(pad("publicKey", 20) + pad("RSA-" + keyBits(cert.getPublicKey()), 44)
                + "被绑定的公钥本体——整张证书就是为了它");
        System.out.println(pad("signatureValue", 20) + pad("256B", 44)
                + "CA 私钥对上面全部字段的签名");
        System.out.println(pad("extensions", 20) + pad("basicConstraints / keyUsage", 44)
                + "约束\"这张证书能干什么\"（见下）");
        System.out.println(pad("", 20) + pad("/ SAN / EKU / SKI", 44));

        System.out.println();
        System.out.println("扩展里最关键的三条：");
        System.out.println("  basicConstraints CA=" + (cert.getBasicConstraints() >= 0)
                + "  —— false 表示这是\"终端证书\"，不能用来签下一代证书；");
        System.out.println("                       能签证书的 CA 证书必须写 CA:true（见第 6 段）");
        System.out.println("  keyUsage         " + keyUsageText(cert.getKeyUsage())
                + "  —— 只允许干这些事，越权即拒");
        System.out.println("  subjectAltName   " + sanText(cert)
                + "  —— 证书真正覆盖哪些域名/IP（现代客户端只认它）");

        check(cert.getBasicConstraints() == -1, "终端证书的 basicConstraints 是 CA:false（getBasicConstraints 返回 -1）");
        check(cert.getKeyUsage() != null && cert.getKeyUsage()[0], "keyUsage 里有 digitalSignature 位");
        check(cert.getSubjectAlternativeNames() != null && cert.getSubjectAlternativeNames().size() == 2,
                "SAN 里有两个 dNSName");
        System.out.println();
        System.out.println("注意 issuer 与 subject 都是\"目录名\"（DN），格式是 C=..,O=..,CN=..；");
        System.out.println("DN 只是文字，谁都可以写成任何样子——它的可信度完全来自\"谁签的\"。");
    }

    // ==================================================================
    // 4. 篡改
    // ==================================================================
    private static void section4Tamper() throws Exception {
        title(4, "篡改：签名到底盖住了哪些字节");

        KeyPair ca = rsaKeyPair();
        KeyPair otherCa = rsaKeyPair();
        KeyPair server = rsaKeyPair();
        KeyPair attacker = rsaKeyPair();

        CertSpec original = new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic())
                .san("server.example.com");
        byte[] tbs = original.tbs();
        byte[] signature = sign(ca.getPrivate(), tbs);
        byte[] cert = Der.seq(tbs, algorithmIdentifier(), Der.bitString(signature));

        // (1) 改 subject：用 CA 的签名去盖另一段 tbs
        CertSpec evilSubject = new CertSpec()
                .subject("evil.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic())
                .san("server.example.com");
        byte[] spliced = Der.seq(evilSubject.tbs(), algorithmIdentifier(), Der.bitString(signature));
        checkThrows("把 subject 改成 evil.example.com，签名不动 -> 验签拒绝", new Action() {
            public void run() throws Exception {
                parse(spliced).verify(ca.getPublic());
            }
        });

        // (2) 换公钥：tbs 里换成攻击者的公钥，签名不动
        CertSpec evilKey = new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(attacker.getPublic())
                .san("server.example.com");
        byte[] keySwapped = Der.seq(evilKey.tbs(), algorithmIdentifier(), Der.bitString(signature));
        checkThrows("把公钥换成攻击者的公钥，签名不动 -> 验签拒绝（公钥被签名保护）", new Action() {
            public void run() throws Exception {
                parse(keySwapped).verify(ca.getPublic());
            }
        });

        // (3) 换签发的私钥：同一段 tbs，用另一把 CA 私钥签
        byte[] signedByOther = Der.seq(tbs, algorithmIdentifier(),
                Der.bitString(sign(otherCa.getPrivate(), tbs)));
        checkThrows("用另一把 CA 私钥签同一段 tbs -> 用真 CA 公钥验签拒绝", new Action() {
            public void run() throws Exception {
                parse(signedByOther).verify(ca.getPublic());
            }
        });

        // (4) 只改一个 bit：直接改 tbs 的最后一个字节（SKI 扩展里的字节）
        byte[] flipped = tbs.clone();
        flipped[flipped.length - 1] ^= 0x01;
        byte[] oneBit = Der.seq(flipped, algorithmIdentifier(), Der.bitString(signature));
        checkThrows("只翻转 tbs 最后一个字节的 1 个 bit -> 验签拒绝", new Action() {
            public void run() throws Exception {
                parse(oneBit).verify(ca.getPublic());
            }
        });

        check(isValid(parse(cert), ca.getPublic()), "对照组：原始证书验签通过");

        System.out.println();
        System.out.println("结论：签名覆盖的是 tbsCertificate 的**全部字节**，改一个 bit 都不行。");
        System.out.println("      所以攻击者改不了 subject 也换不了公钥——他只能做另一件事：");
        System.out.println("      去找一个客户端本来就信任的 CA，让它来签。这就是第 6、8 段要防的。");
    }

    // ==================================================================
    // 5. 有效期与 UTC 两位年份陷阱
    // ==================================================================
    private static void section5Validity() throws Exception {
        title(5, "有效期：过期、未生效、以及 2050 年的两位年份陷阱");

        KeyPair ca = rsaKeyPair();
        KeyPair server = rsaKeyPair();
        long now = System.currentTimeMillis();
        long day = 24L * 3600 * 1000;

        X509Certificate expired = parse(new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic())
                .validity(new Date(now - 10 * day), new Date(now - 1 * day))
                .build(ca.getPrivate()));
        System.out.println("过期证书：notAfter = " + fmt(expired.getNotAfter()) + "（昨天）");
        checkThrows("checkValidity() -> 抛 CertificateExpiredException", new Action() {
            public void run() throws Exception {
                expired.checkValidity();
            }
        });

        X509Certificate notYet = parse(new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic())
                .validity(new Date(now + 1 * day), new Date(now + 30 * day))
                .build(ca.getPrivate()));
        System.out.println("未生效证书：notBefore = " + fmt(notYet.getNotBefore()) + "（明天）");
        checkThrows("checkValidity() -> 抛 CertificateNotYetValidException", new Action() {
            public void run() throws Exception {
                notYet.checkValidity();
            }
        });

        System.out.println();
        System.out.println("--- UTCTime 的两位年份陷阱 ---");
        Calendar calendar = Calendar.getInstance(Der.UTC);
        calendar.clear();
        calendar.set(2050, Calendar.DECEMBER, 31, 23, 59, 59);
        Date year2050 = calendar.getTime();

        X509Certificate correct2050 = parse(new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic())
                .validity(new Date(now - day), year2050)
                .build(ca.getPrivate()));
        System.out.println("想表达\"2050-12-31 到期\"，按规则用 GeneralizedTime(\"20501231235959Z\")：");
        System.out.println("  解析回来 notAfter = " + fmt(correct2050.getNotAfter()) + "  <- 正确");
        check(isValid(correct2050, ca.getPublic()), "这张证书现在有效（2050 年还没到）");

        X509Certificate trap2050 = parse(new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic())
                .validity(new Date(now - day), year2050)
                .forceUtcTime()
                .build(ca.getPrivate()));
        System.out.println("同一个日期，若有人用两位年份写成 UTCTime(\"501231235959Z\")：");
        System.out.println("  解析回来 notAfter = " + fmt(trap2050.getNotAfter()) + "  <- 变成 1950 年了");
        checkThrows("→ 这张证书被判为早已过期（CertificateExpiredException）", new Action() {
            public void run() throws Exception {
                trap2050.checkValidity();
            }
        });
        System.out.println();
        System.out.println("规则来自 RFC 5280：UTCTime 的 YY≥50 视为 19YY，YY<50 视为 20YY。");
        System.out.println("所以 2050 年及以后必须用 GeneralizedTime，四位年份，不给自己挖坑。");
    }

    // ==================================================================
    // 6. CA 约束：普通证书不能当 CA 用
    // ==================================================================
    private static void section6CaConstraints() throws Exception {
        title(6, "CA 约束：basicConstraints / keyUsage / pathLen 拦住\"自己给自己发证\"");

        KeyPair root = rsaKeyPair();
        KeyPair intermediate = rsaKeyPair();
        KeyPair server = rsaKeyPair();
        KeyPair evil = rsaKeyPair();
        KeyPair forged = rsaKeyPair();

        X509Certificate rootCert = parse(new CertSpec()
                .subject("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(root.getPublic()).ca(true).keyCertSign().cRLSign()
                .build(root.getPrivate()));
        X509Certificate intermediateCert = parse(new CertSpec()
                .subject("Alg4 Demo Intermediate CA", "Alg4 Demo", null, "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(intermediate.getPublic()).ca(true).pathLen(0).keyCertSign().cRLSign()
                .build(root.getPrivate()));
        X509Certificate serverCert = parse(new CertSpec()
                .subject("server.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Intermediate CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic()).san("server.example.com").ekuServerAuth()
                .build(intermediate.getPrivate()));

        System.out.println("正常链：root（CA:true） -> intermediate（CA:true, pathLen=0） -> server.example.com");
        check(validate(new CertPathHolder(serverCert, intermediateCert), rootCert),
                "PKIX 校验通过：链走到预置的根，且每一级都是合格的 CA");

        System.out.println();
        System.out.println("攻击链：普通证书（CA:false）用它的私钥去签一张\"Bob 的证书\"，再把它当中间 CA 拼进链里。");
        X509Certificate evilCert = parse(new CertSpec()
                .subject("www.evil.example", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(evil.getPublic()).san("www.evil.example")
                .build(root.getPrivate()));
        X509Certificate forgedCert = parse(new CertSpec()
                .subject("Bob", "Alg4 Demo", null, "CN")
                .issuer("www.evil.example", "Alg4 Demo", "Web", "CN")
                .key(forged.getPublic())
                .build(evil.getPrivate()));      // 普通证书的私钥签的
        boolean forgedAccepted = validate(new CertPathHolder(forgedCert, evilCert), rootCert);
        check(!forgedAccepted, "PKIX 校验 -> 拒绝（普通证书不是 CA，不能签发下一级）");
        System.out.println("  被拒原因：" + lastRejectReason);

        System.out.println();
        System.out.println("这就是为什么 basicConstraints 与 keyUsage 是**关键扩展（critical）**：");
        System.out.println("  basicConstraints: CA:true + pathLen:0  —— 我能签证书，但只能签终端证书；");
        System.out.println("  keyUsage: keyCertSign                  —— 只允许做\"签发证书\"这一件事；");
        System.out.println("两者缺一，校验器就不让你当中间 CA。攻击者只能去偷 CA 的私钥，不能自己声明是 CA。");
    }

    // ==================================================================
    // 7. 主机名与 SAN
    // ==================================================================
    private static void section7Hostname() throws Exception {
        title(7, "主机名：SAN 里的名字怎么和你要连的主机对上");

        KeyPair ca = rsaKeyPair();
        KeyPair server = rsaKeyPair();
        X509Certificate exact = parse(new CertSpec()
                .subject("www.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic()).san("www.example.com")
                .build(ca.getPrivate()));
        X509Certificate wildcard = parse(new CertSpec()
                .subject("*.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(server.getPublic()).san("*.example.com")
                .build(ca.getPrivate()));

        System.out.println("证书 A 的 SAN: " + sanText(exact));
        System.out.println("证书 B 的 SAN: " + sanText(wildcard));
        System.out.println();
        System.out.println("规则：先看 SAN（Subject Alternative Name）；通配符只允许出现在最左一个标签，且只顶一层。");
        System.out.println();

        check(hostMatches(exact, "www.example.com"), "A vs www.example.com -> 匹配");
        check(!hostMatches(exact, "www.example.com.evil.cn"), "A vs www.example.com.evil.cn -> 不匹配（后缀不同）");
        check(!hostMatches(exact, "example.com"), "A vs example.com -> 不匹配（少了 www 这一层）");
        check(hostMatches(wildcard, "a.example.com"), "B vs a.example.com -> 匹配（* 顶一层）");
        check(!hostMatches(wildcard, "a.b.example.com"), "B vs a.b.example.com -> 不匹配（* 不能跨层）");
        check(!hostMatches(wildcard, "example.com"), "B vs example.com -> 不匹配（* 必须至少顶一层）");
        check(!hostMatches(wildcard, "evilexample.com"), "B vs evilexample.com -> 不匹配（必须有点号边界）");

        System.out.println();
        System.out.println("常见误解：");
        System.out.println("  1) \"证书里写了 CN=www.example.com 就够了\" —— 不。现代客户端只认 SAN；");
        System.out.println("     CA/Browser Forum 2017 年起要求公开 CA 签发的证书必须带 SAN。");
        System.out.println("  2) \"*.example.com 能匹配 a.b.example.com\" —— 不能，* 只顶一层标签；");
        System.out.println("     它也不能匹配 example.com 本身。");
        System.out.println("  3) \"证书配了就能用\" —— 证书覆盖的是\"名字\"，你连的地址必须和 SAN 对得上。");

        KeyPair self = rsaKeyPair();
        X509Certificate cnOnly = parse(new CertSpec()
                .subject("cnonly.example.com", "Alg4 Demo", "Web", "CN")
                .issuer("Alg4 Demo Root CA", "Alg4 Demo", null, "CN")
                .key(self.getPublic())          // 故意不写 SAN
                .build(ca.getPrivate()));
        check(cnOnly.getSubjectAlternativeNames() == null, "不写 SAN 的证书：getSubjectAlternativeNames() 返回 null");
        check(!hostMatches(cnOnly, "cnonly.example.com"),
                "按现代规则，只有 CN 没有 SAN -> 不匹配（CN 只是给人看的）");
        System.out.println();
        System.out.println("（JDK 17 的 TLS 校验在 SAN 缺失时仍会回退去比 CN——CertChainDemo 场景 5 实测握手成功；");
        System.out.println("  浏览器对公开 CA 签发的证书只认 SAN。要兼容老客户端才需要留 CN。）");
    }

    // ==================================================================
    // 8. 信任锚：cacerts 里躺着谁
    // ==================================================================
    private static void section8TrustAnchors() throws Exception {
        title(8, "信任锚：你凭什么是\"可信\"的");

        System.out.println("验签总得有一把\"一开始就信\"的公钥。它来自操作系统 / 浏览器 / 运行时预装的根证书库。");
        System.out.println("下面打开 JDK 自带的 cacerts（口令 changeit）看真实数据：");
        System.out.println("  " + cacertsPath());

        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        java.io.InputStream in = java.nio.file.Files.newInputStream(cacertsPath());
        try {
            store.load(in, "changeit".toCharArray());
        } finally {
            in.close();
        }
        List<String> aliases = Collections.list(store.aliases());
        Collections.sort(aliases);
        System.out.println("  共 " + aliases.size() + " 张预置根证书（去重后的信任锚）。前 3 张：");
        int shown = 0;
        for (String alias : aliases) {
            Object entry = store.getCertificate(alias);
            if (!(entry instanceof X509Certificate)) {
                continue;
            }
            X509Certificate cert = (X509Certificate) entry;
            System.out.println("  - " + alias);
            System.out.println("      subject : " + abbreviate(cert.getSubjectX500Principal().getName(), 90));
            System.out.println("      有效期  : " + fmt(cert.getNotBefore()) + " ~ " + fmt(cert.getNotAfter())
                    + "，算法 " + cert.getSigAlgName());
            System.out.println("      SHA-256 : " + fingerprint(cert));
            if (++shown >= 3) {
                break;
            }
        }
        check(aliases.size() > 50, "JDK 预置的根证书数量 > 50（这就是默认信任集合）");
        System.out.println();
        System.out.println("要点：");
        System.out.println("  - 信任是**传递**的：你信根，根签中间，中间签站点，于是你信站点；");
        System.out.println("  - 根证书的私钥对应着\"签发能力\"，所以必须离线保管、多年才轮换一次；");
        System.out.println("  - 企业做 HTTPS 解密（MITM 代理）时，就是往客户端塞一张自制根证书——");
        System.out.println("    客户端从此信任它签发的所有站点证书。这也解释了为什么\"装根证书\"是敏感操作。");
    }

    // ==================================================================
    // 9. 小结
    // ==================================================================
    private static void section9Summary() {
        title(9, "小结：一张证书到手后要过的六道检查");

        System.out.println("  1. 签名    ：用签发者（issuer）的公钥验 tbsCertificate 的签名 —— 内容没被改；");
        System.out.println("  2. 签发者  ：issuer 的证书是否可信？往上递归，直到撞上预置的信任锚（根证书）；");
        System.out.println("  3. 有效期  ：notBefore <= 现在 <= notAfter；");
        System.out.println("  4. CA 资格 ：链上每一级（除了叶子）必须是 CA:true 且带 keyCertSign，pathLen 不越界；");
        System.out.println("  5. 用途    ：keyUsage / extendedKeyUsage 允许这次用途（如 serverAuth）；");
        System.out.println("  6. 主机名  ：SAN 里有没有你要连的那个名字（或 IP）。");
        System.out.println("  另外还有第 7 道：吊销（CRL / OCSP）—— 证书没过期但私钥泄露了，得能让它立刻失效。");
        System.out.println();
        System.out.println("一句话：证书 = \"身份 + 公钥 + 用途 + 有效期\" 的一段数据，被 CA 的私钥签过名；");
        System.out.println("        验证证书 = 一路把签名验到某张你本来就信的根证书上，再逐条查那些约束。");
        System.out.println();
        System.out.println("真实世界里这套流程由 TLS 握手自动完成，下一段演示（CertChainDemo）会用 keytool");
        System.out.println("生成 root -> intermediate -> server 的证书链，并真跑一次 TLS 握手看它怎么被拒。");
    }

    // ==================================================================
    // 证书规格：把\"拼字段\"和\"签名\"分开，方便做篡改实验
    // ==================================================================

    /** 一张证书的字段集合。链式 setter 便于阅读，字段默认值对齐一张普通终端证书。 */
    static final class CertSpec {
        private String subjectCn;
        private String subjectO;
        private String subjectOu;
        private String subjectC = "CN";
        private String issuerCn;
        private String issuerO;
        private String issuerOu;
        private String issuerC = "CN";
        private BigInteger serial = new BigInteger(63, RANDOM).add(BigInteger.ONE);
        private Date notBefore = new Date(System.currentTimeMillis() - 3600_000L);
        private Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000);
        private boolean forceUtcTime;
        private PublicKey key;
        private boolean ca;
        private int pathLen = -1;
        private boolean digitalSignature = true;
        private boolean keyEncipherment;
        private boolean keyCertSign;
        private boolean cRLSign;
        private String[] sanDns = new String[0];
        private String eku;

        CertSpec subject(String cn, String o, String ou, String c) {
            this.subjectCn = cn;
            this.subjectO = o;
            this.subjectOu = ou;
            this.subjectC = c;
            return this;
        }

        CertSpec issuer(String cn, String o, String ou, String c) {
            this.issuerCn = cn;
            this.issuerO = o;
            this.issuerOu = ou;
            this.issuerC = c;
            return this;
        }

        CertSpec key(PublicKey key) {
            this.key = key;
            return this;
        }

        CertSpec validity(Date notBefore, Date notAfter) {
            this.notBefore = notBefore;
            this.notAfter = notAfter;
            return this;
        }

        /** 故意用两位年份的 UTCTime 编码 2050 年，复现 RFC 5280 的两位年份陷阱。 */
        CertSpec forceUtcTime() {
            this.forceUtcTime = true;
            return this;
        }

        CertSpec ca(boolean ca) {
            this.ca = ca;
            return this;
        }

        CertSpec pathLen(int pathLen) {
            this.pathLen = pathLen;
            return this;
        }

        CertSpec keyEncipherment() {
            this.keyEncipherment = true;
            return this;
        }

        CertSpec keyCertSign() {
            this.keyCertSign = true;
            return this;
        }

        CertSpec cRLSign() {
            this.cRLSign = true;
            return this;
        }

        CertSpec san(String... dnsNames) {
            this.sanDns = dnsNames;
            return this;
        }

        CertSpec ekuServerAuth() {
            this.eku = "1.3.6.1.5.5.7.3.1";
            return this;
        }

        byte[] tbs() throws Exception {
            List<byte[]> parts = new ArrayList<byte[]>();
            parts.add(Der.explicit(0, Der.integer(2)));                       // version = v3
            parts.add(Der.integer(serial));
            parts.add(algorithmIdentifier());
            parts.add(name(issuerC, issuerO, issuerOu, issuerCn));
            parts.add(Der.seq(time(notBefore), time(notAfter)));
            parts.add(name(subjectC, subjectO, subjectOu, subjectCn));
            parts.add(key.getEncoded());                                      // SubjectPublicKeyInfo
            List<byte[]> extensions = extensions();
            if (!extensions.isEmpty()) {
                parts.add(Der.explicit(3, Der.seq(extensions.toArray(new byte[0][]))));
            }
            return Der.seq(parts.toArray(new byte[0][]));
        }

        /** 用签发者的私钥签出完整证书（tbs + 算法标识 + 签名值）。 */
        byte[] build(PrivateKey issuerKey) throws Exception {
            byte[] tbs = tbs();
            byte[] signature = sign(issuerKey, tbs);
            return Der.seq(tbs, algorithmIdentifier(), Der.bitString(signature));
        }

        private List<byte[]> extensions() throws Exception {
            List<byte[]> list = new ArrayList<byte[]>();
            list.add(extension("2.5.29.19", true, basicConstraints()));
            list.add(extension("2.5.29.15", true, keyUsage()));
            if (sanDns.length > 0) {
                List<byte[]> names = new ArrayList<byte[]>();
                for (String dns : sanDns) {
                    names.add(Der.tlv(0x82, dns.getBytes(StandardCharsets.US_ASCII)));   // [2] dNSName
                }
                list.add(extension("2.5.29.17", false, Der.seq(names.toArray(new byte[0][]))));
            }
            if (eku != null) {
                list.add(extension("2.5.29.37", false, Der.seq(Der.oid(eku))));
            }
            list.add(extension("2.5.29.14", false, Der.octetString(subjectKeyId())));
            return list;
        }

        private byte[] basicConstraints() {
            if (!ca) {
                // CA:false 是 DEFAULT，DER 要求省略，所以写成空 SEQUENCE
                return Der.seq();
            }
            return pathLen >= 0 ? Der.seq(Der.bool(true), Der.integer(pathLen)) : Der.seq(Der.bool(true));
        }

        /** KeyUsage 位串：bit0 digitalSignature、bit2 keyEncipherment、bit5 keyCertSign、bit6 cRLSign。 */
        private byte[] keyUsage() {
            int bits = 0;
            if (digitalSignature) {
                bits |= 1 << 7;
            }
            if (keyEncipherment) {
                bits |= 1 << 5;
            }
            if (keyCertSign) {
                bits |= 1 << 2;
            }
            if (cRLSign) {
                bits |= 1 << 1;
            }
            if (bits == 0) {
                return Der.bitString(0, new byte[]{0});
            }
            int msbIndex = 7 - Integer.numberOfTrailingZeros(bits);
            return Der.bitString(7 - msbIndex, new byte[]{(byte) bits});
        }

        /** SubjectKeyIdentifier = SHA-1(公钥位串)，RFC 5280 §4.2.1.2 推荐的生成方式。 */
        private byte[] subjectKeyId() throws Exception {
            List<byte[]> spki = Der.children(key.getEncoded());
            byte[] bitStringContent = Der.content(spki.get(1));
            byte[] publicKeyBits = Der.slice(bitStringContent, 1, bitStringContent.length - 1);
            return MessageDigest.getInstance("SHA-1").digest(publicKeyBits);
        }

        private byte[] time(Date date) {
            Calendar calendar = Calendar.getInstance(Der.UTC);
            calendar.setTime(date);
            int year = calendar.get(Calendar.YEAR);
            if (!forceUtcTime && year >= 2050) {
                return Der.generalizedTime(Der.generalized(date));
            }
            return Der.utcTime(Der.utc(date));
        }

        private static byte[] extension(String oid, boolean critical, byte[] value) {
            if (critical) {
                return Der.seq(Der.oid(oid), Der.bool(true), Der.octetString(value));
            }
            return Der.seq(Der.oid(oid), Der.octetString(value));
        }

        private static byte[] name(String c, String o, String ou, String cn) {
            List<byte[]> rdns = new ArrayList<byte[]>();
            if (c != null) {
                rdns.add(Der.set(Der.seq(Der.oid("2.5.4.6"), Der.printable(c))));
            }
            if (o != null) {
                rdns.add(Der.set(Der.seq(Der.oid("2.5.4.10"), Der.utf8(o))));
            }
            if (ou != null) {
                rdns.add(Der.set(Der.seq(Der.oid("2.5.4.11"), Der.utf8(ou))));
            }
            if (cn != null) {
                rdns.add(Der.set(Der.seq(Der.oid("2.5.4.3"), Der.utf8(cn))));
            }
            return Der.seq(rdns.toArray(new byte[0][]));
        }
    }

    // ==================================================================
    // 工具
    // ==================================================================

    private static byte[] algorithmIdentifier() {
        return Der.seq(Der.oid("1.2.840.113549.1.1.11"), Der.nul());   // sha256WithRSAEncryption
    }

    private static X509Certificate parse(byte[] der) throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(der));
    }

    private static byte[] tbsOf(byte[] certificateDer) {
        return Der.children(certificateDer).get(0);
    }

    /**
     * 从 tbsCertificate 里取出某个扩展的 extnValue（OCTET STRING 的内容）。
     * 结构是 tbs 的 [3] -> SEQUENCE OF Extension，每个 Extension = SEQUENCE{OID, [critical], OCTET STRING}。
     */
    private static byte[] extensionValue(byte[] tbs, String oid) {
        for (byte[] child : Der.children(tbs)) {
            if (Der.node(child, 0).tag != 0xA3) {        // 0xA3 = [3] extensions
                continue;
            }
            byte[] extensionsSequence = Der.content(child);
            for (byte[] extension : Der.children(extensionsSequence)) {
                List<byte[]> fields = Der.children(extension);
                String id = Der.decodeOidRaw(Der.content(fields.get(0)));
                if (oid.equals(id)) {
                    return Der.content(fields.get(fields.size() - 1));
                }
            }
        }
        return null;
    }

    private static byte[] signatureOf(byte[] certificateDer) {
        byte[] bitString = Der.content(Der.children(certificateDer).get(2));
        return Der.slice(bitString, 1, bitString.length - 1);
    }

    /** 手工走一遍链：path[0] 是叶子，path[1..] 是中间 CA；anchor 是预置的根。 */
    private static final class CertPathHolder {
        private final List<X509Certificate> chain = new ArrayList<X509Certificate>();

        CertPathHolder(X509Certificate... certs) {
            Collections.addAll(chain, certs);
        }
    }

    private static String lastRejectReason = "(无)";

    private static boolean validate(CertPathHolder holder, X509Certificate anchor) {
        try {
            CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(holder.chain);
            Set<TrustAnchor> anchors = Collections.singleton(new TrustAnchor(anchor, null));
            PKIXParameters parameters = new PKIXParameters(anchors);
            parameters.setRevocationEnabled(false);      // 离线演示不做 CRL/OCSP
            CertPathValidator.getInstance("PKIX").validate(path, parameters);
            lastRejectReason = "(通过)";
            return true;
        } catch (Exception e) {
            lastRejectReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            return false;
        }
    }

    private static boolean isValid(X509Certificate cert, PublicKey issuerKey) {
        try {
            cert.checkValidity();
            cert.verify(issuerKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** RFC 6125 的简化版：先看 SAN 的 dNSName，通配符只允许最左一个标签且只顶一层。 */
    private static boolean hostMatches(X509Certificate cert, String host) {
        String lowerHost = host.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<String>();
        try {
            java.util.Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans != null) {
                for (List<?> san : sans) {
                    if (((Integer) san.get(0)) == 2) {          // 2 = dNSName
                        names.add(((String) san.get(1)).toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        for (String name : names) {
            if (name.startsWith("*.")) {
                String suffix = name.substring(1);              // ".example.com"
                if (!lowerHost.endsWith(suffix)) {
                    continue;
                }
                String left = lowerHost.substring(0, lowerHost.length() - suffix.length());
                if (!left.isEmpty() && left.indexOf('.') < 0) {
                    return true;
                }
            } else if (name.equals(lowerHost)) {
                return true;
            }
        }
        return false;
    }

    private static String sanText(X509Certificate cert) {
        try {
            java.util.Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans == null) {
                return "(无 SAN)";
            }
            StringBuilder sb = new StringBuilder();
            for (List<?> san : sans) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(((Integer) san.get(0)) == 2 ? "DNS:" : "[" + san.get(0) + "]:");
                sb.append(san.get(1));
            }
            return sb.toString();
        } catch (Exception e) {
            return "(解析失败)";
        }
    }

    private static String keyUsageText(boolean[] usage) {
        if (usage == null) {
            return "(未声明)";
        }
        String[] names = {"digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment",
                "keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < usage.length && i < names.length; i++) {
            if (usage[i]) {
                if (sb.length() > 0) {
                    sb.append("+");
                }
                sb.append(names[i]);
            }
        }
        return sb.toString();
    }

    private static java.nio.file.Path cacertsPath() {
        return java.nio.file.Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static byte[] sign(PrivateKey key, byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(data);
        return signature.sign();
    }

    private static boolean verify(PublicKey key, byte[] data, byte[] signatureValue) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update(data);
            return signature.verify(signatureValue);
        } catch (Exception e) {
            return false;
        }
    }

    private static String fingerprint(X509Certificate cert) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                if (i > 0) {
                    sb.append(':');
                }
                sb.append(String.format("%02X", digest[i] & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            return "(计算失败)";
        }
    }

    private static int keyBits(PublicKey key) {
        if (key instanceof java.security.interfaces.RSAPublicKey) {
            return ((java.security.interfaces.RSAPublicKey) key).getModulus().bitLength();
        }
        return key.getEncoded().length * 8;
    }

    private static String fmt(Date date) {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
        format.setTimeZone(Der.UTC);
        return format.format(date);
    }

    /** 表格里用的短日期，只到分钟，避免一列放不下。 */
    private static String fmtShort(Date date) {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        format.setTimeZone(Der.UTC);
        return format.format(date);
    }

    private static String abbreviate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static String pad(String text, int width) {
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
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

    private interface Action {
        void run() throws Exception;
    }

    /** 断言这段代码一定抛异常；顺便把异常类型与消息打印出来，证明它不是"碰巧崩了"。 */
    private static void checkThrows(String what, Action action) {
        try {
            action.run();
            failed++;
            System.out.println("  [失败] " + what + "（本应失败，却成功了）");
        } catch (Exception e) {
            passed++;
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = "(无消息)";
            }
            System.out.println("  [通过] " + what);
            System.out.println("         实际抛出 " + e.getClass().getSimpleName() + ": " + abbreviate(message, 100));
        }
    }
}
