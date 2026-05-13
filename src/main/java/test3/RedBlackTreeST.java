package test3;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛型红黑树符号表（加固版）
 * 改动说明：
 * 1. 删除操作 size 维护修正
 * 2. floor/ceiling/select 改为迭代，防止栈溢出
 * 3. 哨兵 NIL 改为 static final，节省内存
 * 4. 明确 null 值语义：put(key, null) 等价于 remove(key)；get 返回 null 表示键不存在
 * 5. 增加红黑树性质验证方法，便于测试
 */
public class RedBlackTreeST<K extends Comparable<K>, V> {
    private static final boolean RED = true;
    private static final boolean BLACK = false;

    // ---------- 静态共享哨兵 ----------
    @SuppressWarnings("rawtypes")
    private static final Node NIL = new Node<>(null, null);
    static {
        NIL.color = BLACK;
        NIL.size = 0;
        NIL.left = NIL;
        NIL.right = NIL;
        NIL.parent = NIL;
    }

    // ---------- 节点定义 ----------
    @SuppressWarnings("rawtypes")
    private static class Node<Key,Value> {
        Key key;
        Value value;
        Node left, right, parent;
        boolean color;
        int size;

        Node(Key key, Value value) {
            this.key = key;
            this.value = value;
            this.left = NIL;
            this.right = NIL;
            this.parent = NIL;
            this.color = RED;
            this.size = 1;
        }

        Key getKey() { return  key; }

        Value getValue() { return value; }

        void setValue(Value value) { this.value = value; }
    }

    private Node<K,V> root;

    @SuppressWarnings("unchecked")
    public RedBlackTreeST() {
        root = (Node<K,V>) NIL;
    }

    // ==============================
    // 符号表核心 API
    // ==============================

    /**
     * 插入或更新键值对。若 value 为 null，则执行删除操作。
     */
    @SuppressWarnings("unchecked")
    public void put(K key, V value) {
        if (key == null) throw new IllegalArgumentException("key 不能为 null");
        if (value == null) {
            remove(key);
            return;
        }

        Node<K,V> exist = searchNode(root, key);
        if (exist != NIL) {
            exist.setValue(value);
            return;
        }

        Node<K,V> z = new Node<>(key, value);
        Node<K,V> y = NIL;
        Node<K,V> x = root;

        // 查找插入位置，同时维护 size
        while (x != NIL) {
            y = x;
            // 稍后在修复时统一更新 size，此处暂不递增，避免重复计算
            int cmp = compare(key, x.getKey());
            if (cmp < 0) x = x.left;
            else x = x.right;
        }

        z.parent = y;
        if (y == NIL) root = z;
        else if (compare(key, y.getKey()) < 0) y.left = z;
        else y.right = z;

        insertFixup(z);
        updateSizeUpward(z);    // 从插入点向上修正 size
    }

    public V get(K key) {
        if (key == null) return null;
        Node<K,V> node = searchNode(root, key);
        return node == NIL ? null : node.getValue();
    }

    public boolean containsKey(K key) {
        return searchNode(root, key) != NIL;
    }

    public void remove(K key) {
        if (key == null) return;
        Node<K,V> z = searchNode(root, key);
        if (z == NIL) return;
        deleteNode(z);
    }

    public int size() {
        return root.size;
    }

    public boolean isEmpty() {
        return root == NIL;
    }

    public K min() {
        if (isEmpty()) return null;
        return minimum(root).getKey();
    }

    public K max() {
        if (isEmpty()) return null;
        return maximum(root).getKey();
    }

    public K floor(K key) {
        if (key == null) return null;
        Node<K,V> res = floorNode(root, key);
        return res == NIL ? null : res.getKey();
    }

    public K ceiling(K key) {
        if (key == null) return null;
        Node<K,V> res = ceilingNode(root, key);
        return res == NIL ? null : res.getKey();
    }

    public int rank(K key) {
        if (key == null) return 0;
        return rank(root, key);
    }

    public K select(int k) {
        if (k < 0 || k >= size()) return null;
        return select(root, k).getKey();
    }

    public List<K> keySet() {
        List<K> list = new ArrayList<>(size());
        inOrder(root, list);
        return list;
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        root = (Node<K, V>) NIL;
    }

    // ==============================
    // 红黑树核心算法（旋转、插入修复、删除）
    // ==============================

    private boolean getColor(Node<K,V> node) {
        return node.color;
    }

    @SuppressWarnings("unchecked")
    private void rotateLeft(Node<K,V> x) {
        Node<K,V> y = x.right;
        x.right = y.left;
        if (y.left != NIL) y.left.parent = x;
        y.parent = x.parent;
        if (x.parent == NIL) root = y;
        else if (x == x.parent.left) x.parent.left = y;
        else x.parent.right = y;
        y.left = x;
        x.parent = y;

        // 修正 size
        y.size = x.size;
        x.size = x.left.size + x.right.size + 1;
    }

