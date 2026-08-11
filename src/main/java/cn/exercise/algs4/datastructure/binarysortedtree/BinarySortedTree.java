package cn.exercise.algs4.datastructure.binarysortedtree;

import java.util.*;

/**
 * 二叉排序树 (Binary Search Tree)
 * <p>
 * 特点：左子树所有节点值 < 根节点值 < 右子树所有节点值，且不允许重复值。
 * 支持增删查、遍历、统计等常用操作。
 * </p>
 *
 * @author kevin
 * @version 1.0
 */
public class BinarySortedTree implements Iterable<Integer> {

    /**
     * 二叉排序树的节点类型（静态内部类）
     */
    private static class Node {
        int data;               // 数据域
        Node parent;            // 父节点指针
        Node lChild;            // 左子节点指针
        Node rChild;            // 右子节点指针

        Node(int data) {
            this.data = data;
        }
    }

    /** 根节点 */
    private Node root;

    /** 当前树中元素个数 */
    private int size;

    // ==================== 构造方法 ====================

    /** 构造一棵空树 */
    public BinarySortedTree() {
    }

    /**
     * 批量构造 —— 依次插入所有值
     *
     * @param values 待插入的值
     */
    public BinarySortedTree(int... values) {
        for (int v : values) {
            add(v);
        }
    }

    // ==================== 增删改查 ====================

