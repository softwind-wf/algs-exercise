package com.ds.fd;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 演示工具：用教材《数据库系统概念》8.4 节的例子跑通"计算 F⁺"的两条路线。
 *
 * <pre>
 * R = (A, B, C, G, H, I)
 * F = { A→B, A→C, CG→H, CG→I, B→H }
 * </pre>
 *
 * <p>路线一：教材图 8-7 的算法（逐条应用三条公理，迭代到不动点）—— 教学用，暴力。
 * <p>路线二：属性闭包 X⁺ 直接枚举（{@code F⁺ = { X→Y : Y ⊆ X⁺ }}）—— 实践中真正的做法。
 *
 * <p>运行（无需构建整个工程）：
 * <pre>
 * javac -encoding UTF-8 -d target/fd-demo src/main/java/com/ds/fd/*.java
 * java -Dfile.encoding=UTF-8 -cp target/fd-demo com.ds.fd.FdClosureDemo
 * </pre>
 */
public final class FdClosureDemo {

    public static void main(String[] args) {
        final Set<String> attrs = FunctionalDependency.chars("ABCGHI");
        final List<FunctionalDependency> f = new java.util.ArrayList<FunctionalDependency>();
        f.add(FunctionalDependency.of("A", "B"));
        f.add(FunctionalDependency.of("A", "C"));
        f.add(FunctionalDependency.of("CG", "H"));
        f.add(FunctionalDependency.of("CG", "I"));
        f.add(FunctionalDependency.of("B", "H"));

        System.out.println("========================================================================");
        System.out.println("教材例子: R = (A,B,C,G,H,I)");
        System.out.print("F = { ");
        for (int i = 0; i < f.size(); i++) {
            System.out.print(f.get(i) + (i < f.size() - 1 ? ", " : ""));
        }
        System.out.println(" }");
        int subsetCount = FdCalculator.subsets(attrs).size();
        System.out.println("R 的非空子集共 " + subsetCount + " 个 => F+ 理论条数上限 "
                + (subsetCount * subsetCount) + " 条(含平凡依赖)");
        System.out.println("========================================================================");

        // ---------- 零、图 8-8 的算法：逐步计算 (AG)+ ----------
        System.out.println();
        System.out.println("【零】按图 8-8 的算法逐步计算 (AG)+  (教材第 193 页)");
        FdCalculator.ClosureTrace agPlus = FdCalculator.attributeClosureTrace(
                FunctionalDependency.chars("AG"), f);
        System.out.println("  result := " + FdCalculator.join(agPlus.initial()));
        int printedRound = 0;
        for (FdCalculator.ClosureStep step : agPlus.steps()) {
            if (step.round() != printedRound) {
                printedRound = step.round();
                System.out.println("  第 " + printedRound + " 轮 repeat:");
            }
            System.out.printf("    %-8s (左边 ⊆ %s) => result := %s%n",
                    step.applied(),
                    FdCalculator.join(step.before()),
                    FdCalculator.join(step.after()));
        }
        System.out.println("  第 " + agPlus.totalRounds() + " 轮 repeat:");
        System.out.println("    没有 FD 能让 result 再变大 => result 不变, 算法终止");
        System.out.println("  => (AG)+ = " + FdCalculator.join(agPlus.closure())
                + "  (共 " + agPlus.steps().size() + " 条 FD 真正生效, "
                + agPlus.totalRounds() + " 轮)");

        // ---------- 一、图 8-7 的暴力算法 ----------
        System.out.println();
        System.out.println("【一】按图 8-7 的算法暴力迭代");
        FdCalculator.Result naive = FdCalculator.closureNaive(attrs, f);
        System.out.printf("  %-8s %12s %10s %12s%n", "轮次", "本轮生成候选", "本轮新增", "累计条数");
        for (int[] r : naive.rounds()) {
            System.out.printf("  第 %2d 轮 %12d %10d %12d%n", r[0], r[1], r[2], r[3]);
        }
        System.out.println("  => 收敛, 共 " + naive.fplus().size() + " 条 (含平凡依赖), 迭代 "
                + naive.totalRounds() + " 轮");

        // ---------- 二、属性闭包法 ----------
        System.out.println();
        System.out.println("【二】按属性闭包 X+ 直接枚举(集合论定义)");
        Set<FunctionalDependency> byAttr = FdCalculator.closureByAttributeClosures(attrs, f);
        System.out.println("  => 共 " + byAttr.size() + " 条");

        // ---------- 三、两法对比 ----------
        System.out.println();
        System.out.println("【三】两种方法对比");
        Set<FunctionalDependency> missing = new LinkedHashSet<FunctionalDependency>(byAttr);
        missing.removeAll(naive.fplus());
        Set<FunctionalDependency> extra = new LinkedHashSet<FunctionalDependency>(naive.fplus());
        extra.removeAll(byAttr);

        boolean allMissingTrivial = true;
        for (FunctionalDependency fd : missing) {
            if (!fd.isTrivial()) {
                allMissingTrivial = false;
            }
        }
        System.out.println("  图 8-7 算法 : " + naive.fplus().size() + " 条");
        System.out.println("  X+ 法       : " + byAttr.size() + " 条");
        System.out.println("  图 8-7 缺少 : " + missing.size() + " 条, 全部是平凡依赖(Y ⊆ X)? "
                + (allMissingTrivial ? "是" : "否"));
        System.out.println("  图 8-7 多出 : " + extra.size() + " 条");

        int ntAttr = 0;
        for (FunctionalDependency fd : byAttr) {
            if (!fd.isTrivial()) {
                ntAttr++;
            }
        }
        int ntNaive = 0;
        for (FunctionalDependency fd : naive.fplus()) {
            if (!fd.isTrivial()) {
                ntNaive++;
            }
        }
        Set<FunctionalDependency> ntAttrSet = new LinkedHashSet<FunctionalDependency>();
        for (FunctionalDependency fd : byAttr) {
            if (!fd.isTrivial()) {
                ntAttrSet.add(fd);
            }
        }
        Set<FunctionalDependency> ntNaiveSet = new LinkedHashSet<FunctionalDependency>();
        for (FunctionalDependency fd : naive.fplus()) {
            if (!fd.isTrivial()) {
                ntNaiveSet.add(fd);
            }
        }
        System.out.println("  只看非平凡依赖: X+ 法 " + ntAttr + " 条, 图 8-7 " + ntNaive
                + " 条, 完全一致? " + (ntAttrSet.equals(ntNaiveSet) ? "是" : "否"));

        Set<FunctionalDependency> seeded = new LinkedHashSet<FunctionalDependency>(naive.fplus());
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            for (Set<String> sub : FdCalculator.subsets(x)) {
                seeded.add(new FunctionalDependency(x, sub));
            }
        }
        System.out.println();
        System.out.println("  ★ 把自反律一次性加满(对每个子集 X 补上 X->X)后:");
        System.out.println("      " + seeded.size() + " 条 == X+ 法的 " + byAttr.size() + " 条 ? "
                + (seeded.equals(byAttr) ? "是, 两法完全一致" : "否"));
        System.out.println("  ★ 原因: 图 8-7 的自反律只对'已在 F+ 里的 FD 的左边'生成子集;");
        System.out.println("     而 C / G / H / I 从未单独作为某个 FD 的左边出现过(只出现在 CG 里),");
        System.out.println("     所以 C->C、H->H 这类平凡依赖永远生不出来 —— 不是算法错, 是它没定义初值。");

