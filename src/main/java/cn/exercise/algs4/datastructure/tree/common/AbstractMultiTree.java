package cn.exercise.algs4.datastructure.tree.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * 多叉树(N-ary Tree)抽象基类 —— 树族中"任意个孩子的结构树"分支
 * <p>
 * 与前面的树有本质区别：二叉树/多路树/Trie 都是<b>有序集合</b>(有排序键、可增删查、
 * 有 inorder)，而多叉树是<b>结构树</b>——孩子是任意个、无序，没有"按值插入/删除"的集合语义。
 * 因此它<b>不实现</b> {@link Tree} 接口(该接口为有序集合树而设计)，而是自成一支：
 * 抽象"遍历算法 + 结构统计"，把孩子<b>如何存储</b>作为变体点。
 * </p>
 * <p>
 * 本类的价值：原 com.ds.datastructure.tree.Tree 的多叉树只有一种表示(List 孩子)，且遍历直接打印到 stdout。
 * 这里把先序/后序(递归 + 栈)、层序、size/height、迭代器、缩进打印统一实现一遍，
 * 孩子存储抽象成三个原语，支持两种经典表示：
 * </p>
 * <ul>
 *     <li>{@link ListTree}：List&lt;Node&gt; 孩子(直观、随机访问)；</li>
 *     <li>{@link SiblingTree}：firstChild/nextSibling 左孩子右兄弟链表(省空间，是
 *         多叉树转二叉树的中间形态)。</li>
 * </ul>
 *
 * @param <E> 节点元素类型
 */
public abstract class AbstractMultiTree<E> implements Iterable<E> {

    /** 多叉树节点：数据 + 父指针 */
    protected static class Node<E> {
        E data;
        Node<E> parent;

        Node(E data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return String.valueOf(data);
        }
    }

    /** 根节点 */
    protected Node<E> root;

    /** 树中节点总数 */
    protected int size;

    /** 构造一棵空树 */
    protected AbstractMultiTree() {
    }

    /**
     * 构造一棵只含根节点的树
     *
     * @param rootData 根节点数据
     */
    protected AbstractMultiTree(E rootData) {
        this.root = newNode(rootData);
        this.size = 1;
    }

    // ==================== 孩子存储抽象原语(子类实现) ====================

    /** 枚举节点的全部孩子(保持从左到右的顺序) */
    protected abstract List<Node<E>> childrenOf(Node<E> node);

    /** 把 child 挂到 parent 的孩子末尾(存储层的具体操作) */
    protected abstract void linkChild(Node<E> parent, Node<E> child);

    /** 创建子类自己的节点 */
    protected abstract Node<E> newNode(E data);

    // ==================== 构建 ====================

    /** 返回根节点 */
    public Node<E> getRoot() {
        return root;
    }

    /**
     * 设置根节点(仅空树可调用，用于从空树逐步构建)
     *
     * @param data 根节点数据
     * @return 新建的根节点
     */
    public Node<E> setRoot(E data) {
        if (root != null) {
            throw new IllegalStateException("树已有根节点，不能重复设置");
        }
        this.root = newNode(data);
        this.size = 1;
        return this.root;
    }

    /**
     * 为指定父节点添加一个数据为 childData 的新孩子
     *
     * @param parent    父节点
     * @param childData 孩子数据
     * @return 新建的孩子节点
     */
    public Node<E> addChild(Node<E> parent, E childData) {
        return addChild(parent, newNode(childData));
    }

    /**
     * 为指定父节点添加孩子节点(维护 parent 指针并计数)
     *
     * @param parent 父节点
     * @param child  孩子节点
     * @return 孩子节点
     */
    public Node<E> addChild(Node<E> parent, Node<E> child) {
        linkChild(parent, child);
        child.parent = parent;
        size++;
        return child;
    }

    // ==================== 统计信息 ====================

    /** 返回树中节点总数 */
    public int size() {
        return size;
    }

