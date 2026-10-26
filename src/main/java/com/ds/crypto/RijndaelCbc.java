package com.ds.crypto;

import java.util.Arrays;

/**
 * Rijndael 的 CBC 模式封装，配 PKCS#7 填充（AES 场景下即 JCE 的 PKCS5Padding）。
 *
 * <p>CBC 的核心公式：{@code C_i = E_K(P_i xor C_{i-1})}，{@code C_0 = IV}；
 * 解密反过来：{@code P_i = D_K(C_i) xor C_{i-1}}。分组长度由 {@link Rijndael} 决定，
 * 默认 16 字节（AES）。这里只做模式与填充，不涉及认证，因此**不提供完整性保护**
 * （需要认证请用 GCM 等 AEAD 模式）。
 */
public final class RijndaelCbc {

    private final Rijndael cipher;

    /** 以 128 位分组构造。 */
    public RijndaelCbc(byte[] key) {
        this(key, 16);
    }

    /**
     * @param key            16/24/32 字节密钥
     * @param blockSizeBytes 16/24/32 字节分组
     */
    public RijndaelCbc(byte[] key, int blockSizeBytes) {
        this.cipher = new Rijndael(key, blockSizeBytes);
    }

    /** 底层分组密码（可用来单独加密一个分组）。 */
    public Rijndael cipher() {
        return cipher;
    }

    /** 分组长度（字节）。 */
    public int blockSize() {
        return cipher.blockSize();
    }

    /**
     * 加密任意长度明文：PKCS#7 填充后逐块 CBC 加密。
     *
     * @param plaintext 明文（可为空数组）
     * @param iv        初始向量，长度必须等于分组长度
     * @return 密文，长度为分组长度的整数倍
     */
    public byte[] encrypt(byte[] plaintext, byte[] iv) {
        requireIv(iv);
        int blockSize = blockSize();
        byte[] padded = pkcs7Pad(plaintext, blockSize);
        byte[] out = new byte[padded.length];
        byte[] previous = iv.clone();

        for (int offset = 0; offset < padded.length; offset += blockSize) {
            byte[] block = Arrays.copyOfRange(padded, offset, offset + blockSize);
            for (int i = 0; i < blockSize; i++) {
                block[i] ^= previous[i];
            }
            byte[] encrypted = cipher.encryptBlock(block);
            System.arraycopy(encrypted, 0, out, offset, blockSize);
            previous = encrypted;
        }
        return out;
    }

    /**
     * 解密 CBC 密文并去掉 PKCS#7 填充。
     *
     * @param ciphertext 密文，长度必须是分组长度的正整数倍
     * @param iv         初始向量，长度必须等于分组长度
     * @return 原始明文
     * @throws IllegalArgumentException 密文长度非法或末尾填充不合法
     */
    public byte[] decrypt(byte[] ciphertext, byte[] iv) {
        if (ciphertext == null) {
            throw new IllegalArgumentException("密文不能为 null");
        }
        requireIv(iv);
        int blockSize = blockSize();
        if (ciphertext.length == 0 || ciphertext.length % blockSize != 0) {
            throw new IllegalArgumentException(
                    "密文长度必须是 " + blockSize + " 的正整数倍，实际 " + ciphertext.length + " 字节");
        }
        byte[] out = new byte[ciphertext.length];
        byte[] previous = iv.clone();

        for (int offset = 0; offset < ciphertext.length; offset += blockSize) {
            byte[] block = Arrays.copyOfRange(ciphertext, offset, offset + blockSize);
            byte[] decrypted = cipher.decryptBlock(block);
            for (int i = 0; i < blockSize; i++) {
                decrypted[i] ^= previous[i];
            }
            System.arraycopy(decrypted, 0, out, offset, blockSize);
            previous = block;
        }
        return pkcs7Unpad(out, blockSize);
    }

    /** PKCS#7 填充：缺 n 字节就补 n 个值为 n 的字节（恰好整块时补一整块）。 */
    public static byte[] pkcs7Pad(byte[] data, int blockSize) {
        if (data == null) {
            throw new IllegalArgumentException("明文不能为 null");
        }
        if (blockSize < 1 || blockSize > 255) {
            throw new IllegalArgumentException("分组长度必须在 1..255 之间，实际 " + blockSize);
        }
        int pad = blockSize - data.length % blockSize;
        byte[] out = Arrays.copyOf(data, data.length + pad);
        for (int i = data.length; i < out.length; i++) {
            out[i] = (byte) pad;
        }
        return out;
    }

    /** 去掉 PKCS#7 填充；填充格式不对时抛 {@link IllegalArgumentException}。 */
    public static byte[] pkcs7Unpad(byte[] padded, int blockSize) {
        if (padded == null) {
            throw new IllegalArgumentException("带填充的数据不能为 null");
        }
        if (blockSize < 1 || blockSize > 255) {
            throw new IllegalArgumentException("分组长度必须在 1..255 之间，实际 " + blockSize);
        }
        if (padded.length == 0 || padded.length % blockSize != 0) {
            throw new IllegalArgumentException("带填充的数据长度必须是分组长度的正整数倍");
        }
        int pad = padded[padded.length - 1] & 0xff;
        if (pad < 1 || pad > blockSize) {
            throw new IllegalArgumentException("PKCS#7 填充非法：末字节 " + pad);
        }
        for (int i = padded.length - pad; i < padded.length; i++) {
            if ((padded[i] & 0xff) != pad) {
                throw new IllegalArgumentException("PKCS#7 填充非法：填充字节不一致");
            }
        }
        return Arrays.copyOf(padded, padded.length - pad);
    }

    private void requireIv(byte[] iv) {
        if (iv == null) {
            throw new IllegalArgumentException("IV 不能为 null");
        }
        if (iv.length != blockSize()) {
            throw new IllegalArgumentException(
                    "IV 长度必须是 " + blockSize() + " 字节，实际 " + iv.length + " 字节");
        }
    }
}
