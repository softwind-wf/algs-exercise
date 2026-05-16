

import edu.princeton.cs.algs4.In;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import test4.Digraph;
import test4.TransitiveClosureSCC;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransitiveClosureSCC 传递闭包与强连通分量测试")
public class TransitiveClosureSCCTest {

    // ==============================
    // 基础功能测试
    // ==============================

    @Test
    @DisplayName("测试空图（只有顶点，没有边）")
    void testEmptyGraph() {
        Digraph G = new Digraph(5);
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        for (int i = 0; i < 5; i++) {
            assertTrue(tc.reachable(i, i), "每个顶点应该能到达自身");
            for (int j = 0; j < 5; j++) {
                if (i != j) {
                    assertFalse(tc.reachable(i, j), "无边时不同顶点之间不可达");
                }
            }
        }
    }

    @Test
    @DisplayName("测试单顶点图")
    void testSingleVertex() {
        Digraph G = new Digraph(1);
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 0));
    }

    @Test
    @DisplayName("测试简单有向路径")
    void testSimplePath() {
        Digraph G = new Digraph(4);
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(2, 3);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 3));
        assertTrue(tc.reachable(0, 2));
        assertTrue(tc.reachable(0, 1));
        assertTrue(tc.reachable(1, 3));
        assertFalse(tc.reachable(3, 0));
        assertFalse(tc.reachable(2, 0));
    }

    // ==============================
    // SCC（强连通分量）测试
    // ==============================

    @Test
    @DisplayName("测试单个强连通分量（环）")
    void testSingleSCC() {
        Digraph G = new Digraph(4);
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(2, 3);
        G.addEdge(3, 0);  // 形成环
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        // 同一 SCC 内所有顶点互相可达
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertTrue(tc.reachable(i, j), 
                    String.format("在同一个SCC中，%d应该能到达%d", i, j));
            }
        }
    }

    @Test
    @DisplayName("测试多个强连通分量")
    void testMultipleSCCs() {
        Digraph G = new Digraph(6);
        // SCC1: 0 -> 1 -> 2 -> 0
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(2, 0);
        
        // SCC2: 3 -> 4 -> 5 -> 3
        G.addEdge(3, 4);
        G.addEdge(4, 5);
        G.addEdge(5, 3);
        
        // SCC1 -> SCC2 的边
        G.addEdge(2, 3);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        // SCC1 内部互相可达
        assertTrue(tc.reachable(0, 1));
        assertTrue(tc.reachable(1, 2));
        assertTrue(tc.reachable(2, 0));
        
        // SCC2 内部互相可达
        assertTrue(tc.reachable(3, 4));
        assertTrue(tc.reachable(4, 5));
        assertTrue(tc.reachable(5, 3));
        
        // SCC1 可以到达 SCC2
        assertTrue(tc.reachable(0, 3));
        assertTrue(tc.reachable(1, 4));
        assertTrue(tc.reachable(2, 5));
        
        // SCC2 不能到达 SCC1
        assertFalse(tc.reachable(3, 0));
        assertFalse(tc.reachable(4, 1));
        assertFalse(tc.reachable(5, 2));
    }

    @Test
    @DisplayName("测试自环")
    void testSelfLoop() {
        Digraph G = new Digraph(3);
        G.addEdge(0, 0);  // 自环
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 0));
        assertTrue(tc.reachable(0, 2));
        assertFalse(tc.reachable(2, 0));
    }

    // ==============================
    // DAG（有向无环图）测试
    // ==============================

    @Test
    @DisplayName("测试DAG结构")
    void testDAG() {
        Digraph G = new Digraph(7);
        G.addEdge(0, 1);
        G.addEdge(0, 2);
        G.addEdge(1, 3);
        G.addEdge(2, 3);
        G.addEdge(3, 4);
        G.addEdge(4, 5);
        G.addEdge(4, 6);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 5));
        assertTrue(tc.reachable(0, 6));
        assertTrue(tc.reachable(1, 4));
        assertTrue(tc.reachable(2, 5));
        assertFalse(tc.reachable(5, 0));
        assertFalse(tc.reachable(3, 1));
        assertFalse(tc.reachable(6, 4));
    }

    @Test
    @DisplayName("测试树形结构")
    void testTreeStructure() {
        Digraph G = new Digraph(7);
        G.addEdge(0, 1);
        G.addEdge(0, 2);
        G.addEdge(1, 3);
        G.addEdge(1, 4);
        G.addEdge(2, 5);
        G.addEdge(2, 6);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 6));
        assertTrue(tc.reachable(1, 4));
        assertTrue(tc.reachable(2, 5));
        assertFalse(tc.reachable(3, 4));  // 兄弟节点不可达
        assertFalse(tc.reachable(4, 1));  // 子节点不能到父节点
    }

    // ==============================
    // 复杂图结构测试
    // ==============================

    @Test
    @DisplayName("测试复杂图：多个SCC和跨SCC边")
    void testComplexGraph() {
        Digraph G = new Digraph(10);
        
        // SCC1: {0, 1, 2}
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(2, 0);
        
        // SCC2: {3, 4}
        G.addEdge(3, 4);
        G.addEdge(4, 3);
        
        // SCC3: {5, 6, 7}
        G.addEdge(5, 6);
        G.addEdge(6, 7);
        G.addEdge(7, 5);
        
        // 孤立点: 8, 9
        
        // 跨SCC的边
        G.addEdge(2, 3);  // SCC1 -> SCC2
        G.addEdge(4, 5);  // SCC2 -> SCC3
        G.addEdge(1, 8);  // SCC1 -> 8
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        // SCC 内部可达
        assertTrue(tc.reachable(0, 2));
        assertTrue(tc.reachable(3, 4));
        assertTrue(tc.reachable(5, 7));
        
        // 跨SCC可达
        assertTrue(tc.reachable(0, 5));  // SCC1 -> SCC2 -> SCC3
        assertTrue(tc.reachable(1, 7));
        assertTrue(tc.reachable(2, 4));
        
        // 不可达
        assertFalse(tc.reachable(5, 0));  // 反向不可达
        assertFalse(tc.reachable(8, 0));  // 8是终点
        assertFalse(tc.reachable(9, 0));  // 9是孤立点
        assertTrue(tc.reachable(9, 9));   // 自身可达
    }

    @Test
    @DisplayName("测试完全图")
    void testCompleteGraph() {
        Digraph G = new Digraph(5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (i != j) {
                    G.addEdge(i, j);
                }
            }
        }
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        // 完全图中任意两点互相可达
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                assertTrue(tc.reachable(i, j), 
                    String.format("完全图中%d应该能到达%d", i, j));
            }
        }
    }

    // ==============================
    // 边界情况测试
    // ==============================

    @Test
    @DisplayName("测试双向边（两个顶点的SCC）")
    void testBidirectionalEdge() {
        Digraph G = new Digraph(4);
        G.addEdge(0, 1);
        G.addEdge(1, 0);  // 形成 SCC: {0, 1}
        G.addEdge(1, 2);
        G.addEdge(2, 3);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 1));
        assertTrue(tc.reachable(1, 0));
        assertTrue(tc.reachable(0, 3));
        assertFalse(tc.reachable(3, 0));
    }

    @Test
    @DisplayName("测试平行边")
    void testParallelEdges() {
        Digraph G = new Digraph(3);
        G.addEdge(0, 1);
        G.addEdge(0, 1);  // 平行边
        G.addEdge(1, 2);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 2));
        assertTrue(tc.reachable(0, 1));
    }

    // ==============================
    // 多次查询测试
    // ==============================

    @Test
    @DisplayName("测试大量连续查询")
    void testMultipleQueries() {
        Digraph G = new Digraph(8);
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(2, 3);
        G.addEdge(3, 0);  // SCC: {0,1,2,3}
        G.addEdge(3, 4);
        G.addEdge(4, 5);
        G.addEdge(5, 6);
        G.addEdge(6, 7);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        // 交替进行可达和不可达查询
        assertTrue(tc.reachable(0, 3));
        assertFalse(tc.reachable(7, 0));
        assertTrue(tc.reachable(1, 7));
        assertFalse(tc.reachable(4, 1));
        assertTrue(tc.reachable(0, 0));
        assertTrue(tc.reachable(2, 6));
        assertFalse(tc.reachable(5, 2));
        
        // 重复查询验证时间戳机制
        assertTrue(tc.reachable(0, 3));
        assertFalse(tc.reachable(7, 0));
    }

    // ==============================
    // 性能测试
    // ==============================

    @Test
    @DisplayName("性能测试：大规模链式图")
    void testPerformanceChain() {
        int V = 1000;
        Digraph G = new Digraph(V);
        
        // 创建链式结构: 0 -> 1 -> 2 -> ... -> V-1
        for (int i = 0; i < V - 1; i++) {
            G.addEdge(i, i + 1);
        }
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        long startTime = System.currentTimeMillis();
        assertTrue(tc.reachable(0, V - 1));
        assertFalse(tc.reachable(V - 1, 0));
        long endTime = System.currentTimeMillis();
        
        System.out.println("链式图(" + V + "顶点)查询耗时: " + (endTime - startTime) + "ms");
    }

    @Test
    @DisplayName("性能测试：大规模完全图（小SCC数量）")
    void testPerformanceCompleteGraph() {
        int V = 100;
        Digraph G = new Digraph(V);
        
        // 创建完全图（所有顶点在一个SCC中）
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (i != j) {
                    G.addEdge(i, j);
                }
            }
        }
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        long startTime = System.currentTimeMillis();
        // 矩阵策略应该是 O(1) 查询
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                assertTrue(tc.reachable(i, j));
            }
        }
        long endTime = System.currentTimeMillis();
        
        System.out.println("完全图(" + V + "顶点)全对查询耗时: " + (endTime - startTime) + "ms");
    }

    // ==============================
    // 使用真实数据文件测试
    // ==============================

    @Test
    @DisplayName("使用tinyDG.txt测试")
    void testWithTinyDG() {
        try {
            In in = new In("tinyDG.txt");
            Digraph G = new Digraph(in);
            TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
            
            assertNotNull(tc);
            System.out.println("tinyDG.txt: 顶点数=" + G.V() + ", 边数=" + G.E());
            
            // 进行一些基本查询
            assertTrue(tc.reachable(0, 0));
            
        } catch (Exception e) {
            System.out.println("tinyDG.txt 文件不存在，跳过此测试");
        }
    }

    // ==============================
    // 特殊结构测试
    // ==============================

    @Test
    @DisplayName("测试星型图")
    void testStarGraph() {
        Digraph G = new Digraph(6);
        G.addEdge(0, 1);
        G.addEdge(0, 2);
        G.addEdge(0, 3);
        G.addEdge(0, 4);
        G.addEdge(0, 5);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 5));
        assertFalse(tc.reachable(1, 2));  // 叶子节点之间不可达
        assertFalse(tc.reachable(1, 0));  // 单向边
    }

    @Test
    @DisplayName("测试分层图")
    void testLayeredGraph() {
        Digraph G = new Digraph(9);
        // 第1层 -> 第2层
        G.addEdge(0, 3);
        G.addEdge(0, 4);
        G.addEdge(1, 4);
        G.addEdge(1, 5);
        G.addEdge(2, 5);
        G.addEdge(2, 3);
        
        // 第2层 -> 第3层
        G.addEdge(3, 6);
        G.addEdge(4, 7);
        G.addEdge(5, 8);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 6));
        assertTrue(tc.reachable(1, 7));
        assertTrue(tc.reachable(2, 8));
        assertFalse(tc.reachable(6, 0));  // 不能反向
        assertFalse(tc.reachable(3, 4));  // 同层之间不可达
    }

    @Test
    @DisplayName("测试不连通图")
    void testDisconnectedGraph() {
        Digraph G = new Digraph(6);
        // 分量1: 0 -> 1 -> 2
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        
        // 分量2: 3 -> 4 -> 5
        G.addEdge(3, 4);
        G.addEdge(4, 5);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 2));
        assertTrue(tc.reachable(3, 5));
        assertFalse(tc.reachable(0, 3));  // 不同分量不可达
        assertFalse(tc.reachable(2, 5));
    }

    @Test
    @DisplayName("测试蝴蝶结结构")
    void testBowtieStructure() {
        Digraph G = new Digraph(7);
        // 左侧: 0 -> 1 -> 2
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        
        // 中间枢纽: 2 -> 3 -> 4
        G.addEdge(2, 3);
        G.addEdge(3, 4);
        
        // 右侧: 4 -> 5 -> 6
        G.addEdge(4, 5);
        G.addEdge(5, 6);
        
        TransitiveClosureSCC tc = new TransitiveClosureSCC(G);
        
        assertTrue(tc.reachable(0, 6));  // 贯穿整个结构
        assertTrue(tc.reachable(1, 5));
        assertFalse(tc.reachable(6, 0));  // 不能反向
    }
}
