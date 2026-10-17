package com.ds.fd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 无关属性与正则覆盖（教材 8.4.3 / 图 8-9）的回归测试。
 *
 * <p>教材原例：F = {AB→CD, A→E, E→C}，C 在 AB→CD 右部无关（F′ 下 AB⁺ = ABCDE）；
 * F = {AB→C, A→C}，B 在 AB→C 左部无关。性质部分对 fd-cases.txt 的每个用例断言：
 * 正则覆盖与 F 闭包相同、不含无关属性、左部互不相同。
 */
class CanonicalCoverTest {

    /** 规定：正则覆盖与 F 有相同闭包、无无关属性、左部唯一。 */
    @Test
    @DisplayName("左部无关：B 在 AB→C 中多余（A→C 已能推出 C）")
    void extraneousInLhs() {
        List<FunctionalDependency> f = fds("AB->C", "A->C");
        CanonicalCover.ExtraneousCheck check = CanonicalCover.check(f.get(0), f);
        assertEquals("B", FdCalculator.join(check.lhsExtraneous()));
        // C 在右部同样可删（A→C 已能推出它）；两边都无关时先删哪边都合法
        assertEquals("C", FdCalculator.join(check.rhsExtraneous()));
        assertTrue(check.any());

        CanonicalCover.ExtraneousCheck noExtraneous = CanonicalCover.check(f.get(1), f);
        assertFalse(noExtraneous.any());
    }

    /** 教材原例：C 在 AB→CD 右部无关，判据是 F′ 下 AB⁺ 已包含 C。 */
    @Test
    @DisplayName("右部无关：C 在 AB→CD 中多余（F′ 下 AB⁺ = ABCDE）")
    void extraneousInRhs() {
        List<FunctionalDependency> f = fds("AB->CD", "A->E", "E->C");
        CanonicalCover.ExtraneousCheck check = CanonicalCover.check(f.get(0), f);
        assertEquals("C", FdCalculator.join(check.rhsExtraneous()));
        assertTrue(check.lhsExtraneous().isEmpty());

        // 复算教材手算过程：F′ = {AB→D, A→E, E→C}，AB⁺ = ABCDE
        List<FunctionalDependency> fPrime = fds("AB->D", "A->E", "E->C");
        assertEquals("ABCDE", FdCalculator.join(FdCalculator.attributeClosure(
                FunctionalDependency.chars("AB"), fPrime)));
    }

    /** 教材例：{AB→CD, A→E, E→C} 的正则覆盖。 */
    @Test
    @DisplayName("正则覆盖：{AB→CD, A→E, E→C} => {AB→D, A→E, E→C}")
    void coverBookExample() {
        CanonicalCover.Result result = CanonicalCover.compute(fds("AB->CD", "A->E", "E->C"));
        assertEquals(set(fds("AB->D", "A->E", "E->C")), new LinkedHashSet<FunctionalDependency>(result.fds()));
        assertEquals(3, result.size());
        assertFalse(result.steps().isEmpty());
    }

    /** 合并律 + 连锁化简：{A→BC, B→C, A→B, AB→C} => {A→B, B→C}。 */
    @Test
    @DisplayName("正则覆盖：{A→BC, B→C, A→B, AB→C} => {A→B, B→C}")
    void coverMergeAndDrop() {
        List<FunctionalDependency> f = fds("A->BC", "B->C", "A->B", "AB->C");
        CanonicalCover.Result result = CanonicalCover.compute(f);
        assertEquals(set(fds("A->B", "B->C")), new LinkedHashSet<FunctionalDependency>(result.fds()));
    }

    /** 教材 8.4 的 F：先合并 CG→H 与 CG→I，再检查无关属性，5 条降为 3 条。 */
    @Test
    @DisplayName("正则覆盖：教材 8.4 的 F => {A→BC, CG→HI, B→H}")
    void coverTextbookExample() {
        List<FunctionalDependency> f = fds("A->B", "A->C", "CG->H", "CG->I", "B->H");
        CanonicalCover.Result result = CanonicalCover.compute(f);
        assertEquals(set(fds("A->BC", "CG->HI", "B->H")),
                new LinkedHashSet<FunctionalDependency>(result.fds()));
        assertEquals(3, result.size());
    }

    /**
     * 多字符属性名：{A, BC} 与 {AB, C} 拼成字符串都是 "ABC"，但左部不同，不能合并。
     * 这条测试钉住"分组键必须用属性集合，不能用 join 拼串"。
     */
    @Test
    @DisplayName("多字符属性名：{A,BC} 与 {AB,C} 不能因拼串相同被误合并")
    void multiCharacterAttributeNames() {
        List<FunctionalDependency> f = new ArrayList<FunctionalDependency>();
        f.add(new FunctionalDependency(setOf("A", "BC"), setOf("X")));
        f.add(new FunctionalDependency(setOf("AB", "C"), setOf("Y")));

        List<FunctionalDependency> cover = CanonicalCover.cover(f);
        assertEquals(2, cover.size(), "两个不同左部不能被合并");
        assertEquals(new LinkedHashSet<FunctionalDependency>(f),
                new LinkedHashSet<FunctionalDependency>(cover));

        // 真正同左部（同一组多字符属性）仍应正常合并
        List<FunctionalDependency> merge = new ArrayList<FunctionalDependency>();
        merge.add(new FunctionalDependency(setOf("A", "BC"), setOf("X")));
        merge.add(new FunctionalDependency(setOf("BC", "A"), setOf("Y")));
        List<FunctionalDependency> merged = CanonicalCover.cover(merge);
        assertEquals(1, merged.size());
        assertEquals(setOf("A", "BC"), merged.get(0).determinant());
        assertEquals(setOf("X", "Y"), merged.get(0).dependent());
    }

    /**
     * 右部为空的依赖恒成立、不约束任何东西：入口直接丢弃，不能化简成 ∅→∅ 留在结果里。
     */
    @Test
    @DisplayName("右部为空的依赖直接丢弃，不产出 ∅→∅")
    void emptyRhsIsDropped() {
        List<FunctionalDependency> onlyEmpty = new ArrayList<FunctionalDependency>();
        onlyEmpty.add(new FunctionalDependency(setOf("A", "B"), new TreeSet<String>()));
        assertTrue(CanonicalCover.cover(onlyEmpty).isEmpty());

        List<FunctionalDependency> mixed = new ArrayList<FunctionalDependency>();
        mixed.add(new FunctionalDependency(setOf("A", "B"), new TreeSet<String>()));
        mixed.add(FunctionalDependency.of("B", "C"));
        List<FunctionalDependency> cover = CanonicalCover.cover(mixed);
        assertEquals(1, cover.size());
        assertEquals("B", FdCalculator.join(cover.get(0).determinant()));
        assertEquals("C", FdCalculator.join(cover.get(0).dependent()));
    }

    /** 对用例文件里的每个例子断言正则覆盖的每条定义性质。 */
    @Test
    @DisplayName("正则覆盖性质：闭包相同、无无关属性、左部唯一、左右不相交")
    void coverProperties() throws IOException {
        Path cases = Paths.get("src/main/resources/fd/fd-cases.txt");
        List<FdSpec.Case> all = FdSpec.load(cases);
        assertFalse(all.isEmpty());

        for (FdSpec.Case c : all) {
            List<FunctionalDependency> cover = CanonicalCover.cover(c.fds());
            assertTrue(cover.size() <= c.fds().size(), c.name() + ": 覆盖不应比原集更大");

            // 1) 闭包相同：F 与 F_c 对每个属性子集算出的 X⁺ 一致
            for (Set<String> x : FdCalculator.subsets(c.attrs())) {
                assertEquals(FdCalculator.attributeClosure(x, c.fds()),
                        FdCalculator.attributeClosure(x, cover),
                        c.name() + ": 闭包不同, X = " + FdCalculator.join(x));
            }

            // 2) 不含无关属性（用 F_c 自身检验）
            for (FunctionalDependency fd : cover) {
                assertFalse(CanonicalCover.check(fd, cover).any(),
                        c.name() + ": " + fd + " 仍含无关属性");
            }

            // 3) 左部互不相同（合并律已生效）
            for (int i = 0; i < cover.size(); i++) {
                for (int j = i + 1; j < cover.size(); j++) {
                    assertFalse(cover.get(i).determinant().equals(cover.get(j).determinant()),
                            c.name() + ": 左部重复 " + cover.get(i) + " / " + cover.get(j));
                }
            }

            // 4) 每条都非平凡：右部非空且与左部不相交
            for (FunctionalDependency fd : cover) {
                assertFalse(fd.dependent().isEmpty(), c.name() + ": 右部为空 " + fd);
                assertTrue(java.util.Collections.disjoint(fd.determinant(), fd.dependent()),
                        c.name() + ": 左右相交 " + fd);
            }
        }
    }

    // ------------------------------------------------------------------

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

    private static Set<FunctionalDependency> set(List<FunctionalDependency> fds) {
        return new LinkedHashSet<FunctionalDependency>(fds);
    }

    private static Set<String> setOf(String... attrs) {
        return new TreeSet<String>(Arrays.asList(attrs));
    }
}
