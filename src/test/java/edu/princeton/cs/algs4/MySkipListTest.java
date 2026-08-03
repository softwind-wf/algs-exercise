package edu.princeton.cs.algs4;

import com.ds.linked.MySkipList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MySkipListTest {

    // ==================== 空表基础测试 ====================

    @Test
    void testEmptySkipList() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        assertEquals(0, sl.size());
        assertTrue(sl.isEmpty());
        assertEquals(0, sl.level());
        assertNull(sl.get(1));
        assertFalse(sl.containsKey(1));
        assertNull(sl.firstKey());
        assertNull(sl.lastKey());
        assertNull(sl.floorKey(1));
        assertNull(sl.ceilingKey(1));
        assertNull(sl.remove(1));
    }

    // ==================== 插入 & 查找测试 ====================

    @Test
    void testPutAndGetSingleElement() {
        MySkipList<String, Integer> sl = new MySkipList<>();
        sl.put("hello", 100);
        assertEquals(1, sl.size());
        assertEquals(Integer.valueOf(100), sl.get("hello"));
        assertTrue(sl.containsKey("hello"));
    }

    @Test
    void testPutAndGetMultipleElements() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(5, "five");
        sl.put(3, "three");
        sl.put(7, "seven");
        sl.put(1, "one");
        sl.put(9, "nine");

        assertEquals(5, sl.size());
        assertEquals("one", sl.get(1));
        assertEquals("three", sl.get(3));
        assertEquals("five", sl.get(5));
        assertEquals("seven", sl.get(7));
        assertEquals("nine", sl.get(9));
    }

    @Test
    void testUpdateExistingKey() {
        MySkipList<String, Integer> sl = new MySkipList<>();
        sl.put("key", 10);
        assertEquals(Integer.valueOf(10), sl.get("key"));

        sl.put("key", 20);
        assertEquals(1, sl.size());
        assertEquals(Integer.valueOf(20), sl.get("key"));
    }

    @Test
    void testUpdateExistingKeyMultipleTimes() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "A");
        sl.put(2, "B");
        sl.put(3, "C");

        sl.put(2, "BB");
        sl.put(2, "BBB");

        assertEquals(3, sl.size());
        assertEquals("BBB", sl.get(2));
        assertEquals("A", sl.get(1));
        assertEquals("C", sl.get(3));
    }

    @Test
    void testGetNonExistentKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "one");
        sl.put(3, "three");
        sl.put(5, "five");

        assertNull(sl.get(0));
        assertNull(sl.get(2));
        assertNull(sl.get(6));
        assertNull(sl.get(100));
    }

    @Test
    void testContainsKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(10, "ten");
        sl.put(20, "twenty");

        assertTrue(sl.containsKey(10));
        assertTrue(sl.containsKey(20));
        assertFalse(sl.containsKey(5));
        assertFalse(sl.containsKey(15));
        assertFalse(sl.containsKey(30));
    }

    // ==================== 删除测试 ====================

    @Test
    void testRemoveExistingKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "one");
        sl.put(2, "two");
        sl.put(3, "three");

        assertEquals("two", sl.remove(2));
        assertEquals(2, sl.size());
        assertNull(sl.get(2));
        assertEquals("one", sl.get(1));
        assertEquals("three", sl.get(3));
    }

    @Test
    void testRemoveNonExistentKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "one");
        sl.put(3, "three");

        assertNull(sl.remove(2));
        assertNull(sl.remove(100));
        assertEquals(2, sl.size());
    }

    @Test
    void testRemoveAllElements() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        int[] keys = {5, 3, 7, 1, 9, 2, 8, 4, 6};
        for (int k : keys) {
            sl.put(k, "V" + k);
        }

        assertEquals(9, sl.size());

        for (int k : keys) {
            assertEquals("V" + k, sl.remove(k));
        }

        assertEquals(0, sl.size());
        assertTrue(sl.isEmpty());
        assertNull(sl.firstKey());
        assertNull(sl.lastKey());
    }

    @Test
    void testRemoveFirstAndLast() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(10, "ten");
        sl.put(20, "twenty");
        sl.put(30, "thirty");

        assertEquals("ten", sl.remove(10));
        assertEquals(Integer.valueOf(20), sl.firstKey());
        assertEquals(Integer.valueOf(30), sl.lastKey());

        assertEquals("thirty", sl.remove(30));
        assertEquals(Integer.valueOf(20), sl.firstKey());
        assertEquals(Integer.valueOf(20), sl.lastKey());

        assertEquals("twenty", sl.remove(20));
        assertTrue(sl.isEmpty());
    }

    // ==================== firstKey / lastKey 测试 ====================

    @Test
    void testFirstKeyAndLastKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(50, "fifty");
        sl.put(10, "ten");
        sl.put(30, "thirty");
        sl.put(70, "seventy");
        sl.put(20, "twenty");

        assertEquals(Integer.valueOf(10), sl.firstKey());
        assertEquals(Integer.valueOf(70), sl.lastKey());
    }

    @Test
    void testFirstKeyAndLastKeySingleElement() {
        MySkipList<String, Integer> sl = new MySkipList<>();
        sl.put("only", 1);

        assertEquals("only", sl.firstKey());
        assertEquals("only", sl.lastKey());
    }

    // ==================== floorKey / ceilingKey 测试 ====================

    @Test
    void testFloorKeyExactMatch() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        for (int i = 10; i <= 100; i += 10) {
            sl.put(i, "V" + i);
        }

        assertEquals(Integer.valueOf(30), sl.floorKey(30));
        assertEquals(Integer.valueOf(50), sl.floorKey(50));
        assertEquals(Integer.valueOf(100), sl.floorKey(100));
    }

    @Test
    void testFloorKeyNoExactMatch() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        for (int i = 10; i <= 100; i += 10) {
            sl.put(i, "V" + i);
        }

        assertEquals(Integer.valueOf(30), sl.floorKey(35));
        assertEquals(Integer.valueOf(60), sl.floorKey(69));
        assertEquals(Integer.valueOf(90), sl.floorKey(99));
        assertEquals(Integer.valueOf(10), sl.floorKey(15));
    }

    @Test
    void testFloorKeyBelowMinimum() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(10, "ten");
        sl.put(20, "twenty");

        assertNull(sl.floorKey(5));
    }

    @Test
    void testFloorKeyEmptyList() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        assertNull(sl.floorKey(100));
    }

    @Test
    void testCeilingKeyExactMatch() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        for (int i = 10; i <= 100; i += 10) {
            sl.put(i, "V" + i);
        }

        assertEquals(Integer.valueOf(30), sl.ceilingKey(30));
        assertEquals(Integer.valueOf(50), sl.ceilingKey(50));
        assertEquals(Integer.valueOf(10), sl.ceilingKey(10));
    }

    @Test
    void testCeilingKeyNoExactMatch() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        for (int i = 10; i <= 100; i += 10) {
            sl.put(i, "V" + i);
        }

        assertEquals(Integer.valueOf(40), sl.ceilingKey(35));
        assertEquals(Integer.valueOf(70), sl.ceilingKey(69));
        assertEquals(Integer.valueOf(100), sl.ceilingKey(95));
        assertEquals(Integer.valueOf(10), sl.ceilingKey(5));
    }

    @Test
    void testCeilingKeyAboveMaximum() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(10, "ten");
        sl.put(20, "twenty");

        assertNull(sl.ceilingKey(50));
    }

    @Test
    void testCeilingKeyEmptyList() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        assertNull(sl.ceilingKey(100));
    }

    // ==================== 升序插入测试 ====================

    @Test
    void testInsertInAscendingOrder() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        for (int i = 1; i <= 20; i++) {
            sl.put(i, "V" + i);
        }

        assertEquals(20, sl.size());
        assertEquals(Integer.valueOf(1), sl.firstKey());
        assertEquals(Integer.valueOf(20), sl.lastKey());
        assertEquals("V10", sl.get(10));
    }

    // ==================== 降序插入测试 ====================

    @Test
    void testInsertInDescendingOrder() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        for (int i = 20; i >= 1; i--) {
            sl.put(i, "V" + i);
        }

        assertEquals(20, sl.size());
        assertEquals(Integer.valueOf(1), sl.firstKey());
        assertEquals(Integer.valueOf(20), sl.lastKey());
        assertEquals("V10", sl.get(10));
    }

    // ==================== 字符串键测试 ====================

    @Test
    void testStringKeys() {
        MySkipList<String, Integer> sl = new MySkipList<>();
        sl.put("banana", 2);
        sl.put("apple", 1);
        sl.put("cherry", 3);
        sl.put("date", 4);

        assertEquals(4, sl.size());
        assertEquals("apple", sl.firstKey());
        assertEquals("date", sl.lastKey());
        assertEquals(Integer.valueOf(2), sl.get("banana"));

        assertEquals("banana", sl.floorKey("blueberry"));
        assertEquals("cherry", sl.ceilingKey("blueberry"));
    }

    // ==================== 边界与异常测试 ====================

    @Test
    void testNullKeyThrowsExceptionOnGet() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "one");
        assertThrows(IllegalArgumentException.class, () -> sl.get(null));
    }

    @Test
    void testNullKeyThrowsExceptionOnPut() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        assertThrows(IllegalArgumentException.class, () -> sl.put(null, "value"));
    }

    @Test
    void testNullKeyThrowsExceptionOnRemove() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "one");
        assertThrows(IllegalArgumentException.class, () -> sl.remove(null));
    }

    @Test
    void testFloorKeyWithNullKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "one");
        assertNull(sl.floorKey(null));
    }

    @Test
    void testCeilingKeyWithNullKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "one");
        assertNull(sl.ceilingKey(null));
    }

    // ==================== 大容量测试 ====================

    @Test
    void testLargeDataSet() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        int n = 5000;

        for (int i = 0; i < n; i++) {
            sl.put(i, "val" + i);
        }

        assertEquals(n, sl.size());
        assertEquals(Integer.valueOf(0), sl.firstKey());
        assertEquals(Integer.valueOf(n - 1), sl.lastKey());

        for (int i = 0; i < n; i++) {
            assertEquals("val" + i, sl.get(i));
        }
    }

    @Test
    void testLargeDataSetRandomOrder() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        int n = 5000;

        // 伪随机顺序插入
        for (int i = 0; i < n; i++) {
            int key = (i * 7919) % n;
            sl.put(key, "val" + key);
        }

        assertEquals(n, sl.size());
        assertEquals(Integer.valueOf(0), sl.firstKey());
        assertEquals(Integer.valueOf(n - 1), sl.lastKey());
    }

    @Test
    void testLargeDataSetRemoveHalf() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        int n = 2000;

        for (int i = 0; i < n; i++) {
            sl.put(i, "val" + i);
        }

        // 删除所有偶数
        for (int i = 0; i < n; i += 2) {
            assertEquals("val" + i, sl.remove(i));
        }

        assertEquals(n / 2, sl.size());
        assertEquals(Integer.valueOf(1), sl.firstKey());
        assertEquals(Integer.valueOf(n - 1), sl.lastKey());

        // 验证剩余的奇数都存在，偶数都不存在
        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                assertNull(sl.get(i));
            } else {
                assertEquals("val" + i, sl.get(i));
            }
        }
    }

    // ==================== 层级测试 ====================

    @Test
    void testLevelAfterInserts() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        assertEquals(0, sl.level());

        for (int i = 0; i < 1000; i++) {
            sl.put(i, "val" + i);
        }

        int lvl = sl.level();
        assertTrue(lvl > 0, "层级应大于 0");
        assertTrue(lvl <= 16, "层级不应超过 MAX_LEVEL(16)");
    }

    // ==================== 综合场景测试 ====================

    @Test
    void testInsertRemoveReinsertSameKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(5, "five");
        sl.put(3, "three");
        sl.put(7, "seven");

        assertEquals("five", sl.remove(5));
        assertNull(sl.get(5));
        assertEquals(2, sl.size());

        sl.put(5, "FIVE");
        assertEquals("FIVE", sl.get(5));
        assertEquals(3, sl.size());
    }

    @Test
    void testRepeatedOperations() {
        MySkipList<Integer, String> sl = new MySkipList<>();

        // 插入一批
        for (int i = 0; i < 50; i++) {
            sl.put(i, "val" + i);
        }
        assertEquals(50, sl.size());

        // 删除一部分
        for (int i = 0; i < 25; i++) {
            sl.remove(i);
        }
        assertEquals(25, sl.size());
        assertEquals(Integer.valueOf(25), sl.firstKey());

        // 再插入
        for (int i = 50; i < 75; i++) {
            sl.put(i, "val" + i);
        }
        assertEquals(50, sl.size());
        assertEquals(Integer.valueOf(25), sl.firstKey());
        assertEquals(Integer.valueOf(74), sl.lastKey());

        // 再删除全部
        for (int i = 25; i < 75; i++) {
            assertEquals("val" + i, sl.remove(i));
        }
        assertTrue(sl.isEmpty());
    }

    @Test
    void testInsertNullValue() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, null);

        assertEquals(1, sl.size());
        assertNull(sl.get(1));
        assertTrue(sl.containsKey(1));
    }

    @Test
    void testUpdateToNullValue() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(1, "one");
        assertEquals("one", sl.get(1));

        sl.put(1, null);
        assertNull(sl.get(1));
        assertTrue(sl.containsKey(1));
        assertEquals(1, sl.size());
    }

    // ==================== firstEntry 测试 ====================

    @Test
    void testFirstEntry() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(30, "thirty");
        sl.put(10, "ten");
        sl.put(20, "twenty");

        assertEquals(Integer.valueOf(10), sl.firstKey());
        assertEquals("ten", sl.get(sl.firstKey()));
    }

    @Test
    void testFirstEntryEmpty() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        assertNull(sl.firstEntry());
    }

    // ==================== 复杂 floor/ceiling 边界测试 ====================

    @Test
    void testFloorAndCeilingAfterRemove() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        for (int i = 2; i <= 20; i += 2) {
            sl.put(i, "V" + i);
        }

        assertEquals(Integer.valueOf(12), sl.floorKey(12));
        assertEquals(Integer.valueOf(12), sl.ceilingKey(12));

        sl.put(12, "twelve");

        assertEquals(Integer.valueOf(12), sl.floorKey(12));
        assertEquals(Integer.valueOf(12), sl.ceilingKey(12));

        sl.remove(12);

        assertEquals(Integer.valueOf(10), sl.floorKey(12));
        assertEquals(Integer.valueOf(14), sl.ceilingKey(12));
    }

    @Test
    void testFloorKeyAtMaxKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(10, "ten");
        sl.put(20, "twenty");
        sl.put(30, "thirty");

        assertEquals(Integer.valueOf(30), sl.floorKey(30));
        assertEquals(Integer.valueOf(30), sl.floorKey(100));
    }

    @Test
    void testCeilingKeyAtMinKey() {
        MySkipList<Integer, String> sl = new MySkipList<>();
        sl.put(10, "ten");
        sl.put(20, "twenty");
        sl.put(30, "thirty");

        assertEquals(Integer.valueOf(10), sl.ceilingKey(10));
        assertEquals(Integer.valueOf(10), sl.ceilingKey(0));
    }
}
