package com.ds.trie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chapter6_Tree
 * ArrayTrie.java
 * 通过二维数组实现的字典树（纯数组版）
 * <p>
 * 不再使用"对象 + 子节点引用"的链表式结构，而是用一张二维表存储整棵树：
 * path[节点编号][字符编码] = 子节点编号，值为0代表该分支不存在；
 * count[节点编号] 记录以该节点结尾的单词插入次数。根节点编号固定为0。
 * </p>
 * <p>
 * 相比链表式字典树，纯数组版避免了为每个节点分配数组/对象的开销，
 * 且节点可以顺序扫描，常用于算法竞赛等高性能场景。
 * </p>
 * <p>
 * 在经典实现基础上的优化与完善：
 * <ul>
 *     <li>动态扩容：path/count 数组不再依赖预先估算的长度，容量不足时自动翻倍扩容；</li>
 *     <li>节点回收：删除单词时，被剪除的分支节点编号压入空闲栈，后续插入优先复用，避免数组无限增长；</li>
 *     <li>字符越界校验：字符编码超过 range 时抛出明确异常，而不是数组越界异常；</li>
 *     <li>功能补全：删除、包含判断、前缀判断/统计、单词遍历、节点数统计等常用操作。</li>
 * </ul>
 * </p>
 * <p>
 * 注意：range 需覆盖所有待存储字符的编码（存中文需 range 至少 65536，内存开销较大，
 * 此时建议改用哈希表子节点版字典树）。
 * </p>
 */
public class ArrayTrie {

    /** 数组初始最小容量 */
    private static final int DEFAULT_CAPACITY = 8;

    //路径数组：path[当前节点编号][字符编码] = 子节点编号，值为0代表不存在子节点
    int[][] path;
    //单词插入次数统计数组：count[节点编号]记录以该节点结尾的单词插入次数
    int[] count;
    //节点编号全局自增变量，根节点固定编号0
    int id;
    //空闲节点编号栈：回收的节点编号压入此栈，后续插入优先复用
    int[] freeStack;
    //空闲栈栈顶指针，-1代表栈为空
    int freeTop;
    //字符编码取值范围
    int range;
    //path/count/freeStack 当前容量
    int capacity;
    //字典树中不同单词的个数
    int size;
    //字典树中所有单词被插入的总次数（含重复插入）
    int wordCount;

    /**
     * 构造方法
     * @param length 初始容量预估（不够会自动扩容，可传0）
     * @param range 字符编码取值范围
     */
    public ArrayTrie(int length, int range) {
        if (range <= 0) {
            throw new IllegalArgumentException("range必须大于0");
        }
        this.range = range;
        //初始容量取预估节点数与默认最小值中的较大者
        int initCap = Math.max(DEFAULT_CAPACITY, length + 1);
        this.path = new int[initCap][range];
        this.count = new int[initCap];
        this.freeStack = new int[initCap];
        this.capacity = initCap;
        this.id = 0;      //空根节点编号固定为0
        this.freeTop = -1; //空闲栈为空
        this.size = 0;
        this.wordCount = 0;
    }

    /** 构造方法：初始容量为默认值 */
    public ArrayTrie(int range) {
        this(0, range);
    }

    /** 默认构造方法：字符编码取值范围 0~255（覆盖全部ASCII可打印字符） */
    public ArrayTrie() {
        this(0, 256);
    }

    /**
     * 向字典树插入一个单词
     * @param str 待插入单词
     */
    public void add(String str) {
        checkStr(str);
        char[] chars = str.toCharArray();
        //p记录当前遍历所在节点编号，初始从根节点0出发
        int p = 0;
        for (char data : chars) {
            checkRange(data);
            //当前节点p沿着字符data没有后继节点，需要新建节点
            if (path[p][data] == 0) {
                path[p][data] = newNode();
            }
            //移动指针到下一个子节点
            p = path[p][data];
        }
        //该单词此前不存在，不同单词的个数加1
        if (count[p] == 0) {
            this.size++;
        }
        //遍历完单词所有字符，对应结尾节点计数+1
        count[p]++;
        //累计插入总次数加1
        this.wordCount++;
    }

    /**
     * 查询单词被插入的次数
     * @param str 待查询单词
     * @return 插入次数，单词不存在返回0
     */
    public int getCount(String str) {
        if (str == null) {
            return 0;
        }
        int p = findNode(str);
        //路径中断返回0，否则返回对应计数
        return p == -1 ? 0 : count[p];
    }

    /** 判断字典树中是否存在指定单词 */
    public boolean contains(String str) {
        return getCount(str) > 0;
    }

