package cn.exercise.algs4.datastructure.heap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 斜堆（Skew Heap）—— 大顶堆语义，基于显式二叉树
 *
 * 核心性质：堆序性 —— 任意节点的值 >= 其左右子节点的值（大顶堆）
 *
 * 与左式堆的区别：
 *   - 左式堆：额外维护 npl（零路径长），合并时只在破坏左倾性时交换子树，
 *     保证最右路径 O(log n)，所有操作严格 O(log n)
 *   - 斜堆：不维护任何附加信息，合并时无条件交换左右子树，
 *     单次操作最坏 O(n)，但均摊（amortized）仍为 O(log n)
 *
 * 无条件交换的直觉：
 *   每次合并都会把"最右路径"整条翻转到左边，
 *   下次合并又会把它翻回去 —— 长路径不会连续被走，从而摊还 O(log n)
 *
 * 优点：实现比左式堆更简单，节点无需额外字段，常数更小
 * 经典应用：与左式堆相同 —— 需要高效 merge 的优先队列场景
 *
 * @param <E> 元素类型，必须实现 Comparable 接口
 */
public class SkewHeap<E extends Comparable<? super E>> {

    // ==================== 节点定义 ====================

    private static class Node<E> {
        E item;
        Node<E> left;
        Node<E> right;

        Node(E item) {
            this.item = item;
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
     * 均摊时间复杂度：O(log n)
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
     * 均摊时间复杂度：O(log n)
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
     * 合并另一个斜堆到当前堆，another 堆会被清空
     * 均摊时间复杂度：O(log n)
     *
     * @param another 要合并进来的另一个堆
     * @throws IllegalArgumentException 如果 another 为 null
     */
    public void merge(SkewHeap<E> another) {
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
     * 合并两个斜堆 —— 斜堆的灵魂操作
     *
     * 递归算法：
     *   1. 基线：任一为空则返回另一个
     *   2. 保证 h1 的根 >= h2 的根（否则交换，h1 是"主堆"）
     *   3. 将 h1 的右子树 与 h2 递归合并，结果挂回 h1.right
     *   4. 与左式堆唯一的区别：无条件交换 h1 的左右子树
     *      （左式堆在这里先比较 npl 再决定是否交换）
     *
     * 均摊复杂度：O(log n) —— 递归沿最右路径进行，
     * 无条件交换使长路径不会连续出现在最右侧，摊还代价对数级
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

        // 斜堆的核心：无条件交换左右子树
        Node<E> temp = h1.left;
        h1.left = h1.right;
        h1.right = temp;

        return h1;
    }

    // ==================== 构造与辅助 ====================

    public SkewHeap() {
    }

    /**
     * 从已有数组建堆
     * 时间复杂度：O(n log n) —— 逐个插入
     */
    public SkewHeap(E[] items) {
        for (E item : items) {
            insert(item);
        }
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
        SkewHeap<Integer> heap = new SkewHeap<>(data);
        System.out.println("原数组: " + Arrays.toString(data));
        System.out.println("建堆后(层序): " + heap);
        System.out.println("peek(): " + heap.peek() + ", size: " + heap.size());
        System.out.print("出堆（从大到小）: ");
        while (!heap.isEmpty()) {
            System.out.print(heap.delMax() + " ");
        }
        System.out.println("\n");

        // 2. 合并两个堆 —— 斜堆的看家本领
        System.out.println("--- 2. 合并两个堆 ---");
        SkewHeap<Integer> h1 = new SkewHeap<>(new Integer[]{3, 10, 5});
        SkewHeap<Integer> h2 = new SkewHeap<>(new Integer[]{8, 1, 12});
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
        SkewHeap<String> empty = new SkewHeap<>();
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
