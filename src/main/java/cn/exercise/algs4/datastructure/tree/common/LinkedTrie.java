package cn.exercise.algs4.datastructure.tree.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 链式字典树(定长数组子节点版) —— {@link AbstractTrie} 的数组存储实现
 * <p>
 * 子节点用预分配的 {@code Node[range]} 数组，以字符编码作为下标，定位 O(1)。
 * 每个节点即使只有一个子节点，也占用一整块 range 大小的引用空间——
 * 这是空间换时间的经典取舍；range 需覆盖所有待存储字符的编码。
 * 本类只实现六个子节点存储原语，增删查、前缀、计数、剪除、遍历等全部复用抽象基类。
 * </p>
 *
 * <p>与原 cn.exercise.algs4.datastructure.trie.LinkedTrie 的关系：同一实现思路的泛型/分层化版本。</p>
 */
public class LinkedTrie extends AbstractTrie {

    /** 字符编码取值范围：字符编码须小于 range 才能存储 */
    private final int range;

    /** 默认构造器：字符编码取值空间为 0~255(覆盖全部 ASCII 可打印字符) */
    public LinkedTrie() {
        this(256);
    }

    /**
     * @param range 字符编码取值范围(如要存中文需 65536 以上，此时内存开销大，建议改用 {@link HashTrie})
     */
    public LinkedTrie(int range) {
        if (range <= 0) {
            throw new IllegalArgumentException("range 必须大于 0，实际 " + range);
        }
        this.range = range;
        this.root = newNode((char) 0);
    }

    /** 数组子节点版节点：children 以字符编码为下标 */
    private static class ArrayNode extends AbstractTrie.Node {
        final Node[] children;

        ArrayNode(char data, int range) {
            super(data);
            this.children = new Node[range];
        }
    }

    // ==================== 子节点存储原语实现 ====================

    @Override
    protected Node getChild(Node node, char c) {
        checkRange(c);
        return ((ArrayNode) node).children[c];
    }

    @Override
    protected void setChild(Node node, char c, Node child) {
        checkRange(c);
        ((ArrayNode) node).children[c] = child;
    }

    @Override
    protected boolean removeChild(Node node, char c) {
        checkRange(c);
        ArrayNode n = (ArrayNode) node;
        boolean existed = n.children[c] != null;
        n.children[c] = null;
        return existed;
    }

    @Override
    protected boolean isLeaf(Node node) {
        for (Node child : ((ArrayNode) node).children) {
            if (child != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected List<Node> childrenOf(Node node) {
        List<Node> list = new ArrayList<>();
        for (Node child : ((ArrayNode) node).children) {
            if (child != null) {
                list.add(child);
            }
        }
        return list;
    }

    @Override
    protected Node newNode(char c) {
        checkRange(c);
        return new ArrayNode(c, range);
    }

    /** 校验字符编码在 range 范围内 */
    private void checkRange(char data) {
        if (data >= range) {
            throw new IllegalArgumentException(
                    "字符 '" + data + "' 的编码 " + (int) data + " 超出 range 范围 " + range);
        }
    }
}
