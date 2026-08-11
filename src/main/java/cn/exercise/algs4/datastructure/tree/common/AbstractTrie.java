package cn.exercise.algs4.datastructure.tree.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 字典树 / 前缀树(Trie)抽象基类 —— 树族中"按字符序列组织"的独立分支
 * <p>
 * 与二叉树({@link AbstractBinaryTree})、多路树({@link AbstractMultiWayTree})不同，
 * Trie 的孩子是"按字符编码索引"的分支，且允许同一单词重复插入(计数)——
 * 因此它<b>不并入</b>前两者的继承树，而是自成一支。
 * 但它依然实现统一的 {@link Tree} 接口：以 String 为元素、字典序为自然序，
 * 证明"统一树接口"能覆盖二叉 / 多路 / 前缀三类树。
 * </p>
 * <p>
 * 本类的价值：原仓库的 LinkedTrie(Node[range] 数组)与 HashTrie(Map&lt;Character,Node&gt;)
 * 增删查逻辑逐行相同，只有<b>子节点的存取方式</b>不同。这里把前缀树的全部通用逻辑
 * (增删查、前缀判断/统计、分支剪除、字典序遍历、计数)实现一遍，
 * 子节点存储被抽象成六个原语，由子类各自实现：
 * </p>
 * <ul>
 *     <li>{@link #getChild} / {@link #setChild} / {@link #removeChild}：按字符取 / 设 / 删子节点；</li>
 *     <li>{@link #isLeaf}：判断是否无子节点(删除剪除用)；</li>
 *     <li>{@link #childrenOf}：枚举全部子节点(字典序由本类统一排序，不依赖子类顺序)；</li>
 *     <li>{@link #newNode}：创建子类自己的节点。</li>
 * </ul>
 * <p>
 * 说明：纯二维数组版字典树(编号存储、无对象节点)存储模型差异过大，不属于
 * "对象节点 + 子节点字典"这一族，不在此抽象范围内——正如 B 树(数组孩子)不在二叉树抽象内。
 * </p>
 */
public abstract class AbstractTrie implements Tree<String> {

    /**
     * 前缀树节点：data 为该节点代表的字符(根节点为 0)，
     * count 为以该节点结尾的单词被插入的次数(0 表示此处不是单词结尾)。
     */
    protected static class Node {
        char data;
        int count;

        Node(char data) {
            this.data = data;
        }
    }

    /** 字典树的空根节点(不存储实际字符) */
    protected Node root;

    /** 字典树中不同单词的个数 */
    protected int size;

    /** 字典树中所有单词被插入的总次数(含重复插入) */
    protected int wordCount;

    protected AbstractTrie() {
    }

    // ==================== 子节点存储抽象原语(子类实现) ====================

    /** 按字符取子节点，不存在返回 null */
    protected abstract Node getChild(Node node, char c);

    /** 按字符设置子节点 */
    protected abstract void setChild(Node node, char c, Node child);

    /** 按字符删除子节点，返回是否删除成功 */
    protected abstract boolean removeChild(Node node, char c);

    /** 判断节点是否没有子节点(叶节点) */
    protected abstract boolean isLeaf(Node node);

    /** 枚举节点的全部子节点(顺序不保证，字典序由本类统一排序) */
    protected abstract List<Node> childrenOf(Node node);

    /** 创建子类自己的节点(根节点 data 传 (char) 0) */
    protected abstract Node newNode(char c);

    // ==================== 增删查 ====================

    /**
     * 向字典树中插入一个单词(重复插入会累计插入次数)
     *
     * @param str 待插入的单词
     * @return true 表示该单词首次插入(新增了一个不同单词)；false 表示此前已存在(仅计数增加)
     */
    @Override
    public boolean insert(String str) {
        checkStr(str);
        Node cur = root;
        for (char data : str.toCharArray()) {
            Node child = getChild(cur, data);
            if (child == null) {
                child = newNode(data);
                setChild(cur, data, child);
            }
            cur = child;
        }
        // 该单词此前不存在，不同单词的个数加 1
        if (cur.count == 0) {
            size++;
        }
        cur.count++;
        wordCount++;
        // 是否"新增"了一个不同单词：插入前 count == 0
        return cur.count == 1;
    }

    /**
     * 插入单词(兼容原 LinkedTrie/HashTrie 的 void 语义，内部转调 {@link #insert})
     *
     * @param str 待插入的单词
     */
    public void add(String str) {
        insert(str);
    }

    /**
     * 查询单词被插入的次数
     *
     * @param str 待查询的单词
     * @return 该单词被插入的次数；单词不存在或 str 为 null 返回 0
     */
    public int getCount(String str) {
        if (str == null) {
            return 0;
        }
        Node cur = findNode(str.toCharArray());
        return cur == null ? 0 : cur.count;
    }

    /** 判断字典树中是否存在指定单词(Trie 特有方法,Tree 接口未声明 contains) */
    public boolean contains(String str) {
        return getCount(str) > 0;
    }

    /** 判断指定字符串是否为某个单词的前缀(字符串本身可以不是完整单词) */
    public boolean isPrefix(String prefix) {
        if (prefix == null) {
            return false;
        }
        return findNode(prefix.toCharArray()) != null;
    }

    /** 统计所有以指定字符串为前缀的单词被插入的累计次数 */
    public int countPrefix(String prefix) {
        if (prefix == null) {
            return 0;
        }
        Node cur = findNode(prefix.toCharArray());
        if (cur == null) {
            return 0;
        }
        // 前缀分支下所有节点的 count 之和，即命中该前缀的单词累计次数
        return sumCount(cur);
    }

    /**
     * 从字典树中删除指定单词的一次插入，并自动剪除不再被任何单词使用的分支节点
     *
     * @param str 待删除的单词
     * @return true 删除成功；false 单词不存在
     */
    @Override
    public boolean remove(String str) {
        checkStr(str);
        boolean removed = remove(root, str.toCharArray(), 0);
        if (removed) {
            wordCount--;
        }
        return removed;
    }

    // ==================== 统计信息 ====================

    /** 返回字典树中不同单词的个数 */
    @Override
    public int size() {
        return size;
    }

    /** 返回字典树中所有单词被插入的总次数(含重复) */
    public int wordCount() {
        return wordCount;
    }

    /** 判断字典树是否为空 */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /** 清空字典树 */
    @Override
    public void clear() {
        root = newNode((char) 0);
        size = 0;
        wordCount = 0;
    }

    /**
     * 树高 = 最长单词的字符数(空树 -1；例如仅存 "pen" 时高度为 3)
     */
    @Override
    public int height() {
        if (isEmpty()) {
            return -1;
        }
        return height(root);
    }

    // ==================== 遍历 ====================

    /** 按字典序返回字典树中的全部单词(重复插入的单词只返回一次) */
    public List<String> getAllWords() {
        List<String> words = new ArrayList<>();
        collectWords(root, new StringBuilder(), words);
        return words;
    }

    /** 中序遍历 = 字典序全部单词(与 {@link #getAllWords()} 一致) */
    @Override
    public List<String> inorder() {
        return getAllWords();
    }

    /** 层序遍历：按 BFS 输出每个节点的字符(虚拟根不输出) */
    @Override
    public List<String> levelOrder() {
        List<String> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            if (cur.data != (char) 0) {
                result.add(String.valueOf(cur.data));
            }
            for (Node child : childrenOf(cur)) {
                queue.offer(child);
            }
        }
        return result;
    }

    /** 返回按字典序迭代全部单词的迭代器 */
    @Override
    public Iterator<String> iterator() {
        return getAllWords().iterator();
    }

    /** 以字符树的形式输出整棵字典树的结构 */
    public void printTrie() {
        StringBuilder sb = new StringBuilder();
        printTrie(root, sb, "");
        System.out.print(sb);
    }

    @Override
    public String toString() {
        return getAllWords().toString();
    }

    // ==================== 内部方法 ====================

    /** 沿单词路径向下找到对应的节点；路径中断返回 null */
    private Node findNode(char[] chars) {
        Node cur = root;
        for (char data : chars) {
            cur = getChild(cur, data);
            if (cur == null) {
                return null;
            }
        }
        return cur;
    }

    /** 递归删除单词的一次插入，返回是否删除成功 */
    private boolean remove(Node node, char[] chars, int depth) {
        if (node == null) {
            return false;
        }
        if (depth == chars.length) {
            if (node.count == 0) {
                return false;
            }
            // 该单词最后一次被删除，不同单词的个数减 1
            if (--node.count == 0) {
                size--;
            }
            return true;
        }
        char data = chars[depth];
        Node child = getChild(node, data);
        boolean removed = remove(child, chars, depth + 1);
        // 删除成功后，若该子节点不再被任何单词使用(无结尾且无子节点)，则剪除该分支
        if (removed && child != null && child.count == 0 && isLeaf(child)) {
            removeChild(node, data);
        }
        return removed;
    }

    /** 递归收集所有以 count>0 节点结尾的单词路径(子节点统一按字符排序保证字典序) */
    private void collectWords(Node node, StringBuilder sb, List<String> words) {
        if (node == null) {
            return;
        }
        if (node.count > 0) {
            words.add(sb.toString());
        }
        List<Node> children = childrenOf(node);
        children.sort(Comparator.comparingInt(n -> n.data));
        for (Node child : children) {
            sb.append(child.data);
            collectWords(child, sb, words);
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    /** 递归累加节点及其全部子孙节点的 count 值 */
    private int sumCount(Node node) {
        if (node == null) {
            return 0;
        }
        int sum = node.count;
        for (Node child : childrenOf(node)) {
            sum += sumCount(child);
        }
        return sum;
    }

    /** 递归输出整棵字典树的结构 */
    private void printTrie(Node node, StringBuilder sb, String prefix) {
        if (node == null) {
            return;
        }
        sb.append(prefix);
        if (node.data == (char) 0) {
            sb.append("(root)");
        } else {
            sb.append(node.data);
            if (node.count > 0) {
                sb.append("  x").append(node.count);
            }
        }
        sb.append('\n');
        List<Node> children = childrenOf(node);
        children.sort(Comparator.comparingInt(n -> n.data));
        for (Node child : children) {
            printTrie(child, sb, prefix + "  ");
        }
    }

    /** 递归计算以 node 为根的子树高度(叶节点为 0) */
    private int height(Node node) {
        int max = -1;
        for (Node child : childrenOf(node)) {
            max = Math.max(max, height(child));
        }
        return max + 1;
    }

    /** 校验单词字符串非 null(允许为空串) */
    private void checkStr(String str) {
        if (str == null) {
            throw new IllegalArgumentException("插入或删除的单词不能为null");
        }
    }
}
