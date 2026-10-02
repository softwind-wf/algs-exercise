package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 穷举测试(小状态空间)+ 容量感知的随机差分测试。
 *
 * <p>与前两类测试的差别:</p>
 * <ul>
 *   <li>差分测试是**随机抽样**,可能碰巧绕开某些操作顺序;</li>
 *   <li>本类对**小表 + 少量关键字**穷举长度 ≤ N 的**全部** put/delete 操作序列,
 *       每一步都与 {@link HashMap} 全面比对(size/isEmpty/get/contains/keys),
 *       因此这类参数下不存在"没抽到"的序列;</li>
 *   <li>并断言**实际枚举的节点数**,防止"枚举没跑满却全绿"的假通过。</li>
 * </ul>
 *
 * <p>覆盖的边界:满表拒插(IllegalStateException)、删除留墓碑、墓碑复用、
 * 连续探测链的增删交织、以及装填因子到 1.0 时的行为。</p>
 */
@DisplayName("散列表穷举与容量感知差分测试")
class HashSTExhaustiveTest {

    /** 本次枚举实际比对的序列数,用于断言枚举确实跑满 */
    private static int checked;

    @Test
    @DisplayName("除留余数法 m=4、p=3:长度 ≤7 的全部 put/delete 序列逐步对照 HashMap")
    void divisionHashExhaustive_depth7() {
        checked = 0;
        int[] keys = {0, 1, 2};
        int[] path = new int[7];
        dfsDivision(keys, 4, path, 0, 7);
        assertEquals(335922, checked, "枚举节点数与 sum(6^k, k=1..7) 不符,枚举没有跑满");
    }

    @Test
    @DisplayName("除留余数法 m=2、p=2:满表拒插与墓碑复用被穷举覆盖")
    void divisionHashExhaustive_fullTable() {
        checked = 0;
        int[] keys = {0, 1};
        int[] path = new int[8];
        dfsDivision(keys, 2, path, 0, 8);
        assertEquals(87380, checked, "枚举节点数与 sum(4^k, k=1..8) 不符,枚举没有跑满");
    }

    @Test
    @DisplayName("直接定址法区间 [0,2]:长度 ≤6 的全部 put/delete 序列逐步对照 HashMap")
    void directAddressExhaustive_depth6() {
        checked = 0;
        int[] keys = {0, 1, 2};
        int[] path = new int[6];
        dfsDirect(keys, path, 0, 6);
        assertEquals(55986, checked, "枚举节点数与 sum(6^k, k=1..6) 不符,枚举没有跑满");
    }

