package cn.exercise.algs4.datastructure.tree.common;

/**
 * 红黑树(左倾 LLRB,泛型集合版) —— 公共抽象层的"第三种平衡策略"
 * <p>
 * 与 {@link AvlTree} 复用结构层不同，红黑树的插入/删除需要在<b>递归下潜路径上</b>就地维护颜色
 * (旋转 + 颜色翻转是"边下潜边做"的)，无法用 AbstractBST 的"先挂节点、后调钩子"流程表达，
 * 因此这里<b>整体覆盖</b> insert/remove；但结构、四种遍历、迭代器、打印、统计、contains/get/findMin/findMax
 * 全部复用父类。这揭示了整合公共层的一个宝贵事实：不同平衡策略需要的"覆盖粒度"不同——
 * AVL 只需覆盖平衡钩子，红黑树需覆盖增删流程本身。
 * </p>
 * <p>
 * 实现为 Sedgewick 左倾红黑树(2-3 树的红黑表示)：红链接左倾、无节点同时连两条红链接、
 * 每条根叶路径黑节点数相同。插入/删除/查找均为 O(log n)。
 * </p>
 *
 * @param <E> 元素类型，须可比较以保证有序性
 */
public class RedBlackTree<E extends Comparable<E>> extends AbstractBST<E> {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    public RedBlackTree() {
        super();
    }

    // ==================== 插入 ====================

    @Override
    public boolean insert(E value) {
        if (value == null) {
            throw new IllegalArgumentException("不允许插入 null");
        }
        boolean[] added = {false};
        root = insert(root, value, added);
        root.color = BLACK;                 // 红黑树根必须为黑
        if (added[0]) {
            size++;
        }
        return added[0];
    }

    /**
     * 递归插入：新节点先为红，返回时沿路径执行旋转/翻修复颜色
     */
    private Node<E> insert(Node<E> h, E value, boolean[] added) {
        if (h == null) {
            Node<E> n = new Node<>(value);
            n.color = RED;
            added[0] = true;
            return n;
        }
        int cmp = value.compareTo(h.data);
        if (cmp < 0) {
            h.left = insert(h.left, value, added);
        } else if (cmp > 0) {
            h.right = insert(h.right, value, added);
        } else {
            added[0] = false;               // 重复键
        }
        if (isRed(h.right) && !isRed(h.left)) {
            h = rotateLeft(h);
        }
        if (isRed(h.left) && isRed(h.left.left)) {
            h = rotateRight(h);
        }
        if (isRed(h.left) && isRed(h.right)) {
            flipColors(h);
        }
        return h;
    }

    // ==================== 删除 ====================

    @Override
    public boolean remove(E value) {
        if (value == null) {
            throw new IllegalArgumentException("不允许删除 null");
        }
        if (!contains(value)) {
            return false;
        }
        // 若根的两个孩子都黑，先临时把根置红，让删除路径上可发生颜色翻转
        if (!isRed(root.left) && !isRed(root.right)) {
            root.color = RED;
        }
        boolean[] removed = {false};
        root = remove(root, value, removed);
        if (root != null) {
            root.color = BLACK;
        }
        if (removed[0]) {
            size--;
        }
        return removed[0];
    }

    /**
     * 递归删除：下潜前用 moveRedLeft/moveRedRight 腾出红节点，命中后用右子树最小键替换
     */
    private Node<E> remove(Node<E> h, E value, boolean[] removed) {
        int cmp = value.compareTo(h.data);
        if (cmp < 0) {
            if (!isRed(h.left) && !isRed(h.left.left)) {
                h = moveRedLeft(h);
            }
            h.left = remove(h.left, value, removed);
        } else {
            if (isRed(h.left)) {
                h = rotateRight(h);
            }
            if (value.compareTo(h.data) == 0 && h.right == null) {
                removed[0] = true;
                return null;                // 直接摘除
            }
            if (!isRed(h.right) && !isRed(h.right.left)) {
                h = moveRedRight(h);
            }
            if (value.compareTo(h.data) == 0) {
                Node<E> x = minNode(h.right);   // 用右子树最小键替换被删键
                h.data = x.data;
                h.right = deleteMin(h.right);
                removed[0] = true;
            } else {
                h.right = remove(h.right, value, removed);
            }
        }
        return balance(h);
    }

    /**
     * 删除以 h 为根的子树中的最小键
     */
    private Node<E> deleteMin(Node<E> h) {
        if (h.left == null) {
            return null;
        }
        if (!isRed(h.left) && !isRed(h.left.left)) {
            h = moveRedLeft(h);
        }
        h.left = deleteMin(h.left);
        return balance(h);
    }

    /**
     * 以 h 为根的子树的键最小节点
     */
    private Node<E> minNode(Node<E> h) {
        while (h.left != null) {
            h = h.left;
        }
        return h;
    }

    // ==================== 红黑维护 ====================

    private boolean isRed(Node<E> x) {
        return x != null && x.color == RED;
    }

    private Node<E> rotateRight(Node<E> h) {
        Node<E> x = h.left;
        h.left = x.right;
        x.right = h;
        x.color = h.color;
        h.color = RED;
        return x;
    }

    private Node<E> rotateLeft(Node<E> h) {
        Node<E> x = h.right;
        h.right = x.left;
        x.left = h;
        x.color = h.color;
        h.color = RED;
        return x;
    }

    private void flipColors(Node<E> h) {
        h.color = !h.color;
        h.left.color = !h.left.color;
        h.right.color = !h.right.color;
    }

    private Node<E> moveRedLeft(Node<E> h) {
        flipColors(h);
        if (isRed(h.right.left)) {
            h.right = rotateRight(h.right);
            h = rotateLeft(h);
            flipColors(h);
        }
        return h;
    }

    private Node<E> moveRedRight(Node<E> h) {
        flipColors(h);
        if (isRed(h.left.left)) {
            h = rotateRight(h);
            flipColors(h);
        }
        return h;
    }

    private Node<E> balance(Node<E> h) {
        if (isRed(h.right)) {
            h = rotateLeft(h);
        }
        if (isRed(h.left) && isRed(h.left.left)) {
            h = rotateRight(h);
        }
        if (isRed(h.left) && isRed(h.right)) {
            flipColors(h);
        }
        return h;
    }
}
