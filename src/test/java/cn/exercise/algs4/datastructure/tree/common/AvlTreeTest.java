package cn.exercise.algs4.datastructure.tree.common;

import cn.exercise.algs4.datastructure.tree.common.AbstractBinaryTree;
import cn.exercise.algs4.datastructure.tree.common.AvlTree;
import cn.exercise.algs4.datastructure.tree.common.BinarySearchTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 泛型平衡二叉树测试 —— 验证"公共抽象层 + 两个平衡钩子"即可承载完整 AVL 行为。
 * 访问 protected root/Node(同包)直接校验平衡因子与高度字段。
 */
@DisplayName("AvlTree 泛型平衡二叉树(整合可行性)")
class AvlTreeTest {

    private AvlTree<Integer> tree;

    @BeforeEach
    void setUp() {
        tree = new AvlTree<>();
    }

    /** 递归校验整棵树任一节点平衡因子的绝对值 <= 1 */
    private static boolean isBalanced(AvlTree<?> t) {
        return checkBalance(t.root);
    }

    private static boolean checkBalance(AbstractBinaryTree.Node<?> node) {
        if (node == null) {
            return true;
        }
        int leftH = node.left == null ? -1 : node.left.height;
        int rightH = node.right == null ? -1 : node.right.height;
        if (Math.abs(leftH - rightH) > 1) {
            return false;
        }
        return checkBalance(node.left) && checkBalance(node.right);
    }

    /** 递归校验节点 height 字段与其左右子树高度一致 */
    private static boolean heightConsistent(AvlTree<?> t) {
        return checkHeight(t.root);
    }

    private static boolean checkHeight(AbstractBinaryTree.Node<?> node) {
        if (node == null) {
            return true;
        }
        int expected = 1 + Math.max(node.left == null ? -1 : node.left.height,
                node.right == null ? -1 : node.right.height);
        if (node.height != expected) {
            return false;
        }
        return checkHeight(node.left) && checkHeight(node.right);
    }

    @Nested
    @DisplayName("平衡性验证")
    class BalanceTest {

        @Test
        @DisplayName("升序插入后高度保持 O(log n) 量级")
        void ascendingInsertKeepsLowHeight() {
            for (int i = 1; i <= 1000; i++) {
                tree.insert(i);
            }
            // 1000 个元素完美平衡高度约 9~10;若退化成链,高度会是 999
            assertTrue(tree.height() <= 12, "AVL 升序插入后高度异常: " + tree.height());
            assertEquals(1000, tree.size());
        }

        @Test
        @DisplayName("升序插入后所有节点平衡因子合法")
        void allNodesBalanced() {
            for (int i = 1; i <= 1000; i++) {
                tree.insert(i);
            }
            assertTrue(isBalanced(tree), "升序插入后存在 |平衡因子| > 1 的节点");
        }

        @Test
        @DisplayName("删除后仍保持平衡且高度字段一致")
        void balancedAfterRemove() {
            for (int i = 1; i <= 1000; i++) {
                tree.insert(i);
            }
            // 随机顺序删除一半
            List<Integer> keys = new ArrayList<>();
            for (int i = 1; i <= 1000; i++) {
                keys.add(i);
            }
            Collections.shuffle(keys, new Random(7));
            for (int i = 0; i < 500; i++) {
                assertTrue(tree.remove(keys.get(i)), "删除 " + keys.get(i) + " 失败");
            }
            assertTrue(isBalanced(tree), "删除后出现失衡节点");
            assertTrue(heightConsistent(tree), "删除后 height 字段与子树不一致");
            assertEquals(500, tree.size());
        }
    }

    @Nested
    @DisplayName("有序性验证")
    class OrderTest {

        @Test
        @DisplayName("旋转不改变中序升序")
        void inorderStaysSorted() {
            Random rnd = new Random(42);
            List<Integer> inserted = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                int v = rnd.nextInt(100_000);
                if (tree.insert(v)) {
                    inserted.add(v);
                }
            }
            Collections.sort(inserted);
            assertEquals(inserted, tree.inorder(), "AVL 旋转破坏了中序有序性");
            assertEquals(inserted.size(), tree.size());
        }

        @Test
        @DisplayName("contains / findMin / findMax 在旋转后仍正确")
        void queriesAfterRotations() {
            Random rnd = new Random(1);
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            List<Integer> present = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                int v = rnd.nextInt(1000);
                if (tree.insert(v)) {
                    present.add(v);
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
            for (int v : present) {
                assertTrue(tree.contains(v), "旋转后 contains(" + v + ") 失败");
            }
            assertEquals(min, tree.findMin());
            assertEquals(max, tree.findMax());
        }
    }

    @Nested
    @DisplayName("与普通 BST 的差异对比")
    class ContrastTest {

        @Test
        @DisplayName("升序插入:BST 退化成链,AVL 保持平衡(整合差异的关键证据)")
        void avlBeatsBstOnAscending() {
            BinarySearchTree<Integer> bst = new BinarySearchTree<>();
            AvlTree<Integer> avl = new AvlTree<>();
            for (int i = 1; i <= 500; i++) {
                bst.insert(i);
                avl.insert(i);
            }
            assertEquals(499, bst.height(), "普通 BST 升序插入应退化为链(height=499)");
            assertTrue(avl.height() <= 12, "AVL 升序插入高度应接近 log n,实际 " + avl.height());
            assertTrue(avl.height() < bst.height() / 10);
            // 两者中序一致——同一抽象层、同一种树逻辑
            assertEquals(bst.inorder(), avl.inorder());
        }
    }
}
