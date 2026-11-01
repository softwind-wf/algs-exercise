package com.ds.crypto;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * 极简 DER（ASN.1 的可区分编码规则）编码器 + 反解析器。
 *
 * <p>存在的理由只有一个：X.509 证书就是一段 DER 字节，想"把证书拆开看"就必须懂 TLV
 * （Tag-Length-Value）。本类不追求完整实现 ASN.1（不支持 BER 不定长、不支持 SET OF 排序规则之外的
 * 全部约束），只覆盖一张 v3 证书里出现的类型：SEQUENCE / SET / INTEGER / BIT STRING / OCTET STRING /
 * OID / BOOLEAN / NULL / UTF8String / PrintableString / IA5String / UTCTime / GeneralizedTime
 * 以及 context-specific 标签 {@code [0]}、{@code [2]}、{@code [3]}、{@code [7]} 等。
 *
 * <p>每个 TLV 都是"标签 1 字节 + 长度 1~4 字节 + 内容 N 字节"：
 * <pre>
 *   30 82 03 21     30=SEQUENCE（0x20 位表示"里面还有 TLV"），82=长度用 2 字节，03 21=801
 *   02 01 02        02=INTEGER，01=长度 1，02=值 2（证书版本 v3，注意 v1 时该字段省略，默认 1）
 * </pre>
 *
 * <p>仅供教学演示使用，不要用于生产环境（生产请用 BouncyCastle 或 JDK 自带的
 * {@code CertificateFactory} / {@code X509Certificate}）。
 */
final class Der {

    static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** 常见的 OID 及其人类可读名字，仅用于让 dump 输出好读。 */
    private static final Map<String, String> OID_NAMES = new HashMap<String, String>();

    static {
        OID_NAMES.put("1.2.840.113549.1.1.1", "rsaEncryption");
        OID_NAMES.put("1.2.840.113549.1.1.11", "sha256WithRSAEncryption");
        OID_NAMES.put("1.2.840.113549.1.1.5", "sha1WithRSAEncryption");
        OID_NAMES.put("2.5.4.3", "CN 通用名");
        OID_NAMES.put("2.5.4.6", "C 国家");
        OID_NAMES.put("2.5.4.7", "L 城市");
        OID_NAMES.put("2.5.4.8", "ST 省/州");
        OID_NAMES.put("2.5.4.10", "O 组织");
        OID_NAMES.put("2.5.4.11", "OU 部门");
        OID_NAMES.put("2.5.29.14", "subjectKeyIdentifier 主体密钥标识");
        OID_NAMES.put("2.5.29.15", "keyUsage 密钥用途");
        OID_NAMES.put("2.5.29.17", "subjectAltName 主体备用名");
        OID_NAMES.put("2.5.29.19", "basicConstraints 基本约束");
        OID_NAMES.put("2.5.29.31", "cRLDistributionPoints 吊销列表地址");
        OID_NAMES.put("2.5.29.35", "authorityKeyIdentifier 签发者密钥标识");
        OID_NAMES.put("2.5.29.37", "extKeyUsage 扩展密钥用途");
        OID_NAMES.put("1.3.6.1.5.5.7.3.1", "serverAuth 服务器认证");
        OID_NAMES.put("1.3.6.1.5.5.7.3.2", "clientAuth 客户端认证");
        OID_NAMES.put("1.3.6.1.4.1.311.21.1", "微软 CA 版本");
    }

    private Der() {
    }

    // ==================================================================
    // 编码：拼 TLV
    // ==================================================================

    /** 拼一个 TLV：tag 1 字节 + DER 长度 + 内容。 */
    static byte[] tlv(int tag, byte[] content) {
        byte[] len = encodeLength(content.length);
        byte[] out = new byte[1 + len.length + content.length];
        out[0] = (byte) tag;
        System.arraycopy(len, 0, out, 1, len.length);
        System.arraycopy(content, 0, out, 1 + len.length, content.length);
        return out;
    }

    static byte[] seq(byte[]... parts) {
        return tlv(0x30, concat(parts));
    }

    static byte[] set(byte[]... parts) {
        return tlv(0x31, concat(parts));
    }

    /** {@code [n]} 显式标签（constructed），用于 version、extensions 这类"外面再包一层"的字段。 */
    static byte[] explicit(int n, byte[] content) {
        return tlv(0xA0 | n, content);
    }

    static byte[] integer(long value) {
        return integer(BigInteger.valueOf(value));
    }

    static byte[] integer(BigInteger value) {
        // BigInteger.toByteArray() 本身就是最短补码：正数高位为 1 时会自动补 0x00，正是 DER 要的。
        return tlv(0x02, value.toByteArray());
    }

    static byte[] oid(String dotted) {
        String[] parts = dotted.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("OID 至少要有两段：" + dotted);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBase128(out, Long.parseLong(parts[0]) * 40 + Long.parseLong(parts[1]));
        for (int i = 2; i < parts.length; i++) {
            writeBase128(out, Long.parseLong(parts[i]));
        }
        return tlv(0x06, out.toByteArray());
    }

