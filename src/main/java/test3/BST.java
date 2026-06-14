package test3;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * 基于二叉查找树 (BST) 的有序符号表实现
 * @param <Key> 键的类型，必须实现 Comparable 接口
 * @param <Value> 值的类型
 */
public class BST<Key extends Comparable<Key>, Value> {
    // 二叉查找树的根结点
    private Node root;

    /**
     * 结点内部类
     * 每个结点包含：键、值、左右子结点引用、以该结点为根的子树结点总数
     */
    private class Node {
        private Key key;           // 键
        private Value val;         // 值
        private Node left, right;  // 指向左右子树的链接
        private int N;             // 以该结点为根的子树中的结点总数

        public Node(Key key, Value val, int N) {
            this.key = key;
            this.val = val;
            this.N = N;
        }
    }

    //==================== 基础操作 ====================
    /**
     * 获取符号表中键值对的总数
     * @return 结点总数
     */
    public int size() {
        return size(root);
    }

    /**
     * 辅助方法：获取以 x 为根的子树的结点总数
     * @param x 子树根结点
     * @return 结点总数
     */
    private int size(Node x) {
        if (x == null) return 0;
        else return x.N;
    }

    //==================== 查找与插入 ====================
    /**
     * 根据键获取对应的值
     * @param key 要查找的键
     * @return 对应的值，不存在则返回 null
     */
    public Value get(Key key) {
        return get(root, key);
    }