    @Test
    @DisplayName("近满表随机差分:m=8、装填因子最高到 1.0,满表拒插与墓碑复用都参与比对")
    void nearFullRandomDifferential() {
        int tableSize = 8; // p = 7
        DivisionHashST<String> st = new DivisionHashST<String>(tableSize);
        Map<Integer, String> model = new HashMap<Integer, String>();
        Random random = new Random(20261001L);
        int refused = 0;

        for (int i = 0; i < 20000; i++) {
            int key = random.nextInt(30);
            int op = random.nextInt(3);
            if (op < 2) {
                String value = "v" + i;
                boolean rejected = false;
                try {
                    st.put(key, value);
                } catch (IllegalStateException e) {
                    rejected = true;
                }
                if (rejected) {
                    refused++;
                    assertEquals(tableSize, st.size(), "拒绝插入时表必须已满(无空单元且无墓碑)");
                    assertFalse(model.containsKey(key), "被拒绝的键不应已经存在");
                } else {
                    model.put(key, value);
                }
            } else {
                assertEquals(model.remove(key), st.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }

            assertEquals(model.size(), st.size(), "第 " + i + " 步 size()");
            assertEquals(model.isEmpty(), st.isEmpty(), "第 " + i + " 步 isEmpty()");
            if (i % 97 == 0) {
                assertEquals(model.get(key), st.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(model.containsKey(key), st.contains(key), "第 " + i + " 步 contains(" + key + ")");
                TreeSet<Integer> actual = new TreeSet<Integer>();
                st.keys().forEach(actual::add);
                assertEquals(new TreeSet<Integer>(model.keySet()), actual, "第 " + i + " 步 keys()");
            }
        }
        assertTrue(refused > 0, "该场景从未触发满表拒插,边界路径实际未被覆盖");
    }

    @Test
    @DisplayName("极值关键字:Integer.MIN_VALUE / MAX_VALUE 不产生溢出或越界")
    void extremeKeyValues() {
        DirectAddressHashST<String> minOnly =
                new DirectAddressHashST<String>(Integer.MIN_VALUE, Integer.MIN_VALUE);
        assertEquals(1, minOnly.capacity());
        assertEquals(0, minOnly.hash(Integer.MIN_VALUE));
        minOnly.put(Integer.MIN_VALUE, "lo");
        assertEquals("lo", minOnly.get(Integer.MIN_VALUE));
        for (int k : minOnly.keys()) {
            assertEquals(Integer.MIN_VALUE, k);
        }

        DirectAddressHashST<String> top =
                new DirectAddressHashST<String>(Integer.MAX_VALUE - 2, Integer.MAX_VALUE);
        assertEquals(3, top.capacity());
        assertEquals(0, top.hash(Integer.MAX_VALUE - 2));
        assertEquals(2, top.hash(Integer.MAX_VALUE));
        top.put(Integer.MAX_VALUE, "hi");
        assertEquals("hi", top.get(Integer.MAX_VALUE));

        DivisionHashST<String> div = new DivisionHashST<String>(16); // p = 13
        int[] extremes = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        for (int key : extremes) {
            int addr = div.hash(key);
            assertTrue(addr >= 0 && addr < div.modulus(), "地址越界: " + key + " -> " + addr);
            div.put(key, "k" + key);
            assertEquals("k" + key, div.get(key));
        }
        assertEquals(4, div.size());
    }

    @Test
    @DisplayName("数字分析法(地址空间压到 10、三键同桶):长度 ≤6 的全部 put/delete 序列逐步对照 HashMap")
    void digitAnalysisExhaustive_chain() {
        checked = 0;
        long[] keys = {5, 15, 25}; // 取第 0 位作地址 → 三者恒在同一桶
        int[] path = new int[6];
        dfsDigit(keys, path, 0, 6);
        assertEquals(55986, checked, "枚举节点数与 sum(6^k, k=1..6) 不符,枚举没有跑满");
    }

    @Test
    @DisplayName("平方取中法(表长 10、三键同地址):长度 ≤6 的全部 put/delete 序列逐步对照 HashMap")
    void midSquareExhaustive_probing() {
        checked = 0;
        long[] keys = {2, 12, 38}; // 平方取中 k=1 时三者都映射到地址 4
        int[] path = new int[6];
        dfsMidSquare(keys, path, 0, 6);
        assertEquals(55986, checked, "枚举节点数与 sum(6^k, k=1..6) 不符,枚举没有跑满");
    }

    // ---------------- 穷举骨架 ----------------

    private static void dfsDivision(int[] keys, int tableSize, int[] path, int len, int maxDepth) {
        if (len > 0) {
            checkDivision(keys, tableSize, path, len);
        }
        if (len == maxDepth) {
            return;
        }
        for (int op = 0; op < 2 * keys.length; op++) {
            path[len] = op;
            dfsDivision(keys, tableSize, path, len + 1, maxDepth);
        }
    }

    private static void checkDivision(int[] keys, int tableSize, int[] path, int len) {
        checked++;
        DivisionHashST<String> st = new DivisionHashST<String>(tableSize);
        Map<Integer, String> model = new HashMap<Integer, String>();
        for (int i = 0; i < len; i++) {
            int op = path[i];
            int key = keys[op % keys.length];
            if (op < keys.length) {
                String value = "v" + i;
                boolean rejected = false;
                try {
                    st.put(key, value);
                } catch (IllegalStateException e) {
                    rejected = true;
                }
                if (rejected) {
                    assertTrue(st.size() == tableSize, "拒绝插入时表竟然未满:" + describe(path, len));
                    assertFalse(model.containsKey(key), "被拒绝的键不应已经存在:" + describe(path, len));
                } else {
                    model.put(key, value);
                }
            } else {
                assertEquals(model.remove(key), st.delete(key), "delete(" + key + "):" + describe(path, len));
            }
            compareDivision(keys, st, model, path, len);
        }
    }

    private static void compareDivision(int[] keys, DivisionHashST<String> st,
                                        Map<Integer, String> model, int[] path, int len) {
        String ctx = describe(path, len);
        assertEquals(model.size(), st.size(), ctx + " size");
        assertEquals(model.isEmpty(), st.isEmpty(), ctx + " isEmpty");
        for (int key : keys) {
            assertEquals(model.get(key), st.get(key), ctx + " get(" + key + ")");
            assertEquals(model.containsKey(key), st.contains(key), ctx + " contains(" + key + ")");
        }
        TreeSet<Integer> actual = new TreeSet<Integer>();
        st.keys().forEach(actual::add);
        assertEquals(new TreeSet<Integer>(model.keySet()), actual, ctx + " keys()");
    }

    /** 0~99 样本:两位都均匀;配 maxCapacity=10 时只选第 0 位,地址 = key mod 10 */
    private static long[] digitSample() {
        long[] sample = new long[100];
        for (int i = 0; i < sample.length; i++) {
            sample[i] = i;
        }
        return sample;
    }

    private static void dfsDigit(long[] keys, int[] path, int len, int maxDepth) {
        if (len > 0) {
            checkDigit(keys, path, len);
        }
        if (len == maxDepth) {
            return;
        }
        for (int op = 0; op < 2 * keys.length; op++) {
            path[len] = op;
            dfsDigit(keys, path, len + 1, maxDepth);
        }
    }

    private static void checkDigit(long[] keys, int[] path, int len) {
        checked++;
        DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(digitSample(), 2, 10);
        Map<Long, String> model = new HashMap<Long, String>();
        for (int i = 0; i < len; i++) {
            int op = path[i];
            long key = keys[op % keys.length];
            if (op < keys.length) {
                String value = "v" + i;
                st.put(key, value);
                model.put(key, value);
            } else {
                assertEquals(model.remove(key), st.delete(key), "delete(" + key + "):" + describe(path, len));
            }
            String ctx = describe(path, len);
            assertEquals(model.size(), st.size(), ctx + " size");
            assertEquals(model.isEmpty(), st.isEmpty(), ctx + " isEmpty");
            for (long k : keys) {
                assertEquals(model.get(k), st.get(k), ctx + " get(" + k + ")");
                assertEquals(model.containsKey(k), st.contains(k), ctx + " contains(" + k + ")");
            }
            TreeSet<Long> actual = new TreeSet<Long>();
            st.keys().forEach(actual::add);
            assertEquals(new TreeSet<Long>(model.keySet()), actual, ctx + " keys()");
        }
    }

    private static void dfsMidSquare(long[] keys, int[] path, int len, int maxDepth) {
        if (len > 0) {
            checkMidSquare(keys, path, len);
        }
        if (len == maxDepth) {
            return;
        }
        for (int op = 0; op < 2 * keys.length; op++) {
            path[len] = op;
            dfsMidSquare(keys, path, len + 1, maxDepth);
        }
    }

    private static void checkMidSquare(long[] keys, int[] path, int len) {
        checked++;
        MidSquareHashST<String> st = new MidSquareHashST<String>(1); // 表长 10
        Map<Long, String> model = new HashMap<Long, String>();
        for (int i = 0; i < len; i++) {
            int op = path[i];
            long key = keys[op % keys.length];
            if (op < keys.length) {
                String value = "v" + i;
                boolean rejected = false;
                try {
                    st.put(key, value);
                } catch (IllegalStateException e) {
                    rejected = true;
                }
                if (rejected) {
                    assertEquals(10, st.size(), "拒绝插入时表竟然未满:" + describe(path, len));
                    assertFalse(model.containsKey(key), "被拒绝的键不应已经存在:" + describe(path, len));
                } else {
                    model.put(key, value);
                }
            } else {
                assertEquals(model.remove(key), st.delete(key), "delete(" + key + "):" + describe(path, len));
            }
            String ctx = describe(path, len);
            assertEquals(model.size(), st.size(), ctx + " size");
            assertEquals(model.isEmpty(), st.isEmpty(), ctx + " isEmpty");
            for (long k : keys) {
                assertEquals(model.get(k), st.get(k), ctx + " get(" + k + ")");
                assertEquals(model.containsKey(k), st.contains(k), ctx + " contains(" + k + ")");
            }
            TreeSet<Long> actual = new TreeSet<Long>();
            st.keys().forEach(actual::add);
            assertEquals(new TreeSet<Long>(model.keySet()), actual, ctx + " keys()");
        }
    }

    private static void dfsDirect(int[] keys, int[] path, int len, int maxDepth) {        if (len > 0) {
            checkDirect(keys, path, len);
        }
        if (len == maxDepth) {
            return;
        }
        for (int op = 0; op < 2 * keys.length; op++) {
            path[len] = op;
            dfsDirect(keys, path, len + 1, maxDepth);
        }
    }

    private static void checkDirect(int[] keys, int[] path, int len) {
        checked++;
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(keys[0], keys[keys.length - 1]);
        Map<Integer, String> model = new HashMap<Integer, String>();
        for (int i = 0; i < len; i++) {
            int op = path[i];
            int key = keys[op % keys.length];
            if (op < keys.length) {
                String value = "v" + i;
                st.put(key, value);
                model.put(key, value);
            } else {
                assertEquals(model.remove(key), st.delete(key), "delete(" + key + "):" + describe(path, len));
            }
            String ctx = describe(path, len);
            assertEquals(model.size(), st.size(), ctx + " size");
            for (int k : keys) {
                assertEquals(model.get(k), st.get(k), ctx + " get(" + k + ")");
                assertEquals(model.containsKey(k), st.contains(k), ctx + " contains(" + k + ")");
            }
            TreeSet<Integer> actual = new TreeSet<Integer>();
            st.keys().forEach(actual::add);
            assertEquals(new TreeSet<Integer>(model.keySet()), actual, ctx + " keys()");
        }
    }

    private static String describe(int[] path, int len) {
        StringBuilder sb = new StringBuilder("序列[");
        for (int i = 0; i < len; i++) {
            sb.append(i == 0 ? "" : ",").append(path[i]);
        }
        return sb.append(']').toString();
    }
}
