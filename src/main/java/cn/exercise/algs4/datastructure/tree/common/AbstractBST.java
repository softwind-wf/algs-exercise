package cn.exercise.algs4.datastructure.tree.common;

/**
 * 有序二叉搜索树抽象基类
 * <p>
 * 基于 {@link AbstractBinaryTree} 提供的通用结构，这里实现 BST 的通用增删查：
 * <ul>
 *     <li>{@code insert}：标准 BST 插入(不允许重复)，完成后调用平衡钩子；</li>
 *     <li>{@code remove}：标准 BST 删除(前驱替换，沿用 BinarySortedTree 的完整逻辑)，完成后调用平衡钩子；</li>
 *     <li>{@code contains} / {@code get} / {@code findMin} / {@code findMax}。</li>
 * </ul>
 * 平衡钩子 {@link #rebalanceAfterInsert} 与 {@link #rebalanceAfterRemove} 默认空实现(普通 BST 无需平衡)；
 * AVL 等平衡树只需覆盖这两个钩子。这恰好证明：BST 与 AVL 的全部差异，只有"平衡钩子"那几十行。
 * </p>
 *
 * @param <E> 元素类型，须可比较以保证有序性
 */
public abstract class AbstractBST<E extends Comparable<E>> extends AbstractBinaryTree<E> {

    protected AbstractBST() {
    }

    // ==================== 平衡钩子(默认空实现) ====================

    /**
     * 插入完成后的平衡钩子：从插入节点的父节点开始向上调整。
     * 普通 BST 无需做任何事；AVL 等平衡树覆盖此方法。
     *
     * @param node 插入节点的父节点(从此处向上)
     */
    protected void rebalanceAfterInsert(Node<E> node) {
    }

    /**
     * 删除完成后的平衡钩子：从受影响的节点开始向上调整。
     * 普通 BST 无需做任何事；AVL 等平衡树覆盖此方法。
     *
     * @param start 删除后需要重新平衡的起始节点
     */
    protected void rebalanceAfterRemove(Node<E> start) {
    }

    // ==================== 增删 ====================

    /**
     * 向树中添加数据(不允许重复)
     *
     * @param value 待添加的数据
     * @return true 添加成功；false 值已存在，添加失败
     */
    @Override
    public boolean insert(E value) {
        if (value == null) {
            throw new IllegalArgumentException("不允许插入 null");
        }
        if (root == null) {
            root = new Node<>(value);
            size++;
            return true;
        }
        Node<E> cur = root;
        Node<E> parent = null;
        while (cur != null) {
            parent = cur;
            int cmp = value.compareTo(cur.data);
            if (cmp < 0) {
                cur = cur.left;
            } else if (cmp > 0) {
                cur = cur.right;
            } else {
                return false;
            }
        }
        Node<E> node = new Node<>(value);
        node.parent = parent;
        if (value.compareTo(parent.data) < 0) {
            parent.left = node;
        } else {
            parent.right = node;
        }
        size++;
        rebalanceAfterInsert(node.parent);
        return true;
    }

    /**
     * 从树中删除数据
     *
     * @param value 待删除的数据
     * @return true 删除成功；false 值不存在
     */
    @Override
    public boolean remove(E value) {
        if (value == null) {
            throw new IllegalArgumentException("不允许删除 null");
        }
        Node<E> cur = findNode(value);
        if (cur == null) {
            return false;
        }
        size--;

        Node<E> rebalanceStart = null;

        if (cur.left == null && cur.right == null) {
            rebalanceStart = cur.parent;
            replaceNode(cur, null);
        } else if (cur.left != null && cur.right == null) {
            rebalanceStart = cur.parent;
            replaceNode(cur, cur.left);
        } else if (cur.left == null && cur.right != null) {
            rebalanceStart = cur.parent;
            replaceNode(cur, cur.right);
        } else {
            // 被删除节点同时具有左右子树：用左子树最大节点(前驱)替换
            Node<E> replace = cur.left;
            while (replace.right != null) {
                replace = replace.right;
            }
            // 用替换节点的左子节点替代替换节点在树中的位置
            rebalanceStart = (replace.parent == cur) ? replace : replace.parent;
            replaceNode(replace, replace.left);
            // 用替换节点保存被删除节点的左右子树
            replace.left = cur.left;
            if (cur.left != null) {
                cur.left.parent = replace;
            }
            replace.right = cur.right;
            if (cur.right != null) {
                cur.right.parent = replace;
            }
            replaceNode(cur, replace);
        }
        if (rebalanceStart != null) {
            rebalanceAfterRemove(rebalanceStart);
        }
        return true;
    }

    // ==================== 查找 ====================

    /**
     * 判断树中是否包含指定值
     *
     * @param value 待查找的数据
     * @return true 存在；false 不存在
     */
    public boolean contains(E value) {
        return findNode(value) != null;
    }

    /**
     * 在树中查找数据，不存在时返回 null
     *
     * @param value 待查找的数据
     * @return 查找到的数据；不存在返回 null
     */
    public E get(E value) {
        Node<E> node = findNode(value);
        return node != null ? node.data : null;
    }

    /**
     * 查找树中的最小值
     *
     * @return 最小值；空树返回 null
     */
    public E findMin() {
        if (root == null) {
            return null;
        }
        Node<E> cur = root;
        while (cur.left != null) {
            cur = cur.left;
        }
        return cur.data;
    }

    /**
     * 查找树中的最大值
     *
     * @return 最大值；空树返回 null
     */
    public E findMax() {
        if (root == null) {
            return null;
        }
        Node<E> cur = root;
        while (cur.right != null) {
            cur = cur.right;
        }
        return cur.data;
    }

    // ==================== 内部方法 ====================

    /**
     * 根据值查找节点
     *
     * @param value 待查找的值
     * @return 查找到的节点；不存在返回 null
     */
    private Node<E> findNode(E value) {
        Node<E> cur = root;
        while (cur != null) {
            int cmp = value.compareTo(cur.data);
            if (cmp < 0) {
                cur = cur.left;
            } else if (cmp > 0) {
                cur = cur.right;
            } else {
                return cur;
            }
        }
        return null;
    }

    /**
     * 根据当前节点与其父节点之间的关系，对当前节点在其父节点的左右子节点位置进行节点替换
     *
     * @param cur     被替换的节点
     * @param replace 用来替换的新节点
     */
    private void replaceNode(Node<E> cur, Node<E> replace) {
        if (cur != root) {
            if (cur == cur.parent.left) {
                cur.parent.left = replace;
            } else {
                cur.parent.right = replace;
            }
            if (replace != null) {
                replace.parent = cur.parent;
            }
        } else {
            root = replace;
            if (replace != null) {
                replace.parent = null;
            }
        }
        cur.parent = null;
        cur.left = null;
        cur.right = null;
    }
}
