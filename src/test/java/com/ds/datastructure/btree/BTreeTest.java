package com.ds.datastructure.btree;

import cn.exercise.algs4.datastructure.btree.BTree;
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
 * B 树 JUnit 5 单元测试
 *
 * @author kevin
 * @version 1.0
 */
@DisplayName("BTree 测试")
class BTreeTest {

    // ==================== 辅助方法 ====================

    /**
     * 校验 B 树的结构不变量，任何违反都让测试失败
     */
    private void assertValid(BTree tree) {
        String err = tree.checkStructure();
        assertNull(err, () -> "结构不变量被违反：" + err);
    }

    /**
     * 校验树高不超过理论上界 log_t(n+1)（向上取整），对任意最小度数 t 成立
     */
    private void assertHeightBound(BTree tree, int t, int n) {
        if (n == 0) {
            return;
        }
        double max = Math.ceil(Math.log(n + 1) / Math.log(t));
        assertTrue(tree.height() <= max,
                "树高 " + tree.height() + " 超过上界 " + max + "（t=" + t + ", n=" + n + "）");
    }

    // ==================== 构造函数 ====================

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造函数创建 t=2 的空树")
        void defaultConstructor() {
            BTree tree = new BTree();
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("t=2 构造正常")
        void constructT2() {
            BTree tree = new BTree(2);
            assertTrue(tree.isEmpty());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("t=3 构造正常")
        void constructT3() {
            BTree tree = new BTree(3);
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("t=5 构造正常")
        void constructT5() {
            BTree tree = new BTree(5);
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("t<2 抛出 IllegalArgumentException")
        void constructInvalidT() {
            assertThrows(IllegalArgumentException.class, () -> new BTree(1));
            assertThrows(IllegalArgumentException.class, () -> new BTree(0));
            assertThrows(IllegalArgumentException.class, () -> new BTree(-1));
        }
    }

    // ==================== 基础操作 ====================

    @Nested
    @DisplayName("基础操作")
    class BasicTests {

        @Test
        @DisplayName("空树属性")
        void emptyTree() {
            BTree tree = new BTree();
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertNull(tree.findMin());
            assertNull(tree.findMax());
            assertNull(tree.get(0));
            assertFalse(tree.contains(0));
            assertTrue(tree.inorder().isEmpty());
            assertTrue(tree.levelOrder().isEmpty());
            assertValid(tree);
        }

        @Test
        @DisplayName("插入单个元素")
        void insertSingle() {
            BTree tree = new BTree();
            assertTrue(tree.insert(10));
            assertEquals(1, tree.size());
            assertFalse(tree.isEmpty());
            assertEquals(0, tree.height());
            assertTrue(tree.contains(10));
            assertEquals(10, tree.get(10));
            assertEquals(10, (int) tree.findMin());
            assertEquals(10, (int) tree.findMax());
            assertValid(tree);
        }

        @Test
        @DisplayName("重复键返回 false，size 不变")
        void insertDuplicate() {
            BTree tree = new BTree();
            assertTrue(tree.insert(10));
            assertFalse(tree.insert(10));
            assertEquals(1, tree.size());
            assertValid(tree);
        }

        @Test
        @DisplayName("重复键堆叠：全插同一个值")
        void insertSameValueRepeatedly() {
            BTree tree = new BTree();
            assertTrue(tree.insert(7));
            for (int i = 1; i < 50; i++) {
                assertFalse(tree.insert(7));
            }
            assertEquals(1, tree.size());
            assertValid(tree);
        }

        @Test
        @DisplayName("contains/get 不存在的键")
        void getNotExists() {
            BTree tree = new BTree();
            tree.insert(10);
            assertFalse(tree.contains(99));
            assertNull(tree.get(99));
        }

        @Test
        @DisplayName("负数和零正常处理")
        void negativeAndZero() {
            BTree tree = new BTree();
            tree.insert(0);
            tree.insert(-5);
            tree.insert(3);
            tree.insert(-10);
            assertEquals(Arrays.asList(-10, -5, 0, 3), tree.inorder());
            assertEquals(-10, (int) tree.findMin());
            assertEquals(3, (int) tree.findMax());
            assertValid(tree);
        }
    }

    // ==================== 不同度数 t 的插入与结构 ====================

    @Nested
    @DisplayName("不同度数 t 的插入与结构")
    class DegreeTests {

        @Test
        @DisplayName("t=2 连续递增插入 500 个元素，保持结构合法")
        void sequentialInsertsT2() { assertSequentialInserts(2, 500); }

        @Test
        @DisplayName("t=3 连续递增插入 500 个元素，保持结构合法")
        void sequentialInsertsT3() { assertSequentialInserts(3, 500); }

        @Test
        @DisplayName("t=4 连续递增插入 500 个元素，保持结构合法")
        void sequentialInsertsT4() { assertSequentialInserts(4, 500); }

        @Test
        @DisplayName("t=5 连续递增插入 500 个元素，保持结构合法")
        void sequentialInsertsT5() { assertSequentialInserts(5, 500); }

        private void assertSequentialInserts(int t, int n) {
            BTree tree = new BTree(t);
            for (int i = 1; i <= n; i++) {
                tree.insert(i);
            }
            assertEquals(n, tree.size());
            assertValid(tree);
            List<Integer> expected = new ArrayList<>();
            for (int i = 1; i <= n; i++) {
                expected.add(i);
            }
            assertEquals(expected, tree.inorder());
            assertHeightBound(tree, t, n);
        }

        @Test
        @DisplayName("t=2 连续递减插入 500 个元素")
        void reverseSequentialInsertsT2() { assertReverseSequentialInserts(2, 500); }

        @Test
        @DisplayName("t=3 连续递减插入 500 个元素")
        void reverseSequentialInsertsT3() { assertReverseSequentialInserts(3, 500); }

        @Test
        @DisplayName("t=4 连续递减插入 500 个元素")
        void reverseSequentialInsertsT4() { assertReverseSequentialInserts(4, 500); }

        @Test
        @DisplayName("t=5 连续递减插入 500 个元素")
        void reverseSequentialInsertsT5() { assertReverseSequentialInserts(5, 500); }

        private void assertReverseSequentialInserts(int t, int n) {
            BTree tree = new BTree(t);
            for (int i = n; i >= 1; i--) {
                tree.insert(i);
            }
            assertEquals(n, tree.size());
            assertValid(tree);
            assertEquals(1, (int) tree.findMin());
            assertEquals(n, (int) tree.findMax());
            assertHeightBound(tree, t, n);
        }

        @Test
        @DisplayName("t=2 随机插入 1000 个元素仍保持结构")
        void randomInsertsT2() { assertRandomInserts(2, 1000); }

        @Test
        @DisplayName("t=3 随机插入 1000 个元素仍保持结构")
        void randomInsertsT3() { assertRandomInserts(3, 1000); }

        @Test
        @DisplayName("t=4 随机插入 1000 个元素仍保持结构")
        void randomInsertsT4() { assertRandomInserts(4, 1000); }

        private void assertRandomInserts(int t, int n) {
            BTree tree = new BTree(t);
            Random rand = new Random(42);
            Set<Integer> inserted = new HashSet<>();
            for (int i = 0; i < n; i++) {
                int val = rand.nextInt(100_000);
                tree.insert(val);
                inserted.add(val);
            }
            assertEquals(inserted.size(), tree.size());
            assertValid(tree);

            List<Integer> expected = new ArrayList<>(inserted);
            Collections.sort(expected);
            assertEquals(expected, tree.inorder());
            assertHeightBound(tree, t, inserted.size());
        }

        @Test
        @DisplayName("t=2 根分裂：插入 1..8 后根为 4-节点，插入 9 触发根分裂增高")
        void rootSplitT2() {
            BTree tree = new BTree(2);
            for (int i = 1; i <= 8; i++) {
                tree.insert(i);
            }
            assertEquals(1, tree.height());
            assertValid(tree);

            tree.insert(9);
            assertEquals(2, tree.height());
            assertEquals(9, tree.size());
            assertValid(tree);
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), tree.inorder());
        }

        @Test
        @DisplayName("t=3 插入触发分裂：容量为 5，插入 1..6 后触发分裂")
        void rootSplitT3() {
            BTree tree = new BTree(3);
            // t=3: maxKeys = 5, 插入 1..5 后根满，插入 6 触发根分裂
            for (int i = 1; i <= 5; i++) {
                tree.insert(i);
            }
            assertEquals(0, tree.height());
            assertValid(tree);

            tree.insert(6);
            assertEquals(1, tree.height());
            assertEquals(6, tree.size());
            assertValid(tree);
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), tree.inorder());
        }

