package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 开放地址法(Open Addressing)散列表,用<b>双重散列</b>处理冲突。
 *
 * <pre>
 *     H_i = ( H1(key) + i * H2(key) ) mod m ,  i = 0, 1, 2, …
 * </pre>
 *
 * <p><b>与二次探测的关键差别</b>:步长 H2(key) 随关键字变化,所以<b>即使两个关键字的 H1 相同,
 * 它们走的也是不同的探测序列</b> —— 二次探测的"次级聚集"被彻底消除。这也是双重散列在开放地址法里
 * 最接近"随机探测"的原因:它的平均查找长度可以直接用随机探测的公式估计</p>
 * <pre>
 *     ASL成功 ≈ ln(1/(1-α)) / α ,    ASL失败 ≈ 1/(1-α)
 * </pre>
 *
 * <p><b>覆盖性</b>:序列能遍历全表 ⟺ <b>gcd(H2(key), m) = 1</b>。所以实践中有两条硬要求:</p>
 * <ol>
 *   <li>H2(key) 必须落在 [1, m-1](为 0 就是原地打转);</li>
 *   <li>默认做法让 <b>m 取素数</b>、H2(key) = 1 + (key mod (m-1)),此时任何关键字都满足互素,
 *       序列必然覆盖全表。若 m 是合数而 H2 与其不互素,就会出现"表里还有空位却插不进"
 *       (与二次探测“覆盖不全”是同一类现象,只是原因不同)。</li>
 * </ol>
 *
 * <p>因此本类:{@link #stepOf(long)} 会校验 H2 ∈ [1, m-1];{@link #fullCoverageGuaranteed()}
 * 报告当前表长下是否保证全表覆盖;扩容始终取素数(复用 {@link OpenAddressHashST#nextPrime(int)})。
 * 删除留墓碑、插入优先复用序列上第一个墓碑、α 超过上限时重建(只搬活元素)。</p>
 *
 * <p>H1、H2 都是可插拔的({@link HashFunction});本类不支持 null 值;不是线程安全的。</p>
 *
 * @param <Value> 关键字关联的值类型
 * @see QuadraticProbeHashST 二次探测版本(有次级聚集)
 */
public class DoubleHashingHashST<Value> {

    /** 可插拔散列函数:返回 [0, capacity) 内的地址 */
    public interface HashFunction {
        /**
         * @param key      关键字
         * @param capacity 当前表长
         * @return 地址,必须落在 [0, capacity)
         */
        int hash(long key, int capacity);
    }

    /** 默认初始容量(素数) */
    private static final int DEFAULT_CAPACITY = 17;

    /** 默认装填因子上限 */
    private static final double DEFAULT_MAX_LOAD_FACTOR = 0.75;

    /** 容量上限 2^26 */
    private static final int MAX_CAPACITY = 1 << 26;

    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;
    private static final byte TOMBSTONE = 2;

    /** 主散列函数 H1 */
    private final HashFunction hashFunction1;

    /** 步长散列函数 H2 */
    private final HashFunction hashFunction2;

    private final double maxLoadFactor;

    private int capacity;
    private long[] slotKeys;
    private Object[] slotValues;
    private byte[] state;

    private int n;
    private int tombstones;
    private int resizeCount;
    private int lastProbes;

    /**
     * 默认构造:容量 17、H1 = key mod m、H2 = 1 + key mod (m-1)、α 上限 0.75。
     */
    public DoubleHashingHashST() {
        this(DEFAULT_CAPACITY, DEFAULT_MAX_LOAD_FACTOR, defaultH1(), defaultH2());
    }

    /**
     * @param initialCapacity 初始容量,至少 2
     */
    public DoubleHashingHashST(int initialCapacity) {
        this(initialCapacity, DEFAULT_MAX_LOAD_FACTOR, defaultH1(), defaultH2());
    }

    /**
     * @param initialCapacity 初始容量,至少 2
     * @param maxLoadFactor   装填因子上限,取值 (0, 1]
     */
    public DoubleHashingHashST(int initialCapacity, double maxLoadFactor) {
        this(initialCapacity, maxLoadFactor, defaultH1(), defaultH2());
    }

    /**
     * @param initialCapacity 初始容量,至少 2
     * @param maxLoadFactor   装填因子上限
     * @param hashFunction1   主散列函数 H1
     */
    public DoubleHashingHashST(int initialCapacity, double maxLoadFactor, HashFunction hashFunction1) {
        this(initialCapacity, maxLoadFactor, hashFunction1, defaultH2());
    }

    /**
     * 完整构造。
     *
     * @param initialCapacity 初始容量,至少 2
     * @param maxLoadFactor   装填因子上限,取值 (0, 1]
     * @param hashFunction1   主散列函数 H1,返回 [0, capacity)
     * @param hashFunction2   步长函数 H2,返回 [1, capacity-1]
     * @throws IllegalArgumentException 参数非法
     */
    public DoubleHashingHashST(int initialCapacity, double maxLoadFactor,
                               HashFunction hashFunction1, HashFunction hashFunction2) {
        if (initialCapacity < 2 || initialCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("初始容量必须在 [2, " + MAX_CAPACITY
                    + "] 内,当前为 " + initialCapacity);
        }
        if (maxLoadFactor <= 0.0 || maxLoadFactor > 1.0) {
            throw new IllegalArgumentException("装填因子上限必须在 (0, 1] 内,当前为 " + maxLoadFactor);
        }
        this.hashFunction1 = Objects.requireNonNull(hashFunction1, "H1 不能为 null");
        this.hashFunction2 = Objects.requireNonNull(hashFunction2, "H2 不能为 null");
        this.maxLoadFactor = maxLoadFactor;
        allocate(initialCapacity);
    }

    /**
     * 插入或更新键值对。序列覆盖全表时,只有"表真的填满且没有墓碑"才会失败。
     *
     * @param key   关键字
     * @param value 值,不能为 null
     * @throws IllegalStateException 序列走完仍无空位,且无法扩容
     */
    public void put(long key, Value value) {
        Objects.requireNonNull(value, "值不能为 null");
        if (maxLoadFactor < 1.0 && (double) (n + 1) / capacity > maxLoadFactor) {
            if (!grow()) {
                throw new IllegalStateException("已达容量上限 " + MAX_CAPACITY + ",无法继续扩容");
            }
        }
        if (insert(key, value)) {
            return;
        }
        if (maxLoadFactor >= 1.0) {
            throw new IllegalStateException("探测序列已走完仍无空位(m = " + capacity + ",n = " + n
                    + ",墓碑 = " + tombstones + "):H2 与表长可能不互素,或表已真正填满");
        }
        if (!grow() || !insert(key, value)) {
            throw new IllegalStateException("已达容量上限 " + MAX_CAPACITY + ",无法插入关键字 " + key);
        }
    }

    /**
     * 查找关键字对应的值。
     *
     * @param key 关键字
     * @return 值;不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public Value get(long key) {
        int i = find(key);
        return i < 0 ? null : (Value) slotValues[i];
    }

    /**
     * @param key 关键字
     * @return 是否存在
     */
    public boolean contains(long key) {
        return find(key) >= 0;
    }

    /**
     * @param key 关键字
     * @return 下标;不存在返回 -1(内部执行一次查找,会更新 lastProbes)
     */
    public int slotOf(long key) {
        return find(key);
    }

    /**
     * 删除关键字:被删位置留墓碑。
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
     * @return 已存元素个数
     */
    public int size() {
        return n;
    }

    /**
     * @return 是否为空
     */
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * @return 当前表长 m
     */
    public int capacity() {
        return capacity;
    }

    /**
     * @return 墓碑个数
     */
    public int tombstones() {
        return tombstones;
    }

    /**
     * @return 扩容次数
     */
    public int resizeCount() {
        return resizeCount;
    }

    /**
     * @return 装填因子上限
     */
    public double maxLoadFactor() {
        return maxLoadFactor;
    }

    /**
     * @return 装填因子 α = n / m
     */
    public double loadFactor() {
        return (double) n / capacity;
    }

    /**
     * @return 最近一次操作的探测次数
     */
    public int lastProbes() {
        return lastProbes;
    }

    /**
     * 主散列地址 H1(key)。
     *
     * @param key 关键字
     * @return 地址,落在 [0, capacity)
     * @throws IllegalStateException H1 返回越界地址
     */
    public int hashAddress(long key) {
        int address = hashFunction1.hash(key, capacity);
        if (address < 0 || address >= capacity) {
            throw new IllegalStateException("H1 返回了越界地址:" + address + "(表长 " + capacity + ")");
        }
        return address;
    }

    /**
     * 步长 H2(key),校验其落在 [1, capacity-1]。
     *
     * @param key 关键字
     * @return 步长
     * @throws IllegalStateException H2 返回 0 或越界(0 会让探测原地打转)
     */
    public int stepOf(long key) {
        int step = hashFunction2.hash(key, capacity);
        if (step <= 0 || step >= capacity) {
            throw new IllegalStateException("H2 必须返回 [1, " + (capacity - 1) + "] 内的步长,当前为 " + step
                    + "(0 会让探测原地打转)");
        }
        return step;
    }

    /**
     * 当前表长下是否保证探测序列覆盖全表。
     *
     * <p>判据:表长 m 为素数时,H2 ∈ [1, m-1] 必然与 m 互素,序列覆盖全表;
     * m 为合数时不保证(例如 m = 9、H2 = 3 只能覆盖 3 个槽位)。</p>
     *
     * @return 是否保证全表覆盖
     */
    public boolean fullCoverageGuaranteed() {
        return isPrime(capacity);
    }

    /**
     * 从关键字出发的探测序列(去重,按访问顺序),直到序列回到起点。
     *
     * @param key 关键字
     * @return 依次访问到的不同地址
     */
    public int[] probeSequence(long key) {
        int home = hashAddress(key);
        int step = stepOf(key);
        List<Integer> sequence = new ArrayList<Integer>();
        for (int i = 0; i < capacity; i++) {
            int address = addressAt(home, step, i);
            if (i > 0 && address == home) {
                break;
            }
            if (!sequence.contains(address)) {
                sequence.add(address);
            }
        }
        int[] result = new int[sequence.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = sequence.get(i);
        }
        return result;
    }

    /**
     * @return 最长连续占用块长度(墓碑也算占用)
     */
    public int maxClusterLength() {
        if (n + tombstones == 0) {
            return 0;
        }
        if (n + tombstones == capacity) {
            return capacity;
        }
        int start = -1;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == EMPTY) {
                start = i;
                break;
            }
        }
        int max = 0;
        int run = 0;
        for (int step = 1; step <= capacity; step++) {
            int i = (start + step) % capacity;
            if (state[i] == EMPTY) {
                max = Math.max(max, run);
                run = 0;
            } else {
                run++;
            }
        }
        return Math.max(max, run);
    }

    /**
     * @return 连续占用块个数
     */
    public int clusterCount() {
        if (n + tombstones == 0) {
            return 0;
        }
        if (n + tombstones == capacity) {
            return 1;
        }
        int start = -1;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == EMPTY) {
                start = i;
                break;
            }
        }
        int clusters = 0;
        boolean inCluster = false;
        for (int step = 1; step <= capacity; step++) {
            int i = (start + step) % capacity;
            if (state[i] == EMPTY) {
                inCluster = false;
            } else if (!inCluster) {
                clusters++;
                inCluster = true;
            }
        }
        return clusters;
    }

    /**
     * 查找成功时的总探测次数。
     *
     * @return 所有已存关键字探测次数之和
     */
    public long successfulProbeSum() {
        long total = 0;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED) {
                total += probeCount(slotKeys[i]);
            }
        }
        return total;
    }

    /**
     * 「连续占用块」式失败探测总量(与线性探测同口径,便于跨实现横向对比):
     * 以每个槽位为起点,每次前进 1 步,直到遇到空单元;计数包含"判定为空"的那次比较。
     *
     * <p><b>口径说明</b>:双重散列的步长 H2 依赖关键字,因此"从某个起点出发的失败查找"
     * 并没有唯一确定的探测序列,本方法度量的是表内连续占用块的长度分布,<b>不等于</b>
     * 双重散列真实失败查找代价的期望。后者请用 {@link #averageUnsuccessfulProbes(long[])}
     * 实测,或看理论值 {@link #randomProbingUnsuccessfulProbes()}。</p>
     *
     * @return 各起点按步长 1 前进到空单元的探测次数之和
     */
    public long structuralFailureProbeSum() {
        long total = 0;
        for (int start = 0; start < capacity; start++) {
            int count = 0;
            for (int i = 0; i < capacity; i++) {
                int address = (int) Math.floorMod((long) start + (long) i, (long) capacity);
                count++;
                if (state[address] == EMPTY) {
                    break;
                }
            }
            total += count;
        }
        return total;
    }

    /**
     * @return 「连续占用块」式失败探测的平均值(= structuralFailureProbeSum / m)
     */
    public double averageStructuralFailureProbes() {
        return (double) structuralFailureProbeSum() / capacity;
    }

    /**
     * 经验测得的失败查找平均探测次数:对给定的、<b>不在表中</b>的关键字,
     * 按它们各自的 (H1, H2) 序列探测到第一个空单元(或序列闭合)为止,取平均。
     * 这是与真实查找行为一致的度量,可与 {@link #randomProbingUnsuccessfulProbes()} 对照。
     *
     * @param absentKeys 一组不在表中的关键字;为空时返回 0
     * @return 平均探测次数
     */
    public double averageUnsuccessfulProbes(long[] absentKeys) {
        Objects.requireNonNull(absentKeys, "关键字数组不能为 null");
        if (absentKeys.length == 0) {
            return 0.0;
        }
        long total = 0;
        for (long key : absentKeys) {
            int home = hashAddress(key);
            int step = stepOf(key);
            int probes = 0;
            for (int i = 0; i < capacity; i++) {
                int address = addressAt(home, step, i);
                if (i > 0 && address == home) {
                    break;
                }
                probes++;
                if (state[address] == EMPTY) {
                    break;
                }
            }
            total += probes;
        }
        return (double) total / absentKeys.length;
    }

    /**
     * @return 查找成功的平均查找长度
     */
    public double averageSuccessfulProbes() {
        return n == 0 ? 0.0 : (double) successfulProbeSum() / n;
    }

    /**
     * @return 随机探测理论值:ASL成功 ≈ ln(1/(1-α)) / α(双重散列最接近这一模型)
     */
    public double randomProbingSuccessfulProbes() {
        double alpha = loadFactor();
        if (alpha <= 0.0) {
            return 1.0;
        }
        if (alpha >= 1.0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.log(1.0 / (1.0 - alpha)) / alpha;
    }

    /**
     * @return 随机探测理论值:ASL失败 ≈ 1/(1-α)
     */
    public double randomProbingUnsuccessfulProbes() {
        double alpha = loadFactor();
        return alpha >= 1.0 ? Double.POSITIVE_INFINITY : 1.0 / (1.0 - alpha);
    }

    /**
     * @return 所有关键字(按数组下标升序)
     */
    public Iterable<Long> keys() {
        List<Long> result = new ArrayList<Long>(n);
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED) {
                result.add(slotKeys[i]);
            }
        }
        return result;
    }

    /**
     * 按数组下标升序输出所有键值对。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        String sep = "";
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED) {
                sb.append(sep).append(slotKeys[i]).append(' ').append(slotValues[i]);
                sep = ", ";
            }
        }
        return sb.append('}').toString();
    }

    // ---------------- 内部实现 ----------------

    private static HashFunction defaultH1() {
        return new HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, (long) capacity);
            }
        };
    }

    /** 经典选择:H2(key) = 1 + (key mod (m-1)),m 取素数时它与 m 必然互素 */
    private static HashFunction defaultH2() {
        return new HashFunction() {
            public int hash(long key, int capacity) {
                return 1 + (int) Math.floorMod(key, (long) (capacity - 1));
            }
        };
    }

    private void allocate(int newCapacity) {
        this.capacity = newCapacity;
        this.slotKeys = new long[newCapacity];
        this.slotValues = new Object[newCapacity];
        this.state = new byte[newCapacity];
    }

    private int addressAt(int home, int step, int i) {
        return (int) Math.floorMod((long) home + (long) i * step, (long) capacity);
    }

    private boolean insert(long key, Value value) {
        int home = hashAddress(key);
        int step = stepOf(key);
        int firstTombstone = -1;
        int probes = 0;
        for (int i = 0; i < capacity; i++) {
            int address = addressAt(home, step, i);
            if (i > 0 && address == home) {
                break;
            }
            probes++;
            if (state[address] == EMPTY) {
                int target = firstTombstone >= 0 ? firstTombstone : address;
                if (firstTombstone >= 0) {
                    tombstones--;
                }
                slotKeys[target] = key;
                slotValues[target] = value;
                state[target] = OCCUPIED;
                n++;
                lastProbes = probes;
                return true;
            }
            if (state[address] == TOMBSTONE) {
                if (firstTombstone < 0) {
                    firstTombstone = address;
                }
            } else if (slotKeys[address] == key) {
                slotValues[address] = value;
                lastProbes = probes;
                return true;
            }
        }
        if (firstTombstone >= 0) {
            slotKeys[firstTombstone] = key;
            slotValues[firstTombstone] = value;
            state[firstTombstone] = OCCUPIED;
            tombstones--;
            n++;
            lastProbes = probes;
            return true;
        }
        lastProbes = probes;
        return false;
    }

    /** 扩容到不小于 2 倍旧容量的素数(保持 H2 与 m 互素的前提),只搬活元素 */
    private boolean grow() {
        long target = (long) capacity * 2;
        if (target > MAX_CAPACITY) {
            target = MAX_CAPACITY;
        }
        int newCapacity = OpenAddressHashST.nextPrime((int) target);
        if (newCapacity <= capacity || newCapacity > MAX_CAPACITY) {
            return false;
        }
        long[] oldKeys = slotKeys;
        Object[] oldValues = slotValues;
        byte[] oldState = state;
        int oldCapacity = capacity;

        allocate(newCapacity);
        n = 0;
        tombstones = 0;
        for (int i = 0; i < oldCapacity; i++) {
            if (oldState[i] == OCCUPIED) {
                insert(oldKeys[i], castValue(oldValues[i]));
            }
        }
        resizeCount++;
        return true;
    }

    @SuppressWarnings("unchecked")
    private Value castValue(Object raw) {
        return (Value) raw;
    }

    private int find(long key) {
        int home = hashAddress(key);
        int step = stepOf(key);
        for (int i = 0; i < capacity; i++) {
            int address = addressAt(home, step, i);
            if (i > 0 && address == home) {
                break;
            }
            if (state[address] == EMPTY) {
                lastProbes = i + 1;
                return -1;
            }
            if (state[address] == OCCUPIED && slotKeys[address] == key) {
                lastProbes = i + 1;
                return address;
            }
        }
        lastProbes = capacity;
        return -1;
    }

    private int probeCount(long key) {
        int home = hashAddress(key);
        int step = stepOf(key);
        for (int i = 0; i < capacity; i++) {
            int address = addressAt(home, step, i);
            if (i > 0 && address == home) {
                break;
            }
            if (state[address] == EMPTY) {
                return i + 1;
            }
            if (state[address] == OCCUPIED && slotKeys[address] == key) {
                return i + 1;
            }
        }
        return capacity;
    }

    private static boolean isPrime(int value) {
        if (value < 2) {
            return false;
        }
        for (int d = 2; (long) d * d <= value; d++) {
            if (value % d == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 演示:与二次探测的序列对比(次级聚集)、覆盖性、ASL 与随机探测理论值、扩容与墓碑。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("开放地址法 + 双重散列:H_i = (H1(key) + i*H2(key)) mod m");
        System.out.println();

        int m = 11;
        DoubleHashingHashST<String> dh = new DoubleHashingHashST<String>(m, 1.0);
        QuadraticProbeHashST<String> qp = new QuadraticProbeHashST<String>(m, 1.0);
        System.out.println("用例 1:同起点关键字的探测序列对比(表长 m = " + m + ",H1 = key mod 11)");
        System.out.println("  关键字   H1   H2(双重)  双重散列序列(前 6 个)        二次探测序列(前 6 个)");
        long[] sameHome = {0, 11, 22, 33};
        for (long key : sameHome) {
            int[] seq = dh.probeSequence(key);
            int[] qseq = qp.probeSequence(dh.hashAddress(key));
            System.out.printf("  %-7d  %-4d %-9d %-32s %s%n", key, dh.hashAddress(key), dh.stepOf(key),
                    head(seq, 6), head(qseq, 6));
        }
        if (sameHome.length > 1) {
            System.out.println("  关键字 0 与 11 的 H1 相同,但双重散列的 H2 不同 → 序列不同;"
                    + "二次探测只有 H1 参数 → 序列完全一样(次级聚集)");
        }
        System.out.println();

        System.out.println("用例 2:覆盖性(序列长度 / m)");
        System.out.println("  表长 m   是否素数   覆盖槽位数");
        for (int size : new int[]{7, 9, 11, 13}) {
            DoubleHashingHashST<String> st = new DoubleHashingHashST<String>(size, 1.0);
            System.out.printf("  %-8d %-10s %s%n", size, st.fullCoverageGuaranteed() ? "是" : "否",
                    st.probeSequence(5).length + " / " + size);
        }
        DoubleHashingHashST<String> badStep = new DoubleHashingHashST<String>(9, 1.0,
                new DoubleHashingHashST.HashFunction() {
                    public int hash(long key, int capacity) {
                        return (int) Math.floorMod(key, (long) capacity);
                    }
                },
                new DoubleHashingHashST.HashFunction() {
                    public int hash(long key, int capacity) {
                        return 3; // gcd(3, 9) = 3 → 只能覆盖 3 个槽位
                    }
                });
        System.out.println("  m = 9 且 H2 恒为 3(gcd(3,9)=3)→ 覆盖 " + badStep.probeSequence(1).length
                + " / 9,此时会出现'有空位却插不进'");
        System.out.println();

        System.out.println("用例 3:失败查找代价 —— 结构度量、经验实测与随机探测理论值");
        System.out.println("  (容量 101,固定种子关键字;结构度量 = 连续占用块口径,只有它可跨实现对比)");
        System.out.println("  元素数   α      双重·结构度量   线性·结构度量   二次·结构度量   双重·经验ASL   理论1/(1-α)");
        java.util.Random random = new java.util.Random(20261002L);
        long[] keys = new long[90];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = random.nextInt(1000000);
        }
        long[] absent = new long[2000];
        for (int i = 0; i < absent.length; i++) {
            absent[i] = 2000000L + random.nextInt(1000000); // 与 keys 不同区间,保证缺席
        }
        for (int count : new int[]{10, 30, 50, 70}) {
            DoubleHashingHashST<String> doubleHash = new DoubleHashingHashST<String>(101, 1.0);
            OpenAddressHashST<String> linear = new OpenAddressHashST<String>(101, 1.0);
            QuadraticProbeHashST<String> quad = new QuadraticProbeHashST<String>(101, 1.0);
            for (int i = 0; i < count; i++) {
                doubleHash.put(keys[i], "v" + i);
                linear.put(keys[i], "v" + i);
                quad.put(keys[i], "v" + i);
            }
            System.out.printf("  %-8d %-6.2f %-15.3f %-15.3f %-15.3f %-14.3f %.3f%n",
                    count, doubleHash.loadFactor(),
                    doubleHash.averageStructuralFailureProbes(), linear.averageUnsuccessfulProbes(),
                    quad.averageUnsuccessfulProbes(),
                    doubleHash.averageUnsuccessfulProbes(absent),
                    doubleHash.randomProbingUnsuccessfulProbes());
        }
        System.out.println("  经验 ASL 与理论 1/(1-α) 同数量级且更小 —— 这正是双重散列接近随机探测的表现");
        System.out.println();

        System.out.println("用例 4:扩容保持素数 + 墓碑");
        DoubleHashingHashST<String> growing = new DoubleHashingHashST<String>();
        StringBuilder path = new StringBuilder("  初始 m = " + growing.capacity());
        int last = growing.capacity();
        for (long i = 0; i < 300; i++) {
            growing.put(i * 1000003L, "v" + i);
            if (growing.capacity() != last) {
                path.append(" → ").append(growing.capacity());
                last = growing.capacity();
            }
        }
        System.out.println(path);
        System.out.printf("  最终:size = %d,m = %d,α = %.4f,扩容 %d 次,全表覆盖 = %s%n",
                growing.size(), growing.capacity(), growing.loadFactor(), growing.resizeCount(),
                growing.fullCoverageGuaranteed());

        DoubleHashingHashST<String> tomb = new DoubleHashingHashST<String>(11, 1.0);
        long[] tombKeys = {0, 11, 22};
        StringBuilder slots = new StringBuilder();
        for (long key : tombKeys) {
            tomb.put(key, "v" + key);
            slots.append(key).append("→槽位").append(tomb.slotOf(key)).append("  ");
        }
        System.out.println("  三个键 H1 都是 0,但 H2 不同:" + slots.toString().trim());
        tomb.delete(0);
        System.out.println("  删除 0 后:墓碑 = " + tomb.tombstones() + ",contains(22) = " + tomb.contains(22L));
        tomb.put(33, "v33");
        System.out.println("  插入 33 后:墓碑 = " + tomb.tombstones() + ",size = " + tomb.size()
                + ",slotOf(33) = " + tomb.slotOf(33));
    }

    private static String head(int[] values, int limit) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(values.length, limit); i++) {
            sb.append(i == 0 ? "" : " ").append(values[i]);
        }
        return sb.toString();
    }
}
