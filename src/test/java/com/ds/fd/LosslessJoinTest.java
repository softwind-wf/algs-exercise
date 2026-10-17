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
 * 无损分解判定（教材 8.5 节）的回归测试。
 *
 * <p>覆盖两条判定路径：二元分解定理（R1 ∩ R2 是否决定某一侧）与 n 元表格法（chase）。
 * 最后一条测试在 fd-cases.txt 的全部用例、全部二拆分上交叉验证两条路径结论一致。
 */
class LosslessJoinTest {

    @Test
    @DisplayName("二元定理：公共属性是任一侧的超键 ⇒ 无损")
    void binaryLossless() {
        List<FunctionalDependency> f = fds("A->B");
        LosslessJoin.BinaryResult r1 = LosslessJoin.checkBinary(
                chars("ABC"), chars("AB"), chars("AC"), f);
        assertEquals("A", FdCalculator.join(r1.common()));
        assertEquals("AB", FdCalculator.join(r1.closure()));
        assertTrue(r1.impliesR1());
        assertFalse(r1.impliesR2());

        List<FunctionalDependency> f2 = fds("B->C");
        LosslessJoin.BinaryResult r2 = LosslessJoin.checkBinary(
                chars("ABC"), chars("AB"), chars("BC"), f2);
        assertEquals("B", FdCalculator.join(r2.common()));
        assertEquals("BC", FdCalculator.join(r2.closure()));
        assertFalse(r2.impliesR1());
        assertTrue(r2.impliesR2());
    }

    @Test
    @DisplayName("二元定理：公共属性不是任一侧的超键 ⇒ 有损")
    void binaryLossy() {
        List<FunctionalDependency> f = fds("A->B");
        LosslessJoin.BinaryResult r = LosslessJoin.checkBinary(
                chars("ABC"), chars("AB"), chars("BC"), f);
        assertEquals("B", FdCalculator.join(r.common()));
        assertEquals("B", FdCalculator.join(r.closure()));
        assertFalse(r.impliesR1());
        assertFalse(r.impliesR2());
        assertFalse(r.lossless());
    }

    @Test
    @DisplayName("表格法：R=(A,B,C,D), F={A→B, C→D}, 拆成 (AB)(CD)(AC) 无损")
    void chaseLossless() {
        List<FunctionalDependency> f = fds("A->B", "C->D");
        List<Set<String>> parts = parts("AB", "CD", "AC");
        LosslessJoin.ChaseResult r = LosslessJoin.checkChase(chars("ABCD"), parts, f);
        assertTrue(r.lossless());
        assertFalse(r.steps().isEmpty(), "无损分解应当至少应用过一次 FD");
    }

    @Test
    @DisplayName("表格法：拆成 (AB)(CD) 是笛卡尔积 ⇒ 有损")
    void chaseLossy() {
        List<FunctionalDependency> f = fds("A->B", "C->D");
        LosslessJoin.ChaseResult r = LosslessJoin.checkChase(
                chars("ABCD"), parts("AB", "CD"), f);
        assertFalse(r.lossless());
    }

    @Test
    @DisplayName("分解必须恰好覆盖 R，否则抛 IllegalArgumentException")
    void decompositionMustCoverR() {
        List<FunctionalDependency> f = fds("A->B");
        assertThrows(IllegalArgumentException.class,
                () -> LosslessJoin.checkChase(chars("ABCD"), parts("AB", "AC"), f));
        assertThrows(IllegalArgumentException.class,
                () -> LosslessJoin.checkChase(chars("ABC"), parts("ABE", "C"), f));
        assertThrows(IllegalArgumentException.class,
                () -> LosslessJoin.checkChase(chars("ABC"), parts(), f));
        assertThrows(IllegalArgumentException.class,
                () -> LosslessJoin.checkBinary(chars("ABC"), chars("AB"), chars("BCE"), f));
    }

    /**
     * 二元分解定理与表格法必须给出同一答案：对每个用例、每个二拆分逐一比对。
     * 这条测试同时防止 chase 的符号统一逻辑写偏。
     */
    @Test
    @DisplayName("二元定理与表格法在所有用例的所有二拆分上一致")
    void binaryAgreesWithChase() throws IOException {
        Path cases = Paths.get("src/main/resources/fd/fd-cases.txt");
        List<FdSpec.Case> all = FdSpec.load(cases);
        assertFalse(all.isEmpty());

        for (FdSpec.Case c : all) {
            for (Set<String> r1 : FdCalculator.subsets(c.attrs())) {
                if (r1.size() == c.attrs().size()) {
                    continue;                      // 需要真二拆：R2 非空
                }
                Set<String> r2 = new TreeSet<String>(c.attrs());
                r2.removeAll(r1);

                boolean binary = LosslessJoin.isBinaryLossless(c.attrs(), r1, r2, c.fds());
                List<Set<String>> split = new ArrayList<Set<String>>();
                split.add(r1);
                split.add(r2);
                boolean chase = LosslessJoin.isLossless(c.attrs(), split, c.fds());
                assertEquals(binary, chase,
                        c.name() + ": " + FdCalculator.join(r1) + " | " + FdCalculator.join(r2));
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
}
