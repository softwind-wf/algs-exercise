import exercise1.exercise1_3.ResizingArrayDeque;
import org.junit.jupiter.api.Test;
import java.util.Iterator;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

public class ResizingArrayDequeTest {

    // 测试1：初始化空队列的基本属性
    @Test
    void testInitialEmptyDeque() {
        ResizingArrayDeque<String> deque = new ResizingArrayDeque<>();
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    // 测试2：向右端添加元素（pushRight）+ 从右端删除（popRight）
    @Test
    void testPushRightAndPopRight() {
        ResizingArrayDeque<Integer> deque = new ResizingArrayDeque<>();
        
        // 添加元素
        deque.pushRight(1);
        deque.pushRight(2);
        deque.pushRight(3);
        
        // 验证大小
        assertFalse(deque.isEmpty());
        assertEquals(3, deque.size());
        
        // 从右端删除，验证顺序（后进先出）
        assertEquals(3, deque.popRight());
        assertEquals(2, deque.popRight());
        assertEquals(1, deque.popRight());
        
        // 删除后为空
        assertTrue(deque.isEmpty());
    }

    // 测试3：向左端添加元素（pushLeft）+ 从左端删除（popLeft）
    @Test
    void testPushLeftAndPopLeft() {
        ResizingArrayDeque<String> deque = new ResizingArrayDeque<>();
        
        // 向左添加：a → b → c（队列顺序：c, b, a）
        deque.pushLeft("a");
        deque.pushLeft("b");
        deque.pushLeft("c");
        
        assertEquals(3, deque.size());
        
        // 从左删除，顺序：c → b → a
        assertEquals("c", deque.popLeft());
        assertEquals("b", deque.popLeft());
        assertEquals("a", deque.popLeft());
        
        assertTrue(deque.isEmpty());
    }

    // 测试4：自动扩容（初始容量2，添加3个元素触发扩容）
    @Test
    void testResizeWhenFull() {
        ResizingArrayDeque<Integer> deque = new ResizingArrayDeque<>();
        
        // 初始容量2，添加2个元素刚好满
        deque.pushRight(1);
        deque.pushRight(2);
        assertEquals(2, deque.size());
        
        // 添加第3个元素，触发扩容到4
        deque.pushRight(3);
        assertEquals(3, deque.size());
        
        // 验证元素正确性
        assertEquals(1, deque.popLeft());
        assertEquals(2, deque.popLeft());
        assertEquals(3, deque.popLeft());
    }

    // 测试5：自动缩容（元素数量到容量1/4时缩容）
    @Test
    void testResizeWhenShrink() {
        ResizingArrayDeque<String> deque = new ResizingArrayDeque<>();
        
        // 添加4个元素（容量扩容到4）
        deque.pushRight("a");
        deque.pushRight("b");
        deque.pushRight("c");
        deque.pushRight("d");
        assertEquals(4, deque.size());
        
        // 删除3个元素，剩余1个（容量4的1/4=1，触发缩容到2）
        deque.popLeft();
        deque.popLeft();
        deque.popLeft();
        assertEquals(1, deque.size());
        
        // 验证剩余元素正确
        assertEquals("d", deque.popLeft());
        assertTrue(deque.isEmpty());
    }

    // 测试6：异常场景 - 添加null元素
    @Test
    void testPushNullItemThrowsException() {
        ResizingArrayDeque<String> deque = new ResizingArrayDeque<>();
        
        // pushLeft添加null，抛出IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> deque.pushLeft(null));
        
        // pushRight添加null，抛出IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> deque.pushRight(null));
    }

    // 测试7：异常场景 - 空队列删除元素
    @Test
    void testPopFromEmptyDequeThrowsException() {
        ResizingArrayDeque<Integer> deque = new ResizingArrayDeque<>();
        
        // 空队列popLeft，抛出NoSuchElementException
        assertThrows(NoSuchElementException.class, deque::popLeft);
        
        // 空队列popRight，抛出NoSuchElementException
        assertThrows(NoSuchElementException.class, deque::popRight);
    }

    // 测试8：迭代器功能
    @Test
    void testIterator() {
        ResizingArrayDeque<String> deque = new ResizingArrayDeque<>();
        deque.pushRight("first");
        deque.pushRight("second");
        deque.pushLeft("zero");
        
        Iterator<String> iterator = deque.iterator();
        
        // 验证迭代顺序（zero → first → second）
        assertTrue(iterator.hasNext());
        assertEquals("zero", iterator.next());
        
        assertTrue(iterator.hasNext());
        assertEquals("first", iterator.next());
        
        assertTrue(iterator.hasNext());
        assertEquals("second", iterator.next());
        
        // 无更多元素
        assertFalse(iterator.hasNext());
        
        // 调用next()抛出异常
        assertThrows(NoSuchElementException.class, iterator::next);
        
        // 调用remove()抛出不支持异常
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }
}