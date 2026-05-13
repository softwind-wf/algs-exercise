package test3;

import java.util.NoSuchElementException;

public class RedBlackBST<Key extends Comparable<Key>, Value> {
    // 红黑树颜色常量
    private static final boolean RED   = true;
    private static final boolean BLACK = false;

    private Node root;     // 根结点

    // 结点内部类
    private class Node {
        Key key;           // 键
        Value val;         // 值
        Node left, right;  // 左右子树
        int N;             // 子树结点总数
        boolean color;     // 指向该结点的链接颜色

        Node(Key key, Value val, int N, boolean color) {
            this.key = key;
            this.val = val;
            this.N = N;
            this.color = color;
        }
    }

    // ------------------------------
    // 基础工具方法
    // ------------------------------
    // 检查结点颜色（空结点默认黑色）
    private boolean isRed(Node x) {
        if (x == null) return false;
        return x.color == RED;
    }

    // 获取树的结点总数
    public int size() {
        return size(root);
    }
    private int size(Node x) {
        if (x == null) return 0;
        return x.N;
    }

    public boolean isEmpty() {
        return root == null;
    }

    // ------------------------------
    // 旋转操作（左旋/右旋，和教材完全对应）
    // ------------------------------
    // 左旋：将结点h的红色右链转为左链
    private Node rotateLeft(Node h) {
        Node x = h.right;
        h.right = x.left;
        x.left = h;
        x.color = h.color;
        h.color = RED;
        x.N = h.N;
        h.N = 1 + size(h.left) + size(h.right);
        return x;
    }

    // 右旋：将结点h的红色左链转为右链
    private Node rotateRight(Node h) {
        Node x = h.left;
        h.left = x.right;
        x.right = h;
        x.color = h.color;
        h.color = RED;
        x.N = h.N;
        h.N = 1 + size(h.left) + size(h.right);
        return x;
    }

    // 颜色转换：分解4-结点（对应教材flipColors）
    private void flipColors(Node h) {
        h.color = RED;
        h.left.color = BLACK;
        h.right.color = BLACK;
    }

    // 辅助：删除操作中用的颜色转换（反向）
    private void flipColorsReverse(Node h) {
        h.color = BLACK;
        h.left.color = RED;
        h.right.color = RED;
    }

    // 保持红黑树平衡的通用修复方法（插入/删除后调用）
    private Node balance(Node h) {
        // 1. 右链红、左链黑：左旋
        if (isRed(h.right) && !isRed(h.left)) {
            h = rotateLeft(h);
        }
        // 2. 连续两个左红链：右旋
        if (isRed(h.left) && isRed(h.left.left)) {
            h = rotateRight(h);
        }
        // 3. 左右都是红链：颜色转换
        if (isRed(h.left) && isRed(h.right)) {
            flipColors(h);
        }

        h.N = 1 + size(h.left) + size(h.right);
        return h;
    }

