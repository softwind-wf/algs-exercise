package com.ds.crypto;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把 CA / Let's Encrypt 交付的 PEM 证书链与私钥，打包成 Spring Boot 能直接读的 PKCS12。
 *
 * <p>为什么需要它：Spring Boot 2.7（本项目）只认 keystore（PKCS12/JKS），不认 PEM；
 * 而 certbot 交付的是 {@code fullchain.pem} + {@code privkey.pem}。Linux 服务器上用
 * {@code openssl pkcs12 -export} 一条命令即可，但 Windows 上未必有 openssl，
 * 本工具用纯 JDK 完成同样的事，并把"链序不对""私钥与证书不配对"这类常见错误当场报出来。
 *
 * <p>支持：PKCS#8 私钥（{@code BEGIN PRIVATE KEY}）、PKCS#1 RSA 私钥
 * （{@code BEGIN RSA PRIVATE KEY}，自动包成 PKCS#8）；证书链任意顺序，自动按
 * "issuer == 上一张的 subject" 排成 leaf → intermediate → root。
 *
 * <p>用法：
 * <pre>
 *   java -cp target\cert-classes com.ds.crypto.PemToPkcs12 ^
 *        fullchain.pem privkey.pem university.p12 changeit university
 * </pre>
 *
 * <p>参数：证书链 PEM、私钥 PEM（可与证书同一个文件）、输出 p12、口令、别名（可选，默认 university）。
 */
public final class PemToPkcs12 {

    private static final Pattern PEM_BLOCK = Pattern.compile(
            "-----BEGIN ([A-Z0-9 ]+)-----([\\s\\S]*?)-----END \\1-----");

