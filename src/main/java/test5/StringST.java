package test5;

import java.util.*;

/**
 * 以字符串为键的符号表（单词查找树/Trie 实现）
 * @param <Value> 存储的值类型
 */
public class StringST<Value> {

    private static class Node<Value> {
        private Value value;
        private Node<Value>[] next;

        @SuppressWarnings("unchecked")
        Node(int alphabetSize) {
            next = new Node[alphabetSize];
        }
    }

    // 默认使用 256 扩展 ASCII 字母表
    private static final int R = 256;
    private Node<Value> root;
    private int size;

    public StringST() {
        root = new Node<>(R);
        size = 0;
    }

    /**
     * 向符号表中插入键值对
     * @param key 字符串键
     * @param val 值，如果为 null 表示删除该键
     */
    public void put(String key, Value val) {
        if (key == null) throw new IllegalArgumentException("key 不能为 null");
        if (val == null) {
            delete(key);
            return;
        }
        root = put(root, key, val, 0);
    }

    private Node<Value> put(Node<Value> node, String key, Value val, int d) {
        if (node == null) {
            node = new Node<>(R);
        }
        if (d == key.length()) {
            if (node.value == null) size++;
            node.value = val;
            return node;
        }
        char c = key.charAt(d);
        node.next[c] = put(node.next[c], key, val, d + 1);
        return node;
    }

    /**
     * 获取键 key 对应的值
     * @param key 字符串键
     * @return key 对应的值，不存在返回 null
     */
    public Value get(String key) {
        if (key == null) throw new IllegalArgumentException("key 不能为 null");
        Node<Value> node = get(root, key, 0);
        return node == null ? null : node.value;
    }

    private Node<Value> get(Node<Value> node, String key, int d) {
        if (node == null) return null;
        if (d == key.length()) return node;
        char c = key.charAt(d);
        return get(node.next[c], key, d + 1);
    }

    /**
     * 删除键 key（及其对应的值）
     * @param key 字符串键
     */
    public void delete(String key) {
        if (key == null) throw new IllegalArgumentException("key 不能为 null");
        root = delete(root, key, 0);
    }

    private Node<Value> delete(Node<Value> node, String key, int d) {
        if (node == null) return null;
        if (d == key.length()) {
            if (node.value != null) size--;
            node.value = null;
        } else {
            char c = key.charAt(d);
            node.next[c] = delete(node.next[c], key, d + 1);
        }

        // 如果当前节点没有 value，也没有子节点，则可以删除
        if (node.value != null) return node;
        for (int c = 0; c < R; c++) {
            if (node.next[c] != null) return node;
        }
        return null;
    }

    /**
     * 符号表中是否保存 key
     * @param key 字符串键
     * @return 存在返回 true
     */
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * 符号表是否为空
     * @return 空返回 true
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 返回符号表中键的数量
     * @return 键的数量
     */
    public int size() {
        return size;
    }

    // --------------------- 核心扩展方法（截图里的 3 个新方法） ---------------------

    /**
     * 找到 s 的前缀中，在符号表里存在的、最长的那个键
     * @param s 字符串
     * @return 最长前缀键，不存在返回 null
     */
    public String longestPrefixOf(String s) {
        if (s == null) throw new IllegalArgumentException("s 不能为 null");
        int length = longestPrefixOf(root, s, 0, 0);
        return s.substring(0, length);
    }

    private int longestPrefixOf(Node<Value> node, String s, int d, int length) {
        if (node == null) return length;
        if (node.value != null) length = d;
        if (d == s.length()) return length;
        char c = s.charAt(d);
        return longestPrefixOf(node.next[c], s, d + 1, length);
    }

    /**
     * 返回所有以 s 为前缀的键
     * @param s 前缀字符串
     * @return 所有匹配的键的迭代器
     */
    public Iterable<String> keysWithPrefix(String s) {
        Queue<String> queue = new LinkedList<>();
        Node<Value> node = get(root, s, 0);
        collect(node, new StringBuilder(s), queue);
        return queue;
    }

    /**
     * 返回所有和模式 s 匹配的键（用 "." 匹配任意单个字符）
     * @param s 模式字符串，可含 "." 通配符
     * @return 所有匹配的键的迭代器
     */
    public Iterable<String> keysThatMatch(String s) {
        Queue<String> queue = new LinkedList<>();
        collect(root, new StringBuilder(), 0, s, queue);
        return queue;
    }

    // --------------------- 辅助方法 ---------------------

    private void collect(Node<Value> node, StringBuilder prefix, Queue<String> queue) {
        if (node == null) return;
        if (node.value != null) queue.add(prefix.toString());
        for (int c = 0; c < R; c++) {
            prefix.append((char) c);
            collect(node.next[c], prefix, queue);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    private void collect(Node<Value> node, StringBuilder prefix, int d, String pattern, Queue<String> queue) {
        if (node == null) return;
        if (d == pattern.length()) {
            if (node.value != null) queue.add(prefix.toString());
            return;
        }
        char c = pattern.charAt(d);
        if (c == '.') {
            for (int ch = 0; ch < R; ch++) {
                prefix.append((char) ch);
                collect(node.next[ch], prefix, d + 1, pattern, queue);
                prefix.deleteCharAt(prefix.length() - 1);
            }
        } else {
            prefix.append(c);
            collect(node.next[c], prefix, d + 1, pattern, queue);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    /**
     * 返回符号表中的所有键
     * @return 所有键的迭代器
     */
    public Iterable<String> keys() {
        return keysWithPrefix("");
    }

    // --------------------- 测试用例（和截图示例对应） ---------------------
    public static void main(String[] args) {
        StringST<Integer> st = new StringST<>();

        // 插入示例数据：和你截图的示例一致
        st.put("she", 0);
        st.put("sells", 1);
        st.put("sea", 2);
        st.put("shells", 3);
        st.put("by", 4);
        st.put("the", 5);
        st.put("shore", 6);

        System.out.println("所有键：");
        for (String key : st.keys()) {
            System.out.println(key + " -> " + st.get(key));
        }

        System.out.println("\nlongestPrefixOf(\"shell\") = " + st.longestPrefixOf("shell")); // 输出 she
        System.out.println("longestPrefixOf(\"shells\") = " + st.longestPrefixOf("shells")); // 输出 shells

        System.out.println("\nkeysWithPrefix(\"sh\")：");
        for (String key : st.keysWithPrefix("sh")) {
            System.out.println(key);
        }

        System.out.println("\nkeysThatMatch(\"s..\")：");
        for (String key : st.keysThatMatch("s..")) {
            System.out.println(key);
        }
    }
}