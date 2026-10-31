/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.sign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.KeyFactory;
import java.util.Base64;

/**
 * 成绩单签名者：持有签名私钥，负责"签"和"验"。
 *
 * <p>密钥来源：application.yml 里的 PKCS12 密钥库（推荐生产用 KMS/HSM 或加密密钥库）；
 * 文件不存在且 generate-if-missing=true 时生成临时 RSA 密钥（仅开发环境，重启后历史签名失效）。
 *
 * <p>算法：JDK 11+ 用 RSASSA-PSS（签名方案首选），JDK 8 自动回退 SHA256withRSA。
 */
@Component
public class TranscriptSigner {

    private static final Logger log = LoggerFactory.getLogger(TranscriptSigner.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final PSSParameterSpec PSS_SHA256 = new PSSParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);

    private final TranscriptSigningProperties properties;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String certificateFingerprint;
    private String certificateSerial;
    private String algorithm = "未启用";

    @Autowired
    public TranscriptSigner(TranscriptSigningProperties properties) {
        this.properties = properties;
    }

    /** 测试用：直接注入密钥对，不读密钥库。 */
    TranscriptSigner(PrivateKey privateKey, PublicKey publicKey) {
        this.properties = null;
        initKeys(privateKey, publicKey, null, null);
    }

    @PostConstruct
    void init() {
        if (properties == null || !properties.isEnabled()) {
            log.warn("成绩单签名未启用（app.transcript-signing.enabled=false）");
            return;
        }
        Path path = Paths.get(properties.getKeystore()).toAbsolutePath().normalize();
        try {
            if (Files.exists(path)) {
                loadFromKeystore(path);
            } else if (properties.isGenerateIfMissing()) {
                generateEphemeral();
            } else {
                throw new IllegalStateException("签名密钥库不存在：" + path
                        + "（请用 scripts/gen-transcript-signing-keystore.ps1 生成，或设 generate-if-missing=true 仅开发使用）");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("初始化成绩单签名密钥失败：" + path, e);
        }
    }

    private void loadFromKeystore(Path path) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(path)) {
            keyStore.load(in, properties.getStorePassword().toCharArray());
        }
        Key key = keyStore.getKey(properties.getAlias(), properties.getStorePassword().toCharArray());
        if (!(key instanceof PrivateKey)) {
            throw new IllegalStateException("密钥别名 " + properties.getAlias() + " 不是私钥：" + path);
        }
        Certificate certificate = keyStore.getCertificate(properties.getAlias());
        PublicKey pub;
        String fingerprint = null;
        String serial = null;
        if (certificate != null) {
            pub = certificate.getPublicKey();
            fingerprint = fingerprint(certificate.getEncoded());
            if (certificate instanceof X509Certificate) {
                serial = ((X509Certificate) certificate).getSerialNumber().toString(16);
            }
        } else {
            pub = derivePublicKey((PrivateKey) key);
        }
        initKeys((PrivateKey) key, pub, fingerprint, serial);
        log.info("成绩单签名已启用：算法={}，证书指纹={}，序列号={}", algorithm,
                certificateFingerprint, certificateSerial);
    }

    private void generateEphemeral() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(properties.getKeySize());
        KeyPair keyPair = generator.generateKeyPair();
        initKeys(keyPair.getPrivate(), keyPair.getPublic(), null, null);
        log.warn("未找到签名密钥库，已生成临时 RSA {} 位密钥（仅开发环境；重启后历史签名将无法验证）。"
                + "生产请配置 app.transcript-signing.keystore。", properties.getKeySize());
    }

    private void initKeys(PrivateKey priv, PublicKey pub, String fingerprint, String serial) {
        this.privateKey = priv;
        this.publicKey = pub;
        this.certificateFingerprint = fingerprint;
        this.certificateSerial = serial;
        this.algorithm = detectAlgorithm();
    }

    private static String detectAlgorithm() {
        try {
            Signature.getInstance("RSASSA-PSS");
            return "RSASSA-PSS";
        } catch (GeneralSecurityException e) {
            return "SHA256withRSA";
        }
    }

    private static PublicKey derivePublicKey(PrivateKey privateKey) throws Exception {
        if (!(privateKey instanceof RSAPrivateCrtKey)) {
            throw new IllegalStateException("无证书时仅支持 RSA 私钥推导公钥");
        }
        RSAPrivateCrtKey rsa = (RSAPrivateCrtKey) privateKey;
        return KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(rsa.getModulus(), rsa.getPublicExponent()));
    }

    /** 是否可用（启用且密钥已加载） */
    public boolean isEnabled() {
        return privateKey != null && publicKey != null;
    }

    /** 签名算法名（展示用） */
    public String algorithm() {
        return algorithm;
    }

    /** 证书指纹（无证书时说明为临时密钥） */
    public String certificateFingerprint() {
        return certificateFingerprint == null ? "开发环境临时密钥（未配置证书）" : certificateFingerprint;
    }

    /** 证书序列号（无证书时为 null） */
    public String certificateSerial() {
        return certificateSerial;
    }

    /** 公钥（X.509 编码的 Base64），供验证端/接口输出 */
    public String publicKeyBase64() {
        return publicKey == null ? null : Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /** 对字节签名 */
    public byte[] sign(byte[] data) throws GeneralSecurityException {
        if (!isEnabled()) {
            throw new IllegalStateException("成绩单签名未启用");
        }
        Signature signature = Signature.getInstance(algorithm);
        if ("RSASSA-PSS".equals(algorithm)) {
            signature.setParameter(PSS_SHA256);
        }
        signature.initSign(privateKey, RANDOM);
        signature.update(data);
        return signature.sign();
    }

    /** 验签；任何异常都按"不通过"处理（调用方只需布尔结果） */
    public boolean verify(byte[] data, byte[] signatureBytes) {
        if (!isEnabled()) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(algorithm);
            if ("RSASSA-PSS".equals(algorithm)) {
                signature.setParameter(PSS_SHA256);
            }
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static String fingerprint(byte[] encodedCertificate) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(encodedCertificate);
            byte[] head = new byte[16];
            System.arraycopy(digest, 0, head, 0, head.length);
            StringBuilder hex = new StringBuilder(32);
            for (byte b : head) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            return null;
        }
    }
}
