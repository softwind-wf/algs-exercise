

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import test3.LinearProbingHashST;
import test3.LinearProbingHashST1;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 两个版本线性探测散列表的性能对比测试
 */
public class LinearProbingHashSTPerformanceTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;
    private static final int LARGE_TEST_SIZE = 100000;

    // ==================== 功能一致性测试 ====================

    @Test
    @DisplayName("验证两个版本的功能一致性 - 基本操作")
    void testFunctionalConsistency() {
        LinearProbingHashST<String, Integer> st1 = new LinearProbingHashST<>();
        LinearProbingHashST1<String, Integer> st2 = new LinearProbingHashST1<>();

        String[] keys = {"E", "A", "R", "C", "H", "X", "M", "P", "L"};
        
        for (int i = 0; i < keys.length; i++) {
            st1.put(keys[i], i);
            st2.put(keys[i], i);
        }

        assertEquals(st1.size(), st2.size());
        
        for (String key : keys) {
            assertEquals(st1.get(key), st2.get(key), "键: " + key);
            assertEquals(st1.contains(key), st2.contains(key), "键: " + key);
        }

        st1.delete("E");
        st2.delete("E");
        
        assertEquals(st1.size(), st2.size());
        assertNull(st1.get("E"));
        assertNull(st2.get("E"));
    }

    @Test
    @DisplayName("验证两个版本的功能一致性 - 大量数据")
    void testFunctionalConsistencyLargeData() {
        LinearProbingHashST<Integer, Integer> st1 = new LinearProbingHashST<>();
        LinearProbingHashST1<Integer, Integer> st2 = new LinearProbingHashST1<>();

        int n = 10000;
        for (int i = 0; i < n; i++) {
            st1.put(i, i * 2);
            st2.put(i, i * 2);
        }

        assertEquals(st1.size(), st2.size());
        
        for (int i = 0; i < n; i++) {
            assertEquals(st1.get(i), st2.get(i));
        }
    }

    // ==================== 性能测试方法 ====================

    /**
     * 测试插入性能
     */
    private long benchmarkInsert(int size, boolean useOptimized) {
        long startTime, endTime;
        
        if (useOptimized) {
            LinearProbingHashST<Integer, Integer> st = new LinearProbingHashST<>();
            startTime = System.nanoTime();
            for (int i = 0; i < size; i++) {
                st.put(i, i);
            }
            endTime = System.nanoTime();
        } else {
            LinearProbingHashST1<Integer, Integer> st = new LinearProbingHashST1<>();
            startTime = System.nanoTime();
            for (int i = 0; i < size; i++) {
                st.put(i, i);
            }
            endTime = System.nanoTime();
        }
        
        return endTime - startTime;
    }

    /**
     * 测试查询性能
     */
    private long benchmarkGet(int size, boolean useOptimized) {
        LinearProbingHashST<Integer, Integer> st1;
        LinearProbingHashST1<Integer, Integer> st2;
        
        if (useOptimized) {
            st1 = new LinearProbingHashST<>();
            for (int i = 0; i < size; i++) {
                st1.put(i, i);
            }
            
            long startTime = System.nanoTime();
            for (int i = 0; i < size; i++) {
                st1.get(i);
            }
            return System.nanoTime() - startTime;
        } else {
            st2 = new LinearProbingHashST1<>();
            for (int i = 0; i < size; i++) {
                st2.put(i, i);
            }
            
            long startTime = System.nanoTime();
            for (int i = 0; i < size; i++) {
                st2.get(i);
            }
            return System.nanoTime() - startTime;
        }
    }

    /**
     * 测试删除性能
     */
    private long benchmarkDelete(int size, boolean useOptimized) {
        if (useOptimized) {
            LinearProbingHashST<Integer, Integer> st = new LinearProbingHashST<>();
            for (int i = 0; i < size; i++) {
                st.put(i, i);
            }
            
            long startTime = System.nanoTime();
            for (int i = 0; i < size / 2; i++) {
                st.delete(i);
            }
            return System.nanoTime() - startTime;
        } else {
            LinearProbingHashST1<Integer, Integer> st = new LinearProbingHashST1<>();
            for (int i = 0; i < size; i++) {
                st.put(i, i);
            }
            
            long startTime = System.nanoTime();
            for (int i = 0; i < size / 2; i++) {
                st.delete(i);
            }
            return System.nanoTime() - startTime;
        }
    }

    /**
     * 测试混合操作性能（插入、查询、删除交替）
     */
    private long benchmarkMixedOperations(int size, boolean useOptimized) {
        if (useOptimized) {
            LinearProbingHashST<Integer, Integer> st = new LinearProbingHashST<>();
            
            long startTime = System.nanoTime();
            for (int i = 0; i < size; i++) {
                st.put(i, i);
            }
            for (int i = 0; i < size; i++) {
                st.get(i);
            }
            for (int i = 0; i < size / 2; i++) {
                st.delete(i);
            }
            for (int i = size; i < size * 3 / 2; i++) {
                st.put(i, i);
            }
            return System.nanoTime() - startTime;
        } else {
            LinearProbingHashST1<Integer, Integer> st = new LinearProbingHashST1<>();
            
            long startTime = System.nanoTime();
            for (int i = 0; i < size; i++) {
                st.put(i, i);
            }
            for (int i = 0; i < size; i++) {
                st.get(i);
            }
            for (int i = 0; i < size / 2; i++) {
                st.delete(i);
            }
            for (int i = size; i < size * 3 / 2; i++) {
                st.put(i, i);
            }
            return System.nanoTime() - startTime;
        }
    }

    // ==================== 性能测试用例 ====================

    @Test
    @DisplayName("性能对比：小规模数据插入 (1000条)")
    void testInsertPerformanceSmall() {
        int size = 1000;
        
        long timeOptimized = benchmarkInsert(size, true);
        long timeBasic = benchmarkInsert(size, false);
        
        System.out.println("\n========== 插入性能测试 (" + size + " 条数据) ==========");
        System.out.printf("优化版 (LinearProbingHashST):  %.4f ms%n", timeOptimized / 1_000_000.0);
        System.out.printf("基础版 (LinearProbingHashST1): %.4f ms%n", timeBasic / 1_000_000.0);
        System.out.printf("性能提升: %.2f%%%n", ((timeBasic - timeOptimized) * 100.0 / timeBasic));
        
        assertTrue(timeOptimized > 0 && timeBasic > 0);
    }

    @Test
    @DisplayName("性能对比：中等规模数据插入 (10000条)")
    void testInsertPerformanceMedium() {
        int size = 10000;
        
        long timeOptimized = benchmarkInsert(size, true);
        long timeBasic = benchmarkInsert(size, false);
        
        System.out.println("\n========== 插入性能测试 (" + size + " 条数据) ==========");
        System.out.printf("优化版 (LinearProbingHashST):  %.4f ms%n", timeOptimized / 1_000_000.0);
        System.out.printf("基础版 (LinearProbingHashST1): %.4f ms%n", timeBasic / 1_000_000.0);
        System.out.printf("性能提升: %.2f%%%n", ((timeBasic - timeOptimized) * 100.0 / timeBasic));
    }

    @Test
    @DisplayName("性能对比：大规模数据插入 (100000条)")
    void testInsertPerformanceLarge() {
        int size = 100000;
        
        long timeOptimized = benchmarkInsert(size, true);
        long timeBasic = benchmarkInsert(size, false);
        
        System.out.println("\n========== 插入性能测试 (" + size + " 条数据) ==========");
        System.out.printf("优化版 (LinearProbingHashST):  %.4f ms%n", timeOptimized / 1_000_000.0);
        System.out.printf("基础版 (LinearProbingHashST1): %.4f ms%n", timeBasic / 1_000_000.0);
        System.out.printf("性能提升: %.2f%%%n", ((timeBasic - timeOptimized) * 100.0 / timeBasic));
    }

    @Test
    @DisplayName("性能对比：查询操作 (10000条数据)")
    void testGetPerformance() {
        int size = 10000;
        
        long timeOptimized = benchmarkGet(size, true);
        long timeBasic = benchmarkGet(size, false);
        
        System.out.println("\n========== 查询性能测试 (" + size + " 条数据) ==========");
        System.out.printf("优化版 (LinearProbingHashST):  %.4f ms%n", timeOptimized / 1_000_000.0);
        System.out.printf("基础版 (LinearProbingHashST1): %.4f ms%n", timeBasic / 1_000_000.0);
        System.out.printf("性能提升: %.2f%%%n", ((timeBasic - timeOptimized) * 100.0 / timeBasic));
    }

    @Test
    @DisplayName("性能对比：删除操作 (10000条数据)")
    void testDeletePerformance() {
        int size = 10000;
        
        long timeOptimized = benchmarkDelete(size, true);
        long timeBasic = benchmarkDelete(size, false);
        
        System.out.println("\n========== 删除性能测试 (删除 " + size/2 + " 条数据) ==========");
        System.out.printf("优化版 (LinearProbingHashST):  %.4f ms%n", timeOptimized / 1_000_000.0);
        System.out.printf("基础版 (LinearProbingHashST1): %.4f ms%n", timeBasic / 1_000_000.0);
        System.out.printf("性能提升: %.2f%%%n", ((timeBasic - timeOptimized) * 100.0 / timeBasic));
    }

    @Test
    @DisplayName("性能对比：混合操作 (10000条数据)")
    void testMixedOperationsPerformance() {
        int size = 10000;
        
        long timeOptimized = benchmarkMixedOperations(size, true);
        long timeBasic = benchmarkMixedOperations(size, false);
        
        System.out.println("\n========== 混合操作性能测试 ==========");
        System.out.printf("优化版 (LinearProbingHashST):  %.4f ms%n", timeOptimized / 1_000_000.0);
        System.out.printf("基础版 (LinearProbingHashST1): %.4f ms%n", timeBasic / 1_000_000.0);
        System.out.printf("性能提升: %.2f%%%n", ((timeBasic - timeOptimized) * 100.0 / timeBasic));
    }

    @Test
    @DisplayName("综合性能对比报告")
    void comprehensivePerformanceReport() {
        System.out.println("\n" + "===============================================");
        System.out.println("           线性探测散列表性能对比报告");
        System.out.println("=============================================================");
        
        int[] sizes = {1000, 10000, 50000};
        
        for (int size : sizes) {
            System.out.println("\n--- 数据规模: " + size + " ---");
            
            long insertOpt = benchmarkInsert(size, true);
            long insertBasic = benchmarkInsert(size, false);
            System.out.printf("  插入: 优化版=%.2fms, 基础版=%.2fms, 提升=%.2f%%%n",
                insertOpt / 1_000_000.0, insertBasic / 1_000_000.0,
                (insertBasic - insertOpt) * 100.0 / insertBasic);
            
            long getOpt = benchmarkGet(size, true);
            long getBasic = benchmarkGet(size, false);
            System.out.printf("  查询: 优化版=%.2fms, 基础版=%.2fms, 提升=%.2f%%%n",
                getOpt / 1_000_000.0, getBasic / 1_000_000.0,
                (getBasic - getOpt) * 100.0 / getBasic);
            
            long deleteOpt = benchmarkDelete(size, true);
            long deleteBasic = benchmarkDelete(size, false);
            System.out.printf("  删除: 优化版=%.2fms, 基础版=%.2fms, 提升=%.2f%%%n",
                deleteOpt / 1_000_000.0, deleteBasic / 1_000_000.0,
                (deleteBasic - deleteOpt) * 100.0 / deleteBasic);
        }
        
        System.out.println("\n" + "=====================================================");
    }

    @Test
    @DisplayName("压力测试：极端场景")
    void stressTest() {
        System.out.println("\n========== 压力测试 ==========");
        
        int size = 1000000;
        
        long startTime = System.nanoTime();
        LinearProbingHashST<Integer, Integer> st1 = new LinearProbingHashST<>();
        for (int i = 0; i < size; i++) {
            st1.put(i, i);
        }
        long insertTime1 = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        LinearProbingHashST1<Integer, Integer> st2 = new LinearProbingHashST1<>();
        for (int i = 0; i < size; i++) {
            st2.put(i, i);
        }
        long insertTime2 = System.nanoTime() - startTime;
        
        System.out.printf("大规模插入 (%d 条):%n", size);
        System.out.printf("  优化版: %.2f ms%n", insertTime1 / 1_000_000.0);
        System.out.printf("  基础版: %.2f ms%n", insertTime2 / 1_000_000.0);
        
        assertEquals(st1.size(), st2.size());
    }
}
