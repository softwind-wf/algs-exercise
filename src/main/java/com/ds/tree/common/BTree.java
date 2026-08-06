package com.ds.tree.common;

/**
 * B 树(泛型版) —— 多路树抽象层的具体实现
 * <p>
 * 基于 {@link AbstractMultiWayTree} 复用结构、遍历、迭代器、统计，这里只实现两件事：
 * <ul>
 *     <li>插入：自顶向下分裂。下行时遇到满(2t-1 键)节点先分裂，中间键上浮到父，
 *         到达的叶子必不满，可直接插入，无需回溯；</li>
 *     <li>删除：自顶向下保障。下潜前用借键(旋转)或合并保证目标孩子至少 t 个键，
 *         因此命中键后不会下溢；内部节点用前驱/后继替换，或合并两侧孩子就地删除。</li>
 * </ul>
 * 与二叉树的 {@link AbstractBST} 形成对照：两者都复用公共层的遍历/统计，
 * 差异在于孩子存储结构(数组 vs 左右指针)与分裂/合并 vs 旋转的平衡策略。
 * </p>
 *
 * @param <E> 元素类型，须可比较以保证有序性
 */
public class BTree<E extends Comparable<E>> extends AbstractMultiWayTree<E> {

    public BTree() {
        this(2);
    }

    public BTree(int t) {
        super(t);
    }

    // ==================== 增查 ====================

    @Override
    public boolean insert(E value) {
        if (root == null) {
            root = new Node<>(maxKeys);
            root.setKey(0, value);
            root.n = 1;
            size++;
            return true;
        }
        // 根没有父节点可承接上浮的键，若已满必须先单独分裂(树增高一层)
        if (root.n == maxKeys) {
            splitRoot();
        }
        Node<E> cur = root;
        while (true) {
            int i = 0;
            while (i < cur.n && value.compareTo(cur.keyAt(i)) > 0) {
                i++;
            }
            if (i < cur.n && value.compareTo(cur.keyAt(i)) == 0) {
                return false;               // 重复键
            }
            if (cur.children[0] == null) {
                insertKey(cur, i, value);   // 叶子：下行时已保证不满，直接插入
                size++;
                return true;
            }
            if (cur.children[i].n == maxKeys) {
                splitChild(cur, i);         // 下行孩子已满：先分裂，再重新定位
                continue;
            }
            cur = cur.children[i];
        }
    }

    @Override
    public E get(E value) {
        Node<E> cur = root;
        while (cur != null) {
            int i = 0;
            while (i < cur.n && value.compareTo(cur.keyAt(i)) > 0) {
                i++;
            }
            if (i < cur.n && value.compareTo(cur.keyAt(i)) == 0) {
                return cur.keyAt(i);
            }
            cur = cur.children[i];
        }
        return null;
    }

    // ==================== 删除 ====================

    @Override
    public boolean remove(E value) {
        if (root == null) {
            return false;
        }
        boolean found = removeRec(root, value);
        if (found) {
            size--;
            // 整棵树只有一个键且被删除：根变空
            if (root.n == 0) {
                root = null;
            }
        }
        return found;
    }

    /**
     * 自顶向下删除：下潜过程中保证除根外每个节点至少 t 个键。
     * 遇到 t-1 键的孩子先借键(旋转)或合并，因此删除后不会下溢，无需回溯修复。
     */
    private boolean removeRec(Node<E> node, E key) {
        int i = 0;
        while (i < node.n && key.compareTo(node.keyAt(i)) > 0) {
            i++;
        }
        if (i < node.n && node.keyAt(i).compareTo(key) == 0) {
            if (node.children[0] == null) {
                removeKeyAt(node, i);
                return true;
            }
            // 内部节点：优先用左子树最大值(前驱)替换并删除之
            if (node.children[i].n >= t) {
                E pred = subtreeMax(node.children[i]);
                node.setKey(i, pred);
                return removeRec(node.children[i], pred);
            }
            // 左侧不足但右侧有富余：改用右子树最小值(后继)
            if (node.children[i + 1].n >= t) {
                E succ = subtreeMin(node.children[i + 1]);
                node.setKey(i, succ);
                return removeRec(node.children[i + 1], succ);
            }
            // 两侧都只有 t-1 键：合并，待删键沉入合并节点后在其中直接删除
            mergeRight(node, i);
            if (node == root && node.n == 0) {
                root = node.children[0];    // 根被合并清空，树降低一层
            }
            return removeRec(node.children[i], key);
        }
        if (node.children[0] == null) {
            return false;                   // 到叶子仍未找到
        }
        Node<E> child = fixChild(node, i);
        return removeRec(child, key);
    }

