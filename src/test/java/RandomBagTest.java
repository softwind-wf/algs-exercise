import exercise1.exercise1_3.RandomBag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class RandomBagTest {

    // 测试1：初始化空背包的基本属性
    @Test
    void testInitialEmptyBag() {
        RandomBag<String> bag = new RandomBag<>();
        assertTrue(bag.isEmpty());
        assertEquals(0, bag.size());
    }

    // 测试2：添加元素后，大小和非空状态正确
    @Test
    void testAddElements() {
        RandomBag<Integer> bag = new RandomBag<>();
        bag.add(1);
        bag.add(2);
        bag.add(3);

        assertFalse(bag.isEmpty());
        assertEquals(3, bag.size());
    }

    // 测试3：添加null元素抛出异常
    @Test
    void testAddNullThrowsException() {
        RandomBag<String> bag = new RandomBag<>();
        assertThrows(IllegalArgumentException.class, () -> bag.add(null));
    }

    // 测试4：迭代器能遍历所有元素，且数量正确
    @Test
    void testIteratorContainsAllElements() {
        RandomBag<String> bag = new RandomBag<>();
        Set<String> expected = new HashSet<>(Arrays.asList("A", "B", "C", "D"));
        bag.add("A");
        bag.add("B");
        bag.add("C");
        bag.add("D");

        Set<String> actual = new HashSet<>();
        for (String s : bag) {
            actual.add(s);
        }

        assertEquals(expected, actual);
        assertEquals(4, actual.size());
    }

    // 测试5：两次迭代顺序不同（验证随机性）
    @Test
    void testIterationOrderIsRandom() {
        RandomBag<Integer> bag = new RandomBag<>();
        for (int i = 0; i < 10; i++) {
            bag.add(i);
        }

        // 收集第一次迭代顺序
        List<Integer> firstOrder = new ArrayList<>();
        for (int num : bag) {
            firstOrder.add(num);
        }

        // 收集第二次迭代顺序
        List<Integer> secondOrder = new ArrayList<>();
        for (int num : bag) {
            secondOrder.add(num);
        }

        // 元素集合相同，但顺序大概率不同
        assertEquals(new HashSet<>(firstOrder), new HashSet<>(secondOrder));
        // 注意：理论上存在极小概率顺序相同，若测试失败可重试
        assertNotEquals(firstOrder, secondOrder);
    }

    // 测试6：空背包迭代器没有元素
    @Test
    void testEmptyBagIterator() {
        RandomBag<Integer> bag = new RandomBag<>();
        Iterator<Integer> iterator = bag.iterator();

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    // 测试7：迭代器不支持remove操作
    @Test
    void testIteratorRemoveThrowsException() {
        RandomBag<String> bag = new RandomBag<>();
        bag.add("test");
        Iterator<String> iterator = bag.iterator();

        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    // 测试8：多次迭代都能正确遍历所有元素
    @Test
    void testMultipleIterations() {
        RandomBag<Integer> bag = new RandomBag<>();
        int[] elements = {1, 2, 3, 4, 5};
        for (int e : elements) {
            bag.add(e);
        }

        for (int iter = 0; iter < 5; iter++) {
            Set<Integer> collected = new HashSet<>();
            for (int num : bag) {
                collected.add(num);
            }
            assertEquals(elements.length, collected.size());
            for (int e : elements) {
                assertTrue(collected.contains(e));
            }
        }
    }
}