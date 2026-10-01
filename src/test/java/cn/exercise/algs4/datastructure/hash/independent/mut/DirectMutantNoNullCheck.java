package cn.exercise.algs4.datastructure.hash.independent.mut;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 直接定址法(Direct Addressing)散列表。
 *
 * <p>散列函数取关键字的线性函数:
 * <pre>
 *     H(key) = a * key + b      (a != 0)
 * </pre>
 * 数组下标就是地址本身,地址空间完整覆盖 [H(minKey), H(maxKey)] 中的每一个整数,
 * 因此只要 a != 0,不同的关键字必然映射到不同地址,<b>不产生冲突</b>,
 * 查找、插入、删除的时间复杂度都是 O(1)(一次定址,无需探测或拉链)。</p>
 *
 * <p>两个常用特例:</p>
 * <ul>
 *   <li>a = 1,b = -minKey,即 {@code H(key) = key - minKey};关键字为连续区间
 *       (如年龄 18~65、学号 20210101~20210110)时用它,地址空间恰好装下所有关键字。</li>
 *   <li>a = 1,b = 0,即 {@code H(key) = key};关键字本身即地址,一般要求关键字从 0 开始。</li>
 * </ul>
 *
 * <p><b>缺点</b>:地址空间由关键字的取值范围(而不是元素个数)决定。若关键字分布稀疏,
 * 例如 {1, 1000, 10000} 用 H(key) = key 会开出 10001 个单元、装填因子不到万分之一,
 * 此时应改用除留余数法、数字分析法、平方取中法等散列函数。</p>
 *
 * <p>本实现不支持 null 值:数组中的 null 专门用于表示"该地址为空"。</p>
 *
 * @param <Value> 关键字关联的值类型
 */
public class DirectMutantNoNullCheck<Value> {

    /**
     * 地址空间上限:2^26 = 67108864 个单元。
     * 超过此值几乎必然是误传了关键字区间,直接拒绝,避免 JVM 因分配超大数组而 OutOfMemoryError。
     */
    private static final int MAX_CAPACITY = 1 << 26;

    /** 散列函数的一次项系数 a(H(key) = a * key + b) */
    private final long a;

    /** 散列函数的常数项 b */
    private final long b;

    /** 允许的关键字下界(闭区间) */
    private final int minKey;

    /** 允许的关键字上界(闭区间) */
    private final int maxKey;

    /** 地址空间下界,即 H(key) 能取到的最小值 */
    private final int minAddr;

    /** 地址空间大小,即散列表长度 */
    private final int capacity;

    /** 地址 -> 值,下标即地址 */
    private final Object[] values;

    /** 已存放的关键字个数 */
    private int n;

    /**
     * 使用最常用的直接定址函数 H(key) = key - minKey 构造散列表。
     *
     * @param minKey 允许的最小关键字
     * @param maxKey 允许的最大关键字
     */
    public DirectMutantNoNullCheck(int minKey, int maxKey) {
        this(minKey, maxKey, 1L, -(long) minKey);
    }

    /**
     * 使用自定义的直接定址函数 H(key) = a * key + b 构造散列表。
     *
     * @param minKey 允许的最小关键字
     * @param maxKey 允许的最大关键字
     * @param a      一次项系数,不能为 0
     * @param b      常数项
     * @throws IllegalArgumentException 关键字区间非法、a 为 0 或地址空间超过 int 上限
     */
    public DirectMutantNoNullCheck(int minKey, int maxKey, long a, long b) {
        if (minKey > maxKey) {
            throw new IllegalArgumentException("关键字下界 " + minKey + " 不能大于上界 " + maxKey);
        }
        if (a == 0) {
            throw new IllegalArgumentException("系数 a 不能为 0,否则所有关键字都映射到同一个地址");
        }
        long lo = a * minKey + b;
        long hi = a * maxKey + b;
        if (lo > hi) {
            long tmp = lo;
            lo = hi;
            hi = tmp;
        }
        long size = hi - lo + 1;
        if (size > MAX_CAPACITY) {
            throw new IllegalArgumentException("地址空间过大:" + size + " 个单元(上限 " + MAX_CAPACITY
                    + "),请改用除留余数法等散列函数");
        }
        this.minKey = minKey;
        this.maxKey = maxKey;
        this.a = a;
        this.b = b;
        this.minAddr = (int) lo;
        this.capacity = (int) size;
        this.values = new Object[this.capacity];
    }

    /**
     * 散列函数 H(key) = a * key + b,返回值直接作为数组下标。
     *
     * @param key 关键字
     * @return 地址,落在 [0, capacity) 内
     * @throws IllegalArgumentException 关键字越界
     */
    public int hash(int key) {
        if (key < minKey || key > maxKey) {
            throw new IllegalArgumentException(
                    "关键字 " + key + " 越界,允许范围 [" + minKey + ", " + maxKey + "]");
        }
        return (int) (a * key + b - minAddr);
    }