    // ------------------------------
    // 插入操作（和教材算法3.4一致）
    // ------------------------------
    public void put(Key key, Value val) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        if (val == null) {
            delete(key);
            return;
        }
        root = put(root, key, val);
        root.color = BLACK; // 根结点永远为黑
    }

    private Node put(Node h, Key key, Value val) {
        if (h == null) {
            // 新结点用红链连接
            return new Node(key, val, 1, RED);
        }

        int cmp = key.compareTo(h.key);
        if (cmp < 0) {
            h.left = put(h.left, key, val);
        } else if (cmp > 0) {
            h.right = put(h.right, key, val);
        } else {
            // 键已存在，更新值
            h.val = val;
        }

        // 自下而上修复红黑树性质
        return balance(h);
    }

    // ------------------------------
    // 查找操作
    // ------------------------------
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        return get(root, key);
    }

    private Value get(Node h, Key key) {
        if (h == null) return null;
        int cmp = key.compareTo(h.key);
        if (cmp < 0) {
            return get(h.left, key);
        } else if (cmp > 0) {
            return get(h.right, key);
        } else {
            return h.val;
        }
    }

    public boolean contains(Key key) {
        return get(key) != null;
    }

    // ------------------------------
    // 最小/最大键
    // ------------------------------
    public Key min() {
        if (isEmpty()) throw new NoSuchElementException("符号表为空");
        return min(root).key;
    }

    private Node min(Node h) {
        if (h.left == null) return h;
        return min(h.left);
    }

    public Key max() {
        if (isEmpty()) throw new NoSuchElementException("符号表为空");
        return max(root).key;
    }

    private Node max(Node h) {
        if (h.right == null) return h;
        return max(h.right);
    }

    // ------------------------------
    // 向下取整/向上取整
    // ------------------------------
    public Key floor(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        if (isEmpty()) throw new NoSuchElementException("符号表为空");
        Node x = floor(root, key);
        return x == null ? null : x.key;
    }

    private Node floor(Node h, Key key) {
        if (h == null) return null;
        int cmp = key.compareTo(h.key);
        if (cmp == 0) return h;
        if (cmp < 0) return floor(h.left, key);
        Node t = floor(h.right, key);
        return t != null ? t : h;
    }

    public Key ceiling(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        if (isEmpty()) throw new NoSuchElementException("符号表为空");
        Node x = ceiling(root, key);
        return x == null ? null : x.key;
    }

    private Node ceiling(Node h, Key key) {
        if (h == null) return null;
        int cmp = key.compareTo(h.key);
        if (cmp == 0) return h;
        if (cmp > 0) return ceiling(h.right, key);
        Node t = ceiling(h.left, key);
        return t != null ? t : h;
    }

    // ------------------------------
    // 排名/选择
    // ------------------------------
    public int rank(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        return rank(root, key);
    }

    private int rank(Node h, Key key) {
        if (h == null) return 0;
        int cmp = key.compareTo(h.key);
        if (cmp < 0) {
            return rank(h.left, key);
        } else if (cmp > 0) {
            return 1 + size(h.left) + rank(h.right, key);
        } else {
            return size(h.left);
        }
    }

    public Key select(int k) {
        if (k < 0 || k >= size()) throw new IllegalArgumentException("k超出范围");
        return select(root, k).key;
    }

    private Node select(Node h, int k) {
        int t = size(h.left);
        if (t > k) return select(h.left, k);
        if (t < k) return select(h.right, k - t - 1);
        return h;
    }

    // ------------------------------
    // 删除操作（红黑树删除核心逻辑）
    // ------------------------------
    // 删除操作的辅助方法：将h的左链或右链转为红链
    private Node moveRedLeft(Node h) {
        flipColorsReverse(h);
        if (isRed(h.right.left)) {
            h.right = rotateRight(h.right);
            h = rotateLeft(h);
            flipColors(h);
        }
        return h;
    }

    private Node moveRedRight(Node h) {
        flipColorsReverse(h);
        if (!isRed(h.left.left)) {
            h = rotateRight(h);
            flipColors(h);
        }
        return h;
    }

    // 删除最小键
    public void deleteMin() {
        if (isEmpty()) throw new NoSuchElementException("符号表为空");
        if (!isRed(root.left) && !isRed(root.right)) {
            root.color = RED;
        }
        root = deleteMin(root);
        if (!isEmpty()) root.color = BLACK;
    }

    private Node deleteMin(Node h) {
        if (h.left == null) return null;

        // 如果左链是黑且左链的左链也是黑，需要从右边借一个红链
        if (!isRed(h.left) && !isRed(h.left.left)) {
            h = moveRedLeft(h);
        }

        h.left = deleteMin(h.left);
        return balance(h);
    }

    // 删除最大键
    public void deleteMax() {
        if (isEmpty()) throw new NoSuchElementException("符号表为空");
        if (!isRed(root.left) && !isRed(root.right)) {
            root.color = RED;
        }
        root = deleteMax(root);
        if (!isEmpty()) root.color = BLACK;
    }

    private Node deleteMax(Node h) {
        if (isRed(h.left)) {
            h = rotateRight(h);
        }

        if (h.right == null) return null;

        if (!isRed(h.right) && !isRed(h.right.left)) {
            h = moveRedRight(h);
        }

        h.right = deleteMax(h.right);
        return balance(h);
    }

    // 通用删除方法
    public void delete(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        if (!contains(key)) return;

        // 如果根结点的两个子结点都是黑色，先把根设为红色，方便后续操作
        if (!isRed(root.left) && !isRed(root.right)) {
            root.color = RED;
        }

        root = delete(root, key);
        if (!isEmpty()) root.color = BLACK;
    }

    private Node delete(Node h, Key key) {
        if (key.compareTo(h.key) < 0) {
            // 向左子树找
            if (!isRed(h.left) && !isRed(h.left.left)) {
                h = moveRedLeft(h);
            }
            h.left = delete(h.left, key);
        } else {
            // 左链如果是红的，先右旋，让红链转到右边
            if (isRed(h.left)) {
                h = rotateRight(h);
            }

            // 找到结点且是叶子结点，直接删除
            if (key.compareTo(h.key) == 0 && h.right == null) {
                return null;
            }

            // 如果右链是黑且右链的左链也是黑，需要从左边借一个红链
            if (!isRed(h.right) && !isRed(h.right.left)) {
                h = moveRedRight(h);
            }

            if (key.compareTo(h.key) == 0) {
                // 用后继结点替换当前结点，再删除后继
                Node minRight = min(h.right);
                h.key = minRight.key;
                h.val = minRight.val;
                h.right = deleteMin(h.right);
            } else {
                h.right = delete(h.right, key);
            }
        }

        return balance(h);
    }


    public static void main(String[] args) {
        RedBlackBST<String, Integer> st = new RedBlackBST<>();
        String[] keys = {"S", "E", "A", "R", "C", "H", "X", "M", "P", "L"};

        // 插入
        for (int i = 0; i < keys.length; i++) {
            st.put(keys[i], i);
        }

        System.out.println("size: " + st.size()); // 10
        System.out.println("min: " + st.min());   // A
        System.out.println("max: " + st.max());   // X
        System.out.println("floor(\"N\"): " + st.floor("N")); // M
        System.out.println("ceiling(\"N\"): " + st.ceiling("N")); // P
        System.out.println("rank(\"M\"): " + st.rank("M")); // 5
        System.out.println("select(5): " + st.select(5)); // M

        // 删除
        st.delete("E");
        st.deleteMin();
        st.deleteMax();
        System.out.println("after delete, size: " + st.size()); // 7
        System.out.println("new min: " + st.min()); // C
        System.out.println("new max: " + st.max()); // R
    }
}