    /** 判断树是否为空 */
    public boolean isEmpty() {
        return size == 0;
    }

    /** 清空树 */
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * 计算树的高度(根节点高度为 0)
     *
     * @return 树的高度；空树返回 -1
     */
    public int height() {
        return height(root);
    }

    /** 判断节点是否为叶子(无孩子) */
    public boolean isLeaf(Node<E> node) {
        return childrenOf(node).isEmpty();
    }

    // ==================== 遍历 ====================

    /** 先序遍历(根 -> 依次孩子)，递归版 */
    public List<E> preorder() {
        List<E> result = new ArrayList<>();
        preorder(root, result);
        return result;
    }

    /** 先序遍历，栈迭代版(逆序入栈保证从左到右) */
    public List<E> preorderStack() {
        List<E> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Deque<Node<E>> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Node<E> node = stack.pop();
            result.add(node.data);
            List<Node<E>> children = childrenOf(node);
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return result;
    }

    /** 后序遍历(依次孩子 -> 根)，递归版 */
    public List<E> postorder() {
        List<E> result = new ArrayList<>();
        postorder(root, result);
        return result;
    }

    /** 后序遍历，双栈迭代版 */
    public List<E> postorderStack() {
        List<E> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Deque<Node<E>> work = new ArrayDeque<>();
        Deque<Node<E>> out = new ArrayDeque<>();
        work.push(root);
        while (!work.isEmpty()) {
            Node<E> node = work.pop();
            out.push(node);
            for (Node<E> child : childrenOf(node)) {
                work.push(child);
            }
        }
        while (!out.isEmpty()) {
            result.add(out.pop().data);
        }
        return result;
    }

    /** 层序遍历(广度优先)，队列实现 */
    public List<E> levelOrder() {
        List<E> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node<E>> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            Node<E> node = queue.poll();
            result.add(node.data);
            for (Node<E> child : childrenOf(node)) {
                queue.offer(child);
            }
        }
        return result;
    }

    // ==================== 迭代器 ====================

    /** 返回先序遍历迭代器(基于栈，非递归) */
    @Override
    public Iterator<E> iterator() {
        return new PreorderIterator();
    }

    // ==================== 输出 ====================

    @Override
    public String toString() {
        return preorder().toString();
    }

    /** 以缩进树形输出整棵多叉树(├── / └── 风格) */
    public void printTree() {
        printTree(root, "", true);
    }

    // ==================== 内部方法 ====================

    private void preorder(Node<E> node, List<E> list) {
        if (node == null) {
            return;
        }
        list.add(node.data);
        for (Node<E> child : childrenOf(node)) {
            preorder(child, list);
        }
    }

    private void postorder(Node<E> node, List<E> list) {
        if (node == null) {
            return;
        }
        for (Node<E> child : childrenOf(node)) {
            postorder(child, list);
        }
        list.add(node.data);
    }

    private int height(Node<E> node) {
        if (node == null) {
            return -1;
        }
        int max = -1;
        for (Node<E> child : childrenOf(node)) {
            max = Math.max(max, height(child));
        }
        return max + 1;
    }

    private void printTree(Node<E> node, String prefix, boolean isLast) {
        if (node == null) {
            return;
        }
        System.out.println(prefix + (isLast ? "└── " : "├── ") + node.data);
        List<Node<E>> children = childrenOf(node);
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) {
            printTree(children.get(i), childPrefix, i == children.size() - 1);
        }
    }

    // ==================== 内部迭代器 ====================

    /** 先序遍历迭代器：栈中按"逆序孩子"入栈，保证从左到右输出 */
    private class PreorderIterator implements Iterator<E> {
        private final Deque<Node<E>> stack = new ArrayDeque<>();

        PreorderIterator() {
            if (root != null) {
                stack.push(root);
            }
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
            List<Node<E>> children = childrenOf(node);
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
            return node.data;
        }
    }
}
