package cn.exercise.algs4.datastructure.heap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 二项堆（Binomial Heap）—— 大顶堆语义，基于二项树森林
 *
 * 二项树 B(k)：
 *   - B(0) 是单节点
 *   - B(k) 由两棵 B(k-1) 合并得到，共 2^k 个节点，树高为 k
 *   - B(k) 的根有 k 个孩子，度数依次为 k-1, k-2, ..., 0
 *
 * 二项堆 = 一组二项树的森林，满足：
 *   1. 堆序性：每棵树中父节点 >= 孩子（大顶堆语义）
 *   2. 唯一性：森林中每种度数的二项树至多一棵 ——
 *      因此 size 的二进制表示与森林中各树的度数一一对应
 *
 * merge 是核心操作，过程类似"二进制加法"：
 *   两棵同度数的树合并产生一棵度数 +1 的树，就像进位（carry）
 *
 * 与左式堆/斜堆的对比：
 *   - 三者 insert / delMax / merge 均摊均为 O(log n)
 *   - 二项堆的优势：delMax 可以确定性地 O(log n)（恰好扫描 log n 棵树的根）
 *   - 左式堆/斜堆实现更简单；二项堆结构更规整
 *
 * @param <E> 元素类型，必须实现 Comparable 接口
 */
public class BinomialHeap<E extends Comparable<? super E>> {

    // ==================== 节点定义 ====================

    private static class Node<E> {
        E item;
        Node<E> child;    // 第一个孩子（其中度数最高的那棵子树）
        Node<E> sibling;  // 下一个兄弟（度数比当前孩子小 1）
        int degree;       // 当前树的度数 = 孩子个数

        Node(E item) {
            this.item = item;
        }
    }

    // ==================== 成员变量 ====================

