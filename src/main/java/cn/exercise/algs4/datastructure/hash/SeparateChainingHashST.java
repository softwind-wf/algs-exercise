package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 链地址法(拉链法 / 分离链接法,Separate Chaining)散列表。
 *
 * <p>做法:桶数组的每个槽位挂一条链,散列到同一地址的关键字都挂在该链上。</p>
 * <pre>
 *     H(key) = hashFunction(key, capacity)    // 落在 [0, capacity)
 * </pre>
 *
 * <p><b>与开放地址法(线性/二次/双重探测)的关键差别</b>:</p>
 * <ol>
 *   <li><b>删除不需要墓碑</b>:直接把结点从链上摘掉即可,不存在"截断探测链"的问题;
 *       开放地址法删除必须留墓碑。</li>
 *   <li><b>装填因子 α = n/m 可以大于 1</b>:链可以继续变长,表不会"满";
 *       代价是平均查找长度随 α 线性增长,而不是像开放地址法那样在 α→1 时急剧恶化。</li>
 *   <li><b>性能公式简单</b>:ASL成功 ≈ 1 + α/2(查找成功平均比较 1 + α/2 个结点),
 *       ASL失败 ≈ α(失败查找平均比较 α 个结点)。</li>
 * </ol>
 *
 * <p><b>ASL 口径(判定缺陷时必须对齐)</b>:设各桶链长为 l_0…l_{m-1}:</p>
 * <ul>
 *   <li>ASL成功 = (Σ l_i(l_i+1)/2) / n —— 链内每个关键字的比较次数就是它离链头的距离;
 *       该值<b>与链内顺序无关</b>(只取决于各桶链长)。</li>
 *   <li>ASL失败 = (Σ l_i) / m = α —— 只比较链上结点,不计最后那次"判空"(教材常用口径)。</li>
 *   <li>{@link #averageUnsuccessfulProbesWithEmptyCheck()} = α + 1 —— 把"发现链尾为空"也算一次比较。</li>
 * </ul>
 *
 * <p>链内顺序为"最近插入在前"(头插,插入 O(1))。散列函数可插拔;默认 {@code key mod capacity},
 * 容量默认取素数,α 超过上限(默认 1.0)时扩容到不小于 2 倍旧容量的素数并重新散列。
 * 本类不支持 null 值;不是线程安全的。</p>
 *
 * @param <Value> 关键字关联的值类型
 * @see OpenAddressHashST 开放地址法(线性探测),可对照 ASL 与删除代价
 */
public class SeparateChainingHashST<Value> {

    /** 可插拔散列函数:返回 [0, capacity) 内的地址 */
    public interface HashFunction {
        /**
         * @param key      关键字
         * @param capacity 当前桶数
         * @return 地址,必须落在 [0, capacity)
         */
        int hash(long key, int capacity);
    }

    /** 默认桶数(素数) */
    private static final int DEFAULT_CAPACITY = 17;

    /** 默认装填因子上限:链地址法允许 α 达到 1 */
    private static final double DEFAULT_MAX_LOAD_FACTOR = 1.0;

    /** 桶数上限 2^26 */
    private static final int MAX_CAPACITY = 1 << 26;

    /** 链结点 */
    private static final class Node<Value> {
        final long key;
        Value value;
        Node<Value> next;

        Node(long key, Value value, Node<Value> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private final HashFunction hashFunction;
    private final double maxLoadFactor;

    private int capacity;
    private Node<Value>[] buckets;
    private int n;
    private int resizeCount;

    /**
     * 默认构造:桶数 17、散列函数 {@code key mod capacity}、α 上限 1.0。
     */
    public SeparateChainingHashST() {
        this(DEFAULT_CAPACITY, DEFAULT_MAX_LOAD_FACTOR, defaultHashFunction());
    }

    /**
     * @param initialCapacity 初始桶数,至少 1
     */
    public SeparateChainingHashST(int initialCapacity) {
        this(initialCapacity, DEFAULT_MAX_LOAD_FACTOR, defaultHashFunction());
    }

    /**
     * @param initialCapacity 初始桶数,至少 1
     * @param maxLoadFactor   装填因子上限,取值 (0, 16]
     */
    public SeparateChainingHashST(int initialCapacity, double maxLoadFactor) {
        this(initialCapacity, maxLoadFactor, defaultHashFunction());
    }

    /**
     * 完整构造。
     *
     * @param initialCapacity 初始桶数,至少 1
     * @param maxLoadFactor   装填因子上限,大于 0 且不超过 16(链地址法允许 α 远大于 1)
     * @param hashFunction    散列函数,不能为 null
     * @throws IllegalArgumentException 参数非法
     */
    @SuppressWarnings("unchecked")
    public SeparateChainingHashST(int initialCapacity, double maxLoadFactor,
                                  HashFunction hashFunction) {
        if (initialCapacity < 1 || initialCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("桶数必须在 [1, " + MAX_CAPACITY
                    + "] 内,当前为 " + initialCapacity);
        }
        if (maxLoadFactor <= 0.0 || maxLoadFactor > 16.0) {
            throw new IllegalArgumentException("装填因子上限必须在 (0, 16] 内,当前为 " + maxLoadFactor);
        }
        this.hashFunction = Objects.requireNonNull(hashFunction, "散列函数不能为 null");
        this.maxLoadFactor = maxLoadFactor;
        this.capacity = initialCapacity;
        this.buckets = (Node<Value>[]) new Node[initialCapacity];
    }

    /**
     * 插入或更新键值对:新结点头插到对应链上;α 超过上限时先扩容。
     *
     * @param key   关键字
     * @param value 值,不能为 null
     */
    public void put(long key, Value value) {
        Objects.requireNonNull(value, "值不能为 null");
        if ((double) (n + 1) / capacity > maxLoadFactor) {
            grow();
        }
        int address = hashAddress(key);
        for (Node<Value> node = buckets[address]; node != null; node = node.next) {
            if (node.key == key) {
                node.value = value; // 已存在则覆盖,不新增结点
                return;
            }
        }
        buckets[address] = new Node<Value>(key, value, buckets[address]);
        n++;
    }

    /**
     * 查找关键字对应的值。
     *
     * @param key 关键字
     * @return 值;不存在返回 null
     */
    public Value get(long key) {
        for (Node<Value> node = buckets[hashAddress(key)]; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }

    /**
     * @param key 关键字
     * @return 是否存在
     */
    public boolean contains(long key) {
        return get(key) != null;
    }

    /**
     * 删除关键字:<b>直接摘链,不需要墓碑</b>。
     *
     * @param key 关键字
     * @return 被删除的值;不存在返回 null
     */
    public Value delete(long key) {
        int address = hashAddress(key);
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
     * @return 桶数 m
     */
    public int capacity() {
        return capacity;
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
     * @return 装填因子 α = n / m(链地址法下可以大于 1)
     */
    public double loadFactor() {
        return (double) n / capacity;
    }

    /**
     * 散列地址(并校验散列函数返回值)。
     *
     * @param key 关键字
     * @return 地址,落在 [0, capacity)
     * @throws IllegalStateException 散列函数返回越界地址
     */
    public int hashAddress(long key) {
        int address = hashFunction.hash(key, capacity);
        if (address < 0 || address >= capacity) {
            throw new IllegalStateException("散列函数返回了越界地址:" + address + "(桶数 " + capacity + ")");
        }
        return address;
    }

    /**
     * @param key 关键字
     * @return 关键字在链上的位置(1 表示链头);不存在返回 -1
     */
    public int positionOf(long key) {
        int position = 0;
        for (Node<Value> node = buckets[hashAddress(key)]; node != null; node = node.next) {
            position++;
            if (node.key == key) {
                return position;
            }
        }
        return -1;
    }

    /**
     * @return 各桶链长的副本(下标 = 桶号)
     */
    public int[] chainLengths() {
        int[] lengths = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            int len = 0;
            for (Node<Value> node = buckets[i]; node != null; node = node.next) {
                len++;
            }
            lengths[i] = len;
        }
        return lengths;
    }

    /**
     * @return 最长链长度(最坏查找代价)
     */
    public int maxChainLength() {
        int max = 0;
        for (int len : chainLengths()) {
            max = Math.max(max, len);
        }
        return max;
    }

    /**
     * @return 空桶个数
     */
    public int emptyBucketCount() {
        int empty = 0;
        for (int len : chainLengths()) {
            if (len == 0) {
                empty++;
            }
        }
        return empty;
    }

    /**
     * @return 平均链长,等于装填因子 α
     */
    public double averageChainLength() {
        return loadFactor();
    }

    /**
     * 查找成功的总比较次数 = Σ l_i(l_i+1)/2(链内每个关键字的比较次数是其离链头的距离)。
     *
     * @return 总比较次数
     */
    public long successfulProbeSum() {
        long total = 0;
        for (int len : chainLengths()) {
            total += (long) len * (len + 1) / 2;
        }
        return total;
    }

    /**
     * @return ASL成功 = 总比较次数 / n(与链内顺序无关)
     */
    public double averageSuccessfulProbes() {
        return n == 0 ? 0.0 : (double) successfulProbeSum() / n;
    }

    /**
     * 查找失败的总比较次数 = Σ l_i(只比较链上结点)。
     *
     * @return 总比较次数
     */
    public long failureComparisonSum() {
        long total = 0;
        for (int len : chainLengths()) {
            total += len;
        }
        return total;
    }

    /**
     * @return ASL失败 = (Σ l_i)/m = α(教材常用口径:不计最后那次判空)
     */
    public double averageUnsuccessfulProbes() {
        return (double) failureComparisonSum() / capacity;
    }

    /**
     * @return ASL失败的另一种口径 = α + 1(把"发现链尾为空"也算一次比较)
     */
    public double averageUnsuccessfulProbesWithEmptyCheck() {
        return averageUnsuccessfulProbes() + 1.0;
    }

    /**
     * @return 教材近似值 ASL成功 ≈ 1 + α/2
     */
    public double theoreticalSuccessfulProbes() {
        return 1.0 + loadFactor() / 2.0;
    }

    /**
     * @return 教材近似值 ASL失败 ≈ α
     */
    public double theoreticalUnsuccessfulProbes() {
        return loadFactor();
    }

    /**
     * @return 所有关键字(按桶号升序,桶内从链头到链尾)
     */
    public Iterable<Long> keys() {
        List<Long> result = new ArrayList<Long>(n);
        for (int i = 0; i < capacity; i++) {
            for (Node<Value> node = buckets[i]; node != null; node = node.next) {
                result.add(node.key);
            }
        }
        return result;
    }

    /**
     * 按桶号升序输出所有键值对:{key value, key value}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        String sep = "";
        for (int i = 0; i < capacity; i++) {
            for (Node<Value> node = buckets[i]; node != null; node = node.next) {
                sb.append(sep).append(node.key).append(' ').append(node.value);
                sep = ", ";
            }
        }
        return sb.append('}').toString();
    }

    // ---------------- 内部实现 ----------------

    private static HashFunction defaultHashFunction() {
        return new HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, (long) capacity);
            }
        };
    }

    /** 扩容到不小于 2 倍旧桶数的素数,并重新散列所有元素 */
    @SuppressWarnings("unchecked")
    private void grow() {
        long target = (long) capacity * 2;
        if (target > MAX_CAPACITY) {
            target = MAX_CAPACITY;
        }
        int newCapacity = OpenAddressHashST.nextPrime((int) target);
        if (newCapacity <= capacity || newCapacity > MAX_CAPACITY) {
            return;
        }
        Node<Value>[] oldBuckets = buckets;
        int oldCapacity = capacity;

        this.capacity = newCapacity;
        this.buckets = (Node<Value>[]) new Node[newCapacity];
        this.n = 0;
        for (int i = 0; i < oldCapacity; i++) {
            for (Node<Value> node = oldBuckets[i]; node != null; node = node.next) {
                put(node.key, node.value); // 重新散列(容量变了,地址可能不同)
            }
        }
        resizeCount++;
    }

    /**
     * 演示:教材经典例题、α &gt; 1、链长分布、扩容、删除不需要墓碑。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("链地址法(拉链法):每个桶挂一条链,H(key) = key mod m");
        System.out.println();

        int m = 11;
        int[] keys = {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79};
        SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(m, 16.0);
        System.out.println("用例 1:教材经典例题(桶数 m = 11,H(key) = key mod 11,关键字 "
                + java.util.Arrays.toString(keys) + ")");
        for (int key : keys) {
            st.put(key, "v" + key);
        }
        int[] lengths = st.chainLengths();
        System.out.println("  桶号   链长   链上关键字(链头在前)");
        for (int i = 0; i < m; i++) {
            StringBuilder chainKeys = new StringBuilder();
            for (long key : st.keys()) {
                if (st.hashAddress(key) == i) {
                    chainKeys.append(key).append(' ');
                }
            }
            System.out.printf("  %-6d %-6d %s%n", i, lengths[i], chainKeys.toString().trim());
        }
        System.out.printf("  n = %d,α = %.4f,最长链 = %d,空桶 = %d%n",
                st.size(), st.loadFactor(), st.maxChainLength(), st.emptyBucketCount());
        System.out.printf("  ASL成功 = %d/%d = %.4f(教材近似 1 + α/2 = %.4f)%n",
                st.successfulProbeSum(), st.size(), st.averageSuccessfulProbes(), st.theoreticalSuccessfulProbes());
        System.out.printf("  ASL失败 = %d/%d = %.4f(不计判空,教材近似 α = %.4f);计判空则为 %.4f%n",
                st.failureComparisonSum(), st.capacity(), st.averageUnsuccessfulProbes(),
                st.theoreticalUnsuccessfulProbes(), st.averageUnsuccessfulProbesWithEmptyCheck());
        System.out.println("  教材经典答案:ASL成功 = 15/12 = 1.25,ASL失败 = 12/11 ≈ 1.0909");
        System.out.println();

        System.out.println("用例 2:与开放地址法对比(同样的桶数与散列函数:H(key) = key mod 13、容量 16)");
        SeparateChainingHashST<String> chained = new SeparateChainingHashST<String>(16, 16.0, modThirteen());
        OpenAddressHashST<String> linear = new OpenAddressHashST<String>(16, 1.0, modThirteenOA());
        for (int key : keys) {
            chained.put(key, "v" + key);
            linear.put(key, "v" + key);
        }
        System.out.printf("  链地址法  :ASL成功 = %.4f,ASL失败 = %.4f,最长链 = %d%n",
                chained.averageSuccessfulProbes(), chained.averageUnsuccessfulProbes(),
                chained.maxChainLength());
        System.out.printf("  线性探测法:ASL成功 = %.4f,ASL失败 = %.4f%n",
                linear.averageSuccessfulProbes(), linear.averageUnsuccessfulProbes());
        chained.delete(1); // 直接在链上摘除,不需要墓碑
        System.out.println("  删除 1 之后:contains(79) = " + chained.contains(79)
                + ",contains(23) = " + chained.contains(23) + "(链地址法摘链即可,无需墓碑)");
        System.out.println();

        System.out.println("用例 3:装填因子可以大于 1(桶数 17,插入 200 个随机关键字,不扩容)");
        java.util.Random random = new java.util.Random(20261003L);
        SeparateChainingHashST<String> dense = new SeparateChainingHashST<String>(17, 16.0);
        for (long i = 0; i < 200; i++) {
            dense.put(random.nextInt(10000000), "v" + i);
        }
        System.out.printf("  α = %.2f,最长链 = %d,空桶 = %d%n",
                dense.loadFactor(), dense.maxChainLength(), dense.emptyBucketCount());
        System.out.printf("  ASL成功 = %.3f(近似 1 + α/2 = %.3f),ASL失败 = %.3f(近似 α = %.3f)%n",
                dense.averageSuccessfulProbes(), dense.theoreticalSuccessfulProbes(),
                dense.averageUnsuccessfulProbes(), dense.theoreticalUnsuccessfulProbes());
        System.out.println("  对照:开放地址法在 α → 1 时会急剧恶化甚至插不进去,链地址法只是链变长");
        System.out.println();

        System.out.println("用例 4:扩容(默认 α 上限 1.0,容量取素数)");
        SeparateChainingHashST<String> growing = new SeparateChainingHashST<String>();
        StringBuilder path = new StringBuilder("  初始 m = " + growing.capacity());
        int last = growing.capacity();
        for (long i = 0; i < 300; i++) {
            growing.put(random.nextInt(10000000), "v" + i);
            if (growing.capacity() != last) {
                path.append(" → ").append(growing.capacity());
                last = growing.capacity();
            }
        }
        System.out.println(path);
        System.out.printf("  最终:size = %d,m = %d,α = %.4f,扩容 %d 次,最长链 = %d%n",
                growing.size(), growing.capacity(), growing.loadFactor(), growing.resizeCount(),
                growing.maxChainLength());
        System.out.println();

        System.out.println("用例 5:链长分布(1 万个随机关键字投到 1024 个桶)");
        SeparateChainingHashST<String> wide = new SeparateChainingHashST<String>(1024, 16.0);
        for (long i = 0; i < 10000; i++) {
            wide.put(random.nextInt(10000000), "v" + i);
        }
        int[] wideLengths = wide.chainLengths();
        int zero = 0;
        int one = 0;
        int two = 0;
        int threePlus = 0;
        for (int len : wideLengths) {
            if (len == 0) {
                zero++;
            } else if (len == 1) {
                one++;
            } else if (len == 2) {
                two++;
            } else {
                threePlus++;
            }
        }
        System.out.printf("  平均链长 = %.2f,最长链 = %d%n", wide.averageChainLength(), wide.maxChainLength());
        System.out.println("  链长为 0 / 1 / 2 / ≥3 的桶数:" + zero + " / " + one + " / " + two + " / " + threePlus);
    }

    private static SeparateChainingHashST.HashFunction modThirteen() {
        return new SeparateChainingHashST.HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, 13L);
            }
        };
    }

    private static OpenAddressHashST.HashFunction modThirteenOA() {
        return new OpenAddressHashST.HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, 13L);
            }
        };
    }
}