    private void rotateRight(Node x) {
        Node y = x.left;
        x.left = y.right;
        if (y.right != NIL) y.right.parent = x;
        y.parent = x.parent;
        if (x.parent == NIL) root = y;
        else if (x == x.parent.right) x.parent.right = y;
        else x.parent.left = y;
        y.right = x;
        x.parent = y;

        y.size = x.size;
        x.size = x.left.size + x.right.size + 1;
    }

    @SuppressWarnings("unchecked")
    private void insertFixup(Node<K,V> z) {
        while (getColor(z.parent) == RED) {
            if (z.parent == z.parent.parent.left) {
                Node<K,V> y = z.parent.parent.right;
                if (getColor(y) == RED) {
                    z.parent.color = BLACK;
                    y.color = BLACK;
                    z.parent.parent.color = RED;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.right) {
                        z = z.parent;
                        rotateLeft(z);
                    }
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    rotateRight(z.parent.parent);
                }
            } else {
                Node<K,V> y = z.parent.parent.left;
                if (getColor(y) == RED) {
                    z.parent.color = BLACK;
                    y.color = BLACK;
                    z.parent.parent.color = RED;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.left) {
                        z = z.parent;
                        rotateRight(z);
                    }
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    rotateLeft(z.parent.parent);
                }
            }
        }
        root.color = BLACK;
    }

    private void transplant(Node<K,V> u, Node<K,V> v) {
        if (u.parent == NIL) root = v;
        else if (u == u.parent.left) u.parent.left = v;
        else u.parent.right = v;
        v.parent = u.parent;
    }

    @SuppressWarnings("unchecked")
    private void deleteNode(Node<K,V> z) {
        Node<K,V> y = z;
        boolean yOriginalColor = y.color;
        Node<K,V> x;
        Node<K,V> xParent; // 记录修复前 x 的父节点，用于后续 size 更新

        if (z.left == NIL) {
            x = z.right;
            transplant(z, z.right);
            xParent = z.parent; // 此时 z.parent 可能为 NIL，但 size 更新会处理
        } else if (z.right == NIL) {
            x = z.left;
            transplant(z, z.left);
            xParent = z.parent;
        } else {
            y = minimum(z.right);
            yOriginalColor = y.color;
            x = y.right;
            if (y.parent == z) {
                x.parent = y;      // 确保 x.parent 正确
                xParent = y;
            } else {
                transplant(y, y.right);
                y.right = z.right;
                y.right.parent = y;
                xParent = y.parent; // 注意：此时 y 的原父节点就是 x.parent
            }
            transplant(z, y);
            y.left = z.left;
            y.left.parent = y;
            y.color = z.color;
            // 注意：y 的 size 将在 updateSizeUpward 中修正，此处不单独设置
        }

        if (yOriginalColor == BLACK)
            deleteFixup(x);

        // 关键修复：从 xParent 开始向上更新 size
        updateSizeUpward(xParent);
    }

    @SuppressWarnings("unchecked")
    private void deleteFixup(Node<K,V> x) {
        while (x != root && getColor(x) == BLACK) {
            if (x == x.parent.left) {
                Node<K,V> w = x.parent.right;
                if (getColor(w) == RED) {
                    w.color = BLACK;
                    x.parent.color = RED;
                    rotateLeft(x.parent);
                    w = x.parent.right;
                }
                if (getColor(w.left) == BLACK && getColor(w.right) == BLACK) {
                    w.color = RED;
                    x = x.parent;
                } else {
                    if (getColor(w.right) == BLACK) {
                        w.left.color = BLACK;
                        w.color = RED;
                        rotateRight(w);
                        w = x.parent.right;
                    }
                    w.color = x.parent.color;
                    x.parent.color = BLACK;
                    w.right.color = BLACK;
                    rotateLeft(x.parent);
                    x = root;
                }
            } else {
                Node<K,V> w = x.parent.left;
                if (getColor(w) == RED) {
                    w.color = BLACK;
                    x.parent.color = RED;
                    rotateRight(x.parent);
                    w = x.parent.left;
                }
                if (getColor(w.right) == BLACK && getColor(w.left) == BLACK) {
                    w.color = RED;
                    x = x.parent;
                } else {
                    if (getColor(w.left) == BLACK) {
                        w.right.color = BLACK;
                        w.color = RED;
                        rotateLeft(w);
                        w = x.parent.left;
                    }
                    w.color = x.parent.color;
                    x.parent.color = BLACK;
                    w.left.color = BLACK;
                    rotateRight(x.parent);
                    x = root;
                }
            }
        }
        x.color = BLACK;
    }

    // ---------- 辅助函数 ----------
    @SuppressWarnings("unchecked")
    private Node<K,V> minimum(Node<K,V> node) {
        while (node.left != NIL) node = node.left;
        return node;
    }

    @SuppressWarnings("unchecked")
    private Node<K,V> maximum(Node<K,V> node) {
        while (node.right != NIL) node = node.right;
        return node;
    }

    @SuppressWarnings("unchecked")
    private Node<K,V> searchNode(Node<K,V> node, K key) {
        while (node != NIL) {
            int cmp = compare(key, node.getKey());
            if (cmp < 0) node = node.left;
            else if (cmp > 0) node = node.right;
            else return node;
        }
        return (Node<K,V>)NIL;
    }

    /**
     * 从指定节点开始向上更新 size，直到根节点
     */
    @SuppressWarnings("unchecked")
    private void updateSizeUpward(Node<K,V> node) {
        while (node != NIL) {
            node.size = node.left.size + node.right.size + 1;
            node = node.parent;
        }
    }

    /**
     * 迭代版 floor 查找
     */
    @SuppressWarnings("unchecked")
    private Node<K,V> floorNode(Node<K,V> x, K key) {
        Node<K,V> floor = NIL;
        while (x != NIL) {
            int cmp = compare(key, x.getKey());
            if (cmp == 0) return x;
            if (cmp < 0) {
                x = x.left;
            } else {
                floor = x;
                x = x.right;
            }
        }
        return floor;
    }

    /**
     * 迭代版 ceiling 查找
     */
    @SuppressWarnings("unchecked")
    private Node<K,V> ceilingNode(Node<K,V> x, K key) {
        Node<K,V> ceil = NIL;
        while (x != NIL) {
            int cmp = compare(key, (K) x.getKey());
            if (cmp == 0) return x;
            if (cmp > 0) {
                x = x.right;
            } else {
                ceil = x;
                x = x.left;
            }
        }
        return ceil;
    }

    @SuppressWarnings("unchecked")
    private int rank(Node<K,V> x, K key) {
        int rank = 0;
        while (x != NIL) {
            int cmp = compare(key, x.getKey());
            if (cmp < 0) {
                x = x.left;
            } else if (cmp > 0) {
                rank += x.left.size + 1;
                x = x.right;
            } else {
                rank += x.left.size;
                break;
            }
        }
        return rank;
    }

    /**
     * 迭代版 select
     */
    @SuppressWarnings("unchecked")
    private Node<K,V> select(Node<K,V> x, int k) {
        while (x != NIL) {
            int leftSize = x.left.size;
            if (k < leftSize) {
                x = x.left;
            } else if (k > leftSize) {
                k -= (leftSize + 1);
                x = x.right;
            } else {
                return x;
            }
        }
        return NIL; // 不会到达
    }

    @SuppressWarnings("unchecked")
    private void inOrder(Node<K,V> node, List<K> list) {
        if (node == NIL) return;
        inOrder(node.left, list);
        list.add((K) node.getKey());
        inOrder(node.right, list);
    }

    @SuppressWarnings("unchecked")
    private int compare(K a, K b) {
        return a.compareTo(b);
    }

    // ==============================
    // 验证红黑树性质（测试用）
    // ==============================
    public boolean isValidRBTree() {
        if (root == NIL) return true;
        // 1. 根必须为黑
        if (getColor(root) != BLACK) return false;
        // 2. 不能有连续红色
        // 3. 所有路径黑高一致，并同时验证 size 字段
        return checkNodeProperties(root) && checkBlackHeight(root) != -1;
    }

    private boolean checkNodeProperties(Node node) {
        if (node == NIL) return true;
        if (getColor(node) == RED) {
            if (getColor(node.left) == RED || getColor(node.right) == RED)
                return false;
        }
        int computedSize = node.left.size + node.right.size + 1;
        if (node.size != computedSize) return false;
        return checkNodeProperties(node.left) && checkNodeProperties(node.right);
    }

    private int checkBlackHeight(Node node) {
        if (node == NIL) return 1;
        int leftBH = checkBlackHeight(node.left);
        int rightBH = checkBlackHeight(node.right);
        if (leftBH == -1 || rightBH == -1 || leftBH != rightBH) return -1;
        return leftBH + (getColor(node) == BLACK ? 1 : 0);
    }

    // ==============================
    // 测试示例
    // ==============================
    public static void main(String[] args) {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(10, "A");
        st.put(5, "B");
        st.put(15, "C");
        st.put(3, "D");
        st.put(7, "E");
        st.put(12, "F");
        st.put(17, "G");

        System.out.println("size = " + st.size());
        System.out.println("keySet = " + st.keySet());
        System.out.println("floor(6) = " + st.floor(6));
        System.out.println("ceiling(6) = " + st.ceiling(6));
        System.out.println("select(3) = " + st.select(3));
        System.out.println("rank(10) = " + st.rank(10));

        st.remove(10);
        System.out.println("After remove 10: " + st.keySet());
        System.out.println("RBTree valid? " + st.isValidRBTree());

        // 测试 null 值语义
        st.put(100, null);
        System.out.println("containsKey(100)? " + st.containsKey(100)); // false
    }
}