        // ---------- 四、X+ 一览 ----------
        System.out.println();
        System.out.println("【四】X+ 一览(实践中真正要用的东西)");
        System.out.printf("   %-8s %-16s %s%n", "X", "X+", "说明");
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            Set<String> plus = FdCalculator.attributeClosure(x, f);
            String note = plus.containsAll(attrs) ? "<= 超键" : "";
            System.out.printf("   %-8s %-16s %s%n", FdCalculator.join(x), FdCalculator.join(plus), note);
        }

        // ---------- 五、用 X+ 判定一条 FD 是否成立 ----------
        System.out.println();
        System.out.println("【五】用 X+ 判定一条 FD 是否成立: X -> Y 成立 <=> Y ⊆ X+");
        String[][] checks = {
                {"A", "H"}, {"CG", "H"}, {"G", "H"}, {"AG", "HI"},
                {"BG", "C"}, {"C", "I"}, {"A", "HI"}, {"AB", "A"},
        };
        System.out.printf("   %-10s %-9s %-10s %s%n", "待判 FD", "X+", "Y 里缺什么", "结论");
        for (String[] c : checks) {
            FdCalculator.FdCheck check = FdCalculator.checkFd(
                    FunctionalDependency.chars(c[0]), FunctionalDependency.chars(c[1]), f);
            String verdict = check.holds()
                    ? (check.trivial() ? "成立(平凡, 与 F 无关)" : "成立")
                    : "不成立";
            System.out.printf("   %-10s %-9s %-10s %s%n",
                    c[0] + " -> " + c[1],
                    FdCalculator.join(check.closure()),
                    check.missing().isEmpty() ? "(无)" : FdCalculator.join(check.missing()),
                    verdict);
        }
        System.out.println("   (判据只看左边闭包; X -> Y 成立 <=> Y ⊆ X+, 不必枚举逐条 F+)");

        // ---------- 六、用 X+ 判断超键，再由极小性得到候选键 ----------
        System.out.println();
        System.out.println("【六】用 X+ 判断超键: α 是超键 <=> α+ ⊇ R = " + FdCalculator.join(attrs));
        String[] candidates = {"A", "G", "CG", "AG", "ABC", "ABG", "ABCGHI"};
        System.out.printf("   %-8s %-14s %-12s %s%n", "α", "α+", "缺哪些属性", "结论");
        for (String c : candidates) {
            FdCalculator.SuperkeyCheck check = FdCalculator.checkSuperkey(
                    FunctionalDependency.chars(c), attrs, f);
            System.out.printf("   %-8s %-14s %-12s %s%n",
                    c,
                    FdCalculator.join(check.closure()),
                    check.missing().isEmpty() ? "(无)" : FdCalculator.join(check.missing()),
                    check.isSuperkey() ? "超键" : "不是超键");
        }
        System.out.println("   (超键不要求极小: AG / ABG / ABCGHI 都算; 极小性由下一步筛)");

        List<Set<String>> keys = FdCalculator.candidateKeys(attrs, f);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            sb.append(FdCalculator.joinWithComma(keys.get(i)));   // joinWithComma 自带花括号
            if (i < keys.size() - 1) {
                sb.append("  |  ");
            }
        }
        System.out.println("   候选键(极小超键): " + sb);
        System.out.println("   (AG 能推出全部属性; 单独 A 或 G 都不能 => 唯一极小超键)");
    }
}
