package com.ds.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * 用 JDK 自带 JCE 演示 RSA 的加密/解密过程。
 *
 * <p>本类<b>不实现 RSA 数学</b>，只调用 {@link Cipher}、{@link KeyPairGenerator} 等 JDK 接口，
 * 展示实际使用中的四件事：
 * <ol>
 *   <li>生成 2048 位 RSA 密钥对；</li>
 *   <li>公钥加密、私钥解密（RSA-OAEP，SHA-256）；</li>
 *   <li>公钥不能解密（RSA 的单向性）；</li>
 *   <li>RSA 单次加密有长度上限，长消息要用 RSA + AES-GCM 混合加密。</li>
 * </ol>
 *
 * <p>运行：{@code java -Dfile.encoding=UTF-8 -cp target\rijndael-classes com.ds.crypto.RsaJdkDemo}
 */
public final class RsaJdkDemo {

    /** JCE 的 RSA-OAEP 转换名。中间的 ECB 是 JCE 的历史命名占位，RSA 并不是分组密码。 */
    private static final String RSA_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /** 显式指定 OAEP 的摘要与 MGF1 都使用 SHA-256，避免不同 JDK/提供者默认值不一致。 */
    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private static final SecureRandom RANDOM = new SecureRandom();

    private RsaJdkDemo() {
    }

    public static void main(String[] args) throws Exception {
        // ------------------------------------------------------------
        title(1, "生成 RSA 密钥对（JDK 自带 KeyPairGenerator）");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, RANDOM);
        long begin = System.nanoTime();
        KeyPair keyPair = generator.generateKeyPair();
        long elapsedMs = (System.nanoTime() - begin) / 1_000_000;

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        System.out.println("算法: RSA");
        System.out.println("生成耗时: " + elapsedMs + " ms");
        System.out.println("模数 n: " + publicKey.getModulus().bitLength() + " 位（"
                + publicKey.getModulus().bitLength() / 8 + " 字节）");
        System.out.println("公钥指数 e = " + publicKey.getPublicExponent());
        System.out.println("n 的前/后 16 字节: "
                + headTail(publicKey.getModulus().toByteArray(), 16));
        System.out.println("公钥 SHA-256 指纹: " + fingerprint(publicKey.getEncoded()));
        System.out.println("私钥指数 d: " + privateKey.getPrivateExponent().bitLength()
                + " 位（保密，不打印）");
        System.out.println("说明: 公钥 = (n, e)，私钥 = (n, d)；e 通常取 65537。");

