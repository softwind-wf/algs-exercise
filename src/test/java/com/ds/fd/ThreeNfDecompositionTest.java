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
 * 3NF 分解（教材 8.5.2 节、图 8-12）的回归测试。
 *
 * <p>重点断言：
 * <ol>
 *   <li>dept_advisor 例子：F_c = F，合成 R1=(i_ID,dept_name)、R2=(s_ID,dept_name,i_ID)，
 *       R2 含候选键 ⇒ 不补键；删除冗余 R1 ⇒ 只剩 R2；结果 3NF 但非 BCNF；</li>
 *   <li>class 例子：F 恰好是正则覆盖，合成结果与 BCNF 分解的三张表相同，且恰好也是 BCNF；</li>
 *   <li>没有任何模式含候选键时补键（R(A,B,C,D)、F={A→B,B→C} 得到 AB+BC+AD）；</li>
 *   <li>fd-cases 全部用例：结果每个模式 3NF、整体无损且保持依赖；</li>
 *   <li>与 BCNF 分解的取舍对比：R(A,B,C)、F={AB→C,C→A} 的 BCNF 分解丢 AB→C，
 *       3NF 合成保住依赖（代价是结果仍非 BCNF）。</li>
 * </ol>
 */
class ThreeNfDecompositionTest {

    // ------------------------------------------------------------------
    // 1. dept_advisor：教材 8.5.2 的原例
    // ------------------------------------------------------------------

    @Test
    @DisplayName("dept_advisor：合成 R1=(i_ID,dept_name)、R2=(s_ID,dept_name,i_ID)，删 R1 后只剩 R2（3NF 非 BCNF）")
    void deptAdvisorSynthesis() {
        Set<String> r = attrs("s_ID,i_ID,dept_name");
        List<FunctionalDependency> f = new ArrayList<FunctionalDependency>();
        f.add(fd("i_ID", "dept_name"));
        f.add(fd("s_ID,dept_name", "i_ID"));

        ThreeNfDecomposition.Result result = ThreeNfDecomposition.decompose(r, f);

        // F_c = F
        assertEquals(set(f), new LinkedHashSet<FunctionalDependency>(result.canonicalCover()));
        // 合成两张，最后删掉冗余的一张
        assertEquals(2, result.synthesized().size());
        assertEquals(1, result.decomposition().size());
        assertEquals(attrs("s_ID,dept_name,i_ID"), result.decomposition().get(0));
        assertEquals(3, result.steps().size());
        assertEquals(ThreeNfDecomposition.Step.Kind.REMOVE_REDUNDANT, result.steps().get(2).kind());
        assertEquals(attrs("i_ID,dept_name"), result.steps().get(2).schema());
        assertEquals(attrs("s_ID,dept_name,i_ID"), result.steps().get(2).container());

        // 候选键：{s_ID, i_ID} 与 {s_ID, dept_name}（顺序按候选键排序，不依赖实现细节）
        Set<Set<String>> expectedKeys = new LinkedHashSet<Set<String>>();
        expectedKeys.add(attrs("s_ID,i_ID"));
        expectedKeys.add(attrs("s_ID,dept_name"));
        assertEquals(expectedKeys, new LinkedHashSet<Set<String>>(result.candidateKeys()));

        assertTrue(result.thirdNormalForm(), "dept_advisor 属于 3NF");
        assertFalse(result.bcnf(), "dept_advisor 不属于 BCNF（i_ID -> dept_name）");
        assertTrue(result.lossless());
        assertTrue(result.preservesDependencies(f));

        // 不删冗余时能看到 R1 ⊆ R2
        ThreeNfDecomposition.Result keepRedundant = ThreeNfDecomposition.decompose(r, f, false);
        assertEquals(2, keepRedundant.decomposition().size());
        assertTrue(keepRedundant.decomposition().contains(attrs("i_ID,dept_name")));
        assertTrue(keepRedundant.decomposition().contains(attrs("s_ID,dept_name,i_ID")));
        assertTrue(keepRedundant.thirdNormalForm());
        assertTrue(keepRedundant.lossless());
        assertTrue(keepRedundant.preservesDependencies(f));
    }

