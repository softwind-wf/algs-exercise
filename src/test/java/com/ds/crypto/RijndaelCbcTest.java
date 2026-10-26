package com.ds.crypto;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RijndaelCbc}（CBC 模式 + PKCS#7 填充）的单元测试。
 *
 * <p>覆盖：填充的边界（0、整块、跨块）、与 JDK AES/CBC/PKCS5Padding 的差分、
 * NIST SP 800-38A 的 CBC 固定向量（对齐前缀）、以及 IV/密文长度等异常路径。
 */
class RijndaelCbcTest {

    /** NIST SP 800-38A F.2.1 CBC-AES128 的密钥与 IV。 */
    private static final String NIST_KEY = "2b7e151628aed2a6abf7158809cf4f3c";
    private static final String NIST_IV = "000102030405060708090a0b0c0d0e0f";

    /** F.2.1 的 4 个明文分组。 */
    private static final String NIST_PLAINTEXT =
            "6bc1bee22e409f96e93d7e117393172a"
                    + "ae2d8a571e03ac9c9eb76fac45af8e51"
                    + "30c81c46a35ce411e5fbc1191a0a52ef"
                    + "f69f2445df4f9b17ad2b417be66c3710";

    /** F.2.1 的 4 个密文分组。 */
    private static final String NIST_CIPHERTEXT =
            "7649abac8119b246cee98e9b12e9197d"
                    + "5086cb9b507219ee95db113a917678b2"
                    + "73bed6b8e3c1743b7116e69e22229516"
                    + "3ff1caa1681fac09120eca307586e1a7";

    // ------------------------------------------------------------------
    // PKCS#7 填充
    // ------------------------------------------------------------------

    @Test
    void pkcs7PadFillsToBlockBoundaryAndUnpadRestores() {
        Random random = new Random(9527L);
        for (int blockSize : new int[]{16, 24, 32}) {
            for (int length = 0; length <= 2 * blockSize + 1; length++) {
                byte[] data = new byte[length];
                random.nextBytes(data);

                byte[] padded = RijndaelCbc.pkcs7Pad(data, blockSize);
                assertEquals(0, padded.length % blockSize, "填充后长度必须是分组长度的整数倍");
                assertTrue(padded.length > length, "PKCS#7 至少补 1 字节");
                assertArrayEquals(data, RijndaelCbc.pkcs7Unpad(padded, blockSize));
            }
        }
    }

    @Test
    void pkcs7PaddingAddsAWholeBlockWhenAlreadyAligned() {
        byte[] aligned = new byte[16];
        byte[] padded = RijndaelCbc.pkcs7Pad(aligned, 16);
        assertEquals(32, padded.length, "恰好整块时补一整个 0x10 块");
        for (int i = 16; i < 32; i++) {
            assertEquals(0x10, padded[i]);
        }
        assertEquals(16, RijndaelCbc.pkcs7Pad(new byte[0], 16).length, "空明文补一个整块");
    }

    @Test
    void pkcs7UnpadRejectsMalformedPadding() {
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Unpad(new byte[0], 16));
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Unpad(new byte[15], 16));

        byte[] zeroPad = new byte[16];
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Unpad(zeroPad, 16));

        byte[] tooLarge = new byte[16];
        tooLarge[15] = 17;
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Unpad(tooLarge, 16));

        byte[] inconsistent = new byte[16];
        inconsistent[13] = 2;
        inconsistent[14] = 3;
        inconsistent[15] = 3;
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Unpad(inconsistent, 16));

        byte[] fullBlockPad = new byte[16];
        Arrays.fill(fullBlockPad, (byte) 16);
        assertEquals(0, RijndaelCbc.pkcs7Unpad(fullBlockPad, 16).length);
    }

    @Test
    void pkcs7RejectsInvalidBlockSize() {
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Pad(new byte[1], 0));
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Pad(new byte[1], 256));
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Unpad(new byte[16], 0));
        assertThrows(IllegalArgumentException.class, () -> RijndaelCbc.pkcs7Unpad(new byte[16], 256));
    }

    // ------------------------------------------------------------------
    // CBC 往返与差分
    // ------------------------------------------------------------------

    @Test
    void cbcRoundTripAcrossRijndaelBlockSizes() {
        Random random = new Random(31415L);
        for (int blockSize : new int[]{16, 24, 32}) {
            for (int keySize : new int[]{16, 24, 32}) {
                RijndaelCbc cbc = new RijndaelCbc(new byte[keySize], blockSize);
                for (int length = 0; length <= 2 * blockSize + 3; length++) {
                    byte[] plaintext = new byte[length];
                    byte[] iv = new byte[blockSize];
                    byte[] key = new byte[keySize];
                    random.nextBytes(plaintext);
                    random.nextBytes(iv);
                    random.nextBytes(key);
                    cbc = new RijndaelCbc(key, blockSize);

                    byte[] ciphertext = cbc.encrypt(plaintext, iv);
                    assertEquals(0, ciphertext.length % blockSize);
                    assertArrayEquals(plaintext, cbc.decrypt(ciphertext, iv),
                            "block=" + blockSize + " key=" + keySize + " len=" + length);
                }
            }
        }
    }

    @Test
    void aesCbcMatchesJceOnRandomInputs() throws Exception {
        Random random = new Random(1618L);
        int[] lengths = {0, 1, 5, 15, 16, 17, 23, 32, 33, 100};
        for (int keySize : new int[]{16, 24, 32}) {
            for (int length : lengths) {
                for (int round = 0; round < 2; round++) {
                    byte[] key = new byte[keySize];
                    byte[] iv = new byte[16];
                    byte[] plaintext = new byte[length];
                    random.nextBytes(key);
                    random.nextBytes(iv);
                    random.nextBytes(plaintext);

                    RijndaelCbc cbc = new RijndaelCbc(key);
                    byte[] mine = cbc.encrypt(plaintext, iv);
                    assertArrayEquals(jceCbc(Cipher.ENCRYPT_MODE, key, iv, plaintext), mine,
                            "AES-" + (keySize * 8) + " CBC 密文与 JCE 不一致，len=" + length);
                    assertArrayEquals(plaintext, cbc.decrypt(mine, iv));
                    assertArrayEquals(plaintext, jceCbc(Cipher.DECRYPT_MODE, key, iv, mine));
                }
            }
        }
    }

    @Test
    void cbcMatchesNistSp80038aOnAlignedPrefix() {
        byte[] key = Hex.fromHex(NIST_KEY);
        byte[] iv = Hex.fromHex(NIST_IV);
        byte[] plaintext = Hex.fromHex(NIST_PLAINTEXT);
        byte[] expected = Hex.fromHex(NIST_CIPHERTEXT);

        RijndaelCbc cbc = new RijndaelCbc(key);
        byte[] ciphertext = cbc.encrypt(plaintext, iv);

        // 本实现总会追加一个完整填充块，因此前 64 字节应与 NIST 的 4 个密文块逐一相同。
        assertEquals(expected.length + 16, ciphertext.length);
        assertArrayEquals(expected, Arrays.copyOf(ciphertext, expected.length),
                "CBC 链式结果应与 NIST SP 800-38A F.2.1 一致");
        assertArrayEquals(plaintext, cbc.decrypt(ciphertext, iv));

        // NIST 密文没有填充，末字节 0xa7 不是合法填充，必须拒绝而不是静默返回。
        assertThrows(IllegalArgumentException.class, () -> cbc.decrypt(expected, iv));
    }

    @Test
    void differentIvProducesDifferentCiphertext() {
        byte[] key = Hex.fromHex(NIST_KEY);
        byte[] plaintext = Hex.fromHex(NIST_PLAINTEXT);
        RijndaelCbc cbc = new RijndaelCbc(key);
        byte[] ct1 = cbc.encrypt(plaintext, Hex.fromHex(NIST_IV));
        byte[] ct2 = cbc.encrypt(plaintext, Hex.fromHex("0f0e0d0c0b0a09080706050403020100"));
        assertFalse(Arrays.equals(ct1, ct2), "不同 IV 应产生不同密文");
        assertArrayEquals(plaintext, cbc.decrypt(ct1, Hex.fromHex(NIST_IV)));
        assertArrayEquals(plaintext, cbc.decrypt(ct2, Hex.fromHex("0f0e0d0c0b0a09080706050403020100")));
    }

    // ------------------------------------------------------------------
    // 异常路径
    // ------------------------------------------------------------------

    @Test
    void rejectsInvalidIvAndCiphertextLengths() {
        RijndaelCbc cbc = new RijndaelCbc(Hex.fromHex(NIST_KEY));
        assertThrows(IllegalArgumentException.class, () -> cbc.encrypt(new byte[16], null));
        assertThrows(IllegalArgumentException.class, () -> cbc.encrypt(new byte[16], new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> cbc.encrypt(new byte[16], new byte[17]));
        assertThrows(IllegalArgumentException.class, () -> cbc.encrypt(null, new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> cbc.decrypt(new byte[0], new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> cbc.decrypt(new byte[17], new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> cbc.decrypt(new byte[16], new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> cbc.decrypt(null, new byte[16]));
    }

    @Test
    void exposesUnderlyingCipherParameters() {
        RijndaelCbc cbc = new RijndaelCbc(new byte[24], 24);
        assertEquals(24, cbc.blockSize());
        assertEquals(6, cbc.cipher().nb());
        assertEquals(6, cbc.cipher().nk());
        assertEquals(12, cbc.cipher().nr());
    }

    private static byte[] jceCbc(int mode, byte[] key, byte[] iv, byte[] input) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(input);
    }
}
