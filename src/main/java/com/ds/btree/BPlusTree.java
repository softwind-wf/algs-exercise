package com.ds.btree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * B+ 树 —— B 树的变体:数据全部在叶子,内部节点只做路由
 *
 * 与 B 树(com.ds.btree.BTree)的三个本质区别:
 *  1. 真正的键(数据)只存于叶子;内部节点存"分隔键",即各右子树最小键的拷贝。
 *  2. 叶子之间用 next 指针串成有序链表,支持范围查询 O(log n + k)。
 *  3. 叶子分裂时,右半最小键作为分隔键"拷贝"上浮(它仍留在叶子,是真实数据);
 *     内部节点分裂时,中间键"移动"上浮(内部键只是路由,不是数据)。
 *
 * 结构不变量(最小度数 t,与 B 树相同):
 *  - 每个节点(除根)至少 t-1 个键、至多 2t-1 个键;
 *  - 内部节点含 n 个分隔键、n+1 个孩子,且 keys[i] == 右子树最小键;
 *  - 所有叶子位于同一层,并通过 next 链成升序链表。
 *
 * t = 2 时即 2-3-4 树的 B+ 版。
 * 插入沿用自顶向下分裂;删除沿用自顶向下保障(借键/合并),无需回溯修复;
 * 分隔键的维护靠递归返回"该子树最小键是否变化"逐层上修。
 *
 * 对照 B 树阅读:B 树内部节点持有真数据、删除要走"前驱/后继/合并";
 * B+ 树内部只路由,删除永远发生在叶子,反而更简单——但多了一个"分隔键随最小键变化"的维护。
 */
public class BPlusTree {

    private final int t;           // 最小度数,>= 2
    private final int maxKeys;     // 2t-1
    private final int minKeys;     // t-1
    private Node root;
    private int size;

    public BPlusTree() {
        this(2);
    }

    public BPlusTree(int t) {
        if (t < 2) {
            throw new IllegalArgumentException("最小度数 t 必须 >= 2,实际 " + t);
        }
        this.t = t;
        this.maxKeys = 2 * t - 1;
        this.minKeys = t - 1;
    }

    /** 节点:叶子存真数据并带 next 链表指针;内部节点只存分隔键。 */
    private class Node {
        final boolean isLeaf;
        final int[] keys = new int[maxKeys];
        final Node[] children = new Node[maxKeys + 1];   // 叶子不使用
        int n;
        Node next;                                        // 仅叶子使用:下一个叶子

        Node(boolean isLeaf) {
            this.isLeaf = isLeaf;
        }
    }

    // ==================== 增查 ====================

