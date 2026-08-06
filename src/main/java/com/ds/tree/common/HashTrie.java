package com.ds.tree.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 哈希字典树(Map 子节点版) —— {@link AbstractTrie} 的哈希表存储实现
 * <p>
 * 子节点用 {@code Map<Character, Node>}，只给实际存在的分支分配内存，
 * 是 {@link LinkedTrie} 定长数组版的空间优化：
 * <ul>
 *     <li>内存占用与已存单词的字符总数成正比，不再与 range 成正比；</li>
 *     <li>没有 range 限制，任意字符(包括中文、Emoji 等)都可以直接存储；</li>
 *     <li>每个字符的定位仍为 O(1) 级别(HashMap 按编码直接散列)。</li>
 * </ul>
 * 本类只实现六个子节点存储原语，增删查、前缀、计数、剪除、遍历等全部复用抽象基类。
 * </p>
 *
 * <p>与原 com.ds.trie.HashTrie 的关系：同一实现思路的泛型/分层化版本。</p>
 */
public class HashTrie extends AbstractTrie {

    /** 默认构造器：无 range 限制，任意字符均可存储 */
    public HashTrie() {
        this.root = newNode((char) 0);
    }

    /** Map 子节点版节点：children 以字符为键，只保存实际存在的分支 */
    private static class MapNode extends AbstractTrie.Node {
        final Map<Character, Node> children = new HashMap<>();

        MapNode(char data) {
            super(data);
        }
    }

    // ==================== 子节点存储原语实现 ====================

    @Override
    protected Node getChild(Node node, char c) {
        return ((MapNode) node).children.get(c);
    }

    @Override
    protected void setChild(Node node, char c, Node child) {
        ((MapNode) node).children.put(c, child);
    }

    @Override
    protected boolean removeChild(Node node, char c) {
        return ((MapNode) node).children.remove(c) != null;
    }

    @Override
    protected boolean isLeaf(Node node) {
        return ((MapNode) node).children.isEmpty();
    }

    @Override
    protected List<Node> childrenOf(Node node) {
        // Map 本身无序，字典序由 AbstractTrie 统一排序
        return new ArrayList<>(((MapNode) node).children.values());
    }

    @Override
    protected Node newNode(char c) {
        return new MapNode(c);
    }
}
