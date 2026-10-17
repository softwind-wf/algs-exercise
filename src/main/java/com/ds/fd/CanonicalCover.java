package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 无关属性（extraneous attribute）检测与正则覆盖（canonical cover）计算。
 *
 * <p>对应教材《数据库系统概念》8.4.3 节与图 8-9（第 194 页）：
 * <ul>
 *   <li><b>无关属性</b>：从一条 FD 中去掉后不改变 F 所逻辑蕴含的依赖集合（即不改变闭包）的属性；</li>
 *   <li><b>正则覆盖 F_c</b>：与 F 有相同闭包、不含任何无关属性、且左部互不相同的依赖集。
 *       因为它最小，验证"数据库是否满足 F"只需验证 F_c。</li>
 * </ul>
 *
 * <p>图 8-9 的伪代码：
 * <pre>
 * F_c = F;
 * repeat
 *     用合并律把 X→Y1 与 X→Y2 合并成 X→Y1Y2;
 *     在 F_c 中找一条含无关属性的 FD，删掉那个属性;
 *          （注意：用 F_c 而不是最初的 F 检验无关属性；右部删空则整条依赖删除）
 * until (F_c 不变)
 * </pre>
 *
 * <p><b>易错点</b>：①检测必须用"当前"的 F_c，而不是最初的 F —— 否则在已经化简过的依赖集上
 * 会误判；②左部无关与右部无关的检验方向相反：左部看"少一个属性后还能不能推出右边"，
 * 右部看"把该属性从右边拿掉后，剩余部分能不能反推出它"。
 *
 * <p><b>两个实现约束</b>：①右部为空的依赖（恒成立、不约束任何东西）在入口直接丢弃，
 * 否则会被逐步删成 ∅→∅ 垃圾；②合并律按属性集合分组，不按拼接字符串 —— 属性名可以是多字符的，
 * {@code {A, BC}} 与 {@code {AB, C}} 必须视为不同左部。
 */
public final class CanonicalCover {

    private CanonicalCover() {
    }

    // =====================================================================
    // 1. 无关属性检测
    // =====================================================================

    /**
     * 检测 {@code target} 在依赖集 {@code fds}（必须包含 target）中的无关属性。
     *
     * <p>左部属性 A：令 {@code γ = α − {A}}，计算 γ⁺；若 {@code β ⊆ γ⁺}，则 A 在左部无关
     * （因为 A 是多余的，去掉后 γ 仍能推出整个 β）。
     *
     * <p>右部属性 A：令 {@code F′ = (fds − {α→β}) ∪ {α→(β−{A})}}，计算 α⁺；
     * 若 {@code A ∈ α⁺}，则 A 在右部无关（因为剩余部分已经能反推出 A）。
     */
    public static ExtraneousCheck check(FunctionalDependency target, List<FunctionalDependency> fds) {
        int index = fds.indexOf(target);
        if (index < 0) {
            throw new IllegalArgumentException("fds 不包含待检测的依赖: " + target);
        }
        List<FunctionalDependency> others = new ArrayList<FunctionalDependency>(fds);
        others.remove(index);

        // 左部：α − {A} 的闭包是否已包含整个 β
        Set<String> lhsExtraneous = new TreeSet<String>();
        for (String a : target.determinant()) {
            Set<String> gamma = new TreeSet<String>(target.determinant());
            gamma.remove(a);
            if (FdCalculator.attributeClosure(gamma, fds).containsAll(target.dependent())) {
                lhsExtraneous.add(a);
            }
        }

        // 右部：F′ 下 α 的闭包是否包含 A
        Set<String> rhsExtraneous = new TreeSet<String>();
        for (String a : target.dependent()) {
            Set<String> beta = new TreeSet<String>(target.dependent());
            beta.remove(a);
            List<FunctionalDependency> fPrime = new ArrayList<FunctionalDependency>(others);
            if (!beta.isEmpty()) {
                fPrime.add(new FunctionalDependency(target.determinant(), beta));
            }
            if (FdCalculator.attributeClosure(target.determinant(), fPrime).contains(a)) {
                rhsExtraneous.add(a);
            }
        }
        return new ExtraneousCheck(target, lhsExtraneous, rhsExtraneous);
    }

