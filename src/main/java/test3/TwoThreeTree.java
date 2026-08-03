package test3;

import java.util.ArrayList;
import java.util.List;

public class TwoThreeTree<Key extends Comparable<Key>, Value> {

    // 结点类型标记
    private static final int TWO_NODE = 2;
    private static final int THREE_NODE = 3;
    private static final int FOUR_NODE = 4; // 临时4-结点，仅用于插入过程

    // 内部结点类（用List代替数组，彻底解决泛型数组问题）
    private class Node {
        int type;
        List<Key> keys;
        List<Value> values;
        List<Node> children;
        int keyCount;

        Node(int type) {
            this.type = type;
            this.keys = new ArrayList<>(3);   // 预分配最大容量3
            this.values = new ArrayList<>(3);
            this.children = new ArrayList<>(4); // 预分配最大容量4
            this.keyCount = 0;
        }

        // 是否为外部结点（叶子结点，所有子结点为空）
        boolean isLeaf() {
            return children.isEmpty();
        }
    }

    private Node root;
    private int size;   // 键值对总数

    public TwoThreeTree() {
        root = null;
    }

    // 查找键对应的值
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        return get(root, key);
    }

    private Value get(Node x, Key key) {
        if (x == null) return null;

        // 遍历当前结点的键，判断查找方向
        for (int i = 0; i < x.keyCount; i++) {
            int cmp = key.compareTo(x.keys.get(i));
            if (cmp == 0) return x.values.get(i);
            if (cmp < 0) {
                if (i < x.children.size()) {
                    return get(x.children.get(i), key);
                }
                return null;
            }
        }
        // 大于所有键，走最右链
        if (x.keyCount < x.children.size()) {
            return get(x.children.get(x.keyCount), key);
        }
        return null;
    }

    // 插入键值对
    public void put(Key key, Value value) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        boolean[] insertedNewKey = {false};
        root = put(root, key, value, insertedNewKey);
        // 根结点若变成临时4-结点，需要分解
        if (root.type == FOUR_NODE) {
            root = split(root);
        }
        if (insertedNewKey[0]) {
            size++;
        }
    }

    private Node put(Node x, Key key, Value value, boolean[] insertedNewKey) {
        // 空树：创建一个2-结点
        if (x == null) {
            Node newNode = new Node(TWO_NODE);
            newNode.keys.add(key);
            newNode.values.add(value);
            newNode.keyCount = 1;
            insertedNewKey[0] = true;
            return newNode;
        }

        // 1. 叶子结点：直接插入，结点可能从2-结点变为3-结点，或3-结点变为临时4-结点
        if (x.isLeaf()) {
            if (insertIntoNode(x, key, value)) {
                insertedNewKey[0] = true;
            }
            return x;
        }

        // 2. 内部结点：若键已存在，原地更新值，避免重复键被当作新键下潜插入（否则树中会出现两个相同键）
        for (int i = 0; i < x.keyCount; i++) {
            if (key.compareTo(x.keys.get(i)) == 0) {
                x.values.set(i, value);
                return x;
            }
        }

        // 3. 键不在当前结点：递归找到要插入的子树
        int childIndex = findChildIndex(x, key);
        Node child = put(x.children.get(childIndex), key, value, insertedNewKey);
        x.children.set(childIndex, child);

        // 4. 如果递归返回的子结点是临时4-结点，需要将它分解并合并到当前结点
        if (child.type == FOUR_NODE) {
            x = mergeFourNode(x, childIndex, child);
        }
        return x;
    }

    // 找到key应该走的子结点索引
    private int findChildIndex(Node x, Key key) {
        for (int i = 0; i < x.keyCount; i++) {
            if (key.compareTo(x.keys.get(i)) < 0) {
                return i;
            }
        }
        return x.keyCount;
    }

    // 向结点中插入键值对（叶子结点专用）。返回 true 表示新增键；false 表示键已存在仅更新值。
    private boolean insertIntoNode(Node node, Key key, Value value) {
        int insertPos = 0;
        // 找到插入位置，同时处理重复键（覆盖值）
        while (insertPos < node.keyCount && key.compareTo(node.keys.get(insertPos)) > 0) {
            insertPos++;
        }
        if (insertPos < node.keyCount && key.compareTo(node.keys.get(insertPos)) == 0) {
            node.values.set(insertPos, value);
            return false;
        }
        // 插入键值对
        node.keys.add(insertPos, key);
        node.values.add(insertPos, value);
        node.keyCount++;
        // 更新结点类型
        if (node.keyCount == 2) {
            node.type = THREE_NODE;
        } else if (node.keyCount == 3) {
            node.type = FOUR_NODE;
        }
        return true;
    }

    // 合并子结点的临时4-结点到当前结点
    private Node mergeFourNode(Node parent, int childIndex, Node fourNode) {
        Key midKey = fourNode.keys.get(1);
        Value midValue = fourNode.values.get(1);

        // 将中间键插入父结点
        insertIntoNode(parent, midKey, midValue);

        // 分解4-结点为两个2-结点
        Node left = new Node(TWO_NODE);
        left.keys.add(fourNode.keys.get(0));
        left.values.add(fourNode.values.get(0));
        if (fourNode.children.size() > 0) {
            left.children.add(fourNode.children.get(0));
        }
        if (fourNode.children.size() > 1) {
            left.children.add(fourNode.children.get(1));
        }
        left.keyCount = 1;

        Node right = new Node(TWO_NODE);
        right.keys.add(fourNode.keys.get(2));
        right.values.add(fourNode.values.get(2));
        if (fourNode.children.size() > 2) {
            right.children.add(fourNode.children.get(2));
        }
        if (fourNode.children.size() > 3) {
            right.children.add(fourNode.children.get(3));
        }
        right.keyCount = 1;

        // 替换父结点的子结点链
        parent.children.set(childIndex, left);
        parent.children.add(childIndex + 1, right);

        return parent;
    }

    // 分解根结点的临时4-结点
    private Node split(Node fourNode) {
        Key midKey = fourNode.keys.get(1);
        Value midValue = fourNode.values.get(1);

        Node newRoot = new Node(TWO_NODE);
        newRoot.keys.add(midKey);
        newRoot.values.add(midValue);
        newRoot.keyCount = 1;

        Node left = new Node(TWO_NODE);
        left.keys.add(fourNode.keys.get(0));
        left.values.add(fourNode.values.get(0));
        if (fourNode.children.size() > 0) {
            left.children.add(fourNode.children.get(0));
        }
        if (fourNode.children.size() > 1) {
            left.children.add(fourNode.children.get(1));
        }
        left.keyCount = 1;

        Node right = new Node(TWO_NODE);
        right.keys.add(fourNode.keys.get(2));
        right.values.add(fourNode.values.get(2));
        if (fourNode.children.size() > 2) {
            right.children.add(fourNode.children.get(2));
        }
        if (fourNode.children.size() > 3) {
            right.children.add(fourNode.children.get(3));
        }
        right.keyCount = 1;

        newRoot.children.add(left);
        newRoot.children.add(right);
        return newRoot;
    }

    // ==================== 删除 ====================

    /** 删除键对应的键值对。返回 true 表示键存在并已删除；false 表示键不存在。 */
    public boolean delete(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        if (root == null) return false;
        boolean[] found = {false};
        root = delete(root, key, found);
        // 根结点可能被合并清空：提升唯一的孩子为新根（若连孩子也没有则整树为空）
        if (root != null && root.keyCount == 0) {
            root = root.children.isEmpty() ? null : root.children.get(0);
        }
        if (found[0]) {
            size--;
        }
        return found[0];
    }

    /**
     * 递归自底向上删除：键被删后叶子可能变空，由父结点通过借键（旋转）或合并来修复。
     * 返回新的子树根；若 2-结点父结点被合并清空（0个键、1个孩子），返回空结点交由上层继续修复。
     */
    private Node delete(Node x, Key key, boolean[] found) {
        int i = 0;
        while (i < x.keyCount && key.compareTo(x.keys.get(i)) > 0) {
            i++;
        }
        if (i < x.keyCount && key.compareTo(x.keys.get(i)) == 0) {
            found[0] = true;
            if (x.isLeaf()) {
                x.keys.remove(i);
                x.values.remove(i);
                x.keyCount--;
                updateType(x);
                return x;   // 可能变成空叶子，由父结点修复
            }
            // 内部结点：用左子树最大值（前驱）替换，再递归删除前驱
            Key pred = maxKey(x.children.get(i));
            Value predVal = maxValue(x.children.get(i));
            x.keys.set(i, pred);
            x.values.set(i, predVal);
            Node result = delete(x.children.get(i), pred, found);
            x.children.set(i, result);
            if (result.keyCount == 0) {
                return fixEmptyChild(x, i);
            }
            return x;
        }
        if (x.isLeaf()) {
            return x;   // 未找到：不做任何修改
        }
        int j = findChildIndex(x, key);
        Node result = delete(x.children.get(j), key, found);
        x.children.set(j, result);
        if (result.keyCount == 0) {
            return fixEmptyChild(x, j);
        }
        return x;
    }

    /** 修复父结点中变空的孩子（0个键）：先尝试从兄弟借键（旋转），否则与兄弟合并并拉下父键。
     *  若父结点因此被合并清空（2-结点合并后只剩一个孩子），返回空父结点交由上层继续修复。 */
    private Node fixEmptyChild(Node parent, int j) {
        // 左兄弟有富余键：右旋借一个
        if (j > 0 && parent.children.get(j - 1).keyCount >= 2) {
            rotateRight(parent, j);
            return parent;
        }
        // 右兄弟有富余键：左旋借一个
        if (j < parent.children.size() - 1 && parent.children.get(j + 1).keyCount >= 2) {
            rotateLeft(parent, j);
            return parent;
        }
        // 兄弟也都是2-结点：合并（空结点 + 兄弟 + 父键 = 3-结点，不会产生4-结点）
        if (j > 0) {
            mergeChildren(parent, j - 1);
        } else {
            mergeChildren(parent, j);
        }
        if (parent.keyCount == 0) {
            return parent;   // 2-结点被合并清空，向上传播
        }
        return parent;
    }

    /** 右旋：把 parent.keys[j-1] 下沉到空孩子，左兄弟最大键上浮到父，左兄弟最右孩子转给空孩子。 */
    private void rotateRight(Node parent, int j) {
        Node child = parent.children.get(j);
        Node left = parent.children.get(j - 1);
        child.keys.add(0, parent.keys.get(j - 1));
        child.values.add(0, parent.values.get(j - 1));
        child.keyCount++;
        if (!left.children.isEmpty()) {
            child.children.add(0, left.children.get(left.keyCount));
        }
        parent.keys.set(j - 1, left.keys.get(left.keyCount - 1));
        parent.values.set(j - 1, left.values.get(left.keyCount - 1));
        left.keys.remove(left.keyCount - 1);
        left.values.remove(left.keyCount - 1);
        if (!left.children.isEmpty()) {
            left.children.remove(left.keyCount);
        }
        left.keyCount--;
        updateType(left);
        updateType(child);
    }

    /** 左旋：把 parent.keys[j] 下沉到空孩子末尾，右兄弟最小键上浮到父，右兄弟最左孩子转给空孩子。 */
    private void rotateLeft(Node parent, int j) {
        Node child = parent.children.get(j);
        Node right = parent.children.get(j + 1);
        child.keys.add(parent.keys.get(j));
        child.values.add(parent.values.get(j));
        child.keyCount++;
        if (!right.children.isEmpty()) {
            child.children.add(right.children.get(0));
        }
        parent.keys.set(j, right.keys.get(0));
        parent.values.set(j, right.values.get(0));
        right.keys.remove(0);
        right.values.remove(0);
        if (!right.children.isEmpty()) {
            right.children.remove(0);
        }
        right.keyCount--;
        updateType(right);
        updateType(child);
    }

    /** 合并 parent.children[i] 与 children[i+1]，把父键 keys[i] 拉入合并结点；返回合并后的结点。 */
    private Node mergeChildren(Node parent, int i) {
        Node left = parent.children.get(i);
        Node right = parent.children.get(i + 1);
        left.keys.add(parent.keys.get(i));
        left.values.add(parent.values.get(i));
        for (int k = 0; k < right.keyCount; k++) {
            left.keys.add(right.keys.get(k));
            left.values.add(right.values.get(k));
        }
        left.keyCount = left.keys.size();
        for (Node c : right.children) {
            left.children.add(c);
        }
        updateType(left);
        // 从父结点移除该键与右孩子
        parent.keys.remove(i);
        parent.values.remove(i);
        parent.children.remove(i + 1);
        parent.keyCount--;
        updateType(parent);
        return left;
    }

    /** 子树中的最大键（沿最右链下行到叶子） */
    private Key maxKey(Node x) {
        while (!x.isLeaf()) {
            x = x.children.get(x.keyCount);
        }
        return x.keys.get(x.keyCount - 1);
    }

    /** 子树中的最大键对应的值 */
    private Value maxValue(Node x) {
        while (!x.isLeaf()) {
            x = x.children.get(x.keyCount);
        }
        return x.values.get(x.keyCount - 1);
    }

    /** 根据键数刷新结点类型 */
    private void updateType(Node n) {
        if (n.keyCount <= 1) {
            n.type = TWO_NODE;
        } else if (n.keyCount == 2) {
            n.type = THREE_NODE;
        } else {
            n.type = FOUR_NODE;
        }
    }

    // ==================== 基础操作 ====================

    /** 符号表中的键值对总数 */
    public int size() {
        return size;
    }

    /** 符号表是否为空 */
    public boolean isEmpty() {
        return size == 0;
    }

    /** 是否包含指定键（按键比较，与值是否为 null 无关） */
    public boolean contains(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        return contains(root, key);
    }

    private boolean contains(Node x, Key key) {
        while (x != null) {
            int i = 0;
            while (i < x.keyCount && key.compareTo(x.keys.get(i)) > 0) {
                i++;
            }
            if (i < x.keyCount && key.compareTo(x.keys.get(i)) == 0) {
                return true;
            }
            if (x.isLeaf()) {
                return false;
            }
            x = x.children.get(i);
        }
        return false;
    }

    // ==================== 最大/最小键 ====================

    /** 最小键；空表返回 null */
    public Key min() {
        if (root == null) return null;
        Node x = root;
        while (!x.isLeaf()) {
            x = x.children.get(0);
        }
        return x.keys.get(0);
    }

    /** 最大键；空表返回 null */
    public Key max() {
        if (root == null) return null;
        Node x = root;
        while (!x.isLeaf()) {
            x = x.children.get(x.keyCount);
        }
        return x.keys.get(x.keyCount - 1);
    }

    // ==================== 向下取整/向上取整 ====================

    /** 小于等于 key 的最大键；不存在返回 null */
    public Key floor(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        return floor(root, key, null);
    }

    private Key floor(Node x, Key key, Key best) {
        if (x == null) return best;
        for (int i = 0; i < x.keyCount; i++) {
            int cmp = key.compareTo(x.keys.get(i));
            if (cmp == 0) return x.keys.get(i);
            if (cmp < 0) {
                if (!x.isLeaf()) return floor(x.children.get(i), key, best);
                return best;
            }
            best = x.keys.get(i);
        }
        if (!x.isLeaf()) return floor(x.children.get(x.keyCount), key, best);
        return best;
    }

    /** 大于等于 key 的最小键；不存在返回 null */
    public Key ceiling(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        return ceiling(root, key, null);
    }

    private Key ceiling(Node x, Key key, Key best) {
        if (x == null) return best;
        for (int i = 0; i < x.keyCount; i++) {
            int cmp = key.compareTo(x.keys.get(i));
            if (cmp == 0) return x.keys.get(i);
            if (cmp < 0) {
                if (!x.isLeaf()) return ceiling(x.children.get(i), key, x.keys.get(i));
                return x.keys.get(i);
            }
        }
        if (!x.isLeaf()) return ceiling(x.children.get(x.keyCount), key, best);
        return best;
    }

    // ==================== 选择/排名 ====================

    /** 排名为 k 的键（k 从 0 开始）；越界返回 null */
    public Key select(int k) {
        if (k < 0 || k >= size) return null;
        return select(root, k);
    }

    private Key select(Node x, int k) {
        for (int i = 0; i < x.keyCount; i++) {
            int s = x.isLeaf() ? 0 : sizeOf(x.children.get(i));
            if (k < s) return select(x.children.get(i), k);
            k -= s;
            if (k == 0) return x.keys.get(i);
            k--;
        }
        if (!x.isLeaf()) return select(x.children.get(x.keyCount), k);
        return null;
    }

    /** 小于 key 的键的数量 */
    public int rank(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        return rank(root, key);
    }

    private int rank(Node x, Key key) {
        if (x == null) return 0;
        int count = 0;
        for (int i = 0; i < x.keyCount; i++) {
            int cmp = key.compareTo(x.keys.get(i));
            if (cmp == 0) {
                if (!x.isLeaf()) count += sizeOf(x.children.get(i));
                return count;
            }
            if (cmp < 0) {
                if (!x.isLeaf()) return count + rank(x.children.get(i), key);
                return count;
            }
            count += 1;
            if (!x.isLeaf()) count += sizeOf(x.children.get(i));
        }
        if (!x.isLeaf()) return count + rank(x.children.get(x.keyCount), key);
        return count;
    }

    /** 子树中的键数 */
    private int sizeOf(Node x) {
        if (x == null) return 0;
        int count = x.keyCount;
        for (Node c : x.children) {
            count += sizeOf(c);
        }
        return count;
    }

    // ==================== 删除最值 ====================

    /** 删除最小键 */
    public void deleteMin() {
        if (root == null) return;
        Key minKey = min();
        if (minKey != null) delete(minKey);
    }

    /** 删除最大键 */
    public void deleteMax() {
        if (root == null) return;
        Key maxKey = max();
        if (maxKey != null) delete(maxKey);
    }

    // ==================== 范围操作 ====================

    /** [lo, hi] 范围内的键的数量 */
    public int size(Key lo, Key hi) {
        if (lo == null || hi == null) throw new IllegalArgumentException("lo/hi不能为null");
        if (lo.compareTo(hi) > 0) return 0;
        int result = rank(hi) - rank(lo);
        if (contains(lo)) result++;
        return result;
    }

    /** [lo, hi] 范围内的所有键（按升序） */
    public Iterable<Key> keys(Key lo, Key hi) {
        if (lo == null || hi == null) throw new IllegalArgumentException("lo/hi不能为null");
        List<Key> list = new ArrayList<>();
        keys(root, list, lo, hi);
        return list;
    }

    private void keys(Node x, List<Key> list, Key lo, Key hi) {
        if (x == null) return;
        // 最左子树：仅当第一个键 >= lo 时其中才可能有范围内的键
        if (!x.isLeaf() && lo.compareTo(x.keys.get(0)) <= 0) {
            keys(x.children.get(0), list, lo, hi);
        }
        for (int i = 0; i < x.keyCount; i++) {
            if (lo.compareTo(x.keys.get(i)) <= 0 && hi.compareTo(x.keys.get(i)) >= 0) {
                list.add(x.keys.get(i));
            }
            // 第 i 个键的右子树：仅当该键 <= hi 时其中才可能有范围内的键
            if (!x.isLeaf() && hi.compareTo(x.keys.get(i)) >= 0) {
                keys(x.children.get(i + 1), list, lo, hi);
            }
        }
    }

    // 有序遍历（中序）
    public Iterable<Key> keys() {
        List<Key> list = new ArrayList<>();
        inOrder(root, list);
        return list;
    }

    private void inOrder(Node x, List<Key> list) {
        if (x == null) return;
        for (int i = 0; i < x.keyCount; i++) {
            if (!x.children.isEmpty() && i < x.children.size()) {
                inOrder(x.children.get(i), list);
            }
            list.add(x.keys.get(i));
        }
        if (!x.children.isEmpty() && x.keyCount < x.children.size()) {
            inOrder(x.children.get(x.keyCount), list);
        }
    }

    // 测试用例
    public static void main(String[] args) {
        TwoThreeTree<String, Integer> st = new TwoThreeTree<>();

        // 插入示例数据（和书上的例子一致）
        String[] keys = {"S", "E", "A", "R", "C", "H", "X", "M", "P", "L"};
        for (int i = 0; i < keys.length; i++) {
            st.put(keys[i], i);
        }

        // 查找测试
        System.out.println("查找H: " + st.get("H"));
        System.out.println("查找M: " + st.get("M"));
        System.out.println("查找Z: " + st.get("Z"));

        // 有序遍历
        System.out.println("有序遍历结果：");
        for (String k : st.keys()) {
            System.out.print(k + " ");
        }
    }
}