    static byte[] nul() {
        return new byte[]{0x05, 0x00};
    }

    static byte[] bool(boolean value) {
        return new byte[]{0x01, 0x01, (byte) (value ? 0xFF : 0x00)};
    }

    static byte[] octetString(byte[] value) {
        return tlv(0x04, value);
    }

    /** BIT STRING，末尾不补位（只有整数个字节）。 */
    static byte[] bitString(byte[] value) {
        return bitString(0, value);
    }

    /** BIT STRING，{@code unusedBits} 是最后一个字节里没用到的位数（0~7）。 */
    static byte[] bitString(int unusedBits, byte[] value) {
        byte[] content = new byte[value.length + 1];
        content[0] = (byte) unusedBits;
        System.arraycopy(value, 0, content, 1, value.length);
        return tlv(0x03, content);
    }

    static byte[] utf8(String text) {
        return tlv(0x0C, text.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] printable(String text) {
        return tlv(0x13, text.getBytes(StandardCharsets.US_ASCII));
    }

    static byte[] ia5(String text) {
        return tlv(0x16, text.getBytes(StandardCharsets.US_ASCII));
    }

    /** UTCTime：两位年份。RFC 5280 规定 YY≥50 视为 19YY，所以 2050 年不能写成 "50..."。 */
    static byte[] utcTime(String text) {
        return tlv(0x17, text.getBytes(StandardCharsets.US_ASCII));
    }

    /** GeneralizedTime：四位年份，2050 年及以后必须用这个。 */
    static byte[] generalizedTime(String text) {
        return tlv(0x18, text.getBytes(StandardCharsets.US_ASCII));
    }

    static String utc(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US);
        format.setTimeZone(UTC);
        return format.format(date);
    }

    static String generalized(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
        format.setTimeZone(UTC);
        return format.format(date);
    }

    // ==================================================================
    // 反解析：拆 TLV
    // ==================================================================

    /** 一个 TLV 的头信息：标签、头长度（标签+长度字节）、内容长度。 */
    static final class Node {
        final int tag;
        final int headerLength;
        final int contentLength;

        Node(int tag, int headerLength, int contentLength) {
            this.tag = tag;
            this.headerLength = headerLength;
            this.contentLength = contentLength;
        }

        boolean isConstructed() {
            return (tag & 0x20) != 0;
        }

        String name() {
            switch (tag) {
                case 0x30: return "SEQUENCE";
                case 0x31: return "SET";
                case 0x02: return "INTEGER";
                case 0x03: return "BIT STRING";
                case 0x04: return "OCTET STRING";
                case 0x05: return "NULL";
                case 0x06: return "OID";
                case 0x01: return "BOOLEAN";
                case 0x0C: return "UTF8String";
                case 0x13: return "PrintableString";
                case 0x16: return "IA5String";
                case 0x17: return "UTCTime";
                case 0x18: return "GeneralizedTime";
                default:
                    // context-specific 类占高两位 10：0xA0|n 是"外面再包一层"（explicit），
                    // 0x80|n 是直接替换掉原类型的 implicit 标签（如 SAN 里的 [2] dNSName）。
                    if ((tag & 0xC0) == 0x80) {
                        String base = "[" + (tag & 0x1F) + "]";
                        return isConstructed() ? base : base + " implicit";
                    }
                    return "TAG 0x" + Integer.toHexString(tag);
            }
        }
    }

    static Node node(byte[] raw, int offset) {
        int tag = raw[offset] & 0xFF;
        int p = offset + 1;
        int first = raw[p++] & 0xFF;
        int length;
        if (first < 0x80) {
            length = first;
        } else {
            int count = first & 0x7F;
            length = 0;
            for (int i = 0; i < count; i++) {
                length = (length << 8) | (raw[p++] & 0xFF);
            }
        }
        return new Node(tag, p - offset, length);
    }

    /** 取一个完整 TLV 的内容区。 */
    static byte[] content(byte[] tlv) {
        Node node = node(tlv, 0);
        byte[] out = new byte[node.contentLength];
        System.arraycopy(tlv, node.headerLength, out, 0, node.contentLength);
        return out;
    }

    /** 把一个 TLV 的内容区按顺序切成子 TLV 列表（只对 constructed 有意义）。 */
    static List<byte[]> children(byte[] tlv) {
        List<byte[]> out = new ArrayList<byte[]>();
        Node node = node(tlv, 0);
        int p = node.headerLength;
        int end = node.headerLength + node.contentLength;
        while (p < end) {
            Node child = node(tlv, p);
            int total = child.headerLength + child.contentLength;
            byte[] slice = new byte[total];
            System.arraycopy(tlv, p, slice, 0, total);
            out.add(slice);
            p += total;
        }
        return out;
    }

