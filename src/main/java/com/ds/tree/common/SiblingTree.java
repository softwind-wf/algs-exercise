package com.ds.tree.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 多叉树(左孩子右兄弟版) —— {@link AbstractMultiTree} 的链表存储实现
 * <p>
 * 每个节点只保留两个指针：firstChild(第一个孩子)与 nextSibling(下一个兄弟)，
 * 是 List 孩子版的空间优化——任何多叉树都能用"二叉树"的形状表达，
 * 也是多叉树转二叉树(如 TreeToBinaryTree)的经典中间形态。
 * 本类只实现三个孩子存储原语，遍历/统计/迭代/打印全部复用抽象基类。
 * </p>
 *
 * @param <E> 节点元素类型
 */
public class SiblingTree<E> extends AbstractMultiTree<E> {

    /** 构造一棵空树 */
    public SiblingTree() {
        super();
    }

    /** 构造一棵只含根节点的树 */
    public SiblingTree(E rootData) {
        super(rootData);
    }

    /** 左孩子右兄弟版节点：firstChild 指向第一个孩子，nextSibling 指向下一个兄弟 */
    private static class SiblingNode<E> extends AbstractMultiTree.Node<E> {
        Node<E> firstChild;
        Node<E> nextSibling;

        SiblingNode(E data) {
            super(data);
        }
    }

    // ==================== 孩子存储原语实现 ====================

    @Override
    protected List<Node<E>> childrenOf(Node<E> node) {
        // 从 firstChild 沿 nextSibling 链收集全部孩子
        List<Node<E>> list = new ArrayList<>();
        Node<E> child = ((SiblingNode<E>) node).firstChild;
        while (child != null) {
            list.add(child);
            child = ((SiblingNode<E>) child).nextSibling;
        }
        return list;
    }

    @Override
    protected void linkChild(Node<E> parent, Node<E> child) {
        // 挂到 firstChild 链的末尾
        SiblingNode<E> p = (SiblingNode<E>) parent;
        if (p.firstChild == null) {
            p.firstChild = child;
        } else {
            Node<E> last = p.firstChild;
            while (((SiblingNode<E>) last).nextSibling != null) {
                last = ((SiblingNode<E>) last).nextSibling;
            }
            ((SiblingNode<E>) last).nextSibling = child;
        }
        ((SiblingNode<E>) child).nextSibling = null;
    }

    @Override
    protected Node<E> newNode(E data) {
        return new SiblingNode<>(data);
    }
}
