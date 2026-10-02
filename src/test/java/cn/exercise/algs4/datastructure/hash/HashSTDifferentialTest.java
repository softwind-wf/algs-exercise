package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 差分测试(differential testing):用与实现无关的参照物交叉验证。
 *
 * <p>本测试不预设"我认为正确的答案",而是把随机操作序列同时施加于被测散列表和
 * JDK 的 {@link HashMap},逐操作比较 get / contains / delete / size / isEmpty / keys 的结果。
 * HashMap 是独立实现(且经过工业级验证),因此能发现"实现与测试共享同一误解"这类自测盲区。</p>
 *
 * <p>另外用一段独立编写的线性探测模拟程序重算 ASL 的分子,交叉验证
 * {@link DivisionHashST#successfulProbeSum()} 与 {@link DivisionHashST#unsuccessfulProbeSum()}。</p>
 *
 * <p>随机数使用固定种子,保证结果可复现,不是概率性通过。</p>
 */
@DisplayName("散列表差分测试(参照 java.util.HashMap)")
class HashSTDifferentialTest {

    /** 固定随机种子,保证可复现 */
    private static final long SEED = 20261001L;

    /** 每个场景的操作次数 */
    private static final int OPS = 20000;

    @Test
    @DisplayName("DivisionHashST 与 HashMap 在 2 万次随机操作后完全一致")
    void divisionMatchesHashMap() {
        DivisionHashST<String> actual = new DivisionHashST<>(64); // p = 61
        Map<Integer, String> expected = new HashMap<Integer, String>();
        Random random = new Random(SEED);
        int maxLive = 30; // 控制装填因子,避免定长散列表被填满

        for (int i = 0; i < OPS; i++) {
            int key = random.nextInt(200) - 100; // 含负数,覆盖 floorMod 分支
            int op = random.nextInt(4);
            if (op < 2) {
                if (actual.size() >= maxLive && !expected.containsKey(key)) {
                    continue;
                }
                String value = "v" + i;
                actual.put(key, value);
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            assertEquals(expected.isEmpty(), actual.isEmpty(), "第 " + i + " 步 isEmpty()");
        }

        TreeSet<Integer> actualKeys = new TreeSet<Integer>();
        actual.keys().forEach(actualKeys::add);
        assertEquals(new TreeSet<Integer>(expected.keySet()), actualKeys, "keys() 与 HashMap 键集不一致");
    }

    @Test
    @DisplayName("DirectAddressHashST 与 HashMap 在 2 万次随机操作后完全一致")
    void directAddressMatchesHashMap() {
        DirectAddressHashST<String> actual = new DirectAddressHashST<>(-50, 149);
        Map<Integer, String> expected = new HashMap<Integer, String>();
        Random random = new Random(SEED + 1);

        for (int i = 0; i < OPS; i++) {
            int key = random.nextInt(200) - 50;
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                actual.put(key, value);
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
        }

        TreeSet<Integer> actualKeys = new TreeSet<Integer>();
        actual.keys().forEach(actualKeys::add);
        assertEquals(new TreeSet<Integer>(expected.keySet()), actualKeys, "keys() 与 HashMap 键集不一致");
    }

    @Test
    @DisplayName("DigitAnalysisHashST 与 HashMap 在 2 万次随机操作后完全一致")
    void digitAnalysisMatchesHashMap() {
        long[] sample = new long[500];
        for (int i = 0; i < sample.length; i++) {
            sample[i] = 20210000L + (i * 37L) % 10000; // 8 位学号样本
        }
        DigitAnalysisHashST<String> actual = new DigitAnalysisHashST<String>(sample, 8, 1000);
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(SEED + 3);

        for (int i = 0; i < OPS; i++) {
            long key = 20210000L + random.nextInt(10000);
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                actual.put(key, value);
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            assertEquals(expected.isEmpty(), actual.isEmpty(), "第 " + i + " 步 isEmpty()");
        }

        TreeSet<Long> actualKeys = new TreeSet<Long>();
        actual.keys().forEach(actualKeys::add);
        assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, "keys() 与 HashMap 键集不一致");
    }

    @Test
    @DisplayName("MidSquareHashST 与 HashMap 在 2 万次随机操作后完全一致(含满表拒插)")
    void midSquareMatchesHashMap() {
        MidSquareHashST<String> actual = new MidSquareHashST<String>(1); // 表长 10,快速进入满表区
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(SEED + 4);
        int refused = 0;

        for (int i = 0; i < OPS; i++) {
            long key = random.nextInt(2000);
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                boolean rejected = false;
                try {
                    actual.put(key, value);
                } catch (IllegalStateException e) {
                    rejected = true;
                }
                if (rejected) {
                    refused++;
                    assertEquals(10, actual.size(), "拒绝插入时表必须已满(无空槽且无墓碑)");
                    assertFalse(expected.containsKey(key), "被拒绝的键不应已经存在");
                } else {
                    expected.put(key, value);
                }
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            if (i % 97 == 0) {
                TreeSet<Long> actualKeys = new TreeSet<Long>();
                actual.keys().forEach(actualKeys::add);
                assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, "第 " + i + " 步 keys()");
            }
        }
        assertTrue(refused > 0, "该场景从未触发满表拒插,边界路径实际未被覆盖");
    }

    @Test
    @DisplayName("FoldingHashST 与 HashMap 在 2 万次随机操作后完全一致(两种叠加方式各一遍)")
    void foldingMatchesHashMap() {
        verifyFolding(FoldingHashST.Mode.SHIFT, SEED + 5);
        verifyFolding(FoldingHashST.Mode.BOUNDARY, SEED + 6);
    }

    private void verifyFolding(FoldingHashST.Mode mode, long seed) {
        FoldingHashST<String> actual = new FoldingHashST<String>(2, 64, mode); // 段长 2,表长 64
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(seed);

        for (int i = 0; i < OPS; i++) {
            long key = random.nextInt(100000);
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                actual.put(key, value);
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key),
                        mode + " 第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        mode + " 第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key),
                        mode + " 第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), mode + " 第 " + i + " 步 size()");
            assertEquals(expected.isEmpty(), actual.isEmpty(), mode + " 第 " + i + " 步 isEmpty()");
        }

        TreeSet<Long> actualKeys = new TreeSet<Long>();
        actual.keys().forEach(actualKeys::add);
        assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, mode + " keys() 与 HashMap 键集不一致");
    }

    @Test
    @DisplayName("RandomHashST 与 HashMap 在 2 万次随机操作后完全一致(含满表拒插)")
    void randomMatchesHashMap() {
        RandomHashST<String> actual = new RandomHashST<String>(16);
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(SEED + 7);
        int refused = 0;

        for (int i = 0; i < OPS; i++) {
            long key = random.nextInt(200) - 100; // 含负数:随机数法不要求非负
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                boolean rejected = false;
                try {
                    actual.put(key, value);
                } catch (IllegalStateException e) {
                    rejected = true;
                }
                if (rejected) {
                    refused++;
                    assertEquals(16, actual.size(), "拒绝插入时表必须已满(无空槽且无墓碑)");
                    assertFalse(expected.containsKey(key), "被拒绝的键不应已经存在");
                } else {
                    expected.put(key, value);
                }
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            if (i % 97 == 0) {
                TreeSet<Long> actualKeys = new TreeSet<Long>();
                actual.keys().forEach(actualKeys::add);
                assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, "第 " + i + " 步 keys()");
            }
        }
        assertTrue(refused > 0, "该场景从未触发满表拒插,边界路径实际未被覆盖");
    }

    @Test
    @DisplayName("UniversalHashST 与 HashMap 在 2 万次随机操作后完全一致(固定种子的全域族成员)")
    void universalMatchesHashMap() {
        UniversalHashST<String> actual = new UniversalHashST<String>(64, 1009, SEED + 8);
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(SEED + 9);

        for (int i = 0; i < OPS; i++) {
            long key = random.nextInt(1009); // 必须在 [0, p-1]
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                actual.put(key, value);
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            assertEquals(expected.isEmpty(), actual.isEmpty(), "第 " + i + " 步 isEmpty()");
        }

        TreeSet<Long> actualKeys = new TreeSet<Long>();
        actual.keys().forEach(actualKeys::add);
        assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, "keys() 与 HashMap 键集不一致");
    }

    @Test
    @DisplayName("OpenAddressHashST(线性探测)与 HashMap 在 2 万次随机操作后完全一致")
    void openAddressMatchesHashMap() {
        OpenAddressHashST<String> actual = new OpenAddressHashST<String>(8, 0.5); // 小容量 + 低上限 → 频繁扩容
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(SEED + 10);

        for (int i = 0; i < OPS; i++) {
            long key = random.nextInt(5000);
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                actual.put(key, value);
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            if (i % 97 == 0) {
                assertTrue(actual.loadFactor() <= actual.maxLoadFactor(), "第 " + i + " 步 α 超过上限");
                TreeSet<Long> actualKeys = new TreeSet<Long>();
                actual.keys().forEach(actualKeys::add);
                assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, "第 " + i + " 步 keys()");
            }
        }
        assertTrue(actual.resizeCount() > 0, "该场景应触发过扩容");
    }

    @Test
    @DisplayName("QuadraticProbeHashST(二次探测)与 HashMap 在 2 万次随机操作后完全一致")
    void quadraticMatchesHashMap() {
        // ALTERNATING + m ≡ 3 (mod 4) → 全表覆盖,扩容后仍保持,因此不会出现"轨道走完"的失败
        QuadraticProbeHashST<String> actual = new QuadraticProbeHashST<String>(7, 0.5,
                QuadraticProbeHashST.Mode.ALTERNATING);
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(SEED + 11);

        for (int i = 0; i < OPS; i++) {
            long key = random.nextInt(5000);
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                actual.put(key, value);
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            if (i % 97 == 0) {
                assertTrue(actual.loadFactor() <= actual.maxLoadFactor(), "第 " + i + " 步 α 超过上限");
                assertTrue(actual.fullCoverageGuaranteed() || actual.mode() != QuadraticProbeHashST.Mode.ALTERNATING,
                        "ALTERNATING 模式应始终保持 m ≡ 3 (mod 4)");
                TreeSet<Long> actualKeys = new TreeSet<Long>();
                actual.keys().forEach(actualKeys::add);
                assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, "第 " + i + " 步 keys()");
            }
        }
        assertTrue(actual.resizeCount() > 0, "该场景应触发过扩容");
    }

    @Test
    @DisplayName("DoubleHashingHashST(双重散列)与 HashMap 在 2 万次随机操作后完全一致")
    void doubleHashingMatchesHashMap() {
        DoubleHashingHashST<String> actual = new DoubleHashingHashST<String>(7, 0.5); // 容量保持素数 → 全表覆盖
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(SEED + 12);

        for (int i = 0; i < OPS; i++) {
            long key = random.nextInt(5000);
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                actual.put(key, value);
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            if (i % 97 == 0) {
                assertTrue(actual.loadFactor() <= actual.maxLoadFactor(), "第 " + i + " 步 α 超过上限");
                assertTrue(actual.fullCoverageGuaranteed(), "容量应始终保持素数 → 全表覆盖");
                TreeSet<Long> actualKeys = new TreeSet<Long>();
                actual.keys().forEach(actualKeys::add);
                assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, "第 " + i + " 步 keys()");
            }
        }
        assertTrue(actual.resizeCount() > 0, "该场景应触发过扩容");
    }

    @Test
    @DisplayName("SeparateChainingHashST(链地址法)与 HashMap 在 2 万次随机操作后完全一致")
    void chainingMatchesHashMap() {
        SeparateChainingHashST<String> actual = new SeparateChainingHashST<String>(7, 0.75); // 会多次扩容
        Map<Long, String> expected = new HashMap<Long, String>();
        Random random = new Random(SEED + 13);

        for (int i = 0; i < OPS; i++) {
            long key = random.nextInt(5000);
            int op = random.nextInt(4);
            if (op < 2) {
                String value = "v" + i;
                actual.put(key, value); // 链地址法不会拒插
                expected.put(key, value);
            } else if (op == 2) {
                assertEquals(expected.get(key), actual.get(key), "第 " + i + " 步 get(" + key + ")");
                assertEquals(expected.containsKey(key), actual.contains(key),
                        "第 " + i + " 步 contains(" + key + ")");
            } else {
                assertEquals(expected.remove(key), actual.delete(key), "第 " + i + " 步 delete(" + key + ")");
            }
            assertEquals(expected.size(), actual.size(), "第 " + i + " 步 size()");
            if (i % 97 == 0) {
                assertTrue(actual.loadFactor() <= actual.maxLoadFactor(), "第 " + i + " 步 α 超过上限");
                TreeSet<Long> actualKeys = new TreeSet<Long>();
                actual.keys().forEach(actualKeys::add);
                assertEquals(new TreeSet<Long>(expected.keySet()), actualKeys, "第 " + i + " 步 keys()");
            }
        }
        assertTrue(actual.resizeCount() > 0, "该场景应触发过扩容");
    }

    @Test
    @DisplayName("PerfectHashingST(完全散列)构建后与 HashMap 的查找结果完全一致")
    void perfectHashingMatchesHashMap() {
        Random random = new Random(SEED + 14);
        TreeSet<Long> keySet = new TreeSet<Long>();
        while (keySet.size() < 2000) {
            keySet.add((long) random.nextInt((int) PerfectHashingST.MAX_KEY + 1));
        }
        long[] keys = new long[keySet.size()];
        int index = 0;
        for (long key : keySet) {
            keys[index++] = key;
        }
        String[] values = new String[keys.length];
        Map<Long, String> expected = new HashMap<Long, String>();
        for (int i = 0; i < keys.length; i++) {
            values[i] = "v" + i;
            expected.put(keys[i], values[i]);
        }
        PerfectHashingST<String> actual = PerfectHashingST.build(keys, values, SEED);

        assertEquals(expected.size(), actual.size());
        for (Long key : expected.keySet()) {
            assertEquals(expected.get(key), actual.get(key), "命中 " + key);
            assertTrue(actual.contains(key));
        }
        int misses = 0;
        for (int i = 0; i < 2000; i++) {
            long candidate = (long) random.nextInt((int) PerfectHashingST.MAX_KEY + 1);
            if (expected.containsKey(candidate)) {
                continue;
            }
            assertNull(actual.get(candidate), "不该命中 " + candidate);
            assertFalse(actual.contains(candidate));
            misses++;
        }
        assertTrue(misses > 1900, "缺席关键字样本太少:" + misses);
        assertTrue(actual.worstCaseProbes() <= 2);
        assertTrue(actual.totalSubTableSlots() <= 4 * actual.size());
    }

    @Test
    @DisplayName("随机 10 张散列表的探测次数与独立模拟程序一致")
    void randomTablesMatchIndependentSimulation() {
        Random random = new Random(SEED + 2);
        for (int trial = 0; trial < 10; trial++) {
            int tableSize = 23;             // p = 23
            int keyCount = 12 + trial;      // 12~21 个关键字,避免装满
            DivisionHashST<String> st = new DivisionHashST<>(tableSize);
            int[] slots = new int[tableSize];
            Arrays.fill(slots, -1);
            long successSum = 0;

            for (int i = 0; i < keyCount; i++) {
                int key = random.nextInt(1000) - 200; // 含负数
                st.put(key, "v" + key);
                int index = Math.floorMod(key, tableSize);
                int probes = 0;
                while (true) { // 独立编写的线性探测模拟
                    probes++;
                    if (slots[index] == -1) {
                        slots[index] = key;
                        break;
                    }
                    index = (index + 1) % tableSize;
                }
                successSum += probes;
            }

            long failureSum = 0;
            for (int start = 0; start < tableSize; start++) {
                int probes = 1;
                int index = start;
                while (slots[index] != -1) {
                    probes++;
                    index = (index + 1) % tableSize;
                }
                failureSum += probes;
            }

            assertEquals(successSum, st.successfulProbeSum(), "第 " + trial + " 张表 ASL 成功分子");
            assertEquals(failureSum, st.unsuccessfulProbeSum(), "第 " + trial + " 张表 ASL 失败分子");
            assertEquals((double) successSum / keyCount, st.averageSuccessfulProbes(), 1e-12);
            assertEquals((double) failureSum / tableSize, st.averageUnsuccessfulProbes(), 1e-12);
        }
    }
}
