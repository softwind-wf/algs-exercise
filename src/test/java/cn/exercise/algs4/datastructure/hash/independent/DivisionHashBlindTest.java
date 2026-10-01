package cn.exercise.algs4.datastructure.hash.independent;

import cn.exercise.algs4.datastructure.hash.DivisionHashST;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 独立盲测:DivisionHashST。
 * 断言仅依据任务书第 1、2 节契约/规格与独立手算的经典例题数据编写,写作时尚未阅读实现。
 *
 * 经典例题(m=16, p=13, 序列 19,14,23,1,68,20,84,27,55,11,10,79)的期望值均来自独立手算:
 *   地址   = 6,1,10,2,3,7,8,4,5,11,12,9
 *   插入探测 = 1,1,1,2,1,1,3,4,3,1,3,9 (和=30 -> ASL成功=30/12=2.5)
 *   ASL失败 = 94/16 = 5.875 (16 个起始下标各数到首个空单元,含该次比较)
 */
class DivisionHashBlindTest {

    private static final int[] KEYS = {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79};
    private static final int[] FINAL_ADDR = {6, 1, 10, 2, 3, 7, 8, 4, 5, 11, 12, 9};
    private static final int[] INSERT_PROBES = {1, 1, 1, 2, 1, 1, 3, 4, 3, 1, 3, 9};

    // ---- 3.5 模数自动取不大于 m 的最大质数 ----
    @Test
    void modulus_autoSelectsLargestPrimeLE_m() {
        assertEquals(13, new DivisionHashST<String>(16).modulus()); // 2 的幂
        assertEquals(13, new DivisionHashST<String>(13).modulus()); // m 本身是质数
        assertEquals(7, new DivisionHashST<String>(8).modulus());
        assertEquals(3, new DivisionHashST<String>(4).modulus());
        assertEquals(2, new DivisionHashST<String>(2).modulus());   // 极小表
        assertEquals(19, new DivisionHashST<String>(20).modulus());
    }

    // ---- static largestPrimeNotGreaterThan 边界与异常 ----
    @Test
    void largestPrimeNotGreaterThan_values() {
        assertEquals(2, DivisionHashST.largestPrimeNotGreaterThan(2));
        assertEquals(3, DivisionHashST.largestPrimeNotGreaterThan(3));
        assertEquals(3, DivisionHashST.largestPrimeNotGreaterThan(4));
        assertEquals(13, DivisionHashST.largestPrimeNotGreaterThan(16));
        assertEquals(13, DivisionHashST.largestPrimeNotGreaterThan(13));
        assertEquals(1021, DivisionHashST.largestPrimeNotGreaterThan(1024));
        assertEquals(97, DivisionHashST.largestPrimeNotGreaterThan(100));
    }

