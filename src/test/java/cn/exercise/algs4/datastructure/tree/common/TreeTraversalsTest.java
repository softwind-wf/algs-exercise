package cn.exercise.algs4.datastructure.tree.common;

import cn.exercise.algs4.datastructure.tree.common.AbstractBinaryTree;
import cn.exercise.algs4.datastructure.tree.common.BinarySearchTree;
import cn.exercise.algs4.datastructure.tree.common.TreeTraversals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 遍历工具类测试 —— 验证递归/栈/Morris 三种遍历与 AbstractBinaryTree 自身遍历一致，
 * 且 Morris 遍历后树结构未被破坏(线索全部还原)
 */
@DisplayName("TreeTraversals 遍历算法统一工具(整合可行性)")
class TreeTraversalsTest {

    private BinarySearchTree<Integer> tree;
    private TreeTraversals.NodeAccessor<AbstractBinaryTree.Node<Integer>, Integer> accessor;
    private TreeTraversals.MutableNodeAccessor<AbstractBinaryTree.Node<Integer>, Integer> mutable;

    @BeforeEach
    void setUp() {
        //          5
        //        /   \
        //       3     8
        //      / \   / \
        //     1   4 7   9
        //      \
        //       2
        tree = new BinarySearchTree<>();
        for (int v : new int[]{5, 3, 8, 1, 4, 7, 9, 2}) {
            tree.insert(v);
        }
        accessor = new TreeTraversals.NodeAccessor<AbstractBinaryTree.Node<Integer>, Integer>() {
            @Override
            public Integer data(AbstractBinaryTree.Node<Integer> node) {
                return node.data;
            }

            @Override
            public AbstractBinaryTree.Node<Integer> left(AbstractBinaryTree.Node<Integer> node) {
                return node.left;
            }

            @Override
            public AbstractBinaryTree.Node<Integer> right(AbstractBinaryTree.Node<Integer> node) {
                return node.right;
            }
        };
        mutable = new TreeTraversals.MutableNodeAccessor<AbstractBinaryTree.Node<Integer>, Integer>() {
            @Override
            public Integer data(AbstractBinaryTree.Node<Integer> node) {
                return node.data;
            }

            @Override
            public AbstractBinaryTree.Node<Integer> left(AbstractBinaryTree.Node<Integer> node) {
                return node.left;
            }

            @Override
            public AbstractBinaryTree.Node<Integer> right(AbstractBinaryTree.Node<Integer> node) {
                return node.right;
            }

            @Override
            public void setRight(AbstractBinaryTree.Node<Integer> node, AbstractBinaryTree.Node<Integer> right) {
                node.right = right;
            }
        };
    }

    @Test
    @DisplayName("递归遍历与树自身方法一致")
    void recursiveMatchesTree() {
        assertEquals(tree.preorder(), TreeTraversals.preorder(tree.root, accessor));
        assertEquals(tree.inorder(), TreeTraversals.inorder(tree.root, accessor));
        assertEquals(tree.postorder(), TreeTraversals.postorder(tree.root, accessor));
    }

    @Test
    @DisplayName("栈迭代遍历与树自身方法一致")
    void stackMatchesTree() {
        assertEquals(tree.preorder(), TreeTraversals.preorderStack(tree.root, accessor));
        assertEquals(tree.inorder(), TreeTraversals.inorderStack(tree.root, accessor));
        assertEquals(tree.postorder(), TreeTraversals.postorderStack(tree.root, accessor));
    }

    @Test
    @DisplayName("层序遍历与树自身方法一致")
    void levelOrderMatchesTree() {
        assertEquals(tree.levelOrder(), TreeTraversals.levelOrder(tree.root, accessor));
    }

    @Test
    @DisplayName("Morris 中序/先序与递归结果一致")
    void morrisMatchesRecursive() {
        assertEquals(tree.inorder(), TreeTraversals.inorderMorris(tree.root, mutable));
        assertEquals(tree.preorder(), TreeTraversals.preorderMorris(tree.root, mutable));
    }

    @Test
    @DisplayName("Morris 遍历后树结构未被破坏(线索已还原)")
    void morrisLeavesTreeIntact() {
        List<Integer> before = tree.inorder();
        TreeTraversals.inorderMorris(tree.root, mutable);
        TreeTraversals.preorderMorris(tree.root, mutable);
        // 线索应全部还原:树的中序/先序/大小/查找均不变
        assertEquals(before, tree.inorder());
        assertEquals(8, tree.size());
        for (int v : Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9)) {
            assertTrue(tree.contains(v), "Morris 遍历后 contains(" + v + ") 失效");
        }
    }

    @Test
    @DisplayName("递归/栈/Morris 三种实现结果互相一致")
    void allImplementationsAgree() {
        List<Integer> inRec = TreeTraversals.inorder(tree.root, accessor);
        List<Integer> inStack = TreeTraversals.inorderStack(tree.root, accessor);
        List<Integer> inMorris = TreeTraversals.inorderMorris(tree.root, mutable);
        assertEquals(inRec, inStack);
        assertEquals(inRec, inMorris);

        List<Integer> preRec = TreeTraversals.preorder(tree.root, accessor);
        List<Integer> preStack = TreeTraversals.preorderStack(tree.root, accessor);
        List<Integer> preMorris = TreeTraversals.preorderMorris(tree.root, mutable);
        assertEquals(preRec, preStack);
        assertEquals(preRec, preMorris);

        assertEquals(TreeTraversals.postorder(tree.root, accessor),
                TreeTraversals.postorderStack(tree.root, accessor));
    }
}
