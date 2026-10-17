package com.ds.fd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 函数依赖闭包计算的回归测试。
 *
 * <p>用例 = 教材 8.4 节原题：R = (A,B,C,G,H,I)，F = { A→B, A→C, CG→H, CG→I, B→H }。
 * 期望值来自教材给出的 A⁺ = ABCGHI、AG 是候选键，以及本项目对 F⁺ 条数的实测
 * （图 8-7 暴力法 1668 条、X⁺ 法 1701 条、非平凡依赖 1036 条）。
 */
class FdCalculatorTest {

    private static final String ALL = "ABCGHI";

    private Set<String> attrs;
    private List<FunctionalDependency> f;

    @BeforeEach
    void setUp() {
        attrs = FunctionalDependency.chars(ALL);
        f = new ArrayList<FunctionalDependency>();
        f.add(FunctionalDependency.of("A", "B"));
        f.add(FunctionalDependency.of("A", "C"));
        f.add(FunctionalDependency.of("CG", "H"));
        f.add(FunctionalDependency.of("CG", "I"));
        f.add(FunctionalDependency.of("B", "H"));
    }

    /**
     * A⁺ 的推导：A → {A,B,C}；B → H 得 {A,B,C,H}；此时 CG → H / CG → I 都**用不上**，
     * 因为 G 不在闭包里 —— 闭包计算不允许"借用"尚未被推出的属性。
     * 所以 A⁺ = ABCH（我第一版把这条期望值写成了 ABCGHI，是被本测试当场纠正的）。
     */
    @Test
    @DisplayName("属性闭包 A+ = ABCH（不是 ABCGHI），G+ = G，CG+ = CGHI")
    void attributeClosure() {
        assertEquals("ABCH", FdCalculator.join(FdCalculator.attributeClosure(FunctionalDependency.chars("A"), f)));
        assertEquals("G", FdCalculator.join(FdCalculator.attributeClosure(FunctionalDependency.chars("G"), f)));
        assertEquals("CGHI", FdCalculator.join(FdCalculator.attributeClosure(FunctionalDependency.chars("CG"), f)));
    }

    /**
     * 图 8-8 的逐步过程：初始 AG；第 1 轮 A→B 加 B、A→C 加 C、CG→H 加 H、CG→I 加 I；
     * 第 2 轮没有任何 FD 能再增大 result ⇒ 终止。教材 193 页下方手算的四条就是这四步。
     */
    @Test
    @DisplayName("图 8-8 追踪：(AG)+ = ABCGHI，第 1 轮 4 步、第 2 轮收敛")
    void attributeClosureTrace() {
        FdCalculator.ClosureTrace trace =
                FdCalculator.attributeClosureTrace(FunctionalDependency.chars("AG"), f);

        assertEquals("AG", FdCalculator.join(trace.initial()));
        assertEquals("ABCGHI", FdCalculator.join(trace.closure()));
        assertEquals(2, trace.totalRounds());
        assertEquals(2, trace.roundResults().size());
        assertEquals("ABCGHI", FdCalculator.join(trace.roundResults().get(0)));
        assertEquals("ABCGHI", FdCalculator.join(trace.roundResults().get(1)));

        assertEquals(4, trace.steps().size());
        String[] applied = {"A -> B", "A -> C", "CG -> H", "CG -> I"};
        String[] after = {"ABG", "ABCG", "ABCGH", "ABCGHI"};
        for (int i = 0; i < applied.length; i++) {
            FdCalculator.ClosureStep step = trace.steps().get(i);
            assertEquals(1, step.round());
            assertEquals(applied[i], step.applied().toString());
            assertEquals(after[i], FdCalculator.join(step.after()));
            assertTrue(step.before().containsAll(step.applied().determinant()),
                    "第 " + i + " 步生效时左边必须已包含于 result");
        }
        assertEquals("B", FdCalculator.join(trace.steps().get(0).added()));
        assertEquals("I", FdCalculator.join(trace.steps().get(3).added()));
    }

