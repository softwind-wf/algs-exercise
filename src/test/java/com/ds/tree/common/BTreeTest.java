package com.ds.tree.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 泛型 B 树测试 —— 验证多路树抽象层可承载完整的 B 树行为(分裂插入、合并删除、结构不变量)
 */
@DisplayName("BTree 泛型B树(整合可行性)")
class BTreeTest {

    private BTree<Integer> tree;

    @BeforeEach
    void setUp() {
        tree = new BTree<>(2);
    }

    // ==================== 结构不变量校验(同包访问 root/t/maxKeys/minKeys) ====================

    /** 校验键数区间、键有序、BST 约束、叶子同层、size 一致。null 表示合法。 */
    private String checkStructure() {
        if (tree.root == null) {
            return tree.size == 0 ? null : "root 为空但 size=" + tree.size;
        }
        int[] leafDepth = {-1};
        int[] count = {0};
        String err = checkNode(tree.root, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, true, leafDepth, count);
        if (err != null) {
            return err;
        }
        return count[0] == tree.size ? null : "实际键数 " + count[0] + " 与 size " + tree.size + " 不一致";
    }

    private String checkNode(BTree.Node<Integer> node, int lo, int hi, int depth, boolean isRoot,
                             int[] leafDepth, int[] count) {
        count[0] += node.n;
        int min = isRoot ? 1 : tree.minKeys;
        if (node.n < min || node.n > tree.maxKeys) {
            return "键数 " + node.n + " 超出 [" + min + "," + tree.maxKeys + "]";
        }
        for (int i = 1; i < node.n; i++) {
            if (node.keyAt(i) <= node.keyAt(i - 1)) {
                return "节点内键未严格递增";
            }
        }
        for (int i = 0; i < node.n; i++) {
            if (node.keyAt(i) <= lo || node.keyAt(i) >= hi) {
                return "键值越界 (lo=" + lo + ", hi=" + hi + ")";
            }
        }
        boolean leaf = node.children[0] == null;
        for (int i = 0; i <= node.n; i++) {
            if (leaf != (node.children[i] == null)) {
                return "叶子/内部节点结构不一致";
            }
        }
        if (leaf) {
            if (leafDepth[0] == -1) {
                leafDepth[0] = depth;
            } else if (leafDepth[0] != depth) {
                return "叶子深度不一致:期望 " + leafDepth[0] + ",实际 " + depth;
            }
            return null;
        }
        for (int i = 0; i <= node.n; i++) {
            int childLo = (i == 0) ? lo : node.keyAt(i - 1);
            int childHi = (i == node.n) ? hi : node.keyAt(i);
            String err = checkNode(node.children[i], childLo, childHi, depth + 1, false, leafDepth, count);
            if (err != null) {
                return err;
            }
        }
        return null;
    }

    @Nested
    @DisplayName("插入与分裂")
    class InsertTest {

        @Test
        @DisplayName("插入 10 个键后中序升序且结构合法(t=2)")
        void insertCausesSplit() {
            for (int v : new int[]{10, 20, 30, 15, 25, 5, 35, 40, 45, 18}) {
                assertTrue(tree.insert(v));
            }
            assertEquals(ArraysList(5, 10, 15, 18, 20, 25, 30, 35, 40, 45), tree.inorder());
            assertEquals(10, tree.size());
            assertNull(checkStructure(), "分裂插入后结构被破坏: " + checkStructure());
        }

        @Test
        @DisplayName("重复插入返回 false")
        void duplicateRejected() {
            assertTrue(tree.insert(7));
            assertFalse(tree.insert(7));
            assertEquals(1, tree.size());
        }

        @Test
        @DisplayName("大量插入后高度保持对数量级")
        void heightStaysLogarithmic() {
            for (int i = 1; i <= 2000; i++) {
                tree.insert(i);
            }
            // t=2 的 B 树容纳 2000 个键高度约 5~6;若实现错误退化成链会到 2000
            assertTrue(tree.height() <= 12, "B 树高度异常: " + tree.height());
            assertEquals(2000, tree.size());
            assertEquals(1, tree.findMin());
            assertEquals(2000, tree.findMax());
        }

        @Test
        @DisplayName("get / contains 查询")
        void getAndContains() {
            for (int v : new int[]{50, 30, 70, 20, 40}) {
                tree.insert(v);
            }
            assertEquals(40, tree.get(40));
            assertNull(tree.get(99));
            assertTrue(tree.contains(70));
            assertFalse(tree.contains(80));
        }
    }

    @Nested
    @DisplayName("删除与合并")
    class RemoveTest {

        @Test
        @DisplayName("逐个删除所有键后树为空")
        void removeAllLeavesEmpty() {
            Random rnd = new Random(3);
            TreeSet<Integer> set = new TreeSet<>();
            for (int i = 0; i < 300; i++) {
                int v = rnd.nextInt(10_000);
                if (tree.insert(v)) {
                    set.add(v);
                }
            }
            List<Integer> keys = new ArrayList<>(set);
            Collections.shuffle(keys, rnd);
            for (int k : keys) {
                assertTrue(tree.remove(k), "删除 " + k + " 失败");
            }
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("删除不存在返回 false")
        void removeMissing() {
            for (int v : new int[]{5, 3, 8}) {
                tree.insert(v);
            }
            assertFalse(tree.remove(99));
            assertEquals(3, tree.size());
        }
    }

    @Nested
    @DisplayName("随机对拍与迭代器")
    class StressTest {

        @Test
        @DisplayName("随机插入+删除每步与 TreeSet 保持一致且结构合法")
        void randomOpsMatchTreeSet() {
            Random rnd = new Random(42);
            TreeSet<Integer> set = new TreeSet<>();
            for (int step = 0; step < 3000; step++) {
                int v = rnd.nextInt(1000);
                if (rnd.nextBoolean()) {
                    boolean inserted = tree.insert(v);
                    assertEquals(set.add(v), inserted);
                } else {
                    boolean removed = tree.remove(v);
                    assertEquals(set.remove(v), removed);
                }
                if (step % 200 == 0) {
                    assertNull(checkStructure(), "第 " + step + " 步后结构损坏: " + checkStructure());
                    assertEquals(set.size(), tree.size());
                    assertEquals(new ArrayList<>(set), tree.inorder());
                }
            }
            assertEquals(new ArrayList<>(set), tree.inorder());
            assertEquals(set.size(), tree.size());
        }

        @Test
        @DisplayName("迭代器输出与 inorder 一致")
        void iteratorMatchesInorder() {
            Random rnd = new Random(11);
            for (int i = 0; i < 200; i++) {
                tree.insert(rnd.nextInt(5000));
            }
            List<Integer> expected = tree.inorder();
            List<Integer> actual = new ArrayList<>();
            for (Integer v : tree) {
                actual.add(v);
            }
            assertEquals(expected, actual);

            Iterator<Integer> it = tree.iterator();
            assertTrue(it.hasNext());
            it.next();
            assertTrue(it.hasNext());
        }
    }

    /** 测试中的小工具：把 int 数组转成 List */
    private static List<Integer> ArraysList(int... values) {
        List<Integer> list = new ArrayList<>();
        for (int v : values) {
            list.add(v);
        }
        return list;
    }
}
