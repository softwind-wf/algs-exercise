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

        // ---------- 七、无关属性与正则覆盖（图 8-9） ----------
        System.out.println();
        System.out.println("【七】无关属性与正则覆盖 (教材 8.4.3 / 图 8-9)");

        // 例 1：左部无关 —— B 在 AB->C 中多余
        List<FunctionalDependency> ex1 = fds("AB->C", "A->C");
        System.out.println("  例 1: F = " + ex1);
        CanonicalCover.ExtraneousCheck check1 = CanonicalCover.check(ex1.get(0), ex1);
        System.out.println("    AB->C: " + check1);
        System.out.println("    (A+ = " + FdCalculator.join(
                FdCalculator.attributeClosure(FunctionalDependency.chars("A"), ex1))
                + " 已含 C => B 在左部无关; 两边都无关时先删哪边都合法)");
        System.out.println("    正则覆盖: " + CanonicalCover.cover(ex1));

        // 例 2：右部无关 —— C 在 AB->CD 中多余（教材原例）
        List<FunctionalDependency> ex2 = fds("AB->CD", "A->E", "E->C");
        System.out.println("  例 2: F = " + ex2);
        List<FunctionalDependency> f2prime = new java.util.ArrayList<FunctionalDependency>();
        f2prime.add(FunctionalDependency.of("AB", "D"));
        f2prime.add(FunctionalDependency.of("A", "E"));
        f2prime.add(FunctionalDependency.of("E", "C"));
        System.out.println("    F' = " + f2prime + ", AB+ = "
                + FdCalculator.join(FdCalculator.attributeClosure(
                        FunctionalDependency.chars("AB"), f2prime))
                + " 已含 C => C 在右部无关");
        CanonicalCover.Result cover2 = CanonicalCover.compute(ex2);
        System.out.println("    正则覆盖: " + cover2.fds());

        // 例 3：合并律 —— 相同左部合并
        List<FunctionalDependency> ex3 = fds("A->B", "A->C");
        System.out.println("  例 3: F = " + ex3 + " => 正则覆盖: " + CanonicalCover.cover(ex3));

        // 例 4：把教材 8.4 的 F 整体化简
        CanonicalCover.Result cover4 = CanonicalCover.compute(f);
        System.out.println("  例 4: 教材 8.4 的 F (" + f.size() + " 条)");
        for (String step : cover4.steps()) {
            System.out.println("    " + step);
        }
        System.out.println("    正则覆盖: " + cover4.fds()
                + "  (" + f.size() + " 条 -> " + cover4.size() + " 条)");

        // 例 5：多字符属性名 —— 左部 {A,BC} 与 {AB,C} 拼接后都是 "ABC", 不能被误合并
        List<FunctionalDependency> ex5 = new java.util.ArrayList<FunctionalDependency>();
        ex5.add(new FunctionalDependency(
                new java.util.TreeSet<String>(java.util.Arrays.asList("A", "BC")),
                new java.util.TreeSet<String>(java.util.Arrays.asList("X"))));
        ex5.add(new FunctionalDependency(
                new java.util.TreeSet<String>(java.util.Arrays.asList("AB", "C")),
                new java.util.TreeSet<String>(java.util.Arrays.asList("Y"))));
        CanonicalCover.Result cover5 = CanonicalCover.compute(ex5);
        System.out.println("  例 5: 多字符属性名, 左部 {A,BC} 与 {AB,C} (拼成字符串都是 ABC)");
        for (FunctionalDependency fd : cover5.fds()) {
            System.out.println("    " + FdCalculator.joinWithComma(fd.determinant())
                    + " -> " + FdCalculator.joinWithComma(fd.dependent()));
        }
        System.out.println("    正则覆盖条数: " + cover5.size() + "  (应为 2, 不能被误合并)");

        // ---------- 八、无损分解判定（8.5 节） ----------
        System.out.println();
        System.out.println("【八】无损分解判定");
        Set<String> rel = FunctionalDependency.chars("ABC");
        List<FunctionalDependency> fA = new java.util.ArrayList<FunctionalDependency>();
        fA.add(FunctionalDependency.of("A", "B"));

        System.out.println("  例 1: R=(A,B,C), F={A->B}, 拆成 (A,B)+(A,C)");
        LosslessJoin.BinaryResult b1 = LosslessJoin.checkBinary(
                rel, FunctionalDependency.chars("AB"), FunctionalDependency.chars("AC"), fA);
        System.out.println("    " + b1);

        System.out.println("  例 2: 同样的 F, 拆成 (A,B)+(B,C)");
        LosslessJoin.BinaryResult b2 = LosslessJoin.checkBinary(
                rel, FunctionalDependency.chars("AB"), FunctionalDependency.chars("BC"), fA);
        System.out.println("    " + b2);

        Set<String> rel4 = FunctionalDependency.chars("ABCD");
        List<FunctionalDependency> f4 = new java.util.ArrayList<FunctionalDependency>();
        f4.add(FunctionalDependency.of("A", "B"));
        f4.add(FunctionalDependency.of("C", "D"));

        List<Set<String>> parts3 = new java.util.ArrayList<Set<String>>();
        parts3.add(FunctionalDependency.chars("AB"));
        parts3.add(FunctionalDependency.chars("CD"));
        parts3.add(FunctionalDependency.chars("AC"));
        System.out.println("  例 3: R=(A,B,C,D), F={A->B, C->D}, 拆成 (A,B)+(C,D)+(A,C)");
        LosslessJoin.ChaseResult c3 = LosslessJoin.checkChase(rel4, parts3, f4);
        for (String step : c3.steps()) {
            System.out.println("    " + step);
        }
        for (int i = 0; i < c3.table().size(); i++) {
            System.out.println("    第 " + (i + 1) + " 行: " + c3.table().get(i));
        }
        System.out.println("    => " + (c3.lossless() ? "无损" : "有损"));

        List<Set<String>> parts2 = new java.util.ArrayList<Set<String>>();
        parts2.add(FunctionalDependency.chars("AB"));
        parts2.add(FunctionalDependency.chars("CD"));
        System.out.println("  例 4: 拆成 (A,B)+(C,D) (没有公共键, 连接成笛卡尔积)");
        LosslessJoin.ChaseResult c4 = LosslessJoin.checkChase(rel4, parts2, f4);
        System.out.println("    => " + (c4.lossless() ? "无损" : "有损"));

        // ---------- 九、保持依赖判定（8.4.5 节） ----------
        System.out.println();
        System.out.println("【九】保持依赖判定 (教材 8.4.5 / 图 8-10 与修改闭包法)");

        // 例 1：inst_dept 的同构单字母版（I,N,S,D,B -> A,B,C,D,E）
        Set<String> r9 = FunctionalDependency.chars("ABCDE");
        List<FunctionalDependency> f9 = new java.util.ArrayList<FunctionalDependency>();
        f9.add(FunctionalDependency.of("A", "BC"));
        f9.add(FunctionalDependency.of("C", "DE"));
        List<Set<String>> d9 = new java.util.ArrayList<Set<String>>();
        d9.add(FunctionalDependency.chars("ABC"));
        d9.add(FunctionalDependency.chars("CDE"));
        System.out.println("  例 1: R=(A,B,C,D,E), F={A->BC, C->DE}, 拆成 (A,B,C)+(C,D,E)");
        DependencyPreservation.FPlusCheck m1a = DependencyPreservation.checkByFPlus(r9, d9, f9);
        System.out.println("    方法一(图 8-10): |F+| = " + m1a.fplus().size()
                + ", |F'| = " + m1a.fprime().size()
                + ", F'+ = F+ ? " + (m1a.preserves() ? "是 => 保持依赖" : "否"));
        DependencyPreservation.ClosureCheck m2a = DependencyPreservation.checkByModifiedClosure(r9, d9, f9);
        for (DependencyPreservation.Verdict verdict : m2a.verdicts()) {
            System.out.println("    方法二: " + verdict);
        }

        // 例 2：无损但不保持依赖（教材例 2 的形状：ENO->PNO 跨两张表）
        Set<String> r9b = FunctionalDependency.chars("ABC");
        List<FunctionalDependency> f9b = new java.util.ArrayList<FunctionalDependency>();
        f9b.add(FunctionalDependency.of("AB", "C"));
        f9b.add(FunctionalDependency.of("C", "A"));
        List<Set<String>> d9b = new java.util.ArrayList<Set<String>>();
        d9b.add(FunctionalDependency.chars("AC"));
        d9b.add(FunctionalDependency.chars("BC"));
        System.out.println("  例 2: R=(A,B,C), F={AB->C, C->A}, 拆成 (A,C)+(B,C)");
        DependencyPreservation.FPlusCheck m1b = DependencyPreservation.checkByFPlus(r9b, d9b, f9b);
        DependencyPreservation.ClosureCheck m2b = DependencyPreservation.checkByModifiedClosure(r9b, d9b, f9b);
        System.out.println("    方法一: F'+ = F+ ? " + (m1b.preserves() ? "是" : "否")
                + ", 丢失: " + m1b.lost());
        for (DependencyPreservation.Verdict verdict : m2b.verdicts()) {
            System.out.println("    方法二: " + verdict);
        }
        LosslessJoin.BinaryResult lossless9 = LosslessJoin.checkBinary(
                r9b, FunctionalDependency.chars("AC"), FunctionalDependency.chars("BC"), f9b);
        System.out.println("    注: 该分解是否无损? " + (lossless9.lossless() ? "无损" : "有损")
                + " —— 无损与保持依赖是两个独立要求");

        // ---------- 十、BCNF 判定（教材 8.5 节 / 第 198 页） ----------
        System.out.println();
        System.out.println("【十】BCNF 判定 (教材 198 页: 未分解只看 F; 分解后必须看 F+ 投影)");

        Set<String> r10 = FunctionalDependency.chars("ABCDE");
        List<FunctionalDependency> f10 = fds("A->B", "BC->D");
        System.out.println("  例 1: R=(A,B,C,D,E), F={A->B, BC->D}");
        BcnfCheck.CheckResult whole10 = BcnfCheck.checkRelation(r10, f10);
        System.out.println("    未分解, 只看 F: " + (whole10.bcnf() ? "属于 BCNF" : "不属于 BCNF"));
        for (BcnfCheck.Violation violation : whole10.violations()) {
            System.out.println("      " + violation);
        }
        BcnfCheck.CheckResult whole10b = BcnfCheck.checkRelationByFPlus(r10, f10);
        System.out.println("    对照(先物化 F+): " + (whole10b.bcnf() ? "属于 BCNF" : "不属于 BCNF")
                + ", 违反项 " + whole10b.violations().size() + " 条 > " + whole10.violations().size()
                + " 条 —— 结论一致, F+ 里更多");

        List<Set<String>> d10 = new java.util.ArrayList<Set<String>>();
        d10.add(FunctionalDependency.chars("AB"));
        d10.add(FunctionalDependency.chars("ACDE"));
        System.out.println("    拆成 R1=(A,B) + R2=(A,C,D,E)");
        System.out.println("      R1: " + BcnfCheck.checkProjected(FunctionalDependency.chars("AB"), r10, f10));
        System.out.println("      R2: " + BcnfCheck.checkProjected(FunctionalDependency.chars("ACDE"), r10, f10));
        System.out.println("      只看 F 会误判: F 里没有一条依赖的左右部都落在 R2 内,"
                + " 但 F+ 里的 AC->D 违反 BCNF (AC+ = ABCD, 缺 E)");
        BcnfCheck.CheckResult r2b = BcnfCheck.checkProjectedByFPlus(
                FunctionalDependency.chars("ACDE"), r10, f10);
        StringBuilder badLeft = new StringBuilder();
        for (Set<String> det : r2b.violatingDeterminants()) {
            badLeft.append(badLeft.length() == 0 ? "" : ", ").append(FdCalculator.join(det));
        }
        System.out.println("      Π_R2(F+) 里左部不是 R2 超键的: " + badLeft
                + "（其中含 AC->D? "
                + r2b.projected().contains(FunctionalDependency.of("AC", "D")) + "）");
        BcnfCheck.DecompositionResult dec10 = BcnfCheck.checkDecomposition(r10, d10, f10);
        System.out.println("      整个分解 => " + (dec10.bcnf() ? "BCNF 分解" : "不是 BCNF 分解")
                + ", 要再拆的子模式 " + dec10.violatingParts().size() + " 个");

        Set<String> r10c = FunctionalDependency.chars("ABC");
        List<FunctionalDependency> f10c = fds("AB->C", "C->B");
        System.out.println("  例 2: R=(A,B,C), F={AB->C, C->B} (3NF 但非 BCNF 的经典形状)");
        for (BcnfCheck.Violation violation : BcnfCheck.checkRelation(r10c, f10c).violations()) {
            System.out.println("      " + violation + " —— C 是主属性, 3NF 放行, BCNF 不放行");
        }
        List<Set<String>> d10c = new java.util.ArrayList<Set<String>>();
        d10c.add(FunctionalDependency.chars("BC"));
        d10c.add(FunctionalDependency.chars("AC"));
        BcnfCheck.DecompositionResult dec10c = BcnfCheck.checkDecomposition(r10c, d10c, f10c);
        for (BcnfCheck.CheckResult part : dec10c.parts()) {
            System.out.println("      " + part);
        }
        System.out.println("      => (B,C)+(A,C) 是 BCNF 分解; 但它丢 AB->C, 见【九】的结论");

        // ---------- 十一、BCNF 分解算法（8.5.1.2 节 / 图 8-11） ----------
        System.out.println();
        System.out.println("【十一】BCNF 分解算法 (教材 8.5.1.2 / 图 8-11)");

        Set<String> r11 = attrs("course_id,title,dept_name,credits,sec_id,semester,year,"
                + "building,room_number,capacity,time_slot_id");
        List<FunctionalDependency> f11 = new java.util.ArrayList<FunctionalDependency>();
        f11.add(new FunctionalDependency(attrs("course_id"), attrs("title,dept_name,credits")));
        f11.add(new FunctionalDependency(attrs("building,room_number"), attrs("capacity")));
        f11.add(new FunctionalDependency(attrs("course_id,sec_id,semester,year"),
                attrs("building,room_number,time_slot_id")));
        System.out.println("  例 1: class(course_id, title, dept_name, credits, sec_id, semester, year,");
        System.out.println("                building, room_number, capacity, time_slot_id)");
        System.out.println("        F = { course_id -> {title,dept_name,credits},");
        System.out.println("              {building,room_number} -> {capacity},");
        System.out.println("              {course_id,sec_id,semester,year} -> {building,room_number,time_slot_id} }");
        BcnfCheck.CheckResult table11 = BcnfCheck.checkRelation(r11, f11);
        System.out.println("    先判 BCNF: " + (table11.bcnf() ? "属于 BCNF" : "不属于 BCNF"));
        for (BcnfCheck.Violation violation : table11.violations()) {
            System.out.println("      " + violation);
        }
        System.out.print("    候选键: ");
        List<Set<String>> keys11 = FdCalculator.candidateKeys(r11, f11);
        for (int i = 0; i < keys11.size(); i++) {
            System.out.print((i == 0 ? "" : ", ") + FdCalculator.joinWithComma(keys11.get(i)));
        }
        System.out.println();
        BcnfDecomposition.Result dec11 = BcnfDecomposition.decompose(r11, f11);
        System.out.println("    拆成 " + dec11.decomposition().size() + " 张表（" + dec11.stepCount() + " 步）:");
        for (BcnfDecomposition.Step step : dec11.steps()) {
            System.out.println("      " + step);
        }
        System.out.println("    最终: " + dec11);
        System.out.println("    复核: BCNF? " + dec11.bcnf() + ", 无损? " + dec11.lossless()
                + ", 保持依赖? " + dec11.preservesDependencies(f11));

        Set<String> r11b = FunctionalDependency.chars("ABC");
        List<FunctionalDependency> f11b = fds("AB->C", "C->A");
        System.out.println("  例 2: R=(A,B,C), F={AB->C, C->A}");
        BcnfDecomposition.Result dec11b = BcnfDecomposition.decompose(r11b, f11b);
        for (BcnfDecomposition.Step step : dec11b.steps()) {
            System.out.println("      " + step);
        }
        DependencyPreservation.ClosureCheck lost11b =
                DependencyPreservation.checkByModifiedClosure(r11b, dec11b.decomposition(), f11b);
        System.out.println("    最终: " + dec11b + " —— 无损, 但丢依赖 " + lost11b.lost()
                + " (BCNF 分解不保证保持依赖)");

        // ---------- 十二、3NF 合成（8.5.2 节 / 图 8-12） ----------
        System.out.println();
        System.out.println("【十二】3NF 合成算法 (教材 8.5.2 / 图 8-12: 正则覆盖 + 补候选键 + 删冗余)");

        Set<String> r12 = attrs("s_ID,i_ID,dept_name");
        List<FunctionalDependency> f12 = new java.util.ArrayList<FunctionalDependency>();
        f12.add(new FunctionalDependency(attrs("i_ID"), attrs("dept_name")));
        f12.add(new FunctionalDependency(attrs("s_ID,dept_name"), attrs("i_ID")));
        System.out.println("  例 1: dept_advisor(s_ID, i_ID, dept_name),");
        System.out.println("        F={ i_ID->dept_name, {s_ID,dept_name}->i_ID }");
        System.out.println("    候选键: " + joinSets(FdCalculator.candidateKeys(r12, f12)));
        BcnfCheck.CheckResult bcnf12 = BcnfCheck.checkRelation(r12, f12);
        System.out.println("    BCNF 判定: " + (bcnf12.bcnf() ? "属于 BCNF" : "不属于 BCNF")
                + (bcnf12.violations().isEmpty() ? "" : " —— " + bcnf12.violations()));
        ThreeNfDecomposition.ThirdNfCheck nf12 = ThreeNfDecomposition.checkThirdNormalForm(r12, r12, f12);
        System.out.println("    3NF 判定: " + (nf12.thirdNormalForm() ? "属于 3NF" : "不属于 3NF")
                + " (主属性 " + FdCalculator.joinWithComma(nf12.primeAttributes()) + ")");
        ThreeNfDecomposition.Result dec12 = ThreeNfDecomposition.decompose(r12, f12);
        System.out.println("    正则覆盖: " + fdsToString(dec12.canonicalCover()));
        for (ThreeNfDecomposition.Step step : dec12.steps()) {
            System.out.println("      " + step);
        }
        System.out.println("    最终: " + dec12);
        System.out.println("    复核: 3NF? " + dec12.thirdNormalForm() + ", BCNF? " + dec12.bcnf()
                + ", 无损? " + dec12.lossless() + ", 保持依赖? " + dec12.preservesDependencies(f12));

        System.out.println("  例 2: class 模式 (8.5.1.2 的 F 恰好是正则覆盖)");
        ThreeNfDecomposition.Result dec12b = ThreeNfDecomposition.decompose(r11, f11);
        System.out.println("    合成 " + dec12b.synthesized().size() + " 张表, 最终 "
                + dec12b.decomposition().size() + " 张 (section 含候选键, 无需补键; 无冗余可删)");
        System.out.println("    最终: " + dec12b);
        boolean sameAsBcnf = new java.util.LinkedHashSet<Set<String>>(dec12b.decomposition())
                .equals(new java.util.LinkedHashSet<Set<String>>(
                        BcnfDecomposition.decompositionOf(r11, f11)));
        System.out.println("    与 BCNF 分解结果相同? " + sameAsBcnf
                + " —— 3NF 合成有时也恰好得到 BCNF");

        Set<String> r12c = FunctionalDependency.chars("ABCD");
        List<FunctionalDependency> f12c = fds("A->B", "B->C");
        System.out.println("  例 3: R=(A,B,C,D), F={A->B, B->C} (合成的 AB、BC 都不含候选键 AD)");
        ThreeNfDecomposition.Result dec12c = ThreeNfDecomposition.decompose(r12c, f12c);
        for (ThreeNfDecomposition.Step step : dec12c.steps()) {
            System.out.println("      " + step);
        }
        System.out.println("    最终: " + dec12c);
        System.out.println("    复核: 3NF? " + dec12c.thirdNormalForm() + ", 无损? " + dec12c.lossless()
                + ", 保持依赖? " + dec12c.preservesDependencies(f12c));

        Set<String> r12d = FunctionalDependency.chars("ABC");
        List<FunctionalDependency> f12d = fds("AB->C", "C->A");
        System.out.println("  例 4: R=(A,B,C), F={AB->C, C->A} (3NF 但非 BCNF) —— 两种分解的取舍");
        BcnfDecomposition.Result bcnf12d = BcnfDecomposition.decompose(r12d, f12d);
        ThreeNfDecomposition.Result three12d = ThreeNfDecomposition.decompose(r12d, f12d);
        System.out.println("    BCNF 分解: " + bcnf12d + ", 保持依赖? "
                + bcnf12d.preservesDependencies(f12d) + " —— 丢了 AB->C");
        System.out.println("    3NF 合成:  " + three12d + ", 保持依赖? "
                + three12d.preservesDependencies(f12d) + " —— 保住依赖, 但仍是 3NF 非 BCNF");
    }

    /** 把 "AB->C" 这样的简写解析成依赖列表，方便演示。 */
    private static List<FunctionalDependency> fds(String... specs) {
        List<FunctionalDependency> out = new java.util.ArrayList<FunctionalDependency>();
        for (String spec : specs) {
            int arrow = spec.indexOf("->");
            out.add(FunctionalDependency.of(
                    spec.substring(0, arrow).trim(),
                    spec.substring(arrow + 2).trim()));
        }
        return out;
    }

    /** 逗号分隔的属性名，用于多字符属性（如 "course_id,title"）的演示。 */
    private static Set<String> attrs(String commaSeparated) {
        Set<String> out = new java.util.LinkedHashSet<String>();
        for (String token : commaSeparated.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    /** 把一组属性集合打印成 { { … }, { … } } 形式。 */
    private static String joinSets(List<Set<String>> sets) {
        StringBuilder sb = new StringBuilder("{ ");
        for (int i = 0; i < sets.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(FdCalculator.joinWithComma(sets.get(i)));
        }
        return sb.append(" }").toString();
    }

    /** 打印依赖列表（带逗号，避免多字符属性名粘连）。 */
    private static String fdsToString(List<FunctionalDependency> fds) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < fds.size(); i++) {
            FunctionalDependency fd = fds.get(i);
            sb.append(i == 0 ? "" : ", ")
                    .append(FdCalculator.joinWithComma(fd.determinant()))
                    .append(" -> ").append(FdCalculator.joinWithComma(fd.dependent()));
        }
        return sb.append("]").toString();
    }
}
