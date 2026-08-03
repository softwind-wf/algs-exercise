package com.ds.twothreefourtree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 2-3-4 树 —— 自顶向下插入版
 *
 * 每个节点至多容纳 3 个键、4 个孩子：2-节点（1 键）、3-节点（2 键）、4-节点（3 键）。
 * 所有叶子位于同一层，因此树始终完美平衡，查找、插入的时间复杂度为 O(log n)。
 *
 * 插入采用自顶向下分裂策略：从根向下寻找插入位置时，遇到 4-节点就先分裂，把中间键上浮到父节点。
 * 由于下行过程中父节点必不为 4-节点，分裂后总有空位承接；到达的叶子也必定不满，可直接插入，
 * 整个过程无需回溯。这与 test3.TwoThreeTree（递归、自底向上合并）形成对照。
 *
 * 与红黑树的关系：2-3-4 树正是红黑树（left-leaning red-black BST）编码的底层多路树，
 * 理解本实现有助于理解 edu.princeton.cs.algs4.RedBlackBST 的旋转与颜色翻转。
 */
public class TwoThreeFourTree {

    /** 2-3-4 树节点：keys[0..n-1] 升序存放键，children[0..n] 为子树指针。叶子节点的 children 全为 null。 */
    private static class Node {
        final int[] keys = new int[3];
        final Node[] children = new Node[4];
        int n;

        Node(int key) {
            keys[0] = key;
            n = 1;
        }
    }

    private Node root;
    private int size;

    // ==================== 增查 ====================

    /** 插入一个键（不允许重复）。返回 true 插入成功；false 键已存在。 */
    public boolean add(int data) {
        if (root == null) {
            root = new Node(data);
            size++;
            return true;
        }
        // 根没有父节点可承接上浮的键，若为 4-节点必须先单独分裂（树增高一层）
        if (root.n == 3) {
            splitRoot();
        }

        Node cur = root;
        while (true) {
            int i = 0;
            while (i < cur.n && data > cur.keys[i]) {
                i++;
            }
            if (i < cur.n && data == cur.keys[i]) {
                return false;               // 重复键
            }
            if (cur.children[0] == null) {
                insertKey(cur, i, data);    // 叶子：下行时已保证不满，直接插入
                size++;
                return true;
            }
            if (cur.children[i].n == 3) {
                splitChild(cur, i);         // 下行孩子是 4-节点：先分裂，再重新定位
                continue;
            }
            cur = cur.children[i];
        }
    }

    public boolean contains(int data) {
        return get(data) != null;
    }

