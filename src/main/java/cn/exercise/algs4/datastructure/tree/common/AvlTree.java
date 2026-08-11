package cn.exercise.algs4.datastructure.tree.common;

/**
 * 平衡二叉树(AVL,泛型版) —— 公共抽象层的"钩子子类"演示
 * <p>
 * 与 {@link BinarySearchTree} 相比，本类<b>只覆盖了两个平衡钩子</b>，其余(增删查、遍历、迭代器、
 * 打印、统计)全部复用抽象层。这正是"整合"可行性的核心证据：
 * BST 与 AVL 之间的全部差异，只有 rebalanceAfterInsert / rebalanceAfterRemove 钩子里的旋转逻辑。
 * </p>
 * <p>
 * 旋转维护沿用 BalancedBinaryTree 的思路：任一节点左右子树高度差不超过 1，
 * 失衡时按 LL/RR/LR/RL 四种形态旋转修复，保证增删查复杂度始终为 O(log n)。
 * </p>
 *
 * @param <E> 元素类型，须可比较以保证有序性
 */
public class AvlTree<E extends Comparable<E>> extends AbstractBST<E> {

    public AvlTree() {
        super();
    }

    /**
     * 获取节点的平衡因子(左子树高度 - 右子树高度)
     */
    private int balanceFactor(Node<E> node) {
        return node == null ? 0 : getHeight(node.left) - getHeight(node.right);
    }

    /**
     * 插入钩子：从插入节点的父节点开始向上平衡
     */
    @Override
    protected void rebalanceAfterInsert(Node<E> node) {
        rebalance(node);
    }

    /**
     * 删除钩子：从受影响的节点开始向上平衡
     */
    @Override
    protected void rebalanceAfterRemove(Node<E> start) {
        rebalance(start);
    }

    /**
     * 对指定节点进行平衡调整，沿父链向上逐层修复，直到根节点
     */
    private void rebalance(Node<E> start) {
        Node<E> cur = start;
        while (cur != null) {
            updateHeight(cur);
            int bf = balanceFactor(cur);

            Node<E> newSubtreeRoot = null;
            Node<E> originalParent = cur.parent;   // 旋转前保存原始父节点

            if (bf > 1) {
                if (balanceFactor(cur.left) < 0) {
                    cur.left = rotateLeft(cur.left);
                }
                newSubtreeRoot = rotateRight(cur);
            } else if (bf < -1) {
                if (balanceFactor(cur.right) > 0) {
                    cur.right = rotateRight(cur.right);
                }
                newSubtreeRoot = rotateLeft(cur);
            }

            if (newSubtreeRoot != null) {
                if (originalParent == null) {
                    root = newSubtreeRoot;
                } else {
                    if (cur == originalParent.left) {
                        originalParent.left = newSubtreeRoot;
                    } else {
                        originalParent.right = newSubtreeRoot;
                    }
                }
            }

            cur = originalParent;
        }
    }

    /**
     * 右旋(LL 型失衡时使用)
     */
    private Node<E> rotateRight(Node<E> y) {
        Node<E> x = y.left;
        Node<E> b = x.right;

        x.right = y;
        y.left = b;

        if (b != null) {
            b.parent = y;
        }
        x.parent = y.parent;
        y.parent = x;

        updateHeight(y);
        updateHeight(x);
        return x;
    }

    /**
     * 左旋(RR 型失衡时使用)
     */
    private Node<E> rotateLeft(Node<E> y) {
        Node<E> x = y.right;
        Node<E> b = x.left;

        x.left = y;
        y.right = b;

        if (b != null) {
            b.parent = y;
        }
        x.parent = y.parent;
        y.parent = x;

        updateHeight(y);
        updateHeight(x);
        return x;
    }
}
