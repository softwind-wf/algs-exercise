package cn.exercise.algs4.datastructure.hash.independent;

import cn.exercise.algs4.datastructure.hash.DirectAddressHashST;
import cn.exercise.algs4.datastructure.hash.DivisionHashST;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 独立差分测试(盲测,仅依据契约):≥1 万次随机操作,同时施加于被测类与 java.util.HashMap,
 * 逐操作比对 size / get / contains。固定种子保证可复现。
 */
class DifferentialBlindTest {

    private static final String[] OPS = {"put", "get", "contains", "delete"};

    // ---- 直接定址法:关键字限定在 [0, RANGE-1] 区间内 ----
    @Test
    void directAddress_matchesHashMap_over20000Ops() {
        final int RANGE = 500;
        final int MIN = -200;
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(MIN, MIN + RANGE - 1);
        HashMap<Integer, String> ref = new HashMap<Integer, String>();
        Random rnd = new Random(20261001L); // 固定种子

        int ops = 20000;
        int mismatch = 0;
        String firstMismatch = null;
        for (int i = 0; i < ops; i++) {
            int key = MIN + rnd.nextInt(RANGE);
            String op = OPS[rnd.nextInt(OPS.length)];
            if ("put".equals(op)) {
                String v = "v" + i;
                st.put(key, v);
                ref.put(key, v);
            } else if ("get".equals(op)) {
                if (!eq(st.get(key), ref.get(key))) {
                    mismatch++; if (firstMismatch == null) firstMismatch = "get key=" + key;
                }
            } else if ("contains".equals(op)) {
                if (st.contains(key) != ref.containsKey(key)) {
                    mismatch++; if (firstMismatch == null) firstMismatch = "contains key=" + key;
                }
            } else { // delete
                String a = st.delete(key);
                String b = ref.remove(key);
                if (!eq(a, b)) {
                    mismatch++; if (firstMismatch == null) firstMismatch = "delete key=" + key;
                }
            }
            if (st.size() != ref.size()) {
                mismatch++; if (firstMismatch == null) firstMismatch = "size diverged at op " + i;
            }
        }
        assertEquals(0, mismatch, "direct-address differential mismatches, first: " + firstMismatch);
    }

    // ---- 除留余数法:保持低装填,避免表满影响比对 ----
    @Test
    void divisionHash_matchesHashMap_over20000Ops() {
        final int POOL = 200; // 有限关键字池 -> 元素数上限 200 << tableSize
        final int TABLE = 1000; // p = largestPrimeNotGreaterThan(1000) = 997
        DivisionHashST<String> st = new DivisionHashST<String>(TABLE);
        HashMap<Integer, String> ref = new HashMap<Integer, String>();
        Random rnd = new Random(987654321L); // 固定种子

        int ops = 20000;
        int mismatch = 0;
        String firstMismatch = null;
        for (int i = 0; i < ops; i++) {
            // 混合正负关键字以覆盖 floorMod 语义
            int key = rnd.nextInt(POOL) - (rnd.nextInt(10) == 0 ? POOL : 0);
            String op = OPS[rnd.nextInt(OPS.length)];
            if ("put".equals(op)) {
                String v = "v" + i;
                st.put(key, v);
                ref.put(key, v);
            } else if ("get".equals(op)) {
                if (!eq(st.get(key), ref.get(key))) {
                    mismatch++; if (firstMismatch == null) firstMismatch = "get key=" + key;
                }
            } else if ("contains".equals(op)) {
                if (st.contains(key) != ref.containsKey(key)) {
                    mismatch++; if (firstMismatch == null) firstMismatch = "contains key=" + key;
                }
            } else { // delete
                String a = st.delete(key);
                String b = ref.remove(key);
                if (!eq(a, b)) {
                    mismatch++; if (firstMismatch == null) firstMismatch = "delete key=" + key;
                }
            }
            if (st.size() != ref.size()) {
                mismatch++; if (firstMismatch == null) firstMismatch = "size diverged at op " + i;
            }
        }
        assertEquals(0, mismatch, "division-hash differential mismatches, first: " + firstMismatch);
        assertTrue(st.loadFactor() <= 1.0, "load factor should stay bounded: " + st.loadFactor());
    }

    private static boolean eq(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}
