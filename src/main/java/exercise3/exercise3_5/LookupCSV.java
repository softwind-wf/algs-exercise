package exercise3.exercise3_5;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;
import edu.princeton.cs.algs4.ST;
import edu.princeton.cs.algs4.Queue;

public class LookupCSV {
    public static void main(String[] args) {
        // 命令行参数：文件名、键所在列、值所在列
        In in = new In(args[0]);
        int keyField = Integer.parseInt(args[1]);
        int valField = Integer.parseInt(args[2]);

        // 关键修改：符号表的值类型改为 Queue<String>，支持一键多值
        ST<String, Queue<String>> st = new ST<>();

        // 读取CSV文件，构建索引
        while (in.hasNextLine()) {
            String line = in.readLine();
            String[] tokens = line.split(",");

            // 边界检查：确保列数足够
            if (tokens.length <= Math.max(keyField, valField)) {
                continue;
            }

            String key = tokens[keyField];
            String val = tokens[valField];

            // 如果键不存在，先创建一个空队列
            if (!st.contains(key)) {
                st.put(key, new Queue<>());
            }
            // 将值加入队列（而不是覆盖）
            st.get(key).enqueue(val);
        }

        // 接收用户查询，输出该键对应的所有值
        while (!StdIn.isEmpty()) {
            String query = StdIn.readLine().trim();
            if (st.contains(query)) {
                for (String val : st.get(query)) {
                    StdOut.println(val);
                }
            } else {
                StdOut.println("Not found.");
            }
        }
    }
}