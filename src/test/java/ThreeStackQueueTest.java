import exercise1.exercise1_3.ThreeStackQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ThreeStackQueueTest {

    private ThreeStackQueue<Integer> queue;

    @BeforeEach
    void setUp() {
        queue = new ThreeStackQueue<>();
    }

    @Test
    void testEnqueueAndDequeueSingleElement() {
        queue.enqueue(1);
        assertEquals(1, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testEnqueueMultipleAndDequeueInOrder() {
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);

        assertEquals(1, queue.dequeue());
        assertEquals(2, queue.dequeue());
        assertEquals(3, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testInterleavedEnqueueDequeue() {
        queue.enqueue(1);
        assertEquals(1, queue.dequeue());

        queue.enqueue(2);
        queue.enqueue(3);
        assertEquals(2, queue.dequeue());

        queue.enqueue(4);
        assertEquals(3, queue.dequeue());
        assertEquals(4, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testDequeueOnEmptyQueueThrowsException() {
        assertThrows(RuntimeException.class, () -> queue.dequeue());
    }

    @Test
    void testIsEmptyInitially() {
        assertTrue(queue.isEmpty());
    }

    @Test
    void testIsEmptyAfterEnqueueAndDequeue() {
        queue.enqueue(1);
        assertFalse(queue.isEmpty());
        queue.dequeue();
        assertTrue(queue.isEmpty());
    }

    @Test
    void testLargeNumberOfOperations() {
        int n = 1000;
        for (int i = 0; i < n; i++) {
            queue.enqueue(i);
        }
        for (int i = 0; i < n; i++) {
            assertEquals(i, queue.dequeue());
        }
        assertTrue(queue.isEmpty());
    }
}