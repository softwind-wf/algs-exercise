package com.ds.tree.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 多叉树(List 孩子版) —— {@link AbstractMultiTree} 的孩子列表存储实现
 * <p>
 * 每个节点的孩子用 {@code List<Node>} 保存，直观且支持随机访问，
 * 对应原 com.ds.tree.Tree 的表示方式(children 为 ArrayList)。
 * 本类只实现三个孩子存储原语，遍历/统计/迭代/打印全部复用抽象基类。
 * </p>
 *
 * @param <E> 节点元素类型
 */
public class ListTree<E> extends AbstractMultiTree<E> {

    /** 构造一棵空树 */
    public ListTree() {
        super();
    }

    /** 构造一棵只含根节点的树 */
    public ListTree(E rootData) {
        super(rootData);
    }

    /** List 孩子版节点 */
    private static class ListNode<E> extends AbstractMultiTree.Node<E> {
        final List<Node<E>> children = new ArrayList<>();

        ListNode(E data) {
            super(data);
        }
    }

    // ==================== 孩子存储原语实现 ====================

    @Override
    protected List<Node<E>> childrenOf(Node<E> node) {
        return ((ListNode<E>) node).children;
    }

    @Override
    protected void linkChild(Node<E> parent, Node<E> child) {
        ((ListNode<E>) parent).children.add(child);
    }

    @Override
    protected Node<E> newNode(E data) {
        return new ListNode<>(data);
    }
}
