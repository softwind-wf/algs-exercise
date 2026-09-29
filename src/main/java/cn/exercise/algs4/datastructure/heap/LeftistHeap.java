package cn.exercise.algs4.datastructure.heap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 左式堆（Leftist Heap）—— 大顶堆语义，基于显式二叉树
 *
 * 核心性质：
 *   1. 堆序性：任意节点的值 >= 其左右子节点的值（大顶堆）
 *   2. 左倾性：任意节点的左子节点的 npl >= 右子节点的 npl
 *
 * npl（Null Path Length，零路径长）：
 *   从节点出发到最近的空节点的最短路径长度。
 *   空节点 npl = -1；叶子节点 npl = 0；
 *   任意节点 npl = min(左子 npl, 右子 npl) + 1
 *
 * 与数组二叉堆的区别：
 *   - 数组二叉堆：适合静态数据，合并两个堆需 O(n)
 *   - 左式堆：天然支持高效的 merge（合并）操作，只需 O(log n)，
 *     insert 和 delMax 都可以通过 merge 实现
 *
 * 经典应用：可合并堆场景（如 Dijkstra 多源合并）、
 *           贪心合流、优先队列快速合并
 *
 * @param <E> 元素类型，必须实现 Comparable 接口
 */
public class LeftistHeap<E extends Comparable<? super E>> {

    // ==================== 节点定义 ====================

    private static class Node<E> {
        E item;
        Node<E> left;
        Node<E> right;
        int npl;   // Null Path Length：到最近空节点的最短距离

        Node(E item) {
            this.item = item;
            this.npl = 0;
        }
    }

    // ==================== 成员变量 ====================

    private Node<E> root;
    private int size;

