package com.ds.btree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * B 树 —— 2-3-4 树的一般化(CLRS 第 18 章)
 *
 * 最小度数为 t 的 B 树满足:
 *  - 每个内部节点(除根)至少 t-1 个键、至多 2t-1 个键;
 *  - 根至少 1 个键(空树除外)、至多 2t-1 个键;
 *  - 含 n 个键的节点有 n+1 个孩子(叶子除外);
 *  - 所有叶子位于同一层,因此树始终完美平衡,增删查都是 O(log_t n)。
 *
 * 当 t = 2 时,节点容量为 1~3 个键,即一棵 2-3-4 树——
 * 本实现就是 com.ds.twothreefourtree.TwoThreeFourTree 的 t 参数化推广,
 * 插入、删除的"自顶向下"策略完全一致,只是把固定容量推广到 2t-1。
 *
 * 插入:自顶向下分裂。下行时遇到满(2t-1 键)节点先分裂,中间键上浮到父,
 *       到达的叶子必不满,可直接插入,无需回溯。
 * 删除:自顶向下保障。下潜前用借键(旋转)或合并保证目标孩子至少 t 个键,
 *       因此命中键后不会下溢;内部节点用前驱/后继替换,或合并两侧孩子就地删除。
 */
public class BTree {

    private final int t;           // 最小度数,>= 2
    private final int maxKeys;     // 2t-1
    private final int minKeys;     // t-1
    private Node root;
    private int size;

    public BTree() {
        this(2);
    }

    public BTree(int t) {
        if (t < 2) {
            throw new IllegalArgumentException("最小度数 t 必须 >= 2,实际 " + t);
        }
        this.t = t;
        this.maxKeys = 2 * t - 1;
        this.minKeys = t - 1;
    }

    /** B 树节点:keys[0..n-1] 升序存放键,children[0..n] 为子树指针;叶子 children 全为 null。 */
    private class Node {
        final int[] keys = new int[maxKeys];
        final Node[] children = new Node[maxKeys + 1];
        int n;

        Node() {
        }
    }

    // ==================== 增查 ====================

    /** 插入一个键(不允许重复)。返回 true 插入成功;false 键已存在。 */
    public boolean insert(int key) {
        if (root == null) {
            root = new Node();
            root.keys[0] = key;
            root.n = 1;
            size++;
            return true;
        }
        // 根没有父节点可承接上浮的键,若已满必须先单独分裂(树增高一层)
        if (root.n == maxKeys) {
            splitRoot();
        }

        Node cur = root;
        while (true) {
            int i = 0;
            while (i < cur.n && key > cur.keys[i]) {
                i++;
            }
            if (i < cur.n && key == cur.keys[i]) {
                return false;               // 重复键
            }
            if (cur.children[0] == null) {
                insertKey(cur, i, key);     // 叶子:下行时已保证不满,直接插入
                size++;
                return true;
            }
            if (cur.children[i].n == maxKeys) {
                splitChild(cur, i);         // 下行孩子已满:先分裂,再重新定位
                continue;
            }
            cur = cur.children[i];
        }
    }

    public boolean contains(int key) {
        return get(key) != null;
    }

    /** 查找指定键,不存在返回 null */
    public Integer get(int key) {
        Node cur = root;
        while (cur != null) {
            int i = 0;
            while (i < cur.n && key > cur.keys[i]) {
                i++;
            }
            if (i < cur.n && key == cur.keys[i]) {
                return cur.keys[i];
            }
            cur = cur.children[i];
        }
        return null;
    }

