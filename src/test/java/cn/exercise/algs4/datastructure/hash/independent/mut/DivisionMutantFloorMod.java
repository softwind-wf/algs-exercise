package cn.exercise.algs4.datastructure.hash.independent.mut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * 除留余数法(Division / Remainder Method)散列表。
 *
 * <p>散列函数:
 * <pre>
 *     H(key) = key mod p        (p 通常取不大于表长 m 的最大质数)
 * </pre>
 * 关键字为负数时用 {@link Math#floorMod(int, int)} 取模,保证地址落在 [0, p-1]。</p>
 *
 * <p><b>冲突处理</b>:线性探测再散列 H_i = (H(key) + i) mod m(i = 1, 2, 3, ...)。
 * 删除元素时在被删位置留下"墓碑"(TOMBSTONE),否则会截断后面关键字的探测链,
 * 使本来存在的元素查不到;插入时优先复用探测路径上遇到的第一个墓碑。</p>
 *
 * <p><b>模数 p 的选取</b>直接影响冲突次数:p 取质数、且不含小于 20 的质因子时分布最均匀;
 * 若 p 取 2 的幂(如 16),关键字的奇偶性会直接决定地址奇偶性,大量偶数关键字会挤在偶数地址上。
 * 本类的单参构造方法会自动把模数取为"不大于表长的最大质数"。</p>
 *
 * <p>本实现是定长散列表(不自动扩容),装填因子 alpha = n / m 建议不超过 0.5,
 * 否则线性探测产生的聚集(clustering)会让查找效率迅速下降。</p>
 *
 * @param <Value> 关键字关联的值类型
 */
public class DivisionMutantFloorMod<Value> {

    /**
     * 表长上限:2^26 = 67108864 个单元。
     * 超过此值几乎必然是误传,直接拒绝,避免 JVM 因分配超大数组而 OutOfMemoryError。
     */
    private static final int MAX_TABLE_SIZE = 1 << 26;

    /** 墓碑:标记"曾占用、现已删除"的单元,使探测链不断裂 */
    private static final Entry TOMBSTONE = new Entry(0, null);

    /** 散列表的一个存储单元 */
    private static final class Entry {
        final int key;
        Object value;

        Entry(int key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    /** 表长 m */
    private final int tableSize;

    /** 模数 p,即 H(key) = key mod p */
    private final int modulus;

    /** 地址 -> 单元 */
    private final Entry[] table;

    /** 已存关键字个数 */
    private int n;

    /** 墓碑个数 */
    private int tombstones;

    /** 最近一次查找/插入/删除的探测次数 */
    private int lastProbes;

    /**
     * 按"模数取不大于表长的最大质数"这一教材规则构造散列表。
     *
     * @param tableSize 表长 m,必须不小于 2
     * @throws IllegalArgumentException 表长小于 2
     */
    public DivisionMutantFloorMod(int tableSize) {
        this(tableSize, defaultModulus(tableSize));
    }

    /**
     * 指定表长与模数构造散列表(用于对比不同模数,或 p &lt; m 的场景,如 m = 16、p = 13)。
     *
     * @param tableSize 表长 m
     * @param modulus   模数 p,必须满足 1 &lt;= p &lt;= m
     * @throws IllegalArgumentException 表长小于 2,或模数不在 [1, 表长] 内
     */
    public DivisionMutantFloorMod(int tableSize, int modulus) {
        checkTableSize(tableSize);
        if (modulus < 1 || modulus > tableSize) {
            throw new IllegalArgumentException(
                    "模数 p 必须满足 1 <= p <= 表长 " + tableSize + ",当前 p = " + modulus);
        }
        this.tableSize = tableSize;
        this.modulus = modulus;
        this.table = new Entry[tableSize];
    }

    /**
     * 除留余数法散列函数 H(key) = key mod p,负数也能得到非负地址。
     *
     * @param key 关键字
     * @return 地址,落在 [0, p-1]
     */
    public int hash(int key) {
        return (key % modulus);
    }

    /**
     * 插入或更新键值对。冲突时线性探测;探测路径上有墓碑则复用第一个墓碑。
     *
     * @param key   关键字
     * @param value 值,不能为 null
     * @throws IllegalStateException 散列表已满且没有可复用的墓碑
     */
    public void put(int key, Value value) {
        Objects.requireNonNull(value, "值不能为 null");
        int i = hash(key);
        int firstTombstone = -1;
        int probes = 0;
        for (int step = 0; step < tableSize; step++) {
            Entry e = table[i];
            probes++;
            if (e == null) {
                if (firstTombstone >= 0) {
                    table[firstTombstone] = new Entry(key, value);
                    tombstones--;
                } else {
                    table[i] = new Entry(key, value);
                }
                n++;
                lastProbes = probes;
                return;
            }
            if (e == TOMBSTONE) {
                if (firstTombstone < 0) {
                    firstTombstone = i;
                }
            } else if (e.key == key) {
                e.value = value; // 关键字已存在,覆盖旧值
                lastProbes = probes;
                return;
            }
            i = (i + 1) % tableSize;
        }
        if (firstTombstone >= 0) {
            table[firstTombstone] = new Entry(key, value);
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
     * @return 值;关键字不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public Value get(int key) {
        int i = find(key);
        return i < 0 ? null : (Value) table[i].value;
    }

    /**
     * 判断关键字是否存在。
     *
     * @param key 关键字
     * @return 存在返回 true
     */
    public boolean contains(int key) {
        return find(key) >= 0;
    }

    /**
     * 返回关键字所在的数组下标(便于观察冲突与探测结果)。
     * 内部同样执行一次查找,因此会一并更新 {@link #lastProbes()}。
     *
     * @param key 关键字
     * @return 下标;关键字不存在时返回 -1
     */
    public int slotOf(int key) {
        return find(key);
    }

    /**
     * 删除关键字。被删位置留下墓碑以维持探测链。
     *
     * @param key 关键字
     * @return 被删除的值;关键字不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public Value delete(int key) {
        int i = find(key);
        if (i < 0) {
            return null;
        }
        Value old = (Value) table[i].value;
        table[i] = TOMBSTONE;
        tombstones++;
        n--;
        return old;
    }

    /**
     * @return 散列表中键值对的个数
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
     * @return 模数 p
     */
    public int modulus() {
        return modulus;
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
     * 查找成功时的总探测次数(即 ASL 成功公式的分子)。
     *
     * @return 所有已存关键字探测次数之和
     */
    public long successfulProbeSum() {
        long total = 0;
        for (Entry e : table) {
            if (e != null && e != TOMBSTONE) {
                total += probeCount(e.key);
            }
        }
        return total;
    }

    /**
     * 查找失败时的总探测次数(即 ASL 失败公式的分子)。
     * 约定:探测次数包含最后判定为"空单元"的那一次比较;墓碑视为非空,继续向后探测。
     *
     * @return 每个起始地址查找失败所需探测次数之和
     */
    public long unsuccessfulProbeSum() {
        long total = 0;
        for (int start = 0; start < tableSize; start++) {
            int probes = 1;
            int i = start;
            while (table[i] != null) {
                if (probes == tableSize) {
                    break; // 表已满,最多探测 m 次
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
    public Iterable<Integer> keys() {
        List<Integer> result = new ArrayList<>(n);
        for (Entry e : table) {
            if (e != null && e != TOMBSTONE) {
                result.add(e.key);
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
        for (int key : keys()) {
            sb.append(sep).append(key).append(' ').append(get(key));
            sep = ", ";
        }
        return sb.append('}').toString();
    }

    /**
     * 求不大于 n 的最大质数(用于按教材规则选取模数 p)。
     *
     * @param n 上界,必须不小于 2
     * @return 不大于 n 的最大质数
     * @throws IllegalArgumentException n 小于 2
     */
    public static int largestPrimeNotGreaterThan(int n) {
        if (n < 2) {
            throw new IllegalArgumentException("不存在不大于 " + n + " 的质数");
        }
        for (int candidate = n; candidate >= 2; candidate--) {
            if (isPrime(candidate)) {
                return candidate;
            }
        }
        return 2; // n >= 2 时不会执行到此处
    }

    /**
     * 单参构造的模数规则:不大于表长的最大质数。
     */
    private static int defaultModulus(int tableSize) {
        checkTableSize(tableSize);
        return largestPrimeNotGreaterThan(tableSize);
    }

    /**
     * 表长合法性校验:不小于 2,且不超过实用上限(防止误传超大表长导致 OOM)。
     */
    private static void checkTableSize(int tableSize) {
        if (tableSize < 2) {
            throw new IllegalArgumentException("表长必须不小于 2,当前为 " + tableSize);
        }
        if (tableSize > MAX_TABLE_SIZE) {
            throw new IllegalArgumentException("表长过大:" + tableSize + ",上限 " + MAX_TABLE_SIZE);
        }
    }

    /**
     * 试除法判断质数。
     */
    private static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        for (int d = 2; (long) d * d <= n; d++) {
            if (n % d == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 内部查找:返回下标,-1 表示不存在;同时记录探测次数到 lastProbes。
     */
    private int find(int key) {
        int i = hash(key);
        for (int probes = 1; probes <= tableSize; probes++) {
            Entry e = table[i];
            if (e == null) { // 遇到空单元,说明关键字不存在
                lastProbes = probes;
                return -1;
            }
            if (e != TOMBSTONE && e.key == key) {
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
    private int probeCount(int key) {
        int i = hash(key);
        for (int probes = 1; probes <= tableSize; probes++) {
            Entry e = table[i];
            if (e == null) {
                return probes;
            }
            if (e != TOMBSTONE && e.key == key) {
                return probes;
            }
            i = (i + 1) % tableSize;
        }
        return tableSize;
    }

    /**
     * 演示:教材经典例题、模数选择对比、墓碑删除、负数取模。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("除留余数法散列函数:H(key) = key mod p");
        System.out.println();

        // 用例 1:经典例题 —— 表长 16,自动取 p = 13,线性探测再散列
        int[] keys = {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79};
        DivisionMutantFloorMod<String> st = new DivisionMutantFloorMod<>(16);
        System.out.println("用例 1:表长 m = " + st.tableSize() + ",模数 p = " + st.modulus()
                + ",关键字 " + Arrays.toString(keys));
        System.out.println("    关键字        初始地址 H(key)  实际地址        探测次数");
        for (int key : keys) {
            int init = st.hash(key);
            st.put(key, "v" + key);
            System.out.printf("    %-12d  %-14d  %-14d  %d%n", key, init, st.slotOf(key), st.lastProbes());
        }
        System.out.printf("    装填因子 alpha = %d/%d = %.4f%n", st.size(), st.tableSize(), st.loadFactor());
        System.out.printf("    ASL成功 = %d/%d = %.4f%n", st.successfulProbeSum(), st.size(), st.averageSuccessfulProbes());
        System.out.printf("    ASL失败 = %d/%d = %.4f(探测次数含最后判定为空的单元)%n%n",
                st.unsuccessfulProbeSum(), st.tableSize(), st.averageUnsuccessfulProbes());

        // 用例 2:模数选择 —— 质数 vs 2 的幂
        int[] evenKeys = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24};
        System.out.println("用例 2:同一组偶数关键字 " + Arrays.toString(evenKeys));
        reportDistribution(evenKeys, 13);
        reportDistribution(evenKeys, 16);
        System.out.println();

        // 用例 3:删除必须留墓碑,否则探测链断裂
        DivisionMutantFloorMod<String> tomb = new DivisionMutantFloorMod<>(7);
        tomb.put(1, "a");
        tomb.put(8, "b");   // H=1,冲突 -> 探测到 2
        tomb.put(15, "c");  // H=1,冲突 -> 探测到 3
        System.out.println("用例 3:表长 m = 7,H(key) = key mod 7");
        System.out.println("    插入 1, 8, 15 后的下标:" + tomb.slotOf(1) + ", " + tomb.slotOf(8) + ", " + tomb.slotOf(15));
        tomb.delete(1);
        System.out.println("    删除 1 后:墓碑数 = " + tomb.tombstones()
                + ",contains(8) = " + tomb.contains(8) + ",contains(15) = " + tomb.contains(15));
        tomb.put(22, "d"); // H=1,复用墓碑
        System.out.println("    插入 22 后:下标 = " + tomb.slotOf(22)
                + ",墓碑数 = " + tomb.tombstones() + ",size = " + tomb.size());
        System.out.println("    " + tomb);
        System.out.println();

        // 用例 4:负数取模
        DivisionMutantFloorMod<String> neg = new DivisionMutantFloorMod<>(13);
        System.out.println("用例 4:负数取模,p = " + neg.modulus()
                + ":hash(-1) = " + neg.hash(-1) + ",hash(-13) = " + neg.hash(-13)
                + ",hash(-26) = " + neg.hash(-26));
        neg.put(-1, "x");
        neg.put(-13, "y");
        neg.put(-26, "z"); // 与 -13 冲突 -> 探测到 1
        System.out.println("    -1, -13, -26 的下标:" + neg.slotOf(-1) + ", " + neg.slotOf(-13) + ", " + neg.slotOf(-26));
    }

    /**
     * 打印一组关键字在给定模数下的初始地址分布,用于比较模数选取的优劣。
     */
    private static void reportDistribution(int[] keys, int p) {
        TreeSet<Integer> distinct = new TreeSet<>();
        StringBuilder addresses = new StringBuilder();
        int collisions = 0;
        for (int key : keys) {
            int addr = Math.floorMod(key, p);
            if (!distinct.add(addr)) {
                collisions++;
            }
            addresses.append(addr).append(' ');
        }
        System.out.printf("    模数 p = %-3d 初始地址: %-46s 不同地址 %d 个,冲突 %d 次%n",
                p, addresses.toString().trim(), distinct.size(), collisions);
    }
}
