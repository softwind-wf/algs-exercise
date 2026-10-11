package com.ds.fd;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 不变量（性质）测试：不比对第二份实现，而是断言"算法必须永远满足的性质"。
 *
 * <p>用例来自 {@code src/main/resources/fd/fd-cases.txt}，与闭包演示共用同一批输入。
 * 这些性质比"两份实现结果一致"更强 —— 它不依赖任何另一份实现是否正确：
 *
 * <ol>
 *   <li><b>F ⊆ F⁺</b>：原始 FD 必须都在闭包里；</li>
 *   <li><b>图 8-7 法 ⊆ X⁺ 法</b>：暴力迭代只会漏，不会多算；</li>
 *   <li><b>差的都是平凡依赖</b>：缺口只能是 Y ⊆ X 那种；</li>
 *   <li><b>两种口径等价</b>：{@code X→Y ∈ F⁺} ⟺ {@code Y ⊆ X⁺}；</li>
 *   <li><b>候选键极小且能推出全属性</b>：每个候选键闭包 = 全部属性，去掉任一属性就不行；</li>
 *   <li><b>闭包单调</b>：X ⊆ Z ⟹ X⁺ ⊆ Z⁺。</li>
 * </ol>
 */
class FdInvariantTest {

    private static final Path CASES = Paths.get("src/main/resources/fd/fd-cases.txt");

    private static List<FdSpec.Case> cases;

    @BeforeAll
    static void loadCases() throws IOException {
        cases = FdSpec.load(CASES);
        assertFalse(cases.isEmpty(), "用例文件为空: " + CASES.toAbsolutePath());
    }

    @TestFactory
    @DisplayName("对每个用例断言全部不变量")
    Collection<DynamicTest> invariants() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final FdSpec.Case c : cases) {
            tests.add(DynamicTest.dynamicTest(c.name(), new org.junit.jupiter.api.function.Executable() {
                @Override
                public void execute() {
                    checkInvariants(c);
                }
            }));
        }
        return tests;
    }

    private void checkInvariants(FdSpec.Case c) {
        Set<String> attrs = c.attrs();
        List<FunctionalDependency> fds = c.fds();

        FdCalculator.Result naive = FdCalculator.closureNaive(attrs, fds);
        Set<FunctionalDependency> byAttr = FdCalculator.closureByAttributeClosures(attrs, fds);

        // 1) F ⊆ F⁺
        for (FunctionalDependency fd : fds) {
            assertTrue(naive.fplus().contains(fd), c.name() + ": 图8-7法丢了原始 FD " + fd);
            assertTrue(byAttr.contains(fd), c.name() + ": X⁺法丢了原始 FD " + fd);
        }

        // 2) 图 8-7 法 ⊆ X⁺ 法（暴力迭代只会漏，不会多）
        for (FunctionalDependency fd : naive.fplus()) {
            assertTrue(byAttr.contains(fd), c.name() + ": 图8-7法多算了 " + fd);
        }

        // 3) 缺口只能是平凡依赖
        for (FunctionalDependency fd : byAttr) {
            if (!naive.fplus().contains(fd)) {
                assertTrue(fd.isTrivial(),
                        c.name() + ": 缺口不是平凡依赖 -> " + fd);
            }
        }

        // 4) 两种口径等价：X→Y ∈ F⁺ ⟺ Y ⊆ X⁺
        for (Set<String> x : FdCalculator.subsets(attrs)) {
            Set<String> plus = FdCalculator.attributeClosure(x, fds);
            for (Set<String> y : FdCalculator.subsets(attrs)) {
                FunctionalDependency fd = new FunctionalDependency(x, y);
                boolean inFplus = byAttr.contains(fd);
                boolean subset = plus.containsAll(y);
                assertEquals(subset, inFplus,
                        c.name() + ": 口径不一致 " + fd + " (Y⊆X⁺ = " + subset + ")");
            }
        }

        // 5) 候选键：闭包 = 全属性；去掉任一属性就不再是超键（极小性）
        List<Set<String>> keys = FdCalculator.candidateKeys(attrs, fds);
        for (Set<String> k : keys) {
            assertTrue(FdCalculator.isSuperkey(k, attrs, fds), c.name() + ": 候选键推不出全属性 " + k);
            if (k.size() > 1) {
                for (String a : k) {
                    Set<String> smaller = new java.util.TreeSet<String>(k);
                    smaller.remove(a);
                    assertFalse(FdCalculator.isSuperkey(smaller, attrs, fds),
                            c.name() + ": 候选键非极小 " + k + " (去掉 " + a + " 仍是超键)");
                }
            }
        }

        // 6) 闭包单调：X ⊆ Z ⟹ X⁺ ⊆ Z⁺
        List<Set<String>> subs = FdCalculator.subsets(attrs);
        for (Set<String> x : subs) {
            for (Set<String> z : subs) {
                if (z.containsAll(x)) {
                    assertTrue(FdCalculator.attributeClosure(z, fds)
                                    .containsAll(FdCalculator.attributeClosure(x, fds)),
                            c.name() + ": 闭包单调性被破坏 " + x + " ⊆ " + z);
                }
            }
        }
    }
}
