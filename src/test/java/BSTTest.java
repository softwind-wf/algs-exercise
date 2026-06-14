
import test3.BST;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BSTTest {
    
    // 测试空树的基本操作
    @Test
    void testEmptyTree() {
        BST<String, Integer> bst = new BST<>();
        assertEquals(0, bst.size());
        assertNull(bst.get("key"));
    }
    
    // 测试插入单个键值对
    @Test
    void testPutAndGetSingleElement() {
        BST<String, Integer> bst = new BST<>();
        bst.put("hello", 1);
        assertEquals(1, bst.size());
        assertEquals(Integer.valueOf(1), bst.get("hello"));
    }
    
    // 测试插入多个键值对
    @Test
    void testPutAndGetMultipleElements() {
        BST<String, Integer> bst = new BST<>();
        bst.put("c", 3);
        bst.put("a", 1);
        bst.put("b", 2);
        bst.put("d", 4);
        
        assertEquals(4, bst.size());
        assertEquals(Integer.valueOf(1), bst.get("a"));
        assertEquals(Integer.valueOf(2), bst.get("b"));
        assertEquals(Integer.valueOf(3), bst.get("c"));
        assertEquals(Integer.valueOf(4), bst.get("d"));
    }
    
    // 测试更新已存在的键
    @Test
    void testUpdateExistingKey() {
        BST<String, Integer> bst = new BST<>();
        bst.put("key", 1);
        assertEquals(Integer.valueOf(1), bst.get("key"));
        
        bst.put("key", 2);
        assertEquals(1, bst.size());
        assertEquals(Integer.valueOf(2), bst.get("key"));
    }
    
    // 测试查找不存在的键
    @Test
    void testGetNonExistentKey() {
        BST<String, Integer> bst = new BST<>();
        bst.put("a", 1);
        assertNull(bst.get("b"));
    }
    
    // 测试最小键
    @Test
    void testMin() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        bst.put(1, "one");
        bst.put(4, "four");
        
        assertEquals(Integer.valueOf(1), bst.min());
    }
    
    // 测试只有一个结点时的最小键
    @Test
    void testMinSingleNode() {
        BST<String, Integer> bst = new BST<>();
        bst.put("only", 1);
        assertEquals("only", bst.min());
    }
    
    // 测试最大键
    @Test
    void testMax() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        bst.put(1, "one");
        bst.put(4, "four");
        
        assertEquals(Integer.valueOf(7), bst.max());
    }
    
    // 测试只有一个结点时的最大键
    @Test
    void testMaxSingleNode() {
        BST<String, Integer> bst = new BST<>();
        bst.put("only", 1);
        assertEquals("only", bst.max());
    }
    
    // 测试向下取整（floor）
    @Test
    void testFloor() {
        BST<Integer, String> bst = new BST<>();
        for (int i = 0; i <= 10; i += 2) {
            bst.put(i, String.valueOf(i));
        }
        
        assertEquals(Integer.valueOf(6), bst.floor(7));
        assertEquals(Integer.valueOf(6), bst.floor(6));
        assertEquals(Integer.valueOf(4), bst.floor(5));
        assertNull(bst.floor(-1));
    }
    
    // 测试向上取整（ceiling）
    @Test
    void testCeiling() {
        BST<Integer, String> bst = new BST<>();
        for (int i = 0; i <= 10; i += 2) {
            bst.put(i, String.valueOf(i));
        }
        
        assertEquals(Integer.valueOf(8), bst.ceiling(7));
        assertEquals(Integer.valueOf(8), bst.ceiling(8));
        assertEquals(Integer.valueOf(10), bst.ceiling(9));
        assertNull(bst.ceiling(11));
    }
    
    // 测试选择操作（select）
    @Test
    void testSelect() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        bst.put(1, "one");
        bst.put(4, "four");
        
        assertEquals(Integer.valueOf(1), bst.select(0));
        assertEquals(Integer.valueOf(3), bst.select(1));
        assertEquals(Integer.valueOf(4), bst.select(2));
        assertEquals(Integer.valueOf(5), bst.select(3));
        assertEquals(Integer.valueOf(7), bst.select(4));
    }
    
    // 测试排名操作（rank）
    @Test
    void testRank() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        bst.put(1, "one");
        bst.put(4, "four");
        
        assertEquals(0, bst.rank(1));
        assertEquals(1, bst.rank(3));
        assertEquals(2, bst.rank(4));
        assertEquals(3, bst.rank(5));
        assertEquals(4, bst.rank(7));
    }
    
    // 测试 select 和 rank 的互逆性
    @Test
    void testSelectRankInverse() {
        BST<Integer, String> bst = new BST<>();
        int[] keys = {5, 3, 7, 1, 4, 6, 8};
        for (int key : keys) {
            bst.put(key, String.valueOf(key));
        }
        
        for (int i = 0; i < bst.size(); i++) {
            assertEquals(i, bst.rank(bst.select(i)));
        }
    }
    
    // 测试删除最小键
    @Test
    void testDeleteMin() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        bst.put(1, "one");
        bst.put(4, "four");
        
        bst.deleteMin();
        assertNull(bst.get(1));
        assertEquals(4, bst.size());
        assertEquals(Integer.valueOf(3), bst.min());
    }
    
    // 测试删除最小键直到树为空
    @Test
    void testDeleteMinUntilEmpty() {
        BST<Integer, String> bst = new BST<>();
        bst.put(3, "three");
        bst.put(1, "one");
        bst.put(2, "two");
        
        bst.deleteMin();
        bst.deleteMin();
        bst.deleteMin();
        
        assertTrue(bst.size() == 0);
    }
    
    // 测试删除最大键
    @Test
    void testDeleteMax() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        bst.put(1, "one");
        bst.put(4, "four");
        
        bst.deleteMax();
        assertNull(bst.get(7));
        assertEquals(4, bst.size());
        assertEquals(Integer.valueOf(5), bst.max());
    }
    
    // 测试删除最大键直到树为空
    @Test
    void testDeleteMaxUntilEmpty() {
        BST<Integer, String> bst = new BST<>();
        bst.put(3, "three");
        bst.put(1, "one");
        bst.put(2, "two");
        
        bst.deleteMax();
        bst.deleteMax();
        bst.deleteMax();
        
        assertTrue(bst.size() == 0);
    }
    
    // 测试删除指定键
    @Test
    void testDelete() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        bst.put(1, "one");
        bst.put(4, "four");
        
        bst.delete(3);
        assertNull(bst.get(3));
        assertEquals(4, bst.size());
        assertNotNull(bst.get(1));
        assertNotNull(bst.get(4));
        assertNotNull(bst.get(5));
        assertNotNull(bst.get(7));
    }
    
    // 测试删除根结点
    @Test
    void testDeleteRoot() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        
        bst.delete(5);
        assertNull(bst.get(5));
        assertEquals(2, bst.size());
        assertNotNull(bst.get(3));
        assertNotNull(bst.get(7));
    }
    
    // 测试删除不存在的键
    @Test
    void testDeleteNonExistentKey() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        
        bst.delete(10);
        assertEquals(2, bst.size());
    }
    
    // 测试删除后树的性质保持
    @Test
    void testDeleteMaintainsBSTProperty() {
        BST<Integer, String> bst = new BST<>();
        int[] keys = {5, 3, 7, 1, 4, 6, 8};
        for (int key : keys) {
            bst.put(key, String.valueOf(key));
        }
        
        bst.delete(5);
        
        // 验证剩余元素仍然有序
        Integer prev = null;
        for (Integer key : bst.keys()) {
            if (prev != null) {
                assertTrue(prev < key);
            }
            prev = key;
        }
    }
    
    // 测试范围查找
    @Test
    void testKeysInRange() {
        BST<Integer, String> bst = new BST<>();
        int[] keys = {5, 3, 7, 1, 4, 6, 8, 2};
        for (int key : keys) {
            bst.put(key, String.valueOf(key));
        }
        
        Iterable<Integer> rangeKeys = bst.keys(3, 6);
        int count = 0;
        int prev = -1;
        for (Integer key : rangeKeys) {
            assertTrue(key >= 3 && key <= 6);
            assertTrue(key > prev);
            prev = key;
            count++;
        }
        assertEquals(4, count); // 3, 4, 5, 6
    }
    
    // 测试获取所有键
    @Test
    void testAllKeys() {
        BST<Integer, String> bst = new BST<>();
        bst.put(5, "five");
        bst.put(3, "three");
        bst.put(7, "seven");
        bst.put(1, "one");
        
        Iterable<Integer> allKeys = bst.keys();
        int count = 0;
        int prev = -1;
        for (Integer key : allKeys) {
            assertTrue(key > prev);
            prev = key;
            count++;
        }
        assertEquals(4, count);
    }
    
    // 测试范围为空的情况
    @Test
    void testEmptyRange() {
        BST<Integer, String> bst = new BST<>();
        for (int i = 1; i <= 10; i++) {
            bst.put(i, String.valueOf(i));
        }
        
        Iterable<Integer> rangeKeys = bst.keys(15, 20);
        int count = 0;
        for (Integer key : rangeKeys) {
            count++;
        }
        assertEquals(0, count);
    }
    
    // 测试大规模数据插入和查找
    @Test
    void testLargeDataSet() {
        BST<Integer, String> bst = new BST<>();
        int n = 1000;
        
        for (int i = 0; i < n; i++) {
            bst.put(i, String.valueOf(i));
        }
        
        assertEquals(n, bst.size());
        assertEquals(Integer.valueOf(0), bst.min());
        assertEquals(Integer.valueOf(n - 1), bst.max());
        
        for (int i = 0; i < n; i++) {
            assertEquals(String.valueOf(i), bst.get(i));
        }
    }
    
    // 测试字符串键
    @Test
    void testStringKeys() {
        BST<String, Integer> bst = new BST<>();
        bst.put("banana", 2);
        bst.put("apple", 1);
        bst.put("cherry", 3);
        bst.put("date", 4);
        
        assertEquals("apple", bst.min());
        assertEquals("date", bst.max());
        assertEquals(Integer.valueOf(2), bst.get("banana"));
        
        Iterable<String> keys = bst.keys();
        String prev = "";
        for (String key : keys) {
            assertTrue(key.compareTo(prev) > 0);
            prev = key;
        }
    }
    
    // 测试按升序插入
    @Test
    void testInsertInAscendingOrder() {
        BST<Integer, String> bst = new BST<>();
        for (int i = 1; i <= 10; i++) {
            bst.put(i, String.valueOf(i));
        }
        
        assertEquals(10, bst.size());
        assertEquals(Integer.valueOf(1), bst.min());
        assertEquals(Integer.valueOf(10), bst.max());
    }
    
    // 测试按降序插入
    @Test
    void testInsertInDescendingOrder() {
        BST<Integer, String> bst = new BST<>();
        for (int i = 10; i >= 1; i--) {
            bst.put(i, String.valueOf(i));
        }
        
        assertEquals(10, bst.size());
        assertEquals(Integer.valueOf(1), bst.min());
        assertEquals(Integer.valueOf(10), bst.max());
    }
    
    // 可视化测试
    @Test
    void testVisualize() {
        System.out.println("=== BST 可视化测试 ===");
        BST<Integer, String> bst = new BST<>();
        
        int[] values = {5, 3, 7, 1, 4, 6, 8};
        System.out.print("插入序列: ");
        for (int val : values) {
            System.out.print(val + " ");
            bst.put(val, String.valueOf(val));
        }
        System.out.println();
        
        System.out.print("中序遍历: ");
        for (Integer key : bst.keys()) {
            System.out.print(key + " ");
        }
        System.out.println("\n应该是升序排列 ✓");
        
        System.out.println("最小值: " + bst.min());
        System.out.println("最大值: " + bst.max());
        System.out.println("大小: " + bst.size());
    }
}