    /** 判断指定字符串是否为某个单词的前缀（字符串本身可以不是完整单词） */
    public boolean isPrefix(String prefix) {
        if (prefix == null) {
            return false;
        }
        return findNode(prefix) != -1;
    }

    /** 统计所有以指定字符串为前缀的单词被插入的累计次数 */
    public int countPrefix(String prefix) {
        if (prefix == null) {
            return 0;
        }
        int p = findNode(prefix);
        if (p == -1) {
            return 0;
        }
        //前缀分支下所有节点的count之和，即命中该前缀的单词累计次数
        return sumCount(p);
    }

    /**
     * 从字典树中删除指定单词的一次插入
     * <p>删除后若某节点不再被任何单词使用（无结尾且无子节点），
     * 其编号会被压入空闲栈，供后续插入复用。</p>
     * @param str 待删除单词
     * @return true 删除成功；false 单词不存在
     */
    public boolean remove(String str) {
        checkStr(str);
        boolean removed = remove(0, str.toCharArray(), 0);
        if (removed) {
            this.wordCount--;
        }
        return removed;
    }

    /** 返回字典树中不同单词的个数 */
    public int size() {
        return this.size;
    }

    /** 返回字典树中所有单词被插入的总次数（含重复） */
    public int wordCount() {
        return this.wordCount;
    }

    /** 返回当前"存活"的节点个数（含根节点），可用来观察节点的分配与回收 */
    public int nodeCount() {
        return this.id - this.freeTop;
    }

    /** 判断字典树是否为空 */
    public boolean isEmpty() {
        return this.size == 0;
    }

    /** 清空字典树（根节点编号0保留） */
    public void clear() {
        //根节点所在行清零即可，其余节点之后会通过newNode重新覆盖
        Arrays.fill(this.path[0], 0);
        this.count[0] = 0;
        this.id = 0;
        this.freeTop = -1;
        this.size = 0;
        this.wordCount = 0;
    }

    /** 按字典序返回字典树中的全部单词（重复插入的单词只返回一次） */
    public List<String> getAllWords() {
        List<String> words = new ArrayList<>();
        //空根节点本身不构成单词，从子节点开始收集
        collectWords(0, new StringBuilder(), words);
        return words;
    }

    /** 以字符树的形式输出整棵字典树的结构（括号内为节点编号，便于观察节点回收与复用） */
    public void printTrie() {
        StringBuilder sb = new StringBuilder();
        printTrie(0, (char) 0, sb, "");
        System.out.print(sb);
    }

    @Override
    public String toString() {
        return getAllWords().toString();
    }

    //==================== 私有辅助方法 ====================

    //沿单词路径向下找到对应的节点编号；路径中断或字符越界返回 -1
    private int findNode(String str) {
        int p = 0;
        for (char data : str.toCharArray()) {
            if (data >= this.range) {
                return -1;
            }
            int child = path[p][data];
            if (child == 0) {
                return -1;
            }
            p = child;
        }
        return p;
    }

    //分配一个新节点：优先复用空闲栈中的编号，否则申请新编号（自动扩容）
    private int newNode() {
        int nodeId;
        if (freeTop >= 0) {
            //复用回收的节点编号
            nodeId = freeStack[freeTop--];
        } else {
            //申请新编号，并确保容量足够
            nodeId = ++id;
            ensureCapacity(id + 1);
        }
        //新节点所在行必须清零（可能是扩容新行，也可能是clear后复用的旧行）
        Arrays.fill(path[nodeId], 0);
        count[nodeId] = 0;
        return nodeId;
    }

