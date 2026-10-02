package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 开放地址法(Open Addressing)散列表,用<b>二次探测</b>处理冲突。
 *
 * <p>本类支持两种教材常见变体:</p>
 * <pre>
 *   PLUS_I_SQUARE : H_i = (H(key) + i^2) mod m                    i = 0,1,2,…
 *   ALTERNATING   : H_i = (H(key) + 0, +1^2, -1^2, +2^2, -2^2, …) mod m
 * </pre>
 *
 * <p><b>为什么要专门讲二次探测</b>:它解决了线性探测的"初级聚集"(不同初始地址的关键字也会
 * 连成一长串),但仍有两件事必须知道:</p>
 * <ol>
 *   <li><b>探测序列可能覆盖不全</b>:PLUS_I_SQUARE 下 i² 与 (m-i)² 同余,因此表长 m 为素数时
 *       一条序列最多只覆盖 ⌈m/2⌉ 个槽位 —— 于是会出现"表里还有空位,但这个关键字怎么也探测不到"的
 *       情况,插入直接失败。这也是二次探测的装填因子必须压得更低(通常 ≤ 0.5)的原因。</li>
 *   <li><b>交替变体可覆盖全表,但有前提</b>:当 m 是素数且 m ≡ 3 (mod 4) 时,
 *       ALTERNATING 的序列 ±1², ±2², … 恰好遍历全部 m 个槽位;
 *       m ≡ 1 (mod 4) 时这个保证就没了(例如 m = 13 只能覆盖 5 个槽位)。
 *       本类用 {@link #fullCoverageGuaranteed()} 把这个前提直接暴露出来,
 *       并在该模式下扩容时挑"≥ 2 倍旧容量的最小 3 mod 4 素数"。</li>
 *   <li>另有<b>次级聚集</b>:初始地址相同的关键字走同一条探测序列(可用 {@link #probeSequence(int)} 观察),
 *       这是二次探测相对于"双散列"仍存在的弱点。</li>
 * </ol>
 *
 * <p>其余约定与前一个线性探测实现一致:删除留墓碑(否则会截断探测序列,使后续关键字查不到)、
 * 插入优先复用探测路径上第一个墓碑、α 超过上限时扩容到不小于 2 倍旧容量的素数并只搬活元素;
 * 散列函数可插拔。区别在于:插入时若整条探测序列走完都没有空位/墓碑,就会失败 —— 即使表并未填满。</p>
 *
 * <p>本类不支持 null 值;不是线程安全的。</p>
 *
 * @param <Value> 关键字关联的值类型
 * @see OpenAddressHashST 线性探测版本,可对照 ASL 与聚集
 */
public class QuadraticProbeHashST<Value> {

    /** 探测方式 */
    public enum Mode {
        /** H_i = (H(key) + i²) mod m */
        PLUS_I_SQUARE,
        /** H_i = (H(key) + 0, +1², −1², +2², −2², …) mod m */
        ALTERNATING
    }

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

    /** 默认装填因子上限:二次探测应比线性探测更保守 */
    private static final double DEFAULT_MAX_LOAD_FACTOR = 0.5;

    /** 容量上限 2^26 */
    private static final int MAX_CAPACITY = 1 << 26;

    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;
    private static final byte TOMBSTONE = 2;

    private final Mode mode;
    private final HashFunction hashFunction;
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
     * 默认构造:容量 17、PLUS_I_SQUARE 探测、α 上限 0.5。
     */
    public QuadraticProbeHashST() {
        this(DEFAULT_CAPACITY, DEFAULT_MAX_LOAD_FACTOR, defaultHashFunction(), Mode.PLUS_I_SQUARE);
    }

    /**
     * 指定初始容量,其余取默认值。
     *
     * @param initialCapacity 初始容量,至少 2
     */
    public QuadraticProbeHashST(int initialCapacity) {
        this(initialCapacity, DEFAULT_MAX_LOAD_FACTOR, defaultHashFunction(), Mode.PLUS_I_SQUARE);
    }

    /**
     * 指定初始容量与装填因子上限。
     *
     * @param initialCapacity 初始容量,至少 2
     * @param maxLoadFactor   装填因子上限,取值 (0, 1]
     */
    public QuadraticProbeHashST(int initialCapacity, double maxLoadFactor) {
        this(initialCapacity, maxLoadFactor, defaultHashFunction(), Mode.PLUS_I_SQUARE);
    }

    /**
     * 指定探测方式。
     *
     * @param initialCapacity 初始容量,至少 2
     * @param maxLoadFactor   装填因子上限,取值 (0, 1]
     * @param mode            探测方式
     */
    public QuadraticProbeHashST(int initialCapacity, double maxLoadFactor, Mode mode) {
        this(initialCapacity, maxLoadFactor, defaultHashFunction(), mode);
    }

    /**
     * 完整构造。
     *
     * @param initialCapacity 初始容量,至少 2
     * @param maxLoadFactor   装填因子上限,取值 (0, 1]
     * @param hashFunction    散列函数,不能为 null
     * @param mode            探测方式,不能为 null
     * @throws IllegalArgumentException 参数非法
     */
    public QuadraticProbeHashST(int initialCapacity, double maxLoadFactor,
                                HashFunction hashFunction, Mode mode) {
        if (initialCapacity < 2 || initialCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("初始容量必须在 [2, " + MAX_CAPACITY
                    + "] 内,当前为 " + initialCapacity);
        }
        if (maxLoadFactor <= 0.0 || maxLoadFactor > 1.0) {
            throw new IllegalArgumentException("装填因子上限必须在 (0, 1] 内,当前为 " + maxLoadFactor);
        }
        this.hashFunction = Objects.requireNonNull(hashFunction, "散列函数不能为 null");
        this.mode = Objects.requireNonNull(mode, "探测方式不能为 null");
        this.maxLoadFactor = maxLoadFactor;
        allocate(initialCapacity);
    }

    /**
     * 插入或更新键值对。
     *
     * <p>注意与线性探测的差别:若整条二次探测序列走完都没有空位或墓碑,就会失败 ——
     * 即使表中其实还有空位。允许扩容时会先扩容再重试,否则抛 {@link IllegalStateException}。</p>
     *
     * @param key   关键字
     * @param value 值,不能为 null
     * @throws IllegalStateException 探测序列走完仍无空位,且无法扩容
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
                    + ",墓碑 = " + tombstones + "):二次探测序列覆盖不全,请扩容或改用 ALTERNATING 模式");
        }
        if (!grow() || !insert(key, value)) {
            throw new IllegalStateException("已达容量上限 " + MAX_CAPACITY + ",无法插入关键字 " + key);
        }
    }

    /**
     * 查找关键字对应的值(探测到空位即停;墓碑视为非空继续探测)。
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
     * 删除关键字:被删位置留墓碑,否则同一条探测序列上的后续关键字会查不到。
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
     * @return 探测方式
     */
    public Mode mode() {
        return mode;
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
     * 当前 (探测方式, 表长) 能否保证探测序列覆盖整张表。
     *
     * <p>结论:ALTERNATING + m 为素数 + m ≡ 3 (mod 4) 时可以;其它情况不保证
     * (例如 PLUS_I_SQUARE 下素数表长最多覆盖 ⌈m/2⌉ 个槽位,m = 13 的 ALTERNATING 只覆盖 5 个)。</p>
     *
     * @return 是否保证全表覆盖
     */
    public boolean fullCoverageGuaranteed() {
        return mode == Mode.ALTERNATING && isPrime(capacity) && capacity % 4 == 3;
    }

    /**
     * 计算关键字当前的散列地址(探测序列起点)。
     *
     * @param key 关键字
     * @return 地址,落在 [0, capacity)
     * @throws IllegalStateException 散列函数返回越界地址
     */
    public int hashAddress(long key) {
        int address = hashFunction.hash(key, capacity);
        if (address < 0 || address >= capacity) {
            throw new IllegalStateException("散列函数返回了越界地址:" + address + "(表长 " + capacity + ")");
        }
        return address;
    }

    /**
     * 从给定起点出发的<b>去重后</b>探测序列(按访问顺序),直到序列回到起点(轨道走完)。
     * 可用它直接观察"覆盖不全"与"相同起点走同一序列"这两个现象。
     *
     * @param home 探测起点
     * @return 依次访问到的不同地址
     * @throws IllegalArgumentException 起点越界
     */
    public int[] probeSequence(int home) {
        if (home < 0 || home >= capacity) {
            throw new IllegalArgumentException("起点必须在 [0, " + (capacity - 1) + "] 内,当前为 " + home);
        }
        List<Integer> sequence = new ArrayList<Integer>();
        for (int i = 0; i < capacity; i++) {
            int address = probe(home, i);
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
     * @return 最长连续占用块长度(墓碑也算占用;线性探测的初级聚集指标,二次探测下通常很小)
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
     * 查找成功时的总探测次数(ASL成功公式的分子)。
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
     * 查找失败时的总探测次数(ASL失败公式的分子)。
     *
     * <p>口径(与前一个线性探测实现保持一致,便于横向对比):对每个起始地址 s,
     * 沿它的探测序列前进直到遇到空单元,计数包含"判定为空"的那一次比较;
     * 墓碑视为非空;整条序列走完仍无空位时以序列长度兜底。</p>
     *
     * @return 各起点失败查找所需探测次数之和
     */
    public long unsuccessfulProbeSum() {
        long total = 0;
        for (int start = 0; start < capacity; start++) {
            int count = 0;
            for (int i = 0; i < capacity; i++) {
                int address = probe(start, i);
                if (i > 0 && address == start) {
                    break;
                }
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
     * @return 查找成功的平均查找长度(以元素个数为分母)
     */
    public double averageSuccessfulProbes() {
        return n == 0 ? 0.0 : (double) successfulProbeSum() / n;
    }

    /**
     * @return 查找失败的平均查找长度(以表长为分母)
     */
    public double averageUnsuccessfulProbes() {
        return (double) unsuccessfulProbeSum() / capacity;
    }

    /**
     * @return 随机探测(理想均匀)参考值:ASL成功 ≈ ln(1/(1-α)) / α
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
     * @return 随机探测(理想均匀)参考值:ASL失败 ≈ 1/(1-α)
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

    // ---------------- 静态工具 ----------------

    /**
     * 不小于 n 的最小素数。
     *
     * @param n 下界,至少 2
     * @return 不小于 n 的最小素数
     * @throws IllegalArgumentException n 小于 2
     */
    public static int nextPrime(int n) {
        if (n < 2) {
            throw new IllegalArgumentException("不存在不小于 " + n + " 的素数");
        }
        int candidate = n;
        while (!isPrime(candidate)) {
            candidate++;
        }
        return candidate;
    }

    /**
     * 不小于 n 的最小素数 p 且 p ≡ 3 (mod 4)(ALTERNATING 模式保证全表覆盖的前提)。
     *
     * @param n 下界
     * @return 满足条件的素数;n ≤ 3 时返回 3
     * @throws IllegalArgumentException n 小于 2
     */
    public static int nextPrime3Mod4(int n) {
        if (n < 2) {
            throw new IllegalArgumentException("不存在不小于 " + n + " 的素数");
        }
        int candidate = Math.max(3, n);
        while (!(isPrime(candidate) && candidate % 4 == 3)) {
            candidate++;
        }
        return candidate;
    }

    // ---------------- 内部实现 ----------------

    private static HashFunction defaultHashFunction() {
        return new HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, (long) capacity);
            }
        };
    }

    private void allocate(int newCapacity) {
        this.capacity = newCapacity;
        this.slotKeys = new long[newCapacity];
        this.slotValues = new Object[newCapacity];
        this.state = new byte[newCapacity];
    }

    /**
     * 第 i 步探测地址。i = 0 时就是 H(key);
     * PLUS_I_SQUARE:(H + i²) mod m;ALTERNATING:(H + 0, +1², −1², +2², −2², …) mod m。
     */
    private int probe(int home, int i) {
        if (i == 0) {
            return home;
        }
        long step;
        if (mode == Mode.PLUS_I_SQUARE) {
            step = (long) i * i;
        } else {
            long k = (i + 1) / 2;
            step = k * k;
            if (i % 2 == 0) {
                step = -step;
            }
        }
        return (int) Math.floorMod((long) home + step, (long) capacity);
    }

    /**
     * 在当前表内插入;返回 false 表示整条探测序列走完都没有空位或墓碑。
     */
    private boolean insert(long key, Value value) {
        int home = hashAddress(key);
        int firstTombstone = -1;
        int probes = 0;
        for (int i = 0; i < capacity; i++) {
            int address = probe(home, i);
            if (i > 0 && address == home) {
                break; // 轨道走完
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

    /**
     * 扩容:PLUS_I_SQUARE 取不小于 2 倍旧容量的最小素数;
     * ALTERNATING 取不小于 2 倍旧容量的最小"3 mod 4 素数",以维持全表覆盖的前提。
     * 重建时只搬活元素,墓碑全部丢弃。
     */
    private boolean grow() {
        long target = (long) capacity * 2;
        if (target > MAX_CAPACITY) {
            target = MAX_CAPACITY;
        }
        int newCapacity = mode == Mode.ALTERNATING
                ? nextPrime3Mod4((int) target)
                : nextPrime((int) target);
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
        for (int i = 0; i < capacity; i++) {
            int address = probe(home, i);
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
        for (int i = 0; i < capacity; i++) {
            int address = probe(home, i);
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
     * 演示:探测序列的覆盖范围、'有空位却插不进'的坑、与线性探测的对比、扩容与墓碑。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("开放地址法 + 二次探测:PLUS_I_SQUARE = (H + i²) mod m;ALTERNATING = (H + 0, ±1², ±2², …) mod m");
        System.out.println();

        System.out.println("用例 1:探测序列的覆盖范围(从起点 0 出发,去重后的不同槽位数)");
        System.out.println("  表长 m   模式            覆盖槽位数 / m    是否保证全表");
        int[] sizes = {7, 11, 13, 17};
        for (int m : sizes) {
            QuadraticProbeHashST<String> plus = new QuadraticProbeHashST<String>(m, 1.0, QuadraticProbeHashST.Mode.PLUS_I_SQUARE);
            QuadraticProbeHashST<String> alt = new QuadraticProbeHashST<String>(m, 1.0, QuadraticProbeHashST.Mode.ALTERNATING);
            System.out.printf("  %-8d PLUS_I_SQUARE   %-17s %s%n", m,
                    plus.probeSequence(0).length + " / " + m, plus.fullCoverageGuaranteed() ? "是" : "否");
            System.out.printf("  %-8s ALTERNATING     %-17s %s%n", "",
                    alt.probeSequence(0).length + " / " + m, alt.fullCoverageGuaranteed() ? "是" : "否");
        }
        System.out.println("  结论:PLUS 在素数表长下只覆盖 (m+1)/2 个槽位;ALTERNATING 仅当 m ≡ 3 (mod 4) 时覆盖全表");
        System.out.println();

        System.out.println("用例 2:'表里明明有空位,却怎么也插不进去'(m = 7)");
        for (QuadraticProbeHashST.Mode mode : QuadraticProbeHashST.Mode.values()) {
            QuadraticProbeHashST<String> st = new QuadraticProbeHashST<String>(7, 1.0, mode);
            long[] occupy = {0, 1, 2, 4}; // 恰好占满起点 0 的探测轨道(PLUS 下为 {0,1,2,4})
            for (long key : occupy) {
                st.put(key, "v" + key);
            }
            String verdict;
            try {
                st.put(7, "v7"); // 7 mod 7 = 0,起点 0
                verdict = "插入成功,落在槽位 " + st.slotOf(7);
            } catch (IllegalStateException e) {
                verdict = "插入失败(探测序列覆盖不全)";
            }
            System.out.println("  模式 " + mode + ":size = " + st.size() + "/7(还有 "
                    + (st.capacity() - st.size()) + " 个空位)→ " + verdict);
        }
        System.out.println();

        System.out.println("用例 3:与线性探测对比(容量 101、α 上限 1.0、同一批固定种子随机关键字)");
        System.out.println("  元素数    α      二次探测ASL失败   线性探测ASL失败   二次最长连续块   线性最长连续块");
        java.util.Random random = new java.util.Random(20261002L);
        long[] keys = new long[90];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = random.nextInt(1000000);
        }
        for (int count : new int[]{10, 30, 50, 70}) {
            OpenAddressHashST<String> linear = new OpenAddressHashST<String>(101, 1.0);
            QuadraticProbeHashST<String> quad = new QuadraticProbeHashST<String>(101, 1.0);
            for (int i = 0; i < count; i++) {
                linear.put(keys[i], "v" + i);
                quad.put(keys[i], "v" + i);
            }
            System.out.printf("  %-8d  %-6.2f %-17.3f %-17.3f %-17d %d%n",
                    count, quad.loadFactor(), quad.averageUnsuccessfulProbes(),
                    linear.averageUnsuccessfulProbes(), quad.maxClusterLength(), linear.maxClusterLength());
        }
        System.out.println("  可见二次探测把'初级聚集'(最长连续块)压下来了,但仍存在'次级聚集'");
        System.out.println();

        System.out.println("用例 4:扩容(ALTERNATING 保持 m ≡ 3 mod 4;PLUS 取普通素数)");
        for (QuadraticProbeHashST.Mode mode : QuadraticProbeHashST.Mode.values()) {
            QuadraticProbeHashST<String> st = new QuadraticProbeHashST<String>(7, 0.5, mode);
            StringBuilder path = new StringBuilder("  " + mode + ":m = " + st.capacity());
            int lastCapacity = st.capacity();
            for (long i = 0; i < 200; i++) {
                st.put(i * 1000003L, "v" + i);
                if (st.capacity() != lastCapacity) {
                    path.append(" → ").append(st.capacity()).append(" (").append(st.capacity() % 4).append(" mod 4)");
                    lastCapacity = st.capacity();
                }
            }
            System.out.println(path);
        }
        System.out.println();

        System.out.println("用例 5:墓碑删除(容量 11、ALTERNATING、起点 0 的三个关键字)");
        QuadraticProbeHashST<String> tomb = new QuadraticProbeHashST<String>(11, 1.0, QuadraticProbeHashST.Mode.ALTERNATING);
        long[] tombKeys = {0, 11, 22};
        StringBuilder slots = new StringBuilder();
        for (long key : tombKeys) {
            tomb.put(key, "v" + key); // 11 的倍数 → 起点都是 0
            slots.append(key).append("→槽位").append(tomb.slotOf(key)).append("  ");
        }
        System.out.println("  三个键都散列到 0:" + slots.toString().trim());
        tomb.delete(0);
        System.out.println("  删除 0 后:墓碑 = " + tomb.tombstones() + ",contains(22) = " + tomb.contains(22L) + "(序列未断)");
        tomb.put(33, "v33");
        System.out.println("  插入 33 后:墓碑 = " + tomb.tombstones() + ",size = " + tomb.size()
                + ",slotOf(33) = " + tomb.slotOf(33));
    }
}
