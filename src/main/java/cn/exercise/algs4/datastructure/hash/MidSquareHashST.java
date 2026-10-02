package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 平方取中法(Mid-Square Method)散列表。
 *
 * <p>散列函数:</p>
 * <pre>
 *     H(key) = key^2 的中间 k 位十进制数字       (k = addressDigits)
 * </pre>
 *
 * <p><b>为什么要取"中间"位</b>:平方让关键字的每一位都影响结果的高低位,而平方数的
 * 中间几位由关键字的所有位共同决定,分布通常比"取末位"更均匀;相对地,直接取末两位时,
 * 只要关键字末两位相同就必然冲突(例如 1201、2301、…、8901 会全部挤到地址 01),
 * 平方取中法能把它们打散。</p>
 *
 * <p><b>取位约定(判定缺陷时必须遵守)</b>:设平方数共 L 位,若 L ≤ k,则不足 k 位,
 * 直接把平方值本身作为地址(等价于左侧补零);否则从低位起丢弃
 * {@code offset = (L - k) / 2} 位(即向下取整,窗口略偏右),再取 k 位。例:</p>
 * <ul>
 *   <li>key = 1234 → 1234² = 1522756(L = 7)→ offset = 2 → 15227 → 取两位 = <b>27</b></li>
 *   <li>key = 5678 → 5678² = 32239684(L = 8)→ offset = 3 → 32239 → 取两位 = <b>39</b></li>
 *   <li>key = 3 → 3² = 9(L = 1 ≤ 2)→ 地址 = <b>9</b></li>
 * </ul>
 *
 * <p>地址空间由位数决定:表长 = 10^k,地址自然落在 [0, 10^k),因此不需要再取模。
 * 冲突处理采用<b>线性探测再散列</b> H_i = (H(key) + i) mod m,删除留墓碑,
 * 插入优先复用探测路径上第一个墓碑 —— 与 {@link DivisionHashST} 完全同一套机制,
 * 便于只替换散列函数来对比 ASL。</p>
 *
 * <p>约束:key² 必须在 long 范围内,否则抛 {@link IllegalArgumentException};
 * 负数关键字与其相反数平方相同,因此地址也相同(设计如此)。本类不是线程安全的。</p>
 *
 * @param <Value> 关键字关联的值类型
 */
public class MidSquareHashST<Value> {

    /** 一位的散列位:十进制 */
    private static final int RADIX = 10;

    /** 地址位数上限:10^7 = 10000000,不超过表长上限 */
    private static final int MAX_ADDRESS_DIGITS = 7;

    /** 表长上限:2^26,避免误传超大位数导致 OOM */
    private static final int MAX_TABLE_SIZE = 1 << 26;

    /** 槽位状态:空 */
    private static final byte EMPTY = 0;

    /** 槽位状态:占用 */
    private static final byte OCCUPIED = 1;

    /** 槽位状态:墓碑(曾占用、已删除) */
    private static final byte TOMBSTONE = 2;

    /** 地址位数 k */
    private final int addressDigits;

    /** 表长 m = 10^k */
    private final int tableSize;

    /** 槽位上的关键字 */
    private final long[] slotKeys;

    /** 槽位上的值 */
    private final Object[] slotValues;

    /** 槽位状态 */
    private final byte[] state;

    /** 已存关键字个数 */
    private int n;

    /** 墓碑个数 */
    private int tombstones;

    /** 最近一次查找/插入/删除的探测次数 */
    private int lastProbes;

    /**
     * 构造散列表:表长 = 10^addressDigits。
     *
     * @param addressDigits 地址位数 k,取值 [1, 7]
     * @throws IllegalArgumentException 位数不在合法范围内
     */
    public MidSquareHashST(int addressDigits) {
        if (addressDigits < 1 || addressDigits > MAX_ADDRESS_DIGITS) {
            throw new IllegalArgumentException("地址位数必须在 [1, " + MAX_ADDRESS_DIGITS
                    + "] 内,当前为 " + addressDigits);
        }
        long size = 1;
        for (int i = 0; i < addressDigits; i++) {
            size *= RADIX;
        }
        if (size > MAX_TABLE_SIZE) {
            throw new IllegalArgumentException("表长过大:" + size + "(上限 " + MAX_TABLE_SIZE + ")");
        }
        this.addressDigits = addressDigits;
        this.tableSize = (int) size;
        this.slotKeys = new long[tableSize];
        this.slotValues = new Object[tableSize];
        this.state = new byte[tableSize];
    }

    /**
     * 计算关键字的平方(key² 必须在 long 范围内)。
     *
     * @param key 关键字
     * @return key²
     * @throws IllegalArgumentException 平方溢出 long
     */
    public long square(long key) {
        try {
            return Math.multiplyExact(key, key);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("key 过大,key² 超出 long 范围: " + key);
        }
    }

