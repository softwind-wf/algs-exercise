

import exercise1.exercise1_4.DequeWithStackAndSteque;
import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

public class DequeWithStackAndStequeTest {

    // 测试空队列判空和大小
    @Test
    void testEmptyDeque() {
        DequeWithStackAndSteque<Integer> deque = new DequeWithStackAndSteque<>();
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    // 测试addFirst和removeFirst
    @Test
    void testAddFirstAndRemoveFirst() {
        DequeWithStackAndSteque<String> deque = new DequeWithStackAndSteque<>();
        deque.addFirst("A");
        deque.addFirst("B");
        deque.addFirst("C");

        assertFalse(deque.isEmpty());
        assertEquals(3, deque.size());

        assertEquals("C", deque.removeFirst());
        assertEquals("B", deque.removeFirst());
        assertEquals("A", deque.removeFirst());
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    // 测试addLast和removeLast
    @Test
    void testAddLastAndRemoveLast() {
        DequeWithStackAndSteque<Integer> deque = new DequeWithStackAndSteque<>();
        deque.addLast(10);
        deque.addLast(20);
        deque.addLast(30);

        assertFalse(deque.isEmpty());
        assertEquals(3, deque.size());

        assertEquals(30, deque.removeLast());
        assertEquals(20, deque.removeLast());
        assertEquals(10, deque.removeLast());
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    // 测试混合添加和删除
    @Test
    void testMixedOperations() {
        DequeWithStackAndSteque<Character> deque = new DequeWithStackAndSteque<>();
        deque.addFirst('X');
        deque.addLast('Y');
        deque.addFirst('Z');

        assertEquals(3, deque.size());
        assertEquals('Z', deque.removeFirst());
        assertEquals('Y', deque.removeLast());
        assertEquals('X', deque.removeFirst());
        assertTrue(deque.isEmpty());
    }

    // 测试空队列删除时的异常
    @Test
    void testRemoveFromEmptyDeque() {
        DequeWithStackAndSteque<String> deque = new DequeWithStackAndSteque<>();
        
        // 测试removeFirst异常
        assertThrows(NoSuchElementException.class, deque::removeFirst);
        // 测试removeLast异常
        assertThrows(NoSuchElementException.class, deque::removeLast);
    }
}