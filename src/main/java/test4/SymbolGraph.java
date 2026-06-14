package test4;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SymbolGraph {
    // 符号表：符号名 -> 索引
    private Map<String, Integer> st;
    // 反向索引：索引 -> 符号名
    private String[] keys;
    // 底层的图
    private Graph G;

    public SymbolGraph(String filename, String delim) {
        st = new HashMap<>();

        // 第一遍：读取文件，为每个唯一的符号名分配索引
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] a = line.split(delim);
                for (int i = 0; i < a.length; i++) {
                    if (!st.containsKey(a[i])) {
                        st.put(a[i], st.size());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 构建反向索引数组
        keys = new String[st.size()];
        for (String name : st.keySet()) {
            keys[st.get(name)] = name;
        }

        // 第二遍：读取文件，构建图
        G = new Graph(st.size());
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] a = line.split(delim);
                // 将每行第一个顶点和其他顶点相连
                int v = st.get(a[0]);
                for (int i = 1; i < a.length; i++) {
                    int w = st.get(a[i]);
                    G.addEdge(v, w);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // key 是否是一个顶点
    public boolean contains(String s) {
        return st.containsKey(s);
    }

    // 返回 key 对应的索引
    public int index(String s) {
        return st.get(s);
    }

    // 返回索引 v 对应的顶点名
    public String name(int v) {
        return keys[v];
    }

    // 返回底层的 Graph 对象
    public Graph G() {
        return G;
    }
}