    private PemToPkcs12() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("用法: PemToPkcs12 <fullchain.pem> <privkey.pem> <out.p12> <password> [alias]");
            System.out.println("示例: PemToPkcs12 fullchain.pem privkey.pem university.p12 changeit university");
            System.exit(2);
        }
        String chainFile = args[0];
        String keyFile = args[1];
        String outFile = args[2];
        char[] password = args[3].toCharArray();
        String alias = args.length > 4 ? args[4] : "university";

        List<X509Certificate> certificates = readCertificates(chainFile);
        PrivateKey privateKey = readPrivateKey(keyFile);
        System.out.println("证书文件  : " + chainFile + "（" + certificates.size() + " 张证书）");
        System.out.println("私钥文件  : " + keyFile + "（" + privateKey.getAlgorithm() + " "
                + (privateKey.getEncoded() == null ? "?" : privateKey.getEncoded().length) + " 字节）");

        List<X509Certificate> ordered = orderChain(certificates, privateKey);
        X509Certificate leaf = ordered.get(0);
        System.out.println();
        System.out.println("叶子证书  : " + leaf.getSubjectX500Principal().getName());
        System.out.println("签发者    : " + leaf.getIssuerX500Principal().getName());
        System.out.println("有效期    : " + leaf.getNotBefore() + " ~ " + leaf.getNotAfter());
        System.out.println("SAN       : " + sanText(leaf));
        System.out.println("链        : " + chainText(ordered));

        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry(alias, privateKey, password, ordered.toArray(new Certificate[0]));
        try (java.io.OutputStream out = Files.newOutputStream(Paths.get(outFile))) {
            store.store(out, password);
        }
        System.out.println();
        System.out.println("已写出    : " + outFile + "（别名 " + alias + "，口令按命令行传入）");
        System.out.println("Spring Boot 配置：");
        System.out.println("  server.ssl.key-store=" + outFile);
        System.out.println("  server.ssl.key-store-password=******");
        System.out.println("  server.ssl.key-store-type=PKCS12");
        System.out.println("  server.ssl.key-alias=" + alias);
        System.out.println("自检：用 keytool -list -v -keystore " + outFile + " -storepass ****** 应看到"
                + " PrivateKeyEntry 与 Certificate chain length: " + ordered.size());
    }

    // ==================================================================
    // PEM 解析
    // ==================================================================

    private static final class Block {
        final String type;
        final byte[] der;

        Block(String type, byte[] der) {
            this.type = type;
            this.der = der;
        }
    }

    private static List<Block> readPemBlocks(String file) throws Exception {
        String text = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.US_ASCII);
        List<Block> blocks = new ArrayList<Block>();
        Matcher matcher = PEM_BLOCK.matcher(text);
        while (matcher.find()) {
            String type = matcher.group(1).trim();
            String body = matcher.group(2).replaceAll("\\s", "");
            blocks.add(new Block(type, Base64.getDecoder().decode(body)));
        }
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("没有在 " + file + " 里找到 PEM 块（应以 -----BEGIN ...----- 开头）");
        }
        return blocks;
    }

    private static List<X509Certificate> readCertificates(String file) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certificates = new ArrayList<X509Certificate>();
        for (Block block : readPemBlocks(file)) {
            if (!block.type.endsWith("CERTIFICATE")) {
                continue;   // 同一个文件里可能还夹着私钥，跳过
            }
            certificates.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(block.der)));
        }
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException(file + " 里没有 CERTIFICATE 块");
        }
        return certificates;
    }

    private static PrivateKey readPrivateKey(String file) throws Exception {
        for (Block block : readPemBlocks(file)) {
            String type = block.type;
            if (type.equals("PRIVATE KEY")) {
                return toPrivateKey(block.der, false);
            }
            if (type.equals("RSA PRIVATE KEY")) {
                return toPrivateKey(block.der, true);
            }
            if (type.equals("EC PRIVATE KEY")) {
                throw new IllegalArgumentException("私钥是 SEC1 格式（BEGIN EC PRIVATE KEY）。"
                        + "本工具只支持 PKCS#8 / PKCS#1；请用 openssl 转换成 PKCS#8："
                        + "openssl pkcs8 -topk8 -nocrypt -in privkey.pem -out privkey-pkcs8.pem");
            }
        }
        throw new IllegalArgumentException(file + " 里没有 PRIVATE KEY 块");
    }

    private static PrivateKey toPrivateKey(byte[] der, boolean pkcs1Rsa) throws Exception {
        byte[] pkcs8 = pkcs1Rsa ? wrapPkcs1RsaIntoPkcs8(der) : der;
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
        for (String algorithm : new String[]{"RSA", "EC", "Ed25519", "DSA"}) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec);
            } catch (Exception ignored) {
                // 换下一个算法再试
            }
        }
        throw new IllegalArgumentException("无法识别私钥算法（支持的 PKCS#8 算法：RSA / EC / Ed25519 / DSA）");
    }

    /** PKCS#8 PrivateKeyInfo ::= SEQUENCE { INTEGER 0, AlgorithmIdentifier(rsaEncryption,NULL), OCTET STRING(RSA私钥) } */
    private static byte[] wrapPkcs1RsaIntoPkcs8(byte[] pkcs1) {
        byte[] algorithm = Der.seq(Der.oid("1.2.840.113549.1.1.1"), Der.nul());
        return Der.seq(Der.integer(0), algorithm, Der.octetString(pkcs1));
    }

    // ==================================================================
    // 链排序与校验
    // ==================================================================

    /**
     * 找到与私钥配对的那张证书当叶子，再按 issuer == 上一张 subject 串成链。
     * 配不上就报错——这能提前发现"certbot 目录里拷错了 privkey"这类事故。
     */
    private static List<X509Certificate> orderChain(List<X509Certificate> certificates, PrivateKey privateKey) {
        X509Certificate leaf = null;
        for (X509Certificate certificate : certificates) {
            if (publicKeyMatches(certificate.getPublicKey(), privateKey)) {
                leaf = certificate;
                break;
            }
        }
        if (leaf == null) {
            throw new IllegalArgumentException("私钥与任何一张证书的公钥都不匹配："
                    + "是不是 fullchain.pem 与 privkey.pem 不是同一套？");
        }
        List<X509Certificate> ordered = new ArrayList<X509Certificate>();
        ordered.add(leaf);
        List<X509Certificate> rest = new ArrayList<X509Certificate>(certificates);
        rest.remove(leaf);
        while (!rest.isEmpty()) {
            X509Certificate last = ordered.get(ordered.size() - 1);
            X509Certificate next = null;
            for (X509Certificate candidate : rest) {
                if (candidate.getSubjectX500Principal().equals(last.getIssuerX500Principal())) {
                    next = candidate;
                    break;
                }
            }
            if (next == null) {
                break;   // 链不完整（比如漏了中间证书），剩下的原样保留
            }
            ordered.add(next);
            rest.remove(next);
        }
        ordered.addAll(rest);
        return ordered;
    }

    private static boolean publicKeyMatches(PublicKey publicKey, PrivateKey privateKey) {
        try {
            if (privateKey instanceof RSAPrivateCrtKey && publicKey instanceof java.security.interfaces.RSAPublicKey) {
                RSAPrivateCrtKey rsaPrivate = (RSAPrivateCrtKey) privateKey;
                java.security.interfaces.RSAPublicKey rsaPublic =
                        (java.security.interfaces.RSAPublicKey) publicKey;
                return rsaPrivate.getModulus().equals(rsaPublic.getModulus())
                        && rsaPrivate.getPublicExponent().equals(rsaPublic.getPublicExponent());
            }
            // 非 RSA：用签名做一次往返验证
            String algorithm = "EC".equals(privateKey.getAlgorithm()) ? "SHA256withECDSA" : "SHA256withRSA";
            byte[] data = "pem-to-pkcs12-self-check".getBytes(StandardCharsets.US_ASCII);
            java.security.Signature signer = java.security.Signature.getInstance(algorithm);
            signer.initSign(privateKey);
            signer.update(data);
            byte[] signature = signer.sign();
            java.security.Signature verifier = java.security.Signature.getInstance(algorithm);
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private static String chainText(List<X509Certificate> certificates) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < certificates.size(); i++) {
            X509Certificate certificate = certificates.get(i);
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(shortName(certificate.getSubjectX500Principal().getName()));
        }
        return sb.toString();
    }

    private static String shortName(String dn) {
        for (String part : dn.split(",")) {
            if (part.trim().startsWith("CN=")) {
                return part.trim().substring(3);
            }
        }
        return dn;
    }

    private static String sanText(X509Certificate certificate) {
        try {
            java.util.Collection<List<?>> sans = certificate.getSubjectAlternativeNames();
            if (sans == null) {
                return "(无 SAN，只有 CN —— 现代浏览器不认，务必确认 CA 是否签错)";
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
}
