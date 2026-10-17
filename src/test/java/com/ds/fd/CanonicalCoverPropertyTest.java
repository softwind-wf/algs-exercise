package com.ds.fd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 正则覆盖的<b>性质测试（外部锚点版）</b>，与 {@link CanonicalCoverTest} 互补。
 *
 * <p>{@link CanonicalCoverTest} 里"覆盖不含无关属性"这条是用 {@code CanonicalCover.check()}
 * 自己验自己 —— 如果判据本身写反了，两边会一起错。本类不复用被检验代码的判断逻辑，
 * 外部锚点只有两个，都来自已单独验证过的 {@link FdCalculator}：
 *
 * <ol>
 *   <li><b>等价性</b>：\(F^+ = F_c^+\)。按 {@link FdInvariantTest} 已断言过的口径
 *       "\(X\to Y\in F^+ \iff Y\subseteq X^+\)"，逐个子集比较 \(X^+\) 即可，比枚举 \(F^+\) 便宜；</li>
 *   <li><b>无关属性定义</b>：本类另按教材 8.4.3 的原文独立复算一遍
 *       （左部用<b>完整</b>依赖集判 \(\beta\subseteq\gamma^+\)，右部换成 \(F'\) 判 \(A\in\alpha^+\)）。</li>
 * </ol>
 *
 * <p>在此基础上断言正则覆盖的定义性质与配套不变量：
 * <ol>
 *   <li>不含任何无关属性；</li>
 *   <li>左部互不相同（合并律已生效）；</li>
 *   <li>每条依赖都不可再删 —— 删掉任意一条，\(F^+\) 必须变小（正则覆盖必然无冗余依赖）；</li>
 *   <li>每条都非平凡 —— 右部非空且与左部不相交（平凡部分应被自动剥掉）；</li>
 *   <li>条数不多于原集；且 {@code cover(cover(F)) == cover(F)}（幂等）。</li>
 * </ol>
 *
 * <p>输入分四批：教材手算例、边界例（空集/重复/全平凡/全属性互推）、
 * 用例文件 {@code fd-cases.txt}、固定种子的随机依赖集。
 * 随机轮数默认每个属性池 300 组（常驻跑几乎不增时间）；需要放大时用
 * {@code .\scripts\run-fd-tests.ps1 -CoverRounds 20000}（内部就是传 {@code -Dfd.cover.rounds}，
 * 本机 4 万组约 42 秒）。
 *
 * <p>另有两条用例锁定曾经算错过的两个形状（多字符属性名的拼串歧义、右部为空的退化依赖），
 * 现在作为回归测试保持常亮。
 */
class CanonicalCoverPropertyTest {

    /** 常驻默认轮数；跑全量用 -Dfd.cover.rounds=20000。 */
    private static final int ROUNDS = Integer.getInteger("fd.cover.rounds", 300);
    private static final long SEED = 20261017L;
    private static final Path CASES = Paths.get("src/main/resources/fd/fd-cases.txt");

    // ------------------------------------------------------------------
    // 1. 教材手算例
    // ------------------------------------------------------------------

    /**
     * 教材 8.4 与 8.4.3 的例子。
     *
     * <p>注意 {@code {A→BC, C→B, B→AB}}：网上常见的"答案"{A→BC, B→C, C→B} 其实不等价于 F ——
     * 那份里 B⁺ = BC，推不出 F 中的 B→AB；正确的覆盖必须保留 B→A，本类断言的正是这一形状。
     */
    @Test
    @DisplayName("教材用例：结果符合手算，且满足全部定义性质")
    void textbookCases() {
        assertCoverAndExpect("教材 8.4", fds("A->B", "A->C", "CG->H", "CG->I", "B->H"),
                "A->BC B->H CG->HI");
        assertCoverAndExpect("教材 {AB→CD, A→E, E→C}", fds("AB->CD", "A->E", "E->C"),
                "A->E AB->D E->C");
        assertCoverAndExpect("教材 8.4.3 {A→BC, C→B, B→AB}", fds("A->BC", "C->B", "B->AB"),
                "A->C B->A C->B");
        assertCoverAndExpect("讲义例 {A→BC, B→C, A→B, AB→C}",
                fds("A->BC", "B->C", "A->B", "AB->C"), "A->B B->C");

        // 这两例的正则覆盖形状不唯一，只断言性质不断言具体写法
        assertCoverProperties("互推陷阱 {A→B, B→A, B→C, A→C}", fds("A->B", "B->A", "B->C", "A->C"));
        assertCoverProperties("混合 {AB→C, A→C, C→D, CD→E, A→E}",
                fds("AB->C", "A->C", "C->D", "CD->E", "A->E"));
    }

    // ------------------------------------------------------------------
    // 2. 边界例
    // ------------------------------------------------------------------

    @Test
    @DisplayName("边界用例：空集、重复、平凡、全属性互推都不破坏等价性")
    void edgeCases() {
        assertCoverProperties("空依赖集", new ArrayList<FunctionalDependency>());
        assertCoverAndExpect("重复依赖 A→B ×3", fds("A->B", "A->B", "A->B"), "A->B");
        assertCoverAndExpect("平凡依赖 {AB→A, A→B}", fds("AB->A", "A->B"), "A->B");
        assertCoverAndExpect("全平凡依赖 {A→A, AB→B, ABC→AC}",
                fds("A->A", "AB->B", "ABC->AC"), "");
        assertCoverProperties("全属性互推 {ABCDE→ABCD, ABCD→ABCDE}",
                fds("ABCDE->ABCD", "ABCD->ABCDE"));
        assertCoverAndExpect("链式含冗余尾巴",
                fds("A->B", "B->C", "C->D", "D->E", "A->E", "AB->D", "ACD->E"),
                "A->B B->C C->D D->E");
        assertCoverProperties("多候选键 {A→BCDE, B→ACDE, C→ABDE}",
                fds("A->BCDE", "B->ACDE", "C->ABDE"));
        assertCoverAndExpect("右部含左部 {A→AB, B→C}", fds("A->AB", "B->C"), "A->B B->C");
        assertCoverProperties("空左部(常量依赖) {→A, B→C}", fds("->A", "B->C"));
    }

    // ------------------------------------------------------------------
    // 3. 用例文件
    // ------------------------------------------------------------------

    @TestFactory
    @DisplayName("fd-cases.txt 每个用例都用外部锚点对账")
    Collection<DynamicTest> caseFile() throws IOException {
        List<FdSpec.Case> all = FdSpec.load(CASES);
        assertFalse(all.isEmpty(), "用例文件为空: " + CASES.toAbsolutePath());
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final FdSpec.Case c : all) {
            tests.add(DynamicTest.dynamicTest(c.name(), new Executable() {
                @Override
                public void execute() {
                    assertCoverProperties("cases[" + c.name() + "]", c.fds());
                }
            }));
        }
        return tests;
    }

    // ------------------------------------------------------------------
    // 4. 随机差分
    // ------------------------------------------------------------------

    /**
     * 随机依赖集：属性 5~6 个、FD 最多 10 条，左右都非空但允许平凡/重复，
     * 保证覆盖算法的每条分支（合并、删左部、删右部、右部删空整条删除）都被大量命中。
     */
    @Test
    @DisplayName("随机差分：F⁺ 逐子集相等 + 五条定义性质")
    void randomDifferential() {
        String[] pool5 = {"A", "B", "C", "D", "E"};
        String[] pool6 = {"A", "B", "C", "D", "E", "F"};
        randomSweep(new Random(SEED), pool5, 7, ROUNDS);
        randomSweep(new Random(SEED + 1), pool6, 10, ROUNDS);
    }

    private void randomSweep(Random rnd, String[] pool, int maxFd, int rounds) {
        for (int t = 0; t < rounds; t++) {
            List<FunctionalDependency> f = randomFds(rnd, pool, maxFd);
            assertCoverProperties("随机 t=" + t, f);
        }
    }

    private List<FunctionalDependency> randomFds(Random rnd, String[] pool, int maxFd) {
        List<FunctionalDependency> out = new ArrayList<FunctionalDependency>();
        int n = 1 + rnd.nextInt(maxFd);
        for (int i = 0; i < n; i++) {
            Set<String> lhs = randomSubset(rnd, pool);
            Set<String> rhs = randomSubset(rnd, pool);
            // 合法 FD 要求左右都非空；空集依赖的退化行为另见 emptyRightSideYieldsNoJunk
            if (lhs.isEmpty()) {
                lhs.add(pool[rnd.nextInt(pool.length)]);
            }
            if (rhs.isEmpty()) {
                rhs.add(pool[rnd.nextInt(pool.length)]);
            }
            out.add(new FunctionalDependency(lhs, rhs));
        }
        return out;
    }

    private Set<String> randomSubset(Random rnd, String[] pool) {
        Set<String> s = new TreeSet<String>();
        for (String p : pool) {
            if (rnd.nextBoolean()) {
                s.add(p);
            }
        }
        return s;
    }

    // ------------------------------------------------------------------
    // 5. 核心：一次查完全部性质
    // ------------------------------------------------------------------

    /** 只断言性质，不断言具体写法（正则覆盖不唯一，化简顺序会影响形状）。 */
    private void assertCoverProperties(String tag, List<FunctionalDependency> f) {
        Set<String> attrs = attributesOf(f);
        List<FunctionalDependency> cover = CanonicalCover.cover(f);

        // 性质 0：与 F 等价（外部锚点）
        assertEquivalent(tag + ": F 与 F_c 不等价", f, cover, attrs);

        // 性质 1：不含无关属性（按教材定义独立复算，不调用 CanonicalCover.check）
        for (FunctionalDependency fd : cover) {
            assertFalse(hasExtraneousOutside(fd, cover),
                    tag + ": " + fd + " 仍含无关属性, F_c = " + cover);
        }

        // 性质 2：左部互不相同
        for (int i = 0; i < cover.size(); i++) {
            for (int j = i + 1; j < cover.size(); j++) {
                assertFalse(cover.get(i).determinant().equals(cover.get(j).determinant()),
                        tag + ": 左部重复 " + cover.get(i) + " / " + cover.get(j));
            }
        }

        // 性质 3：每条都不可再删
        for (int i = 0; i < cover.size(); i++) {
            List<FunctionalDependency> minus = new ArrayList<FunctionalDependency>(cover);
            minus.remove(i);
            assertFalse(equivalent(minus, cover, attrs),
                    tag + ": " + cover.get(i) + " 是冗余依赖, F_c = " + cover);
        }

        // 性质 4：每条都非平凡
        for (FunctionalDependency fd : cover) {
            assertFalse(fd.dependent().isEmpty(), tag + ": 右部为空 " + fd);
            assertTrue(Collections.disjoint(fd.determinant(), fd.dependent()),
                    tag + ": 左右相交(平凡部分没剥干净) " + fd);
        }

        // 性质 5：条数不增 + 幂等
        assertTrue(cover.size() <= new LinkedHashSet<FunctionalDependency>(f).size(),
                tag + ": 覆盖反而变多了 " + cover);
        assertEquals(new LinkedHashSet<FunctionalDependency>(cover),
                new LinkedHashSet<FunctionalDependency>(CanonicalCover.cover(cover)),
                tag + ": 不幂等, 第二次化简成了 " + CanonicalCover.cover(cover));
    }

    /** 断言性质之外还比对具体结果（用于手算已确定的教材例）。 */
    private void assertCoverAndExpect(String tag, List<FunctionalDependency> f, String expect) {
        List<FunctionalDependency> cover = CanonicalCover.cover(f);
        assertEquals(plain(expect), describe(cover), tag + ": 覆盖形状不对");
        assertCoverProperties(tag, f);
    }

    /** {@code "A->B B->C"} 与覆盖的打印形式互相比较（属性按字典序拼串，空格分隔各条）。 */
    private String plain(String spec) {
        List<String> items = new ArrayList<String>();
        if (!spec.trim().isEmpty()) {
            for (FunctionalDependency fd : fds(spec.trim().split(" "))) {
                items.add(FdCalculator.join(fd.determinant()) + "->" + FdCalculator.join(fd.dependent()));
            }
        }
        Collections.sort(items);
        return items.toString();
    }

    private String describe(List<FunctionalDependency> fds) {
        List<String> items = new ArrayList<String>();
        for (FunctionalDependency fd : fds) {
            items.add(FdCalculator.join(fd.determinant()) + "->" + FdCalculator.join(fd.dependent()));
        }
        Collections.sort(items);
        return items.toString();
    }

    /**
     * 外部锚点：\(F^+ = G^+\) ⟺ 对每个属性子集 X 都有 \(X^+_F = X^+_G\)。
     * 后一命题的等价性由 {@link FdInvariantTest} 的"两种口径等价"保证。
     */
    private void assertEquivalent(String message, List<FunctionalDependency> f,
                                 List<FunctionalDependency> g, Set<String> attrs) {
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            assertEquals(FdCalculator.attributeClosure(x, f), FdCalculator.attributeClosure(x, g),
                    message + ", X = " + FdCalculator.join(x)
                            + "  F = " + f + "  G = " + g);
        }
    }

    private boolean equivalent(List<FunctionalDependency> f, List<FunctionalDependency> g,
                               Set<String> attrs) {
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            if (!FdCalculator.attributeClosure(x, f).equals(FdCalculator.attributeClosure(x, g))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 按教材 8.4.3 的原文独立判定 target 中是否存在无关属性。
     *
     * <p>左部必须用<b>完整</b>依赖集：例 {@code F = {AB→C, B→A}} 中
     * \(B^+ = BAC \supseteq \{C\}\)，A 确实无关；若把 AB→C 自己从检验集删掉会误判成"不无关"。
     * 右部才需要换成 \(F' = (F-\{\alpha\to\beta\}) \cup \{\alpha\to(\beta-\{A\})\}\)。
     */
    private boolean hasExtraneousOutside(FunctionalDependency target, List<FunctionalDependency> fds) {
        List<FunctionalDependency> others = new ArrayList<FunctionalDependency>(fds);
        others.remove(others.indexOf(target));

        for (String a : target.determinant()) {                                  // 左部
            Set<String> gamma = new TreeSet<String>(target.determinant());
            gamma.remove(a);
            if (FdCalculator.holds(gamma, target.dependent(), fds)) {
                return true;
            }
        }
        for (String a : target.dependent()) {                                    // 右部
            Set<String> beta = new TreeSet<String>(target.dependent());
            beta.remove(a);
            List<FunctionalDependency> fPrime = new ArrayList<FunctionalDependency>(others);
            if (!beta.isEmpty()) {
                fPrime.add(new FunctionalDependency(target.determinant(), beta));
            }
            if (FdCalculator.holds(target.determinant(), Collections.singleton(a), fPrime)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> attributesOf(List<FunctionalDependency> fds) {
        Set<String> all = new TreeSet<String>();
        for (FunctionalDependency fd : fds) {
            all.addAll(fd.determinant());
            all.addAll(fd.dependent());
        }
        return all;
    }

    // ------------------------------------------------------------------
    // 6. 工具
    // ------------------------------------------------------------------

    /** 把 "AB->C" 这样的简写解析成依赖列表（一个字符一个属性）。 */
    private List<FunctionalDependency> fds(String... specs) {
        List<FunctionalDependency> out = new ArrayList<FunctionalDependency>();
        for (String spec : specs) {
            int arrow = spec.indexOf("->");
            out.add(new FunctionalDependency(chars(spec.substring(0, arrow)),
                    chars(spec.substring(arrow + 2))));
        }
        return out;
    }

    private Set<String> chars(String attrs) {
        Set<String> set = new TreeSet<String>();
        for (int i = 0; i < attrs.length(); i++) {
            set.add(String.valueOf(attrs.charAt(i)));
        }
        return set;
    }

    private Set<String> setOf(String... names) {
        return new TreeSet<String>(Arrays.asList(names));
    }

    private List<FunctionalDependency> listOf(FunctionalDependency... fds) {
        return new ArrayList<FunctionalDependency>(Arrays.asList(fds));
    }

    // ------------------------------------------------------------------
    // 7. 回归用例：曾经算错的两个形状（现在应一直绿）
    // ------------------------------------------------------------------

    /**
     * 回归：{@code mergeSameLeft} 早先用 {@code FdCalculator.join(determinant)} 的<b>无分隔符</b>
     * 拼串当分组 key，两个不相等的多字符左部可以拼成同一个 key，于是被当作"左部相同"合并，
     * 合并时只保留第一条的左部 —— 第二条的语义被静默吞掉，结果不再与 F 等价。
     * 现在改成用属性集本身做分组键。
     *
     * <p>历史背景：{@code com.ds.fd} 内部调用全走 {@code of()/chars()}（一字符一属性），
     * 碰不上这个问题；但 {@code new FunctionalDependency(Set, Set)} 是 public API，
     * 教务列名天然多字符，所以必须锁住。
     */
    @Test
    @DisplayName("多字符属性名：左部不得因拼串歧义被错误合并（回归）")
    void multiCharAttributeNamesAreNotConfused() {
        // {ab, c} 与 {a, bc} 是两个不相等的左部, 但按字典序拼串都是 "abc"
        List<FunctionalDependency> f = listOf(
                new FunctionalDependency(setOf("ab", "c"), setOf("grad")),
                new FunctionalDependency(setOf("a", "bc"), setOf("name")));
        List<FunctionalDependency> cover = CanonicalCover.cover(f);
        assertEquals(2, cover.size(), "两个不相等的左部不应被合并: " + cover);
        assertEquals(f, cover, "这条 F 里已无无关属性, 应原样保留");
        assertCoverProperties("多字符拼串歧义", f);

        // 不撞车的那一类教务列名能正常化简: dept_name 在第一条左部无关
        List<FunctionalDependency> real = listOf(
                new FunctionalDependency(setOf("inst_id", "dept_name"), setOf("salary")),
                new FunctionalDependency(setOf("inst_id"), setOf("dept_name", "name")),
                new FunctionalDependency(setOf("dept_name"), setOf("building")));
        assertCoverProperties("教务真实列名", real);
        List<FunctionalDependency> realCover = CanonicalCover.cover(real);
        assertEquals(2, realCover.size(), "应化简成 2 条: " + realCover);
        assertEquals(setOf("inst_id"), realCover.get(0).determinant(), "第一条左部: " + realCover);
        assertEquals(setOf("dept_name", "name", "salary"), realCover.get(0).dependent(),
                "第一条右部: " + realCover);
    }

    /**
     * 回归：右部为空时，{@code CanonicalCover.check()} 里的
     * {@code attributeClosure(gamma, fds).containsAll(target.dependent())} 中
     * {@code containsAll(空集)} <b>恒真</b>，于是左部每个属性都被判成无关并被逐个删除，
     * 最后留下一条 {@code ∅→∅} 的垃圾依赖（空左部又无属性可删，它再也过不掉）。
     * 它不影响等价性，但违反"每条依赖都不可再删"。现在 {@code compute} 入口直接丢弃这类恒真依赖。
     *
     * <p>空左部的"常量依赖"{@code ∅→A} 是另一种形状：它真的约束了属性（每个闭包都会多出 A），
     * 必须保留，不能被上面的过滤误伤。
     */
    @Test
    @DisplayName("右部为空的退化依赖不污染结果，常量依赖仍保留（回归）")
    void emptyRightSideYieldsNoJunk() {
        // X→空集 恒真且不约束任何东西, 应该被丢弃, 结果只余 A→B
        List<FunctionalDependency> f = listOf(
                new FunctionalDependency(chars("AB"), new TreeSet<String>()),
                FunctionalDependency.of("A", "B"));
        assertCoverAndExpect("右部为空", f, "A->B");
        assertTrue(!CanonicalCover.compute(f).steps().isEmpty(), "丢弃动作应在步骤里留痕");

        // 常量依赖 ∅→A 是合法形状, 既不该被丢弃, 也不该被化简成 ∅→∅
        List<FunctionalDependency> g = listOf(
                new FunctionalDependency(new TreeSet<String>(), chars("A")),
                FunctionalDependency.of("B", "C"));
        List<FunctionalDependency> cover = CanonicalCover.cover(g);
        assertTrue(cover.contains(FunctionalDependency.of("", "A")),
                "常量依赖 →A 应被保留: " + cover);
        for (FunctionalDependency fd : cover) {
            assertFalse(fd.dependent().isEmpty(), "不该出现空右部的垃圾依赖: " + cover);
        }
        assertCoverProperties("常量依赖", g);
    }
}
