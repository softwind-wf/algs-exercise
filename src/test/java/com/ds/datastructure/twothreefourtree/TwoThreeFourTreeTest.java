package com.ds.datastructure.twothreefourtree;

import cn.exercise.algs4.datastructure.twothreefourtree.TwoThreeFourTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2-3-4 树（自顶向下插入）JUnit 5 单元测试
 *
 * @author kevin
 * @version 1.0
 */
@DisplayName("TwoThreeFourTree 测试")
class TwoThreeFourTreeTest {

    private TwoThreeFourTree tree;

    @BeforeEach
    void setUp() {
        tree = new TwoThreeFourTree();
    }

    // ==================== 辅助方法 ====================

    /**
     * 校验 2-3-4 树的结构不变量，任何违反都让测试失败
     */
    private void assertValid(TwoThreeFourTree t) {
        String err = t.checkStructure();
        assertNull(err, () -> "结构不变量被违反：" + err);
    }

    /**
     * 校验树高不超过全 2-节点（最坏情况）的理论上界 ceil(log2(n+1))
     */
    private void assertHeightBound(int n) {
        double max = Math.ceil(Math.log(n + 1) / Math.log(2));
        assertTrue(tree.height() <= max,
                "树高 " + tree.height() + " 超过上界 " + max + "（n=" + n + "）");
    }

    // ==================== 基础操作 ====================

    @Nested
    @DisplayName("基础操作")
    class BasicTests {

        @Test
        @DisplayName("空树")
        void emptyTree() {
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertNull(tree.findMin());
            assertNull(tree.findMax());
            assertTrue(tree.inorder().isEmpty());
            assertTrue(tree.levelOrder().isEmpty());
        }

        @Test
        @DisplayName("插入单个元素")
        void addSingle() {
            assertTrue(tree.add(10));
            assertEquals(1, tree.size());
            assertTrue(tree.contains(10));
            assertEquals(10, tree.get(10));
            assertEquals(0, tree.height());
            assertValid(tree);
        }

        @Test
        @DisplayName("重复键返回 false")
        void addDuplicate() {
            assertTrue(tree.add(10));
            assertFalse(tree.add(10));
            assertEquals(1, tree.size());
            assertValid(tree);
        }

        @Test
        @DisplayName("重复键堆叠：全插同一个值")
        void addSameValueRepeatedly() {
            assertTrue(tree.add(7));
            for (int i = 1; i < 50; i++) {
                assertFalse(tree.add(7));
            }
            assertEquals(1, tree.size());
            assertValid(tree);
        }

        @Test
        @DisplayName("contains 不存在的键返回 false，get 返回 null")
        void getNotExists() {
            tree.add(10);
            assertFalse(tree.contains(99));
            assertNull(tree.get(99));
        }
    }

    // ==================== 平衡性与结构 ====================

    @Nested
    @DisplayName("平衡性与结构")
    class BalanceTests {

        @Test
        @DisplayName("连续递增插入 1000 个元素，仍保持 2-3-4 结构")
        void sequentialInserts() {
            for (int i = 1; i <= 1000; i++) {
                tree.add(i);
            }
            assertEquals(1000, tree.size());
            assertValid(tree);
            List<Integer> expected = new ArrayList<>();
            for (int i = 1; i <= 1000; i++) {
                expected.add(i);
            }
            assertEquals(expected, tree.inorder());
            assertHeightBound(1000);
        }

        @Test
        @DisplayName("连续递减插入 1000 个元素")
        void reverseSequentialInserts() {
            for (int i = 1000; i >= 1; i--) {
                tree.add(i);
            }
            assertEquals(1000, tree.size());
            assertValid(tree);
            assertEquals(1, (int) tree.findMin());
            assertEquals(1000, (int) tree.findMax());
            assertHeightBound(1000);
        }

        @Test
        @DisplayName("随机插入（含重复）仍保持结构")
        void randomInserts() {
            Random rand = new Random(42);
            Set<Integer> inserted = new HashSet<>();
            for (int i = 0; i < 2000; i++) {
                int val = rand.nextInt(100000);
                tree.add(val);
                inserted.add(val);
            }
            assertEquals(inserted.size(), tree.size());
            assertValid(tree);

            List<Integer> expected = new ArrayList<>(inserted);
            Collections.sort(expected);
            assertEquals(expected, tree.inorder());
            assertHeightBound(inserted.size());
        }

