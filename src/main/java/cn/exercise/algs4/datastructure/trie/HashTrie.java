package cn.exercise.algs4.datastructure.trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典树 / 前缀树 (Trie) —— 哈希表子节点版（数组版的空间优化）
 * <p>
 * 数组版字典树的子节点使用预分配的 Node[range] 数组，每个节点都要开辟一整块
 * range 大小的空间，而绝大多数槽位是 null，非常浪费内存（例如 range=123 时，
 * 每个节点即使只有一个子节点，也要占 123 个引用）。
 * 本类将子节点改为 Map&lt;Character, Node&gt;，只给实际存在的分支分配内存。
 * </p>
 * <p>
 * 空间优化带来的额外好处：
 * <ul>
 *     <li>内存占用与已存单词的字符总数成正比，不再与 range 成正比；</li>
 *     <li>没有 range 限制，任意字符（包括中文、Emoji 等）都可以直接存储；</li>
 *     <li>每个字符的定位仍为 O(1) 级别（HashMap 根据编码直接散列）。</li>
 * </ul>
 * 支持单词的增、删、查、前缀匹配、计数统计、全部单词遍历等常用操作。
 * </p>
 * <p>
 * 说明：本类文件名为 ArrayTrie，但实现已用 Map 取代预分配数组；
 * 若希望类名更贴切，可重命名为 HashTrie / MapTrie。
 * </p>
 */
public class HashTrie {

    //通过静态内部类定义字典树的节点类型
    private static class Node {
        //数据域：当前节点所代表的字符
        char data;
        //以当前节点为结束节点的单词被插入的次数（0 表示此处不是某个单词的结尾）
        int count;
        //子节点指针域：键为下一个字符，只保存实际存在的分支
        Map<Character, Node> children;

        public Node(char data) {
            this.data = data;
            this.children = new HashMap<>();
        }

        //判断当前节点是否没有子节点（叶节点）
        boolean isLeaf() {
            return this.children.isEmpty();
        }
    }

    //字典树的空根节点（不存储实际字符，data 为0）
    private Node root;
    //字典树中不同单词的个数
    private int size;
    //字典树中所有单词被插入的总次数（含重复插入）
    private int wordCount;

    //字典树的构造器
    public HashTrie() {
        //实例化字典树的空根节点
        this.root = new Node((char) 0);
    }

    //向字典树中插入一个单词的方法（重复插入会累计插入次数）
    public void add(String str) {
        checkStr(str);
        //从空根节点开始逐步向下遍历字典树
        Node cur = this.root;
        for (char data : str.toCharArray()) {
            //遇到某前缀不存在的后续分支时，创建新节点并挂载
            if (cur.children.get(data) == null) {
                cur.children.put(data, new Node(data));
            }
            //继续向后遍历
            cur = cur.children.get(data);
        }
        //该单词此前不存在，不同单词的个数加1
        if (cur.count == 0) {
            this.size++;
        }
        //以当前节点为结束的单词插入次数自增1
        cur.count++;
        //累计插入总次数加1
        this.wordCount++;
    }

    //从字典树中检索单词插入次数的方法
    public int getCount(String str) {
        if (str == null) {
            return 0;
        }
        Node cur = findNode(str.toCharArray());
        //路径中断返回0，否则返回该单词被插入的次数
        return cur == null ? 0 : cur.count;
    }

    //判断字典树中是否存在指定单词
    public boolean contains(String str) {
        return getCount(str) > 0;
    }

    //判断指定字符串是否为某个单词的前缀（字符串本身可以不是完整单词）
    public boolean isPrefix(String prefix) {
        if (prefix == null) {
            return false;
        }
        return findNode(prefix.toCharArray()) != null;
    }

    //统计所有以指定字符串为前缀的单词被插入的累计次数
    public int countPrefix(String prefix) {
        if (prefix == null) {
            return 0;
        }
        Node cur = findNode(prefix.toCharArray());
        if (cur == null) {
            return 0;
        }
        //前缀分支下所有节点的count之和，即命中该前缀的单词累计次数
        return sumCount(cur);
    }

    //从字典树中删除指定单词的一次插入，并自动剪除不再被任何单词使用的分支节点
    public boolean remove(String str) {
        checkStr(str);
        boolean removed = remove(this.root, str.toCharArray(), 0);
        if (removed) {
            this.wordCount--;
        }
        return removed;
    }

    //返回字典树中不同单词的个数
    public int size() {
        return this.size;
    }

    //返回字典树中所有单词被插入的总次数（含重复）
    public int wordCount() {
        return this.wordCount;
    }

    //判断字典树是否为空
    public boolean isEmpty() {
        return this.size == 0;
    }

    //清空字典树
    public void clear() {
        this.root = new Node((char) 0);
        this.size = 0;
        this.wordCount = 0;
    }

    //按字典序返回字典树中的全部单词（重复插入的单词只返回一次）
    public List<String> getAllWords() {
        List<String> words = new ArrayList<>();
        //空根节点本身不构成单词，从子节点开始收集
        collectWords(this.root, new StringBuilder(), words);
        return words;
    }