    // ------------------------------------------------------------------
    // 2. class：与 BCNF 分解结果相同
    // ------------------------------------------------------------------

    @Test
    @DisplayName("class 例子：F 恰好是正则覆盖，合成结果 = 三张表（course/classroom/section），恰好也是 BCNF")
    void classExampleMatchesTextbook() {
        Set<String> r = attrs("course_id,title,dept_name,credits,sec_id,semester,year,"
                + "building,room_number,capacity,time_slot_id");
        List<FunctionalDependency> f = new ArrayList<FunctionalDependency>();
        f.add(fd("course_id", "title,dept_name,credits"));
        f.add(fd("building,room_number", "capacity"));
        f.add(fd("course_id,sec_id,semester,year", "building,room_number,time_slot_id"));

        ThreeNfDecomposition.Result result = ThreeNfDecomposition.decompose(r, f);

        assertEquals(3, result.synthesized().size());
        assertEquals(3, result.decomposition().size());

        Set<Set<String>> expected = new LinkedHashSet<Set<String>>();
        expected.add(attrs("course_id,title,dept_name,credits"));
        expected.add(attrs("building,room_number,capacity"));
        expected.add(attrs("course_id,sec_id,semester,year,building,room_number,time_slot_id"));
        assertEquals(expected, new LinkedHashSet<Set<String>>(result.decomposition()));

        // 没有模式被删、也没有补键（section 含候选键）
        for (ThreeNfDecomposition.Step step : result.steps()) {
            assertEquals(ThreeNfDecomposition.Step.Kind.SYNTHESIZE, step.kind());
        }
        assertTrue(result.thirdNormalForm());
        assertTrue(result.bcnf(), "教材：这个 3NF 合成结果恰好也是 BCNF");
        assertTrue(result.lossless());
        assertTrue(result.preservesDependencies(f));

        // 与 BCNF 分解走同一条路：得到完全相同的模式集合
        assertEquals(new LinkedHashSet<Set<String>>(BcnfDecomposition.decompositionOf(r, f)),
                new LinkedHashSet<Set<String>>(result.decomposition()));
    }

    // ------------------------------------------------------------------
    // 3. 必须补候选键
    // ------------------------------------------------------------------

    @Test
    @DisplayName("没有模式含候选键时补键：R(A,B,C,D)、F={A->B,B->C} 得到 AB + BC + AD")
    void candidateKeyIsAdded() {
        Set<String> r = chars("ABCD");
        List<FunctionalDependency> f = fds("A->B", "B->C");

        ThreeNfDecomposition.Result result = ThreeNfDecomposition.decompose(r, f);

        // F_c = {A->B, B->C}；合成 AB、BC 都不含候选键 {A,D} ⇒ 补 AD
        assertEquals(2, result.synthesized().size());
        assertEquals(3, result.decomposition().size());
        Set<Set<String>> expected = new LinkedHashSet<Set<String>>();
        expected.add(chars("AB"));
        expected.add(chars("BC"));
        expected.add(chars("AD"));
        assertEquals(expected, new LinkedHashSet<Set<String>>(result.decomposition()));

        assertEquals(3, result.steps().size());
        assertEquals(ThreeNfDecomposition.Step.Kind.ADD_CANDIDATE_KEY, result.steps().get(2).kind());
        assertEquals(chars("AD"), result.steps().get(2).schema());

        assertTrue(result.thirdNormalForm());
        assertTrue(result.lossless(), "补了候选键 AD 才无损");
        assertTrue(result.preservesDependencies(f));
    }

    // ------------------------------------------------------------------
    // 4. 空 F / 全平凡 F：只剩候选键
    // ------------------------------------------------------------------

