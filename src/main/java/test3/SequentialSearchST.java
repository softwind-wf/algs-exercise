package test3;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SequentialSearchST<Key, Value> {
    private Node first; // 链表首结点
    private int n;      // 符号表中键值对的数量

    // 链表结点的定义
    private class Node {
        Key key;
        Value val;
        Node next;

        public Node(Key key, Value val, Node next) {
            this.key = key;
            this.val = val;
            this.next = next;
        }
    }

    // 构造函数：初始化空符号表
    public SequentialSearchST() {
        first = null;
        n = 0;
    }

    // 返回符号表中键值对的数量
    public int size() {
        return n;
    }

    // 判断符号表是否为空
    public boolean isEmpty() {
        return size() == 0;
    }

    // 查找给定的键，返回相关联的值
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to get() is null");
        for (Node x = first; x != null; x = x.next) {
            if (key.equals(x.key)) {
                return x.val; // 命中
            }
        }
        return null; // 未命中
    }

    // 查找给定的键，找到则更新其值，否则在表中新建结点
    public void put(Key key, Value val) {
        if (key == null) throw new IllegalArgumentException("first argument to put() is null");
        if (val == null) {
            delete(key);
            return;
        }

        for (Node x = first; x != null; x = x.next) {
            if (key.equals(x.key)) {
                x.val = val; // 命中，更新
                return;
            }
        }
        first = new Node(key, val, first); // 未命中，新建结点插入表头
        n++;
    }

    // 删除给定键对应的键值对
    public void delete(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to delete() is null");
        if (isEmpty()) return;

        // 特殊情况：要删除的是首结点
        if (key.equals(first.key)) {
            first = first.next;
            n--;
            return;
        }

        // 遍历查找待删除结点的前驱结点
        Node prev = first;
        for (Node x = first.next; x != null; x = x.next) {
            if (key.equals(x.key)) {
                prev.next = x.next; // 跳过待删除结点
                n--;
                return;
            }
            prev = x;
        }
        // 未找到对应键，无需操作
    }

    // 判断符号表中是否包含指定键
    public boolean contains(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to contains() is null");
        return get(key) != null;
    }

    // 核心：不依赖额外数据结构，直接返回自定义 Iterable
    public Iterable<Key> keys() {
        return new Iterable<Key>() {
            @Override
            public Iterator<Key> iterator() {
                return new Iterator<Key>() {
                    private Node current = first;

                    @Override
                    public boolean hasNext() {
                        return current != null;
                    }

                    @Override
                    public Key next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        Key key = current.key;
                        current = current.next;
                        return key;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    // 测试用例
    public static void main(String[] args) {
        SequentialSearchST<String, Integer> st = new SequentialSearchST<>();
        st.put("S", 0);
        st.put("E", 1);
        st.put("A", 2);
        st.put("R", 3);
        st.put("C", 4);
        st.put("H", 5);
        st.put("E", 6);
        st.put("X", 7);
        st.put("A", 8);

        System.out.println("size: " + st.size());
        for (String key : st.keys()) {
            System.out.println(key + " " + st.get(key));
        }

        st.delete("E");
        System.out.println("\nAfter delete E:");
        System.out.println("size: "+st.size());
        for (String key : st.keys()) {
            System.out.println(key + " " + st.get(key));
        }
    }
}