    /** 一条 FD 的无关属性检测结果：左部无关集合、右部无关集合。 */
    public static final class ExtraneousCheck {
        private final FunctionalDependency fd;
        private final Set<String> lhsExtraneous;
        private final Set<String> rhsExtraneous;

        ExtraneousCheck(FunctionalDependency fd, Set<String> lhsExtraneous, Set<String> rhsExtraneous) {
            this.fd = fd;
            this.lhsExtraneous = Collections.unmodifiableSet(lhsExtraneous);
            this.rhsExtraneous = Collections.unmodifiableSet(rhsExtraneous);
        }

        public FunctionalDependency fd() {
            return fd;
        }

        /** 左部中可删除的属性（删掉不影响 F 的闭包）。 */
        public Set<String> lhsExtraneous() {
            return lhsExtraneous;
        }

        /** 右部中可删除的属性。 */
        public Set<String> rhsExtraneous() {
            return rhsExtraneous;
        }

        public boolean any() {
            return !lhsExtraneous.isEmpty() || !rhsExtraneous.isEmpty();
        }

        @Override
        public String toString() {
            if (!any()) {
                return fd + " 无无关属性";
            }
            StringBuilder sb = new StringBuilder(fd.toString());
            if (!lhsExtraneous.isEmpty()) {
                sb.append(" 左部无关=").append(FdCalculator.join(lhsExtraneous));
            }
            if (!rhsExtraneous.isEmpty()) {
                sb.append(" 右部无关=").append(FdCalculator.join(rhsExtraneous));
            }
            return sb.toString();
        }
    }

    // =====================================================================
    // 2. 正则覆盖（图 8-9）
    // =====================================================================

    /** 求正则覆盖；返回依赖集与化简步骤，便于核对教材手算过程。 */
    public static Result compute(List<FunctionalDependency> fds) {
        List<FunctionalDependency> fc = new ArrayList<FunctionalDependency>();
        List<String> steps = new ArrayList<String>();
        for (FunctionalDependency fd : new LinkedHashSet<FunctionalDependency>(fds)) {
            // 右部为空的依赖恒成立、不约束任何东西；留在集合里会被逐步删空成 ∅→∅ 垃圾
            if (fd.dependent().isEmpty()) {
                steps.add("丢弃右部为空的依赖: " + fd + " (恒成立, 不约束任何东西)");
                continue;
            }
            fc.add(fd);
        }

        boolean changed = true;
        while (changed) {
            changed = mergeSameLeft(fc, steps);

            // 图 8-9：每轮只删一个无关属性，删完重新从合并律开始
            int index = -1;
            String attr = null;
            boolean inLhs = false;
            for (int i = 0; i < fc.size(); i++) {
                ExtraneousCheck check = check(fc.get(i), fc);   // 用 F_c 而不是最初的 F
                if (!check.lhsExtraneous().isEmpty()) {
                    index = i;
                    attr = check.lhsExtraneous().iterator().next();
                    inLhs = true;
                    break;
                }
                if (!check.rhsExtraneous().isEmpty()) {
                    index = i;
                    attr = check.rhsExtraneous().iterator().next();
                    inLhs = false;
                    break;
                }
            }
            if (index < 0) {
                continue;
            }

            FunctionalDependency fd = fc.get(index);
            Set<String> det = new TreeSet<String>(fd.determinant());
            Set<String> dep = new TreeSet<String>(fd.dependent());
            if (inLhs) {
                det.remove(attr);
                FunctionalDependency simplified = new FunctionalDependency(det, dep);
                fc.set(index, simplified);
                steps.add("删除左部无关属性: " + fd + " => " + simplified);
            } else {
                dep.remove(attr);
                if (dep.isEmpty()) {
                    fc.remove(index);
                    steps.add("删除右部无关属性: " + fd + " => 右部为空, 整条删除");
                } else {
                    FunctionalDependency simplified = new FunctionalDependency(det, dep);
                    fc.set(index, simplified);
                    steps.add("删除右部无关属性: " + fd + " => " + simplified);
                }
            }
            changed = true;
        }
        return new Result(fc, steps);
    }

