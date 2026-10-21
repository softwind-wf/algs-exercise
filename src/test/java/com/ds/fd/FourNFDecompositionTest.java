package com.ds.fd;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link FourNFDecomposition}（图 8-16）的回归测试。 */
class FourNFDecompositionTest {

    /** 教材 8.6.3 节的例子：分解出 r21(ID, dept_name) 与 r22(ID, street, city)。 */
    @Test
    void textbookDeptExample() {
        Set<String> r = attrs("ID,dept_name,street,city");
        List<FunctionalDependency> fds = new ArrayList<FunctionalDependency>();
        List<MultivaluedDependency> mvds = mvds(
                new MultivaluedDependency(attrs("ID"), attrs("dept_name")),
                new MultivaluedDependency(attrs("ID"), attrs("street,city")));

        FourNFDecomposition.CheckResult check = FourNFDecomposition.check(r, fds, mvds);
        assertFalse(check.fourNF());
        assertEquals(2, check.violations().size());        // ID->->dept_name 与补律的 ID->->street,city

        FourNFDecomposition.Result result = FourNFDecomposition.decompose(r, fds, mvds);
        assertEquals(2, result.decomposition().size());
        assertEquals(Arrays.<Set<String>>asList(attrs("ID,dept_name"), attrs("city,ID,street")),
                result.decomposition());
        assertEquals(1, result.stepCount());
        FourNFDecomposition.Step step = result.steps().get(0);
        assertEquals(attrs("ID"), step.mvd().determinant());
        assertEquals(attrs("city,street"), step.mvd().dependent());   // 排序后第一条是 ID->->city,street
        assertEquals(attrs("ID,dept_name"), step.left());
        assertEquals(attrs("city,ID,street"), step.right());
        assertTrue(step.inGivenSet());
        assertTrue(result.fourNF());
        assertTrue(result.lossless());
        assertTrue(result.coversAll());
        assertTrue(result.preservesDependencies(fds));     // F 为空, 平凡地保持
    }

    /** 已属 4NF 的模式不拆（FD 复制出的 MVD 左部是超键）。 */
    @Test
    void alreadyFourNFStaysUntouched() {
        Set<String> r = FunctionalDependency.chars("ABC");
        List<FunctionalDependency> fds = fds("A->BC");   // A+ = ABC, A 是超键

        assertTrue(FourNFDecomposition.check(r, fds, mvds()).fourNF());
        FourNFDecomposition.Result result = FourNFDecomposition.decompose(r, fds, mvds());
        assertEquals(1, result.decomposition().size());
        assertEquals(new TreeSet<String>(r), result.decomposition().get(0));
        assertEquals(0, result.stepCount());
        assertTrue(result.fourNF());
        assertTrue(result.lossless());
    }

    /** 平凡 MVD（β⊆α 或 α∪β=R）不构成违反项。 */
    @Test
    void trivialMvdDoesNotSplit() {
        Set<String> r = FunctionalDependency.chars("AB");
        List<MultivaluedDependency> mvds = mvds(
                new MultivaluedDependency(attrs("A"), attrs("B")));   // α∪β = R, 平凡

        assertTrue(FourNFDecomposition.check(r, emptyFds(), mvds).fourNF());
        assertTrue(FourNFDecomposition.check(attrs("ID,dept_name"), emptyFds(), mvds(
                new MultivaluedDependency(attrs("ID"), attrs("dept_name")))).fourNF());
    }

    /** FD 按复制律蕴含 MVD：A→B 与 B→C 让 B→→C 成为违反项（B 不是超键）。 */
    @Test
    void fdReplicationImpliesMvd() {
        Set<String> r = FunctionalDependency.chars("ABCD");
        List<FunctionalDependency> fds = fds("A->B", "B->C");

        FourNFDecomposition.CheckResult check = FourNFDecomposition.check(r, fds, mvds());
        assertFalse(check.fourNF());
        assertEquals(attrs("A"), check.violations().get(0).mvd().determinant());   // 排序后第一条
        assertEquals(attrs("B"), check.violations().get(0).mvd().dependent());
        assertTrue(check.violations().get(0).inGivenSet());
        boolean hasDerived = false;
        for (FourNFDecomposition.Violation v : check.violations()) {
            if (!v.inGivenSet()) {
                hasDerived = true;                             // 例如补律推出的 B->->A,D
            }
        }
        assertTrue(hasDerived);

        FourNFDecomposition.Result result = FourNFDecomposition.decompose(r, fds, mvds());
        assertEquals(Arrays.<Set<String>>asList(attrs("A,C,D"), attrs("A,B")),
                result.decomposition());
        assertEquals(1, result.stepCount());
        assertTrue(result.fourNF());
        assertTrue(result.lossless());
        assertTrue(result.coversAll());
    }

    /** 补律：只给 A->->BC，Ri 上会推出 A->->D；拆分后两块都属 4NF。 */
    @Test
    void complementationRuleDerivesMvd() {
        Set<String> r = FunctionalDependency.chars("ABCD");
        List<MultivaluedDependency> mvds = mvds(
                new MultivaluedDependency(FunctionalDependency.chars("A"), FunctionalDependency.chars("BC")));

        FourNFDecomposition.CheckResult check = FourNFDecomposition.check(r, emptyFds(), mvds);
        assertFalse(check.fourNF());
        assertEquals(attrs("A"), check.violations().get(0).mvd().determinant());
        assertEquals(attrs("B,C"), check.violations().get(0).mvd().dependent());
        assertTrue(check.violations().get(0).inGivenSet());

        FourNFDecomposition.Result result = FourNFDecomposition.decompose(r, emptyFds(), mvds);
        assertEquals(Arrays.<Set<String>>asList(attrs("A,D"), attrs("A,B,C")),
                result.decomposition());
        assertEquals(attrs("A,D"), result.steps().get(0).left());
        assertEquals(attrs("A,B,C"), result.steps().get(0).right());
        assertTrue(result.fourNF());
        assertTrue(result.lossless());
    }

    /** 多步拆分（CTX 型例子）：A→→B 与 FD A→C, 拆两轮, 结果可能过度规范化。 */
    @Test
    void multiStepDecomposition() {
        Set<String> r = FunctionalDependency.chars("ABCD");
        List<FunctionalDependency> fds = fds("A->C");
        List<MultivaluedDependency> mvds = mvds(
                new MultivaluedDependency(attrs("A"), attrs("B")));

        FourNFDecomposition.Result result = FourNFDecomposition.decompose(r, fds, mvds);
        assertEquals(2, result.stepCount());
        assertEquals(Arrays.<Set<String>>asList(attrs("A,B"), attrs("A,D"), attrs("A,C")),
                result.decomposition());
        assertTrue(result.fourNF());
        assertTrue(result.lossless());
        assertTrue(result.coversAll());
    }

    /** 同一输入跑两遍, 分解结果与步骤完全一致（算法可复现）。 */
    @Test
    void decompositionIsDeterministic() {
        Set<String> r = FunctionalDependency.chars("ABCD");
        List<FunctionalDependency> fds = fds("A->C");
        List<MultivaluedDependency> mvds = mvds(
                new MultivaluedDependency(attrs("A"), attrs("B")));

        FourNFDecomposition.Result first = FourNFDecomposition.decompose(r, fds, mvds);
        FourNFDecomposition.Result second = FourNFDecomposition.decompose(r, fds, mvds);
        assertEquals(first.decomposition(), second.decomposition());
        assertEquals(first.steps().toString(), second.steps().toString());
    }

    /** 超键左部的非平凡 MVD 不违反 4NF（4NF 弱于"看起来像"的违反）。 */
    @Test
    void superkeyMvdIsNotAViolation() {
        Set<String> r = FunctionalDependency.chars("ABC");
        List<FunctionalDependency> fds = fds("A->BC");

        assertTrue(FourNFDecomposition.check(r, fds, mvds(
                new MultivaluedDependency(attrs("A"), attrs("B")))).fourNF());
    }

    /** 入参校验：空 R、F/D 里出现 R 之外的属性都拒绝。 */
    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> FourNFDecomposition.decompose(attrs(""), fds("A->B"), mvds()));
        assertThrows(IllegalArgumentException.class,
                () -> FourNFDecomposition.decompose(FunctionalDependency.chars("ABC"), fds("A->D"), mvds()));
        assertThrows(IllegalArgumentException.class,
                () -> FourNFDecomposition.decompose(FunctionalDependency.chars("ABC"), mvds2Fds(), mvds(
                        new MultivaluedDependency(attrs("A"), attrs("D")))));
    }

    // ------------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------------

    private static Set<String> attrs(String commaSeparated) {
        Set<String> out = new TreeSet<String>();
        for (String token : commaSeparated.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
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

    private static List<FunctionalDependency> emptyFds() {
        return new ArrayList<FunctionalDependency>();
    }

    private static List<FunctionalDependency> mvds2Fds() {
        return new ArrayList<FunctionalDependency>();
    }

    private static List<MultivaluedDependency> mvds(MultivaluedDependency... ds) {
        return new ArrayList<MultivaluedDependency>(Arrays.asList(ds));
    }
}
