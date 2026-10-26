package com.ds.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Rijndael 加密/解密过程的逐步演示。
 *
 * <p>主例子取自 FIPS-197 附录 B（AES-128 官方演算）：给定明文与密钥，
 * 打印密钥扩展的 44 个字、加密 10 轮里每一步的状态矩阵（轮 1 与轮 10 展开，
 * 中间轮给一行摘要），以及解密时对应的逆变换；随后与 JDK 自带的 AES 做对照，
 * 演示 CBC + PKCS#7 的字符串加解密、雪崩效应，以及 Nb/Nk 取 6/8 的泛化参数。
 *
 * <p>运行：{@code java -cp target\rijndael-classes com.ds.crypto.RijndaelDemo}，
 * 或用脚本 {@code scripts/run-rijndael-tests.ps1} 一并编译运行。
 */
public final class RijndaelDemo {

    /** FIPS-197 附录 B 的密钥。 */
    private static final byte[] FIPS_KEY = Hex.fromHex("2b7e151628aed2a6abf7158809cf4f3c");

    /** FIPS-197 附录 B 的明文。 */
    private static final byte[] FIPS_INPUT = Hex.fromHex("3243f6a8885a308d313198a2e0370734");

    /** FIPS-197 附录 B 的期望密文。 */
    private static final String FIPS_EXPECTED = "3925841d02dc09fbdc118597196a0b32";

    private RijndaelDemo() {
    }

    public static void main(String[] args) throws Exception {
        Rijndael aes = new Rijndael(FIPS_KEY);

        section(1, "参数：Rijndael 与 AES 的关系");
        System.out.println("本例子来自 FIPS-197 附录 B（AES-128 的官方演算）。");
        System.out.println("明文 P = " + Hex.toHex(FIPS_INPUT));
        System.out.println("密钥 K = " + Hex.toHex(FIPS_KEY));
        System.out.println("Nb = " + aes.nb() + "（分组 " + aes.blockSize() + " 字节），Nk = " + aes.nk()
                + "（密钥 " + aes.keySize() + " 字节），Nr = max(Nb,Nk)+6 = " + aes.nr());

        section(2, "S 盒：GF(2^8) 求逆 + 仿射变换");
        System.out.println("S(0x00) = 0x" + twoDigitHex(Rijndael.sBox(0x00))
                + "，S(0x53) = 0x" + twoDigitHex(Rijndael.sBox(0x53))
                + "，逆 S(0x" + twoDigitHex(Rijndael.sBox(0x53)) + ") = 0x"
                + twoDigitHex(Rijndael.invSBox(Rijndael.sBox(0x53))));
        System.out.println("例如 S(0x63) = 0x" + twoDigitHex(Rijndael.sBox(0x63))
                + "；S 盒与逆 S 盒对 256 个字节的互逆关系由单元测试逐一校验。");

        section(3, "密钥扩展：从 4 个字扩成 44 个字（第 0..10 轮密钥）");
        printKeySchedule(aes);

        section(4, "加密：初始轮 + 10 轮（展开第 1、10 轮，中间轮给摘要）");
        final Rijndael cipher = aes;
        byte[] ciphertext = aes.encryptBlock(FIPS_INPUT, new Rijndael.Trace() {
            @Override
            public void step(int round, String phase, int[][] state) {
                if ("start".equals(phase)) {
                    return;                       // 与上一轮 addRoundKey 的结果相同，避免重复
                }
                if (round == 0) {
                    printGrid("input".equals(phase) ? "初始状态：明文按列优先装入 4×4 状态"
                            : "初始 AddRoundKey 之后（轮密钥 0）", state);
                } else if (round == 1 || round == cipher.nr()) {
                    printGrid("轮 " + round + "：" + phaseName(phase), state);
                } else if ("addRoundKey".equals(phase)) {
                    System.out.println("轮 " + round + " 完成（AddRoundKey 后）：" + stateColumnMajorHex(state));
                }
            }
        });
        System.out.println();
        System.out.println("最终密文 C = " + Hex.toHex(ciphertext));
        System.out.println("与 FIPS-197 附录 B 的期望值 " + FIPS_EXPECTED + " 一致："
                + FIPS_EXPECTED.equals(Hex.toHex(ciphertext)));

        section(5, "解密：逆密码（InvShiftRows / InvSubBytes / AddRoundKey / InvMixColumns）");
        final int totalRounds = aes.nr();
        byte[] recovered = aes.decryptBlock(ciphertext, new Rijndael.Trace() {
            @Override
            public void step(int round, String phase, int[][] state) {
                if ("start".equals(phase)) {
                    return;
                }
                if (round == totalRounds && "addRoundKey".equals(phase)) {
                    printGrid("解密起点：密文 + 第 " + round + " 轮密钥", state);
                } else if (round == totalRounds - 1 || round == 0) {
                    printGrid("逆轮 " + round + "：" + phaseName(phase), state);
                } else if ("invMixColumns".equals(phase)) {
                    System.out.println("逆轮 " + round + " 完成（InvMixColumns 后）：" + stateColumnMajorHex(state));
                }
            }
        });
        System.out.println();
        System.out.println("恢复出的明文 = " + Hex.toHex(recovered));
        System.out.println("与原始明文一致：" + Arrays.equals(FIPS_INPUT, recovered));

        section(6, "与 JDK 标准实现（JCE AES/ECB/NoPadding）对照");
        byte[] fromJce = jceEcb(Cipher.ENCRYPT_MODE, FIPS_KEY, FIPS_INPUT);
        System.out.println("JDK 加密结果   = " + Hex.toHex(fromJce));
        System.out.println("本实现加密结果 = " + Hex.toHex(ciphertext));
        System.out.println("一致：" + Arrays.equals(fromJce, ciphertext));
        byte[] jceDecrypted = jceEcb(Cipher.DECRYPT_MODE, FIPS_KEY, ciphertext);
        System.out.println("JDK 解密本实现密文 = " + Hex.toHex(jceDecrypted)
                + "，与原始明文一致：" + Arrays.equals(FIPS_INPUT, jceDecrypted));

        section(7, "实际消息：CBC 模式 + PKCS#7 填充");
        demoCbcMessage();

        section(8, "雪崩效应：明文改 1 位，密文应大幅变化");
        demoAvalanche(aes);

        section(9, "Rijndael 泛化：Nb、Nk 取 6/8 的分组与密钥");
        demoGeneralized();

        section(10, "说明");
        System.out.println("· 本实现只覆盖分组变换与 CBC 模式；没有认证（完整性），需要时用 GCM 等 AEAD。");
        System.out.println("· 教学实现非恒定时间，不能替代 JDK/BouncyCastle 的生产级 AES。");
        System.out.println("· 复现命令：.\\scripts\\run-rijndael-tests.ps1");
    }

    // ------------------------------------------------------------------
    // 各演示小节
    // ------------------------------------------------------------------

    /** 打印扩展密钥：每行一个轮密钥（Nb 个字），并标注特殊扩展步骤。 */
    private static void printKeySchedule(Rijndael cipher) {
        int[] words = cipher.keySchedule();
        int nb = cipher.nb();
        int nk = cipher.nk();
        for (int round = 0; round * nb < words.length; round++) {
            StringBuilder line = new StringBuilder();
            line.append(String.format("轮密钥 %2d（W[%2d..%2d]）:",
                    round, round * nb, round * nb + nb - 1));
            StringBuilder notes = new StringBuilder();
            for (int j = 0; j < nb; j++) {
                int index = round * nb + j;
                line.append(' ').append(String.format("%08x", words[index]));
                String note = keyScheduleNote(index, nk);
                if (note != null) {
                    notes.append(" W[").append(index).append("]=").append(note);
                }
            }
            System.out.println(line);
            if (notes.length() > 0) {
                System.out.println("           ↳" + notes);
            }
        }
    }

    /** 解释某个扩展字是怎么来的，普通字返回 null。 */
    private static String keyScheduleNote(int index, int nk) {
        if (index < nk) {
            return null;
        }
        if (index % nk == 0) {
            return "RotWord+SubWord+Rcon";
        }
        if (nk > 6 && index % nk == 4) {
            return "仅 SubWord（Nk>6 特有）";
        }
        return null;
    }