    /**
     * 把键值对放入散列表。关键字已存在时覆盖旧值。
     *
     * @param key   关键字
     * @param value 值,不能为 null
     */
    public void put(int key, Value value) {
        if (false) Objects.requireNonNull(value, "值不能为 null");
        int addr = hash(key);
        if (values[addr] == null) {
            n++;
        }
        values[addr] = value;
    }

    /**
     * 取出关键字对应的值。
     *
     * @param key 关键字
     * @return 值;关键字不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public Value get(int key) {
        return (Value) values[hash(key)];
    }

    /**
     * 删除关键字及其值。
     *
     * @param key 关键字
     * @return 被删除的值;关键字不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public Value delete(int key) {
        int addr = hash(key);
        Value old = (Value) values[addr];
        if (old != null) {
            values[addr] = null;
            n--;
        }
        return old;
    }

    /**
     * 判断关键字是否存在。
     *
     * @param key 关键字
     * @return 存在返回 true
     */
    public boolean contains(int key) {
        return values[hash(key)] != null;
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
     * @return 地址空间大小,即散列表长度 = H(maxKey) - H(minKey) + 1
     */
    public int capacity() {
        return capacity;
    }

    /**
     * @return 装填因子 alpha = n / capacity
     */
    public double loadFactor() {
        return (double) n / capacity;
    }

    /**
     * @return 散列函数的一次项系数 a
     */
    public long coefficientA() {
        return a;
    }

    /**
     * @return 散列函数的常数项 b
     */
    public long constantB() {
        return b;
    }

    /**
     * 依次返回散列表中所有关键字(按地址升序)。
     *
     * @return 关键字集合
     */
    public Iterable<Integer> keys() {
        List<Integer> result = new ArrayList<>(n);
        for (int addr = 0; addr < capacity; addr++) {
            if (values[addr] != null) {
                // 由地址反推关键字:addr + minAddr = a * key + b,因为 a != 0 且整除,不存在舍入误差
                result.add((int) ((addr + (long) minAddr - b) / a));
            }
        }
        return result;
    }

    /**
     * 按键的升序输出所有键值对,格式与 algs4 符号表一致:{key value, key value}。
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
     * 演示:三种典型关键字分布下的直接定址效果,并打印每个关键字的地址。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("直接定址法散列函数:H(key) = a * key + b");
        System.out.println();

        DirectMutantNoNullCheck<String> byId = new DirectMutantNoNullCheck<>(20210101, 20210110);
        int[] ids = {20210101, 20210102, 20210105, 20210110};
        String[] names = {"张三", "李四", "王五", "赵六"};
        for (int i = 0; i < ids.length; i++) {
            byId.put(ids[i], names[i]);
        }
        report(byId, ids, "用例 1:学号 20210101~20210110,H(key) = key - 20210101");

        DirectMutantNoNullCheck<String> byAge = new DirectMutantNoNullCheck<>(18, 65);
        int[] ageKeys = {19, 23, 41, 65};
        for (int i = 0; i < ageKeys.length; i++) {
            byAge.put(ageKeys[i], names[i]);
        }
        report(byAge, ageKeys, "用例 2:年龄 18~65,H(key) = key - 18(地址与关键字一一对应)");

        DirectMutantNoNullCheck<String> sparse = new DirectMutantNoNullCheck<>(1, 10000);
        int[] sparseKeys = {1, 1000, 10000};
        for (int i = 0; i < sparseKeys.length; i++) {
            sparse.put(sparseKeys[i], names[i]);
        }
        report(sparse, sparseKeys, "用例 3:稀疏关键字 {1, 1000, 10000},H(key) = key(空间浪费极大)");

        DirectMutantNoNullCheck<String> scaled = new DirectMutantNoNullCheck<>(3, 7, 2L, 1L);
        int[] scaledKeys = {3, 4, 5, 6, 7};
        for (int i = 0; i < scaledKeys.length; i++) {
            scaled.put(scaledKeys[i], names[i % names.length]);
        }
        report(scaled, scaledKeys, "用例 4:自定义系数 H(key) = 2 * key + 1,关键字 3~7");

        System.out.println("查询演示:byAge.get(41) = " + byAge.get(41)
                + ",byAge.contains(18) = " + byAge.contains(18)
                + ",byAge.delete(41) = " + byAge.delete(41)
                + ",删除后 size = " + byAge.size());
    }

    /**
     * 打印一组关键字的"关键字 -> 地址"对照表及统计信息。
     *
     * @param st    已插入数据的散列表
     * @param keys  参与统计的关键字
     * @param title 用例标题
     */
    private static void report(DirectMutantNoNullCheck<String> st, int[] keys, String title) {
        System.out.println(title);
        System.out.println("    关键字          地址      值");
        boolean[] used = new boolean[st.capacity()];
        int collisions = 0;
        for (int key : keys) {
            int addr = st.hash(key);
            if (used[addr]) {
                collisions++;
            }
            used[addr] = true;
            System.out.printf("    %-14d  %-6d  %s%n", key, addr, st.get(key));
        }
        System.out.printf("    冲突次数 = %d(一次定址,无需冲突处理);地址空间 = %d,已用 = %d,装填因子 = %.4f%n%n",
                collisions, st.capacity(), st.size(), st.loadFactor());
    }
}