    /** 只要结果，不要过程。 */
    public static List<FunctionalDependency> cover(List<FunctionalDependency> fds) {
        return compute(fds).fds();
    }

    /**
     * 合并律：把左部相同的依赖合并成一条；重复依赖顺带去重。
     *
     * <p>分组键必须用属性集合本身，不能用 {@code join()} 拼出来的字符串：
     * 属性名可以是多字符的（本工具箱的 {@code FunctionalDependency} 支持
     * {@code "seat_no"} 这种名字），{@code {A, BC}} 与 {@code {AB, C}} 拼接后都是
     * {@code "ABC"}，拼串做键会把两个不同左部误当成同一个，直接改变依赖集语义。
     *
     * @return 是否发生了变化（变化后需要重新检测无关属性）
     */
    private static boolean mergeSameLeft(List<FunctionalDependency> fc, List<String> steps) {
        LinkedHashMap<Set<String>, Set<String>> rhsByLhs =
                new LinkedHashMap<Set<String>, Set<String>>();
        LinkedHashMap<Set<String>, List<String>> rhsPieces =
                new LinkedHashMap<Set<String>, List<String>>();

        for (FunctionalDependency fd : fc) {
            Set<String> key = new TreeSet<String>(fd.determinant());   // 按集合内容分组，与属性名长度无关
            Set<String> rhs = rhsByLhs.get(key);
            if (rhs == null) {
                rhs = new TreeSet<String>();
                rhsByLhs.put(key, rhs);
                rhsPieces.put(key, new ArrayList<String>());
            }
            rhs.addAll(fd.dependent());
            rhsPieces.get(key).add(FdCalculator.joinWithComma(fd.dependent()));
        }

        List<FunctionalDependency> merged = new ArrayList<FunctionalDependency>();
        for (Map.Entry<Set<String>, Set<String>> e : rhsByLhs.entrySet()) {
            merged.add(new FunctionalDependency(e.getKey(), e.getValue()));
        }
        boolean changed = !merged.equals(fc);
        if (changed) {
            for (Map.Entry<Set<String>, List<String>> e : rhsPieces.entrySet()) {
                if (e.getValue().size() > 1) {
                    StringBuilder sb = new StringBuilder("合并律: ");
                    for (int i = 0; i < e.getValue().size(); i++) {
                        sb.append(FdCalculator.joinWithComma(e.getKey())).append(" -> ")
                                .append(e.getValue().get(i));
                        if (i < e.getValue().size() - 1) {
                            sb.append(", ");
                        }
                    }
                    sb.append(" 合并为 ").append(FdCalculator.joinWithComma(e.getKey()))
                            .append(" -> ").append(FdCalculator.joinWithComma(rhsByLhs.get(e.getKey())));
                    steps.add(sb.toString());
                }
            }
            fc.clear();
            fc.addAll(merged);
        }
        return changed;
    }

    /** 正则覆盖的结果：依赖集 + 化简步骤。 */
    public static final class Result {
        private final List<FunctionalDependency> fds;
        private final List<String> steps;

        Result(List<FunctionalDependency> fds, List<String> steps) {
            this.fds = Collections.unmodifiableList(new ArrayList<FunctionalDependency>(fds));
            this.steps = Collections.unmodifiableList(new ArrayList<String>(steps));
        }

        public List<FunctionalDependency> fds() {
            return fds;
        }

        /** 每一步化简的文字说明（合并 / 删除左部属性 / 删除右部属性）。 */
        public List<String> steps() {
            return steps;
        }

        public int size() {
            return fds.size();
        }
    }
}
