package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 折叠法(Folding Method)散列表。
 *
 * <p>把关键字按固定位数切成若干段(最后一段可以短些),再把各段叠加成一个数,最后取模得到地址:</p>
 * <pre>
 *     移位叠加:sum = Σ 各段(全部正序)
 *     分界叠加:sum = Σ (奇数段正序 + 偶数段反序)
 *     H(key) = sum mod tableSize        // 表长为 10^k 时等价于"取叠加和的后 k 位、舍去进位"
 * </pre>
 *
 * <p><b>分段与编号约定(判定缺陷时必须遵守)</b>:关键字按十进制数字处理,<b>从低位向高位</b>
 * 每 {@code segmentDigits} 位切一段,段号从 1 开始;最高位那段可以不足 {@code segmentDigits} 位。
 * 分界叠加时<b>奇数段正序、偶数段反序</b>,反序按固定宽度处理(缺位补零),
 * 例:段 "005" 反序为 "500";段 "040" 反序仍为 "040"。</p>
 *
 * <p>例(segmentDigits = 3):</p>
 * <ul>
 *   <li>key = 123456789 → 各段(低位→高位)= 789 | 456 | 123 →
 *       移位叠加 = 1368,分界叠加 = 789 + 654 + 123 = 1566;
 *       表长 1000 时地址分别为 <b>368</b> 与 <b>566</b>。</li>
 *   <li>key = 87654321 → 321 | 654 | 87 → 移位 = 1062,分界 = 321 + 456 + 87 = 864。</li>
 *   <li>key = 1000 → 0 | 1 → 移位 = 1,分界 = 0 + 100 = 100(偶数段 "001" 按 3 位反序成 "100")。</li>
 * </ul>
 *
 * <p>折叠法适合位数很多的关键字(身份证号、长账号等):它不需要关键字连续,也不依赖样本分析,
 * 只要求各段叠加后能把分布打散。本类用<b>拉链法</b>处理冲突,因此不存在"表满拒插"。</p>
 *
 * <p>约束:关键字必须是非负整数(十进制数字编码),否则抛 {@link IllegalArgumentException};
 * 值为 null 抛 {@link NullPointerException}。本类不是线程安全的。</p>
 *
 * @param <Value> 关键字关联的值类型
 */
public class FoldingHashST<Value> {

    /** 叠加方式 */
    public enum Mode {
        /** 移位叠加:各段全部正序相加 */
        SHIFT,
        /** 分界叠加:奇数段正序、偶数段反序后相加 */
        BOUNDARY
    }

    /** 一位十进制 */
    private static final int RADIX = 10;

    /** 表长上限 2^26,避免误传超大表长导致 OOM */
    private static final int MAX_TABLE_SIZE = 1 << 26;

    /** 拉链法链结点 */
    private static final class Node<Value> {
        final long key;
        Value value;
        Node<Value> next;

        Node(long key, Value value) {
            this.key = key;
            this.value = value;
        }
    }

    /** 段长(每位数字数) */
    private final int segmentDigits;

    /** 表长 */
    private final int tableSize;

    /** 叠加方式 */
    private final Mode mode;

    /** 桶数组 */
    private final Node<Value>[] buckets;

    /** 已存关键字个数 */
    private int n;

    /**
     * 用移位叠加构造。
     *
     * @param segmentDigits 段长,至少 1
     * @param tableSize     表长,取值 [1, 2^26]
     */
    public FoldingHashST(int segmentDigits, int tableSize) {
        this(segmentDigits, tableSize, Mode.SHIFT);
    }

    /**
     * 指定叠加方式构造。
     *
     * @param segmentDigits 段长,至少 1
     * @param tableSize     表长,取值 [1, 2^26]
     * @param mode          叠加方式,不能为 null
     * @throws IllegalArgumentException 段长或表长非法
     */
    public FoldingHashST(int segmentDigits, int tableSize, Mode mode) {
        if (segmentDigits < 1) {
            throw new IllegalArgumentException("段长必须至少为 1,当前为 " + segmentDigits);
        }
        if (tableSize < 1 || tableSize > MAX_TABLE_SIZE) {
            throw new IllegalArgumentException("表长必须在 [1, " + MAX_TABLE_SIZE
                    + "] 内,当前为 " + tableSize);
        }
        this.segmentDigits = segmentDigits;
        this.tableSize = tableSize;
        this.mode = Objects.requireNonNull(mode, "叠加方式不能为 null");
        this.buckets = newNodeArray(tableSize);
    }

