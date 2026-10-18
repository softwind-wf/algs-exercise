package com.ds.fd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BCNF 判定（教材 8.5 节，中文版第 198 页）的回归测试。
 *
 * <p>重点测三件事：
 * <ol>
 *   <li>教材 198 页的反例：R(A,B,C,D,E)、F={A→B, BC→D} 拆成 (A,B)+(A,C,D,E) 后，
 *       R2 只看 F 会误判 BCNF，用 F⁺ 或子集闭包法才能发现 AC→D；</li>
 *   <li>"未分解只看 F 就够"的定理：{@link BcnfCheck#checkRelation} 与
 *       {@link BcnfCheck#checkRelationByFPlus} 结论一致；</li>
 *   <li>分解后子模式的两种判定（子集闭包法 vs 物化 Π_Ri(F⁺)）在 fd-cases.txt 的所有二拆分上一致。</li>
 * </ol>
 */
class BcnfCheckTest {

    // ------------------------------------------------------------------
    // 1. 教材 198 页的例子
    // ------------------------------------------------------------------

    @Test
    @DisplayName("未分解：R(A,B,C,D,E)、F={A->B, BC->D} 不属于 BCNF（两条 FD 左部都不是超键）")
    void textbookWholeRelation() {
        Set<String> r = chars("ABCDE");
        List<FunctionalDependency> f = fds("A->B", "BC->D");

        BcnfCheck.CheckResult byF = BcnfCheck.checkRelation(r, f);
        assertFalse(byF.bcnf());
        assertEquals(2, byF.violations().size());

        BcnfCheck.Violation first = byF.violations().get(0);
        assertEquals(FunctionalDependency.of("A", "B"), first.fd());
        assertEquals("AB", FdCalculator.join(first.closure()), "A+ = AB");
        assertEquals("CDE", FdCalculator.join(first.missing()), "A+ 缺 CDE");
        assertTrue(first.inGivenSet(), "A->B 就在 F 里");

        assertEquals(FunctionalDependency.of("BC", "D"), byF.violations().get(1).fd());
        assertEquals("BCD", FdCalculator.join(byF.violations().get(1).closure()), "BC+ = BCD");
        assertEquals("AE", FdCalculator.join(byF.violations().get(1).missing()));

        // 定理：只看 F 与物化 F⁺ 的结论必须一致；但 F⁺ 法的违反项更多
        BcnfCheck.CheckResult byFPlus = BcnfCheck.checkRelationByFPlus(r, f);
        assertFalse(byFPlus.bcnf());
        assertTrue(byFPlus.violations().size() > byF.violations().size(),
                "F+ 里会多出 AC->D 这类由传递律导出的违反项");
        assertTrue(byFPlus.violatingDeterminants().contains(chars("AC")),
                "AC 在 F+ 里也不是超键");
    }

    @Test
    @DisplayName("教材反例：拆成 (A,B)+(A,C,D,E)，R2 不属于 BCNF，见证依赖 AC->D 不在 F 中")
    void textbookDecomposedR2() {
        Set<String> r = chars("ABCDE");
        List<FunctionalDependency> f = fds("A->B", "BC->D");
        Set<String> r2 = chars("ACDE");

        // 只看 F 是查不出问题的：F 中没有一条依赖的左右部都落在 R2 内
        for (FunctionalDependency fd : f) {
            assertFalse(r2.containsAll(fd.determinant()) && r2.containsAll(fd.dependent()),
                    fd + " 不应整条落在 R2 内");
        }

        BcnfCheck.CheckResult result = BcnfCheck.checkProjected(r2, r, f);
        assertFalse(result.bcnf());
        assertEquals(1, result.violations().size(), "R2 里只有 alpha=AC 违反子集条件");

        BcnfCheck.Violation violation = result.violations().get(0);
        assertEquals(FunctionalDependency.of("AC", "D"), violation.fd());
        assertFalse(violation.inGivenSet(), "AC->D 是 F+ 里才有的依赖");
        assertEquals("ABCD", FdCalculator.join(violation.closure()), "AC+ = ABCD（伪传递律）");
        assertEquals("E", FdCalculator.join(violation.missing()), "AC+ 缺 E，不是 R2 的超键");

        // 物化 Π_R2(F⁺) 的对照路线：结论相同，并且确实能看到 AC->D
        BcnfCheck.CheckResult byFPlus = BcnfCheck.checkProjectedByFPlus(r2, r, f);
        assertFalse(byFPlus.bcnf());
        assertTrue(byFPlus.projected().contains(FunctionalDependency.of("AC", "D")));
        assertEquals(1, byFPlus.violatingDeterminants().size());
        assertEquals(chars("AC"), byFPlus.violatingDeterminants().iterator().next());
    }

    @Test
    @DisplayName("教材反例：R1(A,B) 属于 BCNF；整个分解不是 BCNF 分解")
    void textbookDecomposition() {
        Set<String> r = chars("ABCDE");
        List<FunctionalDependency> f = fds("A->B", "BC->D");
        List<Set<String>> d = parts("AB", "ACDE");

        assertTrue(BcnfCheck.checkProjected(chars("AB"), r, f).bcnf(),
                "A+ = AB 是 AB 的超键，R1 满足 BCNF");

        BcnfCheck.DecompositionResult decomposition = BcnfCheck.checkDecomposition(r, d, f);
        assertFalse(decomposition.bcnf());
        assertEquals(2, decomposition.parts().size());
        assertEquals(1, decomposition.violatingParts().size());
        assertEquals(chars("ACDE"), decomposition.violatingParts().get(0).relation());
        assertFalse(BcnfCheck.isBcnfDecomposition(r, d, f));
    }

    // ------------------------------------------------------------------
    // 2. 经典形状：3NF 但非 BCNF
    // ------------------------------------------------------------------

    /**
     * R(A,B,C)、F={AB→C, C→B}：C→B 的左部 C 不是超键（C⁺=BC 缺 A），
     * 所以 R 是 3NF 但不属于 BCNF（C 是主属性，3NF 放行）。拆成 (B,C)+(A,C) 后每张都 BCNF。
     */
    @Test
    @DisplayName("3NF 但非 BCNF：R(A,B,C)、F={AB->C, C->B}，拆 (B,C)+(A,C) 后都是 BCNF")
    void classic3nfNotBcnfShape() {
        Set<String> r = chars("ABC");
        List<FunctionalDependency> f = fds("AB->C", "C->B");

        BcnfCheck.CheckResult whole = BcnfCheck.checkRelation(r, f);
        assertFalse(whole.bcnf());
        assertEquals(1, whole.violations().size());
        assertEquals(FunctionalDependency.of("C", "B"), whole.violations().get(0).fd());
        assertEquals("A", FdCalculator.join(whole.violations().get(0).missing()));

        List<Set<String>> d = parts("BC", "AC");
        for (BcnfCheck.CheckResult part : BcnfCheck.checkDecomposition(r, d, f).parts()) {
            assertTrue(part.bcnf(), part.toString());
        }
        assertTrue(BcnfCheck.isBcnfDecomposition(r, d, f));
    }

    // ------------------------------------------------------------------
    // 3. 两种路线的交叉验证
    // ------------------------------------------------------------------

    @Test
    @DisplayName("未分解：只看 F 与物化 F+ 对所有用例结论一致")
    void checkRelationAgreesWithFPlus() throws IOException {
        for (FdSpec.Case c : loadCases()) {
            BcnfCheck.CheckResult byF = BcnfCheck.checkRelation(c.attrs(), c.fds());
            BcnfCheck.CheckResult byFPlus = BcnfCheck.checkRelationByFPlus(c.attrs(), c.fds());
            assertEquals(byF.bcnf(), byFPlus.bcnf(), c.name() + " 两种未分解判定结论不一致");
        }
    }

    @Test
    @DisplayName("分解后：子集闭包法与物化 Π_Ri(F+) 在所有用例的所有二拆分上一致")
    void checkProjectedAgreesWithFPlus() throws IOException {
        for (FdSpec.Case c : loadCases()) {
            for (Set<String> r1 : FdCalculator.subsets(c.attrs())) {
                if (r1.size() == c.attrs().size()) {
                    continue;
                }
                Set<String> r2 = new TreeSet<String>(c.attrs());
                r2.removeAll(r1);

                BcnfCheck.CheckResult subset = BcnfCheck.checkProjected(r1, c.attrs(), c.fds());
                BcnfCheck.CheckResult byFPlus = BcnfCheck.checkProjectedByFPlus(r1, c.attrs(), c.fds());
                assertEquals(subset.bcnf(), byFPlus.bcnf(),
                        c.name() + ": " + FdCalculator.join(r1) + " | " + FdCalculator.join(r2));
                assertEquals(subset.violatingDeterminants(), byFPlus.violatingDeterminants(),
                        c.name() + " 违反左部集合不同: " + FdCalculator.join(r1));

                BcnfCheck.CheckResult subset2 = BcnfCheck.checkProjected(r2, c.attrs(), c.fds());
                BcnfCheck.CheckResult byFPlus2 = BcnfCheck.checkProjectedByFPlus(r2, c.attrs(), c.fds());
                assertEquals(subset2.bcnf(), byFPlus2.bcnf(),
                        c.name() + ": " + FdCalculator.join(r2));
            }
        }
    }

    /** 对整张 R 用子集法，结论应与"只看 F"的判定一致（子集法是通用判定，R 本身也是一种 Ri）。 */
    @Test
    @DisplayName("子集法作用在 R 本身上，与只看 F 的判定结论一致")
    void subsetMethodOnWholeRelation() throws IOException {
        for (FdSpec.Case c : loadCases()) {
            BcnfCheck.CheckResult byF = BcnfCheck.checkRelation(c.attrs(), c.fds());
            BcnfCheck.CheckResult subset = BcnfCheck.checkProjected(c.attrs(), c.attrs(), c.fds());
            assertEquals(byF.bcnf(), subset.bcnf(), c.name() + " 结论不一致");
        }
    }

    // ------------------------------------------------------------------
    // 4. 边界
    // ------------------------------------------------------------------

    @Test
    @DisplayName("单属性子模式、空 F、全平凡 F 都属于 BCNF")
    void boundaries() {
        List<FunctionalDependency> f = fds("A->B");
        for (String one : new String[]{"A", "B", "C"}) {
            BcnfCheck.CheckResult part = BcnfCheck.checkProjected(chars(one), chars("ABC"), f);
            assertTrue(part.bcnf(), one + " 单属性子模式必然是 BCNF");
            assertTrue(part.violations().isEmpty());
        }

        assertTrue(BcnfCheck.checkRelation(chars("ABC"), new ArrayList<FunctionalDependency>()).bcnf());
        assertTrue(BcnfCheck.checkRelation(chars("ABC"), fds("A->A", "AB->A", "BC->B")).bcnf(),
                "全平凡依赖不构成违反");
    }

    @Test
    @DisplayName("入参校验：F 越过属性集、子模式不在 R 内、分解未覆盖 R 都要报错")
    void validatesArguments() {
        List<FunctionalDependency> f = fds("A->B");

        assertThrows(IllegalArgumentException.class,
                () -> BcnfCheck.checkRelation(chars("AB"), fds("A->C")));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfCheck.checkRelation(new TreeSet<String>(), f));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfCheck.checkProjected(chars("ABE"), chars("ABC"), f));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfCheck.checkProjected(new TreeSet<String>(), chars("ABC"), f));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfCheck.checkDecomposition(chars("ABC"), parts("AB"), f));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfCheck.checkDecomposition(chars("ABC"), parts(), f));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfCheck.checkDecomposition(chars("ABC"), parts("AB", ""), f));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfCheck.checkDecomposition(chars("ABC"), parts("AB", "AE"), f));
    }

    // ------------------------------------------------------------------

    private static List<FdSpec.Case> loadCases() throws IOException {
        Path cases = Paths.get("src/main/resources/fd/fd-cases.txt");
        List<FdSpec.Case> all = FdSpec.load(cases);
        assertFalse(all.isEmpty());
        return all;
    }

    private static Set<String> chars(String attrs) {
        return FunctionalDependency.chars(attrs);
    }

    private static List<FunctionalDependency> fds(String... specs) {
        List<FunctionalDependency> out = new ArrayList<FunctionalDependency>();
        for (String spec : specs) {
            int arrow = spec.indexOf("->");
            out.add(FunctionalDependency.of(
                    spec.substring(0, arrow).trim(),
                    spec.substring(arrow + 2).trim()));
        }
        return out;
    }

    private static List<Set<String>> parts(String... specs) {
        List<Set<String>> out = new ArrayList<Set<String>>();
        for (String spec : specs) {
            out.add(FunctionalDependency.chars(spec));
        }
        return out;
    }
}