    /**
     * 确保 parent.children[i] 至少 t 个键：先尝试从兄弟借键(旋转)，否则与兄弟合并。
     * 返回继续下潜的孩子节点(合并后下标/对象可能改变；根被合并清空时返回新根)。
     */
    private Node<E> fixChild(Node<E> parent, int i) {
        Node<E> child = parent.children[i];
        if (child.n >= t) {
            return child;
        }
        // 左兄弟有富余键：右旋借一个
        if (i > 0 && parent.children[i - 1].n >= t) {
            rotateRight(parent, i);
            return parent.children[i];
        }
        // 右兄弟有富余键：左旋借一个
        if (i < parent.n && parent.children[i + 1].n >= t) {
            rotateLeft(parent, i);
            return parent.children[i];
        }
        // 兄弟也都不足：合并并把父键拉下来
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

    // ==================== 自顶向下分裂 ====================

    /** 分裂根节点(2t-1 键)。根没有父节点可承接上浮的键，必须单独处理，树增高一层。 */
    private void splitRoot() {
        Node<E> old = root;
        Node<E> left = new Node<>(maxKeys);
        Node<E> right = new Node<>(maxKeys);
        for (int i = 0; i < minKeys; i++) {
            left.setKey(i, old.keyAt(i));
            left.children[i] = old.children[i];
        }
        left.children[minKeys] = old.children[minKeys];
        left.n = minKeys;
        for (int i = 0; i < minKeys; i++) {
            right.setKey(i, old.keyAt(t + i));
            right.children[i] = old.children[t + i];
        }
        right.children[minKeys] = old.children[2 * t - 1];   // 右半最后一个孩子是 old.children[2t-1]
        right.n = minKeys;

        Node<E> newRoot = new Node<>(maxKeys);
        newRoot.setKey(0, old.keyAt(t - 1));
        newRoot.children[0] = left;
        newRoot.children[1] = right;
        newRoot.n = 1;
        root = newRoot;
    }

    /** 分裂 parent 的第 i 个满孩子(2t-1 键)，中间键上浮进 parent.keys[i]。
     *  左、右两半分别挂在 parent.children[i]、children[i+1]。调用前提：parent 不满。 */
    private void splitChild(Node<E> parent, int i) {
        Node<E> child = parent.children[i];
        Node<E> right = new Node<>(maxKeys);
        for (int j = 0; j < minKeys; j++) {
            right.setKey(j, child.keyAt(t + j));
        }
        for (int j = 0; j < t; j++) {
            right.children[j] = child.children[t + j];
        }
        right.n = minKeys;
        child.n = minKeys;                              // 左半保留 keys[0..t-2]

        for (int j = parent.n; j > i; j--) {
            parent.setKey(j, parent.keyAt(j - 1));
        }
        for (int j = parent.n + 1; j > i + 1; j--) {
            parent.children[j] = parent.children[j - 1];
        }
        parent.setKey(i, child.keyAt(t - 1));
        parent.children[i] = child;
        parent.children[i + 1] = right;
        parent.n++;
    }

    /** 将键插入到节点 keys 数组的第 i 个位置(调用前该节点保证未满) */
    private void insertKey(Node<E> node, int i, E key) {
        for (int j = node.n; j > i; j--) {
            node.setKey(j, node.keyAt(j - 1));
        }
        node.setKey(i, key);
        node.n++;
    }

    // ==================== 借键(旋转) / 合并 ====================

    /** 左旋：把 parent.keys[i] 下沉到 child 末尾，右兄弟最小键上浮到父，右兄弟首孩子移入 child。 */
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
        right.children[right.n + 1] = null;    // 清失效的旧末位槽，而非有效孩子
    }

    /** 右旋：把 parent.keys[i-1] 下沉到 child 开头，左兄弟最大键上浮到父，左兄弟末孩子移入 child。 */
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
        left.children[left.n + 1] = null;      // 清失效的旧末位槽，而非有效孩子
    }

    /** 合并 parent.children[i] 与 children[i+1] 到左边，并把 parent.keys[i] 拉入合并节点。 */
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

    // ==================== 内部方法 ====================

    private void removeKeyAt(Node<E> node, int i) {
        for (int j = i; j < node.n - 1; j++) {
            node.setKey(j, node.keyAt(j + 1));
        }
        node.n--;
    }

    /** 子树中的最大键(沿最右路径下行到叶子) */
    private E subtreeMax(Node<E> node) {
        while (node.children[node.n] != null) {
            node = node.children[node.n];
        }
        return node.keyAt(node.n - 1);
    }

    /** 子树中的最小键(沿最左路径下行到叶子) */
    private E subtreeMin(Node<E> node) {
        while (node.children[0] != null) {
            node = node.children[0];
        }
        return node.keyAt(0);
    }
}
