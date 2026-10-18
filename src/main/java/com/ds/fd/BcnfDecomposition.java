package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * BCNF 分解算法，对应教材 8.5.1.2 节、图 8-11（中文版第 198 页下半页）。
 *
 * <p>图 8-11 的伪代码：
 * <pre>
 * result := {R};
 * done := false;
 * 计算 F⁺;
 * while (not done) do
 *     if (result 中存在模式 Ri 不属于 BCNF)
 *     then begin
 *         令 α→β 为一个在 Ri 上成立的非平凡函数依赖,
 *              满足 α→Ri 不属于 F⁺, 并且 α∩β = ∅;
 *         result := (result − Ri) ∪ (Ri − β) ∪ (α, β);
 *     end
 *     else done := true;
 * </pre>
 *
 * <p><b>本实现怎么对应伪代码</b>：
 * <ul>
 *   <li>"计算 F⁺"：不物化 —— 那是指数级条数。按闭包定义，
 *       {@code α→β ∈ F⁺ ⟺ β ⊆ α⁺}、{@code α→Ri ∉ F⁺ ⟺ Ri ⊄ α⁺}，
 *       所以每一步只用几次属性闭包就能完成同样的判断。{@link #decomposeByFPlus}
 *       保留了"先物化 F⁺ 再挑依赖"的字面读法，只适合小例子，用来对照。</li>
 *   <li>"Ri 不属于 BCNF" 与 "α→β"：交给 {@link BcnfCheck#checkProjected} ——
 *       它枚举 Ri 的子集 α，给出见证依赖 {@code α→β, β=(α⁺−α)∩Ri}。
 *       这个 β 天然满足 β ⊆ Ri−α，于是 <b>α∩β=∅</b>，且 β 非空、依赖非平凡。</li>
 *   <li>"α→Ri 不属于 F⁺"：即 {@code Ri ⊄ α⁺}，也就是 α 不是 Ri 的超键 ——
 *       {@code checkProjected} 只在 α 不是超键时才报违反项。</li>
 * </ul>
 *
 * <p><b>为什么 α∩β=∅ 是必需条件</b>：算法用 (Ri−β) 与 (α,β) 替换 Ri。
 * 两块的公共属性是 {@code (Ri−β) ∩ (α∪β) = α − β}。只有当 α∩β=∅ 时它才等于 α，
 * 而 α→β（连同自反律 α→α）保证 α 是 (α,β) 的超键 —— 于是二元无损定理
 * "公共属性决定某一侧" 成立。若 α∩β≠∅，公共属性只剩 α−β，
 * α−β 未必决定 α∩β 那些被 β 带走、已不在 Ri−β 里的属性，无损性就没有保证了。
 *
 * <p><b>为什么分解一定无损</b>：每一步都是合法的二元无损分解（公共属性 α 决定右侧 αβ），
 * 而无损分解的复合仍然无损（教材 8.5.2 的 chase 表格法可以直接验证整个结果）；
 * {@link Result#lossless()} 用 {@link LosslessJoin#checkChase} 对最终结果复核。
 *
 * <p><b>终止性</b>：每次替换把 Ri 换成它的两个<b>真子集</b>
 * （Ri−β 少了非空的 β；αβ ≠ Ri，否则 α 就是 Ri 的超键，矛盾）。
 * 属性集有限，分解树深度 ≤ |R|，所以必然终止。
 *
 * <p><b>代价与"过度规范化"</b>：教材指出该算法所需时间对原始模式规模是指数级的，
 * 因为判断一个关系是否属于 BCNF 的代价可以达到指数级；而且它可能"过度规范化"、
 * 把 Schema 拆成不必要的多块。分解结果还<b>不一定保持依赖</b>
 * （{@link #decompose} 只负责拆；保持依赖要用
 * {@link DependencyPreservation} 另外判——演示【十一】的例 2 就是"无损但丢依赖"）。
 */
public final class BcnfDecomposition {

    private BcnfDecomposition() {
    }

    // =====================================================================
    // 1. 主算法：闭包版（等价于图 8-11，不物化 F⁺）
    // =====================================================================

    /**
     * 按图 8-11 把 R 分解成一组 BCNF 模式。
     *
     * <p>每一轮按插入顺序找第一个不属于 BCNF 的 Ri（保证结果可复现），
     * 用 {@link BcnfCheck#checkProjected} 的第一条违反项做拆分：
     * <pre>
     * 左块 = Ri − β      （保留 α，丢掉了 β）
     * 右块 = α ∪ β       （违反依赖的左右部合成一张表）
     * </pre>
     * 拆到所有 Ri 都属于 BCNF 为止。若结果中某一块与已有模式重复，
     * 集合语义会自然去重（这也是伪代码里 result 作为集合的含义）。
     *
     * @throws IllegalArgumentException R 为空，或 F 中出现了 R 之外的属性
     */
    public static Result decompose(Set<String> allAttrs, List<FunctionalDependency> fds) {
        requireInputs(allAttrs, fds);
        LinkedHashSet<Set<String>> result = new LinkedHashSet<Set<String>>();
        result.add(new TreeSet<String>(allAttrs));
        List<Step> steps = new ArrayList<Step>();

        while (true) {
            Set<String> target = null;
            BcnfCheck.CheckResult targetCheck = null;
            for (Set<String> ri : result) {
                BcnfCheck.CheckResult check = BcnfCheck.checkProjected(ri, allAttrs, fds);
                if (!check.bcnf()) {
                    target = ri;
                    targetCheck = check;
                    break;
                }
            }
            if (target == null) {
                break;
            }
            BcnfCheck.Violation violation = targetCheck.violations().get(0);
            Set<String> beta = new TreeSet<String>(violation.fd().dependent());
            Set<String> left = new TreeSet<String>(target);
            left.removeAll(beta);                                   // Ri − β
            Set<String> right = new TreeSet<String>(violation.fd().determinant());
            right.addAll(beta);                                     // α ∪ β

            result.remove(target);
            result.add(left);
            result.add(right);
            steps.add(new Step(steps.size() + 1, target, violation.fd(), violation.closure(),
                    violation.missing(), left, right, violation.inGivenSet()));
        }
        return new Result(allAttrs, result, steps, fds);
    }

    // =====================================================================
    // 2. 字面读法：先物化 F⁺ 再挑依赖（只适合小例子）
    // =====================================================================

    /** 便捷：只要分解结果中的模式列表。 */
    public static List<Set<String>> decompositionOf(Set<String> allAttrs,
                                                    List<FunctionalDependency> fds) {
        return decompose(allAttrs, fds).decomposition();
    }

    /**
     * 图 8-11 的字面实现：先按集合论算法物化 F⁺，再从 Π_Ri(F⁺) 里挑第一条满足
     * "非平凡、α∩β=∅、α→Ri ∉ F⁺" 的依赖来拆。
     *
     * <p>存在的意义是交叉验证 {@link #decompose}：两者选到的违反项可能不同
     * （伪代码只说"令 α→β 为一个"，没规定选哪条），因此拆出的模式集合也可能不同；
     * 但结果都必须满足"每个模式属于 BCNF + 整体无损 + 覆盖 R"。
     * <b>注意</b>：F⁺ 的条数是指数级的，这里会把所有子集对都枚举一遍，
     * 只适合属性数很少的教学例子（超过 8 个属性就可能很慢）。
     */
    public static Result decomposeByFPlus(Set<String> allAttrs, List<FunctionalDependency> fds) {
        requireInputs(allAttrs, fds);
        Set<FunctionalDependency> fplus = FdCalculator.closureByAttributeClosures(allAttrs, fds);
        Set<FunctionalDependency> given = new LinkedHashSet<FunctionalDependency>(fds);
        Map<Set<String>, Set<String>> closureCache = new HashMap<Set<String>, Set<String>>();

        LinkedHashSet<Set<String>> result = new LinkedHashSet<Set<String>>();
        result.add(new TreeSet<String>(allAttrs));
        List<Step> steps = new ArrayList<Step>();

        while (true) {
            Set<String> target = null;
            FunctionalDependency witness = null;
            Set<String> witnessClosure = null;
            Set<String> witnessMissing = null;

            for (Set<String> ri : result) {
                for (FunctionalDependency fd : fplus) {
                    if (fd.isTrivial()) {
                        continue;                       // 平凡依赖不违反 BCNF
                    }
                    if (!ri.containsAll(fd.determinant()) || !ri.containsAll(fd.dependent())) {
                        continue;                       // 在 Ri 上不成立：左右部没都落在 Ri 内
                    }
                    if (!Collections.disjoint(fd.determinant(), fd.dependent())) {
                        continue;                       // 要求 α∩β=∅（见类注释的"为什么必需"）
                    }
                    Set<String> alpha = new TreeSet<String>(fd.determinant());
                    Set<String> closure = closureOf(alpha, fds, closureCache);
                    if (closure.containsAll(ri)) {
                        continue;                       // α 是 Ri 的超键，不违反
                    }
                    target = ri;
                    witness = fd;
                    witnessClosure = closure;
                    Set<String> missing = new TreeSet<String>(ri);
                    missing.removeAll(closure);
                    witnessMissing = missing;
                    break;
                }
                if (target != null) {
                    break;
                }
            }
            if (target == null) {
                break;
            }

            Set<String> beta = new TreeSet<String>(witness.dependent());
            Set<String> left = new TreeSet<String>(target);
            left.removeAll(beta);
            Set<String> right = new TreeSet<String>(witness.determinant());
            right.addAll(beta);

            result.remove(target);
            result.add(left);
            result.add(right);
            steps.add(new Step(steps.size() + 1, target, witness, witnessClosure,
                    witnessMissing, left, right, given.contains(witness)));
        }
        return new Result(allAttrs, result, steps, fds);
    }

    private static Set<String> closureOf(Set<String> attrs, List<FunctionalDependency> fds,
                                         Map<Set<String>, Set<String>> cache) {
        Set<String> key = new TreeSet<String>(attrs);
        Set<String> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Set<String> closure = FdCalculator.attributeClosure(attrs, fds);
        cache.put(key, closure);
        return closure;
    }

    // =====================================================================
    // 3. 结果载体
    // =====================================================================

    /** 一次 BCNF 分解的结果：最终模式集合、每一步拆分记录、自动复核的结论。 */
    public static final class Result {
        private final Set<String> allAttrs;
        private final List<Set<String>> decomposition;
        private final List<Step> steps;
        private final boolean bcnf;
        private final boolean lossless;

        Result(Set<String> allAttrs, LinkedHashSet<Set<String>> decomposition, List<Step> steps,
               List<FunctionalDependency> fds) {
            this.allAttrs = Collections.unmodifiableSet(new TreeSet<String>(allAttrs));
            List<Set<String>> copy = new ArrayList<Set<String>>();
            for (Set<String> part : decomposition) {
                copy.add(Collections.unmodifiableSet(new TreeSet<String>(part)));
            }
            this.decomposition = Collections.unmodifiableList(copy);
            this.steps = Collections.unmodifiableList(steps);
            this.bcnf = BcnfCheck.checkDecomposition(allAttrs, new ArrayList<Set<String>>(copy), fds).bcnf();
            this.lossless = LosslessJoin.checkChase(allAttrs, new ArrayList<Set<String>>(copy), fds).lossless();
        }

        public Set<String> allAttrs() {
            return allAttrs;
        }

        /** 最终的模式集合（按产生顺序；同一模式不会重复）。 */
        public List<Set<String>> decomposition() {
            return decomposition;
        }

        /** 每次拆分的记录，按发生顺序。 */
        public List<Step> steps() {
            return steps;
        }

        public int stepCount() {
            return steps.size();
        }

        /** 每个模式是否都属于 BCNF（用 {@link BcnfCheck#checkDecomposition} 复核）。 */
        public boolean bcnf() {
            return bcnf;
        }

        /** 整个分解是否无损（用 {@link LosslessJoin#checkChase} 的 chase 表格法复核）。 */
        public boolean lossless() {
            return lossless;
        }

        /** 分解是否保持依赖 —— 不是本算法保证的性质，需要时另行判定。 */
        public boolean preservesDependencies(List<FunctionalDependency> fds) {
            return DependencyPreservation.preserves(allAttrs, decomposition, fds);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < decomposition.size(); i++) {
                sb.append(i == 0 ? "" : " | ")
                        .append(FdCalculator.joinWithComma(decomposition.get(i)));
            }
            sb.append(" => ").append(bcnf ? "BCNF" : "非 BCNF")
                    .append(lossless ? " + 无损" : " + 有损");
            return sb.toString();
        }
    }

    /** 一步拆分：把 relation 按违反依赖 fd 换成 (left, right)。 */
    public static final class Step {
        private final int index;
        private final Set<String> relation;
        private final FunctionalDependency fd;
        private final Set<String> closure;
        private final Set<String> missing;
        private final Set<String> left;
        private final Set<String> right;
        private final boolean inGivenSet;

        Step(int index, Set<String> relation, FunctionalDependency fd, Set<String> closure,
             Set<String> missing, Set<String> left, Set<String> right, boolean inGivenSet) {
            this.index = index;
            this.relation = Collections.unmodifiableSet(new TreeSet<String>(relation));
            this.fd = fd;
            this.closure = Collections.unmodifiableSet(new TreeSet<String>(closure));
            this.missing = Collections.unmodifiableSet(new TreeSet<String>(missing));
            this.left = Collections.unmodifiableSet(new TreeSet<String>(left));
            this.right = Collections.unmodifiableSet(new TreeSet<String>(right));
            this.inGivenSet = inGivenSet;
        }

        public int index() {
            return index;
        }

        /** 本步被拆掉的模式 Ri。 */
        public Set<String> relation() {
            return relation;
        }

        /** 选中的违反依赖 α→β（α∩β=∅）。 */
        public FunctionalDependency fd() {
            return fd;
        }

        /** α⁺。 */
        public Set<String> closure() {
            return closure;
        }

        /** Ri − α⁺：α 不是 Ri 的超键，缺的就是这些属性。 */
        public Set<String> missing() {
            return missing;
        }

        /** Ri − β。 */
        public Set<String> left() {
            return left;
        }

        /** α ∪ β。 */
        public Set<String> right() {
            return right;
        }

        /** 选中的依赖是否就在给定的 F 里；false 表示它来自 F⁺（投影后才出现的违反项）。 */
        public boolean inGivenSet() {
            return inGivenSet;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("第 ").append(index).append(" 步: 拆分 ")
                    .append(FdCalculator.joinWithComma(relation)).append(", 违反项 ")
                    .append(FdCalculator.joinWithComma(fd.determinant())).append(" -> ")
                    .append(FdCalculator.joinWithComma(fd.dependent()))
                    .append(" (").append(FdCalculator.joinWithComma(fd.determinant())).append("+ = ")
                    .append(FdCalculator.joinWithComma(closure))
                    .append(", 还缺 ").append(FdCalculator.joinWithComma(missing)).append(")");
            if (!inGivenSet) {
                sb.append(" [不在 F 中, 来自 F+]");
            }
            sb.append(" => ").append(FdCalculator.joinWithComma(left))
                    .append(" + ").append(FdCalculator.joinWithComma(right));
            return sb.toString();
        }
    }

    // =====================================================================
    // 4. 入参校验
    // =====================================================================

    private static void requireInputs(Set<String> allAttrs, List<FunctionalDependency> fds) {
        if (allAttrs == null || allAttrs.isEmpty()) {
            throw new IllegalArgumentException("关系模式不能为空");
        }
        for (FunctionalDependency fd : fds) {
            for (String attr : fd.determinant()) {
                if (!allAttrs.contains(attr)) {
                    throw new IllegalArgumentException("F 中的属性 " + attr + " 不在属性集 "
                            + FdCalculator.join(allAttrs) + " 内");
                }
            }
            for (String attr : fd.dependent()) {
                if (!allAttrs.contains(attr)) {
                    throw new IllegalArgumentException("F 中的属性 " + attr + " 不在属性集 "
                            + FdCalculator.join(allAttrs) + " 内");
                }
            }
        }
    }
}
