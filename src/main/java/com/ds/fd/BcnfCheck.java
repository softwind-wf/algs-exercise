package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * BCNF 判定，对应教材 8.5 节（中文版第 198 页"BCNF 的判定方法"）。
 *
 * <p>定义与判据：关系模式 R 属于 BCNF ⟺ F⁺ 中每条非平凡函数依赖 α→β（α, β ⊆ R）的决定因素 α
 * 都是 R 的超键，即 α⁺ ⊇ R。
 *
 * <p>本类按教材那一页的三句话给三段判定：
 * <ol>
 *   <li>{@link #checkRelation} —— <b>未分解的 R</b>：只检查<b>给定集合 F</b> 里的每一条非平凡依赖，
 *       算 α⁺ 看是否 ⊇ R。教材："检查关系模式 R 是否属于 BCNF，仅须检查给定集合 F 中的函数依赖
 *       是否违反 BCNF 就够了，不用检查 F⁺ 中的所有函数依赖"。</li>
 *   <li>{@link #checkRelationByFPlus} —— 同一结论的"笨办法"：先物化 F⁺ 再逐条判，用于验证上一条定理；
 *       |F⁺| 是指数级，只适合小例子。</li>
 *   <li>{@link #checkProjected} —— <b>分解后的子模式 Ri</b>：只看 F <b>不再够用</b>。对 Ri 的每个属性
 *       子集 α 算 F 下的闭包 α⁺，要求"α⁺ 不含 Ri−α 的任何属性"或"α⁺ ⊇ Ri"二者至少一个成立；
 *       都违反时，见证依赖 α→(α⁺−α)∩Ri 属于 F⁺ 但不一定属于 F，且 α 不是 Ri 的超键 —— 这就是教材
 *       第 198 页给的判定方法。</li>
 *   <li>{@link #checkProjectedByFPlus} —— 子模式的"物化 Π_Ri(F⁺)"对照实现，用来交叉验证第 3 条。</li>
 *   <li>{@link #checkDecomposition} / {@link #isBcnfDecomposition} —— 对整个分解 {R1,…,Rn}
 *       逐个判，全 BCNF 才算"BCNF 分解"。</li>
 * </ol>
 *
 * <p><b>为什么未分解时只看 F 就够（书上的定理，这里补一条证明）</b>：
 * 设 F 中没有违反 BCNF 的依赖，即 F 中每条非平凡依赖的左部都是 R 的超键。任取属性集 α，
 * 若 α⁺ ≠ α，则图 8-8 的闭包算法在某一轮用了某条 X→Y，其中 X ⊆ 当时的 result ⊆ α⁺。
 * X 是超键 ⇒ X⁺ = R；又 α⁺ ⊇ X，闭包单调 ⇒ (α⁺)⁺ ⊇ X⁺ = R；而闭包幂等 (α⁺)⁺ = α⁺，
 * 故 α⁺ = R。也就是说：在"F 无违反项"的前提下，任何 α 的闭包只能是 α 本身或 R。
 * 于是 F⁺ 中每条非平凡依赖 α→β（β ⊄ α 但 β ⊆ α⁺）都逼出 α⁺ ≠ α ⇒ α⁺ = R ⇒ α 是超键，
 * 所以 F⁺ 里也不会有违反项。
 *
 * <p><b>为什么分解后只看 F 就错（教材 198 页的反例）</b>：
 * R(A,B,C,D,E)、F = {A→B, BC→D}，拆成 R1(A,B) 与 R2(A,C,D,E)。
 * F 中<b>没有一条</b>依赖的左右部都落在 R2 内（A→B 的 B、BC→D 的 B 都不在 R2），
 * 按"只看 F"会误判 R2 属于 BCNF。但 F⁺ 含有 AC→D（A→B 增广得 AC→BC，再与 BC→D 传递，
 * 即伪传递律），它在 R2 内非平凡、左部 AC 在 R2 内不是超键（AC⁺ = ABCD，缺 E），
 * 所以 R2 <b>不属于</b> BCNF。结论：判分解后的 Ri 时，必须看 F⁺ 在 Ri 上的投影。
 *
 * <p><b>子集判定法为什么对（两个方向）</b>：
 * <ol>
 *   <li><b>它报出的都是真违反</b>：α ⊆ Ri 时，β = (α⁺−α)∩Ri ⊆ α⁺ 且 β 非空，按闭包定义
 *       α→β ∈ F⁺；β ∩ α = ∅ 故非平凡；β ⊆ Ri − α 且 α⁺ 不含 Ri 的全部属性，故 α 不是 Ri 的超键。
 *       三条合起来正是"Ri 违反 BCNF"的定义，且 α→β 的左右部都落在 Ri 内。</li>
 *   <li><b>真违反都会被报出</b>：若 Π_Ri(F⁺) 中存在非平凡依赖 α'→β' 且 α' 不是 Ri 的超键，
 *       就取 α = α'：β' ⊆ α'⁺ 且 β' ⊆ Ri、β' ⊄ α'，于是 (α⁺−α)∩Ri ⊇ β' 非空；
 *       又 α⁺ 不含 Ri 的全部属性（否则 α' 是超键），条件被违反，α' 会被枚举到。</li>
 * </ol>
 * 所以"对 Ri 的每个子集 α 都通过上述两条之一" ⟺ "Ri 属于 BCNF"。
 * 复杂度：枚举 2^|Ri| 个子集，每个算一次闭包 O(|Ri|·|F|)；物化 F⁺ 则是指数级的条数。
 */
public final class BcnfCheck {

    private BcnfCheck() {
    }

    // =====================================================================
    // 1. 未分解的关系 R：只看给定集合 F
    // =====================================================================

    /**
     * 判定<b>未分解</b>的关系模式 R 是否属于 BCNF：逐条检查 F 中的非平凡依赖 α→β，
     * 算 α⁺，只要有一条不包含 R（α 不是超键）就判不属于 BCNF。
     *
     * <p>平凡依赖（β ⊆ α）一律跳过：它们由自反律恒成立，永远不会违反 BCNF。
     * 空 F 自然返回"属于 BCNF"（没有任何非平凡依赖可违反）。
     *
     * @throws IllegalArgumentException 关系模式为空，或 F 中出现了 R 之外的属性
     */
    public static CheckResult checkRelation(Set<String> relation, List<FunctionalDependency> fds) {
        requireRelation(relation, "关系模式");
        requireAttrsWithin(fds, relation, "F");

        List<Violation> violations = new ArrayList<Violation>();
        int checked = 0;
        for (FunctionalDependency fd : fds) {
            if (fd.isTrivial()) {
                continue;
            }
            checked++;
            Set<String> closure = FdCalculator.attributeClosure(fd.determinant(), fds);
            Set<String> missing = new TreeSet<String>(relation);
            missing.removeAll(closure);
            if (!missing.isEmpty()) {
                violations.add(new Violation(fd, closure, missing, true));
            }
        }
        return new CheckResult(relation, violations, checked,
                Collections.<FunctionalDependency>emptySet());
    }

    /**
     * 未分解关系的"笨办法"判定：先物化 F⁺，再对其中左右部都落在 R 内的每条非平凡依赖判左部是否超键。
     *
     * <p>存在的意义是对照与验证：{@link #checkRelation} 只看 F 就够了（教材的定理），
     * 两条路线对"是否 BCNF"必须给出相同结论；但 F⁺ 里的违反项可能比 F 中多
     * （例如 R 已违反时，F⁺ 会额外报出由传递律导出的依赖），所以违反项<b>条数</b>不必相等。
     */
    public static CheckResult checkRelationByFPlus(Set<String> relation, List<FunctionalDependency> fds) {
        requireRelation(relation, "关系模式");
        requireAttrsWithin(fds, relation, "F");
        Set<FunctionalDependency> projected =
                FdCalculator.closureByAttributeClosures(relation, fds);
        return judgeProjected(relation, projected, fds);
    }

    // =====================================================================
    // 2. 分解后的子模式 Ri：必须看 F⁺ 的投影
    // =====================================================================

    /**
     * 判定<b>分解后</b>的子模式 Ri 是否属于 BCNF（教材 198 页的判定方法）。
     *
     * <p>对 Ri 的每一个非空真子集 α（α = Ri 恒为超键、α = ∅ 恒通过，故省去）：
     * <pre>
     * 计算 α⁺ (在原来的 F 下)
     * 若 α⁺ ⊇ Ri                  -> α 是 Ri 的超键，通过
     * 否则若 α⁺ ∩ (Ri − α) = ∅    -> α 推不出 Ri 里的任何新属性，通过
     * 否则                         -> 违反！见证依赖 α → (α⁺ − α) ∩ Ri
     * </pre>
     *
     * <p>见证依赖一定属于 F⁺（右边 ⊆ α⁺），但可能不在 F 中 —— 教材的 AC→D 就是这种。
     * 结果里用 {@link Violation#inGivenSet()} 标出这一点，正好解释"为什么只看 F 会漏判"。
     *
     * @param subRelation Ri（不能为空）
     * @param allAttrs    原关系 R 的全部属性，Ri ⊆ R
     * @param fds         原依赖集 F（可以包含 Ri 之外的属性，这正是分解后的常态）
     */
    public static CheckResult checkProjected(Set<String> subRelation, Set<String> allAttrs,
                                             List<FunctionalDependency> fds) {
        requireSubRelation(subRelation, allAttrs);
        requireAttrsWithin(fds, allAttrs, "F");

        Set<FunctionalDependency> given = new LinkedHashSet<FunctionalDependency>(fds);
        List<Violation> violations = new ArrayList<Violation>();
        int checked = 0;
        for (Set<String> alpha : FdCalculator.subsets(subRelation)) {
            if (alpha.size() == subRelation.size()) {
                continue;                       // α = Ri：α⁺ ⊇ α = Ri，恒为超键
            }
            checked++;
            Set<String> closure = FdCalculator.attributeClosure(alpha, fds);
            if (closure.containsAll(subRelation)) {
                continue;                       // α 是 Ri 的超键
            }
            Set<String> beta = new TreeSet<String>(closure);
            beta.retainAll(subRelation);         // α⁺ ∩ Ri
            beta.removeAll(alpha);               // (α⁺ − α) ∩ Ri
            if (beta.isEmpty()) {
                continue;                       // α 推不出 Ri 内的新属性
            }
            Set<String> missing = new TreeSet<String>(subRelation);
            missing.removeAll(closure);          // Ri − α⁺
            FunctionalDependency witness = new FunctionalDependency(alpha, beta);
            violations.add(new Violation(witness, closure, missing, given.contains(witness)));
        }
        return new CheckResult(subRelation, violations, checked,
                Collections.<FunctionalDependency>emptySet());
    }

    /**
     * 子模式判定的"物化 F⁺ 投影"对照实现：取 F⁺ 中左右部都落在 Ri 内的依赖（即 Π_Ri(F⁺)），
     * 对其中每条非平凡依赖 α→β 判 α⁺ 是否 ⊇ Ri。
     *
     * <p>与 {@link #checkProjected} 的结论必须一致（子集法只是把"枚举 F⁺ 的依赖"换成了
     * "枚举 Ri 的属性子集"）；本方法把 Π_Ri(F⁺) 物化出来返回在 {@link CheckResult#projected()} 里，
     * 便于看到"F 里没有、F⁺ 里才有"的那条见证依赖。
     */
    public static CheckResult checkProjectedByFPlus(Set<String> subRelation, Set<String> allAttrs,
                                                    List<FunctionalDependency> fds) {
        requireSubRelation(subRelation, allAttrs);
        requireAttrsWithin(fds, allAttrs, "F");

        Set<FunctionalDependency> fplus = FdCalculator.closureByAttributeClosures(allAttrs, fds);
        Set<FunctionalDependency> projected = new LinkedHashSet<FunctionalDependency>();
        for (FunctionalDependency fd : fplus) {
            if (subRelation.containsAll(fd.determinant()) && subRelation.containsAll(fd.dependent())) {
                projected.add(fd);
            }
        }
        return judgeProjected(subRelation, projected, fds);
    }

    // =====================================================================
    // 3. 整个分解
    // =====================================================================

    /**
     * 判定分解 D = {R1,…,Rn} 是不是"BCNF 分解"：每个 Ri 都用 {@link #checkProjected} 判一遍，
     * 全部属于 BCNF 才算。
     */
    public static DecompositionResult checkDecomposition(Set<String> allAttrs,
                                                         List<Set<String>> decomposition,
                                                         List<FunctionalDependency> fds) {
        requireDecomposition(allAttrs, decomposition);
        requireAttrsWithin(fds, allAttrs, "F");

        List<CheckResult> parts = new ArrayList<CheckResult>();
        for (Set<String> ri : decomposition) {
            parts.add(checkProjected(ri, allAttrs, fds));
        }
        return new DecompositionResult(allAttrs, decomposition, parts);
    }

    /** 便捷：未分解的 R 是否属于 BCNF。 */
    public static boolean isBcnf(Set<String> relation, List<FunctionalDependency> fds) {
        return checkRelation(relation, fds).bcnf();
    }

    /** 便捷：整个分解是否每个子模式都属于 BCNF。 */
    public static boolean isBcnfDecomposition(Set<String> allAttrs, List<Set<String>> decomposition,
                                              List<FunctionalDependency> fds) {
        return checkDecomposition(allAttrs, decomposition, fds).bcnf();
    }

    // =====================================================================
    // 4. 结果载体
    // =====================================================================

    /** 一次判定的结果：被判的属性集、违反项、检查对象个数、（可能物化的）F⁺ 投影。 */
    public static final class CheckResult {
        private final Set<String> relation;
        private final List<Violation> violations;
        private final int checked;
        private final Set<FunctionalDependency> projected;

        CheckResult(Set<String> relation, List<Violation> violations, int checked,
                    Set<FunctionalDependency> projected) {
            this.relation = Collections.unmodifiableSet(new TreeSet<String>(relation));
            this.violations = Collections.unmodifiableList(violations);
            this.checked = checked;
            this.projected = Collections.unmodifiableSet(new LinkedHashSet<FunctionalDependency>(projected));
        }

        /** 被判的属性集：未分解时是 R，分解后是某个 Ri。 */
        public Set<String> relation() {
            return relation;
        }

        /** 是否属于 BCNF（没有违反项）。 */
        public boolean bcnf() {
            return violations.isEmpty();
        }

        public List<Violation> violations() {
            return violations;
        }

        /** 违反项的决定因素集合，用于比较两种判定路线是否报出同一批"肇事左部"。 */
        public Set<Set<String>> violatingDeterminants() {
            Set<Set<String>> out = new LinkedHashSet<Set<String>>();
            for (Violation v : violations) {
                out.add(v.fd().determinant());
            }
            return out;
        }

        /**
         * 实际检查的对象数：{@link #checkRelation} 是 F 中非平凡依赖条数；
         * {@link #checkProjected} 是 Ri 的非空真子集个数；F⁺ 类方法是投影后非平凡依赖条数。
         */
        public int checkedCount() {
            return checked;
        }

        /** F⁺ 在 Ri 上的投影（仅 F⁺ 类方法填充；子集法与只看 F 的方法为空集）。 */
        public Set<FunctionalDependency> projected() {
            return projected;
        }

        @Override
        public String toString() {
            if (bcnf()) {
                return FdCalculator.join(relation) + ": 属于 BCNF";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(FdCalculator.join(relation)).append(": 不属于 BCNF, 违反项 ");
            for (int i = 0; i < violations.size(); i++) {
                sb.append(i == 0 ? "" : "; ").append(violations.get(i));
            }
            return sb.toString();
        }
    }

    /** 一条违反 BCNF 的依赖：左边不是超键，closure 为左部闭包，missing 为闭包还缺的属性。 */
    public static final class Violation {
        private final FunctionalDependency fd;
        private final Set<String> closure;
        private final Set<String> missing;
        private final boolean inGivenSet;

        Violation(FunctionalDependency fd, Set<String> closure, Set<String> missing, boolean inGivenSet) {
            this.fd = fd;
            this.closure = Collections.unmodifiableSet(new TreeSet<String>(closure));
            this.missing = Collections.unmodifiableSet(new TreeSet<String>(missing));
            this.inGivenSet = inGivenSet;
        }

        public FunctionalDependency fd() {
            return fd;
        }

        /** 左部在原 F 下的闭包 α⁺。 */
        public Set<String> closure() {
            return closure;
        }

        /** 闭包里还缺的属性：对未分解的 R 是 R−α⁺；对 Ri 是 Ri−α⁺。 */
        public Set<String> missing() {
            return missing;
        }

        /** 这条依赖是否就在给定的 F 里；false 表示它只在 F⁺ 中（分解后判定的典型情形）。 */
        public boolean inGivenSet() {
            return inGivenSet;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(FdCalculator.joinWithComma(fd.determinant())).append(" -> ")
                    .append(FdCalculator.joinWithComma(fd.dependent()))
                    .append(" 违反 BCNF (")
                    .append(FdCalculator.joinWithComma(fd.determinant())).append("+ = ")
                    .append(FdCalculator.joinWithComma(closure))
                    .append(", 还缺 ").append(FdCalculator.joinWithComma(missing)).append(")");
            if (!inGivenSet) {
                sb.append(" [不在 F 中, 只出现在 F+ 里]");
            }
            return sb.toString();
        }
    }

    /** 整个分解的判定结果：每个子模式一条 {@link CheckResult}。 */
    public static final class DecompositionResult {
        private final Set<String> allAttrs;
        private final List<Set<String>> decomposition;
        private final List<CheckResult> parts;

        DecompositionResult(Set<String> allAttrs, List<Set<String>> decomposition,
                            List<CheckResult> parts) {
            this.allAttrs = Collections.unmodifiableSet(new TreeSet<String>(allAttrs));
            List<Set<String>> copy = new ArrayList<Set<String>>();
            for (Set<String> part : decomposition) {
                copy.add(Collections.unmodifiableSet(new TreeSet<String>(part)));
            }
            this.decomposition = Collections.unmodifiableList(copy);
            this.parts = Collections.unmodifiableList(parts);
        }

        public Set<String> allAttrs() {
            return allAttrs;
        }

        public List<Set<String>> decomposition() {
            return decomposition;
        }

        public List<CheckResult> parts() {
            return parts;
        }

        /** 是否每个子模式都属于 BCNF。 */
        public boolean bcnf() {
            for (CheckResult part : parts) {
                if (!part.bcnf()) {
                    return false;
                }
            }
            return true;
        }

        /** 不属于 BCNF 的那些子模式（用于报告"哪一块要再拆"）。 */
        public List<CheckResult> violatingParts() {
            List<CheckResult> out = new ArrayList<CheckResult>();
            for (CheckResult part : parts) {
                if (!part.bcnf()) {
                    out.add(part);
                }
            }
            return out;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(FdCalculator.join(allAttrs)).append(" 的分解: ");
            for (int i = 0; i < parts.size(); i++) {
                sb.append(i == 0 ? "" : " | ").append(parts.get(i));
            }
            sb.append(" => ").append(bcnf() ? "BCNF 分解" : "不是 BCNF 分解");
            return sb.toString();
        }
    }

    // =====================================================================
    // 5. F⁺ 投影的统一判定 + 入参校验
    // =====================================================================

    /** 对"已经限定到 Ri 的依赖集"逐条判左部是否 Ri 的超键。 */
    private static CheckResult judgeProjected(Set<String> relation,
                                              Set<FunctionalDependency> projected,
                                              List<FunctionalDependency> fds) {
        Set<FunctionalDependency> given = new LinkedHashSet<FunctionalDependency>(fds);
        List<Violation> violations = new ArrayList<Violation>();
        int checked = 0;
        for (FunctionalDependency fd : projected) {
            if (fd.isTrivial()) {
                continue;
            }
            checked++;
            Set<String> closure = FdCalculator.attributeClosure(fd.determinant(), fds);
            Set<String> missing = new TreeSet<String>(relation);
            missing.removeAll(closure);
            if (!missing.isEmpty()) {
                violations.add(new Violation(fd, closure, missing, given.contains(fd)));
            }
        }
        return new CheckResult(relation, violations, checked, projected);
    }

    private static void requireRelation(Set<String> relation, String name) {
        if (relation == null || relation.isEmpty()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }

    private static void requireSubRelation(Set<String> subRelation, Set<String> allAttrs) {
        requireRelation(subRelation, "子模式");
        if (allAttrs == null || allAttrs.isEmpty()) {
            throw new IllegalArgumentException("关系模式不能为空");
        }
        for (String attr : subRelation) {
            if (!allAttrs.contains(attr)) {
                throw new IllegalArgumentException("子模式包含 R 之外的属性: " + attr);
            }
        }
    }

    private static void requireAttrsWithin(List<FunctionalDependency> fds, Set<String> universe,
                                           String name) {
        for (FunctionalDependency fd : fds) {
            for (String attr : fd.determinant()) {
                if (!universe.contains(attr)) {
                    throw new IllegalArgumentException(
                            name + " 中的属性 " + attr + " 不在属性集 "
                                    + FdCalculator.join(universe) + " 内");
                }
            }
            for (String attr : fd.dependent()) {
                if (!universe.contains(attr)) {
                    throw new IllegalArgumentException(
                            name + " 中的属性 " + attr + " 不在属性集 "
                                    + FdCalculator.join(universe) + " 内");
                }
            }
        }
    }

    private static void requireDecomposition(Set<String> allAttrs, List<Set<String>> decomposition) {
        requireRelation(allAttrs, "关系模式");
        if (decomposition == null || decomposition.isEmpty()) {
            throw new IllegalArgumentException("分解不能为空");
        }
        Set<String> union = new TreeSet<String>();
        for (Set<String> part : decomposition) {
            if (part == null || part.isEmpty()) {
                throw new IllegalArgumentException("子模式不能为空集");
            }
            for (String attr : part) {
                if (!allAttrs.contains(attr)) {
                    throw new IllegalArgumentException("分解包含 R 之外的属性: " + attr);
                }
            }
            union.addAll(part);
        }
        if (!union.equals(new TreeSet<String>(allAttrs))) {
            Set<String> missing = new TreeSet<String>(allAttrs);
            missing.removeAll(union);
            throw new IllegalArgumentException("分解未覆盖全部属性, 缺少: " + missing);
        }
    }
}
