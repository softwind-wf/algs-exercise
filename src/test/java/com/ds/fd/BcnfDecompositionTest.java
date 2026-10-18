package com.ds.fd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BCNF 分解算法（教材 8.5.1.2 节、图 8-11）的回归测试。
 *
 * <p>重点断言：
 * <ol>
 *   <li>教材 class 例子拆出的三张表（课程信息 / 开课信息 / 教室容量），且保持依赖；</li>
 *   <li>每一步都满足 α∩β=∅、两块都是 Ri 的真子集、两块并起来还是 Ri；</li>
 *   <li>最终结果一定 BCNF + 无损（chase 复核），但<b>不一定保持依赖</b>
 *       （R(A,B,C)、F={AB→C, C→A} 拆成 (A,C)+(B,C) 就是反例）；</li>
 *   <li>闭包版 {@code decompose} 与"先物化 F⁺"的 {@code decomposeByFPlus} 都产出合法分解。</li>
 * </ol>
 */
class BcnfDecompositionTest {

    // ------------------------------------------------------------------
    // 1. 教材 class 例子
    // ------------------------------------------------------------------

    @Test
    @DisplayName("教材 class 例子：拆成 (course_id,title,dept_name,credits) + (course_id,sec_id,semester,year,building,room_number,time_slot_id) + (building,room_number,capacity)")
    void textbookClassExample() {
        Set<String> r = attrs("course_id,title,dept_name,credits,sec_id,semester,year,"
                + "building,room_number,capacity,time_slot_id");
        List<FunctionalDependency> f = new ArrayList<FunctionalDependency>();
        f.add(fd("course_id", "title,dept_name,credits"));
        f.add(fd("building,room_number", "capacity"));
        f.add(fd("course_id,sec_id,semester,year", "building,room_number,time_slot_id"));

        BcnfDecomposition.Result result = BcnfDecomposition.decompose(r, f);
        assertEquals(2, result.stepCount());
        assertEquals(3, result.decomposition().size());

        Set<Set<String>> expected = new LinkedHashSet<Set<String>>();
        expected.add(attrs("course_id,title,dept_name,credits"));
        expected.add(attrs("course_id,sec_id,semester,year,building,room_number,time_slot_id"));
        expected.add(attrs("building,room_number,capacity"));
        assertEquals(expected, new LinkedHashSet<Set<String>>(result.decomposition()));

        assertTrue(result.bcnf());
        assertTrue(result.lossless());
        assertTrue(result.preservesDependencies(f), "这三张表把 F 的每条依赖都保住了");

        // 判定面：这张宽表为什么不满足 BCNF —— 两条违反项 + 唯一候选键
        BcnfCheck.CheckResult tableCheck = BcnfCheck.checkRelation(r, f);
        assertFalse(tableCheck.bcnf());
        assertEquals(2, tableCheck.violations().size());
        assertEquals(fd("course_id", "title,dept_name,credits"), tableCheck.violations().get(0).fd());
        assertEquals(fd("building,room_number", "capacity"), tableCheck.violations().get(1).fd());

        List<Set<String>> keys = FdCalculator.candidateKeys(r, f);
        assertEquals(1, keys.size(), "class 宽表只有一个候选键");
        assertEquals(attrs("course_id,sec_id,semester,year"), keys.get(0));

        // 第 1 步：class 上 course_id -> title,dept_name,credits
        BcnfDecomposition.Step first = result.steps().get(0);
        assertEquals(fd("course_id", "title,dept_name,credits"), first.fd());
        assertTrue(first.inGivenSet());
        assertEquals(attrs("course_id,title,dept_name,credits"), first.closure());
        assertEquals(attrs("course_id,title,dept_name,credits"), first.right());
        assertEquals(attrs("course_id,sec_id,semester,year,building,room_number,capacity,time_slot_id"),
                first.left());
        assertEquals(attrs("sec_id,semester,year,building,room_number,capacity,time_slot_id"),
                first.missing());

        // 第 2 步：剩下的开课表上 building,room_number -> capacity
        BcnfDecomposition.Step second = result.steps().get(1);
        assertEquals(fd("building,room_number", "capacity"), second.fd());
        assertEquals(attrs("building,room_number,capacity"), second.right());
        assertEquals(attrs("course_id,sec_id,semester,year,building,room_number,time_slot_id"),
                second.left());

        // 同一输入重复跑，结果必须完全一致
        assertEquals(new ArrayList<Set<String>>(result.decomposition()),
                new ArrayList<Set<String>>(BcnfDecomposition.decompose(r, f).decomposition()));
    }

