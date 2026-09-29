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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link LeftistHeap} 的 JUnit 5 单元测试。
 *
 * 覆盖：
 *   - size / isEmpty / peek / insert / delMax 的基本语义与出堆降序
 *   - 升序/降序插入、重复元素、负数、String 类型
 *   - 数组构造建堆、空数组构造
 *   - merge 合并（并清空 another）、合并 null/自身/空堆的边界
 *   - toList 层序遍历（堆顶为最大值、元素完整）
 *   - 异常：空堆 peek/delMax 抛 NoSuchElementException，insert null / merge null 抛 IllegalArgumentException
 *   - 反射校验左式堆两大不变式：堆序性 + 左倾性与 npl 正确维护
 *   - 与 Arrays.sort 对照的排序正确性
 */
@DisplayName("左式堆 LeftistHeap 测试")
class LeftistHeapTest {

    // ==================== 基础查询 ====================

    @Test
    @DisplayName("新建堆为空")
    void newHeapIsEmpty() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    @DisplayName("插入单个元素后 peek/delMax 正确")
    void insertSingleElement() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
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
    void delMaxReturnsDescending() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        for (int v : new int[]{3, 1, 5, 2, 4}) {
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
    @DisplayName("升序插入也降序出堆")
    void ascendingInsertion() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        for (int i = 1; i <= 5; i++) {
            heap.insert(i);
        }
        for (int expected = 5; expected >= 1; expected--) {
            assertEquals(expected, heap.delMax().intValue());
        }
    }

