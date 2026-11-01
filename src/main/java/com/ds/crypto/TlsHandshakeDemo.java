package com.ds.crypto;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 数字证书在真实项目里是怎么"工作"的：把 TLS 握手拆开看，再演示两种生产环境最常见的用法。
 *
 * <p>全部是本机回环连接，不访问外网：
 * <ol>
 *   <li>一次完整握手：客户端拿到证书链、逐张验到信任的根，然后拿到会话密钥；
 *       配合 {@code -Djavax.net.debug=ssl:handshake} 能看真实的 ClientHello / ServerHello /
 *       Certificate / CertificateVerify / Finished 报文；</li>
 *   <li>双向认证 mTLS：服务端要求客户端也出示证书（服务间调用、K8s、数据库强制客户端证书用这套）；</li>
 *   <li>SNI 选证书：一个 IP 一个端口后面挂两个站点，按客户端在 ClientHello 里带的主机名选证书。</li>
 * </ol>
 *
 * <p>证书由 {@code scripts/run-tls-handshake-demo.ps1} 用 keytool 生成到 {@code target/tls-demo}。
 *
 * <p>运行：
 * <pre>
 *   java -cp target\tls-classes com.ds.crypto.TlsHandshakeDemo target\tls-demo
 *   java -Djavax.net.debug=ssl:handshake -cp target\tls-classes com.ds.crypto.TlsHandshakeDemo target\tls-demo trace
 * </pre>
 */
public final class TlsHandshakeDemo {

    private static final char[] PASS = "changeit".toCharArray();
    private static int passed;
    private static int failed;