    //确保数组容量足够容纳 minCapacity 个节点
    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= this.capacity) {
            return;
        }
        int newCapacity = Math.max(minCapacity, Math.max(this.capacity * 2, DEFAULT_CAPACITY));
        //path：先扩容引用数组，再为新增的行分配内存（新行自动全0）
        this.path = Arrays.copyOf(this.path, newCapacity);
        for (int i = this.capacity; i < newCapacity; i++) {
            this.path[i] = new int[this.range];
        }
        this.count = Arrays.copyOf(this.count, newCapacity);
        this.freeStack = Arrays.copyOf(this.freeStack, newCapacity);
        this.capacity = newCapacity;
    }

    //回收节点编号，压入空闲栈以便后续插入复用
    private void recycle(int p) {
        freeStack[++freeTop] = p;
    }

    //递归删除单词的一次插入，返回是否删除成功
    private boolean remove(int p, char[] chars, int depth) {
        //已经沿路径走到单词的结尾节点
        if (depth == chars.length) {
            if (count[p] == 0) {
                return false;
            }
            //该单词最后一次被删除，不同单词的个数减1
            if (--count[p] == 0) {
                this.size--;
            }
            return true;
        }
        char data = chars[depth];
        checkRange(data);
        int child = path[p][data];
        //路径中断，单词不存在
        if (child == 0) {
            return false;
        }
        boolean removed = remove(child, chars, depth + 1);
        //删除成功后，若该子节点不再被任何单词使用（无结尾且无子节点），则回收并剪除该分支
        if (removed && count[child] == 0 && !hasChild(child)) {
            recycle(child);
            path[p][data] = 0;
        }
        return removed;
    }

    //判断指定节点是否还有子节点
    private boolean hasChild(int p) {
        for (int i = 0; i < this.range; i++) {
            if (path[p][i] != 0) {
                return true;
            }
        }
        return false;
    }

    //递归收集所有以 count>0 节点结尾的单词路径
    private void collectWords(int p, StringBuilder sb, List<String> words) {
        //当前节点是某个单词的结尾，收集整条路径
        if (count[p] > 0) {
            words.add(sb.toString());
        }
        //按下标（字符编码）递增的顺序遍历，保证输出字典序
        for (int i = 0; i < this.range; i++) {
            int child = path[p][i];
            if (child != 0) {
                sb.append((char) i);
                collectWords(child, sb, words);
                sb.deleteCharAt(sb.length() - 1);
            }
        }
    }

    //递归累加节点及其全部子孙节点的count值
    private int sumCount(int p) {
        int sum = count[p];
        for (int i = 0; i < this.range; i++) {
            int child = path[p][i];
            if (child != 0) {
                sum += sumCount(child);
            }
        }
        return sum;
    }

    //递归输出整棵字典树的结构，incoming 为进入该节点所经过的字符（根节点为0）
    private void printTrie(int p, char incoming, StringBuilder sb, String indent) {
        //行首统一为纯空格缩进，每个节点只打印自己的边字符
        sb.append(indent);
        if (p == 0) {
            //空根节点特殊标记
            sb.append("(root)");
        } else {
            sb.append(incoming).append("->");
        }
        sb.append('[').append(p).append(']');
        //标注单词结尾及其插入次数
        if (count[p] > 0) {
            sb.append("  x").append(count[p]);
        }
        sb.append('\n');
        for (int i = 0; i < this.range; i++) {
            int child = path[p][i];
            if (child != 0) {
                printTrie(child, (char) i, sb, indent + "  ");
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

    // 测试代码
    public static void main(String[] args) {
        //length传0也没关系，内部会自动扩容
        ArrayTrie trie = new ArrayTrie(0, 123);
        trie.add("penguin");
        trie.add("pencil");
        trie.add("pencil");
        trie.add("pear");
        trie.add("pine");

        System.out.println("pencil 插入次数：" + trie.getCount("pencil"));    //输出2
        System.out.println("penguin 插入次数：" + trie.getCount("penguin"));  //输出1
        System.out.println("pen 插入次数：" + trie.getCount("pen"));          //输出0
        System.out.println("包含 pen：" + trie.contains("pen"));               //输出false
        System.out.println("pen 是否为前缀：" + trie.isPrefix("pen"));          //输出true
        System.out.println("前缀 pen 的单词累计次数：" + trie.countPrefix("pen")); //输出3
        System.out.println("不同单词个数：" + trie.size());                     //输出4
        System.out.println("累计插入次数：" + trie.wordCount());                //输出5
        System.out.println("节点个数：" + trie.nodeCount());                    //输出16
        System.out.println("全部单词：" + trie.getAllWords());                  //输出[pear, pencil, penguin, pine]

        //删除测试（含节点回收）
        System.out.println("删除 pencil：" + trie.remove("pencil"));             //输出true，次数2→1
        System.out.println("删除 pencil：" + trie.remove("pencil"));             //输出true，次数1→0，回收c/i/l节点
        System.out.println("删除后 pencil 插入次数：" + trie.getCount("pencil")); //输出0
        System.out.println("删除不存在的单词 abc：" + trie.remove("abc"));         //输出false
        System.out.println("回收后节点个数：" + trie.nodeCount());                 //输出13

        //再次插入时复用回收的节点编号
        trie.add("pencil");
        System.out.println("再次插入 pencil 后次数：" + trie.getCount("pencil"));  //输出1
        System.out.println("复用回收节点后节点个数：" + trie.nodeCount());           //输出16

        System.out.println("----- 字典树结构（括号内为节点编号） -----");
        trie.printTrie();
    }
}
