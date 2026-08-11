package cn.exercise.algs4.datastructure.tree.common;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * 通用多路树抽象基类(B 树 / B+ 树等"数组孩子"结构的公共层)
 * <p>
 * 与二叉树的 {@link AbstractBinaryTree} 对应：二叉树的孩子是 left/right 两指针，
 * 而 B 树类结构的孩子是 {@code children[0..n]} 数组、键是 {@code keys[0..n-1]} 数组。
 * 本类把这部分结构 + 公共操作统一起来：
 * <ul>
 *     <li>size / isEmpty / clear / height / findMin / findMax；</li>
 *     <li>中序遍历(升序)与层序遍历；</li>
 *     <li>中序惰性迭代器(统一编码孩子/键位置的栈实现)；</li>
 *     <li>toString。</li>
 * </ul>
 * 插入(自顶向下分裂)与删除(自顶向下保障)的具体策略在子类实现。
 * </p>
 * <p>
 * 注意：这也界定了"整合"的边界——多路树与二叉树<b>不能</b>共享同一套孩子结构，
 * 因此它独立成层；但它与二叉树一样复用统一的 {@link Tree} 接口与中序/层序语义。
 * </p>
 *
 * @param <E> 元素类型，须可比较以保证有序性
 */
public abstract class AbstractMultiWayTree<E extends Comparable<E>> implements Tree<E> {

    /**
     * 多路树节点：keys[0..n-1] 升序存放键，children[0..n] 为子树指针；
     * 叶子节点的 children 全部为 null(以 children[0] == null 判定)。
     * next 是"统一节点"的又一代价：B+ 树叶子用 next 串成有序链表，
     * 普通 B 树忽略该字段(B 树叶子/内部节点都不会用到它)。
     */
    protected static class Node<E> {
        final Object[] keys;          // 用 Object[] 承载泛型键，避免直接创建泛型数组
        final Node<E>[] children;
        int n;
        Node<E> next;                 // 仅叶子使用：B+ 树叶子链表指针；B 树忽略

        @SuppressWarnings("unchecked")
        Node(int maxKeys) {
            keys = new Object[maxKeys];
            children = (Node<E>[]) new Node[maxKeys + 1];
        }

        @SuppressWarnings("unchecked")
        E keyAt(int i) {
            return (E) keys[i];
        }

        void setKey(int i, E key) {
            keys[i] = key;
        }
    }

    protected final int t;            // 最小度数，>= 2
    protected final int maxKeys;      // 2t-1
    protected final int minKeys;      // t-1
    protected Node<E> root;
    protected int size;

    protected AbstractMultiWayTree(int t) {
        if (t < 2) {
            throw new IllegalArgumentException("最小度数 t 必须 >= 2，实际 " + t);
        }
        this.t = t;
        this.maxKeys = 2 * t - 1;
        this.minKeys = t - 1;
    }

    // ==================== 统计信息 ====================

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * 树高(空树 -1；单节点 0)。多路树完美平衡，沿最左路径下行深度即树高。
     */
    @Override
    public int height() {
        if (root == null) {
            return -1;
        }
        int h = 0;
        Node<E> cur = root;
        while (cur.children[0] != null) {
            h++;
            cur = cur.children[0];
        }
        return h;
    }

    /**
     * 查找树中的最小值(沿最左路径下行到叶子)
     */
    public E findMin() {
        if (root == null) {
            return null;
        }
        Node<E> cur = root;
        while (cur.children[0] != null) {
            cur = cur.children[0];
        }
        return cur.keyAt(0);
    }

    /**
     * 查找树中的最大值(沿最右路径下行到叶子)
     */
    public E findMax() {
        if (root == null) {
            return null;
        }
        Node<E> cur = root;
        while (cur.children[cur.n] != null) {
            cur = cur.children[cur.n];
        }
        return cur.keyAt(cur.n - 1);
    }

    // ==================== 遍历 ====================

    /**
     * 中序遍历(左子树、键、右子树交替)，结果为升序序列
     */
    @Override
    public List<E> inorder() {
        List<E> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    /**
     * 层序遍历(广度优先)，节点内部按键升序输出
     */
    @Override
    public List<E> levelOrder() {
        List<E> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node<E>> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            Node<E> cur = queue.poll();
            for (int i = 0; i < cur.n; i++) {
                result.add(cur.keyAt(i));
            }
            for (int i = 0; i <= cur.n; i++) {
                if (cur.children[i] != null) {
                    queue.offer(cur.children[i]);
                }
            }
        }
        return result;
    }

    // ==================== 迭代器 ====================

    /**
     * 中序惰性迭代器：栈中的每个节点带一个"位置步" step，编码 0..2n；
     * step 为偶数代表孩子 children[step/2]，奇数代表键 keys[step/2]。
     * 对叶子，偶数位孩子为 null 直接跳过，自然落到奇数位输出键，统一了叶子与内部节点的访问逻辑。
     */
    @Override
    public Iterator<E> iterator() {
        return new InorderIterator();
    }

    // ==================== 输出 ====================

    @Override
    public String toString() {
        return inorder().toString();
    }

    // ==================== 抽象增删查 ====================

    @Override
    public abstract boolean insert(E value);

    @Override
    public abstract boolean remove(E value);

    /**
     * 判断树中是否包含指定值(基于子类实现的 {@link #get} 模板方法)
     *
     * @param value 待查找的数据
     * @return true 存在；false 不存在
     */
    public boolean contains(E value) {
        return get(value) != null;
    }

    /**
     * 查找指定值，不存在返回 null。多路树家族共有的查找骨架由子类实现。
     *
     * @param value 待查找的数据
     * @return 查找到的数据；不存在返回 null
     */
    public abstract E get(E value);

    // ==================== 内部方法 ====================

    private void inorder(Node<E> node, List<E> list) {
        if (node == null) {
            return;
        }
        if (node.children[0] == null) {
            for (int i = 0; i < node.n; i++) {
                list.add(node.keyAt(i));
            }
            return;
        }
        for (int i = 0; i < node.n; i++) {
            inorder(node.children[i], list);
            list.add(node.keyAt(i));
        }
        inorder(node.children[node.n], list);
    }

    // ==================== 内部迭代器 ====================

    private class InorderIterator implements Iterator<E> {
        private final Deque<Node<E>> nodeStack = new ArrayDeque<>();
        private final Deque<Integer> stepStack = new ArrayDeque<>();
        private E nextVal;

        InorderIterator() {
            if (root != null) {
                nodeStack.push(root);
                stepStack.push(0);
                advance();
            }
        }

        @Override
        public boolean hasNext() {
            return nextVal != null;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E out = nextVal;
            nextVal = null;
            advance();
            return out;
        }

        /**
         * 向前推进到下一个键。位置步 step 的取值范围 0..2n：
         * 偶数 = 孩子 children[step/2]，奇数 = 键 keys[step/2]。
         */
        private void advance() {
            while (!nodeStack.isEmpty()) {
                Node<E> node = nodeStack.peek();
                int step = stepStack.peek();
                if (step < 2 * node.n + 1) {
                    stepStack.pop();
                    stepStack.push(step + 1);         // 记录下次的位置步
                    int pos = step / 2;
                    if (step % 2 == 0) {
                        Node<E> child = node.children[pos];
                        if (child != null) {
                            nodeStack.push(child);
                            stepStack.push(0);
                        }
                        // 叶子孩子为 null：跳过该偶数位，继续循环落到奇数位输出键
                    } else {
                        nextVal = node.keyAt(pos);
                        return;
                    }
                } else {
                    nodeStack.pop();
                    stepStack.pop();
                }
            }
        }
    }
}
