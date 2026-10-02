package cn.exercise.algs4.datastructure.hash;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 全域散列法(Universal Hashing)散列表。
 *
 * <p>做法不是固定一个散列函数,而是先固定一族函数,再<b>在建表时随机选其中一个</b>:</p>
 * <pre>
 *     H = { h(a,b) | h(a,b)(key) = ((a * key + b) mod p) mod m }
 *     a ∈ {1, 2, …, p-1},  b ∈ {0, 1, …, p-1},  p 为素数, m = tableSize
 * </pre>
 *
 * <p><b>全域性(universality)</b>:a、b 在全族中均匀随机选取时,对任意两个不同的关键字
 * k1 ≠ k2,有</p>
 * <pre>
 *     Pr[ h(k1) = h(k2) ] ≤ 1/m
 * </pre>
 * <p>也就是说,无论对手怎么挑关键字(哪怕他知道你在用全域散列、知道 p 和 m),他都不能预先造出
 * 一批"必然撞在一起"的关键字,除非他猜到这次选中的 (a, b)。这正是它比固定函数
 * (如 {@code key mod m}) 强的地方:固定函数的碰撞集合是对手可以针对性构造的。</p>
 *
 * <p><b>随机性只用一次</b>:函数一旦选定就必须固定,此后 hash 是确定性的(否则插入与查找
 * 会落到不同地址,散列表立刻失效)。因此本类把 (a, b) 作为实例状态保存。</p>
 *
 * <p>本类用<b>拉链法</b>处理冲突(与全域散列配合时链长期望长度 ≤ 1 + n/m)。注意两点诚实说明:</p>
 * <ul>
 *   <li>默认构造使用固定的 {@link #DEFAULT_SEED},好处是结果可复现、便于测试与教学,
 *       <b>但种子可预测,不构成对真实对手的防护</b>;工程上应传入不可预测的种子
 *       (如 {@code new SecureRandom().nextLong()})。</li>
 *   <li>为让 {@code a * key + b} 不溢出 long,要求 p ≤ {@link Integer#MAX_VALUE}(即 2^31-1),
 *       关键字必须落在 [0, p-1]。更大的关键字空间需要改用 64 位或模运算库的变体。</li>
 * </ul>
 *
 * <p>约束:p 必须是素数且 2 ≤ p ≤ 2^31-1;关键字必须满足 0 ≤ key ≤ p-1;
 * 值为 null 抛 {@link NullPointerException}。本类不是线程安全的。</p>
 *
 * @param <Value> 关键字关联的值类型
 */
public class UniversalHashST<Value> {

    /** 默认种子:固定值,保证默认构造的结果可复现(代价是种子可预测) */
    private static final long DEFAULT_SEED = 0x5DEECE66DL;

    /** 表长上限 2^26 */
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

    /** 素数 p(关键字上界,不含) */
    private final long primeP;

    /** 表长 m */
    private final int tableSize;

    /** 一次项系数 a ∈ [1, p-1] */
    private final long a;

    /** 常数项 b ∈ [0, p-1] */
    private final long b;

    /** 本次建表所用的随机种子 */
    private final long seed;

    /** 桶数组 */
    private final Node<Value>[] buckets;

    /** 已存关键字个数 */
    private int n;

    /**
     * 用默认种子随机选取族中的一个函数(结果可复现;真实对手防护请用带自定义种子的构造方法)。
     *
     * @param tableSize 表长 m
     * @param primeP    素数 p,须满足 2 ≤ p ≤ 2^31-1
     */
    public UniversalHashST(int tableSize, long primeP) {
        this(tableSize, primeP, DEFAULT_SEED);
    }

    /**
     * 用给定种子从族中随机选取 (a, b):同一种子必然选出同一个函数。
     *
     * @param tableSize 表长 m
     * @param primeP    素数 p
     * @param seed      随机种子
     */
    public UniversalHashST(int tableSize, long primeP, long seed) {
        this(tableSize, primeP, seed,
                pickA(primeP, seed), pickB(primeP, seed));
    }

    /**
     * 直接指定 (a, b)(主要用于验证全域性、复现实验与单元测试)。
     *
     * @param tableSize 表长 m
     * @param primeP    素数 p
     * @param a         一次项系数,须满足 1 ≤ a ≤ p-1
     * @param b         常数项,须满足 0 ≤ b ≤ p-1
     * @throws IllegalArgumentException 参数非法
     */
    public UniversalHashST(int tableSize, long primeP, long a, long b) {
        this(tableSize, primeP, DEFAULT_SEED, a, b);
    }

    private UniversalHashST(int tableSize, long primeP, long seed, long a, long b) {
        if (tableSize < 1 || tableSize > MAX_TABLE_SIZE) {
            throw new IllegalArgumentException("表长必须在 [1, " + MAX_TABLE_SIZE
                    + "] 内,当前为 " + tableSize);
        }
        checkPrime(primeP);
        if (a < 1 || a > primeP - 1) {
            throw new IllegalArgumentException("a 必须满足 1 <= a <= p-1,当前 a = " + a);
        }
        if (b < 0 || b > primeP - 1) {
            throw new IllegalArgumentException("b 必须满足 0 <= b <= p-1,当前 b = " + b);
        }
        this.tableSize = tableSize;
        this.primeP = primeP;
        this.a = a;
        this.b = b;
        this.seed = seed;
        this.buckets = newNodeArray(tableSize);
    }

    /**
     * 散列函数 h(a,b)(key) = ((a * key + b) mod p) mod m。
     *
     * @param key 关键字,须满足 0 ≤ key ≤ p-1
     * @return 地址,落在 [0, tableSize)
     * @throws IllegalArgumentException 关键字越界
     */
    public int hash(long key) {
        if (key < 0 || key >= primeP) {
            throw new IllegalArgumentException("关键字必须落在 [0, " + (primeP - 1) + "](全域族要求),当前 key = " + key);
        }
        return (int) (((a * key + b) % primeP) % tableSize);
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
     * @return 素数 p
     */
    public long primeP() {
        return primeP;
    }

    /**
     * @return 表长 m
     */
    public int tableSize() {
        return tableSize;
    }

    /**
     * @return 本次选中的一次项系数 a
     */
    public long a() {
        return a;
    }

    /**
     * @return 本次选中的常数项 b
     */
    public long b() {
        return b;
    }

    /**
     * @return 本次建表所用的种子
     */
    public long seed() {
        return seed;
    }

    /**
     * @return 装填因子 alpha = n / m
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

    // ---------------- 全域族的统计(用于验证与演示) ----------------

    /**
     * 族的大小 |H| = (p-1) * p。
     *
     * @param primeP 素数 p
     * @return 族中函数的个数
     */
    public static long familySize(long primeP) {
        return (primeP - 1) * primeP;
    }

    /**
     * 遍历整个族,统计使两个不同关键字碰撞的函数个数(用于验证 Pr ≤ 1/m)。
     * 仅适用于较小的 p(族大小为 (p-1)·p,遍历成本与之成正比)。
     *
     * @param k1        关键字 1
     * @param k2        关键字 2
     * @param tableSize 表长 m
     * @param primeP    素数 p
     * @return 使 h(k1) == h(k2) 的 (a, b) 个数
     * @throws IllegalArgumentException 关键字相同或越界、p 非素数
     */
    public static long collisionCountInFamily(long k1, long k2, int tableSize, long primeP) {
        if (k1 == k2) {
            throw new IllegalArgumentException("两个关键字必须不同,当前都是 " + k1);
        }
        if (!isPrime(primeP)) {
            throw new IllegalArgumentException("p 必须是素数,当前 p = " + primeP);
        }
        long count = 0;
        for (long a = 1; a <= primeP - 1; a++) {
            for (long b = 0; b <= primeP - 1; b++) {
                long h1 = ((a * k1 + b) % primeP) % tableSize;
                long h2 = ((a * k2 + b) % primeP) % tableSize;
                if (h1 == h2) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 遍历整个族求碰撞概率 = 碰撞函数个数 / |H|,理论保证 ≤ 1/m。
     *
     * @param k1        关键字 1
     * @param k2        关键字 2
     * @param tableSize 表长 m
     * @param primeP    素数 p
     * @return 碰撞概率
     */
    public static double collisionProbabilityInFamily(long k1, long k2, int tableSize, long primeP) {
        return (double) collisionCountInFamily(k1, k2, tableSize, primeP) / familySize(primeP);
    }

    /**
     * 用闭合公式独立计算"使两个不同关键字碰撞的函数个数",与遍历结果互相印证(两条独立路径)。
     *
     * <p>推导:固定 a,令 d = a·(k2-k1) mod p(≠0)。b 遍历 Z_p 时 x1 = (a·k1+b) mod p 取遍 Z_p,
     * 而 x2 = (x1 + d) mod p:其中不越界(x1+d &lt; p)的 x1 有 p-d 个,越界的 d 个。
     * 不越界时碰撞 ⟺ m | d;越界时 x2 = x1 + d - p,碰撞 ⟺ m | (d-p)。
     * a 遍历 Z_p* 时 d 取遍所有非零剩余,于是</p>
     * <pre>
     *     count = Σ_{d=1}^{p-1} [ (p-d)·[m|d] + d·[m|(d-p)] ]
     * </pre>
     * <p>该值与 k1、k2 的具体取值无关(只要求 k1 ≠ k2),且恒 ≤ |H|/m。</p>
     *
     * @param tableSize 表长 m
     * @param primeP    素数 p
     * @return 碰撞函数个数
     */
    public static long predictedCollisionCount(int tableSize, long primeP) {
        if (tableSize < 1) {
            throw new IllegalArgumentException("表长必须至少为 1,当前为 " + tableSize);
        }
        if (!isPrime(primeP)) {
            throw new IllegalArgumentException("p 必须是素数,当前 p = " + primeP);
        }
        long count = 0;
        for (long d = 1; d <= primeP - 1; d++) {
            if (d % tableSize == 0) {
                count += primeP - d;
            }
            if ((d - primeP) % tableSize == 0) {
                count += d;
            }
        }
        return count;
    }

    // ---------------- 内部实现 ----------------

    private static long pickA(long primeP, long seed) {
        checkPrime(primeP);
        Random random = new Random(seed);
        return 1 + Math.floorMod(random.nextLong(), primeP - 1);
    }

    private static long pickB(long primeP, long seed) {
        checkPrime(primeP);
        Random random = new Random(seed);
        random.nextLong(); // 与 pickA 保持同一随机序列的先后关系
        return Math.floorMod(random.nextLong(), primeP);
    }

    /**
     * p 的合法性校验。注意:必须能在 {@link #pickA} 里先行调用 —— 否则 p 非法时
     * {@code floorMod(x, p-1)} / {@code floorMod(x, p)} 会先抛 ArithmeticException(除零),
     * 而不是让调用方看到清晰的 IllegalArgumentException。
     */
    private static void checkPrime(long primeP) {
        if (primeP < 2 || primeP > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("p 必须满足 2 <= p <= " + Integer.MAX_VALUE
                    + ",当前为 " + primeP);
        }
        if (!isPrime(primeP)) {
            throw new IllegalArgumentException("p 必须是素数,否则全域性保证(Pr <= 1/m)失效,当前 p = " + primeP);
        }
    }

    /**
     * 素性判定:用 BigInteger 的概率素性测试(错误概率小于 2^-80),避免复合的 p 让全域性保证失效。
     */
    private static boolean isPrime(long value) {
        return value >= 2 && BigInteger.valueOf(value).isProbablePrime(80);
    }

    @SuppressWarnings("unchecked")
    private static <Value> Node<Value>[] newNodeArray(int size) {
        return (Node<Value>[]) new Node[size];
    }

    /**
     * 演示:随机选出的函数、全域性的穷举验证、与"固定函数被对手针对"的对比、建表与查询。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("全域散列法:H = { h(a,b)(key) = ((a*key + b) mod p) mod m },建表时随机选一个");
        System.out.println();

        long p = 97;
        int m = 16;
        UniversalHashST<String> st = new UniversalHashST<String>(m, p);
        System.out.println("p = " + p + ",m = " + m + ",族大小 |H| = (p-1)*p = " + familySize(p)
                + ",默认种子 = " + st.seed());
        System.out.println("本次随机选中:a = " + st.a() + ",b = " + st.b());
        System.out.println();
        System.out.println("  关键字    地址");
        long[] samples = {0, 1, 13, 42, 96};
        for (long key : samples) {
            System.out.printf("  %-8d  %d%n", key, st.hash(key));
        }
        System.out.println();

        System.out.println("全域性验证:遍历整个族,统计两个不同关键字碰撞的函数个数(理论保证 Pr <= 1/m)");
        long[][] pairs = {{13, 42}, {1, 2}, {0, 96}};
        for (long[] pair : pairs) {
            long hits = collisionCountInFamily(pair[0], pair[1], m, p);
            double prob = (double) hits / familySize(p);
            System.out.printf("  k1 = %-4d k2 = %-4d → 碰撞 %d / %d = %.6f  (1/m = %.6f, %s)%n",
                    pair[0], pair[1], hits, familySize(p), prob, 1.0 / m,
                    prob <= 1.0 / m + 1e-12 ? "满足" : "不满足");
        }
        System.out.println("  闭合公式 predictedCollisionCount(m, p) = " + predictedCollisionCount(m, p)
                + ";朴素上界 |H|/m = " + (familySize(p) / m)
                + "(实际计数恒 ≤ 上界,只有 m = 1 或 p 远大于 m 时才逼近)");
        System.out.println("  概率 = (" + p + "+1-" + m + ")/(" + p + "*" + m + ") = "
                + String.format("%.6f", (double) (p + 1 - m) / ((double) p * m)) + " ≤ 1/m");
        System.out.println();

        long p2 = 1009;
        int m2 = 64;
        long[] adversary = new long[15];
        for (int i = 0; i < adversary.length; i++) {
            adversary[i] = m2 * (i + 1); // 对固定函数 h(key) = key mod 64,它们全部撞在桶 0
        }
        System.out.println("对手视角:p = " + p2 + ",m = " + m2 + ",对手知道 p 和 m,精心挑了一批关键字");
        System.out.println("  集合 = " + java.util.Arrays.toString(adversary).replace(", ", ","));
        System.out.println("  若固定用 h(key) = key mod 64 → 全部落在地址 0,最长链 = " + adversary.length);
        int worst = 0;
        long total = 0;
        long worstSeed = -1;
        int clustered = 0;
        int trials = 500;
        for (long seed = 0; seed < trials; seed++) {
            UniversalHashST<String> random = new UniversalHashST<String>(m2, p2, seed);
            for (long key : adversary) {
                random.put(key, "v");
            }
            int max = random.maxChainLength();
            total += max;
            if (max > worst) {
                worst = max;
                worstSeed = seed;
            }
            if (max >= 3) {
                clustered++;
            }
        }
        UniversalHashST<String> worstCase = new UniversalHashST<String>(m2, p2, worstSeed);
        System.out.printf("  改用全域散列(随机选函数 %d 次):平均最长链 %.2f,最长链 >= 3 的有 %d 次,"
                        + "最差一次 %d(seed = %d,a = %d,b = %d)%n",
                trials, (double) total / trials, clustered, worst, worstSeed, worstCase.a(), worstCase.b());
        System.out.println("  说明:全域性保证的是**概率**(Pr[碰撞] <= 1/m),不保证每个随机函数都均匀 —— ");
        System.out.println("        a = 1 且 b 较小时,h(key) = ((key+b) mod p) mod m 对这批等距关键字无回绕,会整体落进同一个桶。");
        System.out.println();

        st.put(13, "学生13");
        st.put(42, "学生42");
        st.put(1, "学生1");
        System.out.println("建表(拉链法):size = " + st.size()
                + ",装填因子 = " + String.format("%.4f", st.loadFactor())
                + ",最长链 = " + st.maxChainLength());
        System.out.println("查询演示:get(42) = " + st.get(42)
                + ",contains(13) = " + st.contains(13)
                + ",delete(42) = " + st.delete(42)
                + ",删除后 size = " + st.size());
    }
}