    // ------------------------------------------------------------------
    // 2. 无损但丢依赖
    // ------------------------------------------------------------------

    /**
     * R(A,B,C)、F={AB→C, C→A}：C→A 违反 BCNF，拆成 (A,C)+(B,C)。
     * 公共属性 C 决定 A ⇒ 无损；但 AB→C 跨两张表 ⇒ 不保持依赖。
     */
    @Test
    @DisplayName("BCNF 分解可以无损但丢依赖：R(A,B,C)、F={AB->C, C->A} 拆成 (A,C)+(B,C)")
    void losslessButNotDependencyPreserving() {
        Set<String> r = chars("ABC");
        List<FunctionalDependency> f = fds("AB->C", "C->A");

        BcnfDecomposition.Result result = BcnfDecomposition.decompose(r, f);
        assertEquals(1, result.stepCount());

        Set<Set<String>> expected = new LinkedHashSet<Set<String>>();
        expected.add(chars("AC"));
        expected.add(chars("BC"));
        assertEquals(expected, new LinkedHashSet<Set<String>>(result.decomposition()));

        assertTrue(result.bcnf());
        assertTrue(result.lossless());
        assertFalse(result.preservesDependencies(f), "AB->C 跨在两块之间，检查不出来了");
        assertEquals(set(fds("AB->C")),
                DependencyPreservation.checkByModifiedClosure(r, result.decomposition(), f).lost());

        assertEquals(FunctionalDependency.of("C", "A"), result.steps().get(0).fd());
        assertTrue(result.steps().get(0).inGivenSet());
    }

    // ------------------------------------------------------------------
    // 3. 本来就已经是 BCNF
    // ------------------------------------------------------------------

    @Test
    @DisplayName("R 已属于 BCNF 时不做任何拆分")
    void alreadyBcnf() {
        Set<String> r = chars("AB");
        List<FunctionalDependency> f = fds("A->B");

        BcnfDecomposition.Result result = BcnfDecomposition.decompose(r, f);
        assertEquals(0, result.stepCount());
        assertEquals(1, result.decomposition().size());
        assertEquals(chars("AB"), result.decomposition().get(0));
        assertTrue(result.bcnf());
        assertTrue(result.lossless());
        assertEquals(Collections.singletonList(chars("AB")), BcnfDecomposition.decompositionOf(r, f));
    }

    // ------------------------------------------------------------------
    // 4. 非唯一性：选不同违反项，拆法不同但都合法
    // ------------------------------------------------------------------

    /**
     * inst_dept 形状：R(A,...,F)、F={A→BCD, D→EF}。A 通过 A→D、D→EF 传递出全部属性，
     * 所以 A 已是超键、不构成违反；第一条违反项是 D→EF，拆出
     * (A,B,C,D)+(D,E,F) —— 与教材 8.3.2 对 inst_dept 的分解一致。
     * 注意算法对"选哪条违反依赖"没有规定（这里若选 BD→EF 会得到另一种合法分解），
     * 但无论怎么选，结果都必须是 BCNF + 无损。
     */
    @Test
    @DisplayName("inst_dept 形状：本实现命中 D->E,F，拆出 (A,B,C,D)+(D,E,F)，与教材 8.3.2 一致")
    void instDeptShapeMatchesTextbook() {
        Set<String> r = chars("ABCDEF");
        List<FunctionalDependency> f = fds("A->BCD", "D->EF");

        BcnfDecomposition.Result result = BcnfDecomposition.decompose(r, f);
        assertEquals(1, result.stepCount());
        assertEquals(2, result.decomposition().size());

        Set<Set<String>> expected = new LinkedHashSet<Set<String>>();
        expected.add(chars("ABCD"));
        expected.add(chars("DEF"));
        assertEquals(expected, new LinkedHashSet<Set<String>>(result.decomposition()));

        assertEquals(FunctionalDependency.of("D", "EF"), result.steps().get(0).fd());
        assertTrue(result.bcnf());
        assertTrue(result.lossless());
    }

