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

    @Test
    @DisplayName("超键判定：AG 是超键，A 与 G 都不是")
    void superkey() {
        assertTrue(FdCalculator.isSuperkey(FunctionalDependency.chars("AG"), attrs, f));
        assertFalse(FdCalculator.isSuperkey(FunctionalDependency.chars("A"), attrs, f));
        assertFalse(FdCalculator.isSuperkey(FunctionalDependency.chars("G"), attrs, f));
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

    private boolean inFplus(String lhs, String rhs) {
        Set<String> plus = FdCalculator.attributeClosure(FunctionalDependency.chars(lhs), f);
        return plus.containsAll(FunctionalDependency.chars(rhs));
    }
}
