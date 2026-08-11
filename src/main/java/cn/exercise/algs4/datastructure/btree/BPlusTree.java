package cn.exercise.algs4.datastructure.btree;

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
 * 与 B 树(cn.exercise.algs4.datastructure.btree.BTree)的三个本质区别:
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

    // 删除过程跟踪(供可视化,默认关闭):trace != null 时每完成一步删除操作就记录一次快照
    private List<TraceStep> trace;
    private final List<Integer> curPath = new ArrayList<>();   // 当前下潜路径(根到当前节点的孩子下标)

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
            if (trace != null) {
                record("插入 " + key + " 到空树,创建新根叶子",
                        new Hl(HlKind.INSERTED, new int[0], 0));
            }
            return true;
        }
        // 根满:先单独分裂(根无父节点可承接上浮的分隔键)
        if (root.n == maxKeys) {
            if (trace != null) {
                record("根已满,分裂根节点",
                        new Hl(HlKind.SPLITTING, new int[0], -1));
            }
            splitRoot();
            if (trace != null) {
                record("根分裂完成,新根包含分隔键",
                        new Hl(HlKind.MODIFIED, new int[0], -1));
            }
        }

        curPath.clear();
        Node cur = root;
        while (true) {
            if (cur.isLeaf) {
                int i = 0;
                while (i < cur.n && key > cur.keys[i]) {
                    i++;
                }
                if (i < cur.n && key == cur.keys[i]) {
                    if (trace != null) {
                        record("键 " + key + " 已存在,插入失败",
                                new Hl(HlKind.TARGET, hlPath(), i));
                    }
                    return false;
                }
                insertKey(cur, i, key);
                size++;
                if (trace != null) {
                    record("插入键 " + key + " 到叶子 " + keysStr(cur) + " (位置 " + i + ")",
                            new Hl(HlKind.INSERTED, hlPath(), i));
                }
                return true;
            }
            int i = 0;
            while (i < cur.n && key >= cur.keys[i]) {
                i++;
            }
            curPath.add(i);
            if (trace != null) {
                record("内部节点 " + keysStr(cur) + " 路由键 " + key + " → 第 " + i + " 个孩子",
                        new Hl(HlKind.TARGET, hlPath(), -1));
            }
            if (cur.children[i].n == maxKeys) {
                if (trace != null) {
                    record("第 " + i + " 个孩子已满(键数=" + cur.children[i].n + "),分裂节点",
                            new Hl(HlKind.SPLITTING, hlPath(), -1));
                }
                splitChild(cur, i);
                curPath.remove(curPath.size() - 1);
                if (trace != null) {
                    record("分裂完成,重新从当前节点定位",
                            new Hl(HlKind.MODIFIED, hlPath(), -1));
                }
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
            if (trace != null) {
                record("树为空,查找 " + key + " 失败",
                        new Hl(HlKind.TARGET, new int[0], -1));
            }
            return null;
        }
        curPath.clear();
        Node cur = root;
        while (!cur.isLeaf) {
            int i = 0;
            while (i < cur.n && key >= cur.keys[i]) {
                i++;
            }
            curPath.add(i);
            if (trace != null) {
                record("内部节点 " + keysStr(cur) + " 查找键 " + key + " → 路由到第 " + i + " 个孩子",
                        new Hl(HlKind.TARGET, hlPath(), -1));
            }
            cur = cur.children[i];
        }
        int i = 0;
        while (i < cur.n && key > cur.keys[i]) {
            i++;
        }
        boolean found = (i < cur.n && key == cur.keys[i]);
        if (trace != null) {
            if (found) {
                record("在叶子 " + keysStr(cur) + " 中找到键 " + key,
                        new Hl(HlKind.TARGET, hlPath(), i));
            } else {
                record("在叶子 " + keysStr(cur) + " 中未找到键 " + key,
                        new Hl(HlKind.TARGET, hlPath(), -1));
            }
        }
        return found ? cur.keys[i] : null;
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
            record("树为空,删除失败");
            return false;
        }
        DelResult r = removeRec(root, key);
        if (r.found) {
            size--;
            if (root.n == 0) {
                root = null;                // 根是叶子且被删空
                record("根叶子被删空 → 树变为空");
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
                record("叶子 " + keysStr(node) + " 中定位到键 " + key + "(第 " + i + " 个)",
                        new Hl(HlKind.DELETED, hlPath(), i));
                removeKeyAt(node, i);
                if (minChanged) {
                    if (node.n == 0) {
                        record("删除键 " + key + " → 叶子被删空",
                                new Hl(HlKind.MODIFIED, hlPath(), -1));
                    } else {
                        record("删除键 " + key + " → 叶子最小键 " + key + " 变为 " + node.keys[0]
                                        + ",minChanged=true 向上传播",
                                new Hl(HlKind.MODIFIED, hlPath(), -1));
                    }
                } else {
                    record("删除键 " + key + ",叶子最小键未变(minChanged=false)",
                            new Hl(HlKind.MODIFIED, hlPath(), -1));
                }
                return new DelResult(true, minChanged, node.n > 0 ? node.keys[0] : 0);
            }
            record("叶子 " + keysStr(node) + " 中未找到键 " + key,
                    new Hl(HlKind.TARGET, hlPath(), -1));
            return new DelResult(false, false, 0);
        }

        // 内部节点:路由到目标孩子
        int i = 0;
        while (i < node.n && key >= node.keys[i]) {
            i++;
        }
        record("内部节点 " + keysStr(node) + " 路由键 " + key + " → 第 " + i + " 个孩子" + childRangeStr(node, i),
                new Hl(HlKind.TARGET, hlPath(), -1), new Hl(HlKind.TARGET, hlPath(i), -1));
        Node oldRoot = root;                             // 检测 fixChild 是否合并掉了根
        if (node.children[i].n < t) {
            i = fixChild(node, i);                       // 保证目标孩子 >= t 键
        }
        if (root != oldRoot) {                           // 根被合并替换:目标孩子已成为新根
            curPath.clear();                             // 新根路径为空,重置下潜路径
            return removeRec(root, key);                 // 以空路径重新从新根下潜
        }
        curPath.add(i);
        DelResult r = removeRec(node.children[i], key);
        curPath.remove(curPath.size() - 1);
        if (!r.found) {
            return r;
        }
        // 维护分隔键:若右子树最小键变了,更新 keys[i-1]
        if (i >= 1 && r.minChanged) {
            int oldSep = node.keys[i - 1];
            node.keys[i - 1] = r.newMin;
            record("分隔键 keys[" + (i - 1) + "] 更新:右子树最小键 " + oldSep + " → " + r.newMin,
                    new Hl(HlKind.MOVED, hlPath(), i - 1));
            return new DelResult(true, false, 0);        // 就地修复,停止向上传播
        }
        // 本节点最小键 = children[0] 的最小键;只有下潜目标是 children[0] 时才可能变
        if (i == 0 && r.minChanged) {
            record("子树最小键变化向上传播:→ " + r.newMin + "(下潜目标是最左孩子,本节点无分隔键可修,继续上传)",
                    new Hl(HlKind.MODIFIED, hlPath(), -1));
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
        String type = leaf ? "叶" : "内部";
        // 左兄弟有富余:右旋借一个
        if (i > 0 && parent.children[i - 1].n >= t) {
            record("下溢:第 " + i + " 个" + type + "孩子键数 " + child.n + " < t=" + t
                            + ",左兄弟 " + keysStr(parent.children[i - 1]) + " 有富余,借一个键",
                    new Hl(HlKind.MODIFIED, hlPath(i), -1), new Hl(HlKind.TARGET, hlPath(i - 1), -1));
            if (leaf) {
                leafBorrowFromLeft(parent, i);
                record("左借完成:借来 " + parent.children[i].keys[0] + " 成为孩子 #" + i
                                + " 的新最小键,分隔键 keys[" + (i - 1) + "] 同步更新为 " + parent.children[i].keys[0],
                        new Hl(HlKind.MODIFIED, hlPath(i), -1));
            } else {
                rotateRight(parent, i);
                record("右旋完成:分隔键下沉到孩子 #" + i + " 开头,左兄弟最大键 " + parent.keys[i - 1]
                                + " 上浮为新的分隔键 keys[" + (i - 1) + "]",
                        new Hl(HlKind.MODIFIED, hlPath(i), -1));
            }
            return i;
        }
        // 右兄弟有富余:左旋借一个
        if (i < parent.n && parent.children[i + 1].n >= t) {
            record("下溢:第 " + i + " 个" + type + "孩子键数 " + child.n + " < t=" + t
                            + ",右兄弟 " + keysStr(parent.children[i + 1]) + " 有富余,借一个键",
                    new Hl(HlKind.MODIFIED, hlPath(i), -1), new Hl(HlKind.TARGET, hlPath(i + 1), -1));
            if (leaf) {
                leafBorrowFromRight(parent, i);
                record("右借完成:右兄弟最小键移走,其新最小键 " + parent.children[i + 1].keys[0]
                                + " 成为分隔键 keys[" + i + "]",
                        new Hl(HlKind.MODIFIED, hlPath(i), -1));
            } else {
                rotateLeft(parent, i);
                record("左旋完成:分隔键下沉到孩子 #" + i + " 末尾,右兄弟最小键 " + parent.keys[i]
                                + " 上浮为新的分隔键 keys[" + i + "]",
                        new Hl(HlKind.MODIFIED, hlPath(i), -1));
            }
            return i;
        }
        // 兄弟也都不足:合并
        if (i > 0) {
            record("下溢:第 " + i + " 个" + type + "孩子键数 " + child.n + " < t=" + t
                            + ",左右兄弟均不足,与左兄弟 " + keysStr(parent.children[i - 1]) + " 合并",
                    new Hl(HlKind.MODIFIED, hlPath(i - 1), -1), new Hl(HlKind.MODIFIED, hlPath(i), -1));
            if (leaf) {
                leafMerge(parent, i - 1);
            } else {
                mergeRight(parent, i - 1);
            }
            if (parent == root && parent.n == 0) {
                root = parent.children[0];
                record("合并后根键数归零 → 树高降低,合并节点成为新根",
                        new Hl(HlKind.MODIFIED, new int[0], -1));
            } else {
                record("合并完成:分隔键 keys[" + (i - 1) + "] 被并入,孩子 #" + (i - 1) + " 与 #" + i + " 合为一体",
                        new Hl(HlKind.MODIFIED, hlPath(i - 1), -1));
            }
            return i - 1;
        } else {
            record("下溢:第 0 个" + type + "孩子键数 " + child.n + " < t=" + t
                            + ",左右兄弟均不足,与右兄弟 " + keysStr(parent.children[1]) + " 合并",
                    new Hl(HlKind.MODIFIED, hlPath(0), -1), new Hl(HlKind.MODIFIED, hlPath(1), -1));
            if (leaf) {
                leafMerge(parent, 0);
            } else {
                mergeRight(parent, 0);
            }
            if (parent == root && parent.n == 0) {
                root = parent.children[0];
                record("合并后根键数归零 → 树高降低,合并节点成为新根",
                        new Hl(HlKind.MODIFIED, new int[0], -1));
            } else {
                record("合并完成:分隔键 keys[0] 被并入,孩子 #0 与 #1 合为一体",
                        new Hl(HlKind.MODIFIED, hlPath(0), -1));
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

    // ==================== 删除过程跟踪(供可视化,默认关闭) ====================
    // 仅当 trace 非 null(通过 removeTraced)时才会记录;正常 remove() 完全不受影响。

    /** 树快照节点:与 Node 一一对应,供可视化只读绘制(不暴露私有 Node)。 */
    public static class NodeView {
        public boolean isLeaf;
        public int[] keys;
        public NodeView[] children;   // 内部节点长度 = 键数+1;叶子为 null
        public NodeView next;         // 叶子链表(已按中序重连)

        public NodeView(boolean isLeaf) {
            this.isLeaf = isLeaf;
        }
    }

    /** 高亮类型:下潜目标 / 被修改 / 被删除 / 移动中的键 / 新插入 / 正在分裂 */
    public enum HlKind { TARGET, MODIFIED, DELETED, MOVED, INSERTED, SPLITTING }

    /** 一条高亮:path 是从根到目标节点的孩子下标序列;slot=-1 高亮整个节点,否则高亮 keys[slot]。 */
    public static class Hl {
        public final HlKind kind;
        public final int[] path;
        public final int slot;

        public Hl(HlKind kind, int[] path, int slot) {
            this.kind = kind;
            this.path = path;
            this.slot = slot;
        }
    }

    /** 删除过程中的一步:一次操作完成后的树快照 + 中文说明 + 高亮列表。 */
    public static class TraceStep {
        public final String desc;
        public final NodeView root;
        public final List<Hl> hl;

        public TraceStep(String desc, NodeView root, List<Hl> hl) {
            this.desc = desc;
            this.root = root;
            this.hl = hl;
        }
    }

    /** 深拷贝整棵树(含叶子 next 链重连),供快照。root 为 null 时返回 null。 */
    public NodeView snapshot() {
        if (root == null) {
            return null;
        }
        NodeView view = copyView(root);
        List<NodeView> leaves = new ArrayList<>();
        collectLeaves(view, leaves);
        for (int j = 0; j + 1 < leaves.size(); j++) {
            leaves.get(j).next = leaves.get(j + 1);
        }
        return view;
    }

    private NodeView copyView(Node n) {
        NodeView v = new NodeView(n.isLeaf);
        v.keys = Arrays.copyOf(n.keys, n.n);
        if (!n.isLeaf) {
            v.children = new NodeView[n.n + 1];
            for (int c = 0; c <= n.n; c++) {
                v.children[c] = copyView(n.children[c]);
            }
        }
        return v;
    }

    private void collectLeaves(NodeView v, List<NodeView> out) {
        if (v.isLeaf) {
            out.add(v);
        } else {
            for (NodeView c : v.children) {
                collectLeaves(c, out);
            }
        }
    }

    /** 插入并记录全过程步骤(可视化用)。返回每一步的快照+描述+高亮。 */
    public List<TraceStep> insertTraced(int key) {
        trace = new ArrayList<>();
        curPath.clear();
        insert(key);
        List<TraceStep> out = trace;
        trace = null;
        return out;
    }

    /** 查找并记录全过程步骤(可视化用)。返回每一步的快照+描述+高亮。 */
    public List<TraceStep> getTraced(int key) {
        trace = new ArrayList<>();
        curPath.clear();
        get(key);
        List<TraceStep> out = trace;
        trace = null;
        return out;
    }

    /** 删除并记录全过程步骤(可视化用)。返回每一步的快照+描述+高亮。 */
    public List<TraceStep> removeTraced(int key) {
        trace = new ArrayList<>();
        curPath.clear();
        remove(key);
        List<TraceStep> out = trace;
        trace = null;
        return out;
    }

    /** 记录一步:深拷贝当前树作为快照。trace 为 null 时直接返回(零开销)。 */
    private void record(String desc, Hl... hls) {
        if (trace == null) {
            return;
        }
        List<Hl> list = new ArrayList<>();
        for (Hl h : hls) {
            if (h != null) {
                list.add(h);
            }
        }
        trace.add(new TraceStep(desc, snapshot(), list));
    }

    /** 把 curPath 与 tail 拼成一条从当前根出发的高亮路径。 */
    private int[] hlPath(int... tail) {
        int[] p = new int[curPath.size() + tail.length];
        for (int k = 0; k < curPath.size(); k++) {
            p[k] = curPath.get(k);
        }
        System.arraycopy(tail, 0, p, curPath.size(), tail.length);
        return p;
    }

    /** 把节点 keys 格式化成 "[a, b, c]" */
    private static String keysStr(Node n) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n.n; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(n.keys[i]);
        }
        return sb.append("]").toString();
    }

    /** 路由目标孩子 #i 对应的键区间描述 */
    private static String childRangeStr(Node node, int i) {
        if (i == 0) {
            return "(区间 (−∞, " + node.keys[0] + "))";
        }
        if (i == node.n) {
            return "(区间 [" + node.keys[node.n - 1] + ", +∞))";
        }
        return "(区间 [" + node.keys[i - 1] + ", " + node.keys[i] + "))";
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