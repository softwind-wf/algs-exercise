package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 无损分解（lossless-join decomposition）判定，对应教材 8.5 节。
 *
 * <p>直观定义：把 R 拆成 R1,…,Rn 再自然连接回来，如果结果恰好等于 R（不多行、不少行），
 * 这个分解就是无损的。多出来的行叫伪元组（spurious tuple），它们来自"本不相干的行被公共属性配上对"。
 *
 * <p>两种判定：
 * <ol>
 *   <li><b>二元分解定理</b>：R 拆成 R1、R2（R1 ∪ R2 = R），
 *       分解无损 <b>⟺ R1 ∩ R2 → R1 或 R1 ∩ R2 → R2</b>。
 *       用闭包判：公共属性集的闭包盖住 R1（或 R2）即可。直觉是"公共属性得是某一侧的超键"，
 *       否则两边的行会按公共属性乱配。</li>
 *   <li><b>n 元分解</b>：二分定理不能连用（无损不具有传递性），要用图 8-10 的表格法（chase）：
 *       建一张"行 = 子模式、列 = 属性"的表，Ri 含的属性填区分变量 a，否则填非区分变量 b；
 *       反复用 F 中的 FD 让"左部相等的行"在右部也相等；若某一行变成全 a，则无损。</li>
 * </ol>
 *
 * <p>注意"无损"与"保持依赖"是两个独立要求：一个分解可以无损但丢依赖（教材例 2），
 * 也可以保持依赖但有损（无公共键的拆分）。
 */
public final class LosslessJoin {

    private LosslessJoin() {
    }

    // =====================================================================
    // 1. 二元分解：R1 ∩ R2 → R1 或 R1 ∩ R2 → R2
    // =====================================================================

    /** 二元分解是否无损（要求 r1 ∪ r2 = allAttrs）。 */
    public static boolean isBinaryLossless(Set<String> allAttrs, Set<String> r1, Set<String> r2,
                                           List<FunctionalDependency> fds) {
        return checkBinary(allAttrs, r1, r2, fds).lossless();
    }

    /** 二元分解判定，返回公共属性、闭包与两个方向的蕴含结果，便于解释"为什么有损"。 */
    public static BinaryResult checkBinary(Set<String> allAttrs, Set<String> r1, Set<String> r2,
                                           List<FunctionalDependency> fds) {
        requireCoverage(allAttrs, java.util.Arrays.asList(r1, r2));
        Set<String> common = new TreeSet<String>(r1);
        common.retainAll(r2);
        Set<String> closure = FdCalculator.attributeClosure(common, fds);
        boolean impliesR1 = closure.containsAll(r1);
        boolean impliesR2 = closure.containsAll(r2);
        return new BinaryResult(new TreeSet<String>(r1), new TreeSet<String>(r2),
                common, closure, impliesR1, impliesR2);
    }

    /** 二元分解的判定明细。 */
    public static final class BinaryResult {
        private final Set<String> r1;
        private final Set<String> r2;
        private final Set<String> common;
        private final Set<String> closure;
        private final boolean impliesR1;
        private final boolean impliesR2;

        BinaryResult(Set<String> r1, Set<String> r2, Set<String> common, Set<String> closure,
                     boolean impliesR1, boolean impliesR2) {
            this.r1 = Collections.unmodifiableSet(r1);
            this.r2 = Collections.unmodifiableSet(r2);
            this.common = Collections.unmodifiableSet(common);
            this.closure = Collections.unmodifiableSet(closure);
            this.impliesR1 = impliesR1;
            this.impliesR2 = impliesR2;
        }

        public Set<String> r1() {
            return r1;
        }

        public Set<String> r2() {
            return r2;
        }

        /** R1 ∩ R2。 */
        public Set<String> common() {
            return common;
        }

        /** 公共属性集的闭包 (R1 ∩ R2)⁺。 */
        public Set<String> closure() {
            return closure;
        }

        /** 公共属性是否决定 R1。 */
        public boolean impliesR1() {
            return impliesR1;
        }

        /** 公共属性是否决定 R2。 */
        public boolean impliesR2() {
            return impliesR2;
        }

        public boolean lossless() {
            return impliesR1 || impliesR2;
        }

        @Override
        public String toString() {
            String verdict = lossless() ? "无损" : "有损";
            StringBuilder sb = new StringBuilder();
            sb.append(FdCalculator.joinWithComma(common)).append("+ = ")
                    .append(FdCalculator.joinWithComma(closure))
                    .append("  => ").append(verdict);
            if (!lossless()) {
                sb.append(" (公共属性推不出 R1 也推不出 R2)");
            }
            return sb.toString();
        }
    }

    // =====================================================================
    // 2. n 元分解：图 8-10 表格法（chase）
    // =====================================================================

    /** n 元分解是否无损。 */
    public static boolean isLossless(Set<String> allAttrs, List<Set<String>> decomposition,
                                     List<FunctionalDependency> fds) {
        return checkChase(allAttrs, decomposition, fds).lossless();
    }

