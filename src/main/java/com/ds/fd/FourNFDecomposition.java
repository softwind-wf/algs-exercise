package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 4NF 分解算法，对应教材 8.6.3 节、图 8-16（中文版第 272 页）。
 *
 * <p>图 8-16 的伪代码：
 * <pre>
 * result := {R};
 * done := false;
 * 计算 D⁺; 给定模式 Ri, 令 Di 表示 D⁺ 在 Ri 上的限定
 * while (not done) do
 *     if (result 中存在 Ri 不属于 4NF)
 *     then begin
 *         令 α→→β 为 Ri 上成立的非平凡多值依赖,
 *              使得 α→Ri 不属于 Di, 并且 α∩β = ∅;
 *         result := (result − Ri) ∪ (Ri − β) ∪ (α, β);
 *     end
 *     else done := true;
 * </pre>
 *
 * <p><b>本实现怎么对应伪代码</b>：
 * <ul>
 *   <li>"计算 D⁺"：不物化 —— MVD 闭包的条数同样是指数级。每一步只需要
 *       "Ri 上有哪些非平凡 MVD、其左部是不是超键"，
 *       {@link #mvdClosureOn} 在 Ri 上迭代到不动点：
 *       种子是给定 MVD 的限定（α⊆Ri 时取 β∩Ri）与给定 FD 按复制律的翻译
 *       （α→β ⟹ α→→β），之后反复套用 MVD 公理 —— 补律、同一左部的并/交/差、
 *       伪传递（全部限制在 Ri 上）。这是 D⁺ 在 Ri 上限定的可靠下界，
 *       教材例子完全覆盖；得到的结果只保留非平凡成员，供违反项搜索用。</li>
 *   <li>"α→Ri 不属于 Di"：按 FD 投影定理等价于 Ri ⊄ α⁺（α⁺ 用给定 F 的属性闭包），
 *       即 α 不是 Ri 的超键 —— 与 BCNF 的判据同构（见 {@link BcnfDecomposition}）。
 *       超键判定只用给定 F：教材例子中决定超键的都是 FD；
 *       MVD 经 coalescence 律隐含的 FD 不在覆盖范围（教学例子用不到）。</li>
 *   <li>"α∩β = ∅"：WLOG —— α→→β 时 α→→β−α 同样成立，所以违反项一律先约化
 *       β ← β−α，自动满足 α∩β=∅（教材例子里 ID→→street,city 也是这样约化出来的：
 *       它其实是 ID→→dept_name 的补律产物，见 {@link #check} 的演示）。</li>
 *   <li>"Ri 不属于 4NF"：{@link #check} 枚举 Ri 上成立的非平凡 MVD，
 *       左部不是超键的就是违反项 —— 4NF 定义与 BCNF 定义的唯一不同就是
 *       把 FD 换成了 MVD（教材 8.6.2 节原话）。</li>
 * </ul>
 *
 * <p><b>为什么每步无损</b>：拆出的两块是 (Ri−β) 与 (α∪β)，公共属性是 α（因为 α∩β=∅），
 * 右块独有的属性是 β，而 α→→β —— "公共属性多重决定右块差"的二元分解是无损的
 * （多值依赖版二元无损定理）；无损分解的复合仍然无损。
 * {@link Result#lossless()} 对每一步复核这些几何条件，并复核 α→→β 确实是
 * {@link #mvdClosureOn} 推出的、在 Ri 上成立的依赖。
 *
 * <p><b>终止性</b>：每次替换把 Ri 换成两个<b>真子集</b>（β 非空 ⟹ Ri−β ⊂ Ri；
 * α∪β=Ri 是平凡 MVD、已被过滤 ⟹ α∪β ⊂ Ri）。属性集有限，分解树深度 ≤ |R|，必然终止。
 *
 * <p><b>与 BCNF 的关系与代价</b>：4NF 模式一定属于 BCNF（若 α→β 违反 BCNF，
 * 则 α→→β 连同补律就违反 4NF）；4NF 分解一定无损，但<b>不保证保持 FD 依赖</b>，
 * 且可能"过度规范化"——图 8-16 用在 (ID, dept_name, street, city) 上会拆成
 * r21(ID, dept_name) 与 r22(ID, street, city)，正是教材 8.6.3 节的答案。
 */
public final class FourNFDecomposition {

    private FourNFDecomposition() {
    }

    // =====================================================================
    // 1. 4NF 判定（也供分解循环复用）
    // =====================================================================

    /**
     * 判定单个模式 schema 是否属于 4NF（教材 8.6.2 节的定义：
     * 对 D⁺ 在 schema 上的限定 Di 中所有形如 α→→β 的非平凡多值依赖，α 都是超键）。
     *
     * <p>返回所有违反项：每条 α→→β（已约化成 α∩β=∅）配上 α⁺ 与缺口 schema−α⁺。
     * FD 与 MVD 都可以为空；违反项按（左部大小, 打印串）排序，保证可复现。
     */
    public static CheckResult check(Set<String> schema,
                                    List<FunctionalDependency> fds,
                                    List<MultivaluedDependency> mvds) {
        List<Violation> violations = new ArrayList<Violation>();
        for (Map.Entry<Mvd, Boolean> entry : mvdClosureOn(schema, fds, mvds).entrySet()) {
            Mvd holding = entry.getKey();
            Set<String> beta = new TreeSet<String>(holding.beta);
            beta.removeAll(holding.alpha);                  // 约化: α→→β ⟹ α→→β−α, 保证 α∩β=∅
            if (beta.isEmpty()) {
                continue;                                   // β⊆α 的平凡依赖
            }
            Set<String> union = new TreeSet<String>(holding.alpha);
            union.addAll(beta);
            if (union.containsAll(schema)) {
                continue;                                   // α∪β=R 的平凡依赖
            }
            Set<String> closure = FdCalculator.attributeClosure(holding.alpha, fds);
            if (closure.containsAll(schema)) {
                continue;                                   // α 是超键: α→Ri ∈ Di, 不违反
            }
            Set<String> missing = new TreeSet<String>(schema);
            missing.removeAll(closure);
            violations.add(new Violation(
                    new MultivaluedDependency(holding.alpha, beta), closure, missing, entry.getValue()));
        }
        Collections.sort(violations, new Comparator<Violation>() {
            @Override
            public int compare(Violation left, Violation right) {
                int bySize = Integer.compare(left.mvd().determinant().size(),
                        right.mvd().determinant().size());
                if (bySize != 0) {
                    return bySize;
                }
                return left.mvd().toString().compareTo(right.mvd().toString());
            }
        });
        return new CheckResult(schema, violations);
    }

    // =====================================================================
    // 2. 主算法：图 8-16
    // =====================================================================

    /**
     * 按图 8-16 把 R 分解成一组 4NF 模式。
     *
     * <p>每一轮按插入顺序找第一个不属于 4NF 的 Ri，用 {@link #check} 的第一条违反项拆分：
     * <pre>
     * 左块 = Ri − β      （丢了 β 这一组独立信息）
     * 右块 = α ∪ β       （违反依赖的左右部合成一张表）
     * </pre>
     * 拆到所有 Ri 都属于 4NF 为止。若某块与已有模式重复，集合语义自然去重。
     *
     * @throws IllegalArgumentException R 为空，或 F/D 中出现了 R 之外的属性
     */
    public static Result decompose(Set<String> allAttrs,
                                   List<FunctionalDependency> fds,
                                   List<MultivaluedDependency> mvds) {
        requireInputs(allAttrs, fds, mvds);
        LinkedHashSet<Set<String>> result = new LinkedHashSet<Set<String>>();
        result.add(new TreeSet<String>(allAttrs));
        List<Step> steps = new ArrayList<Step>();

        while (true) {
            Set<String> target = null;
            Violation violation = null;
            for (Set<String> ri : result) {
                CheckResult check = check(ri, fds, mvds);
                if (!check.fourNF()) {
                    target = ri;
                    violation = check.violations().get(0);
                    break;
                }
            }
            if (target == null) {
                break;
            }
            Set<String> beta = new TreeSet<String>(violation.mvd().dependent());
            Set<String> left = new TreeSet<String>(target);
            left.removeAll(beta);                               // Ri − β
            Set<String> right = new TreeSet<String>(violation.mvd().determinant());
            right.addAll(beta);                                 // α ∪ β

            result.remove(target);
            result.add(left);
            result.add(right);
            steps.add(new Step(steps.size() + 1, target, violation.mvd(), violation.closure(),
                    violation.missing(), left, right, violation.inGivenSet()));
        }
        return new Result(allAttrs, result, steps, fds, mvds);
    }

    // =====================================================================
    // 3. Ri 上成立的 MVD 闭包（D⁺ 在 Ri 上限定的不动点计算）
    // =====================================================================

    /** 内部表示：schema 上的一条 MVD α→→β（α、β 均为 schema 的子集，按字典序存放）。 */
    private static final class Mvd {
        private final TreeSet<String> alpha;
        private final TreeSet<String> beta;

        Mvd(Set<String> alpha, Set<String> beta) {
            this.alpha = new TreeSet<String>(alpha);
            this.beta = new TreeSet<String>(beta);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Mvd)) {
                return false;
            }
            Mvd other = (Mvd) o;
            return alpha.equals(other.alpha) && beta.equals(other.beta);
        }

        @Override
        public int hashCode() {
            return 31 * alpha.hashCode() + beta.hashCode();
        }

        @Override
        public String toString() {
            return FdCalculator.joinWithComma(alpha) + " ->-> " + FdCalculator.joinWithComma(beta);
        }
    }

    /**
     * 计算 schema 上成立的非平凡 MVD 集合（value = 是否直接来自给定的 D）。
     *
     * <p>种子：给定 MVD 的限定（α⊆Ri 时取 β∩Ri）；给定 FD 按复制律翻译（α→β ⟹ α→→β）。
     * 之后反复套用 MVD 公理（全部限制在 schema 上）直到不动点：
     * <ul>
     *   <li>补律：α→→β ⟹ α→→(Ri−α−β)</li>
     *   <li>同一左部的并 / 交 / 差：α→→β、α→→γ ⟹ α→→(β∪γ) / (β∩γ) / (β−γ)</li>
     *   <li>伪传递（含增广）：α→→β、γ→→δ 且 γ⊆β ⟹ α→→(δ−β)</li>
     * </ul>
     * 结果是 D⁺ 在 Ri 上限定的可靠下界（教学例子内即全部）；
     * 只保留非平凡成员（β−α 与 Ri−α−β 都非空），平凡成员只用于推导、不入集。
     */
    private static Map<Mvd, Boolean> mvdClosureOn(Set<String> schema,
                                                  List<FunctionalDependency> fds,
                                                  List<MultivaluedDependency> mvds) {
        Map<Mvd, Boolean> holding = new LinkedHashMap<Mvd, Boolean>();
        for (MultivaluedDependency mvd : mvds) {
            if (schema.containsAll(mvd.determinant())) {
                TreeSet<String> beta = new TreeSet<String>(mvd.dependent());
                beta.retainAll(schema);
                putMvd(holding, new Mvd(mvd.determinant(), beta), true, schema);
            }
        }
        for (FunctionalDependency fd : fds) {                   // 复制律: FD 蕴含 MVD
            if (schema.containsAll(fd.determinant())) {
                TreeSet<String> beta = new TreeSet<String>(fd.dependent());
                beta.retainAll(schema);
                putMvd(holding, new Mvd(fd.determinant(), beta), true, schema);
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            List<Mvd> current = new ArrayList<Mvd>(holding.keySet());
            for (Mvd mvd : current) {                           // 补律
                TreeSet<String> complement = new TreeSet<String>(schema);
                complement.removeAll(mvd.alpha);
                complement.removeAll(mvd.beta);
                changed |= putMvd(holding, new Mvd(mvd.alpha, complement), false, schema);
            }
            for (Mvd left : current) {                          // 同左部的并、交、差
                for (Mvd right : current) {
                    if (!left.alpha.equals(right.alpha)) {
                        continue;
                    }
                    TreeSet<String> union = new TreeSet<String>(left.beta);
                    union.addAll(right.beta);
                    changed |= putMvd(holding, new Mvd(left.alpha, union), false, schema);
                    TreeSet<String> inter = new TreeSet<String>(left.beta);
                    inter.retainAll(right.beta);
                    changed |= putMvd(holding, new Mvd(left.alpha, inter), false, schema);
                    TreeSet<String> diff = new TreeSet<String>(left.beta);
                    diff.removeAll(right.beta);
                    changed |= putMvd(holding, new Mvd(left.alpha, diff), false, schema);
                }
            }
            for (Mvd first : current) {                         // 伪传递（含增广）
                for (Mvd second : current) {
                    if (!first.beta.containsAll(second.alpha)) {
                        continue;
                    }
                    TreeSet<String> rest = new TreeSet<String>(second.beta);
                    rest.removeAll(first.beta);
                    changed |= putMvd(holding, new Mvd(first.alpha, rest), false, schema);
                }
            }
        }
        return holding;
    }

    /** 非平凡才收：β−α 与 Ri−α−β 都非空；已存在时保留原 origin。返回是否新增。 */
    private static boolean putMvd(Map<Mvd, Boolean> holding, Mvd candidate, boolean fromGiven,
                                  Set<String> schema) {
        Set<String> rest = new TreeSet<String>(candidate.beta);
        rest.removeAll(candidate.alpha);
        if (rest.isEmpty()) {
            return false;                                       // β⊆α, 平凡
        }
        Set<String> union = new TreeSet<String>(candidate.alpha);
        union.addAll(candidate.beta);
        if (union.containsAll(schema)) {
            return false;                                       // α∪β=R, 平凡
        }
        if (holding.containsKey(candidate)) {
            return false;
        }
        holding.put(candidate, fromGiven);
        return true;
    }

    // =====================================================================
    // 4. 结果载体
    // =====================================================================

    /** 一次 4NF 分解的结果：最终模式集合、每一步拆分记录、自动复核的结论。 */
    public static final class Result {
        private final Set<String> allAttrs;
        private final List<Set<String>> decomposition;
        private final List<Step> steps;
        private final boolean fourNF;
        private final boolean lossless;
        private final boolean coversAll;

        Result(Set<String> allAttrs, LinkedHashSet<Set<String>> decomposition, List<Step> steps,
               List<FunctionalDependency> fds, List<MultivaluedDependency> mvds) {
            this.allAttrs = Collections.unmodifiableSet(new TreeSet<String>(allAttrs));
            List<Set<String>> copy = new ArrayList<Set<String>>();
            for (Set<String> part : decomposition) {
                copy.add(Collections.unmodifiableSet(new TreeSet<String>(part)));
            }
            this.decomposition = Collections.unmodifiableList(copy);
            this.steps = Collections.unmodifiableList(steps);
            boolean fourNF = true;
            for (Set<String> ri : copy) {
                if (!check(ri, fds, mvds).fourNF()) {
                    fourNF = false;
                    break;
                }
            }
            this.fourNF = fourNF;
            this.lossless = stepsLegal(steps, fds, mvds);
            Set<String> covered = new TreeSet<String>();
            for (Set<String> ri : copy) {
                covered.addAll(ri);
            }
            this.coversAll = covered.equals(this.allAttrs);
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

        /** 每个模式是否都属于 4NF（用 {@link #check} 对最终结果逐个复核）。 */
        public boolean fourNF() {
            return fourNF;
        }

        /**
         * 整个分解是否无损：逐步复核每步拆分满足二元无损定理的几何条件
         * （公共属性 = α、右块差 = β、α→→β 在 Ri 上成立）。
         */
        public boolean lossless() {
            return lossless;
        }

        /** 最终模式集合的并是否等于 R（算法保证，复核防回归）。 */
        public boolean coversAll() {
            return coversAll;
        }

        /**
         * FD 依赖是否保持 —— 不是本算法保证的性质，需要时另行判定。
         * MVD 的保持依赖不在判定范围（4NF 分解一般会丢 MVD，这正是目的所在）。
         */
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
            sb.append(" => ").append(fourNF ? "4NF" : "非 4NF")
                    .append(lossless ? " + 无损" : " + 有损");
            return sb.toString();
        }
    }

    /** 一步拆分：把 relation 按违反依赖 mvd 换成 (left, right)。 */
    public static final class Step {
        private final int index;
        private final Set<String> relation;
        private final MultivaluedDependency mvd;
        private final Set<String> closure;
        private final Set<String> missing;
        private final Set<String> left;
        private final Set<String> right;
        private final boolean inGivenSet;

        Step(int index, Set<String> relation, MultivaluedDependency mvd, Set<String> closure,
             Set<String> missing, Set<String> left, Set<String> right, boolean inGivenSet) {
            this.index = index;
            this.relation = Collections.unmodifiableSet(new TreeSet<String>(relation));
            this.mvd = mvd;
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

        /** 选中的违反依赖 α→→β（已约化，α∩β=∅）。 */
        public MultivaluedDependency mvd() {
            return mvd;
        }

        /** α⁺（用给定 F 算的属性闭包）。 */
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

        /** 选中的依赖是否直接来自给定的 D（给定 MVD 的限定或给定 FD 的复制）；false 表示它由 MVD 公理推出。 */
        public boolean inGivenSet() {
            return inGivenSet;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("第 ").append(index).append(" 步: 拆分 ")
                    .append(FdCalculator.joinWithComma(relation)).append(", 违反项 ")
                    .append(FdCalculator.joinWithComma(mvd.determinant())).append(" ->-> ")
                    .append(FdCalculator.joinWithComma(mvd.dependent()))
                    .append(" (").append(FdCalculator.joinWithComma(mvd.determinant())).append("+ = ")
                    .append(FdCalculator.joinWithComma(closure))
                    .append(", 还缺 ").append(FdCalculator.joinWithComma(missing)).append(")");
            if (!inGivenSet) {
                sb.append(" [不在 D 中, 来自闭包推导]");
            }
            sb.append(" => ").append(FdCalculator.joinWithComma(left))
                    .append(" + ").append(FdCalculator.joinWithComma(right));
            return sb.toString();
        }
    }

    /** 一条 4NF 违反项：非平凡 MVD α→→β，且 α 不是 Ri 的超键。 */
    public static final class Violation {
        private final MultivaluedDependency mvd;
        private final Set<String> closure;
        private final Set<String> missing;
        private final boolean inGivenSet;

        Violation(MultivaluedDependency mvd, Set<String> closure, Set<String> missing,
                  boolean inGivenSet) {
            this.mvd = mvd;
            this.closure = Collections.unmodifiableSet(new TreeSet<String>(closure));
            this.missing = Collections.unmodifiableSet(new TreeSet<String>(missing));
            this.inGivenSet = inGivenSet;
        }

        public MultivaluedDependency mvd() {
            return mvd;
        }

        /** α⁺。 */
        public Set<String> closure() {
            return closure;
        }

        /** Ri − α⁺。 */
        public Set<String> missing() {
            return missing;
        }

        /** 该 MVD 是否直接来自给定的 D；false 表示由 MVD 公理（补律等）推出。 */
        public boolean inGivenSet() {
            return inGivenSet;
        }

        @Override
        public String toString() {
            return mvd.toString() + " (" + FdCalculator.joinWithComma(mvd.determinant())
                    + "+ = " + FdCalculator.joinWithComma(closure)
                    + ", 还缺 " + FdCalculator.joinWithComma(missing) + ")";
        }
    }

    /** 一次 4NF 判定的结果。 */
    public static final class CheckResult {
        private final Set<String> schema;
        private final List<Violation> violations;

        CheckResult(Set<String> schema, List<Violation> violations) {
            this.schema = Collections.unmodifiableSet(new TreeSet<String>(schema));
            this.violations = Collections.unmodifiableList(violations);
        }

        public Set<String> schema() {
            return schema;
        }

        public List<Violation> violations() {
            return violations;
        }

        /** 无违反项即属于 4NF。 */
        public boolean fourNF() {
            return violations.isEmpty();
        }

        @Override
        public String toString() {
            return fourNF() ? "4NF" : "非 4NF: " + violations;
        }
    }

    // =====================================================================
    // 5. 复核与入参校验
    // =====================================================================

    /**
     * 逐步复核每步拆分是否满足二元无损定理的几何条件：
     * α∪β ⊆ Ri、α∩β=∅、(左∪右)=Ri、(左∩右)=α、(右−左)=β、左右都是非空真子集，
     * 且 α→→β 确实是 {@link #mvdClosureOn} 推出的、在 Ri 上成立的依赖。
     */
    private static boolean stepsLegal(List<Step> steps, List<FunctionalDependency> fds,
                                      List<MultivaluedDependency> mvds) {
        for (Step step : steps) {
            Set<String> ri = step.relation();
            Set<String> alpha = step.mvd().determinant();
            Set<String> beta = step.mvd().dependent();
            if (!ri.containsAll(alpha) || !ri.containsAll(beta)) {
                return false;
            }
            if (!Collections.disjoint(alpha, beta)) {
                return false;
            }
            Set<String> union = new TreeSet<String>(alpha);
            union.addAll(beta);
            Set<String> left = step.left();
            Set<String> right = step.right();
            Set<String> both = new TreeSet<String>(left);
            both.addAll(right);
            Set<String> common = new TreeSet<String>(left);
            common.retainAll(right);
            Set<String> rightOnly = new TreeSet<String>(right);
            rightOnly.removeAll(left);
            if (!both.equals(ri) || !common.equals(alpha) || !rightOnly.equals(beta)) {
                return false;
            }
            if (left.isEmpty() || right.isEmpty() || left.equals(ri) || right.equals(ri)) {
                return false;
            }
            if (!mvdClosureOn(ri, fds, mvds).containsKey(new Mvd(alpha, beta))) {
                return false;
            }
        }
        return true;
    }

    private static void requireInputs(Set<String> allAttrs, List<FunctionalDependency> fds,
                                      List<MultivaluedDependency> mvds) {
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
        for (MultivaluedDependency mvd : mvds) {
            for (String attr : mvd.determinant()) {
                if (!allAttrs.contains(attr)) {
                    throw new IllegalArgumentException("D 中的属性 " + attr + " 不在属性集 "
                            + FdCalculator.join(allAttrs) + " 内");
                }
            }
            for (String attr : mvd.dependent()) {
                if (!allAttrs.contains(attr)) {
                    throw new IllegalArgumentException("D 中的属性 " + attr + " 不在属性集 "
                            + FdCalculator.join(allAttrs) + " 内");
                }
            }
        }
    }
}
