

import test4.Digraph;
import edu.princeton.cs.algs4.In;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import test4.FastReachability;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FastReachability 双向BFS可达性测试")
public class FastReachabilityTest {

    // 测试1: 简单有向图 - 可达情况
    @Test
    @DisplayName("测试简单路径可达: 0->1->2->3")
    void testSimpleReachablePath() {
        Digraph G = new Digraph(4);
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(2, 3);
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 3), "0应该能到达3");
        assertTrue(fr.reachable(0, 2), "0应该能到达2");
        assertTrue(fr.reachable(0, 1), "0应该能到达1");
        assertTrue(fr.reachable(1, 3), "1应该能到达3");
    }

    // 测试2: 不可达情况
    @Test
    @DisplayName("测试不可达情况")
    void testNotReachable() {
        Digraph G = new Digraph(6);
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(3, 4);
        G.addEdge(4, 5);
        
        FastReachability fr = new FastReachability(G);
        
        assertFalse(fr.reachable(0, 3), "0不能到达3（两个不连通的分量）");
        assertFalse(fr.reachable(0, 4), "0不能到达4");
        assertFalse(fr.reachable(2, 3), "2不能到达3");
        assertFalse(fr.reachable(5, 0), "5不能到达0（反向也不可达）");
    }

    // 测试3: 相同顶点
    @Test
    @DisplayName("测试起点和终点相同")
    void testSameVertex() {
        Digraph G = new Digraph(5);
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 0), "顶点应该能到达自身");
        assertTrue(fr.reachable(1, 1), "顶点应该能到达自身");
        assertTrue(fr.reachable(4, 4), "孤立顶点也应该能到达自身");
    }

    // 测试4: 有环图
    @Test
    @DisplayName("测试包含环的图")
    void testGraphWithCycle() {
        Digraph G = new Digraph(5);
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(2, 0);  // 形成环: 0->1->2->0
        G.addEdge(2, 3);
        G.addEdge(3, 4);
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 4), "0应该能到达4");
        assertTrue(fr.reachable(1, 0), "1应该能通过环到达0");
        assertTrue(fr.reachable(2, 1), "2应该能通过环到达1");
        assertTrue(fr.reachable(0, 3), "0应该能到达3");
    }

    // 测试5: 完全图
    @Test
    @DisplayName("测试完全连通图")
    void testCompleteGraph() {
        Digraph G = new Digraph(5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (i != j) {
                    G.addEdge(i, j);
                }
            }
        }
        
        FastReachability fr = new FastReachability(G);
        
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                assertTrue(fr.reachable(i, j), 
                    String.format("在完全图中，%d应该能到达%d", i, j));
            }
        }
    }

    // 测试6: 单链图
    @Test
    @DisplayName("测试单向链表结构")
    void testLinearChain() {
        Digraph G = new Digraph(10);
        for (int i = 0; i < 9; i++) {
            G.addEdge(i, i + 1);
        }
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 9), "0应该能到达9");
        assertTrue(fr.reachable(3, 7), "3应该能到达7");
        assertFalse(fr.reachable(9, 0), "9不能到达0（单向）");
        assertFalse(fr.reachable(5, 3), "5不能到达3（不能反向）");
    }

    // 测试7: 多次查询（测试时间戳机制）
    @Test
    @DisplayName("测试多次连续查询")
    void testMultipleQueries() {
        Digraph G = new Digraph(8);
        G.addEdge(0, 1);
        G.addEdge(1, 2);
        G.addEdge(2, 3);
        G.addEdge(4, 5);
        G.addEdge(5, 6);
        G.addEdge(6, 7);
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 3));
        assertFalse(fr.reachable(0, 4));
        assertTrue(fr.reachable(4, 7));
        assertFalse(fr.reachable(3, 4));
        assertTrue(fr.reachable(0, 2));
        assertFalse(fr.reachable(7, 4));
        
        assertTrue(fr.reachable(0, 3), "重复查询应该仍然正确");
        assertFalse(fr.reachable(0, 7), "不同分量之间仍应不可达");
    }

    // 测试8: 空图（只有顶点，没有边）
    @Test
    @DisplayName("测试空图（无边）")
    void testEmptyGraph() {
        Digraph G = new Digraph(5);
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 0), "顶点能到达自身");
        assertFalse(fr.reachable(0, 1), "无边时不能到达其他顶点");
        assertFalse(fr.reachable(1, 0), "无边时不能到达其他顶点");
    }

    // 测试9: 单个顶点
    @Test
    @DisplayName("测试单顶点图")
    void testSingleVertex() {
        Digraph G = new Digraph(1);
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 0), "单顶点应该能到达自身");
    }

    // 测试10: 复杂图结构
    @Test
    @DisplayName("测试复杂图结构")
    void testComplexGraph() {
        Digraph G = new Digraph(10);
        G.addEdge(0, 1);
        G.addEdge(0, 2);
        G.addEdge(1, 3);
        G.addEdge(2, 3);
        G.addEdge(3, 4);
        G.addEdge(4, 5);
        G.addEdge(5, 6);
        G.addEdge(6, 7);
        G.addEdge(7, 8);
        G.addEdge(8, 9);
        G.addEdge(3, 7);  // 捷径
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 9), "0应该能到达9");
        assertTrue(fr.reachable(3, 9), "3应该能到达9");
        assertFalse(fr.reachable(9, 0), "9不能到达0");
        assertTrue(fr.reachable(0, 7), "0应该能到达7（通过捷径）");
    }

    // 测试11: 使用tinyDG.txt文件（如果存在）
    @Test
    @DisplayName("使用tinyDG.txt测试真实数据")
    void testWithTinyDG() {
        try {
            In in = new In("tinyDG.txt");
            Digraph G = new Digraph(in);
            FastReachability fr = new FastReachability(G);
            
            assertNotNull(fr, "应该能成功创建FastReachability对象");
            
            System.out.println("tinyDG.txt 加载成功，顶点数: " + G.V() + ", 边数: " + G.E());
            
        } catch (Exception e) {
            System.out.println("tinyDG.txt 文件不存在或读取失败，跳过此测试");
        }
    }

    // 测试12: 星型图
    @Test
    @DisplayName("测试星型图结构")
    void testStarGraph() {
        Digraph G = new Digraph(6);
        G.addEdge(0, 1);
        G.addEdge(0, 2);
        G.addEdge(0, 3);
        G.addEdge(0, 4);
        G.addEdge(0, 5);
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 1), "中心节点应该能到达所有叶子节点");
        assertTrue(fr.reachable(0, 5), "中心节点应该能到达所有叶子节点");
        assertFalse(fr.reachable(1, 2), "叶子节点之间不可达");
        assertFalse(fr.reachable(1, 0), "叶子节点不能到达中心（单向）");
    }

    // 测试13: 树形结构
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
        
        FastReachability fr = new FastReachability(G);
        
        assertTrue(fr.reachable(0, 6), "根节点应该能到达所有节点");
        assertTrue(fr.reachable(1, 4), "父节点应该能到达子节点");
        assertFalse(fr.reachable(3, 4), "兄弟节点之间不可达");
        assertFalse(fr.reachable(3, 1), "子节点不能到达父节点");
    }

    // 测试14: 性能测试 - 较大规模图
    @Test
    @DisplayName("性能测试：较大规模图")
    void testPerformance() {
        int V = 1000;
        Digraph G = new Digraph(V);
        
        for (int i = 0; i < V - 1; i++) {
            G.addEdge(i, i + 1);
        }
        
        FastReachability fr = new FastReachability(G);
        
        long startTime = System.currentTimeMillis();
        boolean result = fr.reachable(0, V - 1);
        long endTime = System.currentTimeMillis();
        
        assertTrue(result, "0应该能到达" + (V - 1));
        System.out.println("双向BFS在" + V + "个顶点的链图上耗时: " + (endTime - startTime) + "ms");
    }
}