    /** CBC + PKCS#7 的中文消息加解密，并与 JCE 对照。 */
    private static void demoCbcMessage() throws Exception {
        byte[] key = "RijndaelDemo1234".getBytes(StandardCharsets.US_ASCII);
        byte[] iv = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
        String message = "Rijndael 加密演示：你好，世界！";
        byte[] plain = message.getBytes(StandardCharsets.UTF_8);

        RijndaelCbc cbc = new RijndaelCbc(key);
        byte[] ciphertext = cbc.encrypt(plain, iv);
        byte[] recovered = cbc.decrypt(ciphertext, iv);

        System.out.println("明文        : " + message);
        System.out.println("UTF-8 字节数: " + plain.length + "，填充后 " + ciphertext.length
                + " 字节（" + (ciphertext.length / cbc.blockSize()) + " 个分组）");
        System.out.println("密钥        : " + new String(key, StandardCharsets.US_ASCII)
                + "（" + key.length + " 字节）");
        System.out.println("IV          : " + new String(iv, StandardCharsets.US_ASCII));
        System.out.println("密文        : " + Hex.toHex(ciphertext));
        System.out.println("解密        : " + new String(recovered, StandardCharsets.UTF_8)
                + "，与原文一致：" + Arrays.equals(plain, recovered));

        Cipher jce = Cipher.getInstance("AES/CBC/PKCS5Padding");
        jce.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] jceCiphertext = jce.doFinal(plain);
        System.out.println("与 JDK AES/CBC/PKCS5Padding 密文一致：" + Arrays.equals(ciphertext, jceCiphertext));
    }

    /** 雪崩效应：翻转明文 1 位，统计密文改变的位数。 */
    private static void demoAvalanche(Rijndael aes) {
        byte[] changed = FIPS_INPUT.clone();
        changed[0] ^= 0x01;
        byte[] ct1 = aes.encryptBlock(FIPS_INPUT);
        byte[] ct2 = aes.encryptBlock(changed);
        int differingBits = 0;
        for (int i = 0; i < ct1.length; i++) {
            differingBits += Integer.bitCount((ct1[i] ^ ct2[i]) & 0xff);
        }
        int totalBits = ct1.length * 8;
        System.out.printf("原始明文密文 : %s%n", Hex.toHex(ct1));
        System.out.printf("翻转 1 位后  : %s%n", Hex.toHex(ct2));
        System.out.printf("不同的密文位数 = %d / %d（%.1f%%）%n",
                differingBits, totalBits, 100.0 * differingBits / totalBits);
        System.out.println("（理想情况下约一半的位会改变；这里只翻转了明文的第 1 位。）");
    }

    /** 验证 Nb/Nk 的 9 种组合，并给出 256 位分组 + 256 位密钥的密文例子。 */
    private static void demoGeneralized() {
        int[][] combos = {
                {4, 4}, {4, 6}, {4, 8},
                {6, 4}, {6, 6}, {6, 8},
                {8, 4}, {8, 6}, {8, 8},
        };
        String lastCiphertext = null;
        int ok = 0;
        for (int[] combo : combos) {
            int nb = combo[0];
            int nk = combo[1];
            byte[] key = new byte[4 * nk];
            byte[] block = new byte[4 * nb];
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) (17 * i + 3);
            }
            for (int i = 0; i < block.length; i++) {
                block[i] = (byte) (31 * i + 7);
            }
            Rijndael cipher = new Rijndael(key, block.length);
            byte[] ct = cipher.encryptBlock(block);
            byte[] back = cipher.decryptBlock(ct);
            boolean pass = Arrays.equals(block, back);
            if (pass) {
                ok++;
            }
            System.out.printf("Nb=%d Nk=%d（分组 %2d 字节，密钥 %2d 字节，Nr=%d）：往返%s%n",
                    nb, nk, 4 * nb, 4 * nk, cipher.nr(), pass ? "一致" : "失败");
            if (nb == 8 && nk == 8) {
                lastCiphertext = Hex.toHex(ct);
            }
        }
        System.out.println("9 种组合通过数：" + ok + "/9");
        System.out.println("Nb=8、Nk=8（256 位分组 + 256 位密钥）密文示例：" + lastCiphertext);
        System.out.println("AES 只是其中 Nb=4 的三种组合（Nk=4/6/8），其余 6 种属于 Rijndael 泛化形式。");
    }

    // ------------------------------------------------------------------
    // 输出与 JCE 辅助
    // ------------------------------------------------------------------

    private static void section(int index, String title) {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println("【" + index + "】" + title);
        System.out.println("==================================================================");
    }

    /** 打印 4×Nb 状态矩阵，并附上列优先的一行十六进制。 */
    private static void printGrid(String label, int[][] state) {
        System.out.println(label + "（列优先：" + stateColumnMajorHex(state) + "）");
        for (int r = 0; r < 4; r++) {
            StringBuilder line = new StringBuilder("  ");
            for (int c = 0; c < state[r].length; c++) {
                line.append(String.format("%02x", state[r][c]));
                if (c + 1 < state[r].length) {
                    line.append(' ');
                }
            }
            System.out.println(line);
        }
    }

    /** 状态按列优先序列化成十六进制串（与明文/密文的字节顺序一致）。 */
    private static String stateColumnMajorHex(int[][] state) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < state[0].length; c++) {
            for (int r = 0; r < 4; r++) {
                sb.append(String.format("%02x", state[r][c]));
            }
        }
        return sb.toString();
    }

    private static String phaseName(String phase) {
        switch (phase) {
            case "input":
                return "输入";
            case "subBytes":
                return "SubBytes（字节代换）";
            case "shiftRows":
                return "ShiftRows（行移位）";
            case "mixColumns":
                return "MixColumns（列混合）";
            case "addRoundKey":
                return "AddRoundKey（轮密钥加）";
            case "invShiftRows":
                return "InvShiftRows（逆行移位）";
            case "invSubBytes":
                return "InvSubBytes（逆字节代换）";
            case "invMixColumns":
                return "InvMixColumns（逆列混合）";
            default:
                return phase;
        }
    }

    private static String twoDigitHex(int value) {
        return String.format("%02x", value & 0xff);
    }

    private static byte[] jceEcb(int mode, byte[] key, byte[] input) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(input);
    }
}
