

import exercise2.exercise2_4.MaxHeap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MaxHeapTest {
    
    // 测试空堆判空和大小
    @Test
    void testEmptyHeap() {
        MaxHeap heap = new MaxHeap(10);
        assertTrue(heap.isEmpty(), "新创建的堆应该为空");
        assertEquals(0, heap.size());
    }
    
    // 测试插入单个元素
    @Test
    void testInsertSingleElement() {
        MaxHeap heap = new MaxHeap(10);
        heap.insert(5);
        assertFalse(heap.isEmpty());
        assertEquals(1, heap.size());
        assertEquals(5, heap.delMax());
        assertTrue(heap.isEmpty());
    }
    
    // 测试插入多个元素并删除最大值
    @Test
    void testInsertAndDelMax() {
        MaxHeap heap = new MaxHeap(10);
        heap.insert(3);
        heap.insert(1);
        heap.insert(5);
        heap.insert(2);
        heap.insert(4);
        
        assertEquals(5, heap.delMax());
        assertEquals(4, heap.delMax());
        assertEquals(3, heap.delMax());
        assertEquals(2, heap.delMax());
        assertEquals(1, heap.delMax());
        assertTrue(heap.isEmpty());
    }
    
    // 测试降序插入
    @Test
    void testInsertDescendingOrder() {
        MaxHeap heap = new MaxHeap(10);
        heap.insert(9);
        heap.insert(7);
        heap.insert(5);
        heap.insert(3);
        heap.insert(1);
        
        assertEquals(9, heap.delMax());
        assertEquals(7, heap.delMax());
        assertEquals(5, heap.delMax());
        assertEquals(3, heap.delMax());
        assertEquals(1, heap.delMax());
    }
    
    // 测试升序插入
    @Test
    void testInsertAscendingOrder() {
        MaxHeap heap = new MaxHeap(10);
        heap.insert(1);
        heap.insert(3);
        heap.insert(5);
        heap.insert(7);
        heap.insert(9);
        
        assertEquals(9, heap.delMax());
        assertEquals(7, heap.delMax());
        assertEquals(5, heap.delMax());
        assertEquals(3, heap.delMax());
        assertEquals(1, heap.delMax());
    }
    
    // 测试重复元素
    @Test
    void testDuplicateElements() {
        MaxHeap heap = new MaxHeap(10);
        heap.insert(5);
        heap.insert(3);
        heap.insert(5);
        heap.insert(3);
        heap.insert(5);
        
        assertEquals(5, heap.delMax());
        assertEquals(5, heap.delMax());
        assertEquals(5, heap.delMax());
        assertEquals(3, heap.delMax());
        assertEquals(3, heap.delMax());
    }
    
    // 测试包含负数
    @Test
    void testWithNegativeNumbers() {
        MaxHeap heap = new MaxHeap(10);
        heap.insert(-3);
        heap.insert(-1);
        heap.insert(-5);
        heap.insert(0);
        heap.insert(2);
        
        assertEquals(2, heap.delMax());
        assertEquals(0, heap.delMax());
        assertEquals(-1, heap.delMax());
        assertEquals(-3, heap.delMax());
        assertEquals(-5, heap.delMax());
    }
    
    // 测试从空堆删除
    @Test
    void testDelMaxFromEmptyHeap() {
        MaxHeap heap = new MaxHeap(10);
        assertThrows(NullPointerException.class, () -> {
            heap.delMax();
        });
    }
    
    // 测试大规模数据
    @Test
    void testLargeHeap() {
        MaxHeap heap = new MaxHeap(1000);
        
        // 插入 1000 个随机数
        for (int i = 0; i < 1000; i++) {
            heap.insert((int)(Math.random() * 10000));
        }
        
        // 验证每次删除的都是当前最大值
        Integer lastMax = Integer.MAX_VALUE;
        while (!heap.isEmpty()) {
            Comparable max = heap.delMax();
            assertTrue((Integer) max <= lastMax, 
                "每次删除的应该是当前最大元素");
            lastMax = (Integer) max;
        }
    }
    
    // 测试堆的性质：父节点总是大于等于子节点
    @Test
    void testHeapProperty() {
        MaxHeap heap = new MaxHeap(20);
        Integer[] values = {8, 3, 7, 1, 9, 4, 6, 2, 5, 10};
        
        for (Integer val : values) {
            heap.insert(val);
        }
        
        // 注意：由于是 Object[]，需要通过反射或其他方式访问内部数组
        // 这里通过插入删除顺序间接验证堆性质
        Integer lastMax = Integer.MAX_VALUE;
        while (!heap.isEmpty()) {
            Comparable max = heap.delMax();
            assertTrue((Integer) max <= lastMax);
            lastMax = (Integer) max;
        }
    }
    
    // 可视化测试
    @Test
    void testVisualize() {
        System.out.println("=== 最大堆可视化测试 ===");
        MaxHeap heap = new MaxHeap(15);
        
        Integer[] values = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5};
        System.out.print("插入序列: ");
        for (Integer val : values) {
            System.out.print(val + " ");
            heap.insert(val);
        }
        System.out.println();
        
        System.out.print("删除顺序: ");
        while (!heap.isEmpty()) {
            System.out.print(heap.delMax() + " ");
        }
        System.out.println("\n应该是降序排列 ✓");
    }
    
    // 测试字符串比较（实现 Comparable 接口）
    @Test
    void testWithString() {
        MaxHeap heap = new MaxHeap(10);
        heap.insert("apple");
        heap.insert("zebra");
        heap.insert("banana");
        heap.insert("cat");
        
        assertEquals("zebra", heap.delMax());
        assertEquals("cat", heap.delMax());
        assertEquals("banana", heap.delMax());
        assertEquals("apple", heap.delMax());
    }
}
