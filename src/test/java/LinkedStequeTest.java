import exercise1.exercise1_3.LinkedSteque;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LinkedStequeTest {

    private LinkedSteque<String> steque;

    // 每个测试方法执行前，都初始化一个新的 steque
    @BeforeEach
    void setUp() {
        steque = new LinkedSteque<>();
    }

    @Test
    void testInitialState() {
        assertTrue(steque.isEmpty());
        assertEquals(0, steque.size());
    }

    @Test
    void testPushAndPop() {
        steque.push("A");
        steque.push("B");
        steque.push("C");

        assertEquals(3, steque.size());
        assertEquals("C", steque.pop());
        assertEquals("B", steque.pop());
        assertEquals("A", steque.pop());
        assertTrue(steque.isEmpty());
    }

    @Test
    void testEnqueueAndPop() {
        steque.enqueue("X");
        steque.enqueue("Y");
        steque.enqueue("Z");

        assertEquals(3, steque.size());
        assertEquals("X", steque.pop());
        assertEquals("Y", steque.pop());
        assertEquals("Z", steque.pop());
        assertTrue(steque.isEmpty());
    }

    @Test
    void testMixedOperations() {
        steque.push("A");
        steque.enqueue("B");
        steque.push("C");
        steque.enqueue("D");

        // 顺序：C, A, B, D
        assertEquals("C", steque.pop());
        assertEquals("A", steque.pop());
        assertEquals("B", steque.pop());
        assertEquals("D", steque.pop());
        assertTrue(steque.isEmpty());
    }

    @Test
    void testPopOnEmptyStequeThrowsException() {
        assertThrows(IllegalStateException.class, () -> steque.pop());
    }
}