    @Test
    @DisplayName("降序插入也降序出堆")
    void descendingInsertion() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        for (int i = 5; i >= 1; i--) {
            heap.insert(i);
        }
        for (int expected = 5; expected >= 1; expected--) {
            assertEquals(expected, heap.delMax().intValue());
        }
    }

    // ==================== 特殊元素 ====================

    @Test
    @DisplayName("重复元素全部保留")
    void duplicateElements() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        for (int v : new int[]{5, 3, 5, 3, 5}) {
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
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        for (int v : new int[]{-3, -1, -5, 0, 2}) {
            heap.insert(v);
        }
        assertEquals(2, heap.delMax().intValue());
        assertEquals(0, heap.delMax().intValue());
        assertEquals(-1, heap.delMax().intValue());
        assertEquals(-3, heap.delMax().intValue());
        assertEquals(-5, heap.delMax().intValue());
    }

    @Test
    @DisplayName("支持 String 等 Comparable 类型")
    void supportsString() {
        LeftistHeap<String> heap = new LeftistHeap<>();
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
    void insertNullThrows() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        assertThrows(IllegalArgumentException.class, () -> heap.insert(null));
    }

    @Test
    @DisplayName("空堆 peek 抛 NoSuchElementException")
    void peekOnEmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> new LeftistHeap<>().peek());
    }

    @Test
    @DisplayName("空堆 delMax 抛 NoSuchElementException")
    void delMaxOnEmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> new LeftistHeap<>().delMax());
    }

    // ==================== 数组构造 ====================

    @Test
    @DisplayName("数组构造建堆后按降序出堆")
    void buildFromArray() {
        Integer[] items = {9, 1, 6, 4, 3, 8, 5, 2, 7};
        LeftistHeap<Integer> heap = new LeftistHeap<>(items);
        assertEquals(items.length, heap.size());
        int expected = 9;
        while (!heap.isEmpty()) {
            assertEquals(expected--, heap.delMax().intValue());
        }
    }

    @Test
    @DisplayName("空数组建堆得到空堆")
    void buildFromEmptyArray() {
        LeftistHeap<Integer> heap = new LeftistHeap<>(new Integer[0]);
        assertTrue(heap.isEmpty());
        heap.insert(7);
        assertEquals(7, heap.peek().intValue());
    }

    // ==================== merge 合并 ====================

    @Test
    @DisplayName("merge 合并两堆：元素并集降序出堆且源堆被清空")
    void mergeCombinesAndClearsSource() {
        LeftistHeap<Integer> h1 = new LeftistHeap<>(new Integer[]{3, 10, 5});
        LeftistHeap<Integer> h2 = new LeftistHeap<>(new Integer[]{8, 1, 12});
        h1.merge(h2);

        assertEquals(6, h1.size());
        assertTrue(h2.isEmpty(), "被合并的堆应清空");
        assertEquals(0, h2.size());

        assertEquals(12, h1.delMax().intValue());
        assertEquals(10, h1.delMax().intValue());
        assertEquals(8, h1.delMax().intValue());
        assertEquals(5, h1.delMax().intValue());
        assertEquals(3, h1.delMax().intValue());
        assertEquals(1, h1.delMax().intValue());
        assertTrue(h1.isEmpty());
    }

    @Test
    @DisplayName("merge 合并到空堆等价于接管元素")
    void mergeIntoEmptyHeap() {
        LeftistHeap<Integer> empty = new LeftistHeap<>();
        LeftistHeap<Integer> src = new LeftistHeap<>(new Integer[]{4, 9, 2});
        empty.merge(src);
        assertEquals(3, empty.size());
        assertEquals(9, empty.peek().intValue());
        assertTrue(src.isEmpty());
    }

    @Test
    @DisplayName("merge(null) 抛 IllegalArgumentException")
    void mergeNullThrows() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        assertThrows(IllegalArgumentException.class, () -> heap.merge(null));
    }

    @Test
    @DisplayName("merge 自身是无操作（不会重复或死循环）")
    void mergeSelfIsNoop() {
        LeftistHeap<Integer> heap = new LeftistHeap<>(new Integer[]{3, 10, 5});
        heap.merge(heap);
        assertEquals(3, heap.size());
        assertEquals(10, heap.peek().intValue());
        assertLeftistHeapProperty(heap);
    }

    @Test
    @DisplayName("merge 一个空堆是无操作")
    void mergeEmptySourceIsNoop() {
        LeftistHeap<Integer> heap = new LeftistHeap<>(new Integer[]{3, 10, 5});
        heap.merge(new LeftistHeap<Integer>());
        assertEquals(3, heap.size());
        assertEquals(10, heap.peek().intValue());
    }

    // ==================== toList 层序遍历 ====================

    @Test
    @DisplayName("toList 层序首个元素为最大值且元素完整")
    void toListLevelOrder() {
        Integer[] items = {3, 8, 1, 9, 2, 7, 4, 6, 5, 0};
        LeftistHeap<Integer> heap = new LeftistHeap<>(items);
        List<Integer> list = heap.toList();

        assertEquals(items.length, list.size());
        assertEquals(9, list.get(0).intValue(), "层序遍历首元素应为堆顶(最大值)");

        Integer[] sorted = list.toArray(new Integer[0]);
        Arrays.sort(sorted);
        assertArrayEqualsSorted(items, sorted);
    }

    // ==================== 不变式校验 ====================

    @Test
    @DisplayName("逐个插入后仍满足堆序性与左倾性")
    void leftistPropertyAfterInserts() {
        LeftistHeap<Integer> heap = new LeftistHeap<>();
        for (int v : new int[]{8, 3, 7, 1, 9, 4, 6, 2, 5, 10}) {
            heap.insert(v);
        }
        assertLeftistHeapProperty(heap);
    }

    @Test
    @DisplayName("数组构造后满足左式堆不变式")
    void leftistPropertyAfterBuild() {
        Integer[] items = {16, 4, 10, 14, 7, 9, 3, 2, 8, 1};
        LeftistHeap<Integer> heap = new LeftistHeap<>(items);
        assertLeftistHeapProperty(heap);
    }

    @Test
    @DisplayName("合并后满足左式堆不变式")
    void leftistPropertyAfterMerge() {
        LeftistHeap<Integer> h1 = new LeftistHeap<>(new Integer[]{3, 10, 5, 20});
        LeftistHeap<Integer> h2 = new LeftistHeap<>(new Integer[]{8, 1, 12, 15});
        h1.merge(h2);
        assertLeftistHeapProperty(h1);
    }

    @Test
    @DisplayName("每次 delMax 之后仍保持左式堆不变式")
    void leftistPropertyMaintainedDuringDelMax() {
        Integer[] items = {4, 1, 3, 2, 16, 9, 10, 14, 8, 7};
        LeftistHeap<Integer> heap = new LeftistHeap<>(items);
        while (!heap.isEmpty()) {
            assertLeftistHeapProperty(heap);
            heap.delMax();
        }
    }

    // ==================== 排序正确性 ====================

    @Test
    @DisplayName("反复 delMax 等价于一次正确排序")
    void delMaxProducesCorrectSort() {
        Random random = new Random(20260929L);
        int n = 500;
        Integer[] input = new Integer[n];
        for (int i = 0; i < n; i++) {
            input[i] = random.nextInt(1000) - 500;
        }

        Integer[] expected = input.clone();
        Arrays.sort(expected); // 升序

        LeftistHeap<Integer> heap = new LeftistHeap<>(input);
        int[] actual = new int[n];
        for (int i = 0; i < n; i++) {
            actual[i] = heap.delMax(); // 降序
        }
        for (int i = 0; i < n; i++) {
            assertEquals(expected[i].intValue(), actual[n - 1 - i],
                    "第 " + i + " 个排序结果不匹配");
        }
    }

    @Test
    @DisplayName("多次 merge 累积后整体出堆仍降序正确")
    void repeatedMergeStillCorrect() {
        LeftistHeap<Integer> total = new LeftistHeap<>();
        List<Integer> all = new ArrayList<>();
        Random random = new Random(7L);
        for (int batch = 0; batch < 5; batch++) {
            Integer[] items = new Integer[20];
            for (int i = 0; i < items.length; i++) {
                items[i] = random.nextInt(100);
                all.add(items[i]);
            }
            total.merge(new LeftistHeap<>(items));
        }
        assertEquals(100, total.size());

        Integer[] expected = all.toArray(new Integer[0]);
        Arrays.sort(expected);

        int[] actual = new int[expected.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = total.delMax();
        }
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].intValue(), actual[expected.length - 1 - i]);
        }
    }

    // ==================== 反射辅助方法 ====================

    /**
     * 校验左式堆不变式：
     *   1) 堆序性：父节点 item >= 左右子节点 item
     *   2) 左倾性：左子 npl >= 右子 npl
     *   3) npl 维护：node.npl == npl(右子) + 1（在左倾性成立时 min 即右子）
     * 同时校验从 root 遍历到的节点总数与 size() 一致。
     */
    private static void assertLeftistHeapProperty(LeftistHeap<?> heap) {
        try {
            Object root = readField(heap, "root");
            int counted = checkNode(root);
            assertEquals(heap.size(), counted, "遍历节点数与 size() 不一致");
        } catch (ReflectiveOperationException e) {
            fail("反射访问左式堆内部结构失败: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    private static int checkNode(Object node) throws ReflectiveOperationException {
        if (node == null) {
            return 0;
        }
        Comparable<Object> item = (Comparable<Object>) readField(node, "item");
        Object left = readField(node, "left");
        Object right = readField(node, "right");
        int npl = (Integer) readField(node, "npl");
        int leftNpl = nplOf(left);
        int rightNpl = nplOf(right);

        if (left != null) {
            assertTrue(item.compareTo(readField(left, "item")) >= 0,
                    "堆序性被破坏: 父=" + item + " 左子=" + readField(left, "item"));
        }
        if (right != null) {
            assertTrue(item.compareTo(readField(right, "item")) >= 0,
                    "堆序性被破坏: 父=" + item + " 右子=" + readField(right, "item"));
        }
        assertTrue(leftNpl >= rightNpl,
                "左倾性被破坏: 左npl=" + leftNpl + " < 右npl=" + rightNpl + " 于节点 " + item);
        assertEquals(rightNpl + 1, npl,
                "npl 维护不正确, 节点 " + item);

        return 1 + checkNode(left) + checkNode(right);
    }

    private static int nplOf(Object node) throws ReflectiveOperationException {
        return node == null ? -1 : (Integer) readField(node, "npl");
    }

    private static Object readField(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    /** 两个升序 Integer[] 内容是否一致。 */
    private static void assertArrayEqualsSorted(Integer[] expectedAsc, Integer[] actualAsc) {
        assertEquals(expectedAsc.length, actualAsc.length);
        Integer[] a = expectedAsc.clone();
        Integer[] b = actualAsc.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i], "第 " + i + " 个元素不一致");
        }
    }
}