    // ------------------------------------------------------------------
    // 5. 性质：拆分不变量（所有用例）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("所有 fd-cases 用例：结果 BCNF + 无损，每步 α∩β=∅、两块是 Ri 的真子集且并集仍为 Ri")
    void splitInvariants() throws IOException {
        for (FdSpec.Case c : loadCases()) {
            BcnfDecomposition.Result result = BcnfDecomposition.decompose(c.attrs(), c.fds());
            assertTrue(result.bcnf(), c.name() + " 最终结果不是 BCNF");
            assertTrue(result.lossless(), c.name() + " 最终结果不是无损分解");
            assertEquals(c.attrs(), unionOf(result.decomposition()), c.name() + " 未覆盖 R");

            for (BcnfDecomposition.Step step : result.steps()) {
                assertTrue(Collections.disjoint(step.fd().determinant(), step.fd().dependent()),
                        c.name() + ": " + step.fd() + " 不满足 α∩β=∅");
                assertTrue(step.relation().containsAll(step.fd().determinant()));
                assertTrue(step.relation().containsAll(step.fd().dependent()));
                assertEquals(step.relation(), unionOf(java.util.Arrays.asList(step.left(), step.right())),
                        c.name() + ": 拆完两块并起来必须还是 Ri");
                assertTrue(step.left().size() < step.relation().size(), "Ri−β 必须是真子集");
                assertTrue(step.right().size() < step.relation().size(), "αβ 必须是真子集（否则 α 是超键）");
            }
        }
    }

    // ------------------------------------------------------------------
    // 6. 图 8-11 字面读法（先物化 F⁺）与闭包版都合法
    // ------------------------------------------------------------------

    @Test
    @DisplayName("字面计算 F+ 的版本（decomposeByFPlus）在所有用例上同样产出 BCNF + 无损分解")
    void byFPlusVersionAlsoValid() throws IOException {
        for (FdSpec.Case c : loadCases()) {
            BcnfDecomposition.Result result = BcnfDecomposition.decomposeByFPlus(c.attrs(), c.fds());
            assertTrue(result.bcnf(), c.name() + " (F+ 版) 不是 BCNF");
            assertTrue(result.lossless(), c.name() + " (F+ 版) 不是无损分解");
            assertEquals(c.attrs(), unionOf(result.decomposition()), c.name() + " (F+ 版) 未覆盖 R");
            for (BcnfDecomposition.Step step : result.steps()) {
                assertTrue(Collections.disjoint(step.fd().determinant(), step.fd().dependent()));
            }
        }
    }

    // ------------------------------------------------------------------
    // 7. 入参校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("入参校验：空关系模式、F 越过属性集都要报错")
    void validatesArguments() {
        List<FunctionalDependency> f = fds("A->B");
        assertThrows(IllegalArgumentException.class,
                () -> BcnfDecomposition.decompose(new TreeSet<String>(), f));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfDecomposition.decompose(chars("AB"), fds("A->C")));
        assertThrows(IllegalArgumentException.class,
                () -> BcnfDecomposition.decomposeByFPlus(chars("AB"), fds("A->C")));
    }

    // ------------------------------------------------------------------

    private static List<FdSpec.Case> loadCases() throws IOException {
        Path cases = Paths.get("src/main/resources/fd/fd-cases.txt");
        List<FdSpec.Case> all = FdSpec.load(cases);
        assertFalse(all.isEmpty());
        return all;
    }

    private static Set<String> unionOf(List<Set<String>> parts) {
        Set<String> union = new TreeSet<String>();
        for (Set<String> part : parts) {
            union.addAll(part);
        }
        return union;
    }

    /** 单字母属性集，如 "ABC"。 */
    private static Set<String> chars(String spec) {
        return FunctionalDependency.chars(spec);
    }

    /** 逗号分隔的属性集，支持多字符属性名，如 "course_id,title"。 */
    private static Set<String> attrs(String spec) {
        Set<String> out = new LinkedHashSet<String>();
        for (String token : spec.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static FunctionalDependency fd(String lhs, String rhs) {
        return new FunctionalDependency(attrs(lhs), attrs(rhs));
    }

    /** "AB->C" 这样的单字母简写。 */
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
}
