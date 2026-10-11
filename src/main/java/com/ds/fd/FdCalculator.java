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
 *   <li>{@link #attributeClosure} —— 属性闭包 X⁺（实践中真正要用的那一个）；</li>
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
    // 2. 属性闭包 X⁺ —— 教科书标准算法，O(|F|) 轮扫描
    // =====================================================================

    /**
     * 求属性集 {@code x} 在 FD 集合 {@code fds} 下的闭包 X⁺。
     *
     * <pre>
     * result = X
     * repeat
     *     对每条 FD (lhs -> rhs): 若 lhs ⊆ result, 则 result |= rhs
     * until result 不再变化
     * </pre>
     */
    public static Set<String> attributeClosure(Set<String> x, List<FunctionalDependency> fds) {
        Set<String> result = new TreeSet<String>(x);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (FunctionalDependency fd : fds) {
                if (result.containsAll(fd.determinant()) && !result.containsAll(fd.dependent())) {
                    result.addAll(fd.dependent());
                    changed = true;
                }
            }
        }
        return result;
    }

    /** X 是不是超键：X⁺ 是否包含全部属性。 */
    public static boolean isSuperkey(Set<String> x, Set<String> allAttrs, List<FunctionalDependency> fds) {
        return attributeClosure(x, fds).containsAll(allAttrs);
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
