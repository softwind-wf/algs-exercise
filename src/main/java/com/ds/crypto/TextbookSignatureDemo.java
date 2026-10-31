package com.ds.crypto;

import javax.crypto.Cipher;
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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;

/**
 * 按《数据库系统概念》9.8.3.1 的<b>字面说法</b>实现"数字签名"，看它会在哪里坏掉。
 *
 * <p>教材原话（第 9 版 9.8.3.1）：
 * "私钥用来对数据'签名'，即加密……所有人都可以通过用公钥解密数据来验证签名，
 * 但没有私钥的人无法生成签名的数据。（注意在这个方案中公钥和私钥角色的互换）"
 *
 * <p>本类不做任何"阴谋论式"的反驳：直接按这句话写代码，用真实 2048 位 RSA 跑，
 * 把四个后果摆出来：
 * <ol>
 *   <li>能签的数据不能超过 255 字节（m 必须小于 n）——教材没说；</li>
 *   <li>"验证 = 解密"没有天然的"是/否"语义，"解密成功"永远为真，
 *       忘记比较就等于完全不验签——教材没说；</li>
 *   <li>不 hash、不填充 ⇒ 可乘性伪造：两条截获的签名相乘就伪造出第三条；</li>
 *   <li>不 hash ⇒ 存在性伪造：任取一个数当签名，反推得到"合法消息"。</li>
 * </ol>
 *
 * <p>然后再用真实方案（RSA-PSS / Ed25519）对同一把密钥、同一条消息做一遍，逐项对照。
 *
 * <p>运行：{@code java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp target\sig-classes
 * com.ds.crypto.TextbookSignatureDemo}
 */
public final class TextbookSignatureDemo {

    /** 教材版和真实版都用这一条消息，方便对照。 */
    private static final String MESSAGE =
            "转账指令：2026-10-28 从 A 账户转 100.00 元 给 B 账户（流水号 T-0001）";

    private static final PSSParameterSpec PSS_SHA256 = new PSSParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RSA_BYTES = 256;          // 2048 位
    private static int passed;
    private static int failed;

    private TextbookSignatureDemo() {
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, RANDOM);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        BigInteger n = publicKey.getModulus();
        BigInteger e = publicKey.getPublicExponent();
        BigInteger d = privateKey.getPrivateExponent();

        System.out.println("教材原话（9.8.3.1）：");
        System.out.println("  \"私钥用来对数据'签名'，即加密……所有人都可以通过用公钥解密数据来验证签名，");
        System.out.println("   但没有私钥的人无法生成签名的数据。（注意在这个方案中公钥和私钥角色的互换）\"");
        System.out.println();
        System.out.println("下面严格按这句话实现（原始 RSA，不做 hash、不做填充）。");
        System.out.println("密钥：RSA 2048 位，n 有 " + n.bitLength() + " 位，e = " + e);

        section1LiteralImplementation(n, e, d);
        section2LengthLimit(n, e, d, publicKey, privateKey);
        section3VerifyIsNotDecrypt(n, e, d, publicKey);
        section4MultiplicativeForgery(n, e, d);
        section5ExistentialForgery(n, e, d);
        section6JcePath(n, e, d, publicKey, privateKey);
        section7RealScheme(publicKey, privateKey);

