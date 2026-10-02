package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 随机数法(Random Method)散列表。
 *
 * <p>散列函数取关键字的<b>伪随机函数值</b>:</p>
 * <pre>
 *     H(key) = random(key) mod m        (m = tableSize)
 * </pre>
 *
 * <p><b>最关键的一条</b>:这里的 random 必须是<b>确定性</b>的 —— 同一个关键字每次都必须
 * 得到同一个地址,否则插入与查找落在不同地址,散列表立刻失效。因此实践中要用:
 * 固定的伪随机混合函数(本类采用),或一张预生成的随机数表;绝不能直接用
 * {@code Math.random()} / 未固定种子的 {@code new Random()}(那样同一个关键字两次地址不同)。</p>
 *
 * <p>本类使用的伪随机函数是 64 位混合(xor-shift-multiply 三轮,与 MurmurHash3 的收尾混合同型),
 * 并把 seed 混入关键字:同样输入必然得到同样输出,换 seed 相当于换一个"随机函数"。
 * 它能提供良好的雪崩效应:关键字只差 1 位,输出约有一半的位会翻转,因此
 * 关键字连续、有规律、位数不等时都能得到看不出规律的地址。</p>
 *
 * <p>适用场景:关键字本身没有可利用的规律(如任意编号、混合编码),或关键字长度不等,
 * 无法用数字分析法/平方取中法/折叠法这类依赖十进制位的办法时。</p>
 *
 * <p>冲突处理采用<b>线性探测再散列</b>(与 {@link DivisionHashST}、{@link MidSquareHashST} 同一套机制
 * 与同一套 ASL 口径),便于只替换散列函数做对比。关键字可以是任意 long(含负数),
 * 不要求是十进制编码。本类不是线程安全的。</p>
 *
 * @param <Value> 关键字关联的值类型
 */
public class RandomHashST<Value> {

    /** 默认种子(固定值,保证默认构造下的结果是可复现的) */
    private static final long DEFAULT_SEED = 0x9E3779B97F4A7C15L;

    /** 表长上限 2^26,避免误传超大表长导致 OOM */
    private static final int MAX_TABLE_SIZE = 1 << 26;

    /** 槽位状态:空 */
    private static final byte EMPTY = 0;

    /** 槽位状态:占用 */
    private static final byte OCCUPIED = 1;

    /** 槽位状态:墓碑(曾占用、已删除) */
    private static final byte TOMBSTONE = 2;

    /** 表长 m */
    private final int tableSize;

    /** 随机函数种子 */
    private final long seed;

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
     * 用默认种子构造。
     *
     * @param tableSize 表长 m,取值 [1, 2^26]
     */
    public RandomHashST(int tableSize) {
        this(tableSize, DEFAULT_SEED);
    }

    /**
     * 指定表长与随机函数种子构造。
     *
     * @param tableSize 表长 m,取值 [1, 2^26]
     * @param seed      伪随机函数种子,固定不变才能保证同一关键字地址稳定
     * @throws IllegalArgumentException 表长非法
     */
    public RandomHashST(int tableSize, long seed) {
        if (tableSize < 1 || tableSize > MAX_TABLE_SIZE) {
            throw new IllegalArgumentException("表长必须在 [1, " + MAX_TABLE_SIZE
                    + "] 内,当前为 " + tableSize);
        }
        this.tableSize = tableSize;
        this.seed = seed;
        this.slotKeys = new long[tableSize];
        this.slotValues = new Object[tableSize];
        this.state = new byte[tableSize];
    }

    /**
     * 伪随机函数 random(key):确定性的 64 位混合,返回 31 位非负值。
     *
     * @param key 关键字,可为任意 long(含负数)
     * @return [0, 2^31 - 1] 内的伪随机值
     */
    public int randomBits(long key) {
        long x = key ^ seed;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (int) (x & 0x7fffffffL);
    }

    /**
     * 散列函数 H(key) = random(key) mod m。
     *
     * @param key 关键字
     * @return 地址,落在 [0, tableSize)
     */
    public int hash(long key) {
        return randomBits(key) % tableSize;
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
     * 返回关键字所在下标(内部执行一次查找,会更新 lastProbes)。
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
     * @return 表长 m
     */
    public int tableSize() {
        return tableSize;
    }

    /**
     * @return 随机函数种子
     */
    public long seed() {
        return seed;
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
     * 查找失败时的总探测次数(ASL失败公式的分子),口径与 {@link DivisionHashST} 一致:
     * 探测次数包含最后判定为空单元的那次比较;墓碑视为非空;满表时以 m 次为上限兜底。
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

    /**
     * 演示:随机函数值、雪崩效应、分布均匀性、建表与查询,以及"可变种子"的错误示范。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("随机数法散列函数:H(key) = random(key) mod m(随机函数必须确定性)");
        System.out.println();

        RandomHashST<String> st = new RandomHashST<String>(16);
        System.out.println("表长 m = " + st.tableSize() + ",种子 seed = " + st.seed());
        System.out.println();
        System.out.println("  关键字            random(key)    地址(mod 16)");
        long[] samples = {0, 1, 2, 1000, 1001, -1, Long.MIN_VALUE};
        for (long key : samples) {
            System.out.printf("  %-17d  %-13d  %d%n", key, st.randomBits(key), st.hash(key));
        }
        System.out.println();

        System.out.println("雪崩效应:相邻关键字的 random 值在 32 位中平均有多少位不同(理论期望约 16 位)");
        long diffSum = 0;
        int pairs = 0;
        int minDiff = 32;
        int maxDiff = 0;
        for (long key = 0; key < 5000; key++) {
            int d = Integer.bitCount(st.randomBits(key) ^ st.randomBits(key + 1));
            diffSum += d;
            pairs++;
            if (d < minDiff) {
                minDiff = d;
            }
            if (d > maxDiff) {
                maxDiff = d;
            }
        }
        System.out.printf("  %d 组相邻关键字:平均 %.2f 位,最少 %d 位,最多 %d 位%n",
                pairs, (double) diffSum / pairs, minDiff, maxDiff);
        System.out.println();

        int bucketCount = 1024;
        int keyCount = 10000;
        RandomHashST<String> wide = new RandomHashST<String>(bucketCount);
        int[] load = new int[bucketCount];
        for (int i = 0; i < keyCount; i++) {
            load[wide.hash(i)]++;
        }
        int maxLoad = 0;
        int emptyBuckets = 0;
        for (int count : load) {
            if (count > maxLoad) {
                maxLoad = count;
            }
            if (count == 0) {
                emptyBuckets++;
            }
        }
        System.out.println("分布均匀性:" + keyCount + " 个连续关键字投到 " + bucketCount + " 个地址");
        System.out.printf("  平均每地址 %.2f 个,最忙地址 %d 个,空地址 %d 个%n",
                (double) keyCount / bucketCount, maxLoad, emptyBuckets);
        System.out.println("  对照:若用除留余数法 H(key)=key mod " + bucketCount
                + ",连续关键字的地址就是 0,1,2,…(有规律可循、可被预测)");
        System.out.println();

        RandomHashST<String> other = new RandomHashST<String>(16, st.seed() + 1);
        System.out.println("错误示范与种子说明:");
        System.out.println("  同一个关键字 1000:同种子(默认)地址 = " + st.hash(1000)
                + ",换一个种子后地址 = " + other.hash(1000)
                + " —— 只要种子固定,各自都稳定可查;");
        System.out.println("  若用 Math.random()/未固定种子的 new Random(),同一个关键字两次地址不同,根本查不回来。");
        System.out.println();

        for (long key = 0; key < 12; key++) {
            st.put(key, "v" + key);
        }
        System.out.println("建表(线性探测):size = " + st.size()
                + ",装填因子 = " + String.format("%.4f", st.loadFactor()));
        System.out.printf("ASL成功 = %d/%d = %.4f%n", st.successfulProbeSum(), st.size(), st.averageSuccessfulProbes());
        System.out.printf("ASL失败 = %d/%d = %.4f%n",
                st.unsuccessfulProbeSum(), st.tableSize(), st.averageUnsuccessfulProbes());
        long probe = 7;
        System.out.println("查询演示:get(" + probe + ") = " + st.get(probe)
                + ",slotOf(" + probe + ") = " + st.slotOf(probe)
                + ",delete(" + probe + ") = " + st.delete(probe)
                + ",删除后 size = " + st.size() + ",墓碑 = " + st.tombstones());
    }
}
