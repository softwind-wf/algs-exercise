package cn.exercise.algs4.datastructure.heap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SkewHeap} 的 JUnit 5 单元测试。
 *
 * 覆盖范围：
 *   - 构造方法（默认 / 数组）
 *   - insert / delMax / peek / size / isEmpty 基本功能
 *   - merge 操作（核心看家本领）：与空堆合并、自合并、正常合并、合并后对方清空
 *   - 大顶堆性质验证（通过反射遍历树结构）
 *   - 层序遍历 toList()
 *   - 边界异常：空堆 peek/delMax、插入 null、合并 null
 *   - 与 Arrays.sort 对照的排序正确性
 *   - 多次交错操作
 */
@DisplayName("斜堆 SkewHeap 测试")
class SkewHeapTest {

    // ==================== 基础查询与构造 ====================

    @Test
    @DisplayName("新建堆应为空")
    void shouldBeEmptyOnCreation() {
        SkewHeap<Integer> heap = new SkewHeap<>();
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    @DisplayName("空数组建堆后仍为空")
    void buildFromEmptyArray() {
        SkewHeap<Integer> heap = new SkewHeap<>(new Integer[0]);
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    @DisplayName("数组构造后大小正确且非空")
    void buildFromArray() {
        Integer[] items = {4, 1, 3, 2, 16, 9, 10, 14, 8, 7};
        SkewHeap<Integer> heap = new SkewHeap<>(items);
        assertEquals(items.length, heap.size());
        assertFalse(heap.isEmpty());
    }

    // ==================== 单元素操作 ====================

    @Test
    @DisplayName("插入单个元素后 peek/delMax 行为正确")
    void insertSingleElement() {
        SkewHeap<Integer> heap = new SkewHeap<>();
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
        SkewHeap<Integer> heap = new SkewHeap<>();
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
        SkewHeap<Integer> heap = new SkewHeap<>();
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
        SkewHeap<Integer> heap = new SkewHeap<>();
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
        SkewHeap<Integer> heap = new SkewHeap<>();
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
        SkewHeap<Integer> heap = new SkewHeap<>();
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
        SkewHeap<String> heap = new SkewHeap<>();
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
        SkewHeap<Integer> heap = new SkewHeap<>(new Integer[]{10, 20, 30});
        assertEquals(30, heap.peek().intValue());
        assertEquals(3, heap.size());
        assertEquals(30, heap.peek().intValue()); // 再 peek 仍是同一个值
    }

    // ==================== 异常边界 ====================

    @Test
    @DisplayName("插入 null 抛 IllegalArgumentException")
    void insertNullShouldThrow() {
        SkewHeap<Integer> heap = new SkewHeap<>();
        assertThrows(IllegalArgumentException.class, () -> heap.insert(null));
    }

    @Test
    @DisplayName("空堆 peek 抛 NoSuchElementException")
    void peekOnEmptyShouldThrow() {
        SkewHeap<Integer> heap = new SkewHeap<>();
        assertThrows(NoSuchElementException.class, heap::peek);
    }

    @Test
    @DisplayName("空堆 delMax 抛 NoSuchElementException")
    void delMaxOnEmptyShouldThrow() {
        SkewHeap<Integer> heap = new SkewHeap<>();
        assertThrows(NoSuchElementException.class, heap::delMax);
    }

    @Test
    @DisplayName("合并 null 堆抛 IllegalArgumentException")
    void mergeNullShouldThrow() {
        SkewHeap<Integer> heap = new SkewHeap<>();
        assertThrows(IllegalArgumentException.class, () -> heap.merge(null));
    }

    // ==================== merge 操作 ====================

    @Test
    @DisplayName("与空堆合并不改变当前堆")
    void mergeWithEmptyHeap() {
        SkewHeap<Integer> heap = new SkewHeap<>(new Integer[]{5, 10, 3});
        SkewHeap<Integer> empty = new SkewHeap<>();
        int sizeBefore = heap.size();
        heap.merge(empty);
        assertEquals(sizeBefore, heap.size());
        assertEquals(10, heap.peek().intValue());
    }

    @Test
    @DisplayName("自合并应为无操作")
    void selfMergeIsNoOp() {
        SkewHeap<Integer> heap = new SkewHeap<>(new Integer[]{5, 10, 3});
        int sizeBefore = heap.size();
        heap.merge(heap); // 自己跟自己合并
        assertEquals(sizeBefore, heap.size());
        assertEquals(10, heap.peek().intValue());
    }

    @Test
    @DisplayName("两个堆合并后元素总数正确且出堆有序")
    void mergeTwoHeaps() {
        SkewHeap<Integer> h1 = new SkewHeap<>(new Integer[]{3, 10, 5});
        SkewHeap<Integer> h2 = new SkewHeap<>(new Integer[]{8, 1, 12});
        h1.merge(h2);
        assertEquals(6, h1.size());
        // 出堆应为降序：12, 10, 8, 5, 3, 1
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
        SkewHeap<Integer> h1 = new SkewHeap<>(new Integer[]{1, 2});
        SkewHeap<Integer> h2 = new SkewHeap<>(new Integer[]{3, 4});
        h1.merge(h2);
        assertTrue(h2.isEmpty());
        assertEquals(0, h2.size());
    }

    @Test
    @DisplayName("空堆合并非空堆等同于取过内容")
    void emptyMergeNonEmpty() {
        SkewHeap<Integer> empty = new SkewHeap<>();
        SkewHeap<Integer> full = new SkewHeap<>(new Integer[]{7, 3, 9});
        empty.merge(full);
        assertEquals(3, empty.size());
        assertEquals(9, empty.peek().intValue());
        assertTrue(full.isEmpty());
    }

    @Test
    @DisplayName("合并含重复元素的两个堆")
    void mergeWithDuplicates() {
        SkewHeap<Integer> h1 = new SkewHeap<>(new Integer[]{5, 5, 3});
        SkewHeap<Integer> h2 = new SkewHeap<>(new Integer[]{5, 3, 1});
        h1.merge(h2);
        assertEquals(6, h1.size());
        assertEquals(5, h1.delMax().intValue());
        assertEquals(5, h1.delMax().intValue());
        assertEquals(5, h1.delMax().intValue());
        assertEquals(3, h1.delMax().intValue());
        assertEquals(3, h1.delMax().intValue());
        assertEquals(1, h1.delMax().intValue());
    }

    // ==================== toList 层序遍历 ====================

    @Test
    @DisplayName("空堆 toList 返回空列表")
    void toListOnEmpty() {
        SkewHeap<Integer> heap = new SkewHeap<>();
        assertTrue(heap.toList().isEmpty());
    }

    @Test
    @DisplayName("toList 首元素为堆顶最大值")
    void toListFirstElementIsMax() {
        SkewHeap<Integer> heap = new SkewHeap<>(new Integer[]{4, 1, 3, 2, 16, 9, 10, 14, 8, 7});
        List<Integer> list = heap.toList();
        assertEquals(16, list.get(0).intValue(), "层序遍历首位应为堆顶（最大值）");
    }

    @Test
    @DisplayName("toList 包含所有元素且大小正确")
    void toListContainsAllElements() {
        Integer[] items = {4, 1, 3, 2, 16};
        SkewHeap<Integer> heap = new SkewHeap<>(items);
        List<Integer> list = heap.toList();
        assertEquals(items.length, list.size());
        // 排序后应与原数组排序一致
        Integer[] sortedExpected = items.clone();
        Arrays.sort(sortedExpected);
        Integer[] sortedActual = list.toArray(new Integer[0]);
        Arrays.sort(sortedActual);
        assertTrue(Arrays.equals(sortedExpected, sortedActual), "toList 应包含全部插入元素");
    }

    // ==================== toString ====================

    @Test
    @DisplayName("空堆 toString 输出 []")
    void toStringOnEmpty() {
        SkewHeap<Integer> heap = new SkewHeap<>();
        assertEquals("[]", heap.toString());
    }

    @Test
    @DisplayName("toString 包含所有元素")
    void toStringContainsAllElements() {
        SkewHeap<Integer> heap = new SkewHeap<>(new Integer[]{10, 20, 30});
        String str = heap.toString();
        assertTrue(str.contains("10"));
        assertTrue(str.contains("20"));
        assertTrue(str.contains("30"));
    }

    // ==================== 大顶堆性质验证 ====================

    @Test
    @DisplayName("插入后满足大顶堆树形不变式")
    void heapPropertyAfterInsertions() {
        SkewHeap<Integer> heap = new SkewHeap<>();
        int[] input = {4, 1, 3, 2, 16, 9, 10, 14, 8, 7};
        for (int v : input) {
            heap.insert(v);
        }
        assertMaxHeapTreeProperty(heap);
    }

    @Test
    @DisplayName("合并后满足大顶堆树形不变式")
    void heapPropertyAfterMerge() {
        SkewHeap<Integer> h1 = new SkewHeap<>(new Integer[]{3, 10, 5});
        SkewHeap<Integer> h2 = new SkewHeap<>(new Integer[]{8, 1, 12});
        h1.merge(h2);
        assertMaxHeapTreeProperty(h1);
    }

    @Test
    @DisplayName("delMax 后剩余部分仍满足大顶堆不变式")
    void heapPropertyAfterDelMax() {
        SkewHeap<Integer> heap = new SkewHeap<>(new Integer[]{4, 1, 3, 2, 16, 9, 10, 14, 8, 7});
        heap.delMax(); // remove 16
        assertMaxHeapTreeProperty(heap);
        heap.delMax(); // remove 14
        assertMaxHeapTreeProperty(heap);
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

        SkewHeap<Integer> heap = new SkewHeap<>(input);
        Integer[] actual = new Integer[n];
        for (int i = 0; i < n; i++) {
            actual[i] = heap.delMax(); // 降序
        }

        // 降序出堆反转为升序应与 Arrays.sort 一致
        for (int i = 0; i < n; i++) {
            assertEquals(expected[i], actual[n - 1 - i],
                    "排序结果第 " + i + " 个元素不匹配");
        }
    }

    // ==================== 交错操作 ====================

    @Test
    @DisplayName("插入与删除交替进行结果始终正确")
    void interleavedInsertAndDelMax() {
        SkewHeap<Integer> heap = new SkewHeap<>();
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
        SkewHeap<Integer> main = new SkewHeap<>();
        main.insert(5);

        SkewHeap<Integer> batch1 = new SkewHeap<>(new Integer[]{10, 1});
        main.merge(batch1);
        assertEquals(10, main.delMax().intValue());

        SkewHeap<Integer> batch2 = new SkewHeap<>(new Integer[]{20, 15});
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
        SkewHeap<Integer> heap1 = new SkewHeap<>();
        SkewHeap<Integer> heap2 = new SkewHeap<>();
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

        // 全部出堆应为降序
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

    /**
     * 通过反射访问 SkewHeap 内部的 root 和 Node 结构，
     * 递归验证每个节点的值 >= 其左右子节点的值（大顶堆不变式）。
     */
    private static void assertMaxHeapTreeProperty(SkewHeap<?> heap) {
        try {
            Field rootField = SkewHeap.class.getDeclaredField("root");
            rootField.setAccessible(true);
            Object rootNode = rootField.get(heap);
            if (rootNode != null) {
                assertNodeHeapProperty(rootNode);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("反射访问 SkewHeap 内部结构失败", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void assertNodeHeapProperty(Object node) {
        try {
            Class<?> nodeClass = node.getClass();
            Field itemField = nodeClass.getDeclaredField("item");
            Field leftField = nodeClass.getDeclaredField("left");
            Field rightField = nodeClass.getDeclaredField("right");
            itemField.setAccessible(true);
            leftField.setAccessible(true);
            rightField.setAccessible(true);

            Comparable item = (Comparable) itemField.get(node);
            Object left = leftField.get(node);
            Object right = rightField.get(node);

            if (left != null) {
                Comparable leftItem = (Comparable) itemField.get(left);
                assertTrue(item.compareTo(leftItem) >= 0,
                        "堆性质被破坏: parent=" + item + " < leftChild=" + leftItem);
                assertNodeHeapProperty(left);
            }
            if (right != null) {
                Comparable rightItem = (Comparable) itemField.get(right);
                assertTrue(item.compareTo(rightItem) >= 0,
                        "堆性质被破坏: parent=" + item + " < rightChild=" + rightItem);
                assertNodeHeapProperty(right);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("反射访问 Node 失败", e);
        }
    }
}
