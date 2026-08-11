package cn.exercise.algs4.datastructure.trie;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典树 / 前缀树 (Trie) —— 链式树结构实现
 * <p>
 * 子节点以字符的编码值作为数组下标进行索引，插入/查询时间复杂度均为 O(单词长度)，
 * 与字典树中已存单词的个数无关。
 * 支持单词的增、删、查、前缀匹配、计数统计、全部单词遍历等常用操作。
 * </p>
 * <p>
 * 注意：range 表示字符编码的取值空间，只有编码值小于 range 的字符才能被存储；
 * 若要支持中文等字符，需要相应扩大 range（例如 65536）。
 * </p>
 */
public class LinkedTrie {

    //通过静态内部类定义字典树的节点类型
    private static class Node {
        //数据域：当前节点所代表的字符
        char data;
        //以当前节点为结束节点的单词被插入的次数（0 表示此处不是某个单词的结尾）
        int count;
        //子节点指针域：以下一个字符的编码值为下标
        Node[] children;

        public Node(char data, int range) {
            this.data = data;
            this.children = new Node[range];
        }

        //判断当前节点是否没有子节点（叶节点）
        boolean isLeaf() {
            for (Node child : this.children) {
                if (child != null) {
                    return false;
                }
            }
            return true;
        }
    }

    //字典树的空根节点（不存储实际字符，编码为0）
    private Node root;
    //字典树中所有字符可能的编码取值范围（字符编码须小于 range）
    private int range;
    //字典树中不同单词的个数
    private int size;
    //字典树中所有单词被插入的总次数（含重复插入）
    private int wordCount;

    //字典树的构造器
    public LinkedTrie(int range) {
        this.range = range;
        //实例化字典树的空根节点
        this.root = new Node((char) 0, range);
    }

    //默认构造器：字符编码取值空间为 0~255（覆盖全部 ASCII 可打印字符）
    public LinkedTrie() {
        this(256);
    }

    //向字典树中插入一个单词的方法（重复插入会累计插入次数）
    public void add(String str) {
        checkStr(str);
        //将单词字符串转换为字符数组
        char[] chars = str.toCharArray();
        //从空根节点开始逐步向下遍历字典树
        Node cur = this.root;
        for (char data : chars) {
            checkRange(data);
            //在遍历过程中遇到某前缀不存在的后续分支
            if (cur.children[data] == null) {
                //创建新节点保存当前字符
                Node node = new Node(data, this.range);
                //将当前字符作为前缀的一个子节点进行挂载
                cur.children[data] = node;
            }
            //继续向后遍历或添加节点
            cur = cur.children[data];
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
        char[] chars = str.toCharArray();
        //从空根节点向下按照单词中的字符遍历字典树的一个分支
        Node cur = findNode(chars);
        //完成遍历的情况，返回该单词被插入的次数；路径中断返回0
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
        this.root = new Node((char) 0, this.range);
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
            if (data >= this.range || cur.children[data] == null) {
                return null;
            }
            cur = cur.children[data];
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
        if (data >= this.range) {
            return false;
        }
        Node child = node.children[data];
        boolean removed = remove(child, chars, depth + 1);
        //删除成功后，若该子节点不再被任何单词使用（无结尾且无子节点），则剪除该分支
        if (removed && child != null && child.count == 0 && child.isLeaf()) {
            node.children[data] = null;
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
        //按子节点数组下标递增的顺序遍历，保证输出有序
        for (Node child : node.children) {
            if (child != null) {
                sb.append(child.data);
                collectWords(child, sb, words);
                sb.deleteCharAt(sb.length() - 1);
            }
        }
    }

    //递归累加节点及其全部子孙节点的count值
    private int sumCount(Node node) {
        if (node == null) {
            return 0;
        }
        int sum = node.count;
        for (Node child : node.children) {
            if (child != null) {
                sum += sumCount(child);
            }
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
        for (Node child : node.children) {
            if (child != null) {
                printTrie(child, sb, prefix + "  ");
            }
        }
    }

    //校验单词字符串非null（允许为空串）
    private void checkStr(String str) {
        if (str == null) {
            throw new IllegalArgumentException("插入或删除的单词不能为null");
        }
    }

    //校验字符编码在range范围内
    private void checkRange(char data) {
        if (data >= this.range) {
            throw new IllegalArgumentException(
                "字符 '" + data + "' 的编码 " + (int) data + " 超出range范围 " + this.range);
        }
    }

    //==================== 测试示例 ====================
    public static void main(String[] args) {
        //ASCII可打印字符最大编码'z'=122，范围设123
        LinkedTrie trie = new LinkedTrie(123);
        trie.add("pencil");
        trie.add("pen");
        trie.add("pencil");
        trie.add("pencil");

        System.out.println("pencil插入次数：" + trie.getCount("pencil")); //输出3
        System.out.println("pen插入次数：" + trie.getCount("pen"));       //输出1
        System.out.println("abc插入次数：" + trie.getCount("abc"));       //输出0
        System.out.println("包含pen：" + trie.contains("pen"));            //输出true
        System.out.println("包含pe：" + trie.contains("pe"));              //输出false（pe只是前缀，不是完整单词）
        System.out.println("pe是否为前缀：" + trie.isPrefix("pe"));         //输出true
        System.out.println("前缀p的单词累计次数：" + trie.countPrefix("p"));  //输出4
        System.out.println("不同单词个数：" + trie.size());                  //输出2
        System.out.println("累计插入次数：" + trie.wordCount());             //输出4
        System.out.println("全部单词：" + trie.getAllWords());               //输出[pen, pencil]

        //删除测试
        System.out.println("删除pen：" + trie.remove("pen"));                //输出true
        System.out.println("删除后pen插入次数：" + trie.getCount("pen"));     //输出0
        System.out.println("删除后pencil插入次数：" + trie.getCount("pencil"));//输出3
        System.out.println("删除不存在的单词abc：" + trie.remove("abc"));     //输出false
        System.out.println("删除后不同单词个数：" + trie.size());              //输出1

        //打印树结构
        System.out.println("----- 字典树结构 -----");
        trie.printTrie();
    }
}
