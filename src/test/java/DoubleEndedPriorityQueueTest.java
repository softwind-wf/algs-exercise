import exercise2.exercise2_4.DoubleEndedPriorityQueue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DoubleEndedPriorityQueueTest {
    
    // 测试基本插入和查找
    @Test
    void testBasicInsertAndFind() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        
        assertTrue(pq.isEmpty());
        assertEquals(0, pq.size());
        
        pq.insert(5);
        pq.insert(3);
        pq.insert(7);
        pq.insert(2);
        
        assertFalse(pq.isEmpty());
        assertEquals(4, pq.size());
        assertEquals(2, pq.findMin());
        assertEquals(7, pq.findMax());
    }
    
    // 测试删除最小值
    @Test
    void testDeleteMin() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        pq.insert(5);
        pq.insert(3);
        pq.insert(7);
        pq.insert(2);
        
        assertEquals(2, pq.deleteMin());
        assertEquals(3, pq.findMin());
        assertEquals(7, pq.findMax());
        
        assertEquals(3, pq.deleteMin());
        assertEquals(5, pq.deleteMin());
        assertEquals(7, pq.deleteMin());
        assertNull(pq.deleteMin());
        assertTrue(pq.isEmpty());
    }
    
    // 测试删除最大值
    @Test
    void testDeleteMax() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        pq.insert(5);
        pq.insert(3);
        pq.insert(7);
        pq.insert(2);
        
        assertEquals(7, pq.deleteMax());
        assertEquals(5, pq.findMax());
        
        assertEquals(5, pq.deleteMax());
        assertEquals(3, pq.deleteMax());
        assertEquals(2, pq.deleteMax());
        assertNull(pq.deleteMax());
        assertTrue(pq.isEmpty());
    }
    
    // 测试混合删除操作
    @Test
    void testMixedDeleteOperations() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        pq.insert(1);
        pq.insert(2);
        pq.insert(3);
        pq.insert(4);
        pq.insert(5);
        
        // 交替删除最小和最大
        assertEquals(1, pq.deleteMin());
        assertEquals(5, pq.deleteMax());
        assertEquals(2, pq.deleteMin());
        assertEquals(4, pq.deleteMax());
        assertEquals(3, pq.deleteMin());
        
        assertTrue(pq.isEmpty());
    }
    
    // 测试重复元素
    @Test
    void testDuplicateElements() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        pq.insert(5);
        pq.insert(3);
        pq.insert(5);
        pq.insert(3);
        pq.insert(5);
        
        assertEquals(3, pq.findMin());
        assertEquals(5, pq.findMax());
        
        assertEquals(3, pq.deleteMin());
        assertEquals(3, pq.deleteMin());
        assertEquals(5, pq.deleteMin());
        assertEquals(5, pq.deleteMin());
        assertEquals(5, pq.deleteMin());
        
        assertTrue(pq.isEmpty());
    }
    
    // 测试单个元素
    @Test
    void testSingleElement() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        pq.insert(42);
        
        assertEquals(42, pq.findMin());
        assertEquals(42, pq.findMax());
        assertEquals(42, pq.deleteMin());
        assertTrue(pq.isEmpty());
    }
    
    // 测试空队列操作
    @Test
    void testEmptyQueue() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        
        assertTrue(pq.isEmpty());
        assertNull(pq.findMin());
        assertNull(pq.findMax());
        assertNull(pq.deleteMin());
        assertNull(pq.deleteMax());
    }
    
    // 测试惰性删除 - 关键测试！
    @Test
    void testLazyDeletion() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        pq.insert(1);
        pq.insert(2);
        pq.insert(3);
        pq.insert(4);
        pq.insert(5);
        
        // 删除最小值，这会在最大堆中留下"僵尸"元素
        pq.deleteMin(); // 删除 1
        
        // 此时应该能正常找到最大值
        assertEquals(5, pq.findMax());
        
        // 删除最大值
        pq.deleteMax(); // 删除 5
        
        // 继续验证
        assertEquals(4, pq.findMax());
        assertEquals(2, pq.findMin());
    }
    
    // 测试大量数据
    @Test
    void testLargeDataset() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(1000);
        
        // 插入 100 个元素
        for (int i = 0; i < 100; i++) {
            pq.insert((int)(Math.random() * 1000));
        }
        
        // 验证删除顺序
        Integer lastMin = Integer.MIN_VALUE;
        while (!pq.isEmpty()) {
            Integer min = pq.deleteMin();
            assertTrue(min >= lastMin, "删除的最小值应该递增");
            lastMin = min;
        }
    }
    
    // 测试交叉操作验证同步性
    @Test
    void testCrossValidation() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(20);
        
        // 插入一些元素
        Integer[] values = {10, 5, 15, 3, 7, 12, 20};
        for (Integer val : values) {
            pq.insert(val);
        }
        
        // 验证最小堆和最大堆的一致性
        while (!pq.isEmpty()) {
            Integer min = pq.findMin();
            Integer max = pq.findMax();
            
            // 最小值应该小于等于最大值
            assertTrue(min <= max, "最小值应该小于等于最大值");
            
            // 删除一个元素后继续验证
            if (min == max) {
                // 所有元素相同
                pq.deleteMin();
            } else {
                // 随机删除最小或最大
                if (Math.random() < 0.5) {
                    pq.deleteMin();
                } else {
                    pq.deleteMax();
                }
            }
        }
    }
    
    // 可视化测试
    @Test
    void testVisualize() {
        System.out.println("=== 双端优先队列可视化测试 ===");
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(20);
        
        Integer[] values = {8, 3, 10, 1, 6, 14, 4, 7, 13};
        System.out.print("插入序列：");
        for (Integer val : values) {
            System.out.print(val + " ");
            pq.insert(val);
        }
        System.out.println();
        
        System.out.println("\n当前状态:");
        System.out.println("最小值：" + pq.findMin());
        System.out.println("最大值：" + pq.findMax());
        System.out.println("大小：" + pq.size());
        
        System.out.print("\n按升序删除：");
        DoubleEndedPriorityQueue<Integer> pq2 = new DoubleEndedPriorityQueue<>(20);
        for (Integer val : values) {
            pq2.insert(val);
        }
        while (!pq2.isEmpty()) {
            System.out.print(pq2.deleteMin() + " ");
        }
        
        System.out.print("\n\n按降序删除：");
        pq = new DoubleEndedPriorityQueue<>(20);
        for (Integer val : values) {
            pq.insert(val);
        }
        while (!pq.isEmpty()) {
            System.out.print(pq.deleteMax() + " ");
        }
        System.out.println("\n");
    }
    
    // 压力测试：反复插入删除
    @Test
    void testStressTest() {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(1000);
        
        for (int round = 0; round < 10; round++) {
            // 插入 50 个随机数
            for (int i = 0; i < 50; i++) {
                pq.insert((int)(Math.random() * 100));
            }
            
            // 删除 25 次最小值和 25 次最大值
            for (int i = 0; i < 25; i++) {
                assertNotNull(pq.deleteMin());
            }
            for (int i = 0; i < 25; i++) {
                assertNotNull(pq.deleteMax());
            }
            
            assertTrue(pq.isEmpty(), "第 " + round + " 轮后队列应该为空");
        }
    }
}