    /** 过程视图不能改变结果：每个子集的追踪闭包必须与原 attributeClosure 完全一致。 */
    @Test
    @DisplayName("追踪版与结果版闭包逐个子集一致")
    void traceAgreesWithClosure() {
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            Set<String> plain = FdCalculator.attributeClosure(x, f);
            Set<String> traced = FdCalculator.attributeClosureTrace(x, f).closure();
            assertEquals(plain, traced, "X = " + FdCalculator.join(x));
        }
    }


    @Test
    @DisplayName("超键判定：AG 是超键，A 与 G 都不是")
    void superkey() {
        assertTrue(FdCalculator.isSuperkey(FunctionalDependency.chars("AG"), attrs, f));
        assertFalse(FdCalculator.isSuperkey(FunctionalDependency.chars("A"), attrs, f));
        assertFalse(FdCalculator.isSuperkey(FunctionalDependency.chars("G"), attrs, f));
    }

    /**
     * 判定口径：α 是超键 ⟺ α⁺ ⊇ R。缺口 = R − α⁺，非空就说明"推不出这几列"。
     * A⁺ = ABCH 缺 G、I（G 只能从 AG 或 CG 组合里得到，I 只能由 CG→I 得到）。
     */
    @Test
    @DisplayName("超键判定含解释：A 缺 GI、G 缺 ABCHI、CG 缺 AB、AG 不缺")
    void superkeyCheck() {
        assertCheck("AG", true, "ABCGHI", "");
        assertCheck("A", false, "ABCH", "GI");
        assertCheck("G", false, "G", "ABCHI");
        assertCheck("CG", false, "CGHI", "AB");
        assertCheck("ABC", false, "ABCH", "GI");
        assertCheck("ABCGHI", true, "ABCGHI", "");
    }

    /** 对全部子集交叉印证：缺口恰为 R − X⁺，且两个入口结论一致。 */
    @Test
    @DisplayName("超键判定不变量：缺口 = R − X⁺，isSuperkey ⟺ 缺口为空")
    void superkeyCheckInvariant() {
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            FdCalculator.SuperkeyCheck check = FdCalculator.checkSuperkey(x, attrs, f);

            Set<String> expectedMissing = new java.util.TreeSet<String>(attrs);
            expectedMissing.removeAll(check.closure());
            assertEquals(expectedMissing, check.missing(), "缺口 " + FdCalculator.join(x));
            assertEquals(check.missing().isEmpty(), check.isSuperkey(),
                    "结论应与缺口一致 " + FdCalculator.join(x));
            assertEquals(FdCalculator.attributeClosure(x, f), check.closure(),
                    "闭包应与 attributeClosure 一致 " + FdCalculator.join(x));
            assertEquals(check.isSuperkey(), FdCalculator.isSuperkey(x, attrs, f),
                    "两个入口结论不一致 " + FdCalculator.join(x));
        }
    }

    private void assertCheck(String x, boolean superkey, String closure, String missing) {
        FdCalculator.SuperkeyCheck check =
                FdCalculator.checkSuperkey(FunctionalDependency.chars(x), attrs, f);
        assertEquals(superkey, check.isSuperkey(), "isSuperkey " + x);
        assertEquals(closure, FdCalculator.join(check.closure()), "closure " + x);
        assertEquals(missing, FdCalculator.join(check.missing()), "missing " + x);
        assertEquals(superkey, FdCalculator.isSuperkey(FunctionalDependency.chars(x), attrs, f),
                "简写入口 " + x);
    }

    /** 极小性检查：AG 是唯一候选键，不允许出现 {A,G} 之外的更大的"键"。 */
    @Test
    @DisplayName("候选键唯一为 {A,G}")
    void candidateKey() {
        List<Set<String>> keys = FdCalculator.candidateKeys(attrs, f);
        assertEquals(1, keys.size());
        assertEquals("AG", FdCalculator.join(keys.get(0)));
    }

    @Test
    @DisplayName("F+ 条数：图 8-7 法 1668 条，X+ 法 1701 条")
    void fplusSizes() {
        assertEquals(1668, FdCalculator.closureNaive(attrs, f).fplus().size());
        assertEquals(1701, FdCalculator.closureByAttributeClosures(attrs, f).size());
    }

    /**
     * 这条测试钉住"图 8-7 算法没定义初值"这个结论：
     * 少的 33 条全部是平凡依赖；把自反律一次性加满后，两法结果完全相同。
     */
    @Test
    @DisplayName("图 8-7 缺的 33 条全是平凡依赖；补齐自反律后与 X+ 法一致")
    void trivialDependencyGap() {
        Set<FunctionalDependency> naive = FdCalculator.closureNaive(attrs, f).fplus();
        Set<FunctionalDependency> byAttr = FdCalculator.closureByAttributeClosures(attrs, f);

        Set<FunctionalDependency> missing = new LinkedHashSet<FunctionalDependency>(byAttr);
        missing.removeAll(naive);
        assertEquals(33, missing.size());
        for (FunctionalDependency fd : missing) {
            assertTrue(fd.isTrivial(), "缺失项必须是平凡依赖, 实际: " + fd);
        }

        Set<FunctionalDependency> seeded = new LinkedHashSet<FunctionalDependency>(naive);
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            for (Set<String> sub : FdCalculator.subsets(x)) {
                seeded.add(new FunctionalDependency(x, sub));
            }
        }
        assertEquals(byAttr, seeded);
        assertEquals(1701, seeded.size());
    }

    @Test
    @DisplayName("非平凡依赖两法一致，共 1036 条")
    void nontrivialAgreement() {
        Set<FunctionalDependency> naive = new LinkedHashSet<FunctionalDependency>();
        for (FunctionalDependency fd : FdCalculator.closureNaive(attrs, f).fplus()) {
            if (!fd.isTrivial()) {
                naive.add(fd);
            }
        }
        Set<FunctionalDependency> byAttr = new LinkedHashSet<FunctionalDependency>();
        for (FunctionalDependency fd : FdCalculator.closureByAttributeClosures(attrs, f)) {
            if (!fd.isTrivial()) {
                byAttr.add(fd);
            }
        }
        assertEquals(1036, byAttr.size());
        assertEquals(naive, byAttr);
    }

    /** Y ⊆ X⁺ 就是在 F⁺ 里 —— 这是替代"枚举 1701 条"的判定口径。 */
    @Test
    @DisplayName("FD 归属判定：A→H 在 F+ 中；G→H、BG→C、C→I 不在")
    void membership() {
        assertTrue(inFplus("A", "H"));
        assertTrue(inFplus("CG", "H"));
        assertTrue(inFplus("AG", "HI"));
        assertFalse(inFplus("G", "H"));
        assertFalse(inFplus("BG", "C"));
        assertFalse(inFplus("C", "I"));
    }

    /**
     * 判定口径：X → Y 成立 ⟺ Y ⊆ X⁺。缺口 Y − X⁺ 非空就说明"Y 里有属性推不出来"。
     * A → HI 是"右边部分推不出"的例子：A⁺ = ABCH，H 能推、I 推不出。
     */
    @Test
    @DisplayName("FD 判定：A→H 成立；G→H 缺 H；A→HI 缺 I；AB→A 平凡成立")
    void fdCheck() {
        assertFd("A", "H", true, "ABCH", "", false);
        assertFd("CG", "H", true, "CGHI", "", false);
        assertFd("AG", "HI", true, "ABCGHI", "", false);
        assertFd("G", "H", false, "G", "H", false);
        assertFd("BG", "C", false, "BGH", "C", false);
        assertFd("C", "I", false, "C", "I", false);
        assertFd("A", "HI", false, "ABCH", "I", false);
        assertFd("AB", "A", true, "ABCH", "", true);   // 平凡依赖与 F 无关
    }

    /** 与"枚举 F⁺ 再查表"逐条对账：全部 (X, Y) 组合结论一致。 */
    @Test
    @DisplayName("FD 判定与 F+ 枚举逐对吻合")
    void fdCheckMatchesFplus() {
        Set<FunctionalDependency> fplus = FdCalculator.closureByAttributeClosures(attrs, f);
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            Set<String> closure = FdCalculator.attributeClosure(x, f);
            for (Set<String> y : FdCalculator.subsets(attrs)) {
                FunctionalDependency fd = new FunctionalDependency(x, y);
                FdCalculator.FdCheck check = FdCalculator.checkFd(x, y, f);
                assertEquals(fplus.contains(fd), check.holds(), fd.toString());
                assertEquals(closure, check.closure(), "闭包 " + fd);
                assertEquals(x.containsAll(y), check.trivial(), "平凡性 " + fd);
                Set<String> expectedMissing = new java.util.TreeSet<String>(y);
                expectedMissing.removeAll(closure);
                assertEquals(expectedMissing, check.missing(), "缺口 " + fd);
                assertEquals(check.holds(), FdCalculator.holds(fd, f), "简写入口 " + fd);
            }
        }
    }

    private void assertFd(String lhs, String rhs, boolean holds,
                          String closure, String missing, boolean trivial) {
        FdCalculator.FdCheck check = FdCalculator.checkFd(
                FunctionalDependency.chars(lhs), FunctionalDependency.chars(rhs), f);
        assertEquals(holds, check.holds(), "holds " + lhs + "->" + rhs);
        assertEquals(closure, FdCalculator.join(check.closure()), "closure " + lhs);
        assertEquals(missing, FdCalculator.join(check.missing()), "missing " + lhs);
        assertEquals(trivial, check.trivial(), "trivial " + lhs + "->" + rhs);
        assertEquals(holds, FdCalculator.holds(check.lhs(), check.rhs(), f), "简写入口");
    }

    private boolean inFplus(String lhs, String rhs) {
        Set<String> plus = FdCalculator.attributeClosure(FunctionalDependency.chars(lhs), f);
        return plus.containsAll(FunctionalDependency.chars(rhs));
    }
}