    /** 插入一个键(不允许重复)。返回 true 插入成功;false 键已存在。 */
    public boolean insert(int key) {
        if (root == null) {
            root = new Node(true);
            root.keys[0] = key;
            root.n = 1;
            size++;
            return true;
        }
        // 根满:先单独分裂(根无父节点可承接上浮的分隔键)
        if (root.n == maxKeys) {
            splitRoot();
        }

        Node cur = root;
        while (true) {
            if (cur.isLeaf) {
                int i = 0;
                while (i < cur.n && key > cur.keys[i]) {
                    i++;
                }
                if (i < cur.n && key == cur.keys[i]) {
                    return false;               // 重复键
                }
                insertKey(cur, i, key);         // 叶子:下行时已保证不满,直接插入
                size++;
                return true;
            }
            // 内部节点:路由到孩子,分隔键 keys[i] = 右子树最小键
            int i = 0;
            while (i < cur.n && key >= cur.keys[i]) {
                i++;
            }
            if (cur.children[i].n == maxKeys) {
                splitChild(cur, i);             // 下行孩子已满:先分裂,再重新定位
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
        if (root == null) {
            return null;
        }
        Node cur = root;
        while (!cur.isLeaf) {
            int i = 0;
            while (i < cur.n && key >= cur.keys[i]) {
                i++;
            }
            cur = cur.children[i];
        }
        int i = 0;
        while (i < cur.n && key > cur.keys[i]) {
            i++;
        }
        return (i < cur.n && key == cur.keys[i]) ? cur.keys[i] : null;
    }

    // ==================== 范围查询(B+ 树签名特性:沿叶子链表扫) ====================

    /** 返回 [lo, hi] 闭区间内所有键(升序)。先沿树定位到 lo,再从叶子链表向后扫。 */
    public List<Integer> keysInRange(int lo, int hi) {
        List<Integer> result = new ArrayList<>();
        if (root == null || lo > hi) {
            return result;
        }
        Node cur = root;
        while (!cur.isLeaf) {
            int i = 0;
            while (i < cur.n && lo >= cur.keys[i]) {
                i++;
            }
            cur = cur.children[i];
        }
        while (cur != null) {
            for (int i = 0; i < cur.n; i++) {
                if (cur.keys[i] > hi) {
                    return result;
                }
                if (cur.keys[i] >= lo) {
                    result.add(cur.keys[i]);
                }
            }
            cur = cur.next;
        }
        return result;
    }

    /** 每个叶子节点的内容(展示叶子链表结构) */
    public List<List<Integer>> leafNodes() {
        List<List<Integer>> result = new ArrayList<>();
        Node leaf = firstLeaf();
        while (leaf != null) {
            List<Integer> one = new ArrayList<>();
            for (int i = 0; i < leaf.n; i++) {
                one.add(leaf.keys[i]);
            }
            result.add(one);
            leaf = leaf.next;
        }
        return result;
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

    /** 树高(空树 -1;单节点 0)。B+ 树所有叶子同层,沿最左路径下行深度即树高。 */
    public int height() {
        if (root == null) {
            return -1;
        }
        int h = 0;
        Node cur = root;
        while (!cur.isLeaf) {
            h++;
            cur = cur.children[0];
        }
        return h;
    }

    public Integer findMin() {
        if (root == null) {
            return null;
        }
        Node leaf = firstLeaf();
        return leaf.keys[0];
    }

    public Integer findMax() {
        if (root == null) {
            return null;
        }
        Node cur = root;
        while (!cur.isLeaf) {
            cur = cur.children[cur.n];
        }
        return cur.keys[cur.n - 1];
    }

    // ==================== 遍历 ====================

    /** 中序遍历 = 直接沿叶子链表扫一遍(不用走树) */
    public List<Integer> inorder() {
        List<Integer> result = new ArrayList<>();
        Node leaf = firstLeaf();
        while (leaf != null) {
            for (int i = 0; i < leaf.n; i++) {
                result.add(leaf.keys[i]);
            }
            leaf = leaf.next;
        }
        return result;
    }

    /** 层序遍历。注意:内部节点的分隔键是叶子键的拷贝,因此键会重复出现。 */
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
            if (!cur.isLeaf) {
                for (int i = 0; i <= cur.n; i++) {
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

    /** 分裂根节点(2t-1 键)。根无父节点,必须单独处理,树增高一层。 */
    private void splitRoot() {
        Node old = root;
        Node newRoot = new Node(false);
        if (old.isLeaf) {
            Node right = new Node(true);
            System.arraycopy(old.keys, t - 1, right.keys, 0, t);     // keys[t-1..2t-2],共 t 个键
            right.n = t;
            old.n = t - 1;                                           // 左半 keys[0..t-2]
            right.next = old.next;
            old.next = right;
            newRoot.keys[0] = right.keys[0];                           // 分隔键 = 右叶最小键(拷贝)
            newRoot.children[0] = old;
            newRoot.children[1] = right;
            newRoot.n = 1;
        } else {
            Node right = new Node(false);
            System.arraycopy(old.keys, t, right.keys, 0, t - 1);       // keys[t..2t-2]
            System.arraycopy(old.children, t, right.children, 0, t);   // children[t..2t-1]
            right.n = t - 1;
            old.n = t - 1;                                             // 左半 keys[0..t-2],children[0..t-1]
            newRoot.keys[0] = old.keys[t - 1];                         // 中间键上浮(移动)
            newRoot.children[0] = old;
            newRoot.children[1] = right;
            newRoot.n = 1;
        }
        root = newRoot;
    }

    /** 分裂 parent 的第 i 个满孩子(2t-1 键),中间键上浮进 parent.keys[i]。调用前提:parent 不满。 */
    private void splitChild(Node parent, int i) {
        Node child = parent.children[i];
        if (child.isLeaf) {
            splitLeafChild(parent, i);
        } else {
            splitInternalChild(parent, i);
        }
    }

    /** 叶子分裂:右半最小键作为分隔键拷贝上浮,右半保留该键(它是真实数据)。 */
    private void splitLeafChild(Node parent, int i) {
        Node leaf = parent.children[i];
        Node right = new Node(true);
        System.arraycopy(leaf.keys, t - 1, right.keys, 0, t);        // keys[t-1..2t-2],共 t 个键
        right.n = t;
        leaf.n = t - 1;                                              // 左半 keys[0..t-2]
        right.next = leaf.next;                                        // 维护叶子链表
        leaf.next = right;
        insertSeparator(parent, i, right.keys[0], right);
    }

    /** 内部节点分裂:中间键 keys[t-1] 移动上浮(内部键只是路由,不保留)。 */
    private void splitInternalChild(Node parent, int i) {
        Node node = parent.children[i];
        Node right = new Node(false);
        System.arraycopy(node.keys, t, right.keys, 0, t - 1);          // keys[t..2t-2]
        System.arraycopy(node.children, t, right.children, 0, t);      // children[t..2t-1]
        right.n = t - 1;
        node.n = t - 1;                                                // 左半 keys[0..t-2],children[0..t-1]
        insertSeparator(parent, i, node.keys[t - 1], right);
    }

    /** 在 parent.keys[i] 处插入分隔键 sep,并把新孩子挂在 children[i+1]。 */
    private void insertSeparator(Node parent, int i, int sep, Node newChild) {
        for (int j = parent.n; j > i; j--) {
            parent.keys[j] = parent.keys[j - 1];
        }
        for (int j = parent.n + 1; j > i + 1; j--) {
            parent.children[j] = parent.children[j - 1];
        }
        parent.keys[i] = sep;
        parent.children[i + 1] = newChild;
        parent.n++;
    }

    /** 将键插入叶子 keys 数组的第 i 个位置(调用前该叶子保证未满) */
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
        DelResult r = removeRec(root, key);
        if (r.found) {
            size--;
            if (root.n == 0) {
                root = null;                // 根是叶子且被删空
            }
        }
        return r.found;
    }

    /** 删除递归的结果:是否找到 + 该子树最小键是否变化(用于维护分隔键)。 */
    private static class DelResult {
        boolean found;
        boolean minChanged;
        int newMin;

        DelResult(boolean found, boolean minChanged, int newMin) {
            this.found = found;
            this.minChanged = minChanged;
            this.newMin = newMin;
        }
    }

    /**
     * 自顶向下删除:下潜过程中保证目标孩子至少 t 个键,因此删后不会下溢、无需回溯修复。
     * 删除永远发生在叶子(内部节点只路由)。
     * 返回的 minChanged 标志:若删掉了某子树的最小键,递归返回时逐层上修分隔键。
     */
    private DelResult removeRec(Node node, int key) {
        if (node.isLeaf) {
            int i = 0;
            while (i < node.n && key > node.keys[i]) {
                i++;
            }
            if (i < node.n && key == node.keys[i]) {
                boolean minChanged = (i == 0);           // 删的是叶子最小键?
                removeKeyAt(node, i);
                return new DelResult(true, minChanged, node.n > 0 ? node.keys[0] : 0);
            }
            return new DelResult(false, false, 0);
        }

        // 内部节点:路由到目标孩子
        int i = 0;
        while (i < node.n && key >= node.keys[i]) {
            i++;
        }
        if (node.children[i].n < t) {
            i = fixChild(node, i);                       // 保证目标孩子 >= t 键
        }
        DelResult r = removeRec(node.children[i], key);
        if (!r.found) {
            return r;
        }
        // 维护分隔键:若右子树最小键变了,更新 keys[i-1]
        if (i >= 1 && r.minChanged) {
            node.keys[i - 1] = r.newMin;
        }
        // 本节点最小键 = children[0] 的最小键;只有下潜目标是 children[0] 时才可能变
        if (i == 0 && r.minChanged) {
            return r;                                    // newMin 即本节点新最小键,继续上传
        }
        return new DelResult(true, false, 0);
    }

    /** 确保 parent.children[i] 至少 t 个键:借键(旋转)或合并,并按叶子/内部区分处理。
     *  返回继续下潜的孩子下标(合并后可能改变;根被合并清空时已更新为新根)。 */
    private int fixChild(Node parent, int i) {
        Node child = parent.children[i];
        if (child.n >= t) {
            return i;
        }
        boolean leaf = parent.children[0].isLeaf;
        // 左兄弟有富余:右旋借一个
        if (i > 0 && parent.children[i - 1].n >= t) {
            if (leaf) {
                leafBorrowFromLeft(parent, i);
            } else {
                rotateRight(parent, i);
            }
            return i;
        }
        // 右兄弟有富余:左旋借一个
        if (i < parent.n && parent.children[i + 1].n >= t) {
            if (leaf) {
                leafBorrowFromRight(parent, i);
            } else {
                rotateLeft(parent, i);
            }
            return i;
        }
        // 兄弟也都不足:合并
        if (i > 0) {
            if (leaf) {
                leafMerge(parent, i - 1);
            } else {
                mergeRight(parent, i - 1);
            }
            if (parent == root && parent.n == 0) {
                root = parent.children[0];
            }
            return i - 1;
        } else {
            if (leaf) {
                leafMerge(parent, 0);
            } else {
                mergeRight(parent, 0);
            }
            if (parent == root && parent.n == 0) {
                root = parent.children[0];
            }
            return 0;
        }
    }

    // ---------- 叶子层的借键 / 合并(叶子无孩子,只有键 + 链表) ----------

    /** 叶子借键:children[i-1] 最大键移到 children[i] 开头,分隔键 keys[i-1] 更新为新最小键。 */
    private void leafBorrowFromLeft(Node parent, int i) {
        Node left = parent.children[i - 1];
        Node target = parent.children[i];
        for (int j = target.n; j > 0; j--) {
            target.keys[j] = target.keys[j - 1];
        }
        target.keys[0] = left.keys[left.n - 1];
        target.n++;
        left.n--;
        parent.keys[i - 1] = target.keys[0];
    }

    /** 叶子借键:children[i+1] 最小键移到 children[i] 末尾,分隔键 keys[i] 更新为右兄弟新最小键。 */
    private void leafBorrowFromRight(Node parent, int i) {
        Node target = parent.children[i];
        Node right = parent.children[i + 1];
        target.keys[target.n] = right.keys[0];
        target.n++;
        for (int j = 0; j < right.n - 1; j++) {
            right.keys[j] = right.keys[j + 1];
        }
        right.n--;
        parent.keys[i] = right.keys[0];
    }

    /** 叶子合并:children[k] 与 children[k+1] 合并到 children[k],删除分隔键 keys[k],维护链表。 */
    private void leafMerge(Node parent, int k) {
        Node left = parent.children[k];
        Node right = parent.children[k + 1];
        for (int j = 0; j < right.n; j++) {
            left.keys[left.n + j] = right.keys[j];
        }
        left.n += right.n;
        left.next = right.next;
        for (int j = k; j < parent.n - 1; j++) {
            parent.keys[j] = parent.keys[j + 1];
        }
        for (int j = k + 1; j < parent.n; j++) {
            parent.children[j] = parent.children[j + 1];
        }
        parent.children[parent.n] = null;
        parent.n--;
    }

    // ---------- 内部层的借键 / 合并(与 B 树完全相同,分隔键由旋转自维护) ----------

    /** 左旋:把 parent.keys[i] 下沉到 child 末尾,右兄弟最小键上浮到父。 */
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
        right.children[right.n + 1] = null;
    }

    /** 右旋:把 parent.keys[i-1] 下沉到 child 开头,左兄弟最大键上浮到父。 */
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
        left.children[left.n + 1] = null;
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

    // ==================== 内部方法 ====================

    private void removeKeyAt(Node node, int i) {
        for (int j = i; j < node.n - 1; j++) {
            node.keys[j] = node.keys[j + 1];
        }
        node.n--;
    }

    /** 最左叶子(叶子链表头) */
    private Node firstLeaf() {
        if (root == null) {
            return null;
        }
        Node cur = root;
        while (!cur.isLeaf) {
            cur = cur.children[0];
        }
        return cur;
    }

    /** 子树中的最小键(沿最左路径下行到叶子) */
    private int subtreeMin(Node node) {
        while (!node.isLeaf) {
            node = node.children[0];
        }
        return node.keys[0];
    }

    // ==================== 结构校验(供测试与排错使用) ====================

    /** 校验结构不变量:键数区间、键有序、分隔键 == 右子树最小键、叶子同层、链表一致、size 一致。null 表示合法。 */
    String checkStructure() {
        if (root == null) {
            return size == 0 ? null : "root 为空但 size=" + size;
        }
        int[] leafDepth = {-1};
        int[] count = {0};
        int[] leafCount = {0};
        String err = checkNode(root, Integer.MIN_VALUE, false, Integer.MAX_VALUE, 0, true, leafDepth, count, leafCount);
        if (err != null) {
            return err;
        }
        // 校验叶子链表:键数、叶子数、跨叶有序
        Node leaf = firstLeaf();
        int chainKeys = 0;
        int chainLeaves = 0;
        Node prev = null;
        while (leaf != null) {
            chainKeys += leaf.n;
            chainLeaves++;
            if (prev != null && prev.keys[prev.n - 1] >= leaf.keys[0]) {
                return "叶子链表跨叶无序(" + prev.keys[prev.n - 1] + ">=" + leaf.keys[0] + ")";
            }
            prev = leaf;
            leaf = leaf.next;
        }
        if (chainKeys != count[0]) {
            return "叶子链表键数 " + chainKeys + " != 树中键数 " + count[0];
        }
        if (chainLeaves != leafCount[0]) {
            return "叶子链表节点数 " + chainLeaves + " != 树中叶子数 " + leafCount[0];
        }
        return count[0] == size ? null : "实际键数 " + count[0] + " 与 size " + size + " 不一致";
    }

    private String checkNode(Node node, int lo, boolean loInc, int hi, int depth, boolean isRoot, int[] leafDepth, int[] count, int[] leafCount) {
        int min = isRoot ? 1 : minKeys;
        if (node.n < min || node.n > maxKeys) {
            return "键数 " + node.n + " 超出 [" + min + "," + maxKeys + "]";
        }
        for (int i = 1; i < node.n; i++) {
            if (node.keys[i] <= node.keys[i - 1]) {
                return "节点内键未严格递增";
            }
        }
        // B+ 树分隔键 keys[i] = 右子树最小键,因此孩子左边界是包含的(children[i>=1] 含 keys[i-1]);
        // 仅最左孩子 children[0] 的下界是开区间。loInc 表示 lo 是否包含。
        for (int i = 0; i < node.n; i++) {
            if (node.keys[i] < lo || (node.keys[i] == lo && !loInc) || node.keys[i] >= hi) {
                return "键值越界 (lo=" + lo + (loInc ? "含" : "开") + ", hi=" + hi + ")";
            }
        }
        if (node.isLeaf) {
            leafCount[0]++;
            count[0] += node.n;             // 只有叶子里的键才是真实数据,计入总数
            if (node.children[0] != null) {
                return "叶子不应有孩子";
            }
            if (leafDepth[0] == -1) {
                leafDepth[0] = depth;
            } else if (leafDepth[0] != depth) {
                return "叶子深度不一致:期望 " + leafDepth[0] + ",实际 " + depth;
            }
            return null;
        }
        for (int i = 0; i < node.n; i++) {
            if (node.children[i] == null) {
                return "内部节点缺失孩子 " + i;
            }
            if (node.children[i].isLeaf != node.children[0].isLeaf) {
                return "混合叶/内部孩子";
            }
            int rm = subtreeMin(node.children[i + 1]);
            if (node.keys[i] != rm) {
                return "分隔键 " + node.keys[i] + " != 右子树最小键 " + rm;
            }
        }
        if (node.children[node.n] == null) {
            return "内部节点缺失最后孩子";
        }
        for (int i = 0; i <= node.n; i++) {
            boolean childLoInc = (i == 0) ? loInc : true;
            int childLo = (i == 0) ? lo : node.keys[i - 1];
            int childHi = (i == node.n) ? hi : node.keys[i];
            String err = checkNode(node.children[i], childLo, childLoInc, childHi, depth + 1, false, leafDepth, count, leafCount);
            if (err != null) {
                return err;
            }
        }
        return null;
    }

    // ==================== 演示与自测 ====================

    public static void main(String[] args) {
        demo(2);
        System.out.println();
        demo(3);
        System.out.println();
        stress(2, 2000, 20260802L);
        stress(3, 2000, 99L);
        stress(4, 3000, 42L);
    }

    private static void demo(int t) {
        BPlusTree tree = new BPlusTree(t);
        int[] seq = {10, 20, 30, 15, 25, 5, 35, 40, 45, 18, 12};
        System.out.println("=== B+Tree(t=" + t + ") 插入 " + Arrays.toString(seq) + " ===");
        for (int k : seq) {
            tree.insert(k);
        }
        System.out.println("中序: " + tree.inorder());
        System.out.println("层序: " + tree.levelOrder() + "  (内部键是拷贝,会重复)");
        System.out.println("叶子链: " + tree.leafNodes());
        System.out.println("范围[12,35]: " + tree.keysInRange(12, 35));
        System.out.println("size=" + tree.size() + ", height=" + tree.height() + " -> " + check(tree));

        int[] del = {20, 12, 5, 30, 15, 999};
        for (int k : del) {
            System.out.println("删除 " + k + " -> " + tree.remove(k));
        }
        System.out.println("中序: " + tree.inorder());
        System.out.println("叶子链: " + tree.leafNodes());
        System.out.println("size=" + tree.size() + ", height=" + tree.height() + " -> " + check(tree));
    }

    private static String check(BPlusTree tree) {
        String err = tree.checkStructure();
        return err == null ? "结构合法" : "结构异常: " + err;
    }

    /** 随机插入/删除压测:每步校验结构,并验证中序、范围查询、删除全部后树为空。 */
    private static void stress(int t, int n, long seed) {
        BPlusTree tree = new BPlusTree(t);
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
                    System.out.println("B+Tree(t=" + t + ") 插入阶段损坏: " + err);
                    return;
                }
            }
        }
        Collections.sort(present);
        if (!tree.inorder().equals(present)) {
            System.out.println("B+Tree(t=" + t + ") 中序与插入集合不一致");
            return;
        }
        for (int c = 0; c < 20; c++) {
            int lo = rnd.nextInt(1_000_000);
            int hi = rnd.nextInt(1_000_000);
            if (lo > hi) {
                int tmp = lo;
                lo = hi;
                hi = tmp;
            }
            List<Integer> expected = new ArrayList<>();
            for (int k : present) {
                if (k >= lo && k <= hi) {
                    expected.add(k);
                }
            }
            if (!tree.keysInRange(lo, hi).equals(expected)) {
                System.out.println("B+Tree(t=" + t + ") 范围查询 [" + lo + "," + hi + "] 不一致");
                return;
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
                    System.out.println("B+Tree(t=" + t + ") 删除阶段损坏: " + err);
                    return;
                }
            }
        }
        String err = tree.checkStructure();
        boolean empty = tree.isEmpty() && tree.size() == 0 && tree.height() == -1;
        System.out.println("B+Tree(t=" + t + "): 插入 " + present.size() + ",删除 " + removed
                + ",最终为空=" + empty + " -> " + (err == null && empty ? "全部通过" : "异常"));
    }
}
