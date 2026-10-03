package cn.exercise.algs4.datastructure.graph;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * 图的文本/文件读写工具:把"外部文本格式"和"图的数据结构"彻底分开。
 *
 * <p>{@link UndirectedGraph} 只负责图本身(顶点、边、遍历),不认识文件、不解析文本;
 * 本类承担全部格式转换:algs4 文本格式的解析与输出、Graphviz DOT 的导出。
 * 这样做的好处是:数据结构换存储方式(邻接表→邻接矩阵)不必动 IO;
 * 换数据格式(比如改用 CSV、改用加权图的五元组)也只动本类。</p>
 *
 * <p><b>algs4 文本格式</b>(空白分隔,空格/制表/换行等价):</p>
 * <pre>
 * 第一项      顶点数 V
 * 第二项      边数 E
 * 随后 2E 项  每条边的两个端点,依次给出
 * </pre>
 *
 * <p>示例({@code tinyG.txt},13 个顶点、15 条边):</p>
 * <pre>
 * 13
 * 15
 * 0 1
 * 0 2
 * ...
 * 11 12
 * </pre>
 *
 * <p><b>方法一览</b>:</p>
 * <ul>
 *   <li>无权图 —— 读:{@link #parse(String)}(文本)、{@link #read(Reader)}、{@link #read(InputStream)}、
 *       {@link #readFile(String)}(后三者默认/要求 UTF-8);写:{@link #format(UndirectedGraph)}、
 *       {@link #write(UndirectedGraph, OutputStream)}、{@link #write(UndirectedGraph, String)};</li>
 *   <li>加权图 —— 读:{@link #parseWeighted(String)}、{@link #readWeighted(InputStream)}、
 *       {@link #readWeightedFile(String)};写:{@link #format(EdgeWeightedGraph)}、
 *       {@link #write(EdgeWeightedGraph, OutputStream)}、{@link #write(EdgeWeightedGraph, String)};</li>
 *   <li>导出:{@link #toDot(UndirectedGraph)} 与 {@link #toDot(EdgeWeightedGraph)}
 *       (Graphviz DOT,可用 {@code dot -Tsvg} 出图)。</li>
 * </ul>
 *
 * <p><b>加权图的文本格式</b>(algs4 的 {@code EdgeWeightedGraph(In)})与无权图只差在
 * 每条边多一个权值 —— 前两项仍是 V、E,随后是 {@code 3E} 项:{@code v w weight} 三元组。
 * 权值可以是负数或 0,但必须是有限实数。</p>
 * <pre>
 * 8
 * 16
 * 4 5 0.35
 * 4 7 0.37
 * ...
 * </pre>
 *
 * <p><b>与 {@link UndirectedGraph#toString()} 的区别</b>:{@code toString()} 是给人看的调试视图
 * (首行写 {@code "13 vertices, 15 edges"},逐行列出每个顶点的邻接表,<b>不能</b>再被解析);
 * 本类的 {@link #format(UndirectedGraph)} 才是可交换的数据格式(V、E、边表),可以回读。
 * 两者刻意分开,避免"展示格式"和"数据格式"互相绑死。</p>
 *
 * <p><b>异常约定</b>:参数为 null、文本格式错误(缺项、多项、非整数、边数为负、端点越界)
 * 一律抛 {@link IllegalArgumentException};文件不存在也归入这一类(路径本身有问题),
 * 而读写过程中真正发生的 I/O 故障抛 {@link IllegalStateException}。
 * 由调用方传入的流/Reader/Writer <b>不由本类关闭</b>;本类自己打开的文件流一定关闭。</p>
 *
 * <p><b>输入输出样例</b>:</p>
 * <pre>
 * UndirectedGraph g = GraphIO.readFile("tinyG.txt");
 * System.out.print(GraphIO.toDot(g));            // 画图用
 * GraphIO.write(g, "copy.txt");                  // 写回 algs4 格式
 * </pre>
 *
 * @see UndirectedGraph
 */
public final class GraphIO {

    /** UTF-8:本类默认读写编码 */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /** 工具类不允许实例化 */
    private GraphIO() {
        throw new AssertionError("GraphIO 是工具类,不应该被实例化");
    }

    // ------------------------------------------------------------------
    // 读:文本 / 流 / 文件
    // ------------------------------------------------------------------

    /**
     * 按 algs4 文本格式解析字符串建图。
     *
     * @param text 图数据文本,不能为 null
     * @return 解析出的图
     * @throws IllegalArgumentException 文本为 null、为空、格式错误或端点越界
     */
    public static UndirectedGraph parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("输入文本不能为 null");
        }
        return build(tokenize(text));
    }

    /**
     * 从字符流读取并按 algs4 文本格式建图;<b>流由调用方关闭</b>。
     *
     * @param reader 字符流,不能为 null
     * @return 解析出的图
     * @throws IllegalArgumentException 参数为 null 或内容格式错误
     * @throws IllegalStateException    读取过程中发生 I/O 错误
     */
    public static UndirectedGraph read(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("字符流不能为 null");
        }
        try {
            return parse(readAll(reader));
        }
        catch (IOException e) {
            throw new IllegalStateException("读取图数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从字节流读取(UTF-8)并按 algs4 文本格式建图;<b>流由调用方关闭</b>。
     *
     * @param in 字节流,不能为 null
     * @return 解析出的图
     * @throws IllegalArgumentException 参数为 null 或内容格式错误
     * @throws IllegalStateException    读取过程中发生 I/O 错误
     */
    public static UndirectedGraph read(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("输入流不能为 null");
        }
        return read(new InputStreamReader(in, UTF_8));
    }

    /**
     * 按 algs4 文本格式读取文件(UTF-8)。本类负责关闭文件流。
     *
     * @param path 文件路径,不能为 null
     * @return 解析出的图
     * @throws IllegalArgumentException 路径为 null、文件不存在、内容格式错误或端点越界
     * @throws IllegalStateException    读取过程中发生 I/O 错误
     */
    public static UndirectedGraph readFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为 null");
        }
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            return read(in);
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("文件不存在: " + path, e);
        }
        finally {
            closeQuietly(in);
        }
    }

    // ------------------------------------------------------------------
    // 读:加权图
    // ------------------------------------------------------------------

    /**
     * 按 algs4 加权图文本格式解析字符串建图:V、E、然后 E 个 {@code v w weight} 三元组。
     *
     * @param text 图数据文本,不能为 null
     * @return 解析出的加权图
     * @throws IllegalArgumentException 文本为 null/为空/格式错误、权值非有限实数或端点越界
     */
    public static EdgeWeightedGraph parseWeighted(String text) {
        if (text == null) {
            throw new IllegalArgumentException("输入文本不能为 null");
        }
        return buildWeighted(tokenize(text));
    }

    /**
     * 从字符流读取并按 algs4 加权图格式建图;<b>流由调用方关闭</b>。
     *
     * @param reader 字符流,不能为 null
     * @return 解析出的加权图
     * @throws IllegalArgumentException 参数为 null 或内容格式错误
     * @throws IllegalStateException    读取过程中发生 I/O 错误
     */
    public static EdgeWeightedGraph readWeighted(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("字符流不能为 null");
        }
        try {
            return parseWeighted(readAll(reader));
        }
        catch (IOException e) {
            throw new IllegalStateException("读取图数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从字节流读取(UTF-8)并按 algs4 加权图格式建图;<b>流由调用方关闭</b>。
     *
     * @param in 字节流,不能为 null
     * @return 解析出的加权图
     * @throws IllegalArgumentException 参数为 null 或内容格式错误
     * @throws IllegalStateException    读取过程中发生 I/O 错误
     */
    public static EdgeWeightedGraph readWeighted(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("输入流不能为 null");
        }
        return readWeighted(new InputStreamReader(in, UTF_8));
    }

    /**
     * 按 algs4 加权图文本格式读取文件(UTF-8)。本类负责关闭文件流。
     *
     * @param path 文件路径,不能为 null
     * @return 解析出的加权图
     * @throws IllegalArgumentException 路径为 null、文件不存在、内容格式错误或端点越界
     * @throws IllegalStateException    读取过程中发生 I/O 错误
     */
    public static EdgeWeightedGraph readWeightedFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为 null");
        }
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            return readWeighted(in);
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("文件不存在: " + path, e);
        }
        finally {
            closeQuietly(in);
        }
    }

    // ------------------------------------------------------------------
    // 写:文本 / 流 / 文件
    // ------------------------------------------------------------------

    /**
     * 把图转成 algs4 文本格式(可被 {@link #parse(String)} 原样读回)。
     *
     * <p>输出三部分:顶点数、边数、每条边一行两个端点。边取自 {@link UndirectedGraph#edges()},
     * 因此平行边按重数逐条输出、自环只输出一条 —— 重新读入后 V、E、每个顶点的度数都与原图一致。</p>
     *
     * @param graph 待输出的图,不能为 null
     * @return algs4 文本格式的字符串
     * @throws IllegalArgumentException {@code graph} 为 null
     */
    public static String format(UndirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("待输出的图不能为 null");
        }
        String newline = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(graph.V()).append(newline);
        sb.append(graph.E()).append(newline);
        for (int[] edge : graph.edges()) {
            sb.append(edge[0]).append(' ').append(edge[1]).append(newline);
        }
        return sb.toString();
    }

    /**
     * 把图以 algs4 文本格式(UTF-8)写入字符流;<b>流由调用方关闭</b>。
     *
     * @param graph 待输出的图,不能为 null
     * @param writer 目标字符流,不能为 null
     * @throws IllegalArgumentException 参数为 null
     * @throws IllegalStateException    写入过程中发生 I/O 错误
     */
    public static void write(UndirectedGraph graph, Writer writer) {
        if (writer == null) {
            throw new IllegalArgumentException("输出字符流不能为 null");
        }
        try {
            writer.write(format(graph));
            writer.flush();
        }
        catch (IOException e) {
            throw new IllegalStateException("写出图数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 把图以 algs4 文本格式(UTF-8)写入字节流;<b>流由调用方关闭</b>。
     *
     * @param graph 待输出的图,不能为 null
     * @param out 目标字节流,不能为 null
     * @throws IllegalArgumentException 参数为 null
     * @throws IllegalStateException    写入过程中发生 I/O 错误
     */
    public static void write(UndirectedGraph graph, OutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException("输出流不能为 null");
        }
        write(graph, new OutputStreamWriter(out, UTF_8));
    }

    /**
     * 把图以 algs4 文本格式(UTF-8)写入文件。本类负责关闭文件流。
     *
     * @param graph 待输出的图,不能为 null
     * @param path 目标文件路径,不能为 null
     * @throws IllegalArgumentException 参数为 null;目标路径无法创建(如目录不存在)
     * @throws IllegalStateException    写入过程中发生 I/O 错误
     */
    public static void write(UndirectedGraph graph, String path) {
        if (graph == null) {
            throw new IllegalArgumentException("待输出的图不能为 null");
        }
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为 null");
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(path);
            write(graph, out);
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("无法写入文件: " + path, e);
        }
        finally {
            closeQuietly(out);
        }
    }

    // ------------------------------------------------------------------
    // 写:加权图
    // ------------------------------------------------------------------

    /**
     * 把加权图转成 algs4 加权图文本格式(可被 {@link #parseWeighted(String)} 原样读回)。
     * 每条边输出为 {@code v w weight} 一行;权值打印形式与 {@link Edge#toString()} 一致
     * (整数权值不会带 {@code .0} 之外的多余尾数)。
     *
     * @param graph 待输出的加权图,不能为 null
     * @return algs4 加权图文本格式的字符串
     * @throws IllegalArgumentException {@code graph} 为 null
     */
    public static String format(EdgeWeightedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("待输出的图不能为 null");
        }
        String newline = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(graph.V()).append(newline);
        sb.append(graph.E()).append(newline);
        for (Edge e : graph.edges()) {
            int v = e.either();
            int w = e.other(v);
            sb.append(v).append(' ').append(w).append(' ').append(e.weight()).append(newline);
        }
        return sb.toString();
    }

    /**
     * 把加权图以 algs4 格式(UTF-8)写入字符流;<b>流由调用方关闭</b>。
     *
     * @param graph  待输出的加权图,不能为 null
     * @param writer 目标字符流,不能为 null
     * @throws IllegalArgumentException 参数为 null
     * @throws IllegalStateException    写入过程中发生 I/O 错误
     */
    public static void write(EdgeWeightedGraph graph, Writer writer) {
        if (writer == null) {
            throw new IllegalArgumentException("输出字符流不能为 null");
        }
        try {
            writer.write(format(graph));
            writer.flush();
        }
        catch (IOException e) {
            throw new IllegalStateException("写出图数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 把加权图以 algs4 格式(UTF-8)写入字节流;<b>流由调用方关闭</b>。
     *
     * @param graph 待输出的加权图,不能为 null
     * @param out   目标字节流,不能为 null
     * @throws IllegalArgumentException 参数为 null
     * @throws IllegalStateException    写入过程中发生 I/O 错误
     */
    public static void write(EdgeWeightedGraph graph, OutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException("输出流不能为 null");
        }
        write(graph, new OutputStreamWriter(out, UTF_8));
    }

    /**
     * 把加权图以 algs4 格式(UTF-8)写入文件。本类负责关闭文件流。
     *
     * @param graph 待输出的加权图,不能为 null
     * @param path  目标文件路径,不能为 null
     * @throws IllegalArgumentException 参数为 null;目标路径无法创建(如目录不存在)
     * @throws IllegalStateException    写入过程中发生 I/O 错误
     */
    public static void write(EdgeWeightedGraph graph, String path) {
        if (graph == null) {
            throw new IllegalArgumentException("待输出的图不能为 null");
        }
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为 null");
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(path);
            write(graph, out);
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("无法写入文件: " + path, e);
        }
        finally {
            closeQuietly(out);
        }
    }

    // ------------------------------------------------------------------
    // 导出:Graphviz DOT
    // ------------------------------------------------------------------

    /**
     * 把图导出为 Graphviz DOT 文本({@code graph { ... }}),可直接用 {@code dot -Tsvg} 画图。
     * 每条边只输出一次,自环只输出一条(两个端点相同,画成一个环)。
     *
     * @param graph 待导出的图,不能为 null
     * @return DOT 文本
     * @throws IllegalArgumentException {@code graph} 为 null
     */
    public static String toDot(UndirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("待导出的图不能为 null");
        }
        String newline = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("graph {").append(newline);
        sb.append("node[shape=circle, style=filled, fixedsize=true, width=0.3, fontsize=\"10pt\"]").append(newline);
        for (int[] edge : graph.edges()) {
            sb.append(edge[0]).append(" -- ").append(edge[1]).append(newline);
        }
        sb.append("}").append(newline);
        return sb.toString();
    }

    /**
     * 把加权图导出为 Graphviz DOT 文本,边带上权值标签({@code 0 -- 7 [label="0.16"]}),
     * 可直接用 {@code dot -Tsvg} 画图。
     *
     * @param graph 待导出的加权图,不能为 null
     * @return DOT 文本
     * @throws IllegalArgumentException {@code graph} 为 null
     */
    public static String toDot(EdgeWeightedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("待导出的图不能为 null");
        }
        String newline = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("graph {").append(newline);
        sb.append("node[shape=circle, style=filled, fixedsize=true, width=0.3, fontsize=\"10pt\"]").append(newline);
        sb.append("edge[fontsize=\"9pt\"]").append(newline);
        for (Edge e : graph.edges()) {
            int v = e.either();
            int w = e.other(v);
            sb.append(v).append(" -- ").append(w)
                    .append(" [label=\"").append(e.weight()).append("\"]").append(newline);
        }
        sb.append("}").append(newline);
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 内部:切分与装配
    // ------------------------------------------------------------------

    /** 按空白切分;空串(或全空白)切成 0 个 token 之外的边角情况由 {@link #build} 统一报错 */
    private static String[] tokenize(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("\\s+");
    }

    /**
     * 校验并消费 token 序列:V、E、然后 2E 个端点。
     *
     * @throws IllegalArgumentException 数目不对或端点越界(端点校验委托给 {@code addEdge})
     */
    private static UndirectedGraph build(String[] tokens) {
        if (tokens.length == 0) {
            throw new IllegalArgumentException("输入为空,至少需要给出顶点数");
        }
        int V = parseInt(tokens[0], "顶点数");
        if (tokens.length < 2) {
            throw new IllegalArgumentException("缺少边数:第二项应为边数 E");
        }
        int E = parseInt(tokens[1], "边数");
        if (E < 0) {
            throw new IllegalArgumentException("边数必须非负,当前为 " + E);
        }
        if (tokens.length - 2 != 2 * E) {
            throw new IllegalArgumentException("边端点数目与边数不符:声明 " + E
                    + " 条边,需要 " + (2 * E) + " 个端点,实际给出 " + (tokens.length - 2) + " 个");
        }
        UndirectedGraph graph = new UndirectedGraph(V);
        for (int i = 0; i < E; i++) {
            int v = parseInt(tokens[2 + 2 * i], "第 " + (i + 1) + " 条边的起点");
            int w = parseInt(tokens[3 + 2 * i], "第 " + (i + 1) + " 条边的终点");
            graph.addEdge(v, w);
        }
        return graph;
    }

    /**
     * 校验并消费加权图的 token 序列:V、E、然后 3E 个值(每三个是 v、w、weight)。
     *
     * @throws IllegalArgumentException 数目不对、权值非法或端点越界
     */
    private static EdgeWeightedGraph buildWeighted(String[] tokens) {
        if (tokens.length == 0) {
            throw new IllegalArgumentException("输入为空,至少需要给出顶点数");
        }
        int V = parseInt(tokens[0], "顶点数");
        if (tokens.length < 2) {
            throw new IllegalArgumentException("缺少边数:第二项应为边数 E");
        }
        int E = parseInt(tokens[1], "边数");
        if (E < 0) {
            throw new IllegalArgumentException("边数必须非负,当前为 " + E);
        }
        if (tokens.length - 2 != 3 * E) {
            throw new IllegalArgumentException("三元组数目与边数不符:声明 " + E
                    + " 条边,每条边需要 v、w、weight 三项,共需 " + (3 * E)
                    + " 项,实际给出 " + (tokens.length - 2) + " 项");
        }
        EdgeWeightedGraph graph = new EdgeWeightedGraph(V);
        for (int i = 0; i < E; i++) {
            int v = parseInt(tokens[2 + 3 * i], "第 " + (i + 1) + " 条边的起点");
            int w = parseInt(tokens[3 + 3 * i], "第 " + (i + 1) + " 条边的终点");
            double weight = parseDouble(tokens[4 + 3 * i], "第 " + (i + 1) + " 条边的权值");
            graph.addEdge(new Edge(v, w, weight));
        }
        return graph;
    }

    /** 解析一个整数 token,失败时给出可定位的错误信息 */
    private static int parseInt(String token, String what) {
        try {
            return Integer.parseInt(token);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " 不是合法整数: \"" + token + "\"", e);
        }
    }

    /** 解析一个权值 token(有限实数),失败时给出可定位的错误信息 */
    private static double parseDouble(String token, String what) {
        double value;
        try {
            value = Double.parseDouble(token);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " 不是合法实数: \"" + token + "\"", e);
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(what + " 必须是有限实数: \"" + token + "\"");
        }
        return value;
    }

    /** 把读到的字符全部拼成字符串(JDK 8 无 Reader.readAllAsString) */
    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    /** 关闭流,忽略关闭失败(只读/只写场景下关闭失败没有补救价值) */
    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException ignored) {
                // 关闭失败不影响已读/已写的数据
            }
        }
    }
}