    /**
     * 向二叉排序树中添加数据（不允许重复）
     *
     * @param data 待添加的数据
     * @return true 添加成功；false 值已存在，添加失败
     */
    public boolean add(int data) {
        if (root == null) {
            root = new Node(data);
            size++;
            return true;
        }
        Node cur = root;
        while (true) {
            if (data < cur.data) {
                if (cur.lChild == null) {
                    Node node = new Node(data);
                    cur.lChild = node;
                    node.parent = cur;
                    size++;
                    return true;
                } else {
                    cur = cur.lChild;
                }
            } else if (data > cur.data) {
                if (cur.rChild == null) {
                    Node node = new Node(data);
                    cur.rChild = node;
                    node.parent = cur;
                    size++;
                    return true;
                } else {
                    cur = cur.rChild;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * 从二叉排序树中删除数据
     *
     * @param data 待删除的数据
     * @return true 删除成功；false 值不存在
     */
    public boolean remove(int data) {
        Node cur = findNode(data);
        if (cur == null) {
            return false;
        }
        size--;

        // 被删除节点为叶节点
        if (cur.lChild == null && cur.rChild == null) {
            replaceNode(cur, null);
        }
        // 被删除节点只存在左子树
        else if (cur.lChild != null && cur.rChild == null) {
            replaceNode(cur, cur.lChild);
        }
        // 被删除节点只存在右子树
        else if (cur.lChild == null && cur.rChild != null) {
            replaceNode(cur, cur.rChild);
        }
        // 被删除节点同时具有左右子树
        else {
            Node replace = cur.lChild;
            while (replace.rChild != null) {
                replace = replace.rChild;
            }
            // 用替换节点的左子节点替代替换节点在树中的位置
            replaceNode(replace, replace.lChild);
            // 用替换节点保存被删除节点左右子树
            replace.lChild = cur.lChild;
            if (cur.lChild != null) {
                cur.lChild.parent = replace;
            }
            replace.rChild = cur.rChild;
            if (cur.rChild != null) {
                cur.rChild.parent = replace;
            }
            // 用替换节点替代被删除节点在树中的位置
            replaceNode(cur, replace);
        }
        return true;
    }

    /**
     * 清空树中所有元素
     */
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * 判断树中是否包含指定值
     *
     * @param data 待查找的数据
     * @return true 存在；false 不存在
     */
    public boolean contains(int data) {
        return findNode(data) != null;
    }

    /**
     * 在树中查找数据，不存在时返回 null
     *
     * @param data 待查找的数据
     * @return 查找到的数据；不存在返回 null
     */
    public Integer get(int data) {
        Node node = findNode(data);
        return node != null ? node.data : null;
    }

    // ==================== 统计信息 ====================

    /**
     * 返回树中元素个数
     *
     * @return 元素个数
     */
    public int size() {
        return size;
    }

    /**
     * 判断树是否为空
     *
     * @return true 空树；false 非空
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 查找树中的最小值
     *
     * @return 最小值；空树返回 null
     */
    public Integer findMin() {
        if (root == null) {
            return null;
        }
        Node cur = root;
        while (cur.lChild != null) {
            cur = cur.lChild;
        }
        return cur.data;
    }

    /**
     * 查找树中的最大值
     *
     * @return 最大值；空树返回 null
     */
    public Integer findMax() {
        if (root == null) {
            return null;
        }
        Node cur = root;
        while (cur.rChild != null) {
            cur = cur.rChild;
        }
        return cur.data;
    }

    /**
     * 计算树的高度（根节点高度为 0）
     *
     * @return 树的高度；空树返回 -1
     */
    public int height() {
        return height(root);
    }

    // ==================== 遍历 ====================

    /**
     * 中序遍历（左 -> 根 -> 右），结果为升序序列
     *
     * @return 中序遍历结果列表
     */
    public List<Integer> inorder() {
        List<Integer> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    /**
     * 前序遍历（根 -> 左 -> 右）
     *
     * @return 前序遍历结果列表
     */
    public List<Integer> preorder() {
        List<Integer> result = new ArrayList<>();
        preorder(root, result);
        return result;
    }

    /**
     * 后序遍历（左 -> 右 -> 根）
     *
     * @return 后序遍历结果列表
     */
    public List<Integer> postorder() {
        List<Integer> result = new ArrayList<>();
        postorder(root, result);
        return result;
    }

    /**
     * 层序遍历（广度优先）
     *
     * @return 层序遍历结果列表
     */
    public List<Integer> levelOrder() {
        List<Integer> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            result.add(cur.data);
            if (cur.lChild != null) {
                queue.offer(cur.lChild);
            }
            if (cur.rChild != null) {
                queue.offer(cur.rChild);
            }
        }
        return result;
    }

    // ==================== 转换与迭代 ====================

    /**
     * 将树转为升序数组
     *
     * @return 升序数组
     */
    public int[] toArray() {
        List<Integer> list = inorder();
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    /**
     * 将树转为升序列表（同 {@link #inorder()}）
     *
     * @return 升序列表
     */
    public List<Integer> toList() {
        return inorder();
    }

    /**
     * 返回中序遍历迭代器
     *
     * @return 中序迭代器
     */
    @Override
    public Iterator<Integer> iterator() {
        return new InorderIterator();
    }

    // ==================== 输出 ====================

    /**
     * 返回树的字符串表示（中序遍历结果）
     *
     * @return 中序遍历的字符串
     */
    @Override
    public String toString() {
        return inorder().toString();
    }

    /**
     * 横向打印树形结构（右侧在上为树根方向）
     * <pre>
     * 示例：
     *     8
     *  7
     *     6
     * 5
     *     4
     *  3
     *     2
     * </pre>
     */
    public void printTree() {
        printTree(root, 0);
    }

    // ==================== 内部方法 ====================

    /**
     * 根据值查找节点
     *
     * @param data 待查找的值
     * @return 查找到的节点；不存在返回 null
     */
    private Node findNode(int data) {
        Node cur = root;
        while (cur != null) {
            if (data < cur.data) {
                cur = cur.lChild;
            } else if (data > cur.data) {
                cur = cur.rChild;
            } else {
                return cur;
            }
        }
        return null;
    }

    /**
     * 中序遍历递归
     */
    private void inorder(Node node, List<Integer> list) {
        if (node == null) {
            return;
        }
        inorder(node.lChild, list);
        list.add(node.data);
        inorder(node.rChild, list);
    }

    /**
     * 前序遍历递归
     */
    private void preorder(Node node, List<Integer> list) {
        if (node == null) {
            return;
        }
        list.add(node.data);
        preorder(node.lChild, list);
        preorder(node.rChild, list);
    }

    /**
     * 后序遍历递归
     */
    private void postorder(Node node, List<Integer> list) {
        if (node == null) {
            return;
        }
        postorder(node.lChild, list);
        postorder(node.rChild, list);
        list.add(node.data);
    }

    /**
     * 递归计算节点高度
     */
    private int height(Node node) {
        if (node == null) {
            return -1;
        }
        return 1 + Math.max(height(node.lChild), height(node.rChild));
    }

    /**
     * 横向打印树的递归辅助方法
     */
    private void printTree(Node node, int depth) {
        if (node == null) {
            return;
        }
        // 先打印右子树（在视觉上位于上方）
        printTree(node.rChild, depth + 1);
        // 缩进并打印当前节点
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        System.out.println(indent.toString() + node.data);
        // 再打印左子树（在视觉上位于下方）
        printTree(node.lChild, depth + 1);
    }

    /**
     * 根据当前节点与其父节点之间的关系，对当前节点在其父节点的左右子节点位置进行节点替换
     *
     * @param cur     被替换的节点
     * @param replace 用来替换的新节点
     */
    private void replaceNode(Node cur, Node replace) {
        if (cur != root) {
            if (cur == cur.parent.lChild) {
                cur.parent.lChild = replace;
            } else {
                cur.parent.rChild = replace;
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
        cur.lChild = null;
        cur.rChild = null;
    }

    // ==================== 内部迭代器 ====================

    /**
     * 中序遍历迭代器（基于栈实现，非递归）
     */
    private class InorderIterator implements Iterator<Integer> {
        private final Deque<Node> stack = new ArrayDeque<>();

        InorderIterator() {
            pushLeft(root);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Node node = stack.pop();
            pushLeft(node.rChild);
            return node.data;
        }

        /**
         * 将指定节点的所有左子孙入栈
         */
        private void pushLeft(Node node) {
            while (node != null) {
                stack.push(node);
                node = node.lChild;
            }
        }
    }
}
