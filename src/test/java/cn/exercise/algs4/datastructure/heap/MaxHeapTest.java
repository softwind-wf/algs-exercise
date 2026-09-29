package cn.exercise.algs4.datastructure.heap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link MaxHeap} 的 JUnit 5 单元测试。
 *
 * 覆盖范围：
 *   - 三个构造方法（默认容量 / 指定容量 / 数组 heapify）与非法参数
 *   - insert / delMax / peek 的功能与出堆顺序（降序）
 *   - 重复元素、负数、String 等不同元素类型
 *   - 动态扩容（插入远超初始容量的元素）
 *   - 空堆的 NoSuchElementException、插入 null 的 IllegalArgumentException
 *   - 通过反射直接校验「父节点 >= 子节点」的大顶堆不变式
 *   - 与 Arrays.sort 对照的排序正确性（heap sort）
 */
@DisplayName("大顶堆 MaxHeap 测试")
class MaxHeapTest {

    // ==================== 基础查询与构造 ====================

    @Test
    @DisplayName("新建堆应为空")
    void shouldBeEmptyOnCreation() {
        MaxHeap<Integer> heap = new MaxHeap<>();
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    @DisplayName("指定容量创建后仍为空")
    void shouldBeEmptyWithGivenCapacity() {
        MaxHeap<Integer> heap = new MaxHeap<>(10);
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("非法初始容量应抛 IllegalArgumentException")
    void invalidCapacityShouldThrow(int capacity) {
        assertThrows(IllegalArgumentException.class, () -> new MaxHeap<Integer>(capacity));
    }

    @Test
    @DisplayName("容量为 1 是合法的边界")
    void capacityOneIsValid() {
        MaxHeap<Integer> heap = new MaxHeap<>(1);
        heap.insert(42);
        assertEquals(42, heap.peek().intValue());
        assertEquals(1, heap.size());
    }

    // ==================== 单元素与基本插入删除 ====================

    @Test
    @DisplayName("插入单个元素后 peek/delMax 行为正确")
    void insertSingleElement() {
        MaxHeap<Integer> heap = new MaxHeap<>();
        heap.insert(5);
        assertFalse(heap.isEmpty());
        assertEquals(1, heap.size());
        assertEquals(5, heap.peek().intValue());   // peek 不移除
        assertEquals(1, heap.size());
        assertEquals(5, heap.delMax().intValue()); // delMax 移除
        assertTrue(heap.isEmpty());
    }

    @Test
    @DisplayName("delMax 严格按降序返回")
    void delMaxReturnsInDescendingOrder() {
        MaxHeap<Integer> heap = new MaxHeap<>(10);
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
        MaxHeap<Integer> heap = new MaxHeap<>();
        for (int i = 1; i <= 5; i++) {
            heap.insert(i);
        }
        for (int expected = 5; expected >= 1; expected--) {
            assertEquals(expected, heap.delMax().intValue());
        }
    }

    @Test
    @DisplayName("降序插入也能正确降序出堆")
    void descendingInsertion() {
        MaxHeap<Integer> heap = new MaxHeap<>();
        for (int i = 5; i >= 1; i--) {
            heap.insert(i);
        }
        for (int expected = 5; expected >= 1; expected--) {
            assertEquals(expected, heap.delMax().intValue());
        }
    }

    // ==================== 特殊元素 ====================

    @Test
    @DisplayName("重复元素全部保留并按序出堆")
    void duplicateElements() {
        MaxHeap<Integer> heap = new MaxHeap<>();
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
        MaxHeap<Integer> heap = new MaxHeap<>();
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
        MaxHeap<String> heap = new MaxHeap<>();
        heap.insert("apple");
        heap.insert("zebra");
        heap.insert("banana");
        heap.insert("cat");
        assertEquals("zebra", heap.delMax());
        assertEquals("cat", heap.delMax());
        assertEquals("banana", heap.delMax());
        assertEquals("apple", heap.delMax());
    }

    // ==================== 异常边界 ====================

    @Test
    @DisplayName("插入 null 抛 IllegalArgumentException")
    void insertNullShouldThrow() {
        MaxHeap<Integer> heap = new MaxHeap<>();
        assertThrows(IllegalArgumentException.class, () -> heap.insert(null));
    }

    @Test
    @DisplayName("空堆 peek 抛 NoSuchElementException")
    void peekOnEmptyShouldThrow() {
        MaxHeap<Integer> heap = new MaxHeap<>();
        assertThrows(NoSuchElementException.class, heap::peek);
    }

    @Test
    @DisplayName("空堆 delMax 抛 NoSuchElementException")
    void delMaxOnEmptyShouldThrow() {
        MaxHeap<Integer> heap = new MaxHeap<>();
        assertThrows(NoSuchElementException.class, heap::delMax);
    }

    // ==================== heapify 批量建堆 ====================

    @Test
    @DisplayName("数组构造 O(n) 建堆后按降序出堆")
    void buildFromArray() {
        Integer[] items = {9, 1, 6, 4, 3, 8, 5, 2, 7};
        MaxHeap<Integer> heap = new MaxHeap<>(items);
        assertEquals(items.length, heap.size());
        int expected = 9;
        while (!heap.isEmpty()) {
            assertEquals(expected--, heap.delMax().intValue());
        }
    }

    @Test
    @DisplayName("数组构造不修改入参数组")
    void buildFromArrayDoesNotMutateInput() {
        Integer[] items = {9, 1, 6, 4, 3, 8, 5, 2, 7};
        Integer[] copy = items.clone();
        new MaxHeap<>(items);
        assertArrayEqualsIgnoringType(copy, items);
    }

    @Test
    @DisplayName("空数组建堆后合法可插入")
    void buildFromEmptyArray() {
        MaxHeap<Integer> heap = new MaxHeap<>(new Integer[0]);
        assertTrue(heap.isEmpty());
        heap.insert(7);
        assertEquals(7, heap.peek().intValue());
    }

    @Test
    @DisplayName("heapify 之后仍满足大顶堆不变式")
    void heapifyPreservesHeapProperty() {
        Integer[] items = {16, 4, 10, 14, 7, 9, 3, 2, 8, 1};
        MaxHeap<Integer> heap = new MaxHeap<>(items);
        assertMaxHeapProperty(heap);
    }

    // ==================== 扩容与混合操作 ====================

    @Test
    @DisplayName("插入远超初始容量时自动扩容且数据完整")
    void growsBeyondInitialCapacity() {
        MaxHeap<Integer> heap = new MaxHeap<>(1);   // 反复扩容
        int n = 1000;
        for (int i = 0; i < n; i++) {
            heap.insert(i);
        }
        assertEquals(n, heap.size());
        assertEquals(n - 1, heap.delMax().intValue());
        assertMaxHeapProperty(heap);
    }

    @Test
    @DisplayName("插入与删除交替进行结果始终正确")
    void interleavedInsertAndDelMax() {
        MaxHeap<Integer> heap = new MaxHeap<>();
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

    // ==================== toString ====================

    @Test
    @DisplayName("toString 只展示堆内元素且首元素是最大值")
    void toStringReflectsContents() {
        MaxHeap<Integer> heap = new MaxHeap<>();
        assertEquals("[]", heap.toString());
        heap.insert(1);
        heap.insert(10);
        heap.insert(5);
        String str = heap.toString();
        assertTrue(str.startsWith("[10,"), "堆顶(最大值)应排在最前: " + str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("5"));
    }

    // ==================== 排序正确性（对照 Arrays.sort）====================

    @Test
    @DisplayName("反复 delMax 等价于一次正确的堆排序")
    void delMaxProducesCorrectSort() {
        Random random = new Random(20260929L); // 固定种子保证可复现
        int n = 500;
        Integer[] input = new Integer[n];
        for (int i = 0; i < n; i++) {
            input[i] = random.nextInt(1000) - 500; // 含负数、含重复
        }

        Integer[] expected = input.clone();
        Arrays.sort(expected); // 升序

        MaxHeap<Integer> heap = new MaxHeap<>(input);
        int[] actual = new int[n];
        for (int i = 0; i < n; i++) {
            actual[i] = heap.delMax(); // 降序
        }

        // 降序出堆反转为升序后应与 Arrays.sort 完全一致
        for (int i = 0; i < n; i++) {
            assertEquals(expected[i].intValue(), actual[n - 1 - i],
                    "第 " + i + " 个排序结果不匹配");
        }
    }

    // ==================== topK 实例方法 ====================

    @Test
    @DisplayName("topK(k) 返回最大的 k 个元素且降序排列")
    void instanceTopKReturnsLargestInDescendingOrder() {
        Integer[] items = {3, 8, 1, 9, 2, 7, 4, 6, 5, 0};
        MaxHeap<Integer> heap = new MaxHeap<>(items);
        assertEquals(Arrays.asList(9, 8, 7), heap.topK(3));
        assertEquals(Arrays.asList(9, 8), heap.topK(2));
        assertEquals(Arrays.asList(9), heap.topK(1));
    }

    @Test
    @DisplayName("topK 不修改原堆，可重复调用得到相同结果")
    void instanceTopKDoesNotModifyHeap() {
        Integer[] items = {3, 8, 1, 9, 2, 7, 4, 6, 5, 0};
        MaxHeap<Integer> heap = new MaxHeap<>(items);
        int sizeBefore = heap.size();
        String reprBefore = heap.toString();

        List<Integer> first = heap.topK(5);

        assertEquals(sizeBefore, heap.size(), "topK 不应改变堆大小");
        assertEquals(reprBefore, heap.toString(), "topK 不应改变堆内部结构");
        assertEquals(9, heap.peek().intValue(), "堆顶应仍为最大值");
        assertEquals(first, heap.topK(5), "重复调用结果应稳定");
        assertEquals(sizeBefore, heap.size());
    }

    @Test
    @DisplayName("topK(0) 返回空列表")
    void instanceTopKZeroReturnsEmpty() {
        MaxHeap<Integer> heap = new MaxHeap<>(new Integer[]{1, 2, 3});
        assertTrue(heap.topK(0).isEmpty());
    }

    @Test
    @DisplayName("topK(k>=size) 返回全部元素降序")
    void instanceTopKLargerThanSizeReturnsAll() {
        Integer[] items = {3, 8, 1, 9, 2, 7, 4, 6, 5, 0};
        MaxHeap<Integer> heap = new MaxHeap<>(items);
        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), heap.topK(100));
        assertEquals(heap.size(), heap.topK(100).size());
    }

    @Test
    @DisplayName("topK(负数) 抛 IllegalArgumentException")
    void instanceTopKNegativeThrows() {
        MaxHeap<Integer> heap = new MaxHeap<>(new Integer[]{1, 2, 3});
        assertThrows(IllegalArgumentException.class, () -> heap.topK(-1));
    }

    @Test
    @DisplayName("空堆 topK 返回空列表且不抛异常")
    void instanceTopKOnEmptyHeapReturnsEmpty() {
        MaxHeap<Integer> empty = new MaxHeap<>();
        assertTrue(empty.topK(3).isEmpty());
        assertTrue(empty.topK(0).isEmpty());
    }

    @Test
    @DisplayName("topK 正确处理重复元素")
    void instanceTopKWithDuplicates() {
        MaxHeap<Integer> heap = new MaxHeap<>(new Integer[]{5, 3, 5, 3, 5});
        assertEquals(Arrays.asList(5, 5, 5), heap.topK(3));
        assertEquals(Arrays.asList(5, 5, 5, 3, 3), heap.topK(5));
    }

    @Test
    @DisplayName("topK 支持 String 类型")
    void instanceTopKWithString() {
        MaxHeap<String> heap = new MaxHeap<>(
                new String[]{"apple", "zebra", "banana", "cat"});
        assertEquals(Arrays.asList("zebra", "cat"), heap.topK(2));
    }

    // ==================== topK 静态方法 ====================

    @Test
    @DisplayName("静态 topK 从数组直接取前 k 大")
    void staticTopKBasic() {
        Integer[] nums = {3, 8, 1, 9, 2, 7, 4, 6, 5, 0};
        assertEquals(Arrays.asList(9, 8, 7), MaxHeap.topK(nums, 3));
        assertEquals(Arrays.asList(9, 8, 7, 6, 5), MaxHeap.topK(nums, 5));
    }

    @Test
    @DisplayName("静态 topK 与『完整排序后取前 k 大』一致")
    void staticTopKMatchesSortedDesc() {
        Random random = new Random(12345L); // 固定种子保证可复现
        int n = 200;
        int k = 10;
        Integer[] input = new Integer[n];
        for (int i = 0; i < n; i++) {
            input[i] = random.nextInt(1000) - 500;
        }

        Integer[] sorted = input.clone();
        Arrays.sort(sorted); // 升序
        List<Integer> expected = new ArrayList<>(k);
        for (int i = n - 1; i >= n - k; i--) {
            expected.add(sorted[i]); // 最大的 k 个，降序
        }

        assertEquals(expected, MaxHeap.topK(input, k));
    }

    @Test
    @DisplayName("静态 topK：k=0 或空数组返回空列表")
    void staticTopKZeroOrEmptyArray() {
        Integer[] nums = {3, 8, 1, 9};
        assertTrue(MaxHeap.topK(nums, 0).isEmpty());
        assertTrue(MaxHeap.topK(new Integer[0], 3).isEmpty());
    }

    @Test
    @DisplayName("静态 topK：k 超过数组长度返回全部降序")
    void staticTopKKGreaterThanLength() {
        Integer[] small = {1, 2, 3};
        assertEquals(Arrays.asList(3, 2, 1), MaxHeap.topK(small, 10));
    }

    @Test
    @DisplayName("静态 topK：k 为负数抛 IllegalArgumentException")
    void staticTopKNegativeThrows() {
        Integer[] nums = {3, 8, 1};
        assertThrows(IllegalArgumentException.class, () -> MaxHeap.topK(nums, -1));
    }

    @Test
    @DisplayName("静态 topK：入参数组为 null 抛 IllegalArgumentException")
    void staticTopKNullArrayThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> MaxHeap.topK((Integer[]) null, 3));
    }

    @Test
    @DisplayName("静态 topK 不修改入参数组")
    void staticTopKDoesNotMutateInput() {
        Integer[] input = {3, 8, 1, 9, 2};
        Integer[] snapshot = input.clone();
        MaxHeap.topK(input, 2);
        assertArrayEqualsIgnoringType(snapshot, input);
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过反射读取底层数组，直接校验「父节点 >= 左右子节点」的大顶堆不变式。
     * 这是 swim / sink 正确性的最强断言。
     */
    @SuppressWarnings("unchecked")
    private static void assertMaxHeapProperty(MaxHeap<?> heap) {
        try {
            Field field = MaxHeap.class.getDeclaredField("heap");
            field.setAccessible(true);
            Object[] array = (Object[]) field.get(heap);
            int n = heap.size();
            assertNotNull(array);
            for (int k = 1; k < n; k++) {
                int parent = (k - 1) / 2;
                Comparable p = (Comparable) array[parent];
                Object child = array[k];
                assertTrue(p.compareTo(child) >= 0,
                        "堆性质被破坏: parent[" + parent + "]=" + p
                                + " < child[" + k + "]=" + child);
            }
        } catch (ReflectiveOperationException e) {
            fail("反射访问底层数组失败: " + e);
        }
    }

    /** Integer[] 逐元素比较，避免数组引用比较。 */
    private static void assertArrayEqualsIgnoringType(Integer[] expected, Integer[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "索引 " + i + " 处元素被意外修改");
        }
    }
}
