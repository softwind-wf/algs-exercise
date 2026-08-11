package com.ds.datastructure.btree;

import cn.exercise.algs4.datastructure.btree.BPlusTree;
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
 * B+ 树 JUnit 5 单元测试
 *
 * @author kevin
 * @version 1.0
 */
@DisplayName("BPlusTree 测试")
class BPlusTreeTest {

    // ==================== 辅助方法 ====================

    /** 校验 B+ 树的结构不变量，任何违反都让测试失败 */
    private void assertValid(BPlusTree tree) {
        String err = tree.checkStructure();
        assertNull(err, () -> "结构不变量被违反：" + err);
    }

    /** 校验树高不超过理论上界 log_t(n+1)（向上取整） */
    private void assertHeightBound(BPlusTree tree, int t, int n) {
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
            BPlusTree tree = new BPlusTree();
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("t=2 构造正常")
        void constructT2() {
            BPlusTree tree = new BPlusTree(2);
            assertTrue(tree.isEmpty());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("t=3 构造正常")
        void constructT3() {
            BPlusTree tree = new BPlusTree(3);
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("t=5 构造正常")
        void constructT5() {
            BPlusTree tree = new BPlusTree(5);
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("t<2 抛出 IllegalArgumentException")
        void constructInvalidT() {
            assertThrows(IllegalArgumentException.class, () -> new BPlusTree(1));
            assertThrows(IllegalArgumentException.class, () -> new BPlusTree(0));
            assertThrows(IllegalArgumentException.class, () -> new BPlusTree(-1));
        }
    }

    // ==================== 基础操作 ====================

    @Nested
    @DisplayName("基础操作")
    class BasicTests {

        @Test
        @DisplayName("空树属性")
        void emptyTree() {
            BPlusTree tree = new BPlusTree();
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertNull(tree.findMin());
            assertNull(tree.findMax());
            assertNull(tree.get(0));
            assertFalse(tree.contains(0));
            assertTrue(tree.inorder().isEmpty());
            assertTrue(tree.levelOrder().isEmpty());
            assertTrue(tree.leafNodes().isEmpty());
            assertTrue(tree.keysInRange(0, 100).isEmpty());
            assertValid(tree);
        }

        @Test
        @DisplayName("插入单个元素")
        void insertSingle() {
            BPlusTree tree = new BPlusTree();
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
            BPlusTree tree = new BPlusTree();
            assertTrue(tree.insert(10));
            assertFalse(tree.insert(10));
            assertEquals(1, tree.size());
            assertValid(tree);
        }

        @Test
        @DisplayName("重复键堆叠：全插同一个值")
        void insertSameValueRepeatedly() {
            BPlusTree tree = new BPlusTree();
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
            BPlusTree tree = new BPlusTree();
            tree.insert(10);
            assertFalse(tree.contains(99));
            assertNull(tree.get(99));
        }

        @Test
        @DisplayName("负数和零正常处理")
        void negativeAndZero() {
            BPlusTree tree = new BPlusTree();
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
            BPlusTree tree = new BPlusTree(t);
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
            BPlusTree tree = new BPlusTree(t);
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
            BPlusTree tree = new BPlusTree(t);
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
        @DisplayName("t=2 根分裂：叶子从 1 层变为 2 层")
        void rootSplitT2() {
            BPlusTree tree = new BPlusTree(2);
            // t=2: maxKeys=3, 叶子满 3 个键后插入第 4 个触发根分裂
            tree.insert(1);
            tree.insert(2);
            tree.insert(3);
            assertEquals(0, tree.height());
            assertValid(tree);

            tree.insert(4);
            assertEquals(1, tree.height());
            assertEquals(4, tree.size());
            assertValid(tree);
            assertEquals(Arrays.asList(1, 2, 3, 4), tree.inorder());
        }

        @Test
        @DisplayName("t=3 根分裂：叶子容量 5，插入第 6 个触发分裂")
        void rootSplitT3() {
            BPlusTree tree = new BPlusTree(3);
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
        @DisplayName("分裂后上浮的分隔键恰好是重复键，应被正确拒绝")
        void duplicatePromotedBySplit() {
            BPlusTree tree = new BPlusTree(2);
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
            BPlusTree tree = new BPlusTree();
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
            BPlusTree tree = new BPlusTree();
            tree.insert(5);
            tree.insert(3);
            tree.insert(7);
            tree.insert(1);
            tree.insert(4);
            assertEquals(Arrays.asList(1, 3, 4, 5, 7), tree.inorder());
        }

        @Test
        @DisplayName("层序遍历包含叶子全部键（内部节点分隔键会重复出现）")
        void levelOrderContainsLeafKeys() {
            BPlusTree tree = new BPlusTree();
            for (int i = 1; i <= 20; i++) {
                tree.insert(i);
            }
            List<Integer> level = tree.levelOrder();
            // 层序至少包含全部 20 个键（可能更多，因为内部分隔键是拷贝）
            assertTrue(level.size() >= 20,
                    "层序遍历应至少包含全部叶子键，实际 " + level.size());
            // 叶子键全部存在于层序中
            List<Integer> sorted = new ArrayList<>(level);
            Collections.sort(sorted);
            List<Integer> expected = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                expected.add(i);
            }
            // 层序排序后至少包含 1..20
            assertTrue(sorted.containsAll(expected),
                    "层序遍历应包含全部叶子键");
        }

        @Test
        @DisplayName("toArray / toList / toString")
        void conversions() {
            BPlusTree tree = new BPlusTree();
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
            BPlusTree tree = new BPlusTree();
            tree.insert(1);
            tree.insert(2);
            tree.insert(3);
            tree.clear();
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertNull(tree.findMin());
            assertNull(tree.findMax());
            assertTrue(tree.leafNodes().isEmpty());
            assertValid(tree);
        }

        @Test
        @DisplayName("clear 后可重新插入使用")
        void clearAndReuse() {
            BPlusTree tree = new BPlusTree();
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
            BPlusTree tree = new BPlusTree(2);
            assertEquals(-1, tree.height());
            tree.insert(1);
            assertEquals(0, tree.height());
            // t=2: maxKeys=3, 插入 1..3 后 height=0, 插入 4 触发根分裂
            tree.insert(2);
            tree.insert(3);
            assertEquals(0, tree.height());
            tree.insert(4);
            assertEquals(1, tree.height());
        }
    }

    // ==================== 叶子链表 ====================

    @Nested
    @DisplayName("叶子链表")
    class LeafChainTests {

        @Test
        @DisplayName("单叶子节点：leafNodes 返回一个列表")
        void singleLeaf() {
            BPlusTree tree = new BPlusTree();
            tree.insert(3);
            tree.insert(1);
            tree.insert(2);
            List<List<Integer>> leaves = tree.leafNodes();
            assertEquals(1, leaves.size());
            assertEquals(Arrays.asList(1, 2, 3), leaves.get(0));
        }

        @Test
        @DisplayName("多叶子：leafNodes 按升序串联所有键")
        void multipleLeaves() {
            BPlusTree tree = new BPlusTree(2);
            for (int i = 1; i <= 20; i++) {
                tree.insert(i);
            }
            List<List<Integer>> leaves = tree.leafNodes();
            assertTrue(leaves.size() > 1, "树高 > 0 时应有多个叶子");
            // 拼接所有叶子内容应等于中序遍历
            List<Integer> flat = new ArrayList<>();
            for (List<Integer> leaf : leaves) {
                flat.addAll(leaf);
            }
            assertEquals(tree.inorder(), flat);
        }

        @Test
        @DisplayName("叶子链表键数总和等于 size")
        void leafChainSizeMatches() {
            BPlusTree tree = new BPlusTree(3);
            Random rand = new Random(77);
            Set<Integer> keys = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                int v = rand.nextInt(10000);
                if (keys.add(v)) {
                    tree.insert(v);
                }
            }
            List<List<Integer>> leaves = tree.leafNodes();
            int total = 0;
            for (List<Integer> leaf : leaves) {
                total += leaf.size();
            }
            assertEquals(tree.size(), total);
        }

        @Test
        @DisplayName("叶子间严格有序：前一叶子最大键 < 后一叶子最小键")
        void leafChainCrossLeafOrder() {
            BPlusTree tree = new BPlusTree(2);
            for (int i = 1; i <= 50; i++) {
                tree.insert(i);
            }
            List<List<Integer>> leaves = tree.leafNodes();
            for (int i = 1; i < leaves.size(); i++) {
                List<Integer> prev = leaves.get(i - 1);
                List<Integer> curr = leaves.get(i);
                assertTrue(prev.get(prev.size() - 1) < curr.get(0),
                        "叶子间应严格升序: " + prev.get(prev.size() - 1) + " >= " + curr.get(0));
            }
        }
    }

    // ==================== 范围查询 ====================

    @Nested
    @DisplayName("范围查询 keysInRange")
    class RangeQueryTests {

        @Test
        @DisplayName("空树范围查询返回空列表")
        void emptyTreeRange() {
            BPlusTree tree = new BPlusTree();
            assertTrue(tree.keysInRange(0, 100).isEmpty());
        }

        @Test
        @DisplayName("lo > hi 返回空列表")
        void invalidRange() {
            BPlusTree tree = new BPlusTree();
            tree.insert(5);
            assertTrue(tree.keysInRange(10, 5).isEmpty());
        }

        @Test
        @DisplayName("单元素范围查询：命中")
        void singleElementHit() {
            BPlusTree tree = new BPlusTree();
            tree.insert(5);
            assertEquals(Arrays.asList(5), tree.keysInRange(5, 5));
            assertEquals(Arrays.asList(5), tree.keysInRange(0, 10));
        }

        @Test
        @DisplayName("单元素范围查询：未命中")
        void singleElementMiss() {
            BPlusTree tree = new BPlusTree();
            tree.insert(5);
            assertTrue(tree.keysInRange(6, 10).isEmpty());
            assertTrue(tree.keysInRange(0, 4).isEmpty());
        }

        @Test
        @DisplayName("范围查询覆盖全部键")
        void rangeCoversAll() {
            BPlusTree tree = new BPlusTree();
            for (int i = 1; i <= 20; i++) {
                tree.insert(i);
            }
            List<Integer> all = tree.keysInRange(1, 20);
            List<Integer> expected = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                expected.add(i);
            }
            assertEquals(expected, all);
        }

        @Test
        @DisplayName("范围查询覆盖部分键")
        void rangePartial() {
            BPlusTree tree = new BPlusTree();
            for (int i = 1; i <= 50; i++) {
                tree.insert(i);
            }
            List<Integer> result = tree.keysInRange(10, 20);
            List<Integer> expected = new ArrayList<>();
            for (int i = 10; i <= 20; i++) {
                expected.add(i);
            }
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("范围查询结果与 HashSet 对照")
        void rangeMatchesHashSet() {
            BPlusTree tree = new BPlusTree(3);
            Random rand = new Random(42);
            Set<Integer> keys = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                int v = rand.nextInt(100_000);
                if (keys.add(v)) {
                    tree.insert(v);
                }
            }
            // 测试 50 个随机范围
            Random rangeRand = new Random(99);
            for (int q = 0; q < 50; q++) {
                int lo = rangeRand.nextInt(100_000);
                int hi = rangeRand.nextInt(100_000);
                if (lo > hi) {
                    int tmp = lo;
                    lo = hi;
                    hi = tmp;
                }
                List<Integer> expected = new ArrayList<>();
                for (int k : keys) {
                    if (k >= lo && k <= hi) {
                        expected.add(k);
                    }
                }
                Collections.sort(expected);
                assertEquals(expected, tree.keysInRange(lo, hi),
                        "范围 [" + lo + ", " + hi + "] 不一致");
            }
        }

        @Test
        @DisplayName("范围查询跨越多个叶子")
        void rangeAcrossLeaves() {
            BPlusTree tree = new BPlusTree(2);
            for (int i = 1; i <= 100; i++) {
                tree.insert(i);
            }
            // 确保树高 > 0，范围跨越多个叶子
            assertTrue(tree.height() > 0);
            List<Integer> result = tree.keysInRange(25, 75);
            List<Integer> expected = new ArrayList<>();
            for (int i = 25; i <= 75; i++) {
                expected.add(i);
            }
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("范围查询边界：恰好命中最小/最大键")
        void rangeBoundary() {
            BPlusTree tree = new BPlusTree();
            for (int i = 10; i <= 50; i++) {
                tree.insert(i);
            }
            assertEquals(Arrays.asList(10), tree.keysInRange(10, 10));
            assertEquals(Arrays.asList(50), tree.keysInRange(50, 50));
            assertEquals(Arrays.asList(10, 11), tree.keysInRange(10, 11));
            assertEquals(Arrays.asList(49, 50), tree.keysInRange(49, 50));
        }
    }

    // ==================== 删除 ====================

    @Nested
    @DisplayName("删除")
    class RemoveTests {

        @Test
        @DisplayName("空树删除返回 false")
        void removeFromEmpty() {
            BPlusTree tree = new BPlusTree();
            assertFalse(tree.remove(1));
            assertValid(tree);
        }

        @Test
        @DisplayName("删除不存在的键返回 false，树保持不变")
        void removeMissing() {
            BPlusTree tree = new BPlusTree();
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
            BPlusTree tree = new BPlusTree();
            tree.insert(5);
            assertTrue(tree.remove(5));
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
            assertValid(tree);
        }

        @Test
        @DisplayName("t=2 从小到大依次删光")
        void removeSequentialAllT2() { assertRemoveSequentialAll(2, 30); }

        @Test
        @DisplayName("t=3 从小到大依次删光")
        void removeSequentialAllT3() { assertRemoveSequentialAll(3, 30); }

        @Test
        @DisplayName("t=4 从小到大依次删光")
        void removeSequentialAllT4() { assertRemoveSequentialAll(4, 30); }

        private void assertRemoveSequentialAll(int t, int n) {
            BPlusTree tree = new BPlusTree(t);
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
            BPlusTree tree = new BPlusTree(t);
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
        @DisplayName("t=2 随机键全部删光")
        void removeAllRandomT2() { assertRemoveAllRandom(2); }

        @Test
        @DisplayName("t=3 随机键全部删光")
        void removeAllRandomT3() { assertRemoveAllRandom(3); }

        @Test
        @DisplayName("t=4 随机键全部删光")
        void removeAllRandomT4() { assertRemoveAllRandom(4); }

        private void assertRemoveAllRandom(int t) {
            BPlusTree tree = new BPlusTree(t);
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
            BPlusTree tree = new BPlusTree(t);
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
            BPlusTree tree = new BPlusTree();
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
        @DisplayName("删除叶子最小键后分隔键正确更新")
        void removeLeafMinUpdatesSeparator() {
            BPlusTree tree = new BPlusTree(2);
            for (int i = 1; i <= 20; i++) {
                tree.insert(i);
            }
            // 删除最小键 1，它是最左叶子的第一个键，会触发分隔键更新
            assertTrue(tree.remove(1));
            assertEquals(2, (int) tree.findMin());
            assertFalse(tree.contains(1));
            assertValid(tree);
            // 验证范围查询仍然正确
            List<Integer> range = tree.keysInRange(2, 5);
            assertEquals(Arrays.asList(2, 3, 4, 5), range);
        }

        @Test
        @DisplayName("删除后范围查询仍然正确")
        void removeThenRangeQuery() {
            BPlusTree tree = new BPlusTree(3);
            for (int i = 1; i <= 50; i++) {
                tree.insert(i);
            }
            // 删除一些键
            tree.remove(10);
            tree.remove(20);
            tree.remove(30);
            tree.remove(40);
            assertValid(tree);

            assertEquals(Arrays.asList(11, 12, 13, 14, 15), tree.keysInRange(11, 15));
            assertEquals(Arrays.asList(21, 22, 23), tree.keysInRange(21, 23));
            assertTrue(tree.keysInRange(10, 10).isEmpty());
            assertTrue(tree.keysInRange(30, 30).isEmpty());
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
            BPlusTree tree = new BPlusTree(t);
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

            // 验证中序与插入集合一致
            List<Integer> sorted = new ArrayList<>(present);
            Collections.sort(sorted);
            assertEquals(sorted, tree.inorder());

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
        @DisplayName("交替插入删除 1000 轮 + 范围查询验证")
        void interleaveInsertRemoveWithRangeQuery() {
            BPlusTree tree = new BPlusTree(3);
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
            // 验证中序
            List<Integer> expected = new ArrayList<>(present);
            Collections.sort(expected);
            assertEquals(expected, tree.inorder());
            assertEquals(present.size(), tree.size());

            // 验证范围查询
            Random rangeRand = new Random(777);
            for (int q = 0; q < 30; q++) {
                int lo = rangeRand.nextInt(10_000);
                int hi = rangeRand.nextInt(10_000);
                if (lo > hi) {
                    int tmp = lo;
                    lo = hi;
                    hi = tmp;
                }
                List<Integer> rangeExpected = new ArrayList<>();
                for (int k : present) {
                    if (k >= lo && k <= hi) {
                        rangeExpected.add(k);
                    }
                }
                Collections.sort(rangeExpected);
                assertEquals(rangeExpected, tree.keysInRange(lo, hi),
                        "范围 [" + lo + ", " + hi + "] 不一致");
            }
        }
    }
}