    @Test
    void largestPrimeNotGreaterThan_belowTwo_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> DivisionHashST.largestPrimeNotGreaterThan(1));
        assertThrows(IllegalArgumentException.class, () -> DivisionHashST.largestPrimeNotGreaterThan(0));
        assertThrows(IllegalArgumentException.class, () -> DivisionHashST.largestPrimeNotGreaterThan(-5));
    }

    // ---- 构造参数校验:显式模数需 1<=p<=m ----
    @Test
    void explicitModulus_outOfRange_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(10, 0));  // p<1
        assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(10, 11)); // p>m
    }

    @Test
    void tableSize_accessor() {
        DivisionHashST<String> st = new DivisionHashST<String>(16, 13);
        assertEquals(16, st.tableSize());
        assertEquals(13, st.modulus());
    }

    // ---- 3.1 hash: floorMod 语义,负数也得非负地址,范围 [0,p-1] ----
    @Test
    void hash_isFloorMod_nonNegative() {
        DivisionHashST<String> st = new DivisionHashST<String>(16, 13);
        assertEquals(6, st.hash(19));    // 19 % 13 = 6
        assertEquals(0, st.hash(13));    // 13 % 13 = 0
        assertEquals(12, st.hash(-1));   // floorMod(-1,13)=12 (必须非负)
        assertEquals(11, st.hash(-2));   // floorMod(-2,13)=11
        assertEquals(0, st.hash(-13));   // floorMod(-13,13)=0
        for (int k = -40; k <= 40; k++) {
            int h = st.hash(k);
            assertTrue(h >= 0 && h < st.modulus(), "hash(" + k + ")=" + h + " not in [0,p)");
        }
    }

    // ---- 基本操作 ----
    @Test
    void basicOps_putGetDeleteContainsSize() {
        DivisionHashST<String> st = new DivisionHashST<String>(16, 13);
        assertTrue(st.isEmpty());
        assertNull(st.get(999));
        assertFalse(st.contains(999));

        st.put(19, "a");
        st.put(14, "b");
        st.put(23, "c");
        assertEquals(3, st.size());
        assertEquals("a", st.get(19));
        // 覆盖
        st.put(19, "A");
        assertEquals("A", st.get(19));
        assertEquals(3, st.size());
        // 删除返回值
        assertEquals("A", st.delete(19));
        assertNull(st.get(19));
        assertEquals(-1, st.slotOf(19));
        // 删除不存在
        assertNull(st.delete(1234));
    }

    @Test
    void slotOf_reflectsLinearProbePlacement() {
        DivisionHashST<String> st = new DivisionHashST<String>(16, 13);
        st.put(19, "x");            // addr 6, no collision
        assertEquals(6, st.slotOf(19));
        st.put(14, "y");            // addr 1
        assertEquals(1, st.slotOf(14));
        st.put(32, "z");            // 32 % 13 = 6 -> collides with 19 -> slot 7
        assertEquals(6, st.hash(32));
        assertEquals(7, st.slotOf(32));
    }

    // ---- 墓碑删除:探测链不断裂 ----
    @Test
    void deleteLeavesTombstone_probeChainNotBroken() {
        DivisionHashST<String> st = new DivisionHashST<String>(16, 13);
        // 制造一条链:19->6, 32->(6)7, 45->(6)8
        st.put(19, "a"); // slot6
        st.put(32, "b"); // slot7
        st.put(45, "c"); // slot8 (45%13=6)
        assertEquals(6, st.slotOf(19));
        assertEquals(7, st.slotOf(32));
        assertEquals(8, st.slotOf(45));
        // 删除链中间的 32
        st.delete(32);
        assertEquals(1, st.tombstones());
        // 删除中间元素后,后续 45 仍应能找到(链未断)
        assertTrue(st.contains(45), "probe chain broken after deleting middle element");
        assertEquals("c", st.get(45));
        assertEquals(8, st.slotOf(45));
        // 19 仍在
        assertEquals("a", st.get(19));
    }

    // ---- loadFactor = size/tableSize ----
    @Test
    void loadFactor_usesTableSize() {
        DivisionHashST<String> st = new DivisionHashST<String>(16, 13);
        for (int i = 0; i < 8; i++) st.put(100 + i, "v" + i); // 8 distinct low-collision-ish keys
        assertEquals(8, st.size());
        assertEquals(8.0 / 16.0, st.loadFactor(), 1e-9); // 以 tableSize=16 为分母
    }

    // ---- 3.6 经典例题:地址 / 探测次数 / ASL ----
    @Test
    void classicExample_addressesInsertProbesAndASL() {
        DivisionHashST<String> st = new DivisionHashST<String>(16);
        assertEquals(16, st.tableSize());
        assertEquals(13, st.modulus());

        for (int i = 0; i < KEYS.length; i++) {
            st.put(KEYS[i], "v" + i);
            assertEquals(INSERT_PROBES[i], st.lastProbes(),
                    "insert probes mismatch for key " + KEYS[i] + " (#" + (i + 1) + ")");
        }

        assertEquals(12, st.size());
        for (int i = 0; i < KEYS.length; i++) {
            assertEquals(FINAL_ADDR[i], st.slotOf(KEYS[i]),
                    "final address mismatch for key " + KEYS[i]);
        }

        // ASL 成功 = 30/12 = 2.5
        assertEquals(30L, st.successfulProbeSum(), "successful probe sum");
        assertEquals(2.5, st.averageSuccessfulProbes(), 1e-9, "ASL success");

        // ASL 失败 = 94/16 = 5.875
        assertEquals(94L, st.unsuccessfulProbeSum(), "unsuccessful probe sum");
        assertEquals(5.875, st.averageUnsuccessfulProbes(), 1e-9, "ASL unsuccessful");
    }

    // ---- keys() 按数组下标升序 ----
    @Test
    void keys_iterateByArrayIndexAscending() {
        DivisionHashST<String> st = new DivisionHashST<String>(16);
        for (int i = 0; i < KEYS.length; i++) st.put(KEYS[i], "v" + i);
        List<Integer> ks = new ArrayList<Integer>();
        for (Integer k : st.keys()) ks.add(k);
        assertEquals(12, ks.size());
        // 按 slot 升序:slot->key 映射来自 FINAL_ADDR
        // slot:1->14,2->1,3->68,4->27,5->55,6->19,7->20,8->84,9->79,10->23,11->11,12->10
        assertEquals(Arrays.asList(14, 1, 68, 27, 55, 19, 20, 84, 79, 23, 11, 10), ks);
    }

    // ---- lastProbes 语义:查找/删除也更新 ----
    @Test
    void lastProbes_updatesOnGet() {
        DivisionHashST<String> st = new DivisionHashST<String>(16);
        st.put(19, "a"); // probe 1 -> slot6
        st.put(32, "b"); // probe 2 -> slot7 (6 occupied)
        st.get(32);      // probing: slot6(19) mismatch, slot7(32) match -> 2 probes
        assertEquals(2, st.lastProbes(), "get probe count for key landing after 1 collision");
    }
}