    /** n 元分解的表格法判定：返回是否无损、最终表格与追踪步骤。 */
    public static ChaseResult checkChase(Set<String> allAttrs, List<Set<String>> decomposition,
                                         List<FunctionalDependency> fds) {
        requireCoverage(allAttrs, decomposition);
        List<String> cols = new ArrayList<String>(new TreeSet<String>(allAttrs));
        int rowCount = decomposition.size();
        int colCount = cols.size();

        String[][] table = new String[rowCount][colCount];
        for (int i = 0; i < rowCount; i++) {
            for (int c = 0; c < colCount; c++) {
                String attr = cols.get(c);
                table[i][c] = decomposition.get(i).contains(attr)
                        ? distinguished(attr)
                        : "b" + i + "_" + attr;
            }
        }

        List<String> steps = new ArrayList<String>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (FunctionalDependency fd : fds) {
                for (int i = 0; i < rowCount; i++) {
                    for (int j = i + 1; j < rowCount; j++) {
                        if (!agreeOn(table, i, j, fd.determinant(), cols)) {
                            continue;
                        }
                        for (String attr : fd.dependent()) {
                            int c = cols.indexOf(attr);
                            if (unify(table, i, j, c)) {
                                changed = true;
                                steps.add("应用 " + fd + ": 第 " + (i + 1) + "、"
                                        + (j + 1) + " 行的 " + attr + " 列统一");
                            }
                        }
                    }
                }
            }
        }

        List<Set<String>> rows = new ArrayList<Set<String>>();
        for (int i = 0; i < rowCount; i++) {
            Set<String> row = new LinkedHashSet<String>();
            Collections.addAll(row, table[i]);
            rows.add(row);
        }
        boolean lossless = false;
        for (int i = 0; i < rowCount && !lossless; i++) {
            lossless = true;
            for (int c = 0; c < colCount; c++) {
                if (!isDistinguished(table[i][c])) {
                    lossless = false;
                    break;
                }
            }
        }
        return new ChaseResult(lossless, rows, steps);
    }

    /** n 元分解的判定结果：是否无损、最终表格（每行一个子模式的符号）、追踪步骤。 */
    public static final class ChaseResult {
        private final boolean lossless;
        private final List<Set<String>> table;
        private final List<String> steps;

        ChaseResult(boolean lossless, List<Set<String>> table, List<String> steps) {
            this.lossless = lossless;
            this.table = Collections.unmodifiableList(table);
            this.steps = Collections.unmodifiableList(steps);
        }

        public boolean lossless() {
            return lossless;
        }

        /** 最终表格：每行是某个子模式在各属性列上的符号（a_* 为区分变量）。 */
        public List<Set<String>> table() {
            return table;
        }

        /** 每次统一符号的记录，空表示没用到任何 FD 就结束了（通常是有损或平凡分解）。 */
        public List<String> steps() {
            return steps;
        }
    }

    // =====================================================================
    // 工具
    // =====================================================================

    private static String distinguished(String attr) {
        return "a_" + attr;
    }

    private static boolean isDistinguished(String symbol) {
        return symbol.startsWith("a_");
    }

    private static boolean agreeOn(String[][] table, int i, int j, Set<String> attrs,
                                   List<String> cols) {
        for (String attr : attrs) {
            int c = cols.indexOf(attr);
            if (c < 0) {
                return false;
            }
            if (!table[i][c].equals(table[j][c])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把第 i、j 行在 c 列的符号统一：优先保留区分变量；两边都是 b 时保留第 i 行的。
     * 统一时把全表中等于被丢弃符号的单元格都替换掉 —— 符号相等表示"这两格必然相等"，
     * 只改这两格会丢掉之前建立的等式。
     *
     * @return 是否真的发生了替换
     */
    private static boolean unify(String[][] table, int i, int j, int c) {
        String left = table[i][c];
        String right = table[j][c];
        if (left.equals(right)) {
            return false;
        }
        String keep = (isDistinguished(left) || !isDistinguished(right)) ? left : right;
        String drop = keep.equals(left) ? right : left;
        for (int r = 0; r < table.length; r++) {
            for (int col = 0; col < table[r].length; col++) {
                if (table[r][col].equals(drop)) {
                    table[r][col] = keep;
                }
            }
        }
        return true;
    }

    /** 校验分解确实覆盖 allAttrs：并集必须等于 R，且不能含 R 之外的属性。 */
    private static void requireCoverage(Set<String> allAttrs, List<Set<String>> decomposition) {
        if (decomposition.isEmpty()) {
            throw new IllegalArgumentException("分解不能为空");
        }
        Set<String> union = new TreeSet<String>();
        Set<String> outside = new TreeSet<String>();
        for (Set<String> part : decomposition) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("子模式不能为空集");
            }
            union.addAll(part);
            for (String attr : part) {
                if (!allAttrs.contains(attr)) {
                    outside.add(attr);
                }
            }
        }
        if (!outside.isEmpty()) {
            throw new IllegalArgumentException("分解包含 R 之外的属性: " + outside);
        }
        if (!union.equals(new TreeSet<String>(allAttrs))) {
            Set<String> missing = new TreeSet<String>(allAttrs);
            missing.removeAll(union);
            throw new IllegalArgumentException("分解未覆盖全部属性, 缺少: " + missing);
        }
    }
}