        @Test
        @DisplayName("分裂后上浮的键恰好是重复键，应被正确拒绝")
        void duplicatePromotedBySplit() {
            BTree tree = new BTree(2);
            for (int i = 1; i <= 7; i++) {
                tree.insert(i);
            }
            // 此时树已有键 6，再插入 6 应被拒绝
            assertFalse(tree.insert(6));
            assertEquals(7, tree.size());
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
            BTree tree = new BTree();
            tree.insert(5);
            tree.insert(3);
            tree.insert(8);
            tree.insert(1);
            tree.insert(9);
            assertEquals(1, (int) tree.findMin());
            assertEquals(9, (int) tree.findMax());
        }

        @Test
        @DisplayName("中序遍历结果为升序")
        void inorderSorted() {
            BTree tree = new BTree();
            tree.insert(5);
            tree.insert(3);
            tree.insert(7);
            tree.insert(1);
            tree.insert(4);
            assertEquals(Arrays.asList(1, 3, 4, 5, 7), tree.inorder());
        }

        @Test
        @DisplayName("层序遍历包含全部键")
        void levelOrderContainsAll() {
            BTree tree = new BTree();
            for (int i = 1; i <= 20; i++) {
                tree.insert(i);
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
            BTree tree = new BTree();
            tree.insert(3);
            tree.insert(1);
            tree.insert(2);
            assertArrayEquals(new int[]{1, 2, 3}, tree.toArray());
            assertEquals(Arrays.asList(1, 2, 3), tree.toList());
            assertEquals("[1, 2, 3]", tree.toString());
        }

        @Test
        @DisplayName("clear 后恢复空树")
        void clear() {
            BTree tree = new BTree();
            tree.insert(1);
            tree.insert(2);
            tree.insert(3);
            tree.clear();
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertNull(tree.findMin());
            assertNull(tree.findMax());
            assertValid(tree);
        }

        @Test
        @DisplayName("clear 后可重新插入使用")
        void clearAndReuse() {
            BTree tree = new BTree();
            for (int i = 1; i <= 10; i++) {
                tree.insert(i);
            }
            tree.clear();
            for (int i = 100; i >= 91; i--) {
                tree.insert(i);
            }
            assertEquals(10, tree.size());
            List<Integer> expected = new ArrayList<>();
            for (int i = 91; i <= 100; i++) {
                expected.add(i);
            }
            assertEquals(expected, tree.inorder());
            assertValid(tree);
        }

        @Test
        @DisplayName("height 随插入增长")
        void heightGrows() {
            BTree tree = new BTree(2);
            assertEquals(-1, tree.height());
            tree.insert(1);
            assertEquals(0, tree.height());
            // t=2: 插入 1..8 后 height=1
            for (int i = 2; i <= 8; i++) {
                tree.insert(i);
            }
            assertEquals(1, tree.height());
            // 继续插入触发根分裂，height=2
            tree.insert(9);
            assertEquals(2, tree.height());
        }
    }