    /** 森林中所有树的根，按度数升序通过 sibling 链接 */
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
     * 时间复杂度：O(log n) —— 森林至多 log n 棵树，扫描所有树根
     *
     * @throws NoSuchElementException 如果堆为空
     */
    public E peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("堆为空");
        }
        E max = root.item;
        for (Node<E> node = root.sibling; node != null; node = node.sibling) {
            if (node.item.compareTo(max) > 0) {
                max = node.item;
            }
        }
        return max;
    }

    // ==================== 核心操作 ====================

    /**
     * 插入元素 —— 通过 merge 实现
     * 均摊时间复杂度：O(log n)
     *
     * 算法：将新元素包装成 B(0) 单节点树，与当前森林合并
     */
    public void insert(E item) {
        if (item == null) {
            throw new IllegalArgumentException("不能插入 null");
        }
        root = union(root, new Node<>(item));
        size++;
    }

    /**
     * 移除并返回堆顶元素（最大值）
     * 时间复杂度：O(log n)
     *
     * 算法：
     *   1. 扫描森林中所有树根，找到最大者
     *   2. 将它从森林中摘除
     *   3. 它的孩子本身构成一个合法的二项树森林（度数降序），
     *      反转后变回度数升序
     *   4. 将孩子森林与剩余森林 union 合并
     */
    public E delMax() {
        if (isEmpty()) {
            throw new NoSuchElementException("堆为空");
        }

        // 1. 找到最大树根及其前驱
        Node<E> maxPrev = null;
        Node<E> max = root;
        Node<E> prev = root;
        for (Node<E> node = root.sibling; node != null; prev = node, node = node.sibling) {
            if (node.item.compareTo(max.item) > 0) {
                max = node;
                maxPrev = prev;
            }
        }

        // 2. 摘除最大树根
        if (maxPrev == null) {
            root = max.sibling;
        } else {
            maxPrev.sibling = max.sibling;
        }

        // 3. 孩子链表是度数降序，反转为升序
        Node<E> children = null;
        Node<E> curr = max.child;
        while (curr != null) {
            Node<E> next = curr.sibling;
            curr.sibling = children;
            children = curr;
            curr = next;
        }

        // 4. 剩余森林与孩子森林合并
        root = union(root, children);
        size--;
        return max.item;
    }

    /**
     * 合并另一个二项堆到当前堆，another 堆会被清空
     * 时间复杂度：O(log n)
     *
     * @param another 要合并进来的另一个堆
     * @throws IllegalArgumentException 如果 another 为 null
     */
    public void merge(BinomialHeap<E> another) {
        if (another == null) {
            throw new IllegalArgumentException("不能合并 null 堆");
        }
        if (another == this || another.isEmpty()) {
            return;
        }
        root = union(root, another.root);
        size += another.size;
        another.root = null;
        another.size = 0;
    }

    // ==================== 合并的核心算法 ====================

    /**
     * 合并两个森林 —— 二项堆的灵魂操作
     *
     * 分两步：
     *   1. mergeRoots：把两个按度数升序的根链表归并成一个升序链表
     *   2. 逐一扫描，遇到"同度数"的相邻树就合并（类似二进制加法的进位）：
     *      - 大根树的根当新根
     *      - 小根树挂为其第一个孩子（度数最高的孩子）
     *      - 新树度数 +1，可能继续与下一棵同度数的树"进位"
     *
     * 时间复杂度：O(log n) —— 每个森林至多 log n 棵树
     */
    private Node<E> union(Node<E> h1, Node<E> h2) {
        Node<E> merged = mergeRoots(h1, h2);
        if (merged == null) {
            return null;
        }

        Node<E> prev = null;
        Node<E> curr = merged;
        Node<E> next = curr.sibling;
        while (next != null) {
            if (curr.degree != next.degree
                    || (next.sibling != null && next.sibling.degree == curr.degree)) {
                // 度数不同，或出现三棵同度数树（先跳过第一棵，留待下一轮进位）
                prev = curr;
                curr = next;
            } else if (curr.item.compareTo(next.item) >= 0) {
                // curr 根更大：next 挂为 curr 的孩子
                curr.sibling = next.sibling;
                next.sibling = curr.child;
                curr.child = next;
                curr.degree++;
            } else {
                // next 根更大：curr 挂为 next 的孩子（注意修正前驱指针）
                if (prev == null) {
                    merged = next;
                } else {
                    prev.sibling = next;
                }
                curr.sibling = next.child;
                next.child = curr;
                next.degree++;
                curr = next;
            }
            next = curr.sibling;
        }
        return merged;
    }

    /**
     * 将两个按度数升序的根链表归并为一个升序链表（经典双指针归并）
     */
    private Node<E> mergeRoots(Node<E> h1, Node<E> h2) {
        if (h1 == null) {
            return h2;
        }
        if (h2 == null) {
            return h1;
        }
        Node<E> head;
        Node<E> tail;
        if (h1.degree <= h2.degree) {
            head = h1;
            h1 = h1.sibling;
        } else {
            head = h2;
            h2 = h2.sibling;
        }
        tail = head;
        while (h1 != null && h2 != null) {
            if (h1.degree <= h2.degree) {
                tail.sibling = h1;
                h1 = h1.sibling;
            } else {
                tail.sibling = h2;
                h2 = h2.sibling;
            }
            tail = tail.sibling;
        }
        tail.sibling = (h1 != null) ? h1 : h2;
        return head;
    }

    // ==================== 构造与辅助 ====================

    public BinomialHeap() {
    }

    /**
     * 从已有数组建堆
     * 时间复杂度：O(n log n) —— 逐个插入
     */
    public BinomialHeap(E[] items) {
        for (E item : items) {
            insert(item);
        }
    }

    /**
     * 层序遍历森林（用于打印和测试），树与树之间用 " | " 分隔
     */
    @SuppressWarnings("unchecked")
    public List<E> toList() {
        List<E> result = new ArrayList<>(size);
        boolean first = true;
        for (Node<E> tree = root; tree != null; tree = tree.sibling) {
            if (!first) {
                result.add((E) "|");
            }
            first = false;
            List<Node<E>> level = new ArrayList<>();
            level.add(tree);
            while (!level.isEmpty()) {
                List<Node<E>> next = new ArrayList<>();
                for (Node<E> node : level) {
                    result.add(node.item);
                    for (Node<E> child = node.child; child != null; child = child.sibling) {
                        next.add(child);
                    }
                }
                level = next;
            }
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
        BinomialHeap<Integer> heap = new BinomialHeap<>(data);
        System.out.println("原数组: " + Arrays.toString(data));
        System.out.println("建堆后(森林层序, | 分隔树): " + heap);
        System.out.println("peek(): " + heap.peek() + ", size: " + heap.size());
        System.out.print("出堆（从大到小）: ");
        while (!heap.isEmpty()) {
            System.out.print(heap.delMax() + " ");
        }
        System.out.println("\n");

        // 2. 合并两个堆 —— 二项堆的看家本领
        System.out.println("--- 2. 合并两个堆 ---");
        BinomialHeap<Integer> h1 = new BinomialHeap<>(new Integer[]{3, 10, 5});
        BinomialHeap<Integer> h2 = new BinomialHeap<>(new Integer[]{8, 1, 12});
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
        BinomialHeap<String> empty = new BinomialHeap<>();
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