        // ------------------------------------------------------------
        title(2, "用公钥加密一条短消息");
        String message = "RSA 加密演示：你好，世界！";
        byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);
        Cipher encryptCipher = Cipher.getInstance(RSA_OAEP);
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA256);
        byte[] ciphertext = encryptCipher.doFinal(plaintext);

        System.out.println("明文（" + plaintext.length + " 字节）: " + message);
        System.out.println("加密转换: " + RSA_OAEP);
        System.out.println("密文（" + ciphertext.length + " 字节，Base64）:");
        printWrapped(Base64.getEncoder().encodeToString(ciphertext), 76);

        // ------------------------------------------------------------
        title(3, "用私钥解密");
        Cipher decryptCipher = Cipher.getInstance(RSA_OAEP);
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SHA256);
        byte[] recovered = decryptCipher.doFinal(ciphertext);
        System.out.println("解密结果: " + new String(recovered, StandardCharsets.UTF_8));
        System.out.println("与原文一致: " + Arrays.equals(plaintext, recovered));

        // ------------------------------------------------------------
        title(4, "反过来：用公钥解密行不行？");
        try {
            Cipher wrong = Cipher.getInstance(RSA_OAEP);
            wrong.init(Cipher.DECRYPT_MODE, publicKey, OAEP_SHA256);
            wrong.doFinal(ciphertext);
            System.out.println("用公钥解密：意外成功了（不应该发生）");
        } catch (Exception e) {
            System.out.println("用公钥解密失败: " + e.getClass().getSimpleName()
                    + " —— 公钥只能加密，解密必须用私钥");
        }

        // ------------------------------------------------------------
        title(5, "RSA 一次能加密多长的数据？");
        System.out.println("2048 位 RSA + OAEP-SHA256 的单次上限 = 256 - 2*32 - 2 = 190 字节");
        byte[] tooLong = new byte[300];
        try {
            Cipher tooLongCipher = Cipher.getInstance(RSA_OAEP);
            tooLongCipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA256);
            tooLongCipher.doFinal(tooLong);
            System.out.println("300 字节直接加密：意外成功了");
        } catch (Exception e) {
            System.out.println("300 字节直接加密失败: " + e.getMessage());
        }

        // ------------------------------------------------------------
        title(6, "真实做法：混合加密（RSA 加密 AES 密钥，AES-GCM 加密数据）");
        String longMessage = "这是一条超过 RSA 单次加密上限的长消息。"
                + "真实系统不会用 RSA 直接加密文件或聊天内容，"
                + "而是让 RSA 只加密一把临时 AES 密钥，"
                + "再用 AES-GCM 加密数据。"
                + "TLS、PGP、S/MIME 都是这个思路。";
        byte[] longPlaintext = longMessage.getBytes(StandardCharsets.UTF_8);

        // 1) 随机生成一把 AES-256 会话密钥
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, RANDOM);
        SecretKey aesKey = keyGenerator.generateKey();

        // 2) 用 AES-GCM 加密长消息（12 字节随机 IV，16 字节认证标签）
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        Cipher aesEncrypt = Cipher.getInstance("AES/GCM/NoPadding");
        aesEncrypt.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        byte[] encryptedData = aesEncrypt.doFinal(longPlaintext);

        // 3) 用 RSA 公钥加密 AES 密钥
        Cipher rsaEncrypt = Cipher.getInstance(RSA_OAEP);
        rsaEncrypt.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA256);
        byte[] wrappedKey = rsaEncrypt.doFinal(aesKey.getEncoded());

        System.out.println("长消息（" + longPlaintext.length + " 字节）: " + longMessage);
        System.out.println("AES-GCM 密文: " + encryptedData.length + " 字节（含 16 字节认证标签）");
        System.out.println("RSA 加密后的 AES 密钥: " + wrappedKey.length + " 字节");
        System.out.println("IV（Base64）: " + Base64.getEncoder().encodeToString(iv));

        // 4) 解密：先用 RSA 私钥解出 AES 密钥，再用 AES 解数据
        Cipher rsaDecrypt = Cipher.getInstance(RSA_OAEP);
        rsaDecrypt.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SHA256);
        SecretKey recoveredKey = new SecretKeySpec(rsaDecrypt.doFinal(wrappedKey), "AES");

        Cipher aesDecrypt = Cipher.getInstance("AES/GCM/NoPadding");
        aesDecrypt.init(Cipher.DECRYPT_MODE, recoveredKey, new GCMParameterSpec(128, iv));
        byte[] decryptedData = aesDecrypt.doFinal(encryptedData);

        System.out.println("恢复出的消息: " + new String(decryptedData, StandardCharsets.UTF_8));
        System.out.println("与原文一致: " + Arrays.equals(longPlaintext, decryptedData));

        // ------------------------------------------------------------
        title(7, "小结");
        System.out.println("· 公钥加密、私钥解密；数字签名正好相反（私钥签、公钥验）。");
        System.out.println("· RSA 只适合加密短数据或“密钥”，长数据要用 RSA + AES 混合加密。");
        System.out.println("· 加密用 OAEP，不要用 PKCS#1 v1.5；2048 位是当前兼容性下限。");
        System.out.println("· 这次没有写 RSA 数学，全部由 JDK 的 SunJCE 提供者完成。");
        System.out.println("· JCE 转换名里的 ECB 只是命名占位，RSA 不是分组密码。");
    }

    // ------------------------------------------------------------------
    // 输出辅助
    // ------------------------------------------------------------------

    private static void title(int step, String text) {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println("【" + step + "】" + text);
        System.out.println("==================================================================");
    }

    /** 打印十六进制串并按固定宽度换行，方便阅读长密文/长密钥。 */
    private static void printWrapped(String text, int width) {
        for (int i = 0; i < text.length(); i += width) {
            System.out.println("  " + text.substring(i, Math.min(text.length(), i + width)));
        }
    }

    /** 取字节数组前/后各 n 字节的十六进制，中间用 ... 省略。 */
    private static String headTail(byte[] data, int n) {
        int length = Math.min(n, data.length);
        String head = Hex.toHex(Arrays.copyOfRange(data, 0, length));
        String tail = Hex.toHex(Arrays.copyOfRange(data, data.length - length, data.length));
        return head + " ... " + tail;
    }

    /** 公钥指纹：对 X.509 编码取 SHA-256，截前 16 字节。 */
    private static String fingerprint(byte[] encodedKey) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(encodedKey);
        return Hex.toHex(Arrays.copyOf(digest, 16));
    }
}