    // ==================== 删除 ====================

    @Nested
    @DisplayName("删除")
    class RemoveTests {

        @Test
        @DisplayName("空树删除返回 false")
        void removeFromEmpty() {
            BTree tree = new BTree();
            assertFalse(tree.remove(1));
            assertValid(tree);
        }

        @Test
        @DisplayName("删除不存在的键返回 false，树保持不变")
        void removeMissing() {
            BTree tree = new BTree();
            for (int i = 1; i <= 10; i++) {
                tree.insert(i);
            }
            assertFalse(tree.remove(99));
            assertEquals(10, tree.size());
            assertValid(tree);
        }

        @Test
        @DisplayName("删除唯一键后回到空树")
        void removeAllSingle() {
            BTree tree = new BTree();
            tree.insert(5);
            assertTrue(tree.remove(5));
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertValid(tree);
        }

        @Test
        @DisplayName("t=2 从小到大依次删光，覆盖借键/合并/内部替换各分支")
        void removeSequentialAllT2() { assertRemoveSequentialAll(2, 30); }

        @Test
        @DisplayName("t=3 从小到大依次删光，覆盖借键/合并/内部替换各分支")
        void removeSequentialAllT3() { assertRemoveSequentialAll(3, 30); }

        @Test
        @DisplayName("t=4 从小到大依次删光，覆盖借键/合并/内部替换各分支")
        void removeSequentialAllT4() { assertRemoveSequentialAll(4, 30); }

