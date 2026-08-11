package com.ds.datastructure.tree.common;

import cn.exercise.algs4.datastructure.tree.common.AbstractMultiWayTree;
import cn.exercise.algs4.datastructure.tree.common.BPlusTree;
import cn.exercise.algs4.datastructure.tree.common.BTree;
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
 * 泛型 B+ 树测试 —— 验证多路树抽象层的"叶子链表变体"：
 * inorder 只含真键、分隔键==右子树最小键、范围查询、随机对拍、与 BTree 对比
 */
@DisplayName("BPlusTree 泛型B+树(多路树变体)")
class BPlusTreeTest {

    private BPlusTree<Integer> tree;

    @BeforeEach
    void setUp() {
        tree = new BPlusTree<>(2);
    }

    // ==================== B+ 树结构不变量校验(同包访问 root/Node) ====================

    /** null 表示合法 */
    private String checkStructure() {
        if (tree.root == null) {
            return tree.size == 0 ? null : "root 为空但 size=" + tree.size;
        }
        int[] leafDepth = {-1};
        int[] count = {0};
        int[] leafCount = {0};
        String err = checkNode(tree.root, Integer.MIN_VALUE, false, Integer.MAX_VALUE, 0, true,
                leafDepth, count, leafCount);
        if (err != null) {
            return err;
        }
        // 叶子链表:键数、叶子数、跨叶有序
        AbstractMultiWayTree.Node<Integer> leaf = firstLeaf();
        int chainKeys = 0;
        int chainLeaves = 0;
        AbstractMultiWayTree.Node<Integer> prev = null;
        while (leaf != null) {
            chainKeys += leaf.n;
            chainLeaves++;
            if (prev != null && prev.keyAt(prev.n - 1) >= leaf.keyAt(0)) {
                return "叶子链表跨叶无序(" + prev.keyAt(prev.n - 1) + ">=" + leaf.keyAt(0) + ")";
            }
            prev = leaf;
            leaf = leaf.next;
        }
        if (chainKeys != count[0]) {
            return "叶子链表键数 " + chainKeys + " != 树中真键数 " + count[0];
        }
        if (chainLeaves != leafCount[0]) {
            return "叶子链表节点数 " + chainLeaves + " != 树中叶子数 " + leafCount[0];
        }
        return count[0] == tree.size ? null : "真键数 " + count[0] + " 与 size " + tree.size + " 不一致";
    }

    private String checkNode(AbstractMultiWayTree.Node<Integer> node, int lo, boolean loInc, int hi, int depth,
                             boolean isRoot, int[] leafDepth, int[] count, int[] leafCount) {
        int min = isRoot ? 1 : tree.minKeys;
        if (node.n < min || node.n > tree.maxKeys) {
            return "键数 " + node.n + " 超出 [" + min + "," + tree.maxKeys + "]";
        }
        for (int i = 1; i < node.n; i++) {
            if (node.keyAt(i) <= node.keyAt(i - 1)) {
                return "节点内键未严格递增";
            }
        }
        // B+ 树分隔键 keys[i] = 右子树最小键,孩子 i>=1 下界是包含的;仅最左孩子 children[0] 开区间
        for (int i = 0; i < node.n; i++) {
            if (node.keyAt(i) < lo || (node.keyAt(i) == lo && !loInc) || node.keyAt(i) >= hi) {
                return "键值越界 (lo=" + lo + (loInc ? "含" : "开") + ", hi=" + hi + ")";
            }
        }
        if (isLeaf(node)) {
            leafCount[0]++;
            count[0] += node.n;             // 只有叶子里的键才是真实数据
            for (int i = 0; i <= node.n; i++) {
                if (node.children[i] != null) {
                    return "叶子不应有孩子";
                }
            }
            if (leafDepth[0] == -1) {
                leafDepth[0] = depth;
            } else if (leafDepth[0] != depth) {
                return "叶子深度不一致:期望 " + leafDepth[0] + ",实际 " + depth;
            }
            return null;
        }
        for (int i = 0; i < node.n; i++) {
            if (node.children[i] == null) {
                return "内部节点缺失孩子 " + i;
            }
            int rm = subtreeMin(node.children[i + 1]);
            if (!node.keyAt(i).equals(rm)) {
                return "分隔键 " + node.keyAt(i) + " != 右子树最小键 " + rm;
            }
        }
        if (node.children[node.n] == null) {
            return "内部节点缺失最后孩子";
        }
        for (int i = 0; i <= node.n; i++) {
            boolean childLoInc = (i == 0) ? loInc : true;
            int childLo = (i == 0) ? lo : node.keyAt(i - 1);
            int childHi = (i == node.n) ? hi : node.keyAt(i);
            String err = checkNode(node.children[i], childLo, childLoInc, childHi, depth + 1, false,
                    leafDepth, count, leafCount);
            if (err != null) {
                return err;
            }
        }
        return null;
    }

    private int subtreeMin(AbstractMultiWayTree.Node<Integer> node) {
        while (node.children[0] != null) {
            node = node.children[0];
        }
        return node.keyAt(0);
    }

    private AbstractMultiWayTree.Node<Integer> firstLeaf() {
        if (tree.root == null) {
            return null;
        }
        AbstractMultiWayTree.Node<Integer> cur = tree.root;
        while (cur.children[0] != null) {
            cur = cur.children[0];
        }
        return cur;
    }

    private boolean isLeaf(AbstractMultiWayTree.Node<Integer> node) {
        return node.children[0] == null;
    }

    @Nested
    @DisplayName("插入与结构")
    class InsertTest {

