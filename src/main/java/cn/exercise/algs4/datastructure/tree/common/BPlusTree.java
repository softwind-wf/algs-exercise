package cn.exercise.algs4.datastructure.tree.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * B+ 树(泛型版) —— B 树的变体：真数据全部在叶子，内部节点只做路由
 * <p>
 * 挂在 {@link AbstractMultiWayTree} 多路树抽象层下，与 {@link BTree} 形成对照：
 * 复用同一份结构、size/height/findMin/findMax、contains 模板；差异如下：
 * </p>
 * <ul>
 *     <li><b>数据位置</b>：内部节点只存"分隔键"(各右子树最小键的<b>拷贝</b>)，真数据只在叶子；</li>
 *     <li><b>叶子链表</b>：叶子之间用 next 指针串成有序链表，范围查询 O(log n + k)、
 *         inorder 与迭代器直接沿链表扫；</li>
 *     <li><b>分裂语义</b>：叶子分裂时右半最小键作为分隔键"拷贝"上浮(仍留在叶子，是真实数据)；
 *         内部节点分裂时中间键"移动"上浮(内部键只是路由)；</li>
 *     <li><b>删除语义</b>：删除永远发生在叶子；内部分隔键靠递归返回"子树最小键是否变化"逐层上修。</li>
 * </ul>
 * 因此 {@link #inorder()} 与 {@link #iterator()} 必须<b>覆盖</b>为只输出叶子键(内部键是拷贝，会重复)，
 * 而 height/findMin/findMax/contains 等与 B 树完全一致，直接继承。
 *
 * @param <E> 元素类型，须可比较以保证有序性
 */
public class BPlusTree<E extends Comparable<E>> extends AbstractMultiWayTree<E> {

    public BPlusTree() {
        this(2);
    }

    public BPlusTree(int t) {
        super(t);
    }

    // ==================== 增查 ====================

    /**
     * 插入一个键(不允许重复)。自顶向下分裂：根满先 splitRoot，下行遇到满孩子先分裂，
     * 到达的叶子必不满，可直接插入，无需回溯。
     */
    @Override
    public boolean insert(E key) {
        if (key == null) {
            throw new IllegalArgumentException("不允许插入 null");
        }
        if (root == null) {
            root = new Node<>(maxKeys);
            root.setKey(0, key);
            root.n = 1;
            size++;
            return true;
        }
        // 根满:先单独分裂(根无父节点可承接上浮的分隔键)
        if (root.n == maxKeys) {
            splitRoot();
        }
        Node<E> cur = root;
        while (true) {
            if (isLeaf(cur)) {
                int i = 0;
                while (i < cur.n && key.compareTo(cur.keyAt(i)) > 0) {
                    i++;
                }
                if (i < cur.n && key.compareTo(cur.keyAt(i)) == 0) {
                    return false;
                }
                insertKey(cur, i, key);
                size++;
                return true;
            }
            // 内部节点:分隔键 == 右子树最小键,key 不小于分隔键时进入右子树
            int i = 0;
            while (i < cur.n && key.compareTo(cur.keyAt(i)) >= 0) {
                i++;
            }
            if (cur.children[i].n == maxKeys) {
                splitChild(cur, i);
                continue;
            }
            cur = cur.children[i];
        }
    }

    /**
     * 查找指定键。内部节点的分隔键只是路由，必须下潜到叶子才能确认数据是否存在。
     */
    @Override
    public E get(E key) {
        if (root == null) {
            return null;
        }
        Node<E> cur = root;
        while (!isLeaf(cur)) {
            int i = 0;
            while (i < cur.n && key.compareTo(cur.keyAt(i)) >= 0) {
                i++;
            }
            cur = cur.children[i];
        }
        int i = 0;
        while (i < cur.n && key.compareTo(cur.keyAt(i)) > 0) {
            i++;
        }
        if (i < cur.n && key.compareTo(cur.keyAt(i)) == 0) {
            return cur.keyAt(i);
        }
        return null;
    }

    /**
     * 范围查询：返回 [lo, hi] 闭区间内所有键(升序)。
     * B+ 树签名特性——先沿树定位到 lo，再从叶子链表向后扫，复杂度 O(log n + k)。
     */
    public List<E> keysInRange(E lo, E hi) {
        List<E> result = new ArrayList<>();
        if (root == null || lo.compareTo(hi) > 0) {
            return result;
        }
        Node<E> cur = root;
        while (!isLeaf(cur)) {
            int i = 0;
            while (i < cur.n && lo.compareTo(cur.keyAt(i)) >= 0) {
                i++;
            }
            cur = cur.children[i];
        }
        while (cur != null) {
            for (int i = 0; i < cur.n; i++) {
                if (cur.keyAt(i).compareTo(hi) > 0) {
                    return result;
                }
                if (cur.keyAt(i).compareTo(lo) >= 0) {
                    result.add(cur.keyAt(i));
                }
            }
            cur = cur.next;
        }
        return result;
    }

    // ==================== 遍历(覆盖:只输出叶子真键) ====================

    /** 中序遍历 = 直接沿叶子链表扫一遍，不输出内部重复的分隔键 */
    @Override
    public List<E> inorder() {
        List<E> result = new ArrayList<>();
        Node<E> leaf = firstLeaf();
        while (leaf != null) {
            for (int i = 0; i < leaf.n; i++) {
                result.add(leaf.keyAt(i));
            }
            leaf = leaf.next;
        }
        return result;
    }

    /** 沿叶子链表的惰性迭代器 */
    @Override
    public Iterator<E> iterator() {
        return new LeafIterator();
    }

    /** 每个叶子节点的内容(展示叶子链表结构) */
    public List<List<E>> leafNodes() {
        List<List<E>> result = new ArrayList<>();
        Node<E> leaf = firstLeaf();
        while (leaf != null) {
            List<E> one = new ArrayList<>();
            for (int i = 0; i < leaf.n; i++) {
                one.add(leaf.keyAt(i));
            }
            result.add(one);
            leaf = leaf.next;
        }
        return result;
    }

    // ==================== 删除 ====================

    /**
     * 删除指定键。删除永远发生在叶子；内部分隔键靠 minChanged 标志逐层上修。
     */
    @Override
    public boolean remove(E key) {
        if (key == null) {
            throw new IllegalArgumentException("不允许删除 null");
        }
        if (root == null) {
            return false;
        }
        DelResult<E> r = removeRec(root, key);
        if (r.found) {
            size--;
            if (root.n == 0) {
                root = null;                // 根是叶子且被删空 → 树变为空
            }
        }
        return r.found;
    }

    /** 删除递归的结果：是否找到 + 该子树最小键是否变化(用于维护分隔键) */
    private static class DelResult<E> {
        boolean found;
        boolean minChanged;
        E newMin;

        DelResult(boolean found, boolean minChanged, E newMin) {
            this.found = found;
            this.minChanged = minChanged;
            this.newMin = newMin;
        }
    }

    /**
     * 自顶向下删除：下潜过程中保证目标孩子至少 t 个键，因此删后不会下溢、无需回溯修复。
     * 若删掉了某子树的最小键，递归返回时逐层上修分隔键。
     */
    private DelResult<E> removeRec(Node<E> node, E key) {
        if (isLeaf(node)) {
            int i = 0;
            while (i < node.n && key.compareTo(node.keyAt(i)) > 0) {
                i++;
            }
            if (i < node.n && key.compareTo(node.keyAt(i)) == 0) {
                boolean minChanged = (i == 0);      // 删的是叶子最小键?
                removeKeyAt(node, i);
                return new DelResult<>(true, minChanged, node.n > 0 ? node.keyAt(0) : null);
            }
            return new DelResult<>(false, false, null);
        }

        // 内部节点:路由到目标孩子
        int i = 0;
        while (i < node.n && key.compareTo(node.keyAt(i)) >= 0) {
            i++;
        }
        Node<E> oldRoot = root;                     // 检测 fixChild 是否合并掉了根
        if (node.children[i].n < t) {
            i = fixChild(node, i);                  // 保证目标孩子 >= t 键
        }
        if (root != oldRoot) {                      // 根被合并替换:目标孩子已成为新根
            return removeRec(root, key);            // 重新从新根下潜
        }
        DelResult<E> r = removeRec(node.children[i], key);
        if (!r.found) {
            return r;
        }
        // 维护分隔键:若右子树最小键变了,更新 keys[i-1]
        if (i >= 1 && r.minChanged) {
            node.setKey(i - 1, r.newMin);
            return new DelResult<>(true, false, null);   // 就地修复,停止向上传播
        }
        // 本节点最小键 = children[0] 的最小键;只有下潜目标是 children[0] 时才可能变
        if (i == 0 && r.minChanged) {
            return r;                                   // newMin 即本节点新最小键,继续上传
        }
        return new DelResult<>(true, false, null);
    }

    /**
     * 确保 parent.children[i] 至少 t 个键：借键(旋转)或合并，按叶子/内部区分处理。
     * 返回继续下潜的孩子下标(合并后可能改变；根被合并清空时已更新为新根)。
     */
    private int fixChild(Node<E> parent, int i) {
        Node<E> child = parent.children[i];
        if (child.n >= t) {
            return i;
        }
        boolean leaf = isLeaf(parent.children[0]);
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

    /** 叶子借键:children[i-1] 最大键移到 children[i] 开头,分隔键 keys[i-1] 更新为新最小键 */
    private void leafBorrowFromLeft(Node<E> parent, int i) {
        Node<E> left = parent.children[i - 1];
        Node<E> target = parent.children[i];
        for (int j = target.n; j > 0; j--) {
            target.setKey(j, target.keyAt(j - 1));
        }
        target.setKey(0, left.keyAt(left.n - 1));
        target.n++;
        left.n--;
        parent.setKey(i - 1, target.keyAt(0));
    }

    /** 叶子借键:children[i+1] 最小键移到 children[i] 末尾,分隔键 keys[i] 更新为右兄弟新最小键 */
    private void leafBorrowFromRight(Node<E> parent, int i) {
        Node<E> target = parent.children[i];
        Node<E> right = parent.children[i + 1];
        target.setKey(target.n, right.keyAt(0));
        target.n++;
        for (int j = 0; j < right.n - 1; j++) {
            right.setKey(j, right.keyAt(j + 1));
        }
        right.n--;
        parent.setKey(i, right.keyAt(0));
    }

    /** 叶子合并:children[k] 与 children[k+1] 合并到 children[k],删除分隔键 keys[k],维护链表 */
    private void leafMerge(Node<E> parent, int k) {
        Node<E> left = parent.children[k];
        Node<E> right = parent.children[k + 1];
        for (int j = 0; j < right.n; j++) {
            left.setKey(left.n + j, right.keyAt(j));
        }
        left.n += right.n;
        left.next = right.next;
        for (int j = k; j < parent.n - 1; j++) {
            parent.setKey(j, parent.keyAt(j + 1));
        }
        for (int j = k + 1; j < parent.n; j++) {
            parent.children[j] = parent.children[j + 1];
        }
        parent.children[parent.n] = null;
        parent.n--;
    }

    // ---------- 内部层的借键 / 合并(与 B 树相同,分隔键由旋转自维护) ----------

    /** 左旋:把 parent.keys[i] 下沉到 child 末尾,右兄弟最小键上浮到父。 */
    private void rotateLeft(Node<E> parent, int i) {
        Node<E> child = parent.children[i];
        Node<E> right = parent.children[i + 1];
        child.setKey(child.n, parent.keyAt(i));
        child.children[child.n + 1] = right.children[0];
        child.n++;
        parent.setKey(i, right.keyAt(0));
        for (int j = 0; j < right.n - 1; j++) {
            right.setKey(j, right.keyAt(j + 1));
        }
        for (int j = 0; j < right.n; j++) {
            right.children[j] = right.children[j + 1];
        }
        right.n--;
        right.children[right.n + 1] = null;
    }

    /** 右旋:把 parent.keys[i-1] 下沉到 child 开头,左兄弟最大键上浮到父。 */
    private void rotateRight(Node<E> parent, int i) {
        Node<E> child = parent.children[i];
        Node<E> left = parent.children[i - 1];
        for (int j = child.n; j > 0; j--) {
            child.setKey(j, child.keyAt(j - 1));
        }
        child.setKey(0, parent.keyAt(i - 1));
        for (int j = child.n + 1; j > 0; j--) {
            child.children[j] = child.children[j - 1];
        }
        child.children[0] = left.children[left.n];
        child.n++;
        parent.setKey(i - 1, left.keyAt(left.n - 1));
        left.n--;
        left.children[left.n + 1] = null;
    }

    /** 合并 parent.children[i] 与 children[i+1] 到左边,并把 parent.keys[i] 拉入合并节点。 */
    private void mergeRight(Node<E> parent, int i) {
        Node<E> left = parent.children[i];
        Node<E> right = parent.children[i + 1];
        left.setKey(left.n, parent.keyAt(i));
        for (int j = 0; j < right.n; j++) {
            left.setKey(left.n + 1 + j, right.keyAt(j));
        }
        for (int j = 0; j <= right.n; j++) {
            left.children[left.n + 1 + j] = right.children[j];
        }
        left.n += 1 + right.n;
        for (int j = i; j < parent.n - 1; j++) {
            parent.setKey(j, parent.keyAt(j + 1));
        }
        for (int j = i + 1; j < parent.n; j++) {
            parent.children[j] = parent.children[j + 1];
        }
        parent.children[parent.n] = null;
        parent.n--;
    }

    // ==================== 自顶向下分裂 ====================

    /** 分裂根节点(2t-1 键)。根无父节点,必须单独处理,树增高一层。 */
    private void splitRoot() {
        Node<E> old = root;
        Node<E> newRoot = new Node<>(maxKeys);
        if (isLeaf(old)) {
            // 叶子根:右半 keys[t-1..2t-2] 共 t 个键,右半最小键作为分隔键拷贝上浮
            Node<E> right = new Node<>(maxKeys);
            for (int j = 0; j < t; j++) {
                right.setKey(j, old.keyAt(t - 1 + j));
            }
            right.n = t;
            old.n = t - 1;
            right.next = old.next;
            old.next = right;
            newRoot.setKey(0, right.keyAt(0));
            newRoot.children[0] = old;
            newRoot.children[1] = right;
            newRoot.n = 1;
        } else {
            // 内部根:中间键 keys[t-1] 移动上浮(内部键只是路由)
            Node<E> right = new Node<>(maxKeys);
            for (int j = 0; j < t - 1; j++) {
                right.setKey(j, old.keyAt(t + j));
            }
            for (int j = 0; j < t; j++) {
                right.children[j] = old.children[t + j];
            }
            right.n = t - 1;
            old.n = t - 1;
            newRoot.setKey(0, old.keyAt(t - 1));
            newRoot.children[0] = old;
            newRoot.children[1] = right;
            newRoot.n = 1;
        }
        root = newRoot;
    }

    /** 分裂 parent 的第 i 个满孩子(2t-1 键)。调用前提:parent 不满。 */
    private void splitChild(Node<E> parent, int i) {
        Node<E> child = parent.children[i];
        if (isLeaf(child)) {
            splitLeafChild(parent, i);
        } else {
            splitInternalChild(parent, i);
        }
    }

    /** 叶子分裂:右半最小键作为分隔键拷贝上浮,右半保留该键(它是真实数据) */
    private void splitLeafChild(Node<E> parent, int i) {
        Node<E> leaf = parent.children[i];
        Node<E> right = new Node<>(maxKeys);
        for (int j = 0; j < t; j++) {
            right.setKey(j, leaf.keyAt(t - 1 + j));
        }
        right.n = t;
        leaf.n = t - 1;
        right.next = leaf.next;                  // 维护叶子链表
        leaf.next = right;
        insertSeparator(parent, i, right.keyAt(0), right);
    }

    /** 内部节点分裂:中间键 keys[t-1] 移动上浮(内部键只是路由,不保留) */
    private void splitInternalChild(Node<E> parent, int i) {
        Node<E> node = parent.children[i];
        Node<E> right = new Node<>(maxKeys);
        for (int j = 0; j < t - 1; j++) {
            right.setKey(j, node.keyAt(t + j));
        }
        for (int j = 0; j < t; j++) {
            right.children[j] = node.children[t + j];
        }
        right.n = t - 1;
        node.n = t - 1;
        insertSeparator(parent, i, node.keyAt(t - 1), right);
    }

    /** 在 parent.keys[i] 处插入分隔键 sep,并把新孩子挂在 children[i+1]。 */
    private void insertSeparator(Node<E> parent, int i, E sep, Node<E> newChild) {
        for (int j = parent.n; j > i; j--) {
            parent.setKey(j, parent.keyAt(j - 1));
        }
        for (int j = parent.n + 1; j > i + 1; j--) {
            parent.children[j] = parent.children[j - 1];
        }
        parent.setKey(i, sep);
        parent.children[i + 1] = newChild;
        parent.n++;
    }

    /** 将键插入叶子 keys 数组的第 i 个位置(调用前该叶子保证未满) */
    private void insertKey(Node<E> node, int i, E key) {
        for (int j = node.n; j > i; j--) {
            node.setKey(j, node.keyAt(j - 1));
        }
        node.setKey(i, key);
        node.n++;
    }

    // ==================== 内部方法 ====================

    private void removeKeyAt(Node<E> node, int i) {
        for (int j = i; j < node.n - 1; j++) {
            node.setKey(j, node.keyAt(j + 1));
        }
        node.n--;
    }

    /** 最左叶子(叶子链表头) */
    private Node<E> firstLeaf() {
        if (root == null) {
            return null;
        }
        Node<E> cur = root;
        while (!isLeaf(cur)) {
            cur = cur.children[0];
        }
        return cur;
    }

    /** 是否为叶子(以 children[0] == null 判定) */
    private boolean isLeaf(Node<E> node) {
        return node.children[0] == null;
    }

    // ==================== 内部迭代器 ====================

    /** 沿叶子链表的惰性迭代器:游标 = 当前叶子 + 当前键下标 */
    private class LeafIterator implements Iterator<E> {
        private Node<E> leaf;
        private int idx;

        LeafIterator() {
            leaf = firstLeaf();
            idx = 0;
        }

        @Override
        public boolean hasNext() {
            while (leaf != null && idx >= leaf.n) {
                leaf = leaf.next;
                idx = 0;
            }
            return leaf != null;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E out = leaf.keyAt(idx);
            idx++;
            return out;
        }
    }
}
