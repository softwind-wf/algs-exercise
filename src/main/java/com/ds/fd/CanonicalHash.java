package com.ds.fd;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * 规范化工具：把 F⁺ 集合序列化成稳定的字符串并算 SHA-256。
 *
 * <p>用途是差分测试 —— 1701 条依赖不必逐个列出，用"排序后拼接再哈希"
 * 就能让 Java 与 Python 两个实现做逐位比对。
 *
 * <p>排序规则：先按左边属性集字典序，再按右边；两个实现必须采用同一规则，
 * 否则哈希对不上。属性集一律用 {@link FdCalculator#join} 的紧凑形式（如 {@code ABCH}）。
 */
public final class CanonicalHash {

    /** FD 输出序：先左边、再右边。 */
    public static final Comparator<FunctionalDependency> FD_ORDER =
            new Comparator<FunctionalDependency>() {
                @Override
                public int compare(FunctionalDependency a, FunctionalDependency b) {
                    int c = FdCalculator.join(a.determinant()).compareTo(FdCalculator.join(b.determinant()));
                    return c != 0 ? c : FdCalculator.join(a.dependent()).compareTo(FdCalculator.join(b.dependent()));
                }
            };

    private CanonicalHash() {
    }

    /** 排序后的规范文本（每行一条 FD，行尾 \n）。 */
    public static String canonicalize(Set<FunctionalDependency> fds) {
        TreeSet<FunctionalDependency> sorted = new TreeSet<FunctionalDependency>(FD_ORDER);
        sorted.addAll(fds);
        StringBuilder sb = new StringBuilder();
        for (FunctionalDependency fd : sorted) {
            sb.append(fd).append('\n');
        }
        return sb.toString();
    }

    public static String sha256(Set<FunctionalDependency> fds) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(canonicalize(fds).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }
}