    // ==================== 基础查询 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return root == null;
    }

    /**
     * 查看堆顶元素（最大值）但不移除
     * 时间复杂度：O(1)
     *
     * @throws NoSuchElementException 如果堆为空
     */
    public E peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("堆为空");
        }
        return root.item;
    }

    // ==================== 核心操作 ====================

    /**
     * 插入元素 —— 通过 merge 实现
     * 时间复杂度：O(log n)
     *
     * 算法：将新元素包装成单节点堆，与当前堆合并
     */
    public void insert(E item) {
        if (item == null) {
            throw new IllegalArgumentException("不能插入 null");
        }
        root = merge(root, new Node<>(item));
        size++;
    }

    /**
     * 移除并返回堆顶元素（最大值）—— 通过 merge 实现
     * 时间复杂度：O(log n)
     *
     * 算法：取出根节点，将其左右子树合并作为新的根
     *
     * @throws NoSuchElementException 如果堆为空
     */
    public E delMax() {
        if (isEmpty()) {
            throw new NoSuchElementException("堆为空");
        }
        E max = root.item;
        root = merge(root.left, root.right);
        size--;
        return max;
    }

    /**
     * 合并另一个左式堆到当前堆，another 堆会被清空
     * 时间复杂度：O(log n)
     *
     * @param another 要合并进来的另一个堆
     * @throws IllegalArgumentException 如果 another 为 null
     */
    public void merge(LeftistHeap<E> another) {
        if (another == null) {
            throw new IllegalArgumentException("不能合并 null 堆");
        }
        if (another == this || another.isEmpty()) {
            return;
        }
        root = merge(root, another.root);
        size += another.size;
        another.root = null;
        another.size = 0;
    }

    // ==================== 合并的核心算法 ====================

    /**
     * 合并两个左式堆 —— 左式堆的灵魂操作
     *
     * 递归算法：
     *   1. 基线：任一为空则返回另一个
     *   2. 保证 h1 的根 >= h2 的根（否则交换，h1 是"主堆"）
     *   3. 将 h1 的右子树 与 h2 递归合并，结果挂回 h1.right
     *   4. 检查左倾性：若右子 npl > 左子 npl，交换左右子树
     *   5. 更新 h1 的 npl
     *
     * 时间复杂度：O(log n)——递归始终沿"最右路径"进行，
     * 而左倾性保证最右路径长度不超过 O(log n)
     */
    private Node<E> merge(Node<E> h1, Node<E> h2) {
        if (h1 == null) {
            return h2;
        }
        if (h2 == null) {
            return h1;
        }

        // 保证 h1 是根较大的堆（大顶堆语义）
        if (h1.item.compareTo(h2.item) < 0) {
            Node<E> temp = h1;
            h1 = h2;
            h2 = temp;
        }

        // 将 h2 与 h1 的右子树合并（沿最右路径递归）
        h1.right = merge(h1.right, h2);

        // 维护左倾性：左子 npl 必须 >= 右子 npl
        if (npl(h1.right) > npl(h1.left)) {
            Node<E> temp = h1.left;
            h1.left = h1.right;
            h1.right = temp;
        }

        h1.npl = npl(h1.right) + 1;
        return h1;
    }

    private static int npl(Node<?> node) {
        return node == null ? -1 : node.npl;
    }

    // ==================== 构造与辅助 ====================

    /**
     * 从已有数组建堆
     * 时间复杂度：O(n log n) —— 逐个插入
     */
    public LeftistHeap(E[] items) {
        for (E item : items) {
            insert(item);
        }
    }

    public LeftistHeap() {
    }

    /**
     * 层序遍历（用于打印堆结构和测试）
     */
    public List<E> toList() {
        List<E> result = new ArrayList<>(size);
        List<Node<E>> level = new ArrayList<>();
        if (root != null) {
            level.add(root);
        }
        while (!level.isEmpty()) {
            List<Node<E>> next = new ArrayList<>();
            for (Node<E> node : level) {
                result.add(node.item);
                if (node.left != null) {
                    next.add(node.left);
                }
                if (node.right != null) {
                    next.add(node.right);
                }
            }
            level = next;
        }
        return result;
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        // 1. 逐个插入 + 逐一出堆
        System.out.println("--- 1. 逐个插入 + 逐一出堆 ---");
        Integer[] data = {4, 1, 3, 2, 16, 9, 10, 14, 8, 7};
        LeftistHeap<Integer> heap = new LeftistHeap<>(data);
        System.out.println("原数组: " + Arrays.toString(data));
        System.out.println("建堆后(层序): " + heap);
        System.out.println("peek(): " + heap.peek() + ", size: " + heap.size());
        System.out.print("出堆（从大到小）: ");
        while (!heap.isEmpty()) {
            System.out.print(heap.delMax() + " ");
        }
        System.out.println("\n");

        // 2. 合并两个堆 —— 左式堆的看家本领
        System.out.println("--- 2. 合并两个堆 ---");
        LeftistHeap<Integer> h1 = new LeftistHeap<>(new Integer[]{3, 10, 5});
        LeftistHeap<Integer> h2 = new LeftistHeap<>(new Integer[]{8, 1, 12});
        System.out.println("堆1: " + h1);
        System.out.println("堆2: " + h2);
        h1.merge(h2);
        System.out.println("合并后: " + h1 + ", size: " + h1.size());
        System.out.println("堆2（应被清空）: " + h2 + ", size: " + h2.size());
        System.out.print("合并堆出堆: ");
        while (!h1.isEmpty()) {
            System.out.print(h1.delMax() + " ");
        }
        System.out.println("\n");

        // 3. 边界测试
        System.out.println("--- 3. 边界测试 ---");
        LeftistHeap<String> empty = new LeftistHeap<>();
        try {
            empty.peek();
        } catch (NoSuchElementException e) {
            System.out.println("空堆 peek 预期异常: " + e.getMessage());
        }
        try {
            empty.delMax();
        } catch (NoSuchElementException e) {
            System.out.println("空堆 delMax 预期异常: " + e.getMessage());
        }

        System.out.println("\n========== 测试完成 ==========");
    }
}
