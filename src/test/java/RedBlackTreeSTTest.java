import test3.RedBlackTreeST;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

public class RedBlackTreeSTTest {
    
    // ==============================
    // 基础操作测试
    // ==============================
    
    @Test
    void testEmptyTree() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        assertEquals(0, st.size());
        assertTrue(st.isEmpty());
        assertNull(st.get("key"));
    }
    
    @Test
    void testPutAndGetSingleElement() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("hello", 1);
        assertEquals(1, st.size());
        assertFalse(st.isEmpty());
        assertEquals(Integer.valueOf(1), st.get("hello"));
    }
    
    @Test
    void testPutAndGetMultipleElements() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("S", 0);
        st.put("E", 1);
        st.put("A", 2);
        st.put("R", 3);
        st.put("C", 4);
        st.put("H", 5);
        
        assertEquals(6, st.size());
        assertEquals(Integer.valueOf(0), st.get("S"));
        assertEquals(Integer.valueOf(1), st.get("E"));
        assertEquals(Integer.valueOf(2), st.get("A"));
        assertEquals(Integer.valueOf(3), st.get("R"));
        assertEquals(Integer.valueOf(4), st.get("C"));
        assertEquals(Integer.valueOf(5), st.get("H"));
    }
    
    @Test
    void testUpdateExistingKey() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("key", 1);
        assertEquals(Integer.valueOf(1), st.get("key"));
        
        st.put("key", 2);
        assertEquals(1, st.size());
        assertEquals(Integer.valueOf(2), st.get("key"));
    }
    
    @Test
    void testGetNonExistentKey() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("a", 1);
        assertNull(st.get("b"));
    }
    
    @Test
    void testContainsKey() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("a", 1);
        assertTrue(st.containsKey("a"));
        assertFalse(st.containsKey("b"));
    }
    
    // ==============================
    // 最小/最大键测试
    // ==============================
    
    @Test
    void testMinAndMax() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(5, "five");
        st.put(3, "three");
        st.put(7, "seven");
        st.put(1, "one");
        st.put(4, "four");
        
        assertEquals(Integer.valueOf(1), st.min());
        assertEquals(Integer.valueOf(7), st.max());
    }
    
    @Test
    void testMinAndMaxSingleNode() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("only", 1);
        assertEquals("only", st.min());
        assertEquals("only", st.max());
    }
    
    @Test
    void testMinAndMaxEmpty() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        assertNull(st.min());
        assertNull(st.max());
    }
    
    // ==============================
    // Floor 和 Ceiling 测试
    // ==============================
    
    @Test
    void testFloor() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        for (int i = 0; i <= 10; i += 2) {
            st.put(i, String.valueOf(i));
        }
        
        assertEquals(Integer.valueOf(6), st.floor(7));
        assertEquals(Integer.valueOf(6), st.floor(6));
        assertEquals(Integer.valueOf(4), st.floor(5));
        assertNull(st.floor(-1));
    }
    
    @Test
    void testCeiling() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        for (int i = 0; i <= 10; i += 2) {
            st.put(i, String.valueOf(i));
        }
        
        assertEquals(Integer.valueOf(8), st.ceiling(7));
        assertEquals(Integer.valueOf(8), st.ceiling(8));
        assertEquals(Integer.valueOf(10), st.ceiling(9));
        assertNull(st.ceiling(11));
    }
    
    @Test
    void testFloorAndCeilingSameKey() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(5, "five");
        
        assertEquals(Integer.valueOf(5), st.floor(5));
        assertEquals(Integer.valueOf(5), st.ceiling(5));
    }
    
    // ==============================
    // Rank 和 Select 测试
    // ==============================
    
    @Test
    void testRank() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(5, "five");
        st.put(3, "three");
        st.put(7, "seven");
        st.put(1, "one");
        st.put(4, "four");
        
        assertEquals(0, st.rank(1));
        assertEquals(1, st.rank(3));
        assertEquals(2, st.rank(4));
        assertEquals(3, st.rank(5));
        assertEquals(4, st.rank(7));
    }
    
    @Test
    void testSelect() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(5, "five");
        st.put(3, "three");
        st.put(7, "seven");
        st.put(1, "one");
        st.put(4, "four");
        
        assertEquals(Integer.valueOf(1), st.select(0));
        assertEquals(Integer.valueOf(3), st.select(1));
        assertEquals(Integer.valueOf(4), st.select(2));
        assertEquals(Integer.valueOf(5), st.select(3));
        assertEquals(Integer.valueOf(7), st.select(4));
    }
    
    @Test
    void testSelectRankInverse() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        int[] keys = {5, 3, 7, 1, 4, 6, 8};
        for (int key : keys) {
            st.put(key, String.valueOf(key));
        }
        
        for (int i = 0; i < st.size(); i++) {
            assertEquals(i, st.rank(st.select(i)));
        }
    }
    
    @Test
    void testSelectOutOfRange() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(5, "five");
        assertNull(st.select(-1));
        assertNull(st.select(1));
    }
    
    // ==============================
    // 删除操作测试
    // ==============================
    
    @Test
    void testRemove() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(5, "five");
        st.put(3, "three");
        st.put(7, "seven");
        st.put(1, "one");
        st.put(4, "four");
        
        st.remove(3);
        assertEquals(4, st.size());
        assertNull(st.get(3));
        assertEquals(Integer.valueOf(1), st.min());
        assertEquals(Integer.valueOf(7), st.max());
    }
    
    @Test
    void testRemoveNonExistentKey() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(5, "five");
        st.remove(999);
        assertEquals(1, st.size());
    }
    
    @Test
    void testRemoveRoot() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(5, "five");
        st.put(3, "three");
        st.put(7, "seven");
        
        st.remove(5);
        assertEquals(2, st.size());
        assertNull(st.get(5));
        assertNotNull(st.get(3));
        assertNotNull(st.get(7));
    }
    
    @Test
    void testRemoveAllElements() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(3, "three");
        st.put(1, "one");
        st.put(2, "two");
        
        st.remove(3);
        st.remove(1);
        st.remove(2);
        
        assertEquals(0, st.size());
        assertTrue(st.isEmpty());
        assertNull(st.min());
        assertNull(st.max());
    }
    
    // ==============================
    // Clear 测试
    // ==============================
    
    @Test
    void testClear() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(1, "one");
        st.put(2, "two");
        st.put(3, "three");
        
        st.clear();
        assertEquals(0, st.size());
        assertTrue(st.isEmpty());
    }
    
    // ==============================
    // KeySet 测试
    // ==============================
    
    @Test
    void testKeySet() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("S", 0);
        st.put("E", 1);
        st.put("A", 2);
        st.put("R", 3);
        
        List<String> keys = st.keySet();
        assertEquals(4, keys.size());
        assertEquals(Arrays.asList("A", "E", "R", "S"), keys);
    }
    
    @Test
    void testKeySetEmpty() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        List<String> keys = st.keySet();
        assertEquals(0, keys.size());
    }
    
    // ==============================
    // 泛型测试 - Integer 类型
    // ==============================
    
    @Test
    void testIntegerKeys() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        st.put(10, "A");
        st.put(5, "B");
        st.put(15, "C");
        
        assertEquals("A", st.get(10));
        assertEquals("B", st.get(5));
        assertEquals("C", st.get(15));
        assertEquals(Integer.valueOf(5), st.min());
        assertEquals(Integer.valueOf(15), st.max());
    }
    
    // ==============================
    // 泛型测试 - String 类型
    // ==============================
    
    @Test
    void testStringKeys() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("Apple", 100);
        st.put("Banana", 200);
        st.put("Cat", 300);
        
        assertEquals(Integer.valueOf(100), st.get("Apple"));
        assertEquals(Integer.valueOf(200), st.get("Banana"));
        assertEquals(Integer.valueOf(300), st.get("Cat"));
        assertEquals("Apple", st.min());
        assertEquals("Cat", st.max());
    }
    
    // ==============================
    // 大规模数据测试
    // ==============================
    
    @Test
    void testLargeDataSet() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        int n = 1000;
        
        for (int i = 0; i < n; i++) {
            st.put(i, "value" + i);
        }
        
        assertEquals(n, st.size());
        
        for (int i = 0; i < n; i++) {
            assertEquals("value" + i, st.get(i));
        }
        
        assertEquals(Integer.valueOf(0), st.min());
        assertEquals(Integer.valueOf(n - 1), st.max());
        assertEquals(n / 2, st.rank(n / 2));
    }
    
    @Test
    void testInsertAndDeleteRandomOrder() {
        RedBlackTreeST<Integer, String> st = new RedBlackTreeST<>();
        int[] keys = {8, 4, 12, 2, 6, 10, 14, 1, 3, 5, 7, 9, 11, 13, 15};
        
        for (int key : keys) {
            st.put(key, String.valueOf(key));
        }
        
        assertEquals(15, st.size());
        
        st.remove(8);
        st.remove(4);
        st.remove(12);
        
        assertEquals(12, st.size());
        assertNull(st.get(8));
        assertNull(st.get(4));
        assertNull(st.get(12));
    }
    
    // ==============================
    // 边界情况测试
    // ==============================
    
    @Test
    void testNullKey() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        assertThrows(IllegalArgumentException.class, () -> {
            st.put(null, 1);
        });
    }
    
    @Test
    void testNullValue() {
        RedBlackTreeST<String, Integer> st = new RedBlackTreeST<>();
        st.put("key", null);
        assertNull(st.get("key"));
    }
}
