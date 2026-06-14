package test5;

import java.util.LinkedList;
import java.util.Queue;

public class TST0<Value> implements StringST<Value> {
    private Node root; // 树的根结点
    private int n;     // 键的数量

    // 结点类定义
    private class Node {
        char c;                     // 字符
        Node left, mid, right;      // 左、中、右三向链接
        Value val;                  // 和字符串相关联的值
    }

    public TST0() {
        root = null;
        n = 0;
    }

    @Override
    public StringST<Value> create() {
        return new TST0<>();
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
        if (key == null || key.length() == 0) throw new IllegalArgumentException("key不能为空");
        return get(key) != null;
    }

    @Override
    public Value get(String key) {
        if (key == null || key.length() == 0) throw new IllegalArgumentException("key不能为空");
        Node x = get(root, key, 0);
        if (x == null) return null;
        return x.val;
    }

    // 递归查找辅助函数
    private Node get(Node x, String key, int d) {
        if (x == null) return null;
        char c = key.charAt(d);
        if (c < x.c) return get(x.left, key, d);
        else if (c > x.c) return get(x.right, key, d);
        else if (d < key.length() - 1) return get(x.mid, key, d + 1);
        else return x;
    }

    @Override
    public void put(String key, Value val) {
        if (key == null || key.length() == 0) throw new IllegalArgumentException("key不能为空");
        if (val == null) {
            delete(key);
            return;
        }
        boolean isNewKey = !contains(key);
        root = put(root, key, val, 0);
        if (isNewKey) n++;
    }

    // 递归插入辅助函数
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
        if (key == null || key.length() == 0) throw new IllegalArgumentException("key不能为空");
        if (!contains(key)) return;
        root = delete(root, key, 0);
        n--;
    }

    // 递归删除辅助函数
    private Node delete(Node x, String key, int d) {
        if (x == null) return null;
        char c = key.charAt(d);
        if (c < x.c) x.left = delete(x.left, key, d);
        else if (c > x.c) x.right = delete(x.right, key, d);
        else if (d < key.length() - 1) x.mid = delete(x.mid, key, d + 1);
        else x.val = null;

        // 如果结点没有值且没有子结点，就删除它
        if (x.val == null && x.left == null && x.mid == null && x.right == null) {
            return null;
        }
        return x;
    }

    @Override
    public String longestPrefixOf(String s) {
        if (s == null || s.length() == 0) return null;
        int len = 0;
        Node x = root;
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
        if (s == null || s.length() == 0) {
            collect(root, new StringBuilder(), queue);
            return queue;
        }
        Node x = get(root, s, 0);
        if (x == null) return queue;
        if (x.val != null) queue.add(s);
        collect(x.mid, new StringBuilder(s), queue);
        return queue;
    }

    @Override
    public Iterable<String> keys() {
        Queue<String> queue = new LinkedList<>();
        collect(root, new StringBuilder(), queue);
        return queue;
    }

    // 收集所有以该结点为根的子树中的键
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
        match(root, new StringBuilder(), pattern, 0, queue);
        return queue;
    }

    // 模式匹配辅助函数（支持 "." 通配符）
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


    public static void main(String[] args) {
        TST0<Integer> st = new TST0<>();

        // 插入测试数据
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
        System.out.println(st.longestPrefixOf("shellsort")); // 预期 shells

        System.out.println("\n匹配模式 \".ea\" 的键：");
        for (String key : st.keysThatMatch(".ea")) {
            System.out.println(key); // 预期 sea
        }

        System.out.println("\n删除 \"sea\" 后：");
        st.delete("sea");
        for (String key : st.keys()) {
            System.out.println(key);
        }
    }
}