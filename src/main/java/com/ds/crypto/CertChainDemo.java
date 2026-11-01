package com.ds.crypto;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 真实证书链 + 真 TLS 握手：用 {@code keytool} 生成的 root → intermediate → server 证书链，
 * 看客户端（JDK 自带 TLS 实现）在什么情况下接受、什么情况下拒绝。
 *
 * <p>证书与密钥由 {@code scripts/run-digital-certificate-demo.ps1} 生成，
 * 默认目录 {@code target/cert-demo}，口令 {@code changeit}。本类跑 5 个 TLS 场景：
 * <ol>
 *   <li>证书链完整、主机名对得上 -> 握手成功；</li>
 *   <li>证书只签了 shop.example.com，却去连 localhost -> 主机名不匹配，拒绝；</li>
 *   <li>证书已过期 -> 拒绝；</li>
 *   <li>证书是另一家 CA 签的，客户端不信任 -> 找不到信任路径，拒绝；</li>
 *   <li>只有 CN=localhost、没有 SAN -> 观察 JDK 实际行为（这是"CN 还能不能用"的实测）。</li>
 * </ol>
 *
 * <p>运行：{@code java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp target\cert-classes
 * com.ds.crypto.CertChainDemo target\cert-demo changeit}
 */
public final class CertChainDemo {

    private static int passed;
    private static int failed;
    private static String rejectReason = "(无)";

