package com.ds.fd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 保持依赖判定（教材 8.4.5 节）的回归测试。
 *
 * <p>两种方法都要测：图 8-10 的显式 F⁺ 法，与修改属性闭包法。重点断言：
 * 两者在 fd-cases.txt 的所有二拆分上结论一致；"无损"与"保持依赖"互相独立
 * （既有"无损但丢依赖"，也有"保持依赖但有损"）。
 */
class DependencyPreservationTest {

    @Test
    @DisplayName("保持依赖：inst_dept 形状的拆分（F 全部落在子模式内）")
    void preservedInstDeptShape() {
        Set<String> r = chars("ABCDE");
        List<FunctionalDependency> f = fds("A->BC", "C->DE");
        List<Set<String>> d = parts("ABC", "CDE");

        DependencyPreservation.FPlusCheck byFPlus = DependencyPreservation.checkByFPlus(r, d, f);
        assertTrue(byFPlus.preserves());
        assertTrue(byFPlus.lost().isEmpty());

        DependencyPreservation.ClosureCheck byClosure = DependencyPreservation.checkByModifiedClosure(r, d, f);
        assertTrue(byClosure.preserves());
        assertEquals(2, byClosure.verdicts().size());
        for (DependencyPreservation.Verdict verdict : byClosure.verdicts()) {
            assertTrue(verdict.preserved(), verdict.toString());
        }
    }

    /**
     * 经典反例：R=(A,B,C)、F={AB→C, C→A}，拆成 (A,C)+(B,C)。
     * AB→C 的 AB 跨在两个子模式之间，F' 只剩 C→A，推不出 AB→C。
     * 同时这个拆分是**无损**的（公共属性 C 决定 A），正好说明两个要求互相独立。
     */
    @Test
    @DisplayName("丢依赖：(A,C)+(B,C) 下 AB→C 丢失，但分解无损")
    void lostDependencyButLossless() {
        Set<String> r = chars("ABC");
        List<FunctionalDependency> f = fds("AB->C", "C->A");
        List<Set<String>> d = parts("AC", "BC");

        DependencyPreservation.FPlusCheck byFPlus = DependencyPreservation.checkByFPlus(r, d, f);
        assertFalse(byFPlus.preserves());
        assertEquals(set(fds("AB->C")), byFPlus.lost());

        DependencyPreservation.ClosureCheck byClosure = DependencyPreservation.checkByModifiedClosure(r, d, f);
        assertFalse(byClosure.preserves());
        assertEquals(set(fds("AB->C")), byClosure.lost());

        DependencyPreservation.Verdict ab = byClosure.verdicts().get(0);
        assertEquals("AB", FdCalculator.join(ab.closure()));
        assertFalse(ab.preserved());

        assertTrue(LosslessJoin.isBinaryLossless(r, chars("AC"), chars("BC"), f),
                "C→A 让公共属性 C 成为 AC 的超键，所以这个拆分无损");
    }

    /** 保持依赖但有损：两个子模式各自守住自己的 FD，但连接会变笛卡尔积。 */
    @Test
    @DisplayName("保持依赖但有损：(A,B)+(C,D) 守住 A→B、C→D，但连接成笛卡尔积")
    void preservedButLossy() {
        Set<String> r = chars("ABCD");
        List<FunctionalDependency> f = fds("A->B", "C->D");
        List<Set<String>> d = parts("AB", "CD");

        assertTrue(DependencyPreservation.preserves(r, d, f));
        assertTrue(DependencyPreservation.checkByFPlus(r, d, f).preserves());
        assertFalse(LosslessJoin.isLossless(r, d, f));
    }

    /** 单块分解（不拆）：F'⁺ = F⁺，必然保持依赖。 */
    @Test
    @DisplayName("不拆分（单块 R）必然保持依赖")
    void singlePartAlwaysPreserves() {
        Set<String> r = chars("ABC");
        List<FunctionalDependency> f = fds("AB->C", "C->A");
        List<Set<String>> d = parts("ABC");
        assertTrue(DependencyPreservation.preserves(r, d, f));
        assertTrue(DependencyPreservation.checkByFPlus(r, d, f).preserves());
    }

    @Test
    @DisplayName("分解必须恰好覆盖 R")
    void validatesDecomposition() {
        List<FunctionalDependency> f = fds("A->B");
        assertThrows(IllegalArgumentException.class,
                () -> DependencyPreservation.preserves(chars("ABCD"), parts("AB", "AC"), f));
        assertThrows(IllegalArgumentException.class,
                () -> DependencyPreservation.checkByFPlus(chars("ABC"), parts("ABE", "C"), f));
        assertThrows(IllegalArgumentException.class,
                () -> DependencyPreservation.preserves(chars("ABC"), parts(), f));
    }

    /**
     * 两种方法必须一致：对 fd-cases.txt 的每个用例、每个二拆分，
     * 比较"是否保持依赖"与"丢失了哪些依赖"。
     */
    @Test
    @DisplayName("图 8-10 法与修改闭包法在所有用例的所有二拆分上一致")
    void bothMethodsAgree() throws IOException {
        Path cases = Paths.get("src/main/resources/fd/fd-cases.txt");
        List<FdSpec.Case> all = FdSpec.load(cases);
        assertFalse(all.isEmpty());

        for (FdSpec.Case c : all) {
            for (Set<String> r1 : FdCalculator.subsets(c.attrs())) {
                if (r1.size() == c.attrs().size()) {
                    continue;
                }
                Set<String> r2 = new TreeSet<String>(c.attrs());
                r2.removeAll(r1);
                List<Set<String>> split = java.util.Arrays.asList(r1, r2);

                DependencyPreservation.FPlusCheck byFPlus =
                        DependencyPreservation.checkByFPlus(c.attrs(), split, c.fds());
                DependencyPreservation.ClosureCheck byClosure =
                        DependencyPreservation.checkByModifiedClosure(c.attrs(), split, c.fds());

                assertEquals(byFPlus.preserves(), byClosure.preserves(),
                        c.name() + ": " + FdCalculator.join(r1) + " | " + FdCalculator.join(r2));
                assertEquals(byFPlus.lost(), byClosure.lost(),
                        c.name() + " 丢失集合不同: " + FdCalculator.join(r1) + " | " + FdCalculator.join(r2));
            }
        }
    }

    // ------------------------------------------------------------------

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

    private static Set<FunctionalDependency> set(List<FunctionalDependency> fds) {
        return new LinkedHashSet<FunctionalDependency>(fds);
    }
}
