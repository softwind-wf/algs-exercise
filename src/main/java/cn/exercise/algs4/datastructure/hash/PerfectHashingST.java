package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 完全散列(Perfect Hashing),即两级全域散列的 FKS 方案(Fredman-Komlós-Szemerédi,CLRS 11.5)。
 *
 * <p><b>目标</b>:对<b>已知且互不相同</b>的关键字集合,构造一个静态散列表,使查找在最坏情况下也是 O(1)
 * —— 也就是说,任何关键字的查找代价都是常数次比较,与 n 无关。</p>
 *
 * <p><b>构造方法(两级)</b>:</p>
 * <pre>
 *   第一级:m = n 个槽位,用全域散列 h1(k) = ((a1·k + b1) mod p) mod m,把 n 个关键字分成 m 个桶;
 *          反复随机选取 (a1, b1),直到 Σ n_i² ≤ 4n(否则重来,期望 ≤ 2 次即可成功)。
 *   第二级:对每个含 n_i ≥ 2 个关键字的桶 i,再用一个全域散列
 *          h_i(k) = ((a_i·k + b_i) mod p) mod n_i²,
 *          反复随机选取 (a_i, b_i),直到桶内 n_i 个关键字落到 <b>互不相同</b>的槽位上
 *          (单次尝试的碰撞概率 ≤ C(n_i,2)/n_i² < 1/2,所以期望 ≤ 2 次)。
 *          含 1 个关键字的桶不需要第二级。
 * </pre>
 *
 * <p><b>为什么这样能做到最坏 O(1)</b>:第二级无碰撞 ⇒ 桶内每个关键字独占一个槽位,
 * 查找只需"算两次散列 + 比较一次关键字" ⇒ 比较次数 ≤ 2,与 n 无关。</p>
 *
 * <p><b>空间为什么是 O(n)</b>:Σ n_i² ≤ 4n 是第一级的接收条件;而由全域性,
 * E[Σ n_i²] &lt; 2n(见 CLRS 引理 11.4),所以通常一两次尝试就能通过。
 * 总槽位数 = 第一级 n 个 + 第二级 Σ n_i² ≤ 5n,即 O(n)。</p>
 *
 * <p><b>重要限制(完全散列的代价)</b>:</p>
 * <ul>
 *   <li><b>只能静态构造</b>:关键字集合必须事先已知且互不相同,不支持 put/delete;
 *       集合变了就得整体重建(本类提供 {@link #build} 系列工厂方法)。</li>
 *   <li>需要额外空间(实测空间因子通常在 1.5～2 之间,上界 5)。</li>
 *   <li>关键字需为 [0, {@link #MAX_KEY}] 内的非负 long。</li>
 * </ul>
 *
 * <p>随机选取由 seed 驱动,因此<b>同一 seed + 同一关键字集合 ⇒ 结构完全可复现</b>;
 * 不同 seed 得到不同的 (a,b),但"最坏查找 ≤ 2 次比较"的性质始终成立。</p>
 *
 * <p>本类不支持 null 值;不是线程安全的(构建完成后只读,可安全共享,前提是外部不做可见性发布以外的事情)。</p>
 *
 * @param <Value> 关键字关联的值类型
 * @see UniversalHashST 全域散列族(本类的理论基础)
 */
public final class PerfectHashingST<Value> {

    /** 默认随机种子(保证构建可复现) */
    public static final long DEFAULT_SEED = 20261003L;

    /** 允许的最大关键字(留出安全边界,保证 a·k 不溢出 long) */
    public static final long MAX_KEY = 2000000000L;

    /** 第一级/第二级重新随机化的次数上限(正常情况远达不到) */
    private static final int MAX_ATTEMPTS = 10000;

    /** 第一级的空间接收条件:Σ n_i² ≤ SPACE_BOUND_FACTOR · n */
    private static final int SPACE_BOUND_FACTOR = 4;

    /** 2 级散列的模数:p > 所有关键字 */
    private final long prime;

    private final int n;

    /** 第一级槽位数 m = n(n = 0 时取 1,仅为避免除零) */
    private final int m;

    private final long a1;
    private final long b1;

    /** 扁平存放的关键字/值:同一个桶的关键字连续存放 */
    private final long[] flatKeys;
    private final Object[] flatValues;

    /** 每个桶在扁平数组中的区间 [bucketStart[i], bucketStart[i+1]) */
    private final int[] bucketStart;

    /** 每个桶第二级的槽位数:n_i ≥ 2 时为 n_i²,n_i == 1 时为 1,空桶为 0 */
    private final int[] subSize;

    private final long[] subA;
    private final long[] subB;

    /** 第二级槽位上的关键字/值(空槽关键字为 0、值为 null) */
    private final long[][] subKeys;
    private final Object[][] subValues;

    /** 第二级槽位总数 Σ m_i */
    private final int totalSubSlots;

    private final int level1Attempts;
    private final int level2Attempts;
    private final int maxLevel2Attempts;

    /** 最坏情况下的比较次数(命中时):≤ 2 */
    private final int worstCaseProbes;

    private int lastProbes;

    private PerfectHashingST(long prime, int n, int m, long a1, long b1, long[] flatKeys,
                             Object[] flatValues, int[] bucketStart, int[] subSize, long[] subA,
                             long[] subB, long[][] subKeys, Object[][] subValues, int totalSubSlots,
                             int level1Attempts, int level2Attempts, int maxLevel2Attempts,
                             int worstCaseProbes) {
        this.prime = prime;
        this.n = n;
        this.m = m;
        this.a1 = a1;
        this.b1 = b1;
        this.flatKeys = flatKeys;
        this.flatValues = flatValues;
        this.bucketStart = bucketStart;
        this.subSize = subSize;
        this.subA = subA;
        this.subB = subB;
        this.subKeys = subKeys;
        this.subValues = subValues;
        this.totalSubSlots = totalSubSlots;
        this.level1Attempts = level1Attempts;
        this.level2Attempts = level2Attempts;
        this.maxLevel2Attempts = maxLevel2Attempts;
        this.worstCaseProbes = worstCaseProbes;
    }

    // ---------------- 工厂方法 ----------------

    /**
     * 用默认种子构建:值取关键字本身(Long)。
     *
     * @param keys 关键字数组(互不相同、非负)
     * @return 完全散列表
     */
    public static PerfectHashingST<Long> build(long[] keys) {
        Objects.requireNonNull(keys, "关键字数组不能为 null");
        Long[] values = new Long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = keys[i];
        }
        return build(keys, values);
    }

    /**
     * 用默认种子构建。
     *
     * @param keys   关键字数组(互不相同、非负)
     * @param values 对应的值(不能含 null)
     * @param <V>    值类型
     * @return 完全散列表
     * @throws IllegalArgumentException 长度不一致、关键字重复或越界
     */
    public static <V> PerfectHashingST<V> build(long[] keys, V[] values) {
        return build(keys, values, DEFAULT_SEED);
    }

    /**
     * 指定种子构建(同一集合 + 同一种子 ⇒ 可复现的结构)。
     *
     * @param keys   关键字数组(互不相同、非负)
     * @param values 对应的值(不能含 null)
     * @param seed   随机种子
     * @param <V>    值类型
     * @return 完全散列表
     * @throws IllegalArgumentException 长度不一致、关键字重复或越界
     */
    @SuppressWarnings("unchecked")
    public static <V> PerfectHashingST<V> build(long[] keys, V[] values, long seed) {
        Objects.requireNonNull(keys, "关键字数组不能为 null");
        Objects.requireNonNull(values, "值数组不能为 null");
        if (keys.length != values.length) {
            throw new IllegalArgumentException("关键字与值的个数不一致:" + keys.length + " vs " + values.length);
        }
        int n = keys.length;
        long maxKey = 0;
        for (int i = 0; i < n; i++) {
            long key = keys[i];
            if (key < 0 || key > MAX_KEY) {
                throw new IllegalArgumentException("关键字必须在 [0, " + MAX_KEY + "] 内,当前为 " + key);
            }
            Objects.requireNonNull(values[i], "值不能为 null(关键字 " + key + ")");
            maxKey = Math.max(maxKey, key);
        }
        if (n > 1) {
            long[] sorted = keys.clone();
            Arrays.sort(sorted);
            for (int i = 1; i < n; i++) {
                if (sorted[i] == sorted[i - 1]) {
                    throw new IllegalArgumentException("完全散列要求关键字互不相同,重复关键字:" + sorted[i]);
                }
            }
        }

        long prime = nextPrime(Math.max(maxKey + 1, (long) n + 1));
        int m = Math.max(1, n);
        Random random = new Random(seed);

        // ---- 第一级:反复随机化直到 Σ n_i² ≤ 4n ----
        int[] counts = new int[m];
        int[] bucketOf = new int[n];
        long a1 = 0;
        long b1 = 0;
        int level1Attempts = 0;
        while (true) {
            level1Attempts++;
            if (level1Attempts > MAX_ATTEMPTS) {
                throw new IllegalStateException("第一级随机化 " + MAX_ATTEMPTS + " 次仍未满足空间条件");
            }
            a1 = 1 + Math.floorMod(random.nextLong(), prime - 1);
            b1 = Math.floorMod(random.nextLong(), prime);
            Arrays.fill(counts, 0);
            for (int i = 0; i < n; i++) {
                bucketOf[i] = (int) ((a1 * keys[i] + b1) % prime % m);
                counts[bucketOf[i]]++;
            }
            long subTotal = 0;
            for (int c : counts) {
                subTotal += (long) c * c;
            }
            if (subTotal <= (long) SPACE_BOUND_FACTOR * n) {
                break;
            }
        }

        // ---- 按桶顺序把关键字摊平 ----
        int[] bucketStart = new int[m + 1];
        for (int i = 0; i < m; i++) {
            bucketStart[i + 1] = bucketStart[i] + counts[i];
        }
        long[] flatKeys = new long[n];
        Object[] flatValues = new Object[n];
        int[] cursor = new int[m];
        for (int i = 0; i < n; i++) {
            int bucket = bucketOf[i];
            int position = bucketStart[bucket] + cursor[bucket]++;
            flatKeys[position] = keys[i];
            flatValues[position] = values[i];
        }

        // ---- 第二级:每个含 ≥2 个关键字的桶随机化到无碰撞 ----
        int[] subSize = new int[m];
        long[] subA = new long[m];
        long[] subB = new long[m];
        long[][] subKeys = new long[m][];
        Object[][] subValues = new Object[m][];
        int totalSubSlots = 0;
        int level2Attempts = 0;
        int maxLevel2Attempts = 0;
        int worstCaseProbes = n == 0 ? 0 : 1;

        for (int bucket = 0; bucket < m; bucket++) {
            int size = counts[bucket];
            if (size == 0) {
                continue;
            }
            if (size == 1) {
                subSize[bucket] = 1;
                totalSubSlots += 1;
                continue;
            }
            long m2 = (long) size * size;
            if (m2 > Integer.MAX_VALUE) {
                throw new IllegalStateException("第二级槽位数溢出:" + m2);
            }
            int slots = (int) m2;
            boolean[] used = new boolean[slots];
            long a = 0;
            long b = 0;
            int attempts = 0;
            boolean collisionFree = false;
            while (!collisionFree) {
                attempts++;
                level2Attempts++;
                if (attempts > MAX_ATTEMPTS) {
                    throw new IllegalStateException("桶 " + bucket + " 第二级随机化 " + MAX_ATTEMPTS + " 次仍未无碰撞");
                }
                a = 1 + Math.floorMod(random.nextLong(), prime - 1);
                b = Math.floorMod(random.nextLong(), prime);
                collisionFree = true;
                Arrays.fill(used, false);
                for (int t = bucketStart[bucket]; t < bucketStart[bucket + 1]; t++) {
                    int slot = (int) ((a * flatKeys[t] + b) % prime % slots);
                    if (used[slot]) {
                        collisionFree = false;
                        break;
                    }
                    used[slot] = true;
                }
            }
            long[] keysOfBucket = new long[slots];
            Object[] valuesOfBucket = new Object[slots];
            for (int t = bucketStart[bucket]; t < bucketStart[bucket + 1]; t++) {
                int slot = (int) ((a * flatKeys[t] + b) % prime % slots);
                keysOfBucket[slot] = flatKeys[t];
                valuesOfBucket[slot] = flatValues[t];
            }
            subSize[bucket] = slots;
            subA[bucket] = a;
            subB[bucket] = b;
            subKeys[bucket] = keysOfBucket;
            subValues[bucket] = valuesOfBucket;
            totalSubSlots += slots;
            maxLevel2Attempts = Math.max(maxLevel2Attempts, attempts);
            worstCaseProbes = Math.max(worstCaseProbes, 2);
        }

        return new PerfectHashingST<V>(prime, n, m, a1, b1, flatKeys, flatValues, bucketStart,
                subSize, subA, subB, subKeys, subValues, totalSubSlots, level1Attempts,
                level2Attempts, maxLevel2Attempts, worstCaseProbes);
    }

    // ---------------- 查找 ----------------

    /**
     * 查找关键字(最坏情况 ≤ 2 次比较,与 n 无关)。
     *
     * @param key 关键字;越界或不在集合中返回 null
     * @return 值;不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public Value get(long key) {
        if (n == 0 || key < 0 || key > MAX_KEY) {
            lastProbes = 0;
            return null;
        }
        int bucket = firstLevelAddress(key);
        int start = bucketStart[bucket];
        int size = bucketStart[bucket + 1] - start;
        if (size == 0) {
            lastProbes = 1;
            return null;
        }
        if (size == 1) {
            lastProbes = 1;
            return flatKeys[start] == key ? (Value) flatValues[start] : null;
        }
        int slot = secondLevelAddress(bucket, key);
        lastProbes = 2;
        return subKeys[bucket][slot] == key ? (Value) subValues[bucket][slot] : null;
    }

    /**
     * @param key 关键字
     * @return 是否在集合中
     */
    public boolean contains(long key) {
        return get(key) != null;
    }

    /**
     * @return 最近一次查找的比较次数(命中 ≤ 2;空桶 1)
     */
    public int lastProbes() {
        return lastProbes;
    }

    /**
     * @return 构建时测得的、所有关键字的查找比较次数最大值(≤ 2)
     */
    public int worstCaseProbes() {
        return worstCaseProbes;
    }

    // ---------------- 结构信息 ----------------

    /**
     * @return 关键字个数 n
     */
    public int size() {
        return n;
    }

    /**
     * @return 是否为空表
     */
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * @return 第一级槽位数 m(= n,n = 0 时为 1)
     */
    public int bucketCount() {
        return m;
    }

    /**
     * @return 作为模数的素数 p(大于所有关键字)
     */
    public long prime() {
        return prime;
    }

    /**
     * @return 各桶的关键字个数(长度 m)
     */
    public int[] bucketSizes() {
        int[] sizes = new int[m];
        for (int i = 0; i < m; i++) {
            sizes[i] = bucketStart[i + 1] - bucketStart[i];
        }
        return sizes;
    }

    /**
     * @param bucket 桶号
     * @return 该桶第二级的槽位数(空桶 0,单关键字桶 1,否则 n_i²)
     */
    public int subTableSize(int bucket) {
        if (bucket < 0 || bucket >= m) {
            throw new IllegalArgumentException("桶号必须在 [0, " + (m - 1) + "] 内");
        }
        return subSize[bucket];
    }

    /**
     * @return 第二级槽位总数 Σ m_i(满足 Σ m_i ≤ 4n)
     */
    public int totalSubTableSlots() {
        return totalSubSlots;
    }

    /**
     * @return 整个结构占用的槽位数:第一级 m 个 + 第二级 Σ m_i
     */
    public int totalSlots() {
        return m + totalSubSlots;
    }

    /**
     * @return 空间因子 Σ m_i / n(理论期望 &lt; 2,构造时保证 ≤ 4)
     */
    public double spaceFactor() {
        return n == 0 ? 0.0 : (double) totalSubSlots / n;
    }

    /**
     * @return 总槽位与元素个数之比(含第一级,n = 0 时为 0)
     */
    public double spaceUsageRatio() {
        return n == 0 ? 0.0 : (double) totalSlots() / n;
    }

    /**
     * @return 第一级重新随机化的次数(期望 ≤ 2)
     */
    public int level1Attempts() {
        return level1Attempts;
    }

    /**
     * @return 第二级重新随机化的总次数
     */
    public int level2Attempts() {
        return level2Attempts;
    }

    /**
     * @return 单个桶第二级重新随机化的最大次数(期望 ≤ 2)
     */
    public int maxLevel2Attempts() {
        return maxLevel2Attempts;
    }

    /**
     * 第一级散列地址 h1(k) = ((a1·k + b1) mod p) mod m。
     *
     * @param key 关键字
     * @return 桶号
     */
    public int firstLevelAddress(long key) {
        return (int) ((a1 * key + b1) % prime % m);
    }

    /**
     * 第二级散列地址 h_i(k) = ((a_i·k + b_i) mod p) mod m_i。
     *
     * @param bucket 桶号
     * @param key    关键字
     * @return 该桶内的槽位号;空桶或单关键字桶返回 0
     * @throws IllegalArgumentException 桶号越界
     */
    public int secondLevelAddress(int bucket, long key) {
        if (bucket < 0 || bucket >= m) {
            throw new IllegalArgumentException("桶号必须在 [0, " + (m - 1) + "] 内");
        }
        if (subSize[bucket] <= 1) {
            return 0;
        }
        return (int) ((subA[bucket] * key + subB[bucket]) % prime % subSize[bucket]);
    }

    /**
     * @return 所有关键字(按桶顺序)
     */
    public Iterable<Long> keys() {
        List<Long> result = new ArrayList<Long>(n);
        for (long key : flatKeys) {
            result.add(key);
        }
        return result;
    }

    /**
     * 结构摘要:第一级/第二级规模、空间因子、随机化次数、最坏比较次数。
     */
    @Override
    public String toString() {
        return "PerfectHashingST{n=" + n + ", p=" + prime + ", 第一级槽位=" + m
                + ", 第二级槽位=" + totalSubSlots + ", 空间因子=" + String.format("%.3f", spaceFactor())
                + ", 第一级尝试=" + level1Attempts + ", 第二级尝试=" + level2Attempts
                + "(最大 " + maxLevel2Attempts + "), 最坏比较=" + worstCaseProbes + "}";
    }

    // ---------------- 素数工具 ----------------

    private static long nextPrime(long value) {
        long candidate = Math.max(2, value);
        while (!isPrime(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private static boolean isPrime(long value) {
        if (value < 2) {
            return false;
        }
        for (long d = 2; d * d <= value; d++) {
            if (value % d == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 演示:构造过程、空间因子与理论对照、最坏查找代价与 n 无关、随机化次数。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("完全散列(两级 FKS):第一级 ((a1·k+b1) mod p) mod n,第二级 ((a_i·k+b_i) mod p) mod n_i²");
        System.out.println();

        System.out.println("用例 1:小例子(n = 12 的学号集合)");
        long[] students = {20230101L, 20230102L, 20230105L, 20230210L, 20230211L,
                20230333L, 20230404L, 20230505L, 20230606L, 20230707L, 20230808L, 20230909L};
        PerfectHashingST<Long> small = PerfectHashingST.build(students);
        int[] sizes = small.bucketSizes();
        System.out.println("  第一级桶大小:" + Arrays.toString(sizes));
        StringBuilder subSizes = new StringBuilder();
        for (int i = 0; i < small.bucketCount(); i++) {
            if (sizes[i] > 0) {
                subSizes.append('桶').append(i).append(':').append(small.subTableSize(i)).append("槽 ");
            }
        }
        System.out.println("  第二级槽位:" + subSizes.toString().trim());
        System.out.println("  " + small);
        System.out.println("  验证:所有关键字查找比较次数 = ");
        StringBuilder probes = new StringBuilder();
        for (long key : students) {
            small.get(key);
            probes.append(small.lastProbes()).append(' ');
        }
        System.out.println("    " + probes.toString().trim());
        System.out.println();

        System.out.println("用例 2:理论对照 —— 200 组随机关键字集合(n = 100),空间因子 Σ m_i / n");
        Random random = new Random(20261003L);
        double sum = 0;
        double max = 0;
        int maxAttempt1 = 0;
        int maxAttempt2 = 0;
        double[] factors = new double[200];
        long totalLevel2 = 0;
        for (int t = 0; t < factors.length; t++) {
            long[] keys = distinctKeys(100, random);
            PerfectHashingST<Long> table = PerfectHashingST.build(keys, valuesOf(keys), 1000L + t);
            factors[t] = table.spaceFactor();
            sum += factors[t];
            max = Math.max(max, factors[t]);
            maxAttempt1 = Math.max(maxAttempt1, table.level1Attempts());
            maxAttempt2 = Math.max(maxAttempt2, table.maxLevel2Attempts());
            totalLevel2 += table.totalSubTableSlots();
        }
        Arrays.sort(factors);
        System.out.printf("  空间因子:平均 %.3f,中位数 %.3f,最大 %.3f%n",
                sum / factors.length, factors[factors.length / 2], max);
        System.out.printf("  理论:期望 E[Σ n_i²] < 2n(即空间因子 < 2),构造阈值 4n%n");
        System.out.printf("  随机化次数上限:第一级最多 %d 次,某桶第二级最多 %d 次%n", maxAttempt1, maxAttempt2);
        System.out.printf("  200 次构建的第二级槽位合计 %d,n 合计 %d,总空间因子 %.3f%n",
                totalLevel2, 100L * factors.length, (double) totalLevel2 / (100L * factors.length));
        boolean withinBound = true;
        for (double factor : factors) {
            if (factor > 4.0) {
                withinBound = false;
            }
        }
        System.out.println("  所有 200 次构建都满足 Σ m_i ≤ 4n:" + withinBound);
        System.out.println();

        System.out.println("用例 3:最坏查找代价与 n 无关(完全散列的核心卖点)");
        System.out.println("  n        完全散列最坏比较次数   链地址法最长链   链地址法平均链长");
        for (int size : new int[]{10, 100, 1000, 10000}) {
            long[] keys = distinctKeys(size, random);
            PerfectHashingST<Long> perfect = PerfectHashingST.build(keys, valuesOf(keys), 999L);
            SeparateChainingHashST<Long> chained = new SeparateChainingHashST<Long>(
                    OpenAddressHashST.nextPrime(size), 16.0);
            for (long key : keys) {
                chained.put(key, key);
            }
            System.out.printf("  %-8d %-21d %-16d %.3f%n",
                    size, perfect.worstCaseProbes(), chained.maxChainLength(), chained.averageChainLength());
        }
        System.out.println();

        System.out.println("用例 4:空间对比(n = 1000)");
        long[] keys = distinctKeys(1000, random);
        PerfectHashingST<Long> perfect = PerfectHashingST.build(keys, valuesOf(keys), 777L);
        SeparateChainingHashST<Long> chained = new SeparateChainingHashST<Long>(1009, 16.0);
        for (long key : keys) {
            chained.put(key, key);
        }
        System.out.printf("  完全散列:第一级 %d 槽 + 第二级 %d 槽 = %d 槽(空间因子 %.3f)%n",
                perfect.bucketCount(), perfect.totalSubTableSlots(), perfect.totalSlots(),
                perfect.spaceFactor());
        System.out.printf("  链地址法:桶 %d + 结点 %d = %d 槽(最长链 %d)%n",
                chained.capacity(), chained.size(), chained.capacity() + chained.size(),
                chained.maxChainLength());
        System.out.printf("  完全散列用 %.2f 倍空间换来了最坏 %d 次比较(链地址法最坏 %d 次)%n",
                perfect.spaceUsageRatio(), perfect.worstCaseProbes(), chained.maxChainLength());
    }

    private static Long[] valuesOf(long[] keys) {
        Long[] values = new Long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = keys[i];
        }
        return values;
    }

    /** 生成 n 个互不相同的随机关键字(完全散列要求关键字互异) */
    private static long[] distinctKeys(int n, Random random) {
        java.util.TreeSet<Long> set = new java.util.TreeSet<Long>();
        while (set.size() < n) {
            set.add((long) random.nextInt((int) MAX_KEY + 1));
        }
        long[] keys = new long[n];
        int i = 0;
        for (long key : set) {
            keys[i++] = key;
        }
        return keys;
    }
}
