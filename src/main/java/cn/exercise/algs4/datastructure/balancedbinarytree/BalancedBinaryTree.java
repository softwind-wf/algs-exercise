package cn.exercise.algs4.datastructure.balancedbinarytree;

import java.util.*;

/**
 * 平衡二叉树（AVL Tree）
 * <p>
 * 在二叉排序树的基础上，通过旋转操作保证任意节点的左右子树高度差不超过 1，
 * 从而保证查找、插入、删除的时间复杂度始终为 O(log n)。
 * 支持增删查、遍历、统计等常用操作。
 * </p>
 *
 * @author kevin
 * @version 1.0
 */
public class BalancedBinaryTree implements Iterable<Integer> {

    /**
     * 平衡二叉树的节点类型（静态内部类）
     */
    private static class Node {
        int data;
        int height;
        Node parent;
        Node lChild;
        Node rChild;

        Node(int data) {
            this.data = data;
            this.height = 0;
        }
    }

    /** 根节点 */
    private Node root;

    /** 当前树中元素个数 */
    private int size;

    // ==================== 构造方法 ====================

    /** 构造一棵空树 */
    public BalancedBinaryTree() {
    }

    /**
     * 批量构造 —— 依次插入所有值
     *
     * @param values 待插入的值
     */
    public BalancedBinaryTree(int... values) {
        for (int v : values) {
            add(v);
        }
    }

    // ==================== 增删改查 ====================

    /**
     * 向平衡二叉树中添加数据（不允许重复）
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
        Node parent = null;
        while (cur != null) {
            parent = cur;
            if (data < cur.data) {
                cur = cur.lChild;
            } else if (data > cur.data) {
                cur = cur.rChild;
            } else {
                return false;
            }
        }
        Node node = new Node(data);
        node.parent = parent;
        if (data < parent.data) {
            parent.lChild = node;
        } else {
            parent.rChild = node;
        }
        size++;
        rebalanceAfterInsert(node.parent);
        return true;
    }

    /**
     * 从平衡二叉树中删除数据
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

        Node rebalanceStart = null;

        if (cur.lChild == null && cur.rChild == null) {
            rebalanceStart = cur.parent;
            replaceNode(cur, null);
        } else if (cur.lChild != null && cur.rChild == null) {
            rebalanceStart = cur.parent;
            replaceNode(cur, cur.lChild);
        } else if (cur.lChild == null && cur.rChild != null) {
            rebalanceStart = cur.parent;
            replaceNode(cur, cur.rChild);
        } else {
            Node replace = cur.lChild;
            while (replace.rChild != null) {
                replace = replace.rChild;
            }
            rebalanceStart = (replace.parent == cur) ? replace : replace.parent;
            replaceNode(replace, replace.lChild);
            replace.lChild = cur.lChild;
            if (cur.lChild != null) {
                cur.lChild.parent = replace;
            }
            replace.rChild = cur.rChild;
            if (cur.rChild != null) {
                cur.rChild.parent = replace;
            }
            replaceNode(cur, replace);
        }
        if (rebalanceStart != null) {
            rebalanceAfterRemove(rebalanceStart);
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
        return root == null ? -1 : root.height;
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
     */
    public void printTree() {
        printTree(root, 0);
    }

    // ==================== 平衡维护 ====================

    /**
     * 获取节点高度，空节点高度为 -1
     */
    private int getHeight(Node node) {
        return node == null ? -1 : node.height;
    }

    /**
     * 更新节点高度
     */
    private void updateHeight(Node node) {
        node.height = 1 + Math.max(getHeight(node.lChild), getHeight(node.rChild));
    }

    /**
     * 获取节点的平衡因子（左子树高度 - 右子树高度）
     */
    private int getBalanceFactor(Node node) {
        return node == null ? 0 : getHeight(node.lChild) - getHeight(node.rChild);
    }

    /**
     * 右旋（LL 型失衡时使用）
     */
    private Node rotateRight(Node y) {
        Node x = y.lChild;
        Node b = x.rChild;

        x.rChild = y;
        y.lChild = b;

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
     * 左旋（RR 型失衡时使用）
     */
    private Node rotateLeft(Node y) {
        Node x = y.rChild;
        Node b = x.lChild;

        x.lChild = y;
        y.rChild = b;

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
     * 对指定节点进行平衡调整，沿父链向上逐层修复，直到根节点
     */
    private void rebalance(Node start) {
        Node cur = start;
        while (cur != null) {
            updateHeight(cur);
            int bf = getBalanceFactor(cur);

            Node newSubtreeRoot = null;
            Node originalParent = cur.parent;  // 旋转前保存原始父节点

            if (bf > 1) {
                if (getBalanceFactor(cur.lChild) < 0) {
                    cur.lChild = rotateLeft(cur.lChild);
                }
                newSubtreeRoot = rotateRight(cur);
            } else if (bf < -1) {
                if (getBalanceFactor(cur.rChild) > 0) {
                    cur.rChild = rotateRight(cur.rChild);
                }
                newSubtreeRoot = rotateLeft(cur);
            }

            if (newSubtreeRoot != null) {
                if (originalParent == null) {
                    root = newSubtreeRoot;
                } else {
                    if (cur == originalParent.lChild) {
                        originalParent.lChild = newSubtreeRoot;
                    } else {
                        originalParent.rChild = newSubtreeRoot;
                    }
                }
            }

            cur = originalParent;
        }
    }

    /**
     * 插入后从插入节点的父节点开始向上平衡
     */
    private void rebalanceAfterInsert(Node start) {
        rebalance(start);
    }

    /**
     * 删除后从受影响的节点开始向上平衡
     */
    private void rebalanceAfterRemove(Node start) {
        rebalance(start);
    }

    // ==================== 内部方法 ====================

    /**
     * 根据值查找节点
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
     * 横向打印树的递归辅助方法
     */
    private void printTree(Node node, int depth) {
        if (node == null) {
            return;
        }
        printTree(node.rChild, depth + 1);
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        System.out.println(indent.toString() + node.data);
        printTree(node.lChild, depth + 1);
    }

    /**
     * 节点替换辅助方法
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
