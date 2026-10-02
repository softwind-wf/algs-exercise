package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 开放地址法(Open Addressing)散列表,用<b>线性探测</b>处理冲突。
 *
 * <p>开放地址法的特点:所有元素都存在表内(不开链),发生冲突时按某个探测序列在表内另找空位。
 * 线性探测再散列的探测序列为</p>
 * <pre>
 *     H_i = ( H(key) + i ) mod m ,  i = 0, 1, 2, …, m-1
 * </pre>
 *
 * <p>三个必须记住的结论:</p>
 * <ol>
 *   <li><b>删除不能直接置空</b>:被删位置必须留"墓碑",否则会截断后面关键字的探测链,
 *       使本来存在的元素查不到。墓碑在插入时优先复用;扩容重建时直接丢弃。</li>
 *   <li><b>装填因子 α = n/m 决定性能</b>:线性探测的教材近似式是
 *       ASL成功 ≈ ½(1 + 1/(1-α)),ASL失败 ≈ ½(1 + 1/(1-α)²)。α 接近 1 时性能急剧恶化
 *       (α=0.9 时 ASL失败约 50),所以实际实现要在 α 超过阈值时<b>扩容再散列</b>。</li>
 *   <li><b>线性探测会产生初级聚集</b>:连续占用块越连越长,后续关键字的探测距离也随之变长。
 *       本类提供 {@link #maxClusterLength()} / {@link #clusterCount()} 直接观察这一现象。</li>
 * </ol>
 *
 * <p>本类的散列函数是<b>可插拔</b>的({@link HashFunction}),默认取 {@code key mod capacity}。
 * 之所以单独抽出来,是因为开放地址法与拉链法的差别只在"冲突怎么处理",散列函数可以任意替换:
 * 例如做教材例题(表长 m = 16、散列函数 H(key) = key mod 13)时,传入一个忽略容量的函数即可。</p>
 *
 * <p>容量策略:默认初始容量 17(素数),装填因子超过 {@code maxLoadFactor}(默认 0.75)时
 * 扩容到"不小于 2 倍旧容量的素数";把 {@code maxLoadFactor} 设为 ≥ 1 表示<b>不自动扩容</b>,
 * 此时表满(无空位且无墓碑可复用)会抛 {@link IllegalStateException}。</p>
 *
 * <p>本类不支持 null 值(null 用于表示空位);不是线程安全的;</p>
 *
 * @param <Value> 关键字关联的值类型
 * @see DivisionHashST 定长表上的线性探测(散列函数为除留余数法)
 */
public class OpenAddressHashST<Value> {

    /** 可插拔的散列函数:返回 [0, capacity) 内的地址 */
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

    /** 容量上限 2^26,避免误算超大数组 */
    private static final int MAX_CAPACITY = 1 << 26;

    /** 槽位状态:空 */
    private static final byte EMPTY = 0;

    /** 槽位状态:占用 */
    private static final byte OCCUPIED = 1;

    /** 槽位状态:墓碑 */
    private static final byte TOMBSTONE = 2;

    /** 散列函数 */
    private final HashFunction hashFunction;

    /** 装填因子上限;≥ 1 表示不自动扩容 */
    private final double maxLoadFactor;

    /** 当前表长 */
    private int capacity;

    /** 槽位上的关键字 */
    private long[] slotKeys;

    /** 槽位上的值 */
    private Object[] slotValues;

    /** 槽位状态 */
    private byte[] state;

    /** 已存元素个数 */
    private int n;

    /** 墓碑个数 */
    private int tombstones;

    /** 扩容次数 */
    private int resizeCount;

    /** 最近一次操作的探测次数 */
    private int lastProbes;

    /**
     * 默认构造:容量 17、散列函数 {@code key mod capacity}、装填因子上限 0.75。
     */
    public OpenAddressHashST() {
        this(DEFAULT_CAPACITY, DEFAULT_MAX_LOAD_FACTOR);
    }

    /**
     * 指定初始容量,其余取默认值。
     *
     * @param initialCapacity 初始容量,至少 2
     */
    public OpenAddressHashST(int initialCapacity) {
        this(initialCapacity, DEFAULT_MAX_LOAD_FACTOR);
    }

    /**
     * 指定初始容量与装填因子上限。
     *
     * @param initialCapacity 初始容量,至少 2
     * @param maxLoadFactor   装填因子上限,取值 (0, 1];取 1 表示不自动扩容
     */
    public OpenAddressHashST(int initialCapacity, double maxLoadFactor) {
        this(initialCapacity, maxLoadFactor, new HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, (long) capacity);
            }
        });
    }

    /**
     * 完整构造:指定初始容量、装填因子上限与散列函数。
     *
     * @param initialCapacity 初始容量,至少 2
     * @param maxLoadFactor   装填因子上限,取值 (0, 1]
     * @param hashFunction    散列函数,不能为 null
     * @throws IllegalArgumentException 参数非法
     */
    public OpenAddressHashST(int initialCapacity, double maxLoadFactor, HashFunction hashFunction) {
        if (initialCapacity < 2 || initialCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("初始容量必须在 [2, " + MAX_CAPACITY
                    + "] 内,当前为 " + initialCapacity);
        }
        if (maxLoadFactor <= 0.0 || maxLoadFactor > 1.0) {
            throw new IllegalArgumentException("装填因子上限必须在 (0, 1] 内,当前为 " + maxLoadFactor);
        }
        this.hashFunction = Objects.requireNonNull(hashFunction, "散列函数不能为 null");
        this.maxLoadFactor = maxLoadFactor;
        this.capacity = initialCapacity;
        allocate(initialCapacity);
    }

    /**
     * 插入或更新键值对:从 H(key) 起线性探测;遇到墓碑先记住,遇到空位就落位(优先复用第一个墓碑)。
     *
     * @param key   关键字
     * @param value 值,不能为 null
     * @throws IllegalStateException 不自动扩容时表已满
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
            throw new IllegalStateException("散列表已满(" + capacity + " 个单元),无法插入关键字 " + key);
        }
        if (!grow() || !insert(key, value)) {
            throw new IllegalStateException("已达容量上限 " + MAX_CAPACITY + ",无法插入关键字 " + key);
        }
    }

    /**
     * 查找关键字对应的值(探测到空位即停;墓碑视为"非空"继续探测)。
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
     * 删除关键字:被删位置留墓碑以维持探测链。
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
     * @return 散列表为空返回 true
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
     * @return 最长连续占用块长度(线性探测的"初级聚集"度量;墓碑也算占用)
     */
    public int maxClusterLength() {
        if (n + tombstones == 0) {
            return 0;
        }
        if (n + tombstones == capacity) {
            return capacity; // 整张表连成一块
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
                if (run > max) {
                    max = run;
                }
                run = 0;
            } else {
                run++;
            }
        }
        return Math.max(max, run);
    }

    /**
     * @return 连续占用块的个数(线性探测的聚集度量)
     */
    public int clusterCount() {
        if (n + tombstones == 0) {
            return 0;
        }
        if (n + tombstones == capacity) {
            return 1;
        }
        int clusters = 0;
        int start = -1;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == EMPTY) {
                start = i;
                break;
            }
        }
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
     * 口径与其他线性探测实现一致:包含最后判定为空单元的那次比较;墓碑视为非空;满表以 m 次为上限兜底。
     *
     * @return 每个起始地址查找失败所需探测次数之和
     */
    public long unsuccessfulProbeSum() {
        long total = 0;
        for (int start = 0; start < capacity; start++) {
            int probes = 1;
            int i = start;
            while (state[i] != EMPTY) {
                if (probes == capacity) {
                    break;
                }
                probes++;
                i = (i + 1) % capacity;
            }
            total += probes;
        }
        return total;
    }

    /**
     * @return 查找成功的平均查找长度
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
     * @return 教材近似式给出的 ASL成功 = ½(1 + 1/(1-α))
     */
    public double theoreticalSuccessfulProbes() {
        double alpha = loadFactor();
        return alpha >= 1.0 ? Double.POSITIVE_INFINITY : 0.5 * (1.0 + 1.0 / (1.0 - alpha));
    }

    /**
     * @return 教材近似式给出的 ASL失败 = ½(1 + 1/(1-α)²)
     */
    public double theoreticalUnsuccessfulProbes() {
        double alpha = loadFactor();
        if (alpha >= 1.0) {
            return Double.POSITIVE_INFINITY;
        }
        double d = 1.0 - alpha;
        return 0.5 * (1.0 + 1.0 / (d * d));
    }

    /**
     * 依次返回所有关键字(按数组下标升序)。
     *
     * @return 关键字集合
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
     * 按数组下标升序输出所有键值对:{key value, key value}。
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

    /**
     * 不小于 n 的最小素数(供选择表长使用;线性探测本身不挑表长,但与取模散列配合时素数更均匀)。
     *
     * @param n 下界
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

    // ---------------- 内部实现 ----------------

    private void allocate(int newCapacity) {
        this.capacity = newCapacity;
        this.slotKeys = new long[newCapacity];
        this.slotValues = new Object[newCapacity];
        this.state = new byte[newCapacity];
    }

    /**
     * 在当前表内插入;返回 false 表示整张表没有空位也没有墓碑(表满且不可复用)。
     */
    private boolean insert(long key, Value value) {
        int i = hashAddress(key);
        int firstTombstone = -1;
        int probes = 0;
        for (int step = 0; step < capacity; step++) {
            probes++;
            if (state[i] == EMPTY) {
                int target = firstTombstone >= 0 ? firstTombstone : i;
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
            if (state[i] == TOMBSTONE) {
                if (firstTombstone < 0) {
                    firstTombstone = i;
                }
            } else if (slotKeys[i] == key) {
                slotValues[i] = value;
                lastProbes = probes;
                return true;
            }
            i = (i + 1) % capacity;
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
        return false;
    }

    /**
     * 扩容到"不小于 2 倍旧容量的素数"并重新散列:只搬活元素,墓碑全部丢弃。
     *
     * @return 是否成功扩容
     */
    private boolean grow() {
        long target = (long) capacity * 2;
        if (target > MAX_CAPACITY) {
            target = MAX_CAPACITY;
        }
        int newCapacity = nextPrime((int) target);
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

    /**
     * 计算关键字当前的散列地址(即探测序列的起点 H(key)),并校验散列函数返回值合法。
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

    private int find(long key) {
        int i = hashAddress(key);
        for (int probes = 1; probes <= capacity; probes++) {
            if (state[i] == EMPTY) {
                lastProbes = probes;
                return -1;
            }
            if (state[i] == OCCUPIED && slotKeys[i] == key) {
                lastProbes = probes;
                return i;
            }
            i = (i + 1) % capacity;
        }
        lastProbes = capacity;
        return -1;
    }

    private int probeCount(long key) {
        int i = hashAddress(key);
        for (int probes = 1; probes <= capacity; probes++) {
            if (state[i] == EMPTY) {
                return probes;
            }
            if (state[i] == OCCUPIED && slotKeys[i] == key) {
                return probes;
            }
            i = (i + 1) % capacity;
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
     * 演示:线性探测序列、经典例题、聚集与 ASL 对照、自动扩容、墓碑删除。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("开放地址法散列表 + 线性探测:H_i = (H(key) + i) mod m");
        System.out.println();

        // 用例 1:教材经典例题 —— 表长 16,H(key) = key mod 13,不自动扩容
        final long p = 13;
        OpenAddressHashST<String> classic = new OpenAddressHashST<String>(16, 1.0, new OpenAddressHashST.HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, p);
            }
        });
        int[] keys = {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79};
        System.out.println("用例 1:表长 m = 16,H(key) = key mod 13,关键字 "
                + java.util.Arrays.toString(keys));
        System.out.println("  关键字        初始地址   实际地址   探测次数");
        for (int key : keys) {
            int initial = (int) Math.floorMod(key, p);
            classic.put(key, "v" + key);
            System.out.printf("  %-12d  %-10d  %-10d  %d%n", key, initial, classic.slotOf(key), classic.lastProbes());
        }
        System.out.printf("  装填因子 = %d/16 = %.4f;ASL成功 = %d/12 = %.4f;ASL失败 = %d/16 = %.4f%n",
                classic.size(), classic.loadFactor(), classic.successfulProbeSum(),
                classic.averageSuccessfulProbes(), classic.unsuccessfulProbeSum(),
                classic.averageUnsuccessfulProbes());
        System.out.println("  (与 DivisionHashST 的经典例题结果一致,可交叉印证)");
        System.out.println();

        // 用例 2:聚集与 ASL —— 同一散列函数、不同装填因子
        System.out.println("用例 2:初级聚集与 ASL(表长 101、H(key) = key mod 101、不扩容;关键字用固定种子的伪随机数)");
        long[] probeKeys = new long[90];
        java.util.Random random = new java.util.Random(20261002L);
        for (int i = 0; i < probeKeys.length; i++) {
            probeKeys[i] = random.nextInt(1000000);
        }
        System.out.println("  装填因子 α   实测ASL成功   理论½(1+1/(1-α))   实测ASL失败   理论½(1+1/(1-α)²)   最长连续块");
        for (int count : new int[]{10, 30, 50, 70, 90}) {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(101, 1.0);
            for (int i = 0; i < count; i++) {
                st.put(probeKeys[i], "v" + i);
            }
            System.out.printf("  %-13.2f %-13.3f %-20.3f %-13.3f %-20.3f %d%n",
                    st.loadFactor(), st.averageSuccessfulProbes(), st.theoreticalSuccessfulProbes(),
                    st.averageUnsuccessfulProbes(), st.theoreticalUnsuccessfulProbes(), st.maxClusterLength());
        }
        System.out.println("  说明:教材公式是对随机散列函数的平均值,单个样本会偏离(α 越大方差越大)");
        System.out.println();

        // 用例 3:自动扩容(α 上限 0.75,容量按素数增长)
        OpenAddressHashST<String> growing = new OpenAddressHashST<String>();
        System.out.println("用例 3:自动扩容(初始容量 " + growing.capacity() + ",α 上限 " + growing.maxLoadFactor()
                + ",关键字取 i*1000003 使地址散开)");
        int lastCapacity = growing.capacity();
        for (long i = 0; i < 100; i++) {
            growing.put(i * 1000003L, "v" + i);
            if (growing.capacity() != lastCapacity) {
                System.out.printf("  插入第 %d 个元素后扩容:m = %d → %d,α = %.4f%n",
                        (int) i + 1, lastCapacity, growing.capacity(), growing.loadFactor());
                lastCapacity = growing.capacity();
            }
        }
        System.out.printf("  最终:size = %d,m = %d,α = %.4f,扩容 %d 次,ASL失败 = %.3f,最长连续块 = %d%n",
                growing.size(), growing.capacity(), growing.loadFactor(), growing.resizeCount(),
                growing.averageUnsuccessfulProbes(), growing.maxClusterLength());
        System.out.println("  对照:若用连续关键字 0..99(地址也连续),线性探测会连成一整块,ASL失败会高出一个量级");
        OpenAddressHashST<String> sequential = new OpenAddressHashST<String>();
        for (long key = 0; key < 100; key++) {
            sequential.put(key, "v" + key);
        }
        System.out.printf("        连续关键字:size = %d,m = %d,α = %.4f,ASL失败 = %.3f,最长连续块 = %d%n",
                sequential.size(), sequential.capacity(), sequential.loadFactor(),
                sequential.averageUnsuccessfulProbes(), sequential.maxClusterLength());
        System.out.println();

        // 用例 4:删除留墓碑 + 墓碑复用 + 扩容丢弃墓碑
        OpenAddressHashST<String> tomb = new OpenAddressHashST<String>(8, 1.0);
        System.out.println("用例 4:删除留墓碑(表长 8,默认散列 H(key) = key mod 8)");
        for (long key = 0; key < 6; key++) {
            tomb.put(key * 8, "v" + key); // 全部散列到地址 0
        }
        System.out.println("  6 个都散列到地址 0:落位 " + describeSlots(tomb) + ",最长连续块 = " + tomb.maxClusterLength());
        tomb.delete(8);
        System.out.println("  删除 8 后:墓碑 = " + tomb.tombstones() + ",size = " + tomb.size()
                + ",contains(40) = " + tomb.contains(40) + "(探测链未断)");
        tomb.put(48, "新"); // 48 mod 8 = 0,复用墓碑
        System.out.println("  插入 48 后:墓碑 = " + tomb.tombstones() + ",size = " + tomb.size()
                + ",slotOf(48) = " + tomb.slotOf(48));
        OpenAddressHashST<String> toGrow = new OpenAddressHashST<String>(8, 0.5);
        for (long key = 0; key < 4; key++) {
            toGrow.put(key, "v" + key); // 占 0..3,α = 0.5
        }
        toGrow.delete(0);
        toGrow.delete(1);              // 留下两个墓碑,且后续插入不再经过它们
        toGrow.put(4, "v4");
        toGrow.put(5, "v5");
        System.out.println("  扩容前:m = " + toGrow.capacity() + ",size = " + toGrow.size()
                + ",墓碑 = " + toGrow.tombstones());
        toGrow.put(6, "v6");           // α 将超过 0.5,触发扩容重建
        System.out.println("  扩容后:m = " + toGrow.capacity() + ",size = " + toGrow.size()
                + ",墓碑 = " + toGrow.tombstones() + "(重建时所有元素重新散列,墓碑被丢弃)");
    }

    private static String describeSlots(OpenAddressHashST<String> st) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < st.capacity(); i++) {
            if (st.state[i] == OCCUPIED) {
                sb.append(i).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