    /**
     * 辅助方法：在以 x 为根的子树中查找 key 对应的值
     * @param x 子树根结点
     * @param key 要查找的键
     * @return 对应的值，不存在则返回 null
     */
    private Value get(Node x, Key key) {
        // 如果找不到则返回 null
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp < 0) return get(x.left, key);
        else if (cmp > 0) return get(x.right, key);
        else return x.val;
    }

    /**
     * 向符号表中插入键值对
     * 如果 key 已存在则更新值，否则创建新结点
     * @param key 键
     * @param val 值
     */
    public void put(Key key, Value val) {
        root = put(root, key, val);
    }

    /**
     * 辅助方法：在以 x 为根的子树中插入键值对
     * @param x 子树根结点
     * @param key 键
     * @param val 值
     * @return 插入后的子树根结点
     */
    private Node put(Node x, Key key, Value val) {
        // 如果 key 不存在于以 x 为根的子树中则创建新结点
        if (x == null) return new Node(key, val, 1);
        int cmp = key.compareTo(x.key);
        if (cmp < 0) x.left = put(x.left, key, val);
        else if (cmp > 0) x.right = put(x.right, key, val);
        else x.val = val; // 键已存在，更新值
        // 更新结点计数
        x.N = size(x.left) + size(x.right) + 1;
        return x;
    }

    //==================== 最大/最小键 ====================
    /**
     * 获取符号表中的最小键
     * @return 最小键
     */
    public Key min() {
        return min(root).key;
    }

    /**
     * 辅助方法：查找以 x 为根的子树中的最小键结点
     * @param x 子树根结点
     * @return 最小键结点
     */
    private Node min(Node x) {
        if (x.left == null) return x;
        return min(x.left);
    }

    /**
     * 获取符号表中的最大键
     * @return 最大键
     */
    public Key max() {
        return max(root).key;
    }

    /**
     * 辅助方法：查找以 x 为根的子树中的最大键结点
     * @param x 子树根结点
     * @return 最大键结点
     */
    private Node max(Node x) {
        if (x.right == null) return x;
        return max(x.right);
    }

    //==================== 向下取整/向上取整 ====================
    /**
     * 查找小于等于 key 的最大键
     * @param key 目标键
     * @return 小于等于 key 的最大键，不存在则返回 null
     */
    public Key floor(Key key) {
        Node x = floor(root, key);
        if (x == null) return null;
        return x.key;
    }

    /**
     * 辅助方法：查找以 x 为根的子树中小于等于 key 的最大键结点
     * @param x 子树根结点
     * @param key 目标键
     * @return 目标结点，不存在则返回 null
     */
    private Node floor(Node x, Key key) {
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp == 0) return x;
        if (cmp < 0) return floor(x.left, key);
        // key 大于当前结点，尝试在右子树中查找
        Node t = floor(x.right, key);
        if (t != null) return t;
        else return x;
    }

    /**
     * 查找大于等于 key 的最小键
     * @param key 目标键
     * @return 大于等于 key 的最小键，不存在则返回 null
     */
    public Key ceiling(Key key) {
        Node x = ceiling(root, key);
        if (x == null) return null;
        return x.key;
    }

    /**
     * 辅助方法：查找以 x 为根的子树中大于等于 key 的最小键结点
     * @param x 子树根结点
     * @param key 目标键
     * @return 目标结点，不存在则返回 null
     */
    private Node ceiling(Node x, Key key) {
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp == 0) return x;
        if (cmp > 0) return ceiling(x.right, key);
        // key 小于当前结点，尝试在左子树中查找
        Node t = ceiling(x.left, key);
        if (t != null) return t;
        else return x;
    }

    //==================== 选择/排名 ====================
    /**
     * 查找排名为 k 的键（即树中有 k 个小于它的键）
     * @param k 排名（从 0 开始）
     * @return 对应排名的键
     */
    public Key select(int k) {
        return select(root, k).key;
    }

    /**
     * 辅助方法：查找以 x 为根的子树中排名为 k 的结点
     * @param x 子树根结点
     * @param k 排名
     * @return 对应结点
     */
    private Node select(Node x, int k) {
        if (x == null) return null;
        int t = size(x.left);
        if (t > k) return select(x.left, k);
        else if (t < k) return select(x.right, k - t - 1);
        else return x;
    }

    /**
     * 计算给定键的排名（即树中小于 key 的键的数量）
     * @param key 目标键
     * @return 排名
     */
    public int rank(Key key) {
        return rank(key, root);
    }

    /**
     * 辅助方法：计算以 x 为根的子树中小于 key 的键的数量
     * @param key 目标键
     * @param x 子树根结点
     * @return 排名
     */
    private int rank(Key key, Node x) {
        if (x == null) return 0;
        int cmp = key.compareTo(x.key);
        if (cmp < 0) return rank(key, x.left);
        else if (cmp > 0) return 1 + size(x.left) + rank(key, x.right);
        else return size(x.left);
    }

    //==================== 删除操作 ====================
    /**
     * 删除最小键对应的结点
     */
    public void deleteMin() {
        root = deleteMin(root);
    }

    /**
     * 辅助方法：删除以 x 为根的子树中的最小键结点
     * @param x 子树根结点
     * @return 删除后的子树根结点
     */
    private Node deleteMin(Node x) {
        if (x.left == null) return x.right;
        x.left = deleteMin(x.left);
        x.N = size(x.left) + size(x.right) + 1;
        return x;
    }

    /**
     * 删除最大键对应的结点
     */
    public void deleteMax() {
        root = deleteMax(root);
    }

    /**
     * 辅助方法：删除以 x 为根的子树中的最大键结点
     * @param x 子树根结点
     * @return 删除后的子树根结点
     */
    private Node deleteMax(Node x) {
        if (x.right == null) return x.left;
        x.right = deleteMax(x.right);
        x.N = size(x.left) + size(x.right) + 1;
        return x;
    }

    /**
     * 删除指定键对应的结点
     * @param key 要删除的键
     */
    public void delete(Key key) {
        root = delete(root, key);
    }

    /**
     * 辅助方法：删除以 x 为根的子树中 key 对应的结点
     * @param x 子树根结点
     * @param key 要删除的键
     * @return 删除后的子树根结点
     */
    private Node delete(Node x, Key key) {
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp < 0) x.left = delete(x.left, key);
        else if (cmp > 0) x.right = delete(x.right, key);
        else {
            // 处理只有一个子结点或没有子结点的情况
            if (x.right == null) return x.left;
            if (x.left == null) return x.right;
            // 处理有两个子结点的情况：用后继结点替换
            Node t = x;
            x = min(t.right);          // 找到右子树的最小结点（后继）
            x.right = deleteMin(t.right); // 删除右子树的最小结点
            x.left = t.left;           // 左子树不变
        }
        // 更新结点计数
        x.N = size(x.left) + size(x.right) + 1;
        return x;
    }

    //==================== 范围查找 ====================
    /**
     * 获取符号表中所有键（按升序排列）
     * @return 所有键的可迭代对象
     */
    public Iterable<Key> keys() {
        return keys(min(), max());
    }

    /**
     * 获取 [lo, hi] 范围内的所有键（按升序排列
     * @param lo 下界
     * @param hi 上界
     * @return 范围内键的可迭代对象
     */
    public Iterable<Key> keys(Key lo, Key hi) {
        Queue<Key> queue = new LinkedList<>();
        keys(root, queue, lo, hi);
        return queue;
    }

    /**
     * 辅助方法：中序遍历收集 [lo, hi] 范围内的键
     * @param x 当前结点
     * @param queue 存储结果的队列
     * @param lo 下界
     * @param hi 上界
     */
    private void keys(Node x, Queue<Key> queue, Key lo, Key hi) {
        if (x == null) return;
        int cmplo = lo.compareTo(x.key);
        int cmphi = hi.compareTo(x.key);
        // 先递归左子树
        if (cmplo < 0) keys(x.left, queue, lo, hi);
        // 检查当前结点是否在范围内
        if (cmplo <= 0 && cmphi >= 0) queue.add(x.key);
        // 再递归右子树
        if (cmphi > 0) keys(x.right, queue, lo, hi);
    }
}