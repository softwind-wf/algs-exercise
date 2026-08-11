package edu.princeton.cs.algs4;

import com.ds.stackqueue.MyLinkedDeque;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MyLinkedDequeTest {

    // ==================== 空队列测试 ====================

    @Test
    void testEmptyDeque() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        assertEquals(0, dq.size());
        assertTrue(dq.isEmpty());
        assertNull(dq.peekFirst());
        assertNull(dq.peekLast());
        assertNull(dq.removeFirst());
        assertNull(dq.removeLast());
        assertNull(dq.pop());
        assertNull(dq.dequeue());
        assertFalse(dq.contains(1));
        assertEquals(-1, dq.indexOf(1));
        assertFalse(dq.remove(1));
        assertEquals("[] (空双端队列)", dq.toString());
    }

    // ==================== addFirst / removeFirst / peekFirst ====================

    @Test
    void testAddFirstAndPeekFirst() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addFirst("A");
        assertEquals(1, dq.size());
        assertEquals("A", dq.peekFirst());
        assertEquals("A", dq.peekLast());

        dq.addFirst("B");
        assertEquals(2, dq.size());
        assertEquals("B", dq.peekFirst());
        assertEquals("A", dq.peekLast());
    }

    @Test
    void testRemoveFirst() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addFirst(10);
        dq.addFirst(20);
        dq.addFirst(30);

        assertEquals(Integer.valueOf(30), dq.removeFirst());
        assertEquals(2, dq.size());
        assertEquals(Integer.valueOf(20), dq.removeFirst());
        assertEquals(1, dq.size());
        assertEquals(Integer.valueOf(10), dq.removeFirst());
        assertEquals(0, dq.size());
        assertTrue(dq.isEmpty());
        assertNull(dq.removeFirst());
    }

    // ==================== addLast / removeLast / peekLast ====================

    @Test
    void testAddLastAndPeekLast() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addLast("X");
        assertEquals(1, dq.size());
        assertEquals("X", dq.peekFirst());
        assertEquals("X", dq.peekLast());

        dq.addLast("Y");
        assertEquals(2, dq.size());
        assertEquals("X", dq.peekFirst());
        assertEquals("Y", dq.peekLast());
    }

    @Test
    void testRemoveLast() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addLast(1);
        dq.addLast(2);
        dq.addLast(3);

        assertEquals(Integer.valueOf(3), dq.removeLast());
        assertEquals(2, dq.size());
        assertEquals(Integer.valueOf(2), dq.removeLast());
        assertEquals(1, dq.size());
        assertEquals(Integer.valueOf(1), dq.removeLast());
        assertEquals(0, dq.size());
        assertTrue(dq.isEmpty());
        assertNull(dq.removeLast());
    }

    // ==================== 混合 addFirst + addLast ====================

    @Test
    void testMixAddFirstAndAddLast() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addFirst(20);
        dq.addFirst(10);
        dq.addLast(30);
        dq.addLast(40);

        assertEquals(4, dq.size());
        assertEquals(Integer.valueOf(10), dq.peekFirst());
        assertEquals(Integer.valueOf(40), dq.peekLast());

        assertEquals(Integer.valueOf(10), dq.removeFirst());
        assertEquals(Integer.valueOf(40), dq.removeLast());
        assertEquals(Integer.valueOf(20), dq.removeFirst());
        assertEquals(Integer.valueOf(30), dq.removeLast());
        assertTrue(dq.isEmpty());
    }

    @Test
    void testMixRemoveFirstAndRemoveLast() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        for (int i = 1; i <= 5; i++) {
            dq.addLast(i);
        }

        assertEquals(Integer.valueOf(1), dq.removeFirst());
        assertEquals(Integer.valueOf(5), dq.removeLast());
        assertEquals(Integer.valueOf(2), dq.removeFirst());
        assertEquals(Integer.valueOf(4), dq.removeLast());
        assertEquals(Integer.valueOf(3), dq.removeFirst());
        assertTrue(dq.isEmpty());
    }

    // ==================== 栈语义 push/pop/peek ====================

    @Test
    void testStackSemantics() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.push(1);
        dq.push(2);
        dq.push(3);

        assertEquals(3, dq.size());
        assertEquals(Integer.valueOf(3), dq.peek());
        assertEquals(Integer.valueOf(3), dq.pop());
        assertEquals(Integer.valueOf(2), dq.peek());
        assertEquals(Integer.valueOf(2), dq.pop());
        assertEquals(Integer.valueOf(1), dq.pop());
        assertTrue(dq.isEmpty());
        assertNull(dq.pop());
    }

    @Test
    void testStackEmptyPeekAndPop() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        assertNull(dq.peek());
        assertNull(dq.pop());
    }

    // ==================== 队列语义 enqueue/dequeue ====================

    @Test
    void testQueueSemantics() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.enqueue("first");
        dq.enqueue("second");
        dq.enqueue("third");

        assertEquals(3, dq.size());
        assertEquals("first", dq.peekFirst());
        assertEquals("first", dq.dequeue());
        assertEquals("second", dq.dequeue());
        assertEquals("third", dq.dequeue());
        assertTrue(dq.isEmpty());
        assertNull(dq.dequeue());
    }

    @Test
    void testQueueEmptyDequeue() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        assertNull(dq.dequeue());
    }

    // ==================== 按索引访问 get / set ====================

    @Test
    void testGetByIndex() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        for (int i = 0; i < 5; i++) {
            dq.addLast(i * 100);
        }

        assertEquals(Integer.valueOf(0), dq.get(0));
        assertEquals(Integer.valueOf(100), dq.get(1));
        assertEquals(Integer.valueOf(200), dq.get(2));
        assertEquals(Integer.valueOf(300), dq.get(3));
        assertEquals(Integer.valueOf(400), dq.get(4));
    }

    @Test
    void testSetByIndex() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addLast("a");
        dq.addLast("b");
        dq.addLast("c");

        dq.set(1, "BB");
        assertEquals("a", dq.get(0));
        assertEquals("BB", dq.get(1));
        assertEquals("c", dq.get(2));
        assertEquals(3, dq.size());
    }

    @Test
    void testSetHeadAndTail() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addLast(1);
        dq.addLast(2);
        dq.addLast(3);

        dq.set(0, 999);
        assertEquals(Integer.valueOf(999), dq.peekFirst());

        dq.set(2, 777);
        assertEquals(Integer.valueOf(777), dq.peekLast());
    }

    // ==================== 索引越界异常 ====================

    @Test
    void testGetIndexOutOfBounds() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addLast(1);
        dq.addLast(2);

        assertThrows(IllegalArgumentException.class, () -> dq.get(-1));
        assertThrows(IllegalArgumentException.class, () -> dq.get(2));
        assertThrows(IllegalArgumentException.class, () -> dq.get(100));
    }

    @Test
    void testSetIndexOutOfBounds() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addLast(1);

        assertThrows(IllegalArgumentException.class, () -> dq.set(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> dq.set(1, 0));
        assertThrows(IllegalArgumentException.class, () -> dq.set(100, 0));
    }

    @Test
    void testGetOnEmptyDeque() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        assertThrows(IllegalArgumentException.class, () -> dq.get(0));
    }

    // ==================== contains / indexOf ====================

    @Test
    void testContains() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addLast("apple");
        dq.addLast("banana");
        dq.addLast("cherry");

        assertTrue(dq.contains("apple"));
        assertTrue(dq.contains("banana"));
        assertTrue(dq.contains("cherry"));
        assertFalse(dq.contains("durian"));
        assertFalse(dq.contains(""));
    }

    @Test
    void testIndexOf() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addLast(10);
        dq.addLast(20);
        dq.addLast(30);
        dq.addLast(20);

        assertEquals(0, dq.indexOf(10));
        assertEquals(1, dq.indexOf(20));
        assertEquals(2, dq.indexOf(30));
        assertEquals(-1, dq.indexOf(40));
    }

    @Test
    void testContainsNullElement() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addLast("a");
        dq.addLast(null);
        dq.addLast("b");

        assertTrue(dq.contains(null));
        assertEquals(1, dq.indexOf(null));
    }

    // ==================== 按值删除 remove(E) ====================

    @Test
    void testRemoveByValue() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addLast(1);
        dq.addLast(2);
        dq.addLast(3);
        dq.addLast(2);

        assertTrue(dq.remove(Integer.valueOf(2)));
        assertEquals(3, dq.size());
        assertEquals(Integer.valueOf(1), dq.get(0));
        assertEquals(Integer.valueOf(3), dq.get(1));
        assertEquals(Integer.valueOf(2), dq.get(2));

        assertTrue(dq.remove(Integer.valueOf(1)));
        assertEquals(2, dq.size());
        assertEquals(Integer.valueOf(3), dq.peekFirst());

        assertFalse(dq.remove(Integer.valueOf(999)));
        assertEquals(2, dq.size());
    }

    @Test
    void testRemoveFirstAndLastByValue() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addLast("X");
        dq.addLast("Y");
        dq.addLast("Z");

        assertTrue(dq.remove("X"));
        assertEquals("Y", dq.peekFirst());

        assertTrue(dq.remove("Z"));
        assertEquals("Y", dq.peekLast());
    }

    @Test
    void testRemoveByValueEmptyDeque() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        assertFalse(dq.remove(1));
    }

    @Test
    void testRemoveNullByValue() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addLast("a");
        dq.addLast(null);
        dq.addLast("b");

        assertTrue(dq.remove(null));
        assertEquals(2, dq.size());
        assertFalse(dq.contains(null));
    }

    // ==================== clear ====================

    @Test
    void testClear() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            dq.addLast(i);
        }
        assertEquals(10, dq.size());

        dq.clear();
        assertEquals(0, dq.size());
        assertTrue(dq.isEmpty());
        assertNull(dq.peekFirst());
        assertNull(dq.peekLast());
    }

    @Test
    void testClearThenReuse() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addLast("data");
        dq.clear();

        dq.addFirst("new");
        assertEquals(1, dq.size());
        assertEquals("new", dq.peekFirst());
    }

    // ==================== 批量操作 ====================

    @Test
    void testAddAllFirst() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addAllFirst(1, 2, 3);

        assertEquals(3, dq.size());
        assertEquals(Integer.valueOf(3), dq.peekFirst());
        assertEquals(Integer.valueOf(1), dq.peekLast());
        assertEquals(Integer.valueOf(3), dq.get(0));
        assertEquals(Integer.valueOf(2), dq.get(1));
        assertEquals(Integer.valueOf(1), dq.get(2));
    }

    @Test
    void testAddAllLast() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        dq.addAllLast("a", "b", "c");

        assertEquals(3, dq.size());
        assertEquals("a", dq.peekFirst());
        assertEquals("c", dq.peekLast());
        assertEquals("a", dq.get(0));
        assertEquals("b", dq.get(1));
        assertEquals("c", dq.get(2));
    }

    @Test
    void testAddAllFirstThenAddAllLast() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addAllFirst(10, 20);
        dq.addAllLast(30, 40);

        assertEquals(4, dq.size());
        assertEquals(Integer.valueOf(20), dq.peekFirst());
        assertEquals(Integer.valueOf(40), dq.peekLast());
    }

    // ==================== toArray ====================

    @Test
    void testToArray() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addLast(1);
        dq.addLast(2);
        dq.addLast(3);

        Object[] arr = dq.toArray();
        assertEquals(3, arr.length);
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(3, arr[2]);
    }

    @Test
    void testToArrayEmpty() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();
        Object[] arr = dq.toArray();
        assertEquals(0, arr.length);
    }

    // ==================== toString ====================

    @Test
    void testToString() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        assertEquals("[] (空双端队列)", dq.toString());

        dq.addLast(10);
        dq.addLast(20);
        assertTrue(dq.toString().contains("10"));
        assertTrue(dq.toString().contains("20"));
        assertTrue(dq.toString().contains("↔"));
    }

    // ==================== 大容量测试 ====================

    @Test
    void testLargeDataSet() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        int n = 5000;

        for (int i = 0; i < n; i++) {
            dq.addLast(i);
        }
        assertEquals(n, dq.size());
        assertEquals(Integer.valueOf(0), dq.peekFirst());
        assertEquals(Integer.valueOf(n - 1), dq.peekLast());

        for (int i = 0; i < n; i++) {
            assertEquals(Integer.valueOf(i), dq.get(i));
        }
    }

    @Test
    void testLargeDataSetRemoveAll() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        int n = 2000;

        for (int i = 0; i < n; i++) {
            dq.addLast(i);
        }

        for (int i = 0; i < n; i++) {
            assertEquals(Integer.valueOf(i), dq.removeFirst());
        }
        assertTrue(dq.isEmpty());
    }

    @Test
    void testLargeDataSetRemoveFromBothEnds() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        int n = 1000;

        for (int i = 0; i < n; i++) {
            dq.addLast(i);
        }

        for (int i = 0; i < n / 2; i++) {
            assertEquals(Integer.valueOf(i), dq.removeFirst());
            assertEquals(Integer.valueOf(n - 1 - i), dq.removeLast());
        }
        assertTrue(dq.isEmpty());
    }

    // ==================== 综合场景 ====================

    @Test
    void testAlternateAddAndRemove() {
        MyLinkedDeque<String> dq = new MyLinkedDeque<>();

        dq.addFirst("A");
        dq.addLast("B");
        assertEquals("A", dq.removeFirst());

        dq.addFirst("C");
        dq.addLast("D");
        assertEquals("D", dq.removeLast());

        assertEquals("C", dq.peekFirst());
        assertEquals("B", dq.peekLast());
        assertEquals(2, dq.size());
    }

    @Test
    void testSingleElementAllOperations() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        dq.addFirst(42);

        assertEquals(1, dq.size());
        assertEquals(Integer.valueOf(42), dq.peekFirst());
        assertEquals(Integer.valueOf(42), dq.peekLast());
        assertEquals(Integer.valueOf(42), dq.peek());
        assertEquals(Integer.valueOf(42), dq.get(0));
        assertTrue(dq.contains(42));
        assertEquals(0, dq.indexOf(42));

        dq.set(0, 99);
        assertEquals(Integer.valueOf(99), dq.get(0));

        Integer removed = dq.removeLast();
        assertEquals(Integer.valueOf(99), removed);
        assertTrue(dq.isEmpty());
    }

    @Test
    void testSetUpdatesCorrectPosition() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        for (int i = 0; i < 10; i++) {
            dq.addLast(i);
        }

        dq.set(3, 333);
        dq.set(7, 777);

        assertEquals(Integer.valueOf(333), dq.get(3));
        assertEquals(Integer.valueOf(777), dq.get(7));
        assertEquals(Integer.valueOf(0), dq.get(0));
        assertEquals(Integer.valueOf(9), dq.get(9));
    }

    @Test
    void testGetFromBothEnds() {
        MyLinkedDeque<Integer> dq = new MyLinkedDeque<>();
        int n = 100;
        for (int i = 0; i < n; i++) {
            dq.addLast(i);
        }

        assertEquals(Integer.valueOf(0), dq.get(0));
        assertEquals(Integer.valueOf(49), dq.get(49));
        assertEquals(Integer.valueOf(50), dq.get(50));
        assertEquals(Integer.valueOf(99), dq.get(99));
    }
}