    private CertChainDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "target/cert-demo");
        char[] pass = (args.length > 1 ? args[1] : "changeit").toCharArray();
        System.out.println("证书目录: " + dir.toAbsolutePath());
        System.out.println("JDK 版本: " + System.getProperty("java.version"));

        KeyStore rootStore = load(dir.resolve("root.p12"), pass);
        KeyStore intermediateStore = load(dir.resolve("intermediate.p12"), pass);
        KeyStore serverStore = load(dir.resolve("server.p12"), pass);
        KeyStore otherRootStore = load(dir.resolve("otherroot.p12"), pass);

        X509Certificate root = (X509Certificate) rootStore.getCertificate("root");
        X509Certificate intermediate = (X509Certificate) intermediateStore.getCertificate("intermediate");
        X509Certificate server = (X509Certificate) serverStore.getCertificate("server");
        X509Certificate otherRoot = (X509Certificate) otherRootStore.getCertificate("otherroot");

        section1Chain(root, intermediate, server);
        section2VerifyEachLink(root, intermediate, server);
        section3PkixValidation(root, intermediate, server, otherRoot);
        section4TlsHandshake(dir, pass, root);
        section5Summary();

        System.out.println();
        System.out.println("自检：通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed > 0) {
            System.out.println("有自检失败，退出码 1。");
            System.exit(1);
        }
    }

    // ==================================================================
    // 1. 链上有谁
    // ==================================================================
    private static void section1Chain(X509Certificate root, X509Certificate intermediate, X509Certificate server) {
        title(1, "证书链：三层，一层比一层短命");
        System.out.println("keytool 生成了 3 张证书，私钥分别放在各自的 PKCS12 里。");
        System.out.println();
        printCert("根证书 root", root);
        printCert("中间 CA  intermediate", intermediate);
        printCert("服务器证书 server", server);

        System.out.println();
        System.out.println("链：localhost（服务器证书）  <-中间 CA 签发-  Alg4 Demo Intermediate CA  <-根签发-  Alg4 Demo Root CA（自签）");
        System.out.println("   第一层的\"签发者\"= 第二层的\"主体\"，这就是它能拼成链的原因。");
        System.out.println("   根的私钥对应信任锚；中间 CA 的存在是为了根私钥能离线——");
        System.out.println("   日常签发用中间 CA 的私钥，它泄露了可以吊销换新，根不动。");

        check(server.getIssuerX500Principal().equals(intermediate.getSubjectX500Principal()),
                "server 的 issuer == intermediate 的 subject");
        check(intermediate.getIssuerX500Principal().equals(root.getSubjectX500Principal()),
                "intermediate 的 issuer == root 的 subject");
        check(root.getIssuerX500Principal().equals(root.getSubjectX500Principal()),
                "root 自签：issuer == subject");
        check(intermediate.getBasicConstraints() == 0,
                "intermediate 是 CA (=true)，且 pathLen=0（只能签终端证书，不能再签 CA）");
        check(server.getBasicConstraints() == -1, "server 是终端证书（CA:false）");
        check(root.getNotAfter().after(intermediate.getNotAfter())
                        || root.getNotAfter().equals(intermediate.getNotAfter()),
                "根的到期时间不早于中间 CA（根活得最久）");
    }

    // ==================================================================
    // 2. 逐级验签
    // ==================================================================
    private static void section2VerifyEachLink(X509Certificate root, X509Certificate intermediate,
                                               X509Certificate server) {
        title(2, "逐级验签：每一环都用上一环的公钥验");
        check(verifies(server, intermediate.getPublicKey()), "server 证书 -> 用 intermediate 公钥验签通过");
        check(verifies(intermediate, root.getPublicKey()), "intermediate 证书 -> 用 root 公钥验签通过");
        check(verifies(root, root.getPublicKey()), "root 证书 -> 用自己的公钥验签通过（自签，自己给自己背书）");
        check(!verifies(server, root.getPublicKey()), "server 证书 -> 直接用 root 公钥验，拒绝（中间的签名对不上根的公钥）");
        System.out.println();
        System.out.println("注意最后一行的含义：签名验证不做\"信任判断\"，它只回答\"是不是这把私钥签的\"。");
        System.out.println("\"要不要信\"是链拼接 + 信任锚 + 各项策略检查（有效期/用途/主机名）合起来决定的。");
    }

    // ==================================================================
    // 3. PKIX 校验与信任锚
    // ==================================================================
    private static void section3PkixValidation(X509Certificate root, X509Certificate intermediate,
                                               X509Certificate server, X509Certificate otherRoot) {
        title(3, "PKIX 校验：把\"信任锚\"换掉，结论就变");
        List<X509Certificate> chain = Arrays.asList(server, intermediate);

        boolean trusted = pkixValid(chain, root);
        check(trusted, "信任锚 = 我们的 root -> 校验通过");
        System.out.println("  校验路径：" + (trusted ? "server -> intermediate -> root(锚)" : rejectReason));

        boolean untrusted = pkixValid(chain, otherRoot);
        check(!untrusted, "信任锚换个不相干的根 -> 校验失败");
        System.out.println("  失败原因：" + abbreviate(rejectReason, 160));
        System.out.println();
        System.out.println("同一条链、同样的签名，信任锚不同结论就相反：所谓\"可信\"，");
        System.out.println("从来不是证书自身的属性，而是\"它能不能连到我已经信任的那张根证书上\"。");
    }

    // ==================================================================
    // 4. 真 TLS 握手
    // ==================================================================
    private static void section4TlsHandshake(Path dir, char[] pass, X509Certificate root) throws Exception {
        title(4, "真 TLS 握手：让 JDK 的默认校验器来判卷");

        System.out.println("做法：本地起一个 SSLServerSocket（用 keytool 生成的私钥与证书链），");
        System.out.println("客户端只信任我们的 root，并且打开 HTTPS 式的主机名校验");
        System.out.println("（SSLParameters.setEndpointIdentificationAlgorithm(\"HTTPS\")）。");
        System.out.println("失败原因从握手异常的 cause 链里抠出来，连 JDK 自己的原话一起打印。");
        System.out.println();

        // 4.1 正常
        String result = handshake(dir.resolve("server.p12"), pass, root, "localhost");
        check(result == null, "【场景 1】证书链完整 + SAN 有 localhost -> 握手成功");
        System.out.println("  结果：" + (result == null ? "握手成功，之后可以开始传数据" : result));

        // 4.2 主机名不匹配
        result = handshake(dir.resolve("server-mismatch.p12"), pass, root, "localhost");
        check(result != null && mentions(result, "alternative", "matching", "hostname", "名字"),
                "【场景 2】证书 SAN 是 shop.example.com，却去连 localhost -> 主机名不匹配，拒绝");
        System.out.println("  结果：" + abbreviate(result, 200));

        // 4.3 过期
        result = handshake(dir.resolve("server-expired.p12"), pass, root, "localhost");
        check(result != null && mentions(result, "expired", "validity", "过期", "有效期"),
                "【场景 3】服务器证书已过期 -> 拒绝");
        System.out.println("  结果：" + abbreviate(result, 200));

        // 4.4 另一家 CA 签的
        result = handshake(dir.resolve("server-otherca.p12"), pass, root, "localhost");
        check(result != null && mentions(result, "path", "certification", "trust"),
                "【场景 4】服务器证书由另一家根 CA 签发，客户端不信任 -> 找不到信任路径，拒绝");
        System.out.println("  结果：" + abbreviate(result, 200));

        // 4.5 只有 CN 没有 SAN：不预设立场，实测
        result = handshake(dir.resolve("server-cnonly.p12"), pass, root, "localhost");
        System.out.println();
        System.out.println("【场景 5】证书只写了 CN=localhost、没有 SAN（不预设结论，实测 JDK 的行为）：");
        if (result == null) {
            System.out.println("  结果：握手成功 —— JDK 的 HTTPS 校验在 SAN 缺失时仍回退到 CN（RFC 2818 的老规矩）。");
            System.out.println("  但这不代表可以依赖它：浏览器对公开 CA 的证书只认 SAN，CA/B Forum 2017 年起");
            System.out.println("  就不再签发\"只有 CN\"的证书。自己给自己签测试证书时，务必带上 SAN。");
        } else {
            System.out.println("  结果：拒绝 —— 这个 JDK 版本不再回退到 CN。");
            System.out.println("  原因：" + abbreviate(result, 200));
        }

        System.out.println();
        System.out.println("这 5 个场景全部由客户端默认校验器自动完成，应用代码只是连了一下：");
        System.out.println("链拼接、签名、有效期、CA 资格、用途、主机名，六道里错了任何一道都握手失败。");
    }

    // ==================================================================
    // 5. 小结
    // ==================================================================
    private static void section5Summary() {
        title(5, "小结：证书在 TLS 里干了什么");
        System.out.println("1) 服务器把 证书链（不是公钥）发给客户端；");
        System.out.println("2) 客户端用预置的根证书验证这条链 -> 确认\"这个公钥确实属于这个域名\"；");
        System.out.println("3) 用握手过程临时协商出的对称密钥保护后续数据；");
        System.out.println("4) 证书只在\"证明身份\"这一步用到，它不参与加密业务数据。");
        System.out.println();
        System.out.println("所以整个 HTTPS 的安全感，建立在两种东西上：");
        System.out.println("  - 数学：签名/密钥交换无法伪造；");
        System.out.println("  - 信任：那一百多张预置根证书背后的组织不会乱签。");
        System.out.println("后者是运营与审计问题，不是密码学问题——证书体系把这两件事强行绑在了一起。");
    }

    // ==================================================================
    // TLS 握手工具
    // ==================================================================

    /**
     * 起一个只接受一次握手的 TLS 服务器，用指定 keystore 当服务器身份，
     * 客户端只信任给定根证书并开启主机名校验。
     *
     * @return null 表示握手成功；否则返回失败原因（异常类 + 消息 + cause 链）
     */
    private static String handshake(Path serverStoreFile, char[] pass, X509Certificate trustedRoot,
                                    String host) throws Exception {
        KeyStore serverStore = load(serverStoreFile, pass);
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(serverStore, pass);

        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagers.getKeyManagers(), null, null);
        SSLServerSocket serverSocket =
                (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0);
        serverSocket.setSoTimeout(10_000);
        final int port = serverSocket.getLocalPort();
        final SSLServerSocket finalServerSocket = serverSocket;

        Thread serverThread = new Thread(new Runnable() {
            public void run() {
                try (SSLSocket socket = (SSLSocket) finalServerSocket.accept()) {
                    socket.setSoTimeout(10_000);
                    socket.startHandshake();
                } catch (Exception expectedWhenClientRejects) {
                    // 客户端拒绝时服务器这边同样会抛异常，忽略即可
                } finally {
                    try {
                        finalServerSocket.close();
                    } catch (Exception ignored) {
                        // 关不掉也不影响结论
                    }
                }
            }
        }, "tls-server");
        serverThread.setDaemon(true);
        serverThread.start();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("trusted-root", trustedRoot);
        TrustManagerFactory trustManagers =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init(trustStore);
        SSLContext clientContext = SSLContext.getInstance("TLS");
        clientContext.init(null, trustManagers.getTrustManagers(), null);

        try {
            SSLSocket socket = (SSLSocket) clientContext.getSocketFactory().createSocket(host, port);
            socket.setSoTimeout(10_000);
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parameters);
            try {
                socket.startHandshake();
                return null;
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            return describe(e);
        } finally {
            serverThread.join(2000);
        }
    }

    /** 把异常本身 + cause 链压成一行，方便判断到底是哪一道检查failed。 */
    private static String describe(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 5) {
            if (sb.length() > 0) {
                sb.append("  <-  ");
            }
            sb.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                sb.append(": ").append(current.getMessage());
            }
            if (current instanceof SSLHandshakeException && current.getCause() == null) {
                break;
            }
            current = current.getCause();
            depth++;
        }
        return sb.toString();
    }

    private static boolean mentions(String text, String... keywords) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    // 通用工具
    // ==================================================================

    private static KeyStore load(Path file, char[] pass) throws Exception {
        if (!Files.exists(file)) {
            throw new IllegalStateException("缺少 " + file.toAbsolutePath()
                    + "，请先运行 scripts\\run-digital-certificate-demo.ps1");
        }
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(file)) {
            store.load(in, pass);
        }
        return store;
    }

    private static boolean verifies(X509Certificate cert, java.security.PublicKey issuerKey) {
        try {
            cert.verify(issuerKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean pkixValid(List<X509Certificate> chain, X509Certificate anchor) {
        try {
            CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(chain);
            PKIXParameters parameters =
                    new PKIXParameters(Collections.singleton(new TrustAnchor(anchor, null)));
            parameters.setRevocationEnabled(false);
            CertPathValidator.getInstance("PKIX").validate(path, parameters);
            rejectReason = "(通过)";
            return true;
        } catch (Exception e) {
            rejectReason = describe(e);
            return false;
        }
    }

    private static void printCert(String role, X509Certificate cert) {
        System.out.println("  " + role);
        System.out.println("    subject   : " + cert.getSubjectX500Principal().getName());
        System.out.println("    issuer    : " + cert.getIssuerX500Principal().getName());
        System.out.println("    serial    : 0x" + cert.getSerialNumber().toString(16));
        System.out.println("    有效期    : " + fmt(cert.getNotBefore()) + " ~ " + fmt(cert.getNotAfter()));
        System.out.println("    签名算法  : " + cert.getSigAlgName());
        System.out.println("    CA        : " + (cert.getBasicConstraints() >= 0
                ? "true（pathLen=" + (cert.getBasicConstraints() == Integer.MAX_VALUE
                ? "无限制" : cert.getBasicConstraints()) + "）"
                : "false（终端证书）"));
        System.out.println("    keyUsage  : " + keyUsageText(cert.getKeyUsage()));
        System.out.println("    SAN       : " + sanText(cert));
        System.out.println("    SHA-256   : " + fingerprint(cert));
        System.out.println();
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

    private static String sanText(X509Certificate cert) {
        try {
            java.util.Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans == null) {
                return "(无 SAN，只有 CN)";
            }
            StringBuilder sb = new StringBuilder();
            for (List<?> san : sans) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                int type = (Integer) san.get(0);
                sb.append(type == 2 ? "DNS:" : type == 7 ? "IP:" : "[" + type + "]:");
                sb.append(san.get(1));
            }
            return sb.toString();
        } catch (Exception e) {
            return "(解析失败)";
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

    private static String fmt(Date date) {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return format.format(date) + " UTC";
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "(无)";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
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
