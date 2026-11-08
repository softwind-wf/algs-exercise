package cn.exercise.algs4.datastructure.gist;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * 内存版 GiST(Generalized Search Tree)骨架。
 *
 * <p>GiST 不是某一个具体的数据结构,而是一棵<b>平衡多路搜索树</b>:每个节点是一“页”,
 * 内部条目是 {@code (包围键, 子节点)},叶子条目是 {@code (键, 值)}。
 * 键的语义由 {@link GiSTOps} 决定——只要实现“两个键怎么合并成更大的包围键”
 * 与“键是否可能命中查询”,同一副骨架就能服务不同数据类型(几何、区间、文本签名等)。</p>
 *
 * <p>本实现演示三件事:</p>
 * <ul>
 *   <li><b>下潜</b>:插入时按 {@link GiSTOps#penalty} 选扩张代价最小的子树,并把祖先键扩大成覆盖键;</li>
 *   <li><b>分裂</b>:页满时交给 {@link GiSTOps#pickSplit} 分组,父页补一条新条目,必要时逐层连锁;</li>
 *   <li><b>剪枝</b>:查询先用包围键判“可能命中”,不可能就整棵子树跳过,命中的才比较叶子条目。</li>
 * </ul>
 *
 * <p>与 PostgreSQL 实现的差异:这里是纯内存、单线程、无并发控制与 WAL,
 * 键不做压缩(省略了 compress/decompress 回调),也不做 KNN 距离排序。
 * 它演示的是“树骨架 + 回调”的分工,而不是存储引擎细节。</p>
 *
 * @param <K> 键(包围键)类型,语义由 ops 定义
 * @param <V> 叶子值类型,对应 PostgreSQL 里的 heap TID
 * @param <Q> 查询条件类型,语义由 ops 定义
 */
public final class GiST<K, V, Q> {

    /** 默认页容量,取小值是为了让分裂早出现、树形可观察 */
    private static final int DEFAULT_MAX_ENTRIES = 4;

    /** 页容量下限:再小就无法同时满足“两组都非空” */
    private static final int MIN_MAX_ENTRIES = 3;

    private final GiSTOps<K, Q> ops;
    private final int maxEntries;

    private Node root;
    private int size;
    private int lastVisitedNodes;
    private int lastConsistentChecks;

    public GiST(GiSTOps<K, Q> ops) {
        this(ops, DEFAULT_MAX_ENTRIES);
    }

    public GiST(GiSTOps<K, Q> ops, int maxEntries) {
        if (ops == null) {
            throw new NullPointerException("ops");
        }
        if (maxEntries < MIN_MAX_ENTRIES) {
            throw new IllegalArgumentException("maxEntries 至少为 " + MIN_MAX_ENTRIES);
        }
        this.ops = ops;
        this.maxEntries = maxEntries;
    }

    /** 插入一个键值对。键为 null 抛 {@link NullPointerException}。 */
    public void insert(K key, V value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (root == null) {
            root = new Node(true);
            root.keys.add(key);
            root.values.add(value);
            size++;
            return;
        }
        Node leaf = chooseLeaf(key);
        leaf.keys.add(key);
        leaf.values.add(value);
        size++;
        if (leaf.keys.size() > maxEntries) {
            split(leaf);
        }
    }

    /**
     * 返回所有“可能命中”的叶子值。是否命中最终由 ops 的 consistent 决定,
     * 本实现的键不压缩,所以不存在 lossy 重检。
     */
    public List<V> search(Q query) {
        if (query == null) {
            throw new NullPointerException("query");
        }
        lastVisitedNodes = 0;
        lastConsistentChecks = 0;
        List<V> result = new ArrayList<>();
        if (root != null) {
            searchNode(root, query, result);
        }
        return result;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** 树高;空树为 0,只有一个叶子根时为 1。 */
    public int height() {
        if (root == null) {
            return 0;
        }
        int height = 0;
        Node node = root;
        while (true) {
            height++;
            if (node.leaf || node.children.isEmpty()) {
                return height;
            }
            node = node.children.get(0);
        }
    }

    public int nodeCount() {
        if (root == null) {
            return 0;
        }
        int count = 0;
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            count++;
            for (Node child : node.children) {
                queue.add(child);
            }
        }
        return count;
    }

    public int leafCount() {
        if (root == null) {
            return 0;
        }
        int count = 0;
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.leaf) {
                count++;
            } else {
                for (Node child : node.children) {
                    queue.add(child);
                }
            }
        }
        return count;
    }

    /** 上一次查询访问了多少个节点(演示“剪枝省了多少”) */
    public int lastVisitedNodes() {
        return lastVisitedNodes;
    }

    /** 上一次查询做了多少次包围键判定 */
    public int lastConsistentChecks() {
        return lastConsistentChecks;
    }

    /** 打印树的前若干层,便于观察分裂与包围键。 */
    public String printTree(int maxDepth) {
        if (root == null) {
            return "(empty)\n";
        }
        StringBuilder builder = new StringBuilder();
        printNode(root, 0, maxDepth, builder);
        return builder.toString();
    }

    /** 覆盖不变式:每个条目的包围键都能覆盖它下面的所有键。 */
    boolean checkCoverage() {
        return root == null || checkCoverage(root);
    }

    /** 平衡不变式:所有叶子在同一层。 */
    boolean checkBalanced() {
        return root == null || leafDepth(root) != Integer.MIN_VALUE;
    }

    // ---------------------------------------------------------------- 内部实现

    private Node chooseLeaf(K key) {
        Node node = root;
        while (!node.leaf) {
            int best = 0;
            double bestPenalty = ops.penalty(node.keys.get(0), key);
            for (int i = 1; i < node.keys.size(); i++) {
                double penalty = ops.penalty(node.keys.get(i), key);
                if (penalty < bestPenalty) {
                    bestPenalty = penalty;
                    best = i;
                }
            }
            node.keys.set(best, ops.union(node.keys.get(best), key));
            node = node.children.get(best);
        }
        return node;
    }

    private void split(Node node) {
        boolean[] group = ops.pickSplit(Collections.unmodifiableList(node.keys));
        Node right = new Node(node.leaf);
        for (int i = node.keys.size() - 1; i >= 0; i--) {
            if (group[i]) {
                continue;
            }
            if (node.leaf) {
                right.keys.add(0, node.keys.remove(i));
                right.values.add(0, node.values.remove(i));
            } else {
                Node child = node.children.remove(i);
                right.keys.add(0, node.keys.remove(i));
                right.children.add(0, child);
                child.parent = right;
            }
        }
        if (node == root) {
            Node newRoot = new Node(false);
            newRoot.children.add(node);
            newRoot.children.add(right);
            newRoot.keys.add(coverKey(node));
            newRoot.keys.add(coverKey(right));
            node.parent = newRoot;
            right.parent = newRoot;
            root = newRoot;
            return;
        }
        Node parent = node.parent;
        int index = parent.children.indexOf(node);
        parent.children.add(index + 1, right);
        parent.keys.add(index + 1, coverKey(right));
        parent.keys.set(index, coverKey(node));
        right.parent = parent;
        if (parent.keys.size() > maxEntries) {
            split(parent);
        }
    }

    private K coverKey(Node node) {
        K cover = node.keys.get(0);
        for (int i = 1; i < node.keys.size(); i++) {
            cover = ops.union(cover, node.keys.get(i));
        }
        return cover;
    }

    private void searchNode(Node node, Q query, List<V> result) {
        lastVisitedNodes++;
        for (int i = 0; i < node.keys.size(); i++) {
            lastConsistentChecks++;
            if (!ops.consistent(node.keys.get(i), query)) {
                continue;
            }
            if (node.leaf) {
                result.add(node.values.get(i));
            } else {
                searchNode(node.children.get(i), query, result);
            }
        }
    }

    private boolean checkCoverage(Node node) {
        if (node.leaf) {
            return true;
        }
        for (int i = 0; i < node.children.size(); i++) {
            Node child = node.children.get(i);
            K parentKey = node.keys.get(i);
            for (K childKey : child.keys) {
                if (!ops.equal(ops.union(parentKey, childKey), parentKey)) {
                    return false;
                }
            }
            if (!checkCoverage(child)) {
                return false;
            }
        }
        return true;
    }

    private int leafDepth(Node node) {
        if (node.leaf) {
            return 0;
        }
        int depth = -1;
        for (Node child : node.children) {
            int childDepth = leafDepth(child);
            if (childDepth == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            childDepth++;
            if (depth == -1) {
                depth = childDepth;
            } else if (depth != childDepth) {
                return Integer.MIN_VALUE;
            }
        }
        return depth == -1 ? 0 : depth;
    }

    private void printNode(Node node, int level, int maxDepth, StringBuilder builder) {
        indent(builder, level);
        builder.append(node.leaf ? "叶子页" : "内部页");
        builder.append(" keys=").append(shorten(node.keys));
        if (!node.leaf) {
            builder.append(" children=").append(node.children.size());
        }
        builder.append('\n');
        if (level >= maxDepth || node.leaf) {
            return;
        }
        for (Node child : node.children) {
            printNode(child, level + 1, maxDepth, builder);
        }
    }

    private void indent(StringBuilder builder, int level) {
        for (int i = 0; i < level; i++) {
            builder.append("    ");
        }
    }

    private String shorten(List<K> keys) {
        StringBuilder builder = new StringBuilder("[");
        int limit = Math.min(3, keys.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(keys.get(i));
        }
        if (keys.size() > limit) {
            builder.append(", ...");
        }
        return builder.append(']').toString();
    }

    /** 一个“页”:内部节点用 children,叶子节点用 values,两者都用 keys 与之对齐。 */
    private final class Node {

        private final boolean leaf;
        private final List<K> keys = new ArrayList<>();
        private final List<Node> children = new ArrayList<>();
        private final List<V> values = new ArrayList<>();
        private Node parent;

        private Node(boolean leaf) {
            this.leaf = leaf;
        }
    }
}
