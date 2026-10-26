package com.ds.crypto;

/**
 * 十六进制与字节数组互转的小工具（兼容 JDK 8，不依赖 {@code java.util.HexFormat}）。
 *
 * <p>统一使用小写十六进制，输出不带空格；解析时忽略空白、允许大写。
 */
public final class Hex {

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private Hex() {
    }

    /** 字节数组转小写十六进制串，例如 {@code {0x00, (byte) 0xff}} → {@code "00ff"}。 */
    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes 不能为 null");
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(DIGITS[(b >>> 4) & 0x0f]).append(DIGITS[b & 0x0f]);
        }
        return sb.toString();
    }

    /** 十六进制串转字节数组；长度必须为偶数，允许空格分隔。 */
    public static byte[] fromHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("hex 不能为 null");
        }
        StringBuilder compact = new StringBuilder(hex.length());
        for (int i = 0; i < hex.length(); i++) {
            char ch = hex.charAt(i);
            if (!Character.isWhitespace(ch)) {
                compact.append(ch);
            }
        }
        if ((compact.length() & 1) != 0) {
            throw new IllegalArgumentException("十六进制串长度必须为偶数：" + hex);
        }
        byte[] out = new byte[compact.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) ((digit(compact.charAt(2 * i)) << 4) | digit(compact.charAt(2 * i + 1)));
        }
        return out;
    }

    private static int digit(char ch) {
        int value = Character.digit(ch, 16);
        if (value < 0) {
            throw new IllegalArgumentException("非法十六进制字符：" + ch);
        }
        return value;
    }
}