    /**
     * 散列函数:取 key² 的中间 k 位数字。
     *
     * @param key 关键字
     * @return 地址,落在 [0, tableSize)
     * @throws IllegalArgumentException 平方溢出 long
     */
    public int hash(long key) {
        long square = square(key);
        int length = decimalLength(square);
        if (length <= addressDigits) {
            return (int) square; // 不足 k 位:直接用平方值(等价于左侧补零)
        }
        int offset = (length - addressDigits) / 2;
        long low = powerOfTen(offset);
        return (int) (square / low % powerOfTen(addressDigits));
    }

    /**
     * 插入或更新键值对。冲突时线性探测;探测路径上有墓碑则复用第一个墓碑。
     *
     * @param key   关键字
     * @param value 值,不能为 null
     * @throws IllegalStateException 表已满且没有可复用的墓碑
     */
    public void put(long key, Value value) {
        Objects.requireNonNull(value, "值不能为 null");
        int i = hash(key);
        int firstTombstone = -1;
        int probes = 0;
        for (int step = 0; step < tableSize; step++) {
            probes++;
            if (state[i] == EMPTY) {
                if (firstTombstone >= 0) {
                    slotKeys[firstTombstone] = key;
                    slotValues[firstTombstone] = value;
                    state[firstTombstone] = OCCUPIED;
                    tombstones--;
                } else {
                    slotKeys[i] = key;
                    slotValues[i] = value;
                    state[i] = OCCUPIED;
                }
                n++;
                lastProbes = probes;
                return;
            }
            if (state[i] == TOMBSTONE) {
                if (firstTombstone < 0) {
                    firstTombstone = i;
                }
            } else if (slotKeys[i] == key) {
                slotValues[i] = value;
                lastProbes = probes;
                return;
            }
            i = (i + 1) % tableSize;
        }
        if (firstTombstone >= 0) {
            slotKeys[firstTombstone] = key;
            slotValues[firstTombstone] = value;
            state[firstTombstone] = OCCUPIED;
            tombstones--;
            n++;
            lastProbes = probes;
            return;
        }
        throw new IllegalStateException("散列表已满(" + tableSize + " 个单元),无法插入关键字 " + key);
    }

    /**
     * 查找关键字对应的值。
     *
     * @param key 关键字
     * @return 值;不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public Value get(long key) {
        int i = find(key);
        return i < 0 ? null : (Value) slotValues[i];
    }

    /**
     * 判断关键字是否存在。
     *
     * @param key 关键字
     * @return 存在返回 true
     */
    public boolean contains(long key) {
        return find(key) >= 0;
    }

    /**
     * 返回关键字所在的下标(便于观察探测结果)。内部执行一次查找,会更新 lastProbes。
     *
     * @param key 关键字
     * @return 下标;不存在返回 -1
     */
    public int slotOf(long key) {
        return find(key);
    }

    /**
     * 删除关键字。被删位置留下墓碑以维持探测链。
     *
     * @param key 关键字
     * @return 被删除的值;不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public Value delete(long key) {
        int i = find(key);
        if (i < 0) {
            return null;
        }
        Value old = (Value) slotValues[i];
        state[i] = TOMBSTONE;
        slotValues[i] = null;
        tombstones++;
        n--;
        return old;
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
     * @return 地址位数 k
     */
    public int addressDigits() {
        return addressDigits;
    }

    /**
     * @return 表长 m = 10^k
     */
    public int tableSize() {
        return tableSize;
    }

    /**
     * @return 墓碑个数
     */
    public int tombstones() {
        return tombstones;
    }

    /**
     * @return 装填因子 alpha = n / m
     */
    public double loadFactor() {
        return (double) n / tableSize;
    }

    /**
     * @return 最近一次查找/插入/删除的探测次数
     */
    public int lastProbes() {
        return lastProbes;
    }

    /**
     * 查找成功时的总探测次数(ASL成功公式的分子)。
     *
     * @return 所有已存关键字探测次数之和
     */
    public long successfulProbeSum() {
        long total = 0;
        for (int i = 0; i < tableSize; i++) {
            if (state[i] == OCCUPIED) {
                total += probeCount(slotKeys[i]);
            }
        }
        return total;
    }

    /**
     * 查找失败时的总探测次数(ASL失败公式的分子)。
     * 约定与 {@link DivisionHashST} 一致:探测次数包含最后判定为空单元的那次比较;
     * 墓碑视为非空;满表时以 m 次为上限兜底。
     *
     * @return 每个起始地址查找失败所需探测次数之和
     */
    public long unsuccessfulProbeSum() {
        long total = 0;
        for (int start = 0; start < tableSize; start++) {
            int probes = 1;
            int i = start;
            while (state[i] != EMPTY) {
                if (probes == tableSize) {
                    break;
                }
                probes++;
                i = (i + 1) % tableSize;
            }
            total += probes;
        }
        return total;
    }

    /**
     * @return 查找成功的平均查找长度 ASL成功 = 总探测次数 / n
     */
    public double averageSuccessfulProbes() {
        return n == 0 ? 0.0 : (double) successfulProbeSum() / n;
    }

    /**
     * @return 查找失败的平均查找长度 ASL失败 = 总探测次数 / m
     */
    public double averageUnsuccessfulProbes() {
        return (double) unsuccessfulProbeSum() / tableSize;
    }

    /**
     * 依次返回所有关键字(按数组下标升序)。
     *
     * @return 关键字集合
     */
    public Iterable<Long> keys() {
        List<Long> result = new ArrayList<Long>(n);
        for (int i = 0; i < tableSize; i++) {
            if (state[i] == OCCUPIED) {
                result.add(slotKeys[i]);
            }
        }
        return result;
    }

    /**
     * 按数组下标升序输出所有键值对:{key value, key value}。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        String sep = "";
        for (int i = 0; i < tableSize; i++) {
            if (state[i] == OCCUPIED) {
                sb.append(sep).append(slotKeys[i]).append(' ').append(slotValues[i]);
                sep = ", ";
            }
        }
        return sb.append('}').toString();
    }

    // ---------------- 内部实现 ----------------

    /**
     * 内部查找:返回下标,-1 表示不存在;同时记录探测次数。
     */
    private int find(long key) {
        int i = hash(key);
        for (int probes = 1; probes <= tableSize; probes++) {
            if (state[i] == EMPTY) {
                lastProbes = probes;
                return -1;
            }
            if (state[i] == OCCUPIED && slotKeys[i] == key) {
                lastProbes = probes;
                return i;
            }
            i = (i + 1) % tableSize;
        }
        lastProbes = tableSize;
        return -1;
    }

    /**
     * 纯探测次数统计(不修改 lastProbes),供 ASL 统计使用。
     */
    private int probeCount(long key) {
        int i = hash(key);
        for (int probes = 1; probes <= tableSize; probes++) {
            if (state[i] == EMPTY) {
                return probes;
            }
            if (state[i] == OCCUPIED && slotKeys[i] == key) {
                return probes;
            }
            i = (i + 1) % tableSize;
        }
        return tableSize;
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

    /**
     * 演示:取位过程、与"取末两位"的冲突对比、建表与 ASL。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("平方取中法散列函数:H(key) = key² 的中间 k 位");
        System.out.println();

        MidSquareHashST<String> st = new MidSquareHashST<String>(2); // 表长 100
        System.out.println("地址位数 k = " + st.addressDigits() + ",表长 m = " + st.tableSize());
        System.out.println();
        System.out.println("取位过程(L = 平方数的位数,offset = (L-k)/2,从低位丢弃的位数):");
        System.out.println("  关键字        key²          L    offset   地址");
        long[] samples = {1234, 5678, 3, 99, 100};
        for (long key : samples) {
            long square = st.square(key);
            int length = String.valueOf(square).length();
            int offset = length <= st.addressDigits() ? 0 : (length - st.addressDigits()) / 2;
            System.out.printf("  %-12d  %-12d  %-4d  %-7d  %d%n",
                    key, square, length, offset, st.hash(key));
        }
        System.out.println();

        long[] collideProne = {1201, 2301, 3401, 4501, 5601, 6701, 7801, 8901};
        System.out.println("冲突对比:关键字 " + java.util.Arrays.toString(collideProne));
        System.out.println("  末两位取法    : " + describeAddresses(collideProne, false, st));
        System.out.println("  平方取中法    : " + describeAddresses(collideProne, true, st));
        System.out.println();

        for (long key : collideProne) {
            st.put(key, "v" + key);
        }
        System.out.println("建表后:size = " + st.size() + ",装填因子 = " + String.format("%.4f", st.loadFactor()));
        System.out.printf("ASL成功 = %d/%d = %.4f%n", st.successfulProbeSum(), st.size(), st.averageSuccessfulProbes());
        System.out.printf("ASL失败 = %d/%d = %.4f%n",
                st.unsuccessfulProbeSum(), st.tableSize(), st.averageUnsuccessfulProbes());
        long probe = 3401;
        System.out.println("查询演示:get(" + probe + ") = " + st.get(probe)
                + ",slotOf(" + probe + ") = " + st.slotOf(probe)
                + ",delete(" + probe + ") = " + st.delete(probe)
                + ",删除后 size = " + st.size() + ",墓碑 = " + st.tombstones());
    }

    /**
     * 演示用:分别用"取末两位"或"平方取中"计算地址分布。
     */
    private static String describeAddresses(long[] keys, boolean midSquare, MidSquareHashST<String> st) {
        boolean[] used = new boolean[st.tableSize()];
        int distinct = 0;
        int collisions = 0;
        StringBuilder sb = new StringBuilder();
        for (long key : keys) {
            int address = midSquare ? st.hash(key) : (int) (Math.floorMod(key, 100));
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
