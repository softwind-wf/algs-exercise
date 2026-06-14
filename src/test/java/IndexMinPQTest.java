

import exercise2.exercise2_4.IndexMinPQ;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IndexMinPQTest {
    
    @Test
    void testBasicInsertAndMin() {
        IndexMinPQ<Integer> pq = new IndexMinPQ<>(10);
        
        // 插入几个元素
        pq.insert(0, 5);
        pq.insert(1, 3);
        pq.insert(2, 7);
        
        // 应该返回最小值 3
        assertEquals(3, pq.min());
        assertEquals(1, pq.minIndex()); // 索引 1 对应值 3
    }
    
    @Test
    void testMultipleElements() {
        IndexMinPQ<Integer> pq = new IndexMinPQ<>(10);
        
        pq.insert(0, 10);
        pq.insert(1, 5);
        pq.insert(2, 15);
        pq.insert(3, 3);
        pq.insert(4, 8);
        
        assertEquals(3, pq.min());
        assertEquals(3, pq.minIndex());
    }
    
    @Test
    void testDelMin() {
        IndexMinPQ<Integer> pq = new IndexMinPQ<>(10);
        
        pq.insert(0, 5);
        pq.insert(1, 3);
        pq.insert(2, 7);
        pq.insert(3, 1);
        
        // 删除最小值（索引 3，值为 1）
        int minIdx = pq.delMin();
        assertEquals(3, minIdx);
        assertFalse(pq.contains(3));
        
        // 现在最小值是 3（索引 1）
        assertEquals(3, pq.min());
        assertEquals(1, pq.minIndex());
    }
    
    @Test
    void testChangeKey() {
        IndexMinPQ<Integer> pq = new IndexMinPQ<>(10);
        
        pq.insert(0, 5);
        pq.insert(1, 3);
        pq.insert(2, 7);
        
        // 将索引 1 的值从 3 改为 1（更小）
        pq.change(1, 1);
        
        assertEquals(1, pq.min());
        assertEquals(1, pq.minIndex());
        
        // 将索引 0 的值从 5 改为 0（最小）
        pq.change(0, 0);
        
        assertEquals(0, pq.min());
        assertEquals(0, pq.minIndex());
    }
    
    @Test
    void testDelete() {
        IndexMinPQ<Integer> pq = new IndexMinPQ<>(10);
        
        pq.insert(0, 5);
        pq.insert(1, 3);
        pq.insert(2, 7);
        pq.insert(3, 1);
        
        // 删除索引 3（最小值）
        pq.delete(3);
        assertFalse(pq.contains(3));
        
        // 现在最小值是 3（索引 1）
        assertEquals(3, pq.min());
        assertEquals(1, pq.minIndex());
    }
    
    @Test
    void testEmptyQueue() {
        IndexMinPQ<Integer> pq = new IndexMinPQ<>(10);
        
        assertTrue(pq.isEmpty());
        assertEquals(0, pq.size());
        
        assertThrows(IllegalStateException.class, () -> {
            pq.min();
        });
        
        assertThrows(IllegalStateException.class, () -> {
            pq.delMin();
        });
    }
    
    @Test
    void testLargeDataset() {
        IndexMinPQ<Integer> pq = new IndexMinPQ<>(100);
        
        // 插入 100 个元素
        for (int i = 0; i < 100; i++) {
            pq.insert(i, (int)(Math.random() * 1000));
        }
        
        // 连续删除最小值，应该是升序
        Integer lastMin = Integer.MIN_VALUE;
        while (!pq.isEmpty()) {
            Integer currentMin = pq.min();
            assertTrue(currentMin >= lastMin, "最小值应该递增");
            lastMin = currentMin;
            pq.delMin();
        }
    }
    
    @Test
    void testVisualize() {
        System.out.println("=== 索引最小优先队列可视化 ===");
        IndexMinPQ<Integer> pq = new IndexMinPQ<>(10);
        
        pq.insert(5, 50);   // 索引 5，值 50
        pq.insert(2, 20);   // 索引 2，值 20
        pq.insert(8, 80);   // 索引 8，值 80
        pq.insert(1, 10);   // 索引 1，值 10 ← 最小
        pq.insert(9, 90);   // 索引 9，值 90
        
        System.out.println("当前最小值: " + pq.min()); // 应该输出 10
        System.out.println("最小值索引: " + pq.minIndex()); // 应该输出 1
        
        System.out.print("按升序删除: ");
        while (!pq.isEmpty()) {
            System.out.print(pq.min() + " ");
            pq.delMin();
        }
        System.out.println("\n应该是: 10 20 50 80 90 ✓");
    }
}
