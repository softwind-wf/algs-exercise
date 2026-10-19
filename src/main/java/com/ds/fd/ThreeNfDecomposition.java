package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 3NF 分解（保持依赖且无损），对应教材 8.5.2 节、图 8-12（中文版第 199 页）。
 *
 * <p>图 8-12 的伪代码：
 * <pre>
 * 令 F_c 为 F 的正则覆盖;
 * i := 0;
 * for each F_c 中的函数依赖 α→β
 *     i := i + 1;
 *     R_i := αβ;
 * if 模式 R_j (j=1,2,...,i) 都不包含 R 的候选键
 * then
 *     i := i + 1;
 *     R_i := R 的任意候选键;
 * // (可选) 移除冗余关系
 * repeat
 *     if 模式 R_j 包含于另一个模式 R_k 中
 *     then 删除 R_j
 * until 不再有可以删除的 R_j
 * return (R_1, R_2, ..., R_i)
 * </pre>
 *
 * <p><b>为什么这样拼出来就一定是 3NF</b>：每个模式 αβ 上，α 是键（因为 α→β 在 F_c 里，
 * α 决定整个模式），并且 α→β 的右部 β 都落在该模式内；F_c 与 F 有相同闭包，
 * 所以任何一条 F⁺ 里的依赖都能在某个 αβ 上检查。加上"必须包含一个候选键"这一条，
 * 保证无损。教材还证明了：3NF 的保持依赖分解<b>一定存在</b>，图 8-12 就是构造方法
 * （BCNF 的保持依赖分解则不一定存在，例 8.4.5 的 AB→C 就会丢）。
 *
 * <p><b>两个易错点</b>：
 * <ol>
 *   <li>"都<b>不</b>包含候选键才补"：只要有一个模式包含某个候选键，就不再补。</li>
 *   <li>冗余模式可以被删除：若 R_j ⊆ R_k，R_j 上要检查的依赖 α→β 的 αβ ⊆ R_j ⊆ R_k，
 *       在 R_k 上照样能检查，删除 R_j 既不影响保持依赖、也不影响无损（候选键仍在 R_k 里）。
 *       教材的 dept_advisor 例子就是 R_2 吞掉 R_1，最后只剩一张表。</li>
 * </ol>
 *
 * <p><b>与 BCNF 分解的区别</b>：3NF 合成可能返回"不满足 BCNF 但满足 3NF"的模式
 * （它保留依赖的代价）；也常常恰好得到 BCNF（教材的 class 例子就是）。要判某个结果
 * 是否真的 3NF，用 {@link #checkThirdNormalForm}（主属性 / 候选键口径），而不是
 * {@link BcnfCheck}。
 */
public final class ThreeNfDecomposition {

    private ThreeNfDecomposition() {
    }

    // =====================================================================
    // 1. 主算法
    // =====================================================================

    /** 图 8-12 完整算法（含可选的冗余模式删除）。 */
    public static Result decompose(Set<String> allAttrs, List<FunctionalDependency> fds) {
        return decompose(allAttrs, fds, true);
    }

    /**
     * 图 8-12 算法。
     *
     * @param removeRedundant 是否执行伪代码里"可选"的冗余模式删除；
     *                        false 时保留合成阶段 + 候选键补充后的模式集，
     *                        便于看到 dept_advisor 例子里的 R_1 ⊆ R_2 这种冗余
     */
    public static Result decompose(Set<String> allAttrs, List<FunctionalDependency> fds,
                                   boolean removeRedundant) {
        requireInputs(allAttrs, fds);

        List<FunctionalDependency> fc = CanonicalCover.compute(fds).fds();
        List<Set<String>> synthesized = new ArrayList<Set<String>>();
        List<Step> steps = new ArrayList<Step>();
        for (FunctionalDependency fd : fc) {
            Set<String> schema = new TreeSet<String>(fd.determinant());
            schema.addAll(fd.dependent());
            synthesized.add(Collections.unmodifiableSet(schema));
            steps.add(Step.synthesized(steps.size() + 1, fd, schema));
        }

        List<Set<String>> schemas = new ArrayList<Set<String>>(synthesized);
        List<Set<String>> keys = FdCalculator.candidateKeys(allAttrs, fds);

        // "if 模式 R1..Ri 都不包含 R 的候选键 then 补一个候选键"
        boolean containsKey = false;
        for (Set<String> schema : schemas) {
            for (Set<String> key : keys) {
                if (schema.containsAll(key)) {
                    containsKey = true;
                    break;
                }
            }
            if (containsKey) {
                break;
            }
        }
        if (!containsKey) {
            Set<String> key = new TreeSet<String>(keys.get(0));   // "任意候选键"：取排序后的第一个，保证可复现
            schemas.add(key);
            steps.add(Step.candidateKey(steps.size() + 1, key));
        }

        if (removeRedundant) {
            removeContained(schemas, steps);
        }
        return new Result(allAttrs, fc, synthesized, schemas, keys, steps, fds);
    }

    /** 便捷：只要最终的模式列表。 */
    public static List<Set<String>> decompositionOf(Set<String> allAttrs,
                                                    List<FunctionalDependency> fds) {
        return decompose(allAttrs, fds).decomposition();
    }

    /**
     * 删除"包含于另一个模式"的冗余模式（图 8-12 的可选步骤）。
     *
     * <p>严格包含时删小的；两个模式完全相同时保留靠前的那个。删除顺序按列表顺序确定，
     * 保证同一输入的结果可复现。
     */
    private static void removeContained(List<Set<String>> schemas, List<Step> steps) {
        boolean removed = true;
        while (removed) {
            removed = false;
            for (int i = 0; i < schemas.size() && !removed; i++) {
                for (int j = 0; j < schemas.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    Set<String> candidate = schemas.get(i);
                    Set<String> container = schemas.get(j);
                    if (!container.containsAll(candidate)) {
                        continue;
                    }
                    // 严格包含才无条件删；两者相等时只删靠后的，保留一个
                    boolean strict = candidate.size() < container.size();
                    if (strict || i > j) {
                        Set<String> removedSchema = schemas.remove(i);
                        steps.add(Step.removed(steps.size() + 1, removedSchema, container));
                        removed = true;
                        break;
                    }
                }
            }
        }
    }

    // =====================================================================
    // 2. 3NF 判定（主属性口径）
    // =====================================================================

    /**
     * 判定<b>分解后的子模式</b> Ri 是否属于 3NF。
     *
     * <p>定义：对 F⁺ 中每条非平凡依赖 α→β（α, β ⊆ Ri），要么 α 是 Ri 的超键，
     * 要么 β−α 里<b>每个属性都是 Ri 的主属性</b>（出现在 Ri 的某个候选键里）。
     *
     * <p>实现与 BCNF 的子集法同构：枚举 Ri 的每个非空真子集 α，算 α⁺；若 α 不是 Ri 的超键，
     * 取 β = (α⁺−α)∩Ri —— 这是该 α 能推出的最大右部；只要 β 里有非主属性就违反 3NF。
     * （对同一个 α，任何合法的 β 都是这个最大 β 的子集，所以检查最大 β 就够。）
     *
     * <p>Ri 的候选键用"闭包盖住 Ri"来枚举（{@link FdCalculator#candidateKeys}），
     * 与整个 R 的候选键是两回事 —— 3NF 的"主属性"是相对 Ri 而言的。
     */
    public static ThirdNfCheck checkThirdNormalForm(Set<String> relation, Set<String> allAttrs,
                                                    List<FunctionalDependency> fds) {
        requireSubRelation(relation, allAttrs);
        requireAttrsWithin(fds, allAttrs, "F");

        List<Set<String>> keys = FdCalculator.candidateKeys(relation, fds);
        Set<String> prime = new TreeSet<String>();
        for (Set<String> key : keys) {
            prime.addAll(key);
        }

        List<ThirdNfViolation> violations = new ArrayList<ThirdNfViolation>();
        int checked = 0;
        for (Set<String> alpha : FdCalculator.subsets(relation)) {
            if (alpha.size() == relation.size()) {
                continue;                       // α = Ri 恒为超键
            }
            checked++;
            Set<String> closure = FdCalculator.attributeClosure(alpha, fds);
            if (closure.containsAll(relation)) {
                continue;                       // α 是 Ri 的超键，3NF 放行
            }
            Set<String> beta = new TreeSet<String>(closure);
            beta.retainAll(relation);
            beta.removeAll(alpha);
            if (beta.isEmpty()) {
                continue;
            }
            Set<String> nonPrime = new TreeSet<String>(beta);
            nonPrime.removeAll(prime);
            if (!nonPrime.isEmpty()) {
                FunctionalDependency witness = new FunctionalDependency(alpha, beta);
                violations.add(new ThirdNfViolation(witness, closure, nonPrime));
            }
        }
        return new ThirdNfCheck(relation, keys, prime, violations, checked);
    }

    /** 便捷：Ri 是否属于 3NF。 */
    public static boolean isThirdNormalForm(Set<String> relation, Set<String> allAttrs,
                                            List<FunctionalDependency> fds) {
        return checkThirdNormalForm(relation, allAttrs, fds).thirdNormalForm();
    }

    // =====================================================================
    // 3. 结果载体
    // =====================================================================

    /** 一次 3NF 分解的结果：F_c、合成模式、最终模式、候选键、步骤、自动复核结论。 */
    public static final class Result {
        private final Set<String> allAttrs;
        private final List<FunctionalDependency> canonicalCover;
        private final List<Set<String>> synthesized;
        private final List<Set<String>> decomposition;
        private final List<Set<String>> candidateKeys;
        private final List<Step> steps;
        private final boolean thirdNormalForm;
        private final boolean bcnf;
        private final boolean lossless;

        Result(Set<String> allAttrs, List<FunctionalDependency> canonicalCover,
               List<Set<String>> synthesized, List<Set<String>> decomposition,
               List<Set<String>> candidateKeys, List<Step> steps,
               List<FunctionalDependency> fds) {
            this.allAttrs = Collections.unmodifiableSet(new TreeSet<String>(allAttrs));
            this.canonicalCover = Collections.unmodifiableList(
                    new ArrayList<FunctionalDependency>(canonicalCover));
            List<Set<String>> syntheticCopy = new ArrayList<Set<String>>();
            for (Set<String> part : synthesized) {
                syntheticCopy.add(Collections.unmodifiableSet(new TreeSet<String>(part)));
            }
            this.synthesized = Collections.unmodifiableList(syntheticCopy);
            List<Set<String>> copy = new ArrayList<Set<String>>();
            for (Set<String> part : decomposition) {
                copy.add(Collections.unmodifiableSet(new TreeSet<String>(part)));
            }
            this.decomposition = Collections.unmodifiableList(copy);
            List<Set<String>> keyCopy = new ArrayList<Set<String>>();
            for (Set<String> key : candidateKeys) {
                keyCopy.add(Collections.unmodifiableSet(new TreeSet<String>(key)));
            }
            this.candidateKeys = Collections.unmodifiableList(keyCopy);
            this.steps = Collections.unmodifiableList(steps);

            boolean all3nf = true;
            for (Set<String> part : copy) {
                if (!checkThirdNormalForm(part, allAttrs, fds).thirdNormalForm()) {
                    all3nf = false;
                    break;
                }
            }
            this.thirdNormalForm = all3nf;
            this.bcnf = BcnfCheck.checkDecomposition(allAttrs, new ArrayList<Set<String>>(copy), fds).bcnf();
            this.lossless = LosslessJoin.checkChase(allAttrs, new ArrayList<Set<String>>(copy), fds).lossless();
        }

        public Set<String> allAttrs() {
            return allAttrs;
        }

        /** 正则覆盖 F_c（合成阶段用的依赖集）。 */
        public List<FunctionalDependency> canonicalCover() {
            return canonicalCover;
        }

        /** 合成阶段（未补候选键、未删冗余）的模式：每个 F_c 依赖一张表。 */
        public List<Set<String>> synthesized() {
            return synthesized;
        }

        /** 最终模式列表（补候选键、可选删冗余之后）。 */
        public List<Set<String>> decomposition() {
            return decomposition;
        }

        /** R 的全部候选键。 */
        public List<Set<String>> candidateKeys() {
            return candidateKeys;
        }

        public List<Step> steps() {
            return steps;
        }

        /** 最终每个模式是否都属于 3NF（用主属性口径复核）。 */
        public boolean thirdNormalForm() {
            return thirdNormalForm;
        }

        /** 最终每个模式是否都属于 BCNF（3NF 合成有时恰好也是 BCNF）。 */
        public boolean bcnf() {
            return bcnf;
        }

        /** 整个分解是否无损（chase 表格法复核）。 */
        public boolean lossless() {
            return lossless;
        }

        /** 是否保持依赖：3NF 合成按构造应当为 true。 */
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
            sb.append(" => ").append(thirdNormalForm ? "3NF" : "非 3NF")
                    .append(bcnf ? " (也是 BCNF)" : " (非 BCNF)")
                    .append(lossless ? " + 无损" : " + 有损");
            return sb.toString();
        }
    }

    /** 一步操作：合成一个模式 / 补一个候选键 / 删除一个冗余模式。 */
    public static final class Step {
        /** 步骤类型。 */
        public enum Kind {
            /** 由 F_c 中的一条依赖 α→β 生成模式 αβ。 */
            SYNTHESIZE,
            /** 所有模式都不含候选键，补一个候选键作为模式。 */
            ADD_CANDIDATE_KEY,
            /** 删除被另一个模式包含的冗余模式。 */
            REMOVE_REDUNDANT
        }

        private final int index;
        private final Kind kind;
        private final FunctionalDependency fd;
        private final Set<String> schema;
        private final Set<String> container;

        Step(int index, Kind kind, FunctionalDependency fd, Set<String> schema,
             Set<String> container) {
            this.index = index;
            this.kind = kind;
            this.fd = fd;
            this.schema = Collections.unmodifiableSet(new TreeSet<String>(schema));
            this.container = container == null
                    ? null
                    : Collections.unmodifiableSet(new TreeSet<String>(container));
        }

        static Step synthesized(int index, FunctionalDependency fd, Set<String> schema) {
            return new Step(index, Kind.SYNTHESIZE, fd, schema, null);
        }

        static Step candidateKey(int index, Set<String> key) {
            return new Step(index, Kind.ADD_CANDIDATE_KEY, null, key, null);
        }

        static Step removed(int index, Set<String> removed, Set<String> container) {
            return new Step(index, Kind.REMOVE_REDUNDANT, null, removed, container);
        }

        public int index() {
            return index;
        }

        public Kind kind() {
            return kind;
        }

        /** 合成步骤对应的依赖；补键/删冗余时为 null。 */
        public FunctionalDependency fd() {
            return fd;
        }

        /** 本步涉及的模式：合成出的模式 / 补的键 / 被删除的模式。 */
        public Set<String> schema() {
            return schema;
        }

        /** 删除步骤里"吞掉"被删模式的那个模式 R_k；其他步骤为 null。 */
        public Set<String> container() {
            return container;
        }

        @Override
        public String toString() {
            switch (kind) {
                case SYNTHESIZE:
                    return "第 " + index + " 步: 合成 " + FdCalculator.joinWithComma(schema)
                            + "  (来自 " + FdCalculator.joinWithComma(fd.determinant())
                            + " -> " + FdCalculator.joinWithComma(fd.dependent()) + ")";
                case ADD_CANDIDATE_KEY:
                    return "第 " + index + " 步: 所有模式都不含候选键, 补模式 "
                            + FdCalculator.joinWithComma(schema) + " (R 的一个候选键)";
                case REMOVE_REDUNDANT:
                    return "第 " + index + " 步: 删除冗余模式 " + FdCalculator.joinWithComma(schema)
                            + " (被 " + FdCalculator.joinWithComma(container) + " 包含)";
                default:
                    throw new IllegalStateException("未知步骤类型: " + kind);
            }
        }
    }

    /** 子模式 3NF 判定的完整结论：候选键、主属性、违反项、检查的子集数。 */
    public static final class ThirdNfCheck {
        private final Set<String> relation;
        private final List<Set<String>> candidateKeys;
        private final Set<String> primeAttributes;
        private final List<ThirdNfViolation> violations;
        private final int checked;

        ThirdNfCheck(Set<String> relation, List<Set<String>> candidateKeys,
                     Set<String> primeAttributes, List<ThirdNfViolation> violations, int checked) {
            this.relation = Collections.unmodifiableSet(new TreeSet<String>(relation));
            List<Set<String>> keyCopy = new ArrayList<Set<String>>();
            for (Set<String> key : candidateKeys) {
                keyCopy.add(Collections.unmodifiableSet(new TreeSet<String>(key)));
            }
            this.candidateKeys = Collections.unmodifiableList(keyCopy);
            this.primeAttributes = Collections.unmodifiableSet(new TreeSet<String>(primeAttributes));
            this.violations = Collections.unmodifiableList(violations);
            this.checked = checked;
        }

        public Set<String> relation() {
            return relation;
        }

        /** Ri 的候选键（不是整个 R 的）。 */
        public List<Set<String>> candidateKeys() {
            return candidateKeys;
        }

        /** 主属性 = 所有候选键的并集。 */
        public Set<String> primeAttributes() {
            return primeAttributes;
        }

        /** 违反 3NF 的依赖（右部含非主属性）。 */
        public List<ThirdNfViolation> violations() {
            return violations;
        }

        public boolean thirdNormalForm() {
            return violations.isEmpty();
        }

        public int checkedCount() {
            return checked;
        }

        @Override
        public String toString() {
            return FdCalculator.joinWithComma(relation) + ": "
                    + (thirdNormalForm() ? "属于 3NF" : "不属于 3NF, 违反项 " + violations);
        }
    }

    /** 一条违反 3NF 的依赖：β 中有非主属性。 */
    public static final class ThirdNfViolation {
        private final FunctionalDependency fd;
        private final Set<String> closure;
        private final Set<String> nonPrime;

        ThirdNfViolation(FunctionalDependency fd, Set<String> closure, Set<String> nonPrime) {
            this.fd = fd;
            this.closure = Collections.unmodifiableSet(new TreeSet<String>(closure));
            this.nonPrime = Collections.unmodifiableSet(new TreeSet<String>(nonPrime));
        }

        public FunctionalDependency fd() {
            return fd;
        }

        public Set<String> closure() {
            return closure;
        }

        /** 右部里不是主属性的那些属性。 */
        public Set<String> nonPrime() {
            return nonPrime;
        }

        @Override
        public String toString() {
            return FdCalculator.joinWithComma(fd.determinant()) + " -> "
                    + FdCalculator.joinWithComma(fd.dependent()) + " 违反 3NF (非主属性 "
                    + FdCalculator.joinWithComma(nonPrime) + ", 左部不是超键)";
        }
    }

    // =====================================================================
    // 4. 入参校验
    // =====================================================================

    private static void requireInputs(Set<String> allAttrs, List<FunctionalDependency> fds) {
        if (allAttrs == null || allAttrs.isEmpty()) {
            throw new IllegalArgumentException("关系模式不能为空");
        }
        requireAttrsWithin(fds, allAttrs, "F");
    }

    private static void requireSubRelation(Set<String> relation, Set<String> allAttrs) {
        if (relation == null || relation.isEmpty()) {
            throw new IllegalArgumentException("子模式不能为空");
        }
        if (allAttrs == null || allAttrs.isEmpty()) {
            throw new IllegalArgumentException("关系模式不能为空");
        }
        for (String attr : relation) {
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
                    throw new IllegalArgumentException(name + " 中的属性 " + attr
                            + " 不在属性集 " + FdCalculator.join(universe) + " 内");
                }
            }
            for (String attr : fd.dependent()) {
                if (!universe.contains(attr)) {
                    throw new IllegalArgumentException(name + " 中的属性 " + attr
                            + " 不在属性集 " + FdCalculator.join(universe) + " 内");
                }
            }
        }
    }
}