        @Test
        @DisplayName("插入触发分裂后 inorder 只含真键且升序")
        void inorderContainsOnlyRealKeys() {
            for (int v : new int[]{10, 20, 30, 15, 25, 5, 35, 40, 45, 18, 12}) {
                assertTrue(tree.insert(v));
            }
            assertEquals(ArraysList(5, 10, 12, 15, 18, 20, 25, 30, 35, 40, 45), tree.inorder());
            assertEquals(11, tree.size());
            assertNull(checkStructure(), "分裂插入后结构被破坏: " + checkStructure());
        }

        @Test
        @DisplayName("叶子链表结构与分隔键语义正确")
        void leafChainAndSeparators() {
            for (int v : new int[]{10, 20, 30, 15, 25, 5, 35, 40, 45, 18, 12}) {
                tree.insert(v);
            }
            // 层序:内部节点的分隔键是叶子键的拷贝,因此会重复出现
            List<Integer> level = tree.levelOrder();
            assertTrue(level.size() > tree.size(), "层序应含内部重复分隔键,实际 " + level.size() + " <= " + tree.size());
            // 中序只含叶子真键,不含重复
            assertEquals(11, tree.inorder().size());
            // 叶子链的键总和 = size
            int sum = 0;
            for (List<Integer> leaf : tree.leafNodes()) {
                sum += leaf.size();
            }
            assertEquals(11, sum);
        }

        @Test
        @DisplayName("大量升序插入后高度保持对数量级")
        void heightStaysLogarithmic() {
            for (int i = 1; i <= 2000; i++) {
                tree.insert(i);
            }
            assertTrue(tree.height() <= 12, "B+ 树高度异常: " + tree.height());
            assertEquals(2000, tree.size());
            assertEquals(1, tree.findMin());
            assertEquals(2000, tree.findMax());
            assertNull(checkStructure());
        }

        @Test
        @DisplayName("重复插入返回 false,get/contains 正确")
        void duplicateAndQueries() {
            for (int v : new int[]{50, 30, 70, 20, 40}) {
                tree.insert(v);
            }
            assertFalse(tree.insert(30));
            assertEquals(40, tree.get(40));
            assertNull(tree.get(99));
            assertTrue(tree.contains(70));
            assertFalse(tree.contains(80));
        }
    }

    @Nested
    @DisplayName("范围查询(B+树签名特性)")
    class RangeQueryTest {

        @Test
        @DisplayName("keysInRange 返回闭区间内全部键")
        void keysInRangeWorks() {
            for (int v : new int[]{10, 20, 30, 15, 25, 5, 35, 40, 45, 18, 12}) {
                tree.insert(v);
            }
            assertEquals(ArraysList(15, 18, 20, 25), tree.keysInRange(15, 25));
            assertEquals(ArraysList(5), tree.keysInRange(5, 5));
            assertEquals(Collections.emptyList(), tree.keysInRange(50, 60));
            assertEquals(Collections.emptyList(), tree.keysInRange(30, 10));   // lo > hi
            assertEquals(ArraysList(35, 40, 45), tree.keysInRange(35, 999));
        }

        @Test
        @DisplayName("范围查询与 TreeSet.subSet 对拍")
        void keysInRangeMatchesSubSet() {
            Random rnd = new Random(5);
            TreeSet<Integer> set = new TreeSet<>();
            for (int i = 0; i < 200; i++) {
                int v = rnd.nextInt(10_000);
                if (tree.insert(v)) {
                    set.add(v);
                }
            }
            for (int c = 0; c < 20; c++) {
                int lo = rnd.nextInt(10_000);
                int hi = rnd.nextInt(10_000);
                if (lo > hi) {
                    int tmp = lo;
                    lo = hi;
                    hi = tmp;
                }
                assertEquals(new ArrayList<>(set.subSet(lo, true, hi, true)), tree.keysInRange(lo, hi),
                        "范围 [" + lo + "," + hi + "] 不一致");
            }
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
        @DisplayName("删除后仍满足 B+ 树结构不变量")
        void structureHoldsAfterRemove() {
            for (int i = 1; i <= 500; i++) {
                tree.insert(i);
            }
            List<Integer> keys = new ArrayList<>();
            for (int i = 1; i <= 500; i++) {
                keys.add(i);
            }
            Collections.shuffle(keys, new Random(7));
            for (int i = 0; i < 250; i++) {
                assertTrue(tree.remove(keys.get(i)));
                assertNull(checkStructure(), "删除 " + keys.get(i) + " 后结构损坏: " + checkStructure());
            }
            assertEquals(250, tree.size());
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
    @DisplayName("随机对拍、迭代器与 BTree 对比")
    class StressTest {

        @Test
        @DisplayName("随机插入+删除与 TreeSet 保持一致且结构合法")
        void randomOpsMatchTreeSet() {
            Random rnd = new Random(42);
            TreeSet<Integer> set = new TreeSet<>();
            for (int step = 0; step < 3000; step++) {
                int v = rnd.nextInt(1000);
                if (rnd.nextBoolean()) {
                    assertEquals(set.add(v), tree.insert(v));
                } else {
                    assertEquals(set.remove(v), tree.remove(v));
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
        @DisplayName("迭代器与 inorder 一致")
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

        @Test
        @DisplayName("同一序列插入后,B+ 树与 B 树 inorder 一致")
        void agreesWithBTree() {
            Random rnd = new Random(17);
            BTree<Integer> btree = new BTree<>(2);
            for (int i = 0; i < 500; i++) {
                int v = rnd.nextInt(50_000);
                tree.insert(v);
                btree.insert(v);
            }
            assertEquals(btree.inorder(), tree.inorder());
            assertEquals(btree.size(), tree.size());
            assertNull(checkStructure());
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
