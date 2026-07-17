import com.ds.linked.MyDoubleLinkedList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MyDoubleLinkedListTest {

    // 测试初始化空链表的基本属性
    @Test
    void testInitialEmptyList() {
        MyDoubleLinkedList<String> list = new MyDoubleLinkedList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertNull(list.getFirst());
        assertNull(list.getLast());
        assertNull(list.removeFirst());
        assertNull(list.removeLast());
    }

    // 测试add和toString
    @Test
    void testAddAndToString() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals("1 2 3 ", list.toString());
        assertEquals(3, list.size());
        assertFalse(list.isEmpty());
    }

    // 测试addFirst头部插入
    @Test
    void testAddFirst() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.addFirst(1);
        list.addFirst(2);
        list.addFirst(3);
        assertEquals("3 2 1 ", list.toString());
        assertEquals(3, list.size());
    }

    // 测试addLast尾部插入
    @Test
    void testAddLast() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.addLast(1);
        list.addLast(2);
        list.addLast(3);
        assertEquals("1 2 3 ", list.toString());
    }

    // 测试get按下标获取元素
    @Test
    void testGet() {
        MyDoubleLinkedList<String> list = new MyDoubleLinkedList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");

        assertEquals("a", list.get(0));
        assertEquals("c", list.get(2));
        assertEquals("e", list.get(4));
        // 测试从尾部方向获取（index >= size/2）
        assertEquals("d", list.get(3));
        assertEquals("e", list.get(4));
    }

    // 测试get越界异常
    @Test
    void testGetThrowsOnInvalidIndex() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(2);

        assertThrows(IllegalArgumentException.class, () -> list.get(-1));
        assertThrows(IllegalArgumentException.class, () -> list.get(2));
        assertThrows(IllegalArgumentException.class, () -> list.get(100));
    }

    // 测试insert在中间位置插入
    @Test
    void testInsert() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(3);
        list.add(5);

        // 在中间插入
        list.insert(2, 1);
        assertEquals("1 2 3 5 ", list.toString());

        // 在头部插入
        list.insert(0, 0);
        assertEquals("0 1 2 3 5 ", list.toString());

        // 在尾部插入
        list.insert(6, list.size());
        assertEquals("0 1 2 3 5 6 ", list.toString());
    }

    // 测试insert越界异常
    @Test
    void testInsertThrowsOnInvalidIndex() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);

        assertThrows(IllegalArgumentException.class, () -> list.insert(10, -1));
        assertThrows(IllegalArgumentException.class, () -> list.insert(10, 5));
    }

    // 测试remove按下标删除
    @Test
    void testRemoveByIndex() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);

        // 删除中间元素
        assertEquals(3, list.remove(2));
        assertEquals("1 2 4 5 ", list.toString());

        // 删除头部元素
        assertEquals(1, list.remove(0));
        assertEquals("2 4 5 ", list.toString());

        // 删除尾部元素
        assertEquals(5, list.remove(2));
        assertEquals("2 4 ", list.toString());
    }

    // 测试remove越界异常
    @Test
    void testRemoveThrowsOnInvalidIndex() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);

        assertThrows(IllegalArgumentException.class, () -> list.remove(-1));
        assertThrows(IllegalArgumentException.class, () -> list.remove(1));
    }

    // 测试remove按值删除
    @Test
    void testRemoveByValue() {
        MyDoubleLinkedList<String> list = new MyDoubleLinkedList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");

        assertTrue(list.remove("b"));
        assertEquals("a c d ", list.toString());
        assertEquals(3, list.size());

        // 删除不存在的元素
        assertFalse(list.remove("z"));
        assertEquals(3, list.size());

        // 删除头部和尾部
        assertTrue(list.remove("a"));
        assertTrue(list.remove("d"));
        assertEquals("c ", list.toString());
    }

    // 测试removeFirst和removeLast
    @Test
    void testRemoveFirstAndRemoveLast() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(1, list.removeFirst());
        assertEquals("2 3 ", list.toString());

        assertEquals(3, list.removeLast());
        assertEquals("2 ", list.toString());

        assertEquals(2, list.removeFirst());
        assertTrue(list.isEmpty());
    }

    // 测试getFirst和getLast
    @Test
    void testGetFirstAndGetLast() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(10);
        list.add(20);
        list.add(30);

        assertEquals(10, list.getFirst());
        assertEquals(30, list.getLast());
        // 获取后链表不变
        assertEquals(3, list.size());
    }

    // 测试update更新元素
    @Test
    void testUpdate() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        list.update(1, 20);
        assertEquals("1 20 3 ", list.toString());

        list.update(0, 10);
        assertEquals("10 20 3 ", list.toString());

        list.update(2, 30);
        assertEquals("10 20 30 ", list.toString());
    }

    // 测试update越界异常
    @Test
    void testUpdateThrowsOnInvalidIndex() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);

        assertThrows(IllegalArgumentException.class, () -> list.update(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> list.update(1, 10));
    }

    // 测试contains和indexOf
    @Test
    void testContainsAndIndexOf() {
        MyDoubleLinkedList<String> list = new MyDoubleLinkedList<>();
        list.add("apple");
        list.add("banana");
        list.add("cherry");

        assertTrue(list.contains("banana"));
        assertFalse(list.contains("grape"));
        assertEquals(0, list.indexOf("apple"));
        assertEquals(1, list.indexOf("banana"));
        assertEquals(2, list.indexOf("cherry"));
        assertEquals(-1, list.indexOf("grape"));
    }

    // 测试contains处理null元素
    @Test
    void testContainsWithNull() {
        MyDoubleLinkedList<String> list = new MyDoubleLinkedList<>();
        list.add(null);
        list.add("a");

        assertTrue(list.contains(null));
        assertEquals(0, list.indexOf(null));
        assertFalse(list.contains("b"));
    }

    // 测试clear清空链表
    @Test
    void testClear() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, list.size());

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("", list.toString());
    }

    // 测试重复元素
    @Test
    void testDuplicateElements() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(2);
        list.add(1);
        list.add(2);

        assertEquals("1 2 1 2 ", list.toString());
        assertEquals(0, list.indexOf(1));
        assertTrue(list.remove(list.get(0)));
        assertEquals("2 1 2 ", list.toString());
    }

    // 测试大量元素的插入和删除（含双向遍历优化路径覆盖）
    @Test
    void testLargeListOperations() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        int n = 100;

        // 尾部添加100个元素
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        assertEquals(n, list.size());

        // 验证头中部和尾部的get（覆盖双向遍历两个分支）
        assertEquals(0, list.get(0));
        assertEquals(50, list.get(50));
        assertEquals(99, list.get(99));

        // 删除头中部元素
        assertEquals(0, list.remove(0));
        // 删除尾部元素
        assertEquals(99, list.remove(98));
        assertEquals(98, list.size());
    }

    // 测试字符串类型元素
    @Test
    void testWithStringType() {
        MyDoubleLinkedList<String> list = new MyDoubleLinkedList<>();
        list.add("hello");
        list.add("world");
        list.addFirst("say");

        assertEquals("say hello world ", list.toString());
        assertEquals("say", list.getFirst());
        assertEquals("world", list.getLast());
        assertTrue(list.contains("hello"));
    }

    // 测试单个元素的边界情况
    @Test
    void testSingleElement() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(42);

        assertEquals(1, list.size());
        assertEquals(42, list.get(0));
        assertEquals(42, list.getFirst());
        assertEquals(42, list.getLast());
        assertFalse(list.isEmpty());

        assertEquals(42, list.remove(0));
        assertTrue(list.isEmpty());
    }

    // 测试连续头部和尾部操作交替
    @Test
    void testMixedHeadTailOperations() {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.addLast(2);
        list.addFirst(1);
        list.addLast(3);
        list.addFirst(0);
        // 链表: 0 1 2 3
        assertEquals("0 1 2 3 ", list.toString());

        assertEquals(0, list.removeFirst());
        assertEquals(3, list.removeLast());
        // 链表: 1 2
        assertEquals("1 2 ", list.toString());
        assertEquals(2, list.size());
    }
}