    @Test
    @DisplayName("空 F 或全平凡 F：没有 FD 可合成，直接补候选键 R 本身")
    void emptyOrTrivialF() {
        List<FunctionalDependency> empty = new ArrayList<FunctionalDependency>();
        ThreeNfDecomposition.Result r1 = ThreeNfDecomposition.decompose(chars("ABC"), empty);
        assertEquals(1, r1.decomposition().size());
        assertEquals(chars("ABC"), r1.decomposition().get(0));
        assertEquals(1, r1.steps().size());
        assertEquals(ThreeNfDecomposition.Step.Kind.ADD_CANDIDATE_KEY, r1.steps().get(0).kind());
        assertTrue(r1.thirdNormalForm());
        assertTrue(r1.lossless());

        // A->A 会被正则覆盖当无关依赖删掉，结果与空 F 相同
        ThreeNfDecomposition.Result r2 = ThreeNfDecomposition.decompose(chars("ABC"), fds("A->A", "AB->A"));
        assertTrue(r2.canonicalCover().isEmpty());
        assertEquals(chars("ABC"), r2.decomposition().get(0));
        assertTrue(r2.thirdNormalForm());
        assertTrue(r2.lossless());
    }

    // ------------------------------------------------------------------
    // 5. 3NF 判定（主属性口径）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("3NF 判定：全部属性都是主属性 ⇒ 3NF；右部含非主属性 ⇒ 不是 3NF")
    void thirdNfCheck() {
        // dept_advisor：三个属性全是主属性，3NF 成立，但不是 BCNF
        Set<String> r = attrs("s_ID,i_ID,dept_name");
        List<FunctionalDependency> f = new ArrayList<FunctionalDependency>();
        f.add(fd("i_ID", "dept_name"));
        f.add(fd("s_ID,dept_name", "i_ID"));

        ThreeNfDecomposition.ThirdNfCheck check = ThreeNfDecomposition.checkThirdNormalForm(r, r, f);
        assertTrue(check.thirdNormalForm());
        assertEquals(attrs("s_ID,i_ID,dept_name"), check.primeAttributes());
        assertTrue(check.violations().isEmpty());
        assertFalse(BcnfCheck.checkRelation(r, f).bcnf());

        // R(A,B,C)、F={A->B}：候选键 AC，B 是非主属性 ⇒ 不是 3NF
        Set<String> r2 = chars("ABC");
        List<FunctionalDependency> f2 = fds("A->B");
        ThreeNfDecomposition.ThirdNfCheck check2 = ThreeNfDecomposition.checkThirdNormalForm(r2, r2, f2);
        assertFalse(check2.thirdNormalForm());
        assertEquals(chars("AC"), check2.candidateKeys().get(0));
        assertEquals(chars("B"), check2.violations().get(0).nonPrime());

        // 单属性模式永远是 3NF（没有任何非平凡 FD）
        assertTrue(ThreeNfDecomposition.isThirdNormalForm(chars("A"), r2, f2));
    }

    // ------------------------------------------------------------------
    // 6. 全部用例的性质
    // ------------------------------------------------------------------

