package exercise4.exercise4_1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnePassSymbolGraph {
    private final Map<String, Integer> st;       // 符号名 -> 索引
    private final List<String> keys;             // 索引 -> 符号名（动态）
    private final List<List<Integer>> adj;      // 动态邻接表
    private int E;

    public OnePassSymbolGraph(String filename, String delimiter) throws IOException {
        st = new HashMap<>();
        keys = new ArrayList<>();
        adj = new ArrayList<>();
        E = 0;

        // 【核心：只遍历一次文件】
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            String[] names = line.split(delimiter);
            if (names.length == 0) continue;

            // 处理第一个顶点，获取或分配索引
            int v = getOrAddIndex(names[0]);

            // 处理后面的所有顶点，建立边
            for (int i = 1; i < names.length; i++) {
                int w = getOrAddIndex(names[i]);
                addEdge(v, w);
            }
        }
        br.close();
    }

    // 获取顶点名的索引，如果不存在则新增
    private int getOrAddIndex(String name) {
        if (!st.containsKey(name)) {
            int index = st.size();
            st.put(name, index);
            keys.add(name);
            adj.add(new ArrayList<>()); // 为新顶点初始化邻接表
            return index;
        }
        return st.get(name);
    }

    // 添加边（无向图）
    private void addEdge(int v, int w) {
        adj.get(v).add(w);
        adj.get(w).add(v);
        E++;
    }

    // 顶点数
    public int V() {
        return st.size();
    }

    // 边数
    public int E() {
        return E;
    }

    // 顶点名是否存在
    public boolean contains(String s) {
        return st.containsKey(s);
    }

    // 根据顶点名获取索引
    public int index(String s) {
        return st.get(s);
    }

    // 根据索引获取顶点名
    public String name(int v) {
        return keys.get(v);
    }

    // 获取邻接顶点
    public Iterable<Integer> adj(int v) {
        return adj.get(v);
    }
}