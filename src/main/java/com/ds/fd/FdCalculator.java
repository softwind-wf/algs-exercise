package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 函数依赖与范式的计算工具箱（纯算法，不依赖任何数据库）。
 *
 * <p>包含三类算法，全部只依赖 Armstrong 公理与集合运算：
 * <ol>
 *   <li>{@link #subsets} —— 枚举属性集的所有非空子集（闭包计算的输入空间）；</li>
 *   <li>{@link #attributeClosure} —— 属性闭包 X⁺（教材图 8-8 的算法，实践中真正要用的那一个）；
 *       需要"过程"而不只是"结果"时用 {@link #attributeClosureTrace}；</li>
 *   <li>{@link #closureNaive} —— 教材图 8-7 的"暴力枚举 F⁺"算法，用于对照教学。</li>
 * </ol>
 *
 * <p>术语约定：候选键 = 极小超键；超键 = 闭包等于全部属性的属性集。
 */
public final class FdCalculator {

    private FdCalculator() {
    }

    // =====================================================================
    // 1. 集合工具
    // =====================================================================

    /** 把 {@code attrs} 的所有非空子集按"先按元素个数、再按字典序"列出，保证结果稳定可 diff。 */
    public static List<Set<String>> subsets(Set<String> attrs) {
        List<String> sorted = new ArrayList<String>(new TreeSet<String>(attrs));
        int n = sorted.size();
        List<Set<String>> out = new ArrayList<Set<String>>();
        for (int mask = 1; mask < (1 << n); mask++) {
            Set<String> s = new TreeSet<String>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    s.add(sorted.get(i));
                }
            }
            out.add(s);
        }
        Collections.sort(out, new Comparator<Set<String>>() {
            @Override
            public int compare(Set<String> a, Set<String> b) {
                if (a.size() != b.size()) {
                    return a.size() - b.size();
                }
                String x = join(a);
                String y = join(b);
                return x.compareTo(y);
            }
        });
        return out;
    }

    /** 属性集打印：{@code ABCH} 形式（无分隔符，和教材符号一致）。 */
    public static String join(Set<String> attrs) {
        StringBuilder sb = new StringBuilder();
        for (String a : new TreeSet<String>(attrs)) {
            sb.append(a);
        }
        return sb.toString();
    }

    /** 属性集打印（带逗号），用于表格里展示，形如 {@code { A,G } }。 */
    public static String joinWithComma(Set<String> attrs) {
        return "{ " + new TreeSet<String>(attrs).toString().replaceAll("[\\[\\] ]", "") + " }";
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> u = new TreeSet<String>(a);
        u.addAll(b);
        return u;
    }

    // =====================================================================
    // 2. 属性闭包 X⁺ —— 教材图 8-8 算法：每轮扫一遍 F，最多 |R|+1 轮
    // =====================================================================

    /**
     * 求属性集 {@code x} 在 FD 集合 {@code fds} 下的闭包 X⁺（教材图 8-8，第 193 页）。
     *
     * <p>伪代码逐行对应：
     * <pre>
     * result := α;
     * repeat
     *     for each 函数依赖 β→γ in F do
     *         if β ⊆ result then result := result ∪ γ;
     * until (result 不变)
     * </pre>
     *
     * <p>正确性要点（教材 8.4.2 的证明）：循环中每次加入的 γ 都由 F 中的 FD 从 result 推出，
     * 而 result 又是由 α 推出的，按传递律 α → γ 成立，所以加入的属性必然属于 α⁺；
     * 反过来，若某属性在 α⁺ 中却不在 result 中，则它必由某条 β→γ（β ⊆ result）引入，
     * 而算法结束前所有 FD 都扫过，矛盾 —— 故结束时 result 恰为 α⁺。
     *
     * <p>复杂度：每轮至少多推出一个属性（否则终止），最多 |R|+1 轮，每轮扫 |F| 条 FD，
     * 上界 O(|R| · |F|) 次子集判断；与 |F⁺| 无关 —— 这正是它比图 8-7 实用的原因。
     *
     * <p>本方法只返回结果；要看"哪一轮用了哪条 FD"用 {@link #attributeClosureTrace}。
     */
    public static Set<String> attributeClosure(Set<String> x, List<FunctionalDependency> fds) {
        return attributeClosureTrace(x, fds).closure();
    }

    /**
     * 图 8-8 算法的带过程版本：除了闭包本身，还记录每轮结束后 result 的快照，
     * 以及"真正改变了 result"的那些 (轮次, FD, 变化前, 变化后)。
     *
     * <p>教材下方对 (AG)⁺ 的手算过程就是这份记录：第 1 轮依次 A→B、A→C、CG→H、CG→I 生效，
     * 第 2 轮所有 FD 的右边都已在 result 中 ⇒ result 不变 ⇒ 终止，得到 ABCGHI。
     *
     * <p>注意 "β ⊆ result 成立" 与 "result 真的变大" 是两件事：β 包含于 result 但 γ 已被
     * result 包含时（例如 B→H 在 (ABCGHI) 上），算法什么也不做，这种"空转"不记为一步。
     */
    public static ClosureTrace attributeClosureTrace(Set<String> x, List<FunctionalDependency> fds) {
        Set<String> result = new TreeSet<String>(x);
        List<ClosureStep> steps = new ArrayList<ClosureStep>();
        List<Set<String>> roundResults = new ArrayList<Set<String>>();

        int round = 0;
        boolean changed = true;
        while (changed) {
            round++;
            changed = false;
            for (FunctionalDependency fd : fds) {
                if (!result.containsAll(fd.determinant())) {
                    continue;                       // β ⊄ result，这条 FD 本轮用不上
                }
                if (result.containsAll(fd.dependent())) {
                    continue;                       // γ ⊆ result，推不出新属性（空转，不记一步）
                }
                Set<String> before = new TreeSet<String>(result);
                result.addAll(fd.dependent());
                changed = true;
                steps.add(new ClosureStep(round, fd, before, new TreeSet<String>(result)));
            }
            roundResults.add(new TreeSet<String>(result));
        }
        return new ClosureTrace(new TreeSet<String>(x), new TreeSet<String>(result),
                steps, roundResults, round);
    }

    /** 图 8-8 算法中的一步：第 {@code round} 轮，FD {@code applied} 让 result 从 {@code before} 变成 {@code after}。 */
    public static final class ClosureStep {
        private final int round;
        private final FunctionalDependency applied;
        private final Set<String> before;
        private final Set<String> after;

        ClosureStep(int round, FunctionalDependency applied, Set<String> before, Set<String> after) {
            this.round = round;
            this.applied = applied;
            this.before = Collections.unmodifiableSet(before);
            this.after = Collections.unmodifiableSet(after);
        }

        public int round() {
            return round;
        }

        public FunctionalDependency applied() {
            return applied;
        }

        public Set<String> before() {
            return before;
        }

        public Set<String> after() {
            return after;
        }

        /** 本步新加入的属性（γ − 变化前的 result）。 */
        public Set<String> added() {
            Set<String> added = new TreeSet<String>(after);
            added.removeAll(before);
            return added;
        }

        @Override
        public String toString() {
            return "第 " + round + " 轮 " + applied + ": "
                    + join(before) + " -> " + join(after);
        }
    }

    /** 图 8-8 算法的完整执行记录：初始 α、结果 α⁺、每轮快照、逐步明细、轮数。 */
    public static final class ClosureTrace {
        private final Set<String> initial;
        private final Set<String> closure;
        private final List<ClosureStep> steps;
        private final List<Set<String>> roundResults;
        private final int totalRounds;

        ClosureTrace(Set<String> initial, Set<String> closure,
                     List<ClosureStep> steps, List<Set<String>> roundResults, int totalRounds) {
            this.initial = Collections.unmodifiableSet(initial);
            this.closure = Collections.unmodifiableSet(closure);
            this.steps = Collections.unmodifiableList(steps);
            this.roundResults = Collections.unmodifiableList(roundResults);
            this.totalRounds = totalRounds;
        }

        public Set<String> initial() {
            return initial;
        }

        public Set<String> closure() {
            return closure;
        }

        /** 真正改变过 result 的步骤，按发生顺序。 */
        public List<ClosureStep> steps() {
            return steps;
        }

        /** 每轮 repeat 结束时的 result 快照（含最后一轮"无变化"的快照）。 */
        public List<Set<String>> roundResults() {
            return roundResults;
        }

        /** repeat 循环的执行轮数；最后一轮必为"无变化"轮，因此恒 ≥ 1。 */
        public int totalRounds() {
            return totalRounds;
        }
    }

    /**
     * 超键判定（教材 8.4.2）：{@code α 是 R 的超键 ⟺ α⁺ ⊇ R}。
     *
     * <p>只要闭包装下了全部属性，α 就能函数决定整行，即"知道 α 就知道整条元组"。
     * 超键**不要求极小**：AG 是超键，ABG、ACG、ABCGHI 也都是；极小性由
     * {@link #candidateKeys} 另外保证。所以本方法只回答"是不是超键"，不回答"是不是候选键"。
     *
     * <p>返回闭包与"缺哪些属性"，是为了让"不是超键"这个结论可解释：
     * 缺口非空就说明这些属性推不出来，α 不能唯一确定元组。
     */
    public static SuperkeyCheck checkSuperkey(Set<String> x, Set<String> allAttrs,
                                              List<FunctionalDependency> fds) {
        Set<String> closure = attributeClosure(x, fds);
        Set<String> missing = new TreeSet<String>(allAttrs);
        missing.removeAll(closure);
        return new SuperkeyCheck(new TreeSet<String>(x), closure, missing);
    }

    /** 超键判定的完整结论：α、α⁺、缺哪些属性、是否超键。 */
    public static final class SuperkeyCheck {
        private final Set<String> attrs;
        private final Set<String> closure;
        private final Set<String> missing;

        SuperkeyCheck(Set<String> attrs, Set<String> closure, Set<String> missing) {
            this.attrs = Collections.unmodifiableSet(attrs);
            this.closure = Collections.unmodifiableSet(closure);
            this.missing = Collections.unmodifiableSet(missing);
        }

        /** 被判断的属性集 α。 */
        public Set<String> attrs() {
            return attrs;
        }

        /** α⁺，即 α 在 F 下能函数决定的全部属性。 */
        public Set<String> closure() {
            return closure;
        }

        /** R − α⁺：闭包里还缺哪些属性；空集就说明是超键。 */
        public Set<String> missing() {
            return missing;
        }

        /** α 是否为超键：等价于 {@code missing().isEmpty()}。 */
        public boolean isSuperkey() {
            return missing.isEmpty();
        }

        @Override
        public String toString() {
            return join(attrs) + "+ = " + join(closure)
                    + (isSuperkey() ? " (全属性齐 => 超键)" : " (缺 " + join(missing) + " => 不是超键)");
        }
    }

    /** X 是不是超键：X⁺ 是否包含全部属性（{@link #checkSuperkey} 的简写）。 */
    public static boolean isSuperkey(Set<String> x, Set<String> allAttrs, List<FunctionalDependency> fds) {
        return checkSuperkey(x, allAttrs, fds).isSuperkey();
    }

    /** 求全部候选键（极小超键），结果按"先按元素个数、再按字典序"排序。 */
    public static List<Set<String>> candidateKeys(Set<String> allAttrs, List<FunctionalDependency> fds) {
        List<Set<String>> keys = new ArrayList<Set<String>>();
        for (Set<String> x : subsets(allAttrs)) {
            if (!isSuperkey(x, allAttrs, fds)) {
                continue;
            }
            boolean minimal = true;
            for (Set<String> k : keys) {
                if (x.containsAll(k)) {          // 已经有一条更小的键被它包含 => 非极小
                    minimal = false;
                    break;
                }
            }
            if (minimal) {
                keys.add(x);
            }
        }
        return keys;
    }

    // =====================================================================
    // 3. F⁺ 的两种算法
    // =====================================================================

    /**
     * 教材图 8-7：从 F⁺ = F 出发，反复对集合内每条 FD 应用自反律/增广律，
     * 并对任意两条应用传递律，直到不再变化。
     *
     * <p><b>注意（实测发现的边界）</b>：自反律这里只对"已经是某条 FD 左边的属性集 X"生成
     * X 的子集依赖。若某属性从未单独作为左边出现过（例如 C 只出现在 CG 里），则 C→C 这种
     * 平凡依赖永远生不出来 —— 算法不是在"实质结论"上错，而是没有规定初值。
     */
    public static Result closureNaive(Set<String> allAttrs, List<FunctionalDependency> initial) {
        Set<FunctionalDependency> fp = new java.util.LinkedHashSet<FunctionalDependency>(initial);
        List<int[]> rounds = new ArrayList<int[]>();     // {轮次, 本轮生成候选, 本轮新增, 累计}

        // 预计算一次属性子集（每轮复用，省内存）
        List<Set<String>> attrSubsets = subsets(allAttrs);

        int round = 0;
        while (true) {
            round++;
            int before = fp.size();
            Set<FunctionalDependency> next = new java.util.LinkedHashSet<FunctionalDependency>();
            List<FunctionalDependency> snapshot = new ArrayList<FunctionalDependency>(fp);

            // 第一个 for：自反律 + 增广律
            for (FunctionalDependency fd : snapshot) {
                // 自反律：X → X 的任意非空子集
                for (Set<String> sub : subsets(fd.determinant())) {
                    next.add(new FunctionalDependency(fd.determinant(), sub));
                }
                // 增广律：X → Y  =>  XZ → YZ，Z 取所有非空属性子集（Z = ∅ 就是原 FD）
                for (Set<String> z : attrSubsets) {
                    next.add(new FunctionalDependency(
                            union(fd.determinant(), z),
                            union(fd.dependent(), z)));
                }
            }

            // 第二个 for：传递律
            for (FunctionalDependency f1 : snapshot) {
                for (FunctionalDependency f2 : snapshot) {
                    if (f1.dependent().equals(f2.determinant())) {
                        next.add(new FunctionalDependency(f1.determinant(), f2.dependent()));
                    }
                }
            }

            fp.addAll(next);
            rounds.add(new int[]{round, next.size(), fp.size() - before, fp.size()});
            if (fp.size() == before) {
                break;
            }
        }
        return new Result(fp, rounds, round);
    }

    /**
     * F⁺ 的集合论算法：{@code F⁺ = { X→Y : Y ⊆ X⁺ }}。
     *
     * <p>这就是"闭包"的定义用法 —— 不需要迭代公理，只需要 63 次属性闭包计算。
     */
    public static Set<FunctionalDependency> closureByAttributeClosures(
            Set<String> allAttrs, List<FunctionalDependency> fds) {
        Set<FunctionalDependency> fp = new java.util.LinkedHashSet<FunctionalDependency>();
        List<Set<String>> attrSubsets = subsets(allAttrs);
        for (Set<String> x : attrSubsets) {
            Set<String> plus = attributeClosure(x, fds);
            for (Set<String> y : attrSubsets) {
                if (plus.containsAll(y)) {
                    fp.add(new FunctionalDependency(x, y));
                }
            }
        }
        return fp;
    }

    // =====================================================================
    // 4. 结果载体
    // =====================================================================

    /** 图 8-7 算法的运行结果：F⁺ 集合 + 每轮统计。 */
    public static final class Result {
        private final Set<FunctionalDependency> fplus;
        private final List<int[]> rounds;
        private final int totalRounds;

        Result(Set<FunctionalDependency> fplus, List<int[]> rounds, int totalRounds) {
            this.fplus = fplus;
            this.rounds = rounds;
            this.totalRounds = totalRounds;
        }

        public Set<FunctionalDependency> fplus() {
            return fplus;
        }

        public List<int[]> rounds() {
            return rounds;
        }

        public int totalRounds() {
            return totalRounds;
        }
    }
}