    /**
     * 把关键字按段长切成若干段(低位→高位),段号从 1 开始。
     *
     * @param key 关键字
     * @return 各段的数值(最高段可以不足段长)
     * @throws IllegalArgumentException 关键字为负
     */
    public int[] segments(long key) {
        checkKey(key);
        int count = (decimalLength(key) + segmentDigits - 1) / segmentDigits;
        int[] result = new int[count];
        long unit = powerOfTen(segmentDigits);
        long rest = key;
        for (int i = 0; i < count; i++) {
            result[i] = (int) (rest % unit);
            rest /= unit;
        }
        return result;
    }

    /**
     * 折叠叠加和(未取模)。
     *
     * @param key 关键字
     * @return 各段叠加之和;分界叠加时奇数段正序、偶数段反序
     * @throws IllegalArgumentException 关键字为负
     */
    public long foldedSum(long key) {
        int[] parts = segments(key);
        long sum = 0;
        for (int i = 0; i < parts.length; i++) {
            int part = parts[i];
            if (mode == Mode.BOUNDARY && (i + 1) % 2 == 0) {
                part = reverseDigits(part, segmentDigits);
            }
            sum += part;
        }
        return sum;
    }

    /**
     * 散列函数:折叠叠加和再对表长取模(表长为 10^k 时即取叠加和的后 k 位)。
     *
     * @param key 关键字
     * @return 地址,落在 [0, tableSize)
     * @throws IllegalArgumentException 关键字为负
     */
    public int hash(long key) {
        return (int) (foldedSum(key) % tableSize);
    }

    /**
     * 插入或更新键值对。地址冲突时挂到同一个桶的链上。
     *
     * @param key   关键字
     * @param value 值,不能为 null
     */
    public void put(long key, Value value) {
        Objects.requireNonNull(value, "值不能为 null");
        int address = hash(key);
        for (Node<Value> node = buckets[address]; node != null; node = node.next) {
            if (node.key == key) {
                node.value = value;
                return;
            }
        }
        Node<Value> head = new Node<Value>(key, value);
        head.next = buckets[address];
        buckets[address] = head;
        n++;
    }