    //以字符树的形式输出整棵字典树的结构
    public void printTrie() {
        StringBuilder sb = new StringBuilder();
        printTrie(this.root, sb, "");
        System.out.print(sb);
    }

    @Override
    public String toString() {
        return getAllWords().toString();
    }

    //==================== 私有辅助方法 ====================

    //沿单词路径向下找到对应的节点；路径中断返回 null
    private Node findNode(char[] chars) {
        Node cur = this.root;
        for (char data : chars) {
            cur = cur.children.get(data);
            if (cur == null) {
                return null;
            }
        }
        return cur;
    }

    //递归删除单词的一次插入，返回是否删除成功
    private boolean remove(Node node, char[] chars, int depth) {
        //路径中断，单词不存在
        if (node == null) {
            return false;
        }
        //已经沿路径走到单词的结尾节点
        if (depth == chars.length) {
            if (node.count == 0) {
                return false;
            }
            //该单词最后一次被删除，不同单词的个数减1
            if (--node.count == 0) {
                this.size--;
            }
            return true;
        }
        char data = chars[depth];
        Node child = node.children.get(data);
        boolean removed = remove(child, chars, depth + 1);
        //删除成功后，若该子节点不再被任何单词使用（无结尾且无子节点），则剪除该分支
        if (removed && child != null && child.count == 0 && child.isLeaf()) {
            node.children.remove(data);
        }
        return removed;
    }

    //递归收集所有以 count>0 节点结尾的单词路径
    private void collectWords(Node node, StringBuilder sb, List<String> words) {
        if (node == null) {
            return;
        }
        //当前节点是某个单词的结尾，收集整条路径
        if (node.count > 0) {
            words.add(sb.toString());
        }
        //对子节点按键排序后再遍历，保证输出字典序（HashMap本身无序）
        List<Character> keys = new ArrayList<>(node.children.keySet());
        Collections.sort(keys);
        for (char data : keys) {
            sb.append(data);
            collectWords(node.children.get(data), sb, words);
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    //递归累加节点及其全部子孙节点的count值
    private int sumCount(Node node) {
        if (node == null) {
            return 0;
        }
        int sum = node.count;
        for (Node child : node.children.values()) {
            sum += sumCount(child);
        }
        return sum;
    }

    //递归输出整棵字典树的结构
    private void printTrie(Node node, StringBuilder sb, String prefix) {
        if (node == null) {
            return;
        }
        sb.append(prefix);
        //空根节点特殊标记
        if (node.data == (char) 0) {
            sb.append("(root)");
        } else {
            sb.append(node.data);
            //标注单词结尾及其插入次数
            if (node.count > 0) {
                sb.append("  x").append(node.count);
            }
        }
        sb.append('\n');
        //对子节点按键排序后再遍历，保证输出稳定有序
        List<Character> keys = new ArrayList<>(node.children.keySet());
        Collections.sort(keys);
        for (char data : keys) {
            printTrie(node.children.get(data), sb, prefix + "  ");
        }
    }

    //校验单词字符串非null（允许为空串）
    private void checkStr(String str) {
        if (str == null) {
            throw new IllegalArgumentException("插入或删除的单词不能为null");
        }
    }

    //==================== 测试示例 ====================
    public static void main(String[] args) {
        HashTrie trie = new HashTrie();
        trie.add("pencil");
        trie.add("pen");
        trie.add("pencil");
        trie.add("pencil");
        //没有range限制，可以直接存中文
        trie.add("苹果");
        trie.add("苹果派");

        System.out.println("pencil插入次数：" + trie.getCount("pencil"));   //输出3
        System.out.println("pen插入次数：" + trie.getCount("pen"));         //输出1
        System.out.println("苹果插入次数：" + trie.getCount("苹果"));         //输出1
        System.out.println("包含pe：" + trie.contains("pe"));                //输出false（pe只是前缀）
        System.out.println("pe是否为前缀：" + trie.isPrefix("pe"));           //输出true
        System.out.println("前缀苹果的单词累计次数：" + trie.countPrefix("苹果")); //输出2
        System.out.println("不同单词个数：" + trie.size());                    //输出4
        System.out.println("累计插入次数：" + trie.wordCount());               //输出6
        System.out.println("全部单词：" + trie.getAllWords());                 //输出[pen, pencil, 苹果, 苹果派]

        //删除测试
        System.out.println("删除pen：" + trie.remove("pen"));                  //输出true
        System.out.println("删除后pen插入次数：" + trie.getCount("pen"));       //输出0
        System.out.println("删除后pencil插入次数：" + trie.getCount("pencil")); //输出3
        System.out.println("删除不存在的单词abc：" + trie.remove("abc"));       //输出false
        System.out.println("删除后不同单词个数：" + trie.size());                //输出3

        //打印树结构
        System.out.println("----- 字典树结构 -----");
        trie.printTrie();
    }
}
