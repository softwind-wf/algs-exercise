package cn.exercise.algs4.datastructure.graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GraphIO} 的单元测试:解析、流/文件读写、格式往返、DOT 导出与异常约定。
 *
 * <p>验证重点:</p>
 * <ol>
 *   <li>格式:空白一律作分隔符;自环、平行边、0 顶点图都能正确解析;</li>
 *   <li>往返:{@code parse(format(g))} 与 {@code g} 结构一致(V、E、每个顶点的度、边集、自环数);</li>
 *   <li>异常分类:格式/参数问题抛 {@link IllegalArgumentException},真 I/O 故障抛 {@link IllegalStateException};</li>
 *   <li>资源归属:调用方传入的流/Reader/Writer 不被本类关闭,本类自己打开的文件流一定关闭
 *       (用带"关闭标记"的流验证);</li>
 *   <li>DOT 导出:每条边只出一次、自环只出一条。</li>
 * </ol>
 */
@DisplayName("GraphIO 图读写工具测试")
class GraphIOTest {

    /** 工作区根目录下的 algs4 标准样例图:13 个顶点、15 条边 */
    private static final String TINY_G_PATH = "tinyG.txt";

    /** 期望的 tinyG.txt 顶点度数(顶点 0..12) */
    private static final int[] TINY_G_DEGREES = {4, 1, 1, 2, 3, 3, 3, 2, 2, 3, 2, 2, 2};

    @Nested
    @DisplayName("解析")
    class ParseTest {

        @Test
        @DisplayName("空白(空格/制表/换行)一律作为分隔符")
        void parseWhitespace() {
            UndirectedGraph g = GraphIO.parse("4\n3\n0 1\n1\t2\n 2 3 \n");
            assertEquals(4, g.V());
            assertEquals(3, g.E());
            assertTrue(g.hasEdge(0, 1));
            assertTrue(g.hasEdge(1, 2));
            assertTrue(g.hasEdge(2, 3));
            assertEquals(2, g.degree(1));
            assertEquals(2, g.maxDegree(), "顶点 1 同时连着 0 和 2");
            assertEquals(1, g.minDegree(), "顶点 0、3 各只连一条边");
        }

        @Test
        @DisplayName("全部 token 挤在一行也能解析(格式与换行无关)")
        void parseSingleLine() {
            UndirectedGraph g = GraphIO.parse("3 2 0 1 1 2");
            assertEquals(3, g.V());
            assertEquals(2, g.E());
            assertTrue(g.hasEdge(0, 1));
            assertTrue(g.hasEdge(1, 2));
            assertFalse(g.hasEdge(0, 2));
        }

        @Test
        @DisplayName("自环、平行边、自环+平行边的重数都按原样读入")
        void parseSelfLoopAndParallelEdges() {
            UndirectedGraph g = GraphIO.parse("3\n4\n0 1\n0 1\n2 2\n2 2\n");
            assertEquals(4, g.E());
            assertEquals(2, g.degree(0), "两条平行边 0-1");
            assertEquals(2, g.degree(1), "平行边的另一端同样按重数计");
            assertEquals(4, g.degree(2), "两个自环,每个贡献 2 度");
            assertEquals(2, g.selfLoopCount());
            assertEquals(8, g.degreeSum());
        }

        @Test
        @DisplayName("0 个顶点、0 条边是合法输入")
        void parseEmptyGraph() {
            UndirectedGraph g = GraphIO.parse("0\n0\n");
            assertEquals(0, g.V());
            assertEquals(0, g.E());
            assertFalse(g.edges().iterator().hasNext());

            UndirectedGraph g2 = GraphIO.parse("3\n0");
            assertEquals(3, g2.V());
            assertEquals(0, g2.E());
            assertEquals(0, g2.degreeSum());
        }

        @Test
        @DisplayName("格式错误:空输入、缺项、多项、非整数、边数为负、端点越界、null")
        void parseErrors() {
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse(""));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("   "));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("\n\t\n"));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("5"), "缺边数");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("x 0"), "顶点数非整数");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("5 y"), "边数非整数");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("5 -1"), "边数为负");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("5 2 0 1"), "端点数少于声明");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("5 1 0 1 2 3"), "端点数多于声明");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("5 1 x 1"), "端点非整数");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("5 1 0 5"), "端点越界");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("5 1 -1 0"), "端点为负");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("-3 0"), "顶点数为负");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parse(null), "文本为 null");
        }
    }

    @Nested
    @DisplayName("读取(字符流/字节流/文件)")
    class ReadTest {

        @Test
        @DisplayName("readFile(tinyG.txt):V、E、逐点度数与样例一致")
        void readTinyGFile() {
            File file = new File(TINY_G_PATH);
            assertTrue(file.isFile(), "需要工作区根目录下存在 " + TINY_G_PATH
                    + "(当前目录:" + file.getAbsolutePath() + ")");
            UndirectedGraph g = GraphIO.readFile(TINY_G_PATH);
            assertEquals(13, g.V());
            assertEquals(15, g.E());
            for (int v = 0; v < 13; v++) {
                assertEquals(TINY_G_DEGREES[v], g.degree(v), "顶点 " + v + " 度数不符");
            }
            assertEquals(30, g.degreeSum());
            assertEquals(4, g.maxDegree());
            assertEquals(1, g.minDegree());
        }

        @Test
        @DisplayName("parse / read(StringReader) / read(ByteArrayInputStream) 三者结果一致")
        void threeEntryPointsAgree() {
            String text = "5\n4\n0 1\n1 2\n2 3\n3 4\n";
            UndirectedGraph a = GraphIO.parse(text);
            UndirectedGraph b = GraphIO.read(new StringReader(text));
            UndirectedGraph c = GraphIO.read(new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8"))));
            assertSameStructure(a, b);
            assertSameStructure(a, c);
            assertEquals(a.toString(), c.toString(), "同一份输入,邻接表顺序也应一致");
        }

        @Test
        @DisplayName("中文/非 ASCII 环境不影响解析(按 UTF-8 读字节流)")
        void utf8ByteStream() {
            String text = "2\n1\n0 1\n";
            UndirectedGraph g = GraphIO.read(new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8"))));
            assertEquals(2, g.V());
            assertTrue(g.hasEdge(0, 1));
        }

        @Test
        @DisplayName("参数为 null 或文件不存在都抛 IllegalArgumentException")
        void readErrors() {
            assertThrows(IllegalArgumentException.class, () -> GraphIO.readFile(null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.readFile("no-such-graph-file.txt"));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.read((Reader) null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.read((InputStream) null));
        }

        @Test
        @DisplayName("调用方传入的 Reader / InputStream 不被 GraphIO 关闭")
        void callerOwnedStreamsAreNotClosed() {
            TrackingReader reader = new TrackingReader("2\n1\n0 1\n");
            UndirectedGraph fromReader = GraphIO.read(reader);
            assertEquals(2, fromReader.V());
            assertFalse(reader.closed, "GraphIO 不应关闭调用方传入的 Reader");

            TrackingInputStream in = new TrackingInputStream(
                    new ByteArrayInputStream("2\n1\n0 1\n".getBytes(Charset.forName("UTF-8"))));
            UndirectedGraph fromStream = GraphIO.read(in);
            assertEquals(2, fromStream.V());
            assertFalse(in.closed, "GraphIO 不应关闭调用方传入的 InputStream");

            reader.close();
            assertTrue(reader.closed, "调用方自己关闭后应生效");
        }

        @Test
        @DisplayName("读取时的 I/O 故障包装成 IllegalStateException(保留原因)")
        void ioFailureIsWrapped() {
            Reader failing = new Reader() {
                @Override
                public int read(char[] cbuf, int off, int len) throws IOException {
                    throw new IOException("模拟磁盘故障");
                }

                @Override
                public void close() {
                    // 调用方自己关闭,这里无需处理
                }
            };
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> GraphIO.read(failing));
            assertNotNull(e.getCause(), "应保留原始 IOException 作为 cause");
            assertTrue(e.getMessage().contains("模拟磁盘故障"), "异常信息应带上原因:" + e.getMessage());
        }
    }

    @Nested
    @DisplayName("格式化与写出")
    class WriteTest {

        @Test
        @DisplayName("format 输出 algs4 格式:第 1 行 V、第 2 行 E、其后每行一条边")
        void formatShape() {
            UndirectedGraph g = new UndirectedGraph(4);
            g.addEdge(0, 1);
            g.addEdge(1, 2);
            g.addEdge(2, 2);
            String text = GraphIO.format(g);
            String[] lines = text.split("\\r?\\n");
            assertEquals("4", lines[0]);
            assertEquals("3", lines[1]);
            assertEquals(2 + g.E(), lines.length, "总共 2 + E 行");
            assertEquals("0 1", lines[2]);
            assertEquals("1 2", lines[3]);
            assertEquals("2 2", lines[4], "自环只写一条");
        }

        @Test
        @DisplayName("往返:parse(format(g)) 与 g 的 V/E/度数/边集/自环数完全一致")
        void roundTrip() {
            UndirectedGraph g = new UndirectedGraph(6);
            g.addEdge(0, 1);
            g.addEdge(0, 1);      // 平行边
            g.addEdge(2, 2);      // 自环
            g.addEdge(2, 2);      // 再来一个自环
            g.addEdge(3, 4);
            g.addEdge(5, 5);
            g.addEdge(0, 5);

            UndirectedGraph copy = GraphIO.parse(GraphIO.format(g));
            assertSameStructure(g, copy);
            assertEquals(g.E(), copy.E());
            assertEquals(7, copy.E(), "2(平行边)+2(两自环)+1+1+1");
            assertEquals(3, copy.selfLoopCount());
            assertEquals(4, copy.degree(2), "两个自环 → 4 度");
            assertEquals(3, copy.degree(0), "两条平行边 0-1 保留重数,再加 0-5");
        }

        @Test
        @DisplayName("无边的图往返后仍为无边")
        void roundTripEmptyEdges() {
            UndirectedGraph g = new UndirectedGraph(3);
            UndirectedGraph copy = GraphIO.parse(GraphIO.format(g));
            assertSameStructure(g, copy);
            assertEquals(0, copy.E());
        }

        @Test
        @DisplayName("write(StringWriter) 写出的内容与 format 相同,且不关闭该 Writer")
        void writeToWriter() {
            UndirectedGraph g = GraphIO.parse("3\n2\n0 1\n1 2\n");
            StringWriter writer = new StringWriter();
            GraphIO.write(g, writer);
            assertEquals(GraphIO.format(g), writer.toString());
            assertSameStructure(g, GraphIO.parse(writer.toString()));
        }

        @Test
        @DisplayName("write 到文件再 readFile 回来,结构与原图一致")
        void writeAndReadFile() {
            UndirectedGraph g = GraphIO.parse("4\n4\n0 1\n0 1\n2 2\n3 0\n");
            File dir = new File("target/graph-io-test");
            assertTrue(dir.isDirectory() || dir.mkdirs(), "无法创建测试目录:" + dir.getAbsolutePath());
            File file = new File(dir, "round-trip-" + System.nanoTime() + ".txt");
            try {
                GraphIO.write(g, file.getPath());
                assertTrue(file.isFile());
                UndirectedGraph back = GraphIO.readFile(file.getPath());
                assertSameStructure(g, back);
                assertEquals(4, back.E());
                assertEquals(1, back.selfLoopCount());
                assertEquals(3, back.degree(0), "两条平行边 0-1 保留重数,再加 0-3");
            }
            finally {
                assertTrue(!file.exists() || file.delete(), "测试文件应能删除:" + file.getAbsolutePath());
            }
        }

        @Test
        @DisplayName("参数为 null 抛 IllegalArgumentException")
        void writeNullArguments() {
            UndirectedGraph g = new UndirectedGraph(1);
            assertThrows(IllegalArgumentException.class, () -> GraphIO.format((UndirectedGraph) null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.write((UndirectedGraph) null, "target/x.txt"));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.write(g, (String) null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.write(g, (java.io.OutputStream) null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.write(g, (java.io.Writer) null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.toDot((UndirectedGraph) null));
        }

        @Test
        @DisplayName("目标目录不存在时抛 IllegalArgumentException")
        void writeToMissingDirectory() {
            UndirectedGraph g = GraphIO.parse("2\n1\n0 1\n");
            String path = "target/graph-io-test/no-such-dir-x/graph.txt";
            assertThrows(IllegalArgumentException.class, () -> GraphIO.write(g, path));
        }

        @Test
        @DisplayName("调用方传入的 OutputStream 不被 GraphIO 关闭")
        void callerOwnedOutputStreamIsNotClosed() {
            UndirectedGraph g = GraphIO.parse("2\n1\n0 1\n");
            TrackingOutputStream out = new TrackingOutputStream();
            GraphIO.write(g, (java.io.OutputStream) out);
            assertFalse(out.closed, "GraphIO 不应关闭调用方传入的 OutputStream");
            assertTrue(out.size() > 0, "应已写出内容");
        }
    }

    @Nested
    @DisplayName("Graphviz DOT 导出")
    class DotTest {

        @Test
        @DisplayName("graph 包裹、边只输出一次、自环只输出一条")
        void toDotFormat() {
            UndirectedGraph g = new UndirectedGraph(3);
            g.addEdge(0, 1);
            g.addEdge(2, 2);
            String dot = GraphIO.toDot(g);
            assertTrue(dot.startsWith("graph {"));
            assertTrue(dot.trim().endsWith("}"));
            assertEquals(1, occurrences(dot, "0 -- 1"));
            assertEquals(0, occurrences(dot, "1 -- 0"), "无向边只写一个方向");
            assertEquals(1, occurrences(dot, "2 -- 2"), "自环的两个端点相同,只画一条");
            assertTrue(dot.contains("node[shape=circle"), "应带默认节点样式");
        }

        @Test
        @DisplayName("空图导出后只有 graph 头、节点样式与收尾,没有边行")
        void toDotEmptyGraph() {
            String dot = GraphIO.toDot(new UndirectedGraph(0));
            String[] lines = dot.split("\\r?\\n");
            assertEquals("graph {", lines[0]);
            assertEquals("}", lines[lines.length - 1]);
            assertEquals(0, occurrences(dot, "--"));
            assertEquals(0, occurrences(dot, " -- "));
        }
    }

    @Nested
    @DisplayName("加权图读写")
    class WeightedIoTest {

        private static final String TINY_EWG_PATH = "tinyEWG.txt";

        @Test
        @DisplayName("readWeightedFile(tinyEWG.txt):V=8、E=16、逐点度数与样例一致")
        void readTinyEWGFile() {
            File file = new File(TINY_EWG_PATH);
            assertTrue(file.isFile(), "需要工作区根目录下存在 " + TINY_EWG_PATH);
            EdgeWeightedGraph g = GraphIO.readWeightedFile(TINY_EWG_PATH);
            assertEquals(8, g.V());
            assertEquals(16, g.E());
            assertEquals(4, g.degree(0));
            assertEquals(4, g.degree(4));
            assertEquals(5, g.degree(2));
            assertEquals(5, g.degree(7));
            assertEquals(32, g.degreeSum());
            assertEquals(0, g.selfLoopCount());
        }

        @Test
        @DisplayName("parseWeighted / readWeighted(StringReader) / readWeighted(字节流) 三者一致")
        void threeEntryPointsAgree() {
            String text = "3\n3\n0 1 0.5\n1 2 1.5\n0 2 2.5\n";
            EdgeWeightedGraph a = GraphIO.parseWeighted(text);
            EdgeWeightedGraph b = GraphIO.readWeighted(new StringReader(text));
            EdgeWeightedGraph c = GraphIO.readWeighted(new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8"))));
            assertSameWeightedStructure(a, b);
            assertSameWeightedStructure(a, c);
            assertEquals(a.toString(), c.toString(), "同一份输入,邻接表顺序也应一致");
        }

        @Test
        @DisplayName("往返:parseWeighted(format(g)) 的 V/E/边集/度数一致(含负权与自环)")
        void roundTrip() {
            EdgeWeightedGraph g = new EdgeWeightedGraph(5);
            g.addEdge(0, 1, 0.35);
            g.addEdge(0, 1, 0.37);      // 平行边
            g.addEdge(2, 2, -1.5);      // 自环 + 负权
            g.addEdge(3, 4, 0.0);
            g.addEdge(4, 0, 12.25);

            EdgeWeightedGraph copy = GraphIO.parseWeighted(GraphIO.format(g));
            assertSameWeightedStructure(g, copy);
            assertEquals(g.E(), copy.E());
            assertSameEdgeMultiset(g, copy);
        }

        @Test
        @DisplayName("write 到文件再 readWeightedFile 回来,结构一致")
        void writeAndReadFile() {
            EdgeWeightedGraph g = GraphIO.parseWeighted("4\n3\n0 1 0.25\n1 2 -0.5\n2 2 0.75\n");
            File dir = new File("target/graph-io-test");
            assertTrue(dir.isDirectory() || dir.mkdirs(), "无法创建测试目录:" + dir.getAbsolutePath());
            File file = new File(dir, "weighted-round-trip-" + System.nanoTime() + ".txt");
            try {
                GraphIO.write(g, file.getPath());
                assertTrue(file.isFile());
                EdgeWeightedGraph back = GraphIO.readWeightedFile(file.getPath());
                assertSameWeightedStructure(g, back);
                assertSameEdgeMultiset(g, back);
                assertEquals(-0.5, back.weightOf(1, 2), 1e-12);
            }
            finally {
                assertTrue(!file.exists() || file.delete(), "测试文件应能删除:" + file.getAbsolutePath());
            }
        }

        @Test
        @DisplayName("格式错误:空输入、缺边数、三元组数目不符、非整数端点、非实数权值、NaN/Infinity、越界、null")
        void parseWeightedErrors() {
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted(""));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("   "));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5"), "缺边数");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 x"), "边数非整数");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 -1"), "边数为负");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 2 0 1 0.5"), "三元组少于声明");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 1 0 1 0.5 2 3"), "三元组多于声明");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 1 0 1 abc"), "权值非实数");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 1 x 1 0.5"), "端点非整数");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 1 0 1 NaN"), "NaN 权值");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 1 0 1 Infinity"), "无穷权值");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("5 1 0 5 0.5"), "端点越界");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted("-3 0"), "顶点数为负");
            assertThrows(IllegalArgumentException.class, () -> GraphIO.parseWeighted(null), "文本为 null");

            assertThrows(IllegalArgumentException.class, () -> GraphIO.readWeightedFile(null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.readWeightedFile("no-such-weighted-file.txt"));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.readWeighted((Reader) null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.readWeighted((InputStream) null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.format((EdgeWeightedGraph) null));
            assertThrows(IllegalArgumentException.class, () -> GraphIO.toDot((EdgeWeightedGraph) null));
        }

        @Test
        @DisplayName("加权 DOT:边带上权值标签,每条边一次")
        void toDotWeighted() {
            EdgeWeightedGraph g = new EdgeWeightedGraph(3);
            g.addEdge(0, 1, 0.35);
            g.addEdge(1, 2, 1.5);
            String dot = GraphIO.toDot(g);
            assertTrue(dot.startsWith("graph {"), dot);
            assertTrue(dot.contains("0 -- 1 [label=\"0.35\"]"), dot);
            assertTrue(dot.contains("1 -- 2 [label=\"1.5\"]"), dot);
            assertEquals(1, occurrences(dot, "0 -- 1"));
            assertEquals(0, occurrences(dot, "1 -- 0"), "无向边只写一个方向");
        }

        @Test
        @DisplayName("调用方传入的 Reader 不被关闭")
        void callerOwnedReaderNotClosed() {
            TrackingReader reader = new TrackingReader("2\n1\n0 1 0.5\n");
            EdgeWeightedGraph g = GraphIO.readWeighted(reader);
            assertEquals(2, g.V());
            assertFalse(reader.closed, "GraphIO 不应关闭调用方传入的 Reader");
            reader.close();
        }

        /** V、E、逐点度数、边集(按字符串排序)一致 */
        private void assertSameWeightedStructure(EdgeWeightedGraph expected, EdgeWeightedGraph actual) {
            assertEquals(expected.V(), actual.V(), "顶点数");
            assertEquals(expected.E(), actual.E(), "边数");
            assertEquals(expected.degreeSum(), actual.degreeSum(), "总度数");
            assertEquals(expected.selfLoopCount(), actual.selfLoopCount(), "自环数");
            for (int v = 0; v < expected.V(); v++) {
                assertEquals(expected.degree(v), actual.degree(v), "顶点 " + v + " 的度数");
            }
            assertSameEdgeMultiset(expected, actual);
        }

        /** 边按 "v-w weight" 字符串排序后逐条比对(端点归一,便于跨实现比较) */
        private void assertSameEdgeMultiset(EdgeWeightedGraph expected, EdgeWeightedGraph actual) {
            List<String> a = normalizedEdges(expected);
            List<String> b = normalizedEdges(actual);
            assertEquals(a, b, "边集(含权值)不一致");
        }

        private List<String> normalizedEdges(EdgeWeightedGraph g) {
            List<String> list = new ArrayList<String>();
            for (Edge e : g.edges()) {
                int v = e.either();
                int w = e.other(v);
                list.add(Math.min(v, w) + "-" + Math.max(v, w) + " " + e.weight());
            }
            java.util.Collections.sort(list);
            return list;
        }
    }

    // ------------------------------------------------------------------
    // 测试辅助
    // ------------------------------------------------------------------

    /**
     * 结构等价:V、E、自环数、总度数、逐点度数、逐点邻接点集合、去重后的边集。
     *
     * <p>刻意<b>不</b>比较邻接表的迭代顺序 —— 写出行是按顶点下标、读入是按边表顺序,
     * 两次装配的插入顺序不同,顺序属于实现细节(结构一致才是契约)。</p>
     */
    private static void assertSameStructure(UndirectedGraph expected, UndirectedGraph actual) {
        assertEquals(expected.V(), actual.V(), "顶点数");
        assertEquals(expected.E(), actual.E(), "边数");
        assertEquals(expected.selfLoopCount(), actual.selfLoopCount(), "自环数");
        assertEquals(expected.degreeSum(), actual.degreeSum(), "总度数");
        assertEquals(expected.maxDegree(), actual.maxDegree(), "最大度数");
        assertEquals(expected.minDegree(), actual.minDegree(), "最小度数");
        for (int v = 0; v < expected.V(); v++) {
            assertEquals(expected.degree(v), actual.degree(v), "顶点 " + v + " 的度数");
            assertEquals(neighborsOf(expected, v), neighborsOf(actual, v), "顶点 " + v + " 的邻接表");
        }
        assertEquals(edgeKeys(expected), edgeKeys(actual), "去重后的边集");
    }

    private static TreeSet<Integer> neighborsOf(UndirectedGraph g, int v) {
        TreeSet<Integer> set = new TreeSet<Integer>();
        for (int w : g.adj(v)) {
            set.add(w);
        }
        return set;
    }

    private static TreeSet<String> edgeKeys(UndirectedGraph g) {
        TreeSet<String> keys = new TreeSet<String>();
        for (int[] e : g.edges()) {
            keys.add(Math.min(e[0], e[1]) + "-" + Math.max(e[0], e[1]));
        }
        return keys;
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }

    /** 带"是否被关闭"标记的 Reader,用来验证资源归属 */
    private static final class TrackingReader extends StringReader {
        private boolean closed;

        TrackingReader(String text) {
            super(text);
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }
    }

    /** 带"是否被关闭"标记的输入流 */
    private static final class TrackingInputStream extends FilterInputStream {
        private boolean closed;

        TrackingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /** 带"是否被关闭"标记的内存输出流 */
    private static final class TrackingOutputStream extends java.io.ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