        @Test
        @DisplayName("根分裂：插入 1..8 后根成为 4-节点，插入 9 触发根分裂增高")
        void rootSplitGrowsTree() {
            for (int i = 1; i <= 8; i++) {
                tree.add(i);
            }
            // 此时根为 [2,4,6]，叶子同层深度 1
            assertEquals(1, tree.height());
            assertValid(tree);

            tree.add(9);
            // 根分裂，树增高一层
            assertEquals(2, tree.height());
            assertEquals(9, tree.size());
            assertValid(tree);
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), tree.inorder());
        }

        @Test
        @DisplayName("分裂后上浮的键恰好是重复键，应被正确拒绝")
        void duplicatePromotedBySplit() {
            for (int i = 1; i <= 7; i++) {
                tree.add(i);
            }
            // 此时树为 [2,4] -> [1],[3],[5,6,7]，插入 6 会使 6 在分裂中上浮进根
            assertFalse(tree.add(6));
            assertEquals(7, tree.size());
            assertValid(tree);
        }

        @Test
        @DisplayName("负数和零正常处理")
        void negativeAndZero() {
            tree.add(0);
            tree.add(-5);
            tree.add(3);
            tree.add(-10);
            assertEquals(Arrays.asList(-10, -5, 0, 3), tree.inorder());
            assertValid(tree);
        }
    }

    // ==================== 统计与遍历 ====================

    @Nested
    @DisplayName("统计与遍历")
    class StatsAndTraversalTests {

        @Test
        @DisplayName("findMin / findMax")
        void findMinMax() {
            tree.add(5);
            tree.add(3);
            tree.add(8);
            tree.add(1);
            tree.add(9);
            assertEquals(1, (int) tree.findMin());
            assertEquals(9, (int) tree.findMax());
        }

        @Test
        @DisplayName("中序遍历结果为升序")
        void inorderSorted() {
            tree.add(5);
            tree.add(3);
            tree.add(7);
            tree.add(1);
            tree.add(4);
            assertEquals(Arrays.asList(1, 3, 4, 5, 7), tree.inorder());
        }

        @Test
        @DisplayName("层序遍历包含全部键")
        void levelOrderContainsAll() {
            for (int i = 1; i <= 20; i++) {
                tree.add(i);
            }
            List<Integer> level = tree.levelOrder();
            assertEquals(20, level.size());
            List<Integer> sorted = new ArrayList<>(level);
            Collections.sort(sorted);
            assertEquals(tree.inorder(), sorted);
        }

        @Test
        @DisplayName("toArray / toList / toString")
        void conversions() {
            tree.add(3);
            tree.add(1);
            tree.add(2);
            assertArrayEquals(new int[]{1, 2, 3}, tree.toArray());
            assertEquals(Arrays.asList(1, 2, 3), tree.toList());
            assertEquals("[1, 2, 3]", tree.toString());
        }

        @Test
        @DisplayName("clear 后恢复空树")
        void clear() {
            tree.add(1);
            tree.add(2);
            tree.add(3);
            tree.clear();
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
        }
    }

    // ==================== 删除 ====================

    @Nested
    @DisplayName("删除")
    class RemoveTests {

        @Test
        @DisplayName("空树删除返回 false")
        void removeFromEmpty() {
            assertFalse(tree.remove(1));
            assertValid(tree);
        }

        @Test
        @DisplayName("删除不存在的键返回 false，树保持不变")
        void removeMissing() {
            for (int i = 1; i <= 10; i++) {
                tree.add(i);
            }
            assertFalse(tree.remove(99));
            assertEquals(10, tree.size());
            assertValid(tree);
        }

        @Test
        @DisplayName("删除唯一键后回到空树")
        void removeAllSingle() {
            tree.add(5);
            assertTrue(tree.remove(5));
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertValid(tree);
        }

        @Test
        @DisplayName("从小到大依次删光，覆盖借键/合并/内部替换各分支")
        void removeSequentialAll() {
            for (int i = 1; i <= 20; i++) {
                tree.add(i);
            }
            for (int i = 1; i <= 20; i++) {
                assertTrue(tree.remove(i), "删除 " + i + " 应成功");
                assertEquals(20 - i, tree.size());
                assertValid(tree);
            }
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("从内部节点删除：前驱替换、后继替换、两侧 2-节点合并")
        void removeFromInternal() {
            // 1..8 后根为 [2,4,6]，叶子 [1],[3],[5],[7,8]
            for (int i = 1; i <= 8; i++) {
                tree.add(i);
            }
            assertEquals(1, tree.height());
            assertValid(tree);
            // 删除根键 2：两侧孩子 [1]、[3] 都是 2-节点 → 合并后删除
            assertTrue(tree.remove(2));
            assertEquals(Arrays.asList(1, 3, 4, 5, 6, 7, 8), tree.inorder());
            assertValid(tree);
            // 删除根键 6：左孩子 [5] 是 2-节点、右孩子 [7,8] 有富余 → 后继替换
            assertTrue(tree.remove(6));
            assertEquals(Arrays.asList(1, 3, 4, 5, 7, 8), tree.inorder());
            assertValid(tree);
        }

        @Test
        @DisplayName("随机键全部删光")
        void removeAllRandom() {
            Random rand = new Random(7);
            List<Integer> keys = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                int v = rand.nextInt(1000);
                if (!keys.contains(v)) {
                    keys.add(v);
                }
            }
            for (int v : keys) {
                tree.add(v);
            }
            List<Integer> shuffled = new ArrayList<>(keys);
            Collections.shuffle(shuffled, new Random(99));
            for (int v : shuffled) {
                assertTrue(tree.remove(v), "删除 " + v + " 应成功");
                assertValid(tree);
            }
            assertTrue(tree.isEmpty());
            assertValid(tree);
        }

        @Test
        @DisplayName("随机删除一半后与 TreeSet 对照")
        void removeHalfMatchesTreeSet() {
            Random rand = new Random(123);
            Set<Integer> ref = new HashSet<>();
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                int v = rand.nextInt(10000);
                if (ref.add(v)) {
                    order.add(v);
                }
            }
            for (int v : order) {
                tree.add(v);
            }
            List<Integer> del = new ArrayList<>(order);
            Collections.shuffle(del, new Random(5));
            int cnt = 0;
            for (int v : del) {
                if (cnt++ % 2 == 0) {
                    assertTrue(tree.remove(v));
                    ref.remove(v);
                    assertValid(tree);
                }
            }
            List<Integer> expected = new ArrayList<>(ref);
            Collections.sort(expected);
            assertEquals(expected, tree.inorder());
            assertEquals(ref.size(), tree.size());
        }
    }
}
