package test5;

public interface StringST<Value> {
    // 创建一个符号表
    StringST<Value> create();

    // 向表中插入键值对（如果值为 null 则删除键 key）
    void put(String key, Value val);

    // key 所对应的值（如果键不存在则返回 null）
    Value get(String key);

    // 删除键 key（和它的值）
    void delete(String key);

    // 表中是否保存着 key 的值
    boolean contains(String key);

    // 符号表是否为空
    boolean isEmpty();

    // s 的前缀中最长的键
    String longestPrefixOf(String s);

    // 所有以 s 为前缀的键
    Iterable<String> keysWithPrefix(String s);

    // 所有和 s 匹配的键（其中 "." 能够匹配任意字符）
    Iterable<String> keysThatMatch(String s);

    // 键值对的数量
    int size();

    // 符号表中的所有键
    Iterable<String> keys();
}