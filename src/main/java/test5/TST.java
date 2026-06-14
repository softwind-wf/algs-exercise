package test5;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TST<Value> implements StringST<Value> {
    private static final int R = 256;          // 字符集大小（ASCII）
    private Node[] roots;                      // 根节点数组，按首字符索引
    private int n;                             // 键的数量

    // 结点类定义（与原相同）
    private static class Node {
        char c;
        Node left, mid, right;
        Object val;                            // 存储值，实际类型为Value
    }

    public TST() {
        roots = new Node[R];
        n = 0;
    }

    @Override
    public StringST<Value> create() {
        return new TST<>();
    }

    @Override
    public int size() {
        return n;
    }

    @Override
    public boolean isEmpty() {
        return n == 0;
    }

    @Override
    public boolean contains(String key) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key不能为空");
        return get(key) != null;
    }

    @Override
    public Value get(String key) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key不能为空");
        char first = key.charAt(0);
        Node x = roots[first];
        if (x == null) return null;
        x= get(x, key, 0);
        if(x==null){
            return null;
        }else{
            return (Value) x.val;
        }
    }

    // 递归查找（与原相同，从第0个字符开始）
    @SuppressWarnings("unchecked")
    private Node get(Node x, String key, int d) {
        if (x == null) return null;
        char c = key.charAt(d);
        if (c < x.c) return get(x.left, key, d);
        else if (c > x.c) return get(x.right, key, d);
        else if (d < key.length() - 1) return get(x.mid, key, d + 1);
        else return  x;
    }

    @Override
    public void put(String key, Value val) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key不能为空");
        if (val == null) {
            delete(key);
            return;
        }
        boolean isNewKey = !contains(key);
        char first = key.charAt(0);
        roots[first] = put(roots[first], key, val, 0);
        if (isNewKey) n++;
    }

    // 递归插入（与原相同）
    @SuppressWarnings("unchecked")
    private Node put(Node x, String key, Value val, int d) {
        char c = key.charAt(d);
        if (x == null) {
            x = new Node();
            x.c = c;
        }
        if (c < x.c) x.left = put(x.left, key, val, d);
        else if (c > x.c) x.right = put(x.right, key, val, d);
        else if (d < key.length() - 1) x.mid = put(x.mid, key, val, d + 1);
        else x.val = val;
        return x;
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key不能为空");
        if (!contains(key)) return;
        char first = key.charAt(0);
        roots[first] = delete(roots[first], key, 0);
        n--;
    }

    // 递归删除（与原相同）
    private Node delete(Node x, String key, int d) {
        if (x == null) return null;
        char c = key.charAt(d);
        if (c < x.c) x.left = delete(x.left, key, d);
        else if (c > x.c) x.right = delete(x.right, key, d);
        else if (d < key.length() - 1) x.mid = delete(x.mid, key, d + 1);
        else x.val = null;

        if (x.val == null && x.left == null && x.mid == null && x.right == null)
            return null;
        return x;
    }

    @Override
    public String longestPrefixOf(String s) {
        if (s == null || s.isEmpty()) return null;
        char first = s.charAt(0);
        Node x = roots[first];
        if (x == null) return "";
        int len = 0;
        int i = 0;
        while (x != null && i < s.length()) {
            char c = s.charAt(i);
            if (c < x.c) x = x.left;
            else if (c > x.c) x = x.right;
            else {
                i++;
                if (x.val != null) len = i;
                x = x.mid;
            }
        }
        return s.substring(0, len);
    }

    @Override
    public Iterable<String> keysWithPrefix(String s) {
        Queue<String> queue = new LinkedList<>();
        if (s == null) return queue;
        if (s.isEmpty()) {
            // 空前缀：收集所有键
            for (int i = 0; i < R; i++) {
                if (roots[i] != null)
                    collect(roots[i], new StringBuilder(), queue);
            }
            return queue;
        }
        char first = s.charAt(0);
        Node x = roots[first];
        if (x == null) return queue;
        Node node = get(x, s, 0);
        if (node == null) return queue;
        if (node.val != null) queue.add(s);
        collect(node.mid, new StringBuilder(s), queue);
        return queue;
    }

    @Override
    public Iterable<String> keys() {
        Queue<String> queue = new LinkedList<>();
        for (int i = 0; i < R; i++) {
            if (roots[i] != null)
                collect(roots[i], new StringBuilder(), queue);
        }
        return queue;
    }

    // 收集以某个节点为根的子树中的所有键（与原相同）
    private void collect(Node x, StringBuilder prefix, Queue<String> queue) {
        if (x == null) return;
        collect(x.left, prefix, queue);
        prefix.append(x.c);
        if (x.val != null) queue.add(prefix.toString());
        collect(x.mid, prefix, queue);
        prefix.deleteCharAt(prefix.length() - 1);
        collect(x.right, prefix, queue);
    }

    @Override
    public Iterable<String> keysThatMatch(String pattern) {
        Queue<String> queue = new LinkedList<>();
        if (pattern == null || pattern.isEmpty()) return queue;
        char first = pattern.charAt(0);
        if (first == '.') {
            // 通配符：遍历所有可能的首字符
            for (int i = 0; i < R; i++) {
                if (roots[i] != null)
                    match(roots[i], new StringBuilder(), pattern, 0, queue);
            }
        } else {
            if (roots[first] != null)
                match(roots[first], new StringBuilder(), pattern, 0, queue);
        }
        return queue;
    }

    // 模式匹配辅助函数（支持 "." 通配符，与原相同）
    private void match(Node x, StringBuilder prefix, String pattern, int d, Queue<String> queue) {
        if (x == null) return;
        char c = pattern.charAt(d);
        if (c == '.' || c < x.c) match(x.left, prefix, pattern, d, queue);
        if (c == '.' || c == x.c) {
            if (d == pattern.length() - 1 && x.val != null) {
                queue.add(prefix.toString() + x.c);
            }
            if (d < pattern.length() - 1) {
                prefix.append(x.c);
                match(x.mid, prefix, pattern, d + 1, queue);
                prefix.deleteCharAt(prefix.length() - 1);
            }
        }
        if (c == '.' || c > x.c) match(x.right, prefix, pattern, d, queue);
    }

    // 测试代码（与原相同，验证混合结构）
    public static void main(String[] args) {
        TST<Integer> st = new TST<>();

        String[] keys = {"she", "sells", "sea", "shells", "by", "the", "shore"};
        for (int i = 0; i < keys.length; i++) {
            st.put(keys[i], i);
        }

        System.out.println("所有键：");
        for (String key : st.keys()) {
            System.out.println(key + " -> " + st.get(key));
        }

        System.out.println("\n前缀为 \"she\" 的键：");
        for (String key : st.keysWithPrefix("she")) {
            System.out.println(key);
        }

        System.out.println("\n\"shellsort\" 的最长前缀：");
        System.out.println(st.longestPrefixOf("shellsort"));

        System.out.println("\n匹配模式 \".ea\" 的键：");
        for (String key : st.keysThatMatch(".ea")) {
            System.out.println(key);
        }

        System.out.println("\n删除 \"sea\" 后：");
        st.delete("sea");
        for (String key : st.keys()) {
            System.out.println(key);
        }
    }
}