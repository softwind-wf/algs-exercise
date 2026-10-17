package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 保持依赖（dependency preservation）判定，对应教材 8.4.5 节、图 8-10 与图 8-11。
 *
 * <p>定义：分解 D = {R1,…,Rn} 保持依赖，当且仅当
 * {@code (F1 ∪ … ∪ Fn)⁺ = F⁺}，其中 {@code Fi = F⁺ 在 Ri 上的限定}（只含左右部都在 Ri 里的 FD）。
 * 等价说法：F 里每一条函数依赖都能由"各子模式自己的 FD 并集"推出来。
 *
 * <p>两种判定：
 * <ol>
 *   <li><b>方法一（图 8-10）</b>：先算 F⁺，再按子模式取限定 Fi，拼成 F'，再算 F'⁺ 与 F⁺ 比较。
 *       忠实但昂贵：F⁺ 的条数是指数级的。</li>
 *   <li><b>方法二（修改属性闭包）</b>：不构造 F'，对 F 中的每条 α→β 单独算"投影闭包"：
 *       <pre>
 * result = α;
 * repeat
 *     for each Ri:
 *         t = (result ∩ Ri)⁺ ∩ Ri;   // 只保留 Ri 内的属性
 *         result = result ∪ t;
 * until (result 不变)
 *       </pre>
 *       若结果包含 β，这条依赖保持；所有依赖都保持才算保持依赖。代价是多项式时间。</li>
 * </ol>
 *
 * <p>方法二为什么对：见 {@link #checkByModifiedClosure} 的注释与
 * {@code docs/教材术语速查-函数依赖与范式.md} 第 15 条的"正确性"一段。
 */
public final class DependencyPreservation {

    private DependencyPreservation() {
    }

    // =====================================================================
    // 方法一：图 8-10，显式计算 F⁺
    // =====================================================================

    /** 图 8-10 的判定：算 F⁺ → 限定出 Fi → F' → 比较 F'⁺ 与 F⁺。 */
    public static FPlusCheck checkByFPlus(Set<String> allAttrs, List<Set<String>> decomposition,
                                          List<FunctionalDependency> fds) {
        requireCoverage(allAttrs, decomposition);

        Set<FunctionalDependency> fplus = FdCalculator.closureByAttributeClosures(allAttrs, fds);

        // Fi：F⁺ 中左右部都落在 Ri 里的那些依赖
        Set<FunctionalDependency> fprime = new LinkedHashSet<FunctionalDependency>();
        for (Set<String> ri : decomposition) {
            for (FunctionalDependency fd : fplus) {
                if (ri.containsAll(fd.determinant()) && ri.containsAll(fd.dependent())) {
                    fprime.add(fd);
                }
            }
        }
        Set<FunctionalDependency> fprimePlus =
                FdCalculator.closureByAttributeClosures(allAttrs, new ArrayList<FunctionalDependency>(fprime));

        Set<FunctionalDependency> lost = new LinkedHashSet<FunctionalDependency>();
        for (FunctionalDependency fd : fds) {
            if (!fprimePlus.contains(fd)) {
                lost.add(fd);
            }
        }
        return new FPlusCheck(fplus, fprime, fprimePlus, lost);
    }

    /** 方法一的结果：F⁺、F'（各限定之并）、F'⁺、以及被丢掉的依赖。 */
    public static final class FPlusCheck {
        private final Set<FunctionalDependency> fplus;
        private final Set<FunctionalDependency> fprime;
        private final Set<FunctionalDependency> fprimePlus;
        private final Set<FunctionalDependency> lost;

        FPlusCheck(Set<FunctionalDependency> fplus, Set<FunctionalDependency> fprime,
                   Set<FunctionalDependency> fprimePlus, Set<FunctionalDependency> lost) {
            this.fplus = Collections.unmodifiableSet(fplus);
            this.fprime = Collections.unmodifiableSet(fprime);
            this.fprimePlus = Collections.unmodifiableSet(fprimePlus);
            this.lost = Collections.unmodifiableSet(lost);
        }

        public Set<FunctionalDependency> fplus() {
            return fplus;
        }

        public Set<FunctionalDependency> fprime() {
            return fprime;
        }

        public Set<FunctionalDependency> fprimePlus() {
            return fprimePlus;
        }

        /** F 中不能被 F'⁺ 推出的依赖（保持依赖时为空）。 */
        public Set<FunctionalDependency> lost() {
            return lost;
        }

        public boolean preserves() {
            return lost.isEmpty();
        }
    }

    // =====================================================================
    // 方法二：修改属性闭包（不构造 F'）
    // =====================================================================

    /**
     * 图 8-11 的判定：对 F 的每条 α→β，用"投影闭包"代替 F'⁺ 下的 α⁺。
     *
     * <p><b>正确性</b>（两个方向都不依赖 F'⁺ 的显式构造）：
     * <ol>
     *   <li><b>每一步都是 F' 推出的</b>：设 γ = result ∩ Ri，t = γ⁺_F ∩ Ri。
     *       因为 γ ⊆ Ri 且每个 t 中属性都在 Ri，所以 γ→t ∈ F⁺ 且左右部都在 Ri 内，
     *       按定义 γ→t ∈ Fi ⊆ F'。于是 result 增长的每个属性都在 α⁺_{F'} 里。</li>
     *   <li><b>F' 能推出的属性最终都会被收进来</b>：F' 中每条依赖都属于某个 Fi，
     *       其左右部都在同一个 Ri 里。对"α 到某属性在 F' 下的推导长度"做归纳：
     *       用到 Fi 里的 FD 时，它的左部已经在 result 中；取 result ∩ Ri（包含该左部），
     *       该 FD 在 F 下可用，故右部落在 (result ∩ Ri)⁺_F 里，从而在 t 里被并入。
     *       因此不动点时 result 恰为 α⁺_{F'}。</li>
     * </ol>
     *
     * <p>所以"β ⊆ result"等价于"α→β ∈ F'⁺"。F' ⊆ F⁺ 恒成立，故
     * {@code F'⁺ = F⁺ ⟺ F 的每条依赖都在 F'⁺ 中}，逐条检查即得分解是否保持依赖。
     *
     * <p>为什么用 {@code (result∩Ri)⁺ ∩ Ri} 而不是直接算 Fi 的闭包：因为
     * γ→(γ⁺_F ∩ Ri) 恰好就是 Fi 中的那条依赖（左右部都在 Ri、且属于 F⁺），
     * 求一次 F 下的闭包再截断，等价于在 Fi 下求闭包，却不用先把 Fi 物化出来。
     */
    public static ClosureCheck checkByModifiedClosure(Set<String> allAttrs,
                                                      List<Set<String>> decomposition,
                                                      List<FunctionalDependency> fds) {
        requireCoverage(allAttrs, decomposition);
        List<Verdict> verdicts = new ArrayList<Verdict>();
        Set<FunctionalDependency> lost = new LinkedHashSet<FunctionalDependency>();

        for (FunctionalDependency fd : fds) {
            Set<String> result = new TreeSet<String>(fd.determinant());
            boolean changed = true;
            while (changed) {
                changed = false;
                for (Set<String> ri : decomposition) {
                    Set<String> seed = new TreeSet<String>(result);
                    seed.retainAll(ri);
                    Set<String> t = new TreeSet<String>(FdCalculator.attributeClosure(seed, fds));
                    t.retainAll(ri);
                    if (result.addAll(t)) {
                        changed = true;
                    }
                }
            }
            boolean preserved = result.containsAll(fd.dependent());
            verdicts.add(new Verdict(fd, result, preserved));
            if (!preserved) {
                lost.add(fd);
            }
        }
        return new ClosureCheck(verdicts, lost);
    }

    /** 只要结论：分解是否保持依赖（走方法二，多项式时间）。 */
    public static boolean preserves(Set<String> allAttrs, List<Set<String>> decomposition,
                                    List<FunctionalDependency> fds) {
        return checkByModifiedClosure(allAttrs, decomposition, fds).preserves();
    }

    /** 方法二的结果：每条依赖的投影闭包与结论。 */
    public static final class ClosureCheck {
        private final List<Verdict> verdicts;
        private final Set<FunctionalDependency> lost;

        ClosureCheck(List<Verdict> verdicts, Set<FunctionalDependency> lost) {
            this.verdicts = Collections.unmodifiableList(verdicts);
            this.lost = Collections.unmodifiableSet(lost);
        }

        public List<Verdict> verdicts() {
            return verdicts;
        }

        public Set<FunctionalDependency> lost() {
            return lost;
        }

        public boolean preserves() {
            return lost.isEmpty();
        }
    }

    /** 单条依赖的判定明细：投影闭包最终结果 + 是否保持。 */
    public static final class Verdict {
        private final FunctionalDependency fd;
        private final Set<String> closure;
        private final boolean preserved;

        Verdict(FunctionalDependency fd, Set<String> closure, boolean preserved) {
            this.fd = fd;
            this.closure = Collections.unmodifiableSet(closure);
            this.preserved = preserved;
        }

        public FunctionalDependency fd() {
            return fd;
        }

        /** 投影闭包 result 的最终值（不是 α⁺_F，而是 α⁺_{F'} 的逐步计算结果）。 */
        public Set<String> closure() {
            return closure;
        }

        public boolean preserved() {
            return preserved;
        }

        @Override
        public String toString() {
            return fd + (preserved ? " 保持" : " 丢失") + " (result = " + FdCalculator.join(closure) + ")";
        }
    }

    // =====================================================================
    // 校验
    // =====================================================================

    private static void requireCoverage(Set<String> allAttrs, List<Set<String>> decomposition) {
        if (decomposition.isEmpty()) {
            throw new IllegalArgumentException("分解不能为空");
        }
        Set<String> union = new TreeSet<String>();
        for (Set<String> part : decomposition) {
            if (part.isEmpty()) {
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