    // ==================== 统计信息 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        root = null;
        size = 0;
    }

    /** 树高(空树 -1;单节点 0)。B 树完美平衡,沿最左路径下行深度即树高。 */
    public int height() {
        if (root == null) {
            return -1;
        }
        int h = 0;
        Node cur = root;
        while (cur.children[0] != null) {
            h++;
            cur = cur.children[0];
        }
        return h;
    }

    public Integer findMin() {
        if (root == null) {
            return null;
        }
        Node cur = root;
        while (cur.children[0] != null) {
            cur = cur.children[0];
        }
        return cur.keys[0];
    }

    public Integer findMax() {
        if (root == null) {
            return null;
        }
        Node cur = root;
        while (cur.children[cur.n] != null) {
            cur = cur.children[cur.n];
        }
        return cur.keys[cur.n - 1];
    }

    // ==================== 遍历 ====================

    /** 中序遍历(左子树、键、右子树交替),结果为升序序列 */
    public List<Integer> inorder() {
        List<Integer> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    /** 层序遍历(广度优先),节点内部按键升序输出 */
    public List<Integer> levelOrder() {
        List<Integer> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            for (int i = 0; i < cur.n; i++) {
                result.add(cur.keys[i]);
            }
            for (int i = 0; i <= cur.n; i++) {
                if (cur.children[i] != null) {
                    queue.offer(cur.children[i]);
                }
            }
        }
        return result;
    }

    public int[] toArray() {
        List<Integer> list = inorder();
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public List<Integer> toList() {
        return inorder();
    }

    @Override
    public String toString() {
        return inorder().toString();
    }

    // ==================== 自顶向下分裂 ====================

    /** 分裂根节点(2t-1 键)。根没有父节点可承接上浮的键,必须单独处理,树增高一层。 */
    private void splitRoot() {
        Node old = root;
        Node left = new Node();
        Node right = new Node();
        System.arraycopy(old.keys, 0, left.keys, 0, minKeys);      // keys[0..t-2]
        System.arraycopy(old.children, 0, left.children, 0, t);    // children[0..t-1]
        left.n = minKeys;
        System.arraycopy(old.keys, t, right.keys, 0, minKeys);     // keys[t..2t-2]
        System.arraycopy(old.children, t, right.children, 0, t);   // children[t..2t-1]
        right.n = minKeys;

        Node newRoot = new Node();
        newRoot.keys[0] = old.keys[t - 1];
        newRoot.children[0] = left;
        newRoot.children[1] = right;
        newRoot.n = 1;
        root = newRoot;
    }

    /** 分裂 parent 的第 i 个满孩子(2t-1 键),中间键上浮进 parent.keys[i],
     *  左、右两半分别挂在 parent.children[i]、children[i+1]。调用前提:parent 不满。 */
    private void splitChild(Node parent, int i) {
        Node child = parent.children[i];
        Node right = new Node();
        System.arraycopy(child.keys, t, right.keys, 0, minKeys);    // 右半键
        System.arraycopy(child.children, t, right.children, 0, t);  // 右半孩子
        right.n = minKeys;
        child.n = minKeys;                                          // 左半保留 keys[0..t-2]

        for (int j = parent.n; j > i; j--) {
            parent.keys[j] = parent.keys[j - 1];
        }
        for (int j = parent.n + 1; j > i + 1; j--) {
            parent.children[j] = parent.children[j - 1];
        }
        parent.keys[i] = child.keys[t - 1];
        parent.children[i] = child;
        parent.children[i + 1] = right;
        parent.n++;
    }

    /** 将键插入到节点 keys 数组的第 i 个位置(调用前该节点保证未满) */
    private void insertKey(Node node, int i, int key) {
        for (int j = node.n; j > i; j--) {
            node.keys[j] = node.keys[j - 1];
        }
        node.keys[i] = key;
        node.n++;
    }

    // ==================== 删除 ====================

    /** 删除指定键。返回 true 表示键存在且已删除;false 表示键不存在。 */
    public boolean remove(int key) {
        if (root == null) {
            return false;
        }
        boolean found = removeRec(root, key);
        if (found) {
            size--;
            // 整棵树只有一个键且被删除:根变空
            if (root.n == 0) {
                root = null;
            }
        }
        return found;
    }

    /**
     * 自顶向下删除:下潜过程中保证除根外每个节点至少 t 个键。
     * 遇到 t-1 键的孩子先借键(旋转)或合并,因此删除后不会下溢,无需回溯修复。
     * 内部节点删除:用前驱/后继键替换后再递归删除;若两侧孩子都只有 t-1 键,
     * 则把待删键沉入合并节点,直接在合并节点内删除。
     */
    private boolean removeRec(Node node, int key) {
        int i = 0;
        while (i < node.n && key > node.keys[i]) {
            i++;
        }
        if (i < node.n && node.keys[i] == key) {
            if (node.children[0] == null) {
                removeKeyAt(node, i);
                return true;
            }
            // 内部节点:优先用左子树最大值(前驱)替换并删除之
            if (node.children[i].n >= t) {
                int pred = subtreeMax(node.children[i]);
                node.keys[i] = pred;
                return removeRec(node.children[i], pred);
            }
            // 左侧不足但右侧有富余:改用右子树最小值(后继)
            if (node.children[i + 1].n >= t) {
                int succ = subtreeMin(node.children[i + 1]);
                node.keys[i] = succ;
                return removeRec(node.children[i + 1], succ);
            }
            // 两侧都只有 t-1 键:合并,待删键沉入合并节点后在其中直接删除
            mergeRight(node, i);
            if (node == root && node.n == 0) {
                root = node.children[0];    // 根被合并清空,树降低一层
            }
            return removeRec(node.children[i], key);
        }
        if (node.children[0] == null) {
            return false;                   // 到叶子仍未找到
        }
        Node child = fixChild(node, i);
        return removeRec(child, key);
    }

    /** 确保 parent.children[i] 至少 t 个键:先尝试从兄弟借键(旋转),否则与兄弟合并。
     *  返回继续下潜的孩子节点(合并后下标/对象可能改变;根被合并清空时返回新根)。 */
    private Node fixChild(Node parent, int i) {
        Node child = parent.children[i];
        if (child.n >= t) {
            return child;
        }
        // 左兄弟有富余键:右旋借一个
        if (i > 0 && parent.children[i - 1].n >= t) {
            rotateRight(parent, i);
            return parent.children[i];
        }
        // 右兄弟有富余键:左旋借一个
        if (i < parent.n && parent.children[i + 1].n >= t) {
            rotateLeft(parent, i);
            return parent.children[i];
        }
        // 兄弟也都不足:合并并把父键拉下来
        if (i > 0) {
            mergeRight(parent, i - 1);
            if (parent == root && parent.n == 0) {
                root = parent.children[0];
            }
            return parent.children[i - 1];
        } else {
            mergeRight(parent, i);
            if (parent == root && parent.n == 0) {
                root = parent.children[0];
            }
            return parent.children[i];
        }
    }

    /** 左旋:把 parent.keys[i] 下沉到 child 末尾,右兄弟最小键上浮到父,右兄弟首孩子移入 child。 */
    private void rotateLeft(Node parent, int i) {
        Node child = parent.children[i];
        Node right = parent.children[i + 1];
        child.keys[child.n] = parent.keys[i];
        child.children[child.n + 1] = right.children[0];
        child.n++;
        parent.keys[i] = right.keys[0];
        for (int j = 0; j < right.n - 1; j++) {
            right.keys[j] = right.keys[j + 1];
        }
        for (int j = 0; j < right.n; j++) {
            right.children[j] = right.children[j + 1];
        }
        right.n--;
        right.children[right.n + 1] = null;    // 清失效的旧末位槽,而非有效孩子
    }

    /** 右旋:把 parent.keys[i-1] 下沉到 child 开头,左兄弟最大键上浮到父,左兄弟末孩子移入 child。 */
    private void rotateRight(Node parent, int i) {
        Node child = parent.children[i];
        Node left = parent.children[i - 1];
        for (int j = child.n; j > 0; j--) {
            child.keys[j] = child.keys[j - 1];
        }
        child.keys[0] = parent.keys[i - 1];
        for (int j = child.n + 1; j > 0; j--) {
            child.children[j] = child.children[j - 1];
        }
        child.children[0] = left.children[left.n];
        child.n++;
        parent.keys[i - 1] = left.keys[left.n - 1];
        left.n--;
        left.children[left.n + 1] = null;      // 清失效的旧末位槽,而非有效孩子
    }

    /** 合并 parent.children[i] 与 children[i+1] 到左边,并把 parent.keys[i] 拉入合并节点。 */
    private void mergeRight(Node parent, int i) {
        Node left = parent.children[i];
        Node right = parent.children[i + 1];
        left.keys[left.n] = parent.keys[i];
        for (int j = 0; j < right.n; j++) {
            left.keys[left.n + 1 + j] = right.keys[j];
        }
        for (int j = 0; j <= right.n; j++) {
            left.children[left.n + 1 + j] = right.children[j];
        }
        left.n += 1 + right.n;
        for (int j = i; j < parent.n - 1; j++) {
            parent.keys[j] = parent.keys[j + 1];
        }
        for (int j = i + 1; j < parent.n; j++) {
            parent.children[j] = parent.children[j + 1];
        }
        parent.children[parent.n] = null;
        parent.n--;
    }

    private void removeKeyAt(Node node, int i) {
        for (int j = i; j < node.n - 1; j++) {
            node.keys[j] = node.keys[j + 1];
        }
        node.n--;
    }

    /** 子树中的最大键(沿最右路径下行到叶子) */
    private int subtreeMax(Node node) {
        while (node.children[node.n] != null) {
            node = node.children[node.n];
        }
        return node.keys[node.n - 1];
    }

    /** 子树中的最小键(沿最左路径下行到叶子) */
    private int subtreeMin(Node node) {
        while (node.children[0] != null) {
            node = node.children[0];
        }
        return node.keys[0];
    }

    // ==================== 内部方法 ====================

    /** 中序遍历递归 */
    private void inorder(Node node, List<Integer> list) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < node.n; i++) {
            inorder(node.children[i], list);
            list.add(node.keys[i]);
        }
        inorder(node.children[node.n], list);
    }

    // ==================== 结构校验(供测试与排错使用) ====================

    /** 校验结构不变量:键数区间、键有序、BST 约束、叶子同层、size 一致。null 表示合法。 */
    String checkStructure() {
        if (root == null) {
            return size == 0 ? null : "root 为空但 size=" + size;
        }
        int[] leafDepth = {-1};
        int[] count = {0};
        String err = checkNode(root, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, true, leafDepth, count);
        if (err != null) {
            return err;
        }
        return count[0] == size ? null : "实际键数 " + count[0] + " 与 size " + size + " 不一致";
    }

    private String checkNode(Node node, int lo, int hi, int depth, boolean isRoot, int[] leafDepth, int[] count) {
        count[0] += node.n;
        int min = isRoot ? 1 : minKeys;
        if (node.n < min || node.n > maxKeys) {
            return "键数 " + node.n + " 超出 [" + min + "," + maxKeys + "]";
        }
        for (int i = 1; i < node.n; i++) {
            if (node.keys[i] <= node.keys[i - 1]) {
                return "节点内键未严格递增";
            }
        }
        for (int i = 0; i < node.n; i++) {
            if (node.keys[i] <= lo || node.keys[i] >= hi) {
                return "键值越界 (lo=" + lo + ", hi=" + hi + ")";
            }
        }
        boolean leaf = node.children[0] == null;
        for (int i = 0; i <= node.n; i++) {
            if (leaf != (node.children[i] == null)) {
                return "叶子/内部节点结构不一致";
            }
        }
        if (leaf) {
            if (leafDepth[0] == -1) {
                leafDepth[0] = depth;
            } else if (leafDepth[0] != depth) {
                return "叶子深度不一致:期望 " + leafDepth[0] + ",实际 " + depth;
            }
            return null;
        }
        for (int i = 0; i <= node.n; i++) {
            int childLo = (i == 0) ? lo : node.keys[i - 1];
            int childHi = (i == node.n) ? hi : node.keys[i];
            String err = checkNode(node.children[i], childLo, childHi, depth + 1, false, leafDepth, count);
            if (err != null) {
                return err;
            }
        }
        return null;
    }

    // ==================== 演示与自测 ====================

    public static void main(String[] args) {
        demo(2);   // t=2 即 2-3-4 树
        System.out.println();
        demo(3);
        System.out.println();
        stress(2, 2000, 20260802L);
        stress(3, 2000, 99L);
        stress(4, 3000, 42L);
    }

    private static void demo(int t) {
        BTree tree = new BTree(t);
        int[] seq = {10, 20, 30, 15, 25, 5, 35, 40, 45, 18};
        System.out.println("=== BTree(t=" + t + ") 插入 " + Arrays.toString(seq) + " ===");
        for (int k : seq) {
            tree.insert(k);
        }
        System.out.println("中序: " + tree.inorder());
        System.out.println("层序: " + tree.levelOrder());
        System.out.println("size=" + tree.size() + ", height=" + tree.height() + " -> " + check(tree));

        int[] del = {20, 5, 30, 15, 999};
        for (int k : del) {
            System.out.println("删除 " + k + " -> " + tree.remove(k));
        }
        System.out.println("中序: " + tree.inorder());
        System.out.println("size=" + tree.size() + ", height=" + tree.height() + " -> " + check(tree));
    }

    private static String check(BTree tree) {
        String err = tree.checkStructure();
        return err == null ? "结构合法" : "结构异常: " + err;
    }

    /** 随机插入/删除压测:每步后校验结构,删除全部后校验树为空。 */
    private static void stress(int t, int n, long seed) {
        BTree tree = new BTree(t);
        Random rnd = new Random(seed);
        List<Integer> present = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int k = rnd.nextInt(1_000_000);
            if (tree.insert(k)) {
                present.add(k);
            }
            if (i % 50 == 0) {
                String err = tree.checkStructure();
                if (err != null) {
                    System.out.println("BTree(t=" + t + ") 插入阶段损坏: " + err);
                    return;
                }
            }
        }
        Collections.shuffle(present, rnd);
        int removed = 0;
        for (int k : present) {
            if (tree.remove(k)) {
                removed++;
            }
            if (removed % 50 == 0) {
                String err = tree.checkStructure();
                if (err != null) {
                    System.out.println("BTree(t=" + t + ") 删除阶段损坏: " + err);
                    return;
                }
            }
        }
        String err = tree.checkStructure();
        boolean empty = tree.isEmpty() && tree.size() == 0 && tree.height() == -1;
        System.out.println("BTree(t=" + t + "): 插入 " + present.size() + ",删除 " + removed
                + ",最终为空=" + empty + " -> " + (err == null && empty ? "全部通过" : "异常"));
    }
}
