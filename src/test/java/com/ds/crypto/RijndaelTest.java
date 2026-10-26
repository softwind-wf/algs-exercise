package com.ds.crypto;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Rijndael} 核心变换的单元测试。
 *
 * <p>覆盖四层：
 * <ol>
 *   <li>基础分量：S 盒、逆 S 盒、GF(2^8) 乘法、xtime；</li>
 *   <li>官方已知答案向量（KAT）：FIPS-197 附录 B、C.1–C.3，NIST SP 800-38A ECB；</li>
 *   <li>差分测试：与 JDK 自带 AES（JCE）对同一随机输入比对；</li>
 *   <li>泛化参数：Nb/Nk 的 9 种组合往返，并用独立实现 py3rijndael 生成的固定向量回归。</li>
 * </ol>
 *
 * <p>测试类与方法保持包级可见（JUnit 5 支持），不额外引入测试依赖。
 */
class RijndaelTest {

    /** FIPS-197 附录 B 的密钥（同时是附录 A 密钥扩展例子）。 */
    private static final String FIPS_KEY = "2b7e151628aed2a6abf7158809cf4f3c";

    /** FIPS-197 附录 B 的明文。 */
    private static final String FIPS_INPUT = "3243f6a8885a308d313198a2e0370734";

    /**
     * 泛化参数的固定向量：输入为可复现的模式
     * （key[i]=(17i+3)、block[i]=(31i+7)，均取低 8 位），期望值由独立实现
     * py3rijndael 0.3.3 交叉验证得到，用于锁住 ShiftRows(Nb=8) 与
     * Nk&gt;6 密钥扩展等非 AES 路径。
     */
    private static final String[][] GENERALIZED_VECTORS = {
            {"4", "4", "0aa38aa9c03236fa613694a9887755ee"},
            {"4", "6", "a47e3cf928dbde38427b94a6ea888500"},
            {"4", "8", "f61e836dab38374d10954990637f0d2a"},
            {"6", "4", "f5283e8051ee77ea6ebbddd4d44486b265e612d5c1ec3e74"},
            {"6", "6", "720e0596561d2d6e8286aec5a5d4a0a7333fdd68fb29346f"},
            {"6", "8", "b29f6029e2b19cf64c5c86d80a9883edde3069cb937acfb7"},
            {"8", "4", "3106b2d6f82d2af093dfc0d83528d389346b0c744eea86572fdc07e73056f2f8"},
            {"8", "6", "6868fd3aa912ae7fa97b90041a8ad7d81d80b66b1ffb47a609c0022dfd23ab85"},
            {"8", "8", "448e5ad1a7ad963c1ef5fa997fff15d39a9c0a69d73609f3f425cd7d07627bec"},
    };

    // ------------------------------------------------------------------
    // 1) 基础分量
    // ------------------------------------------------------------------

    @Test
    void sBoxMatchesKnownPoints() {
        assertEquals(0x63, Rijndael.sBox(0x00), "S(00)");
        assertEquals(0x7c, Rijndael.sBox(0x01), "S(01)");
        assertEquals(0x77, Rijndael.sBox(0x02), "S(02)");
        assertEquals(0xed, Rijndael.sBox(0x53), "FIPS-197 的 S(53)");
        assertEquals(0x53, Rijndael.invSBox(0xed), "逆 S(ed)");
    }

    @Test
    void sBoxIsABijectionAndInverseRestoresEveryByte() {
        boolean[] seen = new boolean[256];
        for (int value = 0; value < 256; value++) {
            int substituted = Rijndael.sBox(value);
            assertFalse(seen[substituted], "S 盒输出重复：" + substituted);
            seen[substituted] = true;
            assertEquals(value, Rijndael.invSBox(substituted), "逆 S 盒还原失败：" + value);
        }
    }

    @Test
    void gfMultiplicationMatchesFips197Example() {
        // FIPS-197 第 4.2 节：0x57 • 0x13 = 0xfe
        assertEquals(0xfe, Rijndael.multiply(0x57, 0x13));
        assertEquals(0x57, Rijndael.multiply(0x57, 0x01));
        // 乘法交换律与分配律抽样
        assertEquals(Rijndael.multiply(0x13, 0x57), Rijndael.multiply(0x57, 0x13));
        assertEquals(Rijndael.multiply(0x57, 0x13 ^ 0x83),
                Rijndael.multiply(0x57, 0x13) ^ Rijndael.multiply(0x57, 0x83));
    }

    @Test
    void xtimeAppliesAesReductionPolynomial() {
        assertEquals(0xae, Rijndael.xtime(0x57));
        assertEquals(0x1b, Rijndael.xtime(0x80), "最高位为 1 时异或 0x1b");
        assertEquals(0x00, Rijndael.xtime(0x00));
        assertEquals(0x36, Rijndael.xtime(0x1b));
    }

    // ------------------------------------------------------------------
    // 2) 官方已知答案向量
    // ------------------------------------------------------------------

    @Test
    void fips197AppendixB() {
        assertAesVector(FIPS_KEY, FIPS_INPUT, "3925841d02dc09fbdc118597196a0b32");
    }

    @Test
    void fips197AppendixC1Aes128() {
        assertAesVector("000102030405060708090a0b0c0d0e0f",
                "00112233445566778899aabbccddeeff", "69c4e0d86a7b0430d8cdb78070b4c55a");
    }

    @Test
    void fips197AppendixC2Aes192() {
        assertAesVector("000102030405060708090a0b0c0d0e0f1011121314151617",
                "00112233445566778899aabbccddeeff", "dda97ca4864cdfe06eaf70a0ec0d7191");
    }

    @Test
    void fips197AppendixC3Aes256() {
        assertAesVector("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
                "00112233445566778899aabbccddeeff", "8ea2b7ca516745bfeafc49904b496089");
    }

    @Test
    void nistSp80038aEcbFirstBlock() {
        // NIST SP 800-38A F.1.1 ECB-AES128.Encrypt 的两个分组
        Rijndael aes = new Rijndael(Hex.fromHex("2b7e151628aed2a6abf7158809cf4f3c"));
        assertEquals("3ad77bb40d7a3660a89ecaf32466ef97",
                Hex.toHex(aes.encryptBlock(Hex.fromHex("6bc1bee22e409f96e93d7e117393172a"))));
        assertEquals("f5d3d58503b9699de785895a96fdbaaf",
                Hex.toHex(aes.encryptBlock(Hex.fromHex("ae2d8a571e03ac9c9eb76fac45af8e51"))));
    }

    @Test
    void keyScheduleMatchesFips197AppendixA() {
        Rijndael aes = new Rijndael(Hex.fromHex(FIPS_KEY));
        int[] words = aes.keySchedule();
        assertEquals(44, words.length, "AES-128 共 44 个字");
        assertEquals(0x2b7e1516, words[0]);
        assertEquals(0x09cf4f3c, words[3]);
        assertEquals(0xa0fafe17, words[4], "W[4] 需先 RotWord+SubWord+Rcon");
        assertEquals(0xb6630ca6, words[43], "最后一个扩展字");
        assertArrayEquals(Hex.fromHex(FIPS_KEY), aes.roundKey(0));
        assertArrayEquals(Hex.fromHex("d014f9a8c9ee2589e13f0cc8b6630ca6"), aes.roundKey(10));
        assertThrows(IllegalArgumentException.class, () -> aes.roundKey(-1));
        assertThrows(IllegalArgumentException.class, () -> aes.roundKey(11));
    }

    // ------------------------------------------------------------------
    // 3) 差分测试：与 JDK AES 对照
    // ------------------------------------------------------------------

    @Test
    void aesEcbMatchesJceOnRandomInputs() throws Exception {
        Random random = new Random(20261026L);
        for (int keySize : new int[]{16, 24, 32}) {
            for (int round = 0; round < 60; round++) {
                byte[] key = new byte[keySize];
                byte[] block = new byte[16];
                random.nextBytes(key);
                random.nextBytes(block);

                Rijndael aes = new Rijndael(key);
                byte[] mine = aes.encryptBlock(block);
                byte[] fromJce = jceEcb(Cipher.ENCRYPT_MODE, key, block);
                assertArrayEquals(fromJce, mine, "AES-" + (keySize * 8) + " 加密结果与 JCE 不一致");

                assertArrayEquals(block, aes.decryptBlock(fromJce), "JCE 密文应能由本实现解开");
                assertArrayEquals(block, jceEcb(Cipher.DECRYPT_MODE, key, mine), "JCE 应能解开本实现密文");
            }
        }
    }

    @Test
    void avalancheChangesAboutHalfOfTheCiphertextBits() {
        Rijndael aes = new Rijndael(Hex.fromHex(FIPS_KEY));
        byte[] original = Hex.fromHex(FIPS_INPUT);
        byte[] flipped = original.clone();
        flipped[0] ^= 0x01;

        byte[] ct1 = aes.encryptBlock(original);
        byte[] ct2 = aes.encryptBlock(flipped);
        int differingBits = 0;
        for (int i = 0; i < ct1.length; i++) {
            differingBits += Integer.bitCount((ct1[i] ^ ct2[i]) & 0xff);
        }
        assertTrue(differingBits > 40 && differingBits < 88,
                "1 位差异应扩散到约一半密文位，实际 " + differingBits + "/128");
    }

    @Test
    void wrongKeyDoesNotRecoverPlaintext() {
        Rijndael right = new Rijndael(Hex.fromHex(FIPS_KEY));
        Rijndael wrong = new Rijndael(Hex.fromHex("000102030405060708090a0b0c0d0e0f"));
        byte[] ciphertext = right.encryptBlock(Hex.fromHex(FIPS_INPUT));
        assertFalse(Arrays.equals(Hex.fromHex(FIPS_INPUT), wrong.decryptBlock(ciphertext)));
    }

    // ------------------------------------------------------------------
    // 4) 泛化参数与异常路径
    // ------------------------------------------------------------------

    @Test
    void roundsFollowMaxOfNbAndNkPlusSix() {
        int[][] combinations = {
                {4, 4, 10}, {4, 6, 12}, {4, 8, 14},
                {6, 4, 12}, {6, 6, 12}, {6, 8, 14},
                {8, 4, 14}, {8, 6, 14}, {8, 8, 14},
        };
        for (int[] combination : combinations) {
            int nb = combination[0];
            int nk = combination[1];
            Rijndael cipher = new Rijndael(patternBytes(4 * nk, 17, 3), 4 * nb);
            assertEquals(nb, cipher.nb());
            assertEquals(nk, cipher.nk());
            assertEquals(4 * nb, cipher.blockSize());
            assertEquals(4 * nk, cipher.keySize());
            assertEquals(combination[2], cipher.nr(), "Nr 应为 max(Nb,Nk)+6");
        }
    }

    @Test
    void generalizedVectorsMatchIndependentReference() {
        for (String[] vector : GENERALIZED_VECTORS) {
            int nb = Integer.parseInt(vector[0]);
            int nk = Integer.parseInt(vector[1]);
            byte[] key = patternBytes(4 * nk, 17, 3);
            byte[] block = patternBytes(4 * nb, 31, 7);

            Rijndael cipher = new Rijndael(key, block.length);
            byte[] ciphertext = cipher.encryptBlock(block);
            assertEquals(vector[2], Hex.toHex(ciphertext),
                    "Nb=" + nb + " Nk=" + nk + " 与独立实现向量不一致");
            assertArrayEquals(block, cipher.decryptBlock(ciphertext), "Nb=" + nb + " Nk=" + nk + " 往返失败");
        }
    }

    @Test
    void roundTripAllNineParameterCombinationsOnRandomData() {
        Random random = new Random(271828L);
        for (int nb = 4; nb <= 8; nb += 2) {
            for (int nk = 4; nk <= 8; nk += 2) {
                Rijndael cipher = new Rijndael(new byte[4 * nk], 4 * nb);
                for (int round = 0; round < 10; round++) {
                    byte[] key = new byte[4 * nk];
                    byte[] block = new byte[4 * nb];
                    random.nextBytes(key);
                    random.nextBytes(block);
                    cipher = new Rijndael(key, block.length);
                    assertArrayEquals(block, cipher.decryptBlock(cipher.encryptBlock(block)));
                }
            }
        }
    }

    @Test
    void rejectsInvalidKeyAndBlockSizes() {
        assertThrows(IllegalArgumentException.class, () -> new Rijndael((byte[]) null));
        for (int keyLength : new int[]{0, 15, 17, 20, 28, 33, 64}) {
            assertThrows(IllegalArgumentException.class, () -> new Rijndael(new byte[keyLength]),
                    "非法密钥长度应被拒绝：" + keyLength);
        }
        for (int blockSize : new int[]{0, 12, 20, 40}) {
            assertThrows(IllegalArgumentException.class, () -> new Rijndael(new byte[16], blockSize),
                    "非法分组长度应被拒绝：" + blockSize);
        }
    }

    @Test
    void rejectsWrongBlockLengthOnEncryptAndDecrypt() {
        Rijndael aes = new Rijndael(Hex.fromHex(FIPS_KEY));
        assertThrows(IllegalArgumentException.class, () -> aes.encryptBlock(new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> aes.encryptBlock(new byte[17]));
        assertThrows(IllegalArgumentException.class, () -> aes.decryptBlock(new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> aes.decryptBlock(null));
    }

    @Test
    void traceReportsColumnMajorStateCopies() {
        Rijndael aes = new Rijndael(Hex.fromHex(FIPS_KEY));
        byte[] plaintext = Hex.fromHex(FIPS_INPUT);
        final int[][][] capturedInput = new int[1][][];
        final int[] stepCount = {0};

        aes.encryptBlock(plaintext, (round, phase, state) -> {
            stepCount[0]++;
            if (round == 0 && "input".equals(phase)) {
                capturedInput[0] = state;
            }
        });

        // input(1) + 初始 addRoundKey(1) + 10 轮 × (start/subBytes/shiftRows/addRoundKey)
        // + 9 个 mixColumns = 51
        assertEquals(51, stepCount[0]);
        assertNotNull(capturedInput[0]);
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                assertEquals(plaintext[4 * c + r] & 0xff, capturedInput[0][r][c],
                        "状态应按列优先装入：state[r][c] = input[4c+r]");
            }
        }
        // 回调拿到的是副本：加密结束后快照不应被内部状态改写
        assertArrayEquals(plaintext, Hex.fromHex(columnMajorHex(capturedInput[0])));
    }

    // ------------------------------------------------------------------
    // 测试辅助
    // ------------------------------------------------------------------

    private static void assertAesVector(String keyHex, String inputHex, String expectedHex) {
        Rijndael aes = new Rijndael(Hex.fromHex(keyHex));
        byte[] ciphertext = aes.encryptBlock(Hex.fromHex(inputHex));
        assertEquals(expectedHex, Hex.toHex(ciphertext), "加密结果");
        assertEquals(inputHex, Hex.toHex(aes.decryptBlock(ciphertext)), "解密结果");
    }

    /** 生成可复现的模式字节：value[i] = (factor*i + offset) & 0xff。 */
    private static byte[] patternBytes(int length, int factor, int offset) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = (byte) (factor * i + offset);
        }
        return out;
    }

    private static String columnMajorHex(int[][] state) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < state[0].length; c++) {
            for (int r = 0; r < 4; r++) {
                sb.append(String.format("%02x", state[r][c]));
            }
        }
        return sb.toString();
    }

    private static byte[] jceEcb(int mode, byte[] key, byte[] input) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(input);
    }
}