    private TlsHandshakeDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "target/tls-demo");
        boolean trace = args.length > 1 && "trace".equals(args[1]);
        System.out.println("JDK 版本: " + System.getProperty("java.version"));
        System.out.println("证书目录: " + dir.toAbsolutePath());
        System.out.println();

        section1Handshake(dir, trace);
        if (!trace) {
            section2MutualTls(dir);
            section3Sni(dir);
            section4Summary();
        }

        System.out.println();
        System.out.println("自检：通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ==================================================================
    // 1. 一次完整握手
    // ==================================================================
    private static void section1Handshake(Path dir, boolean trace) throws Exception {
        title(1, "一次完整握手：证书只在握手里出现一次，却决定了后面所有数据发给谁");

        X509Certificate root = cert(load(dir.resolve("root.p12")), "root");
        KeyStore serverStore = load(dir.resolve("a.p12"));
        KeyStore trust = trustStore(root);

        List<String> trustLog = new ArrayList<String>();
        SSLContext client = clientContext(trust, null, trustLog);

        OneShotServer server = new OneShotServer(serverStore, null, false, null);
        HandshakeResult clientSide = connect(client, "127.0.0.1", server.port(), "a.example.com");
        server.await();

        System.out.println("服务端看到的：");
        System.out.println("  协议 / 套件 : " + server.result.protocol + " / " + server.result.cipher);
        System.out.println("客户端看到的：");
        System.out.println("  协议 / 套件 : " + clientSide.protocol + " / " + clientSide.cipher);
        System.out.println("  服务端证书  : " + subjectOf(clientSide.peer));
        System.out.println("  证书 SAN    : " + sanText(clientSide.peer));
        System.out.println("  JDK 实际校验过的链：");
        for (String line : trustLog) {
            System.out.println("    " + line);
        }
        check(clientSide.error == null, "握手成功：证书链能验到预置的根");
        check(clientSide.protocol != null && clientSide.protocol.startsWith("TLSv1.3"),
                "实际协商的是 TLS 1.3");
        check(clientSide.peer != null
                        && clientSide.peer.getSubjectX500Principal().getName().contains("a.example.com"),
                "客户端拿到的是 a.example.com 的证书");
        check(server.result.error == null, "服务端这一侧也完成了握手");

        if (!trace) {
            // 反面：客户端信任的是另一家根，链到这家中断（trace 模式只抓一次干净握手，跳过反面）
            KeyStore otherRootStore = load(dir.resolve("otherroot.p12"));
            KeyStore wrongTrust = trustStore(cert(otherRootStore, "otherroot"));
            OneShotServer server2 = new OneShotServer(serverStore, null, false, null);
            HandshakeResult untrusted = connect(clientContext(wrongTrust, null, new ArrayList<String>()),
                    "127.0.0.1", server2.port(), "a.example.com");
            server2.await();
            System.out.println();
            System.out.println("客户端信任的是另一家根时：");
            System.out.println("  " + abbreviate(describe(untrusted.error), 170));
            check(untrusted.error != null, "拒绝连接：找不到通往信任锚的路径（PKIX path building failed）");

        }

        if (!trace) {
            System.out.println();
            System.out.println("握手过程在线上长这样（TLS 1.3，简化）：");
            System.out.println("  1 ClientHello        客户端：支持的版本/套件/签名算法，我要访问 a.example.com（SNI）");
            System.out.println("  2 ServerHello        服务端：选定的套件、密钥交换参数");
            System.out.println("  3 {EncryptedExtensions} 从这里开始握手消息全部加密");
            System.out.println("  4 Certificate        服务端发证书链（叶子 + 中间，不含根）");
            System.out.println("  5 CertificateVerify  服务端用证书私钥签一段握手摘要 —— 证明私钥在手");
            System.out.println("  6 Finished           双方确认握手内容没被改过");
            System.out.println("之后才是 HTTP 请求，由握手协商出来的对称密钥保护。");
        }
    }

    // ==================================================================
    // 2. 双向认证 mTLS
    // ==================================================================
    private static void section2MutualTls(Path dir) throws Exception {
        title(2, "双向认证 mTLS：不只客户端验服务器，服务器也验客户端");

        X509Certificate root = cert(load(dir.resolve("root.p12")), "root");
        KeyStore serverStore = load(dir.resolve("a.p12"));
        KeyStore clientStore = load(dir.resolve("client.p12"));
        KeyStore trust = trustStore(root);
        System.out.println("场景：服务端在握手时要求客户端出示证书（setNeedClientAuth）。");
        System.out.println("服务间调用、Kubernetes mTLS、数据库\"强制客户端证书\"用的都是这一套。");
        System.out.println();

        String subject = runMutualTls(serverStore, clientStore, trust, true);
        System.out.println("客户端带证书：" + subject);
        check(subject != null && subject.contains("alice"), "服务端拿到了客户端证书 CN=alice，握手完成");

        String rejected = runMutualTls(serverStore, null, trust, false);
        System.out.println("客户端不带证书：" + abbreviate(rejected, 140));
        check(rejected != null && rejected.toLowerCase().contains("certificate"),
                "服务端拒绝对不带证书的客户端完成握手");
        System.out.println();
        System.out.println("要点：mTLS 里双方各有一张证书，各自验对方的链。它回答\"谁能连进来\"；");
        System.out.println("      普通 HTTPS 只回答\"我连的是不是真的你\"。");
    }

    private static String runMutualTls(KeyStore serverStore, KeyStore clientStore,
                                       KeyStore trust, boolean expectSuccess) throws Exception {
        OneShotServer server = new OneShotServer(serverStore, trust, true, null);
        HandshakeResult client = connect(clientContext(trust, clientStore, new ArrayList<String>()),
                "127.0.0.1", server.port(), "a.example.com");
        server.await();

        if (server.result.error == null && client.error == null) {
            return "服务端看到客户端证书：" + subjectOf(server.result.peer);
        }
        String reason = server.result.error != null ? describe(server.result.error) : describe(client.error);
        if (expectSuccess) {
            throw new IllegalStateException("带证书的客户端本应握手成功，实际失败：" + reason);
        }
        return reason;
    }

    // ==================================================================
    // 3. SNI 选证书
    // ==================================================================
    private static void section3Sni(Path dir) throws Exception {
        title(3, "SNI：一个 IP、一个端口，按域名选不同的证书");

        X509Certificate root = cert(load(dir.resolve("root.p12")), "root");
        KeyStore sideA = load(dir.resolve("a.p12"));
        KeyStore sideB = load(dir.resolve("b.p12"));
        KeyStore trust = trustStore(root);
        System.out.println("场景：服务器只有一个 IP:端口，但托管 a.example.com 和 b.example.com 两个站点，各有一张证书。");
        System.out.println("客户端在 ClientHello 里带 SNI 主机名，服务端据此选证书——Nginx/Caddy 的虚拟主机就是这么做的。");
        System.out.println();

        for (final String host : new String[]{"a.example.com", "b.example.com"}) {
            OneShotServer server = new OneShotServer(sideA, null, false, sniKeyManager(sideA, sideB));
            server.setSniMatcher(".*\\.example\\.com");
            HandshakeResult client = connect(clientContext(trust, null, new ArrayList<String>()),
                    "127.0.0.1", server.port(), host);
            server.await();
            String gotHost = shortName(subjectOf(client.peer));
            System.out.println("客户端说访问 " + host + " -> 服务端出示的证书是 " + gotHost
                    + "（SAN " + sanText(client.peer) + "）");
            check(client.error == null && client.peer != null
                            && client.peer.getSubjectX500Principal().getName().contains(host),
                    "SNI=" + host + " 时拿到的是 " + host + " 的证书");
        }
        System.out.println();
        System.out.println("一个 IP 只能配一个默认证书；能不能\"按域名给不同证书\"，取决于客户端有没有带 SNI。");
        System.out.println("老客户端不带 SNI，服务器只能返回默认证书——这就是早年\"一张证书只能配一个站\"的原因。");
    }

    // ==================================================================
    // 4. 小结
    // ==================================================================
    private static void section4Summary() {
        title(4, "小结：证书在项目运行时出现的三个位置");
        System.out.println("1) 握手时：服务端出示证书链，客户端逐张验到信任锚，再用证书里的公钥验证");
        System.out.println("   CertificateVerify 签名。这一步同时证明了\"公钥属于这个域名\"和\"对方有私钥\"。");
        System.out.println("2) 连接建立后：证书不再参与。业务数据全部用握手协商出的对称密钥加密，");
        System.out.println("   证书的开销只在握手（TLS 1.3 一个往返；会话复用可以更少）。");
        System.out.println("3) 业务层：证书还能干别的——代码签名（jarsigner）、成绩单/文档签名、");
        System.out.println("   JWT 签名、mTLS 的客户端身份。同一个 PKCS12 文件，用途由 KeyUsage / EKU 决定。");
        System.out.println();
        System.out.println("运维上真正会出事的，几乎都不是密码学：证书过期、链没发全、");
        System.out.println("域名不在 SAN 里、私钥泄露、轮换时忘了 reload。");
    }

    // ==================================================================
    // 回环 TLS 服务端
    // ==================================================================

    /** 只接受一次握手的服务端；记录协议/套件/对端证书或错误。 */
    private static final class OneShotServer implements Closeable {
        private final SSLServerSocket socket;
        private final Thread thread;
        private final HandshakeResult result = new HandshakeResult();

        OneShotServer(KeyStore keyStore, KeyStore trust, boolean needClientAuth, KeyManager[] keyManagers)
                throws Exception {
            SSLContext context = serverContext(keyStore, trust, needClientAuth, keyManagers);
            socket = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(0);
            socket.setSoTimeout(15_000);
            if (needClientAuth) {
                socket.setNeedClientAuth(true);
            }
            thread = new Thread(new Runnable() {
                public void run() {
                    try (SSLSocket accepted = (SSLSocket) socket.accept()) {
                        accepted.setSoTimeout(15_000);
                        accepted.startHandshake();
                        SSLSession session = accepted.getSession();
                        result.protocol = session.getProtocol();
                        result.cipher = session.getCipherSuite();
                        try {
                            result.peer = (X509Certificate) session.getPeerCertificates()[0];
                        } catch (Exception ignored) {
                            // 没有要求客户端证书时没有对端证书
                        }
                    } catch (Exception e) {
                        result.error = e;
                    } finally {
                        closeQuietly(socket);
                    }
                }
            }, "tls-server");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return socket.getLocalPort();
        }

        void setSniMatcher(String pattern) throws Exception {
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setSNIMatchers(Collections.<SNIMatcher>singletonList(SNIHostName.createSNIMatcher(pattern)));
            socket.setSSLParameters(parameters);
        }

        void await() {
            try {
                thread.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void close() {
            closeQuietly(socket);
        }
    }

    // ==================================================================
    // 工具
    // ==================================================================

    private static final class HandshakeResult {
        String protocol;
        String cipher;
        X509Certificate peer;
        Exception error;
    }

    private static HandshakeResult connect(SSLContext context, String host, int port, String sniHost) {
        HandshakeResult result = new HandshakeResult();
        try {
            SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(host, port);
            socket.setSoTimeout(10_000);
            SSLParameters parameters = socket.getSSLParameters();
            List<SNIServerName> names = new ArrayList<SNIServerName>();
            names.add(new SNIHostName(sniHost));
            parameters.setServerNames(names);
            socket.setSSLParameters(parameters);
            try {
                socket.startHandshake();
                result.protocol = socket.getSession().getProtocol();
                result.cipher = socket.getSession().getCipherSuite();
                result.peer = (X509Certificate) socket.getSession().getPeerCertificates()[0];
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            result.error = e;
        }
        return result;
    }

    private static KeyStore load(Path file) throws Exception {
        if (!Files.exists(file)) {
            throw new IllegalStateException("缺少 " + file.toAbsolutePath()
                    + "，请先运行 scripts\\run-tls-handshake-demo.ps1");
        }
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(file)) {
            store.load(in, PASS);
        }
        return store;
    }

    private static X509Certificate cert(KeyStore store, String alias) throws Exception {
        return (X509Certificate) store.getCertificate(alias);
    }

    private static KeyStore trustStore(X509Certificate... certificates) throws Exception {
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null, null);
        for (int i = 0; i < certificates.length; i++) {
            store.setCertificateEntry("trusted-" + i, certificates[i]);
        }
        return store;
    }

    private static SSLContext serverContext(KeyStore keyStore, KeyStore trust, boolean needClientAuth,
                                            KeyManager[] keyManagers) throws Exception {
        KeyManager[] managers = keyManagers;
        if (managers == null) {
            KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            factory.init(keyStore, PASS);
            managers = factory.getKeyManagers();
        }
        TrustManager[] trustManagers = null;
        if (trust != null) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trust);
            trustManagers = tmf.getTrustManagers();
        }
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(managers, trustManagers, null);
        return context;
    }

    private static SSLContext clientContext(KeyStore trust, KeyStore keyStore, final List<String> log)
            throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trust);
        TrustManager[] managers = factory.getTrustManagers();
        for (int i = 0; i < managers.length; i++) {
            if (managers[i] instanceof X509TrustManager) {
                managers[i] = new LoggingTrustManager((X509TrustManager) managers[i], log);
            }
        }
        KeyManager[] keyManagers = null;
        if (keyStore != null) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, PASS);
            keyManagers = kmf.getKeyManagers();
        }
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, managers, null);
        return context;
    }

    /** 包一层 JDK 的 X509TrustManager：打印它实际收到的链，再交给它做真正的 PKIX 校验。 */
    private static final class LoggingTrustManager implements X509TrustManager {
        private final X509TrustManager delegate;
        private final List<String> log;

        LoggingTrustManager(X509TrustManager delegate, List<String> log) {
            this.delegate = delegate;
            this.log = log;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            log.add("共 " + chain.length + " 张：");
            for (X509Certificate certificate : chain) {
                log.add("  " + certificate.getSubjectX500Principal().getName()
                        + "（由 " + shortName(certificate.getIssuerX500Principal().getName()) + " 签发）");
            }
            delegate.checkServerTrusted(chain, authType);
            log.add("  -> PKIX 校验通过：签名、有效期、CA 资格、链到信任锚");
        }

        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }

    /** 按 ClientHello 里的 SNI 选证书；Nginx/Caddy 在底层做的是同一件事。 */
    private static KeyManager[] sniKeyManager(final KeyStore sideA, final KeyStore sideB) {
        X509ExtendedKeyManager manager = new X509ExtendedKeyManager() {
            public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
                if (socket instanceof SSLSocket) {
                    SSLSession handshake = ((SSLSocket) socket).getHandshakeSession();
                    if (handshake instanceof ExtendedSSLSession) {
                        for (SNIServerName name : ((ExtendedSSLSession) handshake).getRequestedServerNames()) {
                            if (name instanceof SNIHostName
                                    && "b.example.com".equalsIgnoreCase(((SNIHostName) name).getAsciiName())) {
                                return "b";
                            }
                        }
                    }
                }
                return "a";
            }

            public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
                return "a";
            }

            public X509Certificate[] getCertificateChain(String alias) {
                try {
                    Certificate[] certificates = store(alias).getCertificateChain("server");
                    X509Certificate[] out = new X509Certificate[certificates.length];
                    for (int i = 0; i < certificates.length; i++) {
                        out[i] = (X509Certificate) certificates[i];
                    }
                    return out;
                } catch (Exception e) {
                    return null;
                }
            }

            public PrivateKey getPrivateKey(String alias) {
                try {
                    return (PrivateKey) store(alias).getKey("server", PASS);
                } catch (Exception e) {
                    return null;
                }
            }

            public String[] getClientAliases(String keyType, Principal[] issuers) {
                return null;
            }

            public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
                return null;
            }

            public String[] getServerAliases(String keyType, Principal[] issuers) {
                return new String[]{"a", "b"};
            }

            private KeyStore store(String alias) {
                return "b".equals(alias) ? sideB : sideA;
            }
        };
        return new KeyManager[]{manager};
    }

    private static String subjectOf(X509Certificate certificate) {
        return certificate == null ? "(无)" : certificate.getSubjectX500Principal().getName();
    }

    private static String sanText(X509Certificate certificate) {
        if (certificate == null) {
            return "(无)";
        }
        try {
            Collection<List<?>> sans = certificate.getSubjectAlternativeNames();
            if (sans == null) {
                return "(无 SAN)";
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

    private static String shortName(String dn) {
        for (String part : dn.split(",")) {
            if (part.trim().startsWith("CN=")) {
                return part.trim().substring(3);
            }
        }
        return dn;
    }

    private static String describe(Throwable throwable) {
        if (throwable == null) {
            return "(无异常)";
        }
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 4) {
            if (sb.length() > 0) {
                sb.append(" <- ");
            }
            sb.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                sb.append(": ").append(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }
        return sb.toString();
    }

    private static String abbreviate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
            // 忽略
        }
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