    /** 把一个 DER 字节串按树形打印出来（缩进 + 标签 + 内容预览）。 */
    static String dump(byte[] der) {
        StringBuilder sb = new StringBuilder();
        Node node = node(der, 0);
        int total = node.headerLength + node.contentLength;
        byte[] slice = new byte[total];
        System.arraycopy(der, 0, slice, 0, total);
        dumpNode(slice, sb, 0);
        return sb.toString();
    }

    private static void dumpNode(byte[] tlv, StringBuilder sb, int depth) {
        Node node = node(tlv, 0);
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        sb.append(String.format("%-13s", Hex.toHex(slice(tlv, 0, node.headerLength))));
        sb.append(node.name());
        if (node.isConstructed()) {
            sb.append(" (").append(node.contentLength).append(" 字节)");
            sb.append('\n');
            for (byte[] child : children(tlv)) {
                dumpNode(child, sb, depth + 1);
            }
            return;
        }
        sb.append(" = ").append(preview(node.tag, slice(tlv, node.headerLength, node.contentLength)));
        sb.append('\n');
    }

    private static String preview(int tag, byte[] value) {
        switch (tag) {
            case 0x02:
                return new BigInteger(value).toString();
            case 0x01:
                return value.length > 0 && value[0] != 0 ? "TRUE" : "FALSE";
            case 0x05:
                return "(空)";
            case 0x06:
                return decodeOid(value);
            case 0x0C:
                return "\"" + new String(value, StandardCharsets.UTF_8) + "\"";
            case 0x13:
            case 0x16:
                return "\"" + new String(value, StandardCharsets.US_ASCII) + "\"";
            case 0x17:
            case 0x18:
                return "\"" + new String(value, StandardCharsets.US_ASCII) + "\"";
            case 0x03: {
                if (value.length == 0) {
                    return "(空)";
                }
                int unused = value[0] & 0xFF;
                byte[] bits = slice(value, 1, value.length - 1);
                return "未用位 " + unused + "，位串 " + clip(bits);
            }
            case 0x04:
                return clip(value);
            default:
                if ((tag & 0xC0) == 0x80) {
                    // context-specific primitive，如 [2] dNSName / [7] iPAddress
                    boolean printable = true;
                    for (byte b : value) {
                        if (b < 0x20 || b > 0x7E) {
                            printable = false;
                            break;
                        }
                    }
                    if (printable && value.length > 0) {
                        return "\"" + new String(value, StandardCharsets.US_ASCII) + "\"";
                    }
                }
                return clip(value);
        }
    }

    /** 十六进制预览：超过 12 字节就截断，避免一张证书刷屏。 */
    private static String clip(byte[] value) {
        if (value.length <= 12) {
            return Hex.toHex(value);
        }
        byte[] head = slice(value, 0, 12);
        return Hex.toHex(head) + "...（共 " + value.length + " 字节）";
    }

    static String decodeOid(byte[] value) {
        String oid = decodeOidRaw(value);
        String name = OID_NAMES.get(oid);
        return name == null ? oid : oid + "（" + name + "）";
    }

    /** 只解码成点分形式，不带名字（程序里做比较时用这个）。 */
    static String decodeOidRaw(byte[] value) {
        if (value.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        long first = value[0] & 0xFFL;
        if (first < 80) {
            sb.append(first / 40).append('.').append(first % 40);
        } else {
            sb.append("2.").append(first - 80);
        }
        long acc = 0;
        for (int i = 1; i < value.length; i++) {
            int b = value[i] & 0xFF;
            acc = (acc << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                sb.append('.').append(acc);
                acc = 0;
            }
        }
        return sb.toString();
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    private static byte[] encodeLength(int length) {
        if (length < 0x80) {
            return new byte[]{(byte) length};
        }
        if (length < 0x100) {
            return new byte[]{(byte) 0x81, (byte) length};
        }
        if (length < 0x10000) {
            return new byte[]{(byte) 0x82, (byte) (length >>> 8), (byte) length};
        }
        return new byte[]{(byte) 0x83, (byte) (length >>> 16), (byte) (length >>> 8), (byte) length};
    }

    private static void writeBase128(ByteArrayOutputStream out, long value) {
        int groups = 1;
        long v = value;
        while (v >= 0x80) {
            v >>>= 7;
            groups++;
        }
        for (int i = groups - 1; i >= 0; i--) {
            int b = (int) ((value >>> (7 * i)) & 0x7F);
            if (i != 0) {
                b |= 0x80;
            }
            out.write(b);
        }
    }

    static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] part : parts) {
            total += part.length;
        }
        byte[] out = new byte[total];
        int p = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, p, part.length);
            p += part.length;
        }
        return out;
    }

    static byte[] slice(byte[] source, int offset, int length) {
        byte[] out = new byte[length];
        System.arraycopy(source, offset, out, 0, length);
        return out;
    }
}