        private void assertRemoveSequentialAll(int t, int n) {
            BTree tree = new BTree(t);
            for (int i = 1; i <= n; i++) {
                tree.insert(i);
            }
            for (int i = 1; i <= n; i++) {
                assertTrue(tree.remove(i), "删除 " + i + " 应成功");
                assertEquals(n - i, tree.size());
                assertValid(tree);
            }
            assertTrue(tree.isEmpty());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("t=2 从大到小依次删光")
        void removeReverseSequentialAllT2() { assertRemoveReverseSequentialAll(2, 30); }

        @Test
        @DisplayName("t=3 从大到小依次删光")
        void removeReverseSequentialAllT3() { assertRemoveReverseSequentialAll(3, 30); }

        @Test
        @DisplayName("t=4 从大到小依次删光")
        void removeReverseSequentialAllT4() { assertRemoveReverseSequentialAll(4, 30); }

        private void assertRemoveReverseSequentialAll(int t, int n) {
            BTree tree = new BTree(t);
            for (int i = 1; i <= n; i++) {
                tree.insert(i);
            }
            for (int i = n; i >= 1; i--) {
                assertTrue(tree.remove(i), "删除 " + i + " 应成功");
                assertEquals(i - 1, tree.size());
                assertValid(tree);
            }
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("t=2 从内部节点删除：前驱替换、后继替换、合并各分支")
        void removeFromInternalT2() {
            BTree tree = new BTree(2);
            // 1..8 后根为 [2,4,6]，叶子 [1],[3],[5],[7,8]
            for (int i = 1; i <= 8; i++) {
                tree.insert(i);
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
        @DisplayName("t=2 随机键全部删光")
        void removeAllRandomT2() { assertRemoveAllRandom(2); }

        @Test
        @DisplayName("t=3 随机键全部删光")
        void removeAllRandomT3() { assertRemoveAllRandom(3); }

        @Test
        @DisplayName("t=4 随机键全部删光")
        void removeAllRandomT4() { assertRemoveAllRandom(4); }

        private void assertRemoveAllRandom(int t) {
            BTree tree = new BTree(t);
            Random rand = new Random(7);
            List<Integer> keys = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                int v = rand.nextInt(1000);
                if (!keys.contains(v)) {
                    keys.add(v);
                }
            }
            for (int v : keys) {
                tree.insert(v);
            }
            List<Integer> shuffled = new ArrayList<>(keys);
            Collections.shuffle(shuffled, new Random(99));
            for (int v : shuffled) {
                assertTrue(tree.remove(v), "删除 " + v + " 应成功");
                assertValid(tree);
            }
            assertTrue(tree.isEmpty());
            assertEquals(-1, tree.height());
            assertValid(tree);
        }

        @Test
        @DisplayName("t=2 随机删除一半后与 HashSet 对照")
        void removeHalfMatchesHashSetT2() { assertRemoveHalfMatchesHashSet(2); }

        @Test
        @DisplayName("t=3 随机删除一半后与 HashSet 对照")
        void removeHalfMatchesHashSetT3() { assertRemoveHalfMatchesHashSet(3); }

        @Test
        @DisplayName("t=4 随机删除一半后与 HashSet 对照")
        void removeHalfMatchesHashSetT4() { assertRemoveHalfMatchesHashSet(4); }

        private void assertRemoveHalfMatchesHashSet(int t) {
            BTree tree = new BTree(t);
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
                tree.insert(v);
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

        @Test
        @DisplayName("删除后 findMin/findMax 正确更新")
        void removeUpdatesMinMax() {
            BTree tree = new BTree();
            for (int i = 1; i <= 20; i++) {
                tree.insert(i);
            }
            assertEquals(1, (int) tree.findMin());
            assertEquals(20, (int) tree.findMax());

            tree.remove(1);
            assertEquals(2, (int) tree.findMin());
            assertValid(tree);

            tree.remove(20);
            assertEquals(19, (int) tree.findMax());
            assertValid(tree);
        }

        @Test
        @DisplayName("删除根键后树仍合法")
        void removeRootKey() {
            BTree tree = new BTree(2);
            for (int i = 1; i <= 15; i++) {
                tree.insert(i);
            }
            // 根键为某个中间值，删除后应仍保持结构
            Integer rootKey = tree.get(8); // 8 大概率在内部节点
            assertNotNull(rootKey);
            assertTrue(tree.remove(8));
            assertFalse(tree.contains(8));
            assertValid(tree);
        }
    }

    // ==================== 压力测试 ====================

    @Nested
    @DisplayName("压力测试")
    class StressTests {

        @Test
        @DisplayName("t=2 大规模随机插入/删除后结构合法")
        void stressRandomT2() { assertStressRandom(2); }

        @Test
        @DisplayName("t=3 大规模随机插入/删除后结构合法")
        void stressRandomT3() { assertStressRandom(3); }

        @Test
        @DisplayName("t=4 大规模随机插入/删除后结构合法")
        void stressRandomT4() { assertStressRandom(4); }

        @Test
        @DisplayName("t=5 大规模随机插入/删除后结构合法")
        void stressRandomT5() { assertStressRandom(5); }

        private void assertStressRandom(int t) {
            BTree tree = new BTree(t);
            Random rnd = new Random(20260802L);
            List<Integer> present = new ArrayList<>();
            int n = 2000;
            for (int i = 0; i < n; i++) {
                int k = rnd.nextInt(1_000_000);
                if (tree.insert(k)) {
                    present.add(k);
                }
            }
            assertEquals(present.size(), tree.size());
            assertValid(tree);

            Collections.shuffle(present, rnd);
            int removed = 0;
            for (int k : present) {
                assertTrue(tree.remove(k), "删除 " + k + " 应成功");
                removed++;
                if (removed % 100 == 0) {
                    assertValid(tree);
                }
            }
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertValid(tree);
        }

        @Test
        @DisplayName("交替插入删除 1000 轮后结构合法")
        void interleaveInsertRemove() {
            BTree tree = new BTree(3);
            Random rnd = new Random(999L);
            Set<Integer> present = new HashSet<>();
            for (int round = 0; round < 1000; round++) {
                int k = rnd.nextInt(10_000);
                if (present.add(k)) {
                    assertTrue(tree.insert(k));
                } else {
                    assertFalse(tree.insert(k));
                }
                assertValid(tree);
            }
            // 删掉一半
            List<Integer> toRemove = new ArrayList<>(present);
            Collections.shuffle(toRemove, rnd);
            int half = toRemove.size() / 2;
            for (int i = 0; i < half; i++) {
                int k = toRemove.get(i);
                assertTrue(tree.remove(k));
                present.remove(k);
                assertValid(tree);
            }
            // 验证剩余元素
            List<Integer> expected = new ArrayList<>(present);
            Collections.sort(expected);
            assertEquals(expected, tree.inorder());
            assertEquals(present.size(), tree.size());
        }
    }
}