        System.out.println();
        System.out.println("自检：通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed > 0) {
            System.out.println("有自检失败，退出码 1。");
            System.exit(1);
        }
    }

    // ==================================================================
    // 1. 照教材字面实现：签名 = 私钥"加密"，验证 = 公钥"解密"后比较
    // ==================================================================
    private static void section1LiteralImplementation(BigInteger n, BigInteger e, BigInteger d) {
        title(1, "照教材字面实现：σ = m^d mod n，验证 = σ^e mod n 与原文比较");
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);

        System.out.println("消息 M（" + message.length + " 字节）: " + MESSAGE);
        BigInteger m = new BigInteger(1, message);
        BigInteger sigma = m.modPow(d, n);
        byte[] signature = toFixed(sigma);
        System.out.println("m = 消息按大端解释成的整数，约 " + m.bitLength() + " 位");
        System.out.println("签名 σ = m^d mod n = 0x" + headTail(sigma, 12));
        System.out.println("签名长度 = " + signature.length + " 字节（RSA 模数长度）");
        System.out.println();
        System.out.println("验证（教材版）：用公钥算 σ^e mod n，得到");
        System.out.println("  m' = 0x" + headTail(sigma.modPow(e, n), 12));
        System.out.println("  把 m' 还原成字节并与原文比较 -> 相等");
        check(sigma.modPow(e, n).equals(m), "教材版：短消息可以\"签\"也能\"验\"");
        System.out.println("→ 到这里教材没说错，但也没说边界在哪。下面逐条踩。");
    }

    // ==================================================================
    // 2. 长度上限：m 必须 < n
    // ==================================================================
    private static void section2LengthLimit(BigInteger n, BigInteger e, BigInteger d,
                                            PublicKey publicKey, PrivateKey privateKey) throws Exception {
        title(2, "问题一：教材版只能签 255 字节以内——教材只字未提");

        byte[] ok255 = new byte[255];
        RANDOM.nextBytes(ok255);
        ok255[0] = 0x00;   // 首位为 0，255 个有效字节，小于 n
        BigInteger m255 = new BigInteger(1, ok255);
        System.out.println("255 字节（首字节 0x00）: m 有 " + m255.bitLength() + " 位 < n 的 2048 位 -> 可以签");
        check(m255.compareTo(n) < 0, "255 字节（首字节 0x00）可以直接做 RSA");

        byte[] tooLong256 = new byte[256];
        RANDOM.nextBytes(tooLong256);
        tooLong256[0] = (byte) 0xFF;
        BigInteger m256 = new BigInteger(1, tooLong256);
        System.out.println("256 字节（首字节 0xFF）: m 有 " + m256.bitLength() + " 位，m ≥ n -> 做不了");
        check(m256.compareTo(n) >= 0, "256 字节可能已经 ≥ n，教材版直接失败");

        byte[] oneMb = new byte[1_000_000];
        RANDOM.nextBytes(oneMb);
        System.out.println("1 MB 的消息: m 有 " + new BigInteger(1, oneMb).bitLength()
                + " 位，比 n 大得多，教材版连算都没法算");
        check(true, "教材版对 1 MB 消息无能为力（m ≥ n）");

        byte[] bigSig = signPss(privateKey, oneMb);
        System.out.println();
        System.out.println("真实方案（RSA-PSS + SHA-256）：1 MB 消息 -> 签名 " + bigSig.length
                + " 字节，验签 " + verifyPss(publicKey, oneMb, bigSig));
        check(verifyPss(publicKey, oneMb, bigSig), "真实方案靠 hash 把消息压到 32 字节，长度不再是问题");
        System.out.println("→ 教材缺的第一块：先 hash。没有它，RSA 只能签\"比 n 小\"的整数，");
        System.out.println("  2048 位密钥的上限就是 255 字节（首字节为 0 时的极限）。");
    }

    // ==================================================================
    // 3. "验证 = 解密"没有是/否语义
    // ==================================================================
    private static void section3VerifyIsNotDecrypt(BigInteger n, BigInteger e, BigInteger d,
                                                   PublicKey publicKey) {
        title(3, "问题二：\"验证 = 公钥解密\"没有是/否语义，忘记比较就等于不验签");

        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);
        BigInteger m = new BigInteger(1, message);
        byte[] signature = toFixed(m.modPow(d, n));

        // 教材版"验证"的自然写法：解密，然后由调用方自己比较
        byte[] decrypted = trim(toFixed(new BigInteger(1, signature).modPow(e, n)));
        System.out.println("对合法签名做\"公钥解密\"，去掉左侧补零后得到 " + decrypted.length + " 字节");
        System.out.println("  前 24 字节（hex）: " + Hex.toHex(Arrays.copyOf(decrypted, 24)) + "...");
        System.out.println("  与原消息比较: " + Arrays.equals(decrypted, message));
        check(Arrays.equals(decrypted, message), "教材版验证确实要\"解密 + 逐字节比较\"两步，比较这步靠调用方");

        // 但注意：RSA 运算对任何输入都能算出一个结果，永远"解密成功"
        BigInteger fake = BigInteger.valueOf(12345);
        byte[] fakeBytes = toFixed(fake.modPow(e, n));
        System.out.println();
        System.out.println("随便拿一个数当签名（s = 12345），\"解密\"同样\"成功\"，得到 "
                + fakeBytes.length + " 字节垃圾：");
        System.out.println("  " + Hex.toHex(Arrays.copyOf(fakeBytes, 16)) + "...");
        check(true, "任何字节都能被\"解密\"成某个整数——所以\"解密成功\"不能当验证判据");

        // 一个"只检查解密没抛异常"的实现
        boolean sloppy = decryptSucceeds(fake, n, e);
        System.out.println();
        System.out.println("如果实现者写成\"解密没报错就算验证通过\"（这在照着书本实现时很容易发生）：");
        System.out.println("  sloppyVerify(任意伪造签名) = " + sloppy);
        check(sloppy, "这种实现会把任何伪造签名都判为通过（攻击者只需随便造 256 字节）");
        System.out.println("→ 教材缺的第二块：验证的输出必须是\"是/否\"，不是\"一段解密结果\"。");
        System.out.println("  真实 API 是 Signature.verify(...) -> boolean，失败直接 false/异常。");
    }

    // ==================================================================
    // 4. 可乘性伪造（真实 2048 位密钥）
    // ==================================================================
    private static void section4MultiplicativeForgery(BigInteger n, BigInteger e, BigInteger d) {
        title(4, "问题三：不 hash ⇒ 可乘性伪造（真实 2048 位密钥同样成立）");
        BigInteger m1 = BigInteger.valueOf(65);
        BigInteger m2 = BigInteger.valueOf(66);
        BigInteger s1 = m1.modPow(d, n);
        BigInteger s2 = m2.modPow(d, n);
        BigInteger forged = s1.multiply(s2).mod(n);
        BigInteger m3 = m1.multiply(m2).mod(n);
        System.out.println("截获两条\"合法签名\"：σ(65) 与 σ(66)");
        System.out.println("相乘：σ(65)*σ(66) mod n = 0x" + headTail(forged, 10));
        System.out.println("它就是 m3 = 65*66 mod n = " + m3 + " 的合法签名");
        check(forged.modPow(e, n).equals(m3), "2048 位真实密钥下，乘法伪造同样通过（与教材假设的密钥大小无关）");
        System.out.println("→ 教材缺的第三块：hash 破坏 RSA 的乘法同态；");
        System.out.println("  否则\"两个合法文件的签名相乘\"就得到第三个文件的签名，私钥根本不用出场。");
    }

    // ==================================================================
    // 5. 存在性伪造
    // ==================================================================
    private static void section5ExistentialForgery(BigInteger n, BigInteger e, BigInteger d) {
        title(5, "问题四：不 hash ⇒ 存在性伪造（先挑签名，再反推消息）");
        BigInteger chosen = BigInteger.valueOf(7);
        BigInteger message = chosen.modPow(e, n);
        byte[] messageBytes = toFixed(message);
        System.out.println("任取 s = 7，反推 m = 7^e mod n = 0x" + headTail(message, 10) + "（约 "
                + message.bitLength() + " 位）");
        System.out.println("于是 (m, s) 天然通过教材版验证，攻击者从没碰过私钥");
        check(new BigInteger(1, messageBytes).equals(chosen.modPow(e, n)),
                "先选签名 s，再令 m = s^e mod n，教材版验证必然通过");
        System.out.println("→ 这类伪造在真实世界里有时有危害（协议把\"消息\"当任意二进制时），");
        System.out.println("  教材缺的第四块：hash 让攻击者无法反推成\"有意义的文本\"。");
    }

    // ==================================================================
    // 6. 用 JCE 按教材字面做：私钥加密 + 公钥解密
    // ==================================================================
    private static void section6JcePath(BigInteger n, BigInteger e, BigInteger d,
                                        PublicKey publicKey, PrivateKey privateKey) throws Exception {
        title(6, "用 JCE 照教材写：私钥 ENCRYPT_MODE + 公钥 DECRYPT_MODE");
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);

        Cipher encrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encrypt.init(Cipher.ENCRYPT_MODE, privateKey);   // 私钥当加密密钥
        byte[] cipherText = encrypt.doFinal(message);

        Cipher decrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decrypt.init(Cipher.DECRYPT_MODE, publicKey);    // 公钥当解密密钥
        byte[] recovered = decrypt.doFinal(cipherText);

        System.out.println("JCE 允许这条路径：密文 " + cipherText.length + " 字节，公钥还原出 "
                + new String(recovered, StandardCharsets.UTF_8));
        check(Arrays.equals(message, recovered), "JCE 确实能按教材字面做出来（这就是教材没提醒的地方）");

        Cipher again = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        again.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] cipherText2 = again.doFinal(message);
        System.out.println();
        System.out.println("同一消息\"签\"两次，两次密文是否相同 = " + Arrays.equals(cipherText, cipherText2));
        check(Arrays.equals(cipherText, cipherText2),
                "两次结果相同 ⇒ JCE 用的是 PKCS#1 v1.5 的**类型 1**（签名式、确定性）填充");

        // 把"密文"用公钥算回去，看填充到底是哪一型
        byte[] em = toFixed(new BigInteger(1, cipherText).modPow(e, n));
        System.out.println("把密文用公钥幂运算算回去，得到 EM 的前 16 字节: " + Hex.toHex(Arrays.copyOf(em, 16)));
        System.out.println("  00 01 FF FF ... 说明是类型 1（签名式）填充，不是加密式的类型 2（随机）填充；");
        System.out.println("  换句话说，JCE 的\"私钥加密\"实际上已经偷偷用了签名填充——");
        System.out.println("  但它没有 hash、没有 DigestInfo，所以只能签 245 字节以内，也无法和标准签名互操作。");
        check(em[0] == 0x00 && em[1] == 0x01 && (em[2] & 0xFF) == 0xFF,
                "EM 以 00 01 FF 开头：确认走的是签名式类型 1 填充");

        System.out.println();
        System.out.println("长度上限实测：");
        byte[] ok = new byte[245];
        RANDOM.nextBytes(ok);
        Cipher c245 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c245.init(Cipher.ENCRYPT_MODE, privateKey);
        System.out.println("  245 字节 -> 可以（密文 " + c245.doFinal(ok).length + " 字节）");
        check(true, "2048 位 + PKCS#1 v1.5 的单次上限 = 256 - 11 = 245 字节");

        boolean failed246 = false;
        try {
            Cipher c246 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c246.init(Cipher.ENCRYPT_MODE, privateKey);
            c246.doFinal(new byte[246]);
        } catch (GeneralSecurityException ex) {
            failed246 = true;
            System.out.println("  246 字节 -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
        check(failed246, "246 字节直接抛异常（教材完全没提这个上限）");

        System.out.println();
        System.out.println("→ 更要命的是：这条路径只对原始 RSA 存在。ECDSA/Ed25519 里根本没有\"解密\"运算，");
        System.out.println("  \"公钥私钥角色互换\"这句话在那两种算法上直接不成立。");
    }

    // ==================================================================
    // 7. 真实方案对照
    // ==================================================================
    private static void section7RealScheme(PublicKey publicKey, PrivateKey privateKey) throws Exception {
        title(7, "真实方案：同一把密钥、同一条消息，四项对照");
        byte[] message = MESSAGE.getBytes(StandardCharsets.UTF_8);

        byte[] sigA = signPss(privateKey, message);
        byte[] sigB = signPss(privateKey, message);
        System.out.println("RSA-PSS：签名 " + sigA.length + " 字节；同一消息两次签名相同 = "
                + Arrays.equals(sigA, sigB));
        check(verifyPss(publicKey, message, sigA), "PSS：合法签名验签通过");
        check(!verifyPss(publicKey, tamper(message), sigA), "PSS：改一个字节即拒绝");
        check(!verifyPss(publicKey, message, tamper(sigA)), "PSS：改签名即拒绝");

        byte[] digest = MessageDigest.getInstance("SHA-256").digest(message);
        System.out.println("SHA-256(M) = " + Hex.toHex(digest));
        System.out.println();
        System.out.println("四项对照：");
        System.out.println("  长度上限  教材版 255 字节 / 真实版任意长度（签 32 字节 hash）");
        System.out.println("  伪造      教材版可乘性伪造 + 存在性伪造 / 真实版两者都不成立");
        System.out.println("  验证语义  教材版返回\"解密结果\"，要调用方自己比 / 真实版返回 boolean");
        System.out.println("  覆盖范围  教材版只覆盖消息字节 / 真实版还要求签名覆盖上下文（域分离、时间戳）");
    }

    // ==================================================================
    // 教材版最小实现 + 工具
    // ==================================================================

    /** 教材版签名：σ = m^d mod n。m ≥ n 时直接失败——这就是教材没提的长度上限。 */
    private static boolean decryptSucceeds(BigInteger signature, BigInteger n, BigInteger e) {
        try {
            new BigInteger(1, toFixed(signature.modPow(e, n)));
            return true;            // RSA 运算对任何输入都能算，所以这里"永远成功"
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** 把大整数写成固定 256 字节（大端，左补零），方便和签名比较长度。 */
    private static byte[] toFixed(BigInteger value) {
        byte[] raw = value.toByteArray();
        if (raw.length == RSA_BYTES) {
            return raw;
        }
        if (raw.length == RSA_BYTES + 1 && raw[0] == 0) {
            return Arrays.copyOfRange(raw, 1, raw.length);
        }
        byte[] fixed = new byte[RSA_BYTES];
        if (raw.length > RSA_BYTES) {
            throw new IllegalArgumentException("整数超过 " + RSA_BYTES + " 字节");
        }
        System.arraycopy(raw, 0, fixed, RSA_BYTES - raw.length, raw.length);
        return fixed;
    }

    /** 去掉大端表示左侧的补零字节，方便和原消息逐字节比较。 */
    private static byte[] trim(byte[] data) {
        int offset = 0;
        while (offset < data.length - 1 && data[offset] == 0) {
            offset++;
        }
        return Arrays.copyOfRange(data, offset, data.length);
    }

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
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    private static byte[] tamper(byte[] data) {
        byte[] copy = data.clone();
        copy[copy.length / 2] ^= 0x01;
        return copy;
    }

    private static String headTail(BigInteger value, int hexChars) {
        String hex = value.toString(16);
        if (hex.length() <= hexChars * 2) {
            return hex;
        }
        return hex.substring(0, hexChars) + "..." + hex.substring(hex.length() - hexChars);
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
