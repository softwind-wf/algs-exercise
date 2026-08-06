package com.ds.tree.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * 通用二叉树抽象基类 —— 树结构"整合可行性验证"的核心
 * <p>
 * 把不同二叉树(BST / AVL / 平衡二叉树 / 线索遍历等)中高度重复的结构操作集中到这里：
 * <ul>
 *     <li>四种遍历：先序、中序、后序(递归) + 层序(队列)；</li>
 *     <li>中序迭代器(基于栈，非递归，复用 BinarySortedTree/BalancedBinaryTree 的思路)；</li>
 *     <li>树形打印 printTree、toString；</li>
 *     <li>size / isEmpty / clear / height 统计。</li>
 * </ul>
 * 子类只负责实现两个方法：{@code insert(E)} 与 {@code remove(E)}，
 * 并可选择覆盖 {@link AbstractBST#rebalanceAfterInsert} / {@link AbstractBST#rebalanceAfterRemove}
 * 钩子来完成各自所需的平衡(普通 BST 无需，AVL 需要旋转)。
 * </p>
 * <p>设计要点：</p>
 * <ul>
 *     <li>泛型节点 Node 统一带 parent 指针与 height 字段：BST 忽略 height，AVL 用它做平衡，
 *         内存代价仅一个 int；</li>
 *     <li>这是"整合公共层"而非"上帝类"：左右两个孩子的结构是二叉树的共同点，
 *         B 树(数组孩子)、字典树(字符编码下标孩子)、多叉树(List 孩子)的孩子结构差异巨大，不在此列。</li>
 * </ul>
 *
 * @param <E> 元素类型
 */
public abstract class AbstractBinaryTree<E> implements Tree<E> {

    /**
     * 泛型二叉树节点：数据 + 父指针 + 左右孩子 + 平衡树所需的状态字段。
     * <p>
     * height 与 color 是"统一节点"的代价与收益：AVL 用 height、红黑树用 color，
     * 普通 BST 两者都不用——每种树只读取自己需要的字段，其余忽略，代价仅一个 int + 一个 boolean。
     * 这正是"整合到二叉树这一层"与"整合到所有树"的边界：孩子结构(左/右两指针)统一是划算的，
     * 而 B 树(数组孩子)、字典树(字符编码下标孩子)的孩子结构差异巨大，不在此列。
     * </p>
     */
    protected static class Node<E> {
        E data;
        Node<E> parent;
        Node<E> left;
        Node<E> right;
        int height;        // 以该节点为根的子树高度(叶子为 0)；AVL 等使用，BST 忽略
        boolean color;     // 红黑树使用(RED=true, BLACK=false)；BST/AVL 忽略

        Node(E data) {
            this.data = data;
        }
    }

    /** 根节点 */
    protected Node<E> root;

    /** 当前树中元素个数 */
    protected int size;

    // ==================== 构造方法 ====================

    protected AbstractBinaryTree() {
    }

    // ==================== 统计信息 ====================

    /** 返回树中元素个数 */
    public int size() {
        return size;
    }

    /** 判断树是否为空 */
    public boolean isEmpty() {
        return size == 0;
    }

    /** 清空树中所有元素 */
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * 计算树的高度(根节点高度为 0)
     * 实时递归计算，不依赖节点 height 字段(该字段仅供 AVL 等平衡树内部使用，
     * 普通 BST 不维护它)
     *
     * @return 树的高度；空树返回 -1
     */
    public int height() {
        return height(root);
    }

    /**
     * 递归计算以 node 为根的子树高度
     */
    private int height(Node<E> node) {
        if (node == null) {
            return -1;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }

    // ==================== 增删(由子类实现) ====================

    /**
     * 向树中添加数据(不允许重复)
     *
     * @param value 待添加的数据
     * @return true 添加成功；false 值已存在，添加失败
     */
    public abstract boolean insert(E value);

    /**
     * 从树中删除数据
     *
     * @param value 待删除的数据
     * @return true 删除成功；false 值不存在
     */
    public abstract boolean remove(E value);

    // ==================== 遍历(全部复用) ====================

    /**
     * 中序遍历(左 -> 根 -> 右)，结果为升序序列
     *
     * @return 中序遍历结果列表
     */
    public List<E> inorder() {
        List<E> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    /**
     * 前序遍历(根 -> 左 -> 右)
     *
     * @return 前序遍历结果列表
     */
    public List<E> preorder() {
        List<E> result = new ArrayList<>();
        preorder(root, result);
        return result;
    }

    /**
     * 后序遍历(左 -> 右 -> 根)
     *
     * @return 后序遍历结果列表
     */
    public List<E> postorder() {
        List<E> result = new ArrayList<>();
        postorder(root, result);
        return result;
    }

    /**
     * 层序遍历(广度优先)
     *
     * @return 层序遍历结果列表
     */
    public List<E> levelOrder() {
        List<E> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node<E>> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            Node<E> cur = queue.poll();
            result.add(cur.data);
            if (cur.left != null) {
                queue.offer(cur.left);
            }
            if (cur.right != null) {
                queue.offer(cur.right);
            }
        }
        return result;
    }

    // ==================== 迭代器 ====================

    /**
     * 返回中序遍历迭代器
     *
     * @return 中序迭代器
     */
    @Override
    public Iterator<E> iterator() {
        return new InorderIterator();
    }

    // ==================== 输出 ====================

    /**
     * 返回树的字符串表示(中序遍历结果)
     *
     * @return 中序遍历的字符串
     */
    @Override
    public String toString() {
        return inorder().toString();
    }

    /**
     * 横向打印树形结构(右侧在上为树根方向)
     */
    public void printTree() {
        printTree(root, 0);
    }

    // ==================== 内部工具 ====================

    /**
     * 获取节点高度，空节点高度为 -1
     */
    protected int getHeight(Node<E> node) {
        return node == null ? -1 : node.height;
    }

    /**
     * 更新节点高度
     */
    protected void updateHeight(Node<E> node) {
        node.height = 1 + Math.max(getHeight(node.left), getHeight(node.right));
    }

    /**
     * 中序遍历递归
     */
    private void inorder(Node<E> node, List<E> list) {
        if (node == null) {
            return;
        }
        inorder(node.left, list);
        list.add(node.data);
        inorder(node.right, list);
    }

    /**
     * 前序遍历递归
     */
    private void preorder(Node<E> node, List<E> list) {
        if (node == null) {
            return;
        }
        list.add(node.data);
        preorder(node.left, list);
        preorder(node.right, list);
    }

    /**
     * 后序遍历递归
     */
    private void postorder(Node<E> node, List<E> list) {
        if (node == null) {
            return;
        }
        postorder(node.left, list);
        postorder(node.right, list);
        list.add(node.data);
    }

    /**
     * 横向打印树的递归辅助方法
     */
    private void printTree(Node<E> node, int depth) {
        if (node == null) {
            return;
        }
        printTree(node.right, depth + 1);
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        System.out.println(indent.toString() + node.data);
        printTree(node.left, depth + 1);
    }

    // ==================== 内部迭代器 ====================

    /**
     * 中序遍历迭代器(基于栈实现，非递归)
     */
    private class InorderIterator implements Iterator<E> {
        private final Deque<Node<E>> stack = new ArrayDeque<>();

        InorderIterator() {
            pushLeft(root);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Node<E> node = stack.pop();
            pushLeft(node.right);
            return node.data;
        }

        /**
         * 将指定节点的所有左子孙入栈
         */
        private void pushLeft(Node<E> node) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }
    }
}