    /** 查找指定键，不存在返回 null */
    public Integer get(int data) {
        Node cur = root;
        while (cur != null) {
            int i = 0;
            while (i < cur.n && data > cur.keys[i]) {
                i++;
            }
            if (i < cur.n && data == cur.keys[i]) {
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

    /** 树高（空树 -1；单节点 0）。2-3-4 树完美平衡，沿最左路径下行深度即树高。 */
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

    /** 中序遍历（左子树、键、右子树交替），结果为升序序列 */
    public List<Integer> inorder() {
        List<Integer> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    /** 层序遍历（广度优先），节点内部按键升序输出 */
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

    /** 分裂根节点（4-节点）。根没有父节点可承接上浮的键，必须单独处理，树增高一层。 */
    private void splitRoot() {
        Node old = root;
        Node left = new Node(old.keys[0]);
        Node right = new Node(old.keys[2]);
        left.children[0] = old.children[0];
        left.children[1] = old.children[1];
        right.children[0] = old.children[2];
        right.children[1] = old.children[3];
        Node newRoot = new Node(old.keys[1]);
        newRoot.children[0] = left;
        newRoot.children[1] = right;
        root = newRoot;
    }

    /** 分裂 parent 的第 i 个孩子（4-节点），中间键上浮进 parent.keys[i]，
     *  左、右两半分别挂在 parent.children[i]、children[i+1]。调用前提：parent 不满（n<=2）。 */
    private void splitChild(Node parent, int i) {
        Node child = parent.children[i];
        Node left = new Node(child.keys[0]);
        Node right = new Node(child.keys[2]);
        left.children[0] = child.children[0];
        left.children[1] = child.children[1];
        right.children[0] = child.children[2];
        right.children[1] = child.children[3];

        for (int j = parent.n; j > i; j--) {
            parent.keys[j] = parent.keys[j - 1];
        }
        for (int j = parent.n + 1; j > i + 1; j--) {
            parent.children[j] = parent.children[j - 1];
        }
        parent.keys[i] = child.keys[1];
        parent.children[i] = left;
        parent.children[i + 1] = right;
        parent.n++;
    }

    /** 将键插入到节点 keys 数组的第 i 个位置（调用前该节点保证未满） */
    private void insertKey(Node node, int i, int data) {
        for (int j = node.n; j > i; j--) {
            node.keys[j] = node.keys[j - 1];
        }
        node.keys[i] = data;
        node.n++;
    }

    // ==================== 删除 ====================

    /** 删除指定键。返回 true 表示键存在且已删除；false 表示键不存在。 */
    public boolean remove(int data) {
        if (root == null) {
            return false;
        }
        boolean found = removeRec(root, data);
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
     * 自顶向下删除：下潜过程中保证除根外每个节点至少 2 个键。
     * 遇到 2-节点孩子先借键（旋转）或合并，因此删除后不会下溢，无需回溯修复。
     * 内部节点删除：用前驱/后继键替换后再递归删除；若两侧孩子都是 2-节点，
     * 则把待删键沉入合并节点，直接在合并节点内删除。
     */
    private boolean removeRec(Node node, int data) {
        int i = 0;
        while (i < node.n && data > node.keys[i]) {
            i++;
        }
        if (i < node.n && node.keys[i] == data) {
            if (node.children[0] == null) {
                removeKeyAt(node, i);
                return true;
            }
            // 内部节点：优先用左子树最大值（前驱）替换并删除之
            if (node.children[i].n >= 2) {
                int pred = subtreeMax(node.children[i]);
                node.keys[i] = pred;
                return removeRec(node.children[i], pred);
            }
            // 左侧是 2-节点但右侧有富余：改用右子树最小值（后继）
            if (node.children[i + 1].n >= 2) {
                int succ = subtreeMin(node.children[i + 1]);
                node.keys[i] = succ;
                return removeRec(node.children[i + 1], succ);
            }
            // 两侧都是 2-节点：合并，待删键沉入合并节点后在其中直接删除
            mergeRight(node, i);
            if (node == root && node.n == 0) {
                root = node.children[0];    // 根被合并清空，树降低一层
            }
            return removeRec(node.children[i], data);
        }
        if (node.children[0] == null) {
            return false;                   // 到叶子仍未找到
        }
        Node child = fixChild(node, i);
        return removeRec(child, data);
    }

    /** 确保 parent.children[i] 不是 2-节点：先尝试从兄弟借键（旋转），否则与兄弟合并。
     *  返回继续下潜的孩子节点（合并后下标/对象可能改变；根被合并清空时返回新根）。 */
    private Node fixChild(Node parent, int i) {
        Node child = parent.children[i];
        if (child.n >= 2) {
            return child;
        }
        // 左兄弟有富余键：右旋借一个
        if (i > 0 && parent.children[i - 1].n >= 2) {
            rotateRight(parent, i);
            return child;
        }
        // 右兄弟有富余键：左旋借一个
        if (i < parent.n && parent.children[i + 1].n >= 2) {
            rotateLeft(parent, i);
            return child;
        }
        // 兄弟也都是 2-节点：合并并把父键拉下来
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

    /** 左旋：把 parent.keys[i] 下沉到 child 末尾，右兄弟最小键上浮到父。 */
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
        right.children[right.n + 1] = null;    // 清失效的旧末位槽，而非有效孩子
    }

    /** 右旋：把 parent.keys[i-1] 下沉到 child 开头，左兄弟最大键上浮到父。 */
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
        left.children[left.n + 1] = null;      // 清失效的旧末位槽，而非有效孩子
    }

    /** 合并 parent.children[i] 与 children[i+1] 到左边，并把 parent.keys[i] 拉入合并节点。 */
    private void mergeRight(Node parent, int i) {
        Node left = parent.children[i];
        Node right = parent.children[i + 1];
        left.keys[left.n] = parent.keys[i];
        left.children[left.n + 1] = right.children[0];
        left.keys[left.n + 1] = right.keys[0];
        left.children[left.n + 2] = right.children[1];
        left.n += 2;
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

    /** 子树中的最大键（沿最右路径下行到叶子） */
    private int subtreeMax(Node node) {
        while (node.children[node.n] != null) {
            node = node.children[node.n];
        }
        return node.keys[node.n - 1];
    }

    /** 子树中的最小键（沿最左路径下行到叶子） */
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

    // ==================== 结构校验（供测试与排错使用） ====================

    /** 校验结构不变量：键数 1~3、键有序、BST 约束、叶子同层、size 一致。null 表示合法。 */
    String checkStructure() {
        if (root == null) {
            return size == 0 ? null : "root 为空但 size=" + size;
        }
        int[] leafDepth = {-1};
        int[] count = {0};
        String err = checkNode(root, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, leafDepth, count);
        if (err != null) {
            return err;
        }
        return count[0] == size ? null : "实际键数 " + count[0] + " 与 size " + size + " 不一致";
    }

    private String checkNode(Node node, int lo, int hi, int depth, int[] leafDepth, int[] count) {
        count[0] += node.n;
        if (node.n < 1 || node.n > 3) {
            return "键数 " + node.n + " 超出 [1,3]";
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
                return "叶子深度不一致：期望 " + leafDepth[0] + "，实际 " + depth;
            }
            return null;
        }
        for (int i = 0; i <= node.n; i++) {
            int childLo = (i == 0) ? lo : node.keys[i - 1];
            int childHi = (i == node.n) ? hi : node.keys[i];
            String err = checkNode(node.children[i], childLo, childHi, depth + 1, leafDepth, count);
            if (err != null) {
                return err;
            }
        }
        return null;
    }
}