    /**
     * 查找关键字对应的值。
     *
     * @param key 关键字
     * @return 值;不存在时返回 null
     */
    public Value get(long key) {
        int address = hash(key);
        for (Node<Value> node = buckets[address]; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }

    /**
     * 判断关键字是否存在。
     *
     * @param key 关键字
     * @return 存在返回 true
     */
    public boolean contains(long key) {
        int address = hash(key);
        for (Node<Value> node = buckets[address]; node != null; node = node.next) {
            if (node.key == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * 删除关键字及其值。
     *
     * @param key 关键字
     * @return 被删除的值;不存在返回 null
     */
    public Value delete(long key) {
        int address = hash(key);
        Node<Value> prev = null;
        Node<Value> node = buckets[address];
        while (node != null) {
            if (node.key == key) {
                if (prev == null) {
                    buckets[address] = node.next;
                } else {
                    prev.next = node.next;
                }
                n--;
                return node.value;
            }
            prev = node;
            node = node.next;
        }
        return null;
    }

    /**
     * @return 已存关键字个数
     */
    public int size() {
        return n;
    }

    /**
     * @return 散列表为空返回 true
     */
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * @return 段长
     */
    public int segmentDigits() {
        return segmentDigits;
    }

    /**
     * @return 表长
     */
    public int tableSize() {
        return tableSize;
    }

    /**
     * @return 叠加方式
     */
    public Mode mode() {
        return mode;
    }

    /**
     * @return 装填因子 alpha = n / tableSize
     */
    public double loadFactor() {
        return (double) n / tableSize;
    }

    /**
     * @return 最长链长度(拉链法下即最坏查找长度)
     */
    public int maxChainLength() {
        int max = 0;
        for (Node<Value> bucket : buckets) {
            int len = 0;
            for (Node<Value> node = bucket; node != null; node = node.next) {
                len++;
            }
            if (len > max) {
                max = len;
            }
        }
        return max;
    }

    /**
     * 依次返回所有关键字(按地址升序,同桶内为最近插入优先)。
     *
     * @return 关键字集合
     */
    public Iterable<Long> keys() {
        List<Long> result = new ArrayList<Long>(n);
        for (Node<Value> bucket : buckets) {
            for (Node<Value> node = bucket; node != null; node = node.next) {
                result.add(node.key);
            }
        }
        return result;
    }

    /**
     * 按地址升序输出所有键值对:{key value, key value}。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        String sep = "";
        for (Node<Value> bucket : buckets) {
            for (Node<Value> node = bucket; node != null; node = node.next) {
                sb.append(sep).append(node.key).append(' ').append(node.value);
                sep = ", ";
            }
        }
        return sb.append('}').toString();
    }

    // ---------------- 内部实现 ----------------

    /**
     * 按固定宽度反序十进制数字(缺位补零):reverse(5, 3) = 500,reverse(40, 3) = 40。
     */
    private static int reverseDigits(int value, int width) {
        int result = 0;
        int rest = value;
        for (int i = 0; i < width; i++) {
            result = result * RADIX + rest % RADIX;
            rest /= RADIX;
        }
        return result;
    }

    private void checkKey(long key) {
        if (key < 0) {
            throw new IllegalArgumentException("折叠法要求非负的十进制数字编码,当前 key = " + key);
        }
    }

    private static int decimalLength(long value) {
        int length = 1;
        long v = value;
        while (v >= RADIX) {
            v /= RADIX;
            length++;
        }
        return length;
    }

    private static long powerOfTen(int exponent) {
        long result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= RADIX;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <Value> Node<Value>[] newNodeArray(int size) {
        return (Node<Value>[]) new Node[size];
    }

    /**
     * 演示:分段与两种叠加方式、地址计算、建表与查询。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("折叠法散列函数:H(key) = 各段叠加和 mod 表长(舍去进位)");
        System.out.println();

        int segmentDigits = 3;
        int tableSize = 1000;
        FoldingHashST<String> shift = new FoldingHashST<String>(segmentDigits, tableSize, FoldingHashST.Mode.SHIFT);
        FoldingHashST<String> boundary = new FoldingHashST<String>(segmentDigits, tableSize, FoldingHashST.Mode.BOUNDARY);

        System.out.println("段长 = " + segmentDigits + ",表长 = " + tableSize
                + "(等价于取叠加和的后 3 位)");
        System.out.println();
        System.out.println("分段与叠加(段号从低位起:第 1 段是个位那一段)");
        System.out.println("  关键字              各段(低位→高位)        移位叠加   分界叠加   移位地址   分界地址");
        long[] samples = {123456789, 87654321, 1000, 1234567890123L, 7};
        for (long key : samples) {
            int[] parts = shift.segments(key);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                sb.append(i == 0 ? "" : " | ").append(parts[i]);
            }
            System.out.printf("  %-18d  %-22s  %-9d  %-9d  %-9d  %d%n",
                    key, sb.toString(), shift.foldedSum(key), boundary.foldedSum(key),
                    shift.hash(key), boundary.hash(key));
        }
        System.out.println();

        long[] sameTail = {1001, 2001, 3001, 4001, 5001, 6001, 7001, 8001};
        System.out.println("冲突对比:关键字 " + java.util.Arrays.toString(sameTail));
        System.out.println("  取末三位      : " + describe(sameTail, 1000, shift, false));
        System.out.println("  折叠(移位叠加): " + describe(sameTail, 1000, shift, true));
        System.out.println();

        for (long key : sameTail) {
            shift.put(key, "v" + key);
        }
        System.out.println("建表(拉链法):size = " + shift.size()
                + ",装填因子 = " + String.format("%.4f", shift.loadFactor())
                + ",最长链长 = " + shift.maxChainLength());
        long probe = 3001;
        System.out.println("查询演示:get(" + probe + ") = " + shift.get(probe)
                + ",contains(1001) = " + shift.contains(1001)
                + ",delete(" + probe + ") = " + shift.delete(probe)
                + ",删除后 size = " + shift.size());
    }

    /**
     * 演示用:比较"取末三位"与"折叠叠加"的地址分布。
     */
    private static String describe(long[] keys, int tableSize, FoldingHashST<String> st, boolean fold) {
        boolean[] used = new boolean[tableSize];
        int distinct = 0;
        int collisions = 0;
        StringBuilder sb = new StringBuilder();
        for (long key : keys) {
            int address = fold ? st.hash(key) : (int) (key % tableSize);
            if (used[address]) {
                collisions++;
            } else {
                used[address] = true;
                distinct++;
            }
            sb.append(address).append(' ');
        }
        return "地址 " + sb.toString().trim() + " → 不同地址 " + distinct + " 个,冲突 " + collisions + " 次";
    }
}