    @Test
    @DisplayName("fd-cases 全部用例：结果每个模式 3NF、整体无损、保持依赖，删掉的模式都被保留的模式包含")
    void propertiesOverAllCases() throws IOException {
        for (FdSpec.Case c : loadCases()) {
            ThreeNfDecomposition.Result result = ThreeNfDecomposition.decompose(c.attrs(), c.fds());
            assertTrue(result.thirdNormalForm(), c.name() + " 不是 3NF");
            assertTrue(result.lossless(), c.name() + " 不是无损分解");
            assertTrue(result.preservesDependencies(c.fds()), c.name() + " 不保持依赖");
            assertEquals(c.attrs(), unionOf(result.decomposition()), c.name() + " 未覆盖 R");
            for (Set<String> part : result.decomposition()) {
                assertFalse(part.isEmpty());
            }
            for (ThreeNfDecomposition.Step step : result.steps()) {
                if (step.kind() == ThreeNfDecomposition.Step.Kind.REMOVE_REDUNDANT) {
                    assertTrue(step.container().containsAll(step.schema()),
                            c.name() + ": 被删模式必须被保留模式包含");
                }
            }

            // 不删冗余的版本同样 3NF + 无损 + 保依赖，只是可能多几块
            ThreeNfDecomposition.Result noRemoval =
                    ThreeNfDecomposition.decompose(c.attrs(), c.fds(), false);
            assertTrue(noRemoval.thirdNormalForm(), c.name() + " (保留冗余) 不是 3NF");
            assertTrue(noRemoval.lossless(), c.name() + " (保留冗余) 不是无损分解");
            assertTrue(noRemoval.preservesDependencies(c.fds()));
            assertEquals(c.attrs(), unionOf(noRemoval.decomposition()));
            assertTrue(noRemoval.decomposition().size() >= result.decomposition().size());
        }
    }

    // ------------------------------------------------------------------
    // 7. 与 BCNF 分解的取舍对比
    // ------------------------------------------------------------------

    /**
     * R(A,B,C)、F={AB→C, C→A}：R 是 3NF 但不属于 BCNF（C→A，A 是主属性）。
     * BCNF 分解 (B,C)+(A,C) 无损，但丢掉 AB→C；
     * 3NF 合成先得到 ABC + AC，删冗余后只剩 ABC —— 保持依赖，代价是仍非 BCNF。
     */
    @Test
    @DisplayName("3NF 保依赖 vs BCNF 丢依赖：R(A,B,C)、F={AB->C,C->A}，3NF 合成返回 R 本身，BCNF 分解丢 AB->C")
    void threeNfKeepsDependencyThatBcnfLoses() {
        Set<String> r = chars("ABC");
        List<FunctionalDependency> f = fds("AB->C", "C->A");

        BcnfDecomposition.Result bcnf = BcnfDecomposition.decompose(r, f);
        assertTrue(bcnf.bcnf());
        assertTrue(bcnf.lossless());
        assertFalse(bcnf.preservesDependencies(f));

        ThreeNfDecomposition.Result threeNf = ThreeNfDecomposition.decompose(r, f);
        assertEquals(2, threeNf.synthesized().size(), "先合成 AB->C 的 ABC 与 C->A 的 AC");
        assertEquals(1, threeNf.decomposition().size(), "AC 被 ABC 包含，删掉后只剩 ABC");
        assertEquals(chars("ABC"), threeNf.decomposition().get(0));

        assertTrue(threeNf.thirdNormalForm());
        assertFalse(threeNf.bcnf(), "结果仍是原来的 R，不属于 BCNF");
        assertTrue(threeNf.lossless());
        assertTrue(threeNf.preservesDependencies(f), "3NF 合成的意义：保住 AB->C");
    }

    // ------------------------------------------------------------------
    // 8. 入参校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("入参校验：空关系模式、F 越过属性集、子模式越过 R 都要报错")
    void validatesArguments() {
        List<FunctionalDependency> f = fds("A->B");
        assertThrows(IllegalArgumentException.class,
                () -> ThreeNfDecomposition.decompose(new TreeSet<String>(), f));
        assertThrows(IllegalArgumentException.class,
                () -> ThreeNfDecomposition.decompose(chars("AB"), fds("A->C")));
        assertThrows(IllegalArgumentException.class,
                () -> ThreeNfDecomposition.checkThirdNormalForm(chars("ABE"), chars("ABC"), f));
        assertThrows(IllegalArgumentException.class,
                () -> ThreeNfDecomposition.checkThirdNormalForm(new TreeSet<String>(), chars("ABC"), f));
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

    /** 逗号分隔的属性集，支持多字符属性名，如 "s_ID,dept_name"。 */
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
