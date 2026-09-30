package cn.exercise.algs4.datastructure.heap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BinomialHeap} 的 JUnit 5 单元测试。
 *
 * 覆盖范围：
 *   - 构造方法（默认 / 数组）
 *   - insert / delMax / peek / size / isEmpty 基本功能
 *   - merge 操作：与空堆合并、自合并、正常合并、合并后源堆清空
 *   - 边界异常：空堆 peek/delMax、插入 null、合并 null
 *   - 特殊元素：重复、负数与零、String 类型
 *   - 二项堆特有结构不变式（反射校验）：
 *       * 每棵树都是合法二项树（度数 k 的根恰有 k 个孩子，度数 k-1..0）
 *       * 大顶堆序（父 >= 子）
 *       * 森林根度数严格递增（每种度数至多一棵）
 *       * 森林节点总数 == size
 *       * 森林树棵数 == size 的二进制中 1 的个数
 *   - toList / toString（注意 toList 会在树间插入 "|" 分隔符）
 *   - 与 Arrays.sort 对照的排序正确性、交错操作、大规模操作
 */
@DisplayName("二项堆 BinomialHeap 测试")
class BinomialHeapTest {

    // ==================== 基础查询与构造 ====================

    @Test
    @DisplayName("新建堆应为空")
    void shouldBeEmptyOnCreation() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    @DisplayName("空数组建堆后仍为空")
    void buildFromEmptyArray() {
        BinomialHeap<Integer> heap = new BinomialHeap<>(new Integer[0]);
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    @DisplayName("数组构造后大小正确且非空")
    void buildFromArray() {
        Integer[] items = {4, 1, 3, 2, 16, 9, 10, 14, 8, 7};
        BinomialHeap<Integer> heap = new BinomialHeap<>(items);
        assertEquals(items.length, heap.size());
        assertFalse(heap.isEmpty());
    }

    // ==================== 单元素操作 ====================

    @Test
    @DisplayName("插入单个元素后 peek/delMax 行为正确")
    void insertSingleElement() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        heap.insert(5);
        assertFalse(heap.isEmpty());
        assertEquals(1, heap.size());
        assertEquals(5, heap.peek().intValue());
        assertEquals(1, heap.size()); // peek 不改变 size
        assertEquals(5, heap.delMax().intValue());
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    // ==================== 多元素插入与出堆 ====================

    @Test
    @DisplayName("delMax 严格按降序返回")
    void delMaxReturnsInDescendingOrder() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        int[] input = {3, 1, 5, 2, 4};
        for (int v : input) {
            heap.insert(v);
        }
        assertEquals(5, heap.delMax().intValue());
        assertEquals(4, heap.delMax().intValue());
        assertEquals(3, heap.delMax().intValue());
        assertEquals(2, heap.delMax().intValue());
        assertEquals(1, heap.delMax().intValue());
        assertTrue(heap.isEmpty());
    }

    @Test
    @DisplayName("升序插入也能正确降序出堆")
    void ascendingInsertion() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        for (int i = 1; i <= 10; i++) {
            heap.insert(i);
        }
        for (int expected = 10; expected >= 1; expected--) {
            assertEquals(expected, heap.delMax().intValue());
        }
    }

    @Test
    @DisplayName("降序插入也能正确降序出堆")
    void descendingInsertion() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        for (int i = 10; i >= 1; i--) {
            heap.insert(i);
        }
        for (int expected = 10; expected >= 1; expected--) {
            assertEquals(expected, heap.delMax().intValue());
        }
    }

    // ==================== 特殊元素 ====================

    @Test
    @DisplayName("重复元素全部保留并按序出堆")
    void duplicateElements() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        int[] input = {5, 3, 5, 3, 5};
        for (int v : input) {
            heap.insert(v);
        }
        assertEquals(5, heap.delMax().intValue());
        assertEquals(5, heap.delMax().intValue());
        assertEquals(5, heap.delMax().intValue());
        assertEquals(3, heap.delMax().intValue());
        assertEquals(3, heap.delMax().intValue());
    }

    @Test
    @DisplayName("支持负数与零")
    void negativeAndZero() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        int[] input = {-3, -1, -5, 0, 2};
        for (int v : input) {
            heap.insert(v);
        }
        assertEquals(2, heap.delMax().intValue());
        assertEquals(0, heap.delMax().intValue());
        assertEquals(-1, heap.delMax().intValue());
        assertEquals(-3, heap.delMax().intValue());
        assertEquals(-5, heap.delMax().intValue());
    }

    @Test
    @DisplayName("支持 String 等任意 Comparable 类型")
    void supportsStringElements() {
        BinomialHeap<String> heap = new BinomialHeap<>();
        heap.insert("apple");
        heap.insert("zebra");
        heap.insert("banana");
        heap.insert("cat");
        assertEquals("zebra", heap.delMax());
        assertEquals("cat", heap.delMax());
        assertEquals("banana", heap.delMax());
        assertEquals("apple", heap.delMax());
    }

    // ==================== peek 操作 ====================

    @Test
    @DisplayName("peek 返回当前最大值且不移除")
    void peekReturnsMaxWithoutRemoving() {
        BinomialHeap<Integer> heap = new BinomialHeap<>(new Integer[]{10, 20, 30});
        assertEquals(30, heap.peek().intValue());
        assertEquals(3, heap.size());
        assertEquals(30, heap.peek().intValue());
    }

    @Test
    @DisplayName("最大值位于多棵树时应能被 peek 正确找出（森林扫描）")
    void peekScansAllTreeRoots() {
        // 构造森林中棵数 > 1，且最大值不在第一棵（度数最小）树的根
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        // 依次插入使 100 落在度数较大树的根上
        int[] input = {1, 2, 100, 3, 4};
        for (int v : input) {
            heap.insert(v);
        }
        assertEquals(100, heap.peek().intValue());
    }

    // ==================== 异常边界 ====================

    @Test
    @DisplayName("插入 null 抛 IllegalArgumentException")
    void insertNullShouldThrow() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        assertThrows(IllegalArgumentException.class, () -> heap.insert(null));
    }

    @Test
    @DisplayName("空堆 peek 抛 NoSuchElementException")
    void peekOnEmptyShouldThrow() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        assertThrows(NoSuchElementException.class, heap::peek);
    }

    @Test
    @DisplayName("空堆 delMax 抛 NoSuchElementException")
    void delMaxOnEmptyShouldThrow() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        assertThrows(NoSuchElementException.class, heap::delMax);
    }

    @Test
    @DisplayName("合并 null 堆抛 IllegalArgumentException")
    void mergeNullShouldThrow() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        assertThrows(IllegalArgumentException.class, () -> heap.merge(null));
    }

    // ==================== merge 操作 ====================

    @Test
    @DisplayName("与空堆合并不改变当前堆")
    void mergeWithEmptyHeap() {
        BinomialHeap<Integer> heap = new BinomialHeap<>(new Integer[]{5, 10, 3});
        BinomialHeap<Integer> empty = new BinomialHeap<>();
        int sizeBefore = heap.size();
        heap.merge(empty);
        assertEquals(sizeBefore, heap.size());
        assertEquals(10, heap.peek().intValue());
    }

    @Test
    @DisplayName("自合并应为无操作")
    void selfMergeIsNoOp() {
        BinomialHeap<Integer> heap = new BinomialHeap<>(new Integer[]{5, 10, 3});
        int sizeBefore = heap.size();
        heap.merge(heap);
        assertEquals(sizeBefore, heap.size());
        assertEquals(10, heap.peek().intValue());
    }

    @Test
    @DisplayName("两个堆合并后元素总数正确且出堆有序")
    void mergeTwoHeaps() {
        BinomialHeap<Integer> h1 = new BinomialHeap<>(new Integer[]{3, 10, 5});
        BinomialHeap<Integer> h2 = new BinomialHeap<>(new Integer[]{8, 1, 12});
        h1.merge(h2);
        assertEquals(6, h1.size());
        assertEquals(12, h1.delMax().intValue());
        assertEquals(10, h1.delMax().intValue());
        assertEquals(8, h1.delMax().intValue());
        assertEquals(5, h1.delMax().intValue());
        assertEquals(3, h1.delMax().intValue());
        assertEquals(1, h1.delMax().intValue());
        assertTrue(h1.isEmpty());
    }

    @Test
    @DisplayName("合并后源堆被清空")
    void mergeClearsSourceHeap() {
        BinomialHeap<Integer> h1 = new BinomialHeap<>(new Integer[]{1, 2});
        BinomialHeap<Integer> h2 = new BinomialHeap<>(new Integer[]{3, 4});
        h1.merge(h2);
        assertTrue(h2.isEmpty());
        assertEquals(0, h2.size());
    }

    @Test
    @DisplayName("空堆合并非空堆等同于取过内容")
    void emptyMergeNonEmpty() {
        BinomialHeap<Integer> empty = new BinomialHeap<>();
        BinomialHeap<Integer> full = new BinomialHeap<>(new Integer[]{7, 3, 9});
        empty.merge(full);
        assertEquals(3, empty.size());
        assertEquals(9, empty.peek().intValue());
        assertTrue(full.isEmpty());
    }

    @Test
    @DisplayName("合并含重复元素的两个堆")
    void mergeWithDuplicates() {
        BinomialHeap<Integer> h1 = new BinomialHeap<>(new Integer[]{5, 5, 3});
        BinomialHeap<Integer> h2 = new BinomialHeap<>(new Integer[]{5, 3, 1});
        h1.merge(h2);
        assertEquals(6, h1.size());
        assertEquals(5, h1.delMax().intValue());
        assertEquals(5, h1.delMax().intValue());
        assertEquals(5, h1.delMax().intValue());
        assertEquals(3, h1.delMax().intValue());
        assertEquals(3, h1.delMax().intValue());
        assertEquals(1, h1.delMax().intValue());
    }

    // ==================== toList 与 toString ====================

    @Test
    @DisplayName("空堆 toList 返回空列表")
    void toListOnEmpty() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        assertTrue(heap.toList().isEmpty());
    }

    @Test
    @DisplayName("toList 的真实节点数等于 size（忽略 '|' 分隔符）")
    void toListRealNodeCountEqualsSize() {
        BinomialHeap<Integer> heap = new BinomialHeap<>(new Integer[]{4, 1, 3, 2, 16, 9, 10, 14, 8, 7});
        List<Integer> elements = extractIntegers(heap.toList());
        assertEquals(heap.size(), elements.size());
    }

    @Test
    @DisplayName("toList 包含所有插入元素")
    void toListContainsAllElements() {
        Integer[] items = {4, 1, 3, 2, 16};
        BinomialHeap<Integer> heap = new BinomialHeap<>(items);
        List<Integer> actual = extractIntegers(heap.toList());
        Integer[] sortedExpected = items.clone();
        Arrays.sort(sortedExpected);
        Integer[] sortedActual = actual.toArray(new Integer[0]);
        Arrays.sort(sortedActual);
        assertTrue(Arrays.equals(sortedExpected, sortedActual), "toList 应包含全部插入元素");
    }

    @Test
    @DisplayName("空堆 toString 输出 []")
    void toStringOnEmpty() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        assertEquals("[]", heap.toString());
    }

    @Test
    @DisplayName("toString 包含所有元素")
    void toStringContainsAllElements() {
        BinomialHeap<Integer> heap = new BinomialHeap<>(new Integer[]{10, 20, 30});
        String str = heap.toString();
        assertTrue(str.contains("10"));
        assertTrue(str.contains("20"));
        assertTrue(str.contains("30"));
    }

    // ==================== 二项堆结构不变式 ====================

    @Test
    @DisplayName("插入后满足二项堆结构不变式")
    void structureAfterInsertions() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        int[] input = {4, 1, 3, 2, 16, 9, 10, 14, 8, 7};
        for (int v : input) {
            heap.insert(v);
        }
        assertBinomialStructure(heap);
    }

    @Test
    @DisplayName("合并后满足二项堆结构不变式")
    void structureAfterMerge() {
        BinomialHeap<Integer> h1 = new BinomialHeap<>(new Integer[]{3, 10, 5});
        BinomialHeap<Integer> h2 = new BinomialHeap<>(new Integer[]{8, 1, 12});
        h1.merge(h2);
        assertBinomialStructure(h1);
    }

    @Test
    @DisplayName("delMax 后剩余森林仍满足二项堆结构不变式")
    void structureAfterDelMax() {
        BinomialHeap<Integer> heap =
                new BinomialHeap<>(new Integer[]{4, 1, 3, 2, 16, 9, 10, 14, 8, 7});
        heap.delMax();
        assertBinomialStructure(heap);
        heap.delMax();
        assertBinomialStructure(heap);
        heap.delMax();
        assertBinomialStructure(heap);
    }

    @Test
    @DisplayName("森林树棵数应等于 size 二进制中 1 的个数")
    void treeCountEqualsPopcount() {
        for (int n = 1; n <= 32; n++) {
            BinomialHeap<Integer> heap = new BinomialHeap<>();
            for (int i = 0; i < n; i++) {
                heap.insert(i);
            }
            assertEquals(Integer.bitCount(n), treeCount(heap),
                    "size=" + n + " 时森林应有 " + Integer.bitCount(n) + " 棵二项树");
        }
    }

    @Test
    @DisplayName("度数 k 的树根恰有 k 个孩子（对每个规模验证结构）")
    void binomialTreeDegreeMatchesChildren() {
        // 构造一棵完整的 B(4)（16 个节点），森林中应出现度数 4 的树
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        for (int i = 0; i < 16; i++) {
            heap.insert(i);
        }
        assertBinomialStructure(heap); // 内部递归保证度数与孩子数一致
    }

    // ==================== 排序正确性（与 Arrays.sort 对照）====================

    @Test
    @DisplayName("反复 delMax 等价于正确的降序排序")
    void delMaxProducesCorrectSort() {
        Random random = new Random(20260930L);
        int n = 500;
        Integer[] input = new Integer[n];
        for (int i = 0; i < n; i++) {
            input[i] = random.nextInt(1000) - 500;
        }

        Integer[] expected = input.clone();
        Arrays.sort(expected); // 升序

        BinomialHeap<Integer> heap = new BinomialHeap<>(input);
        Integer[] actual = new Integer[n];
        for (int i = 0; i < n; i++) {
            actual[i] = heap.delMax(); // 降序
        }

        for (int i = 0; i < n; i++) {
            assertEquals(expected[i], actual[n - 1 - i],
                    "排序结果第 " + i + " 个元素不匹配");
        }
    }

    // ==================== 交错操作 ====================

    @Test
    @DisplayName("插入与删除交替进行结果始终正确")
    void interleavedInsertAndDelMax() {
        BinomialHeap<Integer> heap = new BinomialHeap<>();
        heap.insert(10);
        heap.insert(20);
        assertEquals(20, heap.delMax().intValue());
        heap.insert(30);
        heap.insert(5);
        assertEquals(30, heap.peek().intValue());
        assertEquals(30, heap.delMax().intValue());
        heap.insert(15);
        assertEquals(15, heap.delMax().intValue());
        assertEquals(10, heap.delMax().intValue());
        assertEquals(5, heap.delMax().intValue());
        assertTrue(heap.isEmpty());
    }

    @Test
    @DisplayName("多次合并与删除交错进行")
    void interleavedMergeAndDelMax() {
        BinomialHeap<Integer> main = new BinomialHeap<>();
        main.insert(5);

        BinomialHeap<Integer> batch1 = new BinomialHeap<>(new Integer[]{10, 1});
        main.merge(batch1);
        assertEquals(10, main.delMax().intValue());

        BinomialHeap<Integer> batch2 = new BinomialHeap<>(new Integer[]{20, 15});
        main.merge(batch2);
        assertEquals(20, main.delMax().intValue());
        assertEquals(15, main.delMax().intValue());
        assertEquals(5, main.delMax().intValue());
        assertEquals(1, main.delMax().intValue());
        assertTrue(main.isEmpty());
    }

    @Test
    @DisplayName("大量数据入堆出堆与合并操作的正确性")
    void largeScaleOperations() {
        Random random = new Random(42L);
        BinomialHeap<Integer> heap1 = new BinomialHeap<>();
        BinomialHeap<Integer> heap2 = new BinomialHeap<>();
        int total = 1000;
        Integer[] all = new Integer[total];

        for (int i = 0; i < total; i++) {
            all[i] = random.nextInt(10000);
            if (i % 2 == 0) {
                heap1.insert(all[i]);
            } else {
                heap2.insert(all[i]);
            }
        }

        heap1.merge(heap2);
        assertEquals(total, heap1.size());
        assertBinomialStructure(heap1);

        Integer prev = null;
        for (int i = 0; i < total; i++) {
            Integer curr = heap1.delMax();
            if (prev != null) {
                assertTrue(curr <= prev, "出堆顺序应为降序: " + prev + " -> " + curr);
            }
            prev = curr;
        }
        assertTrue(heap1.isEmpty());
    }

    // ==================== 辅助方法 ====================

    /** 从 toList 结果中过滤出真正的 Integer 元素（排除树间分隔符 "|"）。 */
    private static List<Integer> extractIntegers(List<?> raw) {
        List<Integer> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Integer) {
                out.add((Integer) o);
            }
        }
        return out;
    }

    /** 反射统计森林中二项树的棵数（根链表长度）。 */
    private static int treeCount(BinomialHeap<?> heap) {
        try {
            Object cur = readField(heap, "root");
            int count = 0;
            while (cur != null) {
                count++;
                cur = sibling(cur);
            }
            return count;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("反射访问失败", e);
        }
    }

    /**
     * 反射校验二项堆的完整不变式：
     *   1. 森林根度数严格递增（每种度数至多一棵）
     *   2. 每棵树都是合法二项树且满足大顶堆序
     *   3. 森林节点总数 == size
     */
    private static void assertBinomialStructure(BinomialHeap<?> heap) {
        try {
            Field sizeF = BinomialHeap.class.getDeclaredField("size");
            sizeF.setAccessible(true);
            int declaredSize = sizeF.getInt(heap);

            Object cur = readField(heap, "root");
            int prevDeg = -1;
            int total = 0;
            while (cur != null) {
                int deg = degree(cur);
                assertTrue(deg > prevDeg,
                        "森林根度数应严格递增: 上一棵=" + prevDeg + ", 当前=" + deg);
                total += checkBinomialTree(cur, deg);
                prevDeg = deg;
                cur = sibling(cur);
            }
            assertEquals(declaredSize, total, "森林节点总数应等于 size");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("反射访问失败", e);
        }
    }

    /**
     * 递归校验以 node 为根、期望度数为 expectedDeg 的二项树：
     *   - 度数与期望一致
     *   - 恰有 expectedDeg 个孩子，度数依次为 expectedDeg-1 .. 0
     *   - 大顶堆序：node.item >= 每个孩子.item
     * 返回该子树的节点总数。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int checkBinomialTree(Object node, int expectedDeg)
            throws ReflectiveOperationException {
        assertEquals(expectedDeg, degree(node), "二项树度数与期望不符");
        Comparable val = item(node);
        Object child = child(node);
        int count = 1;
        for (int c = expectedDeg - 1; c >= 0; c--) {
            assertNotNull(child, "度数 " + expectedDeg + " 的树应有 " + expectedDeg + " 个孩子");
            assertTrue(val.compareTo(item(child)) >= 0,
                    "大顶堆性质被破坏: parent=" + val + " < child=" + item(child));
            count += checkBinomialTree(child, c);
            child = sibling(child);
        }
        assertNull(child, "孩子数量应恰好等于度数");
        return count;
    }

    // -------- 反射读取 Node 内部字段的工具 --------

    private static Object readField(Object target, String name)
            throws ReflectiveOperationException {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static int degree(Object node) throws ReflectiveOperationException {
        Field f = node.getClass().getDeclaredField("degree");
        f.setAccessible(true);
        return f.getInt(node);
    }

    private static Object child(Object node) throws ReflectiveOperationException {
        Field f = node.getClass().getDeclaredField("child");
        f.setAccessible(true);
        return f.get(node);
    }

    private static Object sibling(Object node) throws ReflectiveOperationException {
        Field f = node.getClass().getDeclaredField("sibling");
        f.setAccessible(true);
        return f.get(node);
    }

    @SuppressWarnings("rawtypes")
    private static Comparable item(Object node) throws ReflectiveOperationException {
        Field f = node.getClass().getDeclaredField("item");
        f.setAccessible(true);
        return (Comparable) f.get(node);
    }
}
