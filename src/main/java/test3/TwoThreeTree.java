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
        root = put(root, key, value);
        // 根结点若变成临时4-结点，需要分解
        if (root.type == FOUR_NODE) {
            root = split(root);
        }
    }

    private Node put(Node x, Key key, Value value) {
        // 空树：创建一个2-结点
        if (x == null) {
            Node newNode = new Node(TWO_NODE);
            newNode.keys.add(key);
            newNode.values.add(value);
            newNode.keyCount = 1;
            return newNode;
        }

        // 1. 叶子结点：直接插入，结点可能从2-结点变为3-结点，或3-结点变为临时4-结点
        if (x.isLeaf()) {
            insertIntoNode(x, key, value);
            return x;
        }

        // 2. 非叶子结点：先递归找到要插入的子树
        int childIndex = findChildIndex(x, key);
        Node child = put(x.children.get(childIndex), key, value);
        x.children.set(childIndex, child);

        // 3. 如果递归返回的子结点是临时4-结点，需要将它分解并合并到当前结点
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

    // 向结点中插入键值对（叶子结点专用）
    private void insertIntoNode(Node node, Key key, Value value) {
        int insertPos = 0;
        // 找到插入位置，同时处理重复键（覆盖值）
        while (insertPos < node.keyCount && key.compareTo(node.keys.get(insertPos)) > 0) {
            insertPos++;
        }
        if (insertPos < node.keyCount && key.compareTo(node.keys.get(insertPos)) == 0) {
            node.values.set(insertPos, value);
            return;
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