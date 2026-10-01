package cn.exercise.algs4.datastructure.hash.independent;

import cn.exercise.algs4.datastructure.hash.DirectAddressHashST;
import cn.exercise.algs4.datastructure.hash.DivisionHashST;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 【看实现之后补充的测试】—— 依据任务书第 0.3 条,须与盲测区分标注。
 * 覆盖盲测未涉及、但契约/规格允许或要求判定的边界与实现细节:
 *   - m=2 极小表、表满行为、墓碑复用插入、大量冲突的地址序列;
 *   - DirectAddress 负系数 a、capacity 上限边界与整型区间溢出防护、keys() 反推。
 */
class SupplementaryAfterReadingTest {

    // ================= DivisionHashST =================

    // 极小表 m=2 (p=2)
    @Test
    void tinyTable_m2() {
        DivisionHashST<String> st = new DivisionHashST<String>(2);
        assertEquals(2, st.tableSize());
        assertEquals(2, st.modulus());
        st.put(0, "a");   // hash 0
        st.put(1, "b");   // hash 1
        assertEquals("a", st.get(0));
        assertEquals("b", st.get(1));
        assertEquals(2, st.size());
        // 独立手算:m=2 且两槽均占用(表满)。每个起始下标探测至 probes==tableSize 兜底停止,
        // start0 计 2、start1 计 2 -> sum=4, average=4/2=2.0。
        double au = st.averageUnsuccessfulProbes();
        assertEquals(2.0, au, 1e-9);
    }

    // 表满:插入第 (m+1) 个不同关键字应抛异常(实现选择 IllegalStateException),且不丢失已有数据
    @Test
    void tableFull_throwsAndKeepsData() {
        DivisionHashST<String> st = new DivisionHashST<String>(3); // p=3
        st.put(0, "a");
        st.put(1, "b");
        st.put(2, "c");
        assertEquals(3, st.size());
        // 第 4 个不同关键字(无空槽、无墓碑) -> 异常
        assertThrows(RuntimeException.class, () -> st.put(3, "d"));
        // 异常后原有数据完好
        assertEquals(3, st.size());
        assertEquals("a", st.get(0));
        assertEquals("c", st.get(2));
    }

    // 墓碑复用:删除链中间元素后再插入同哈希的新键,应复用墓碑而非追加
    @Test
    void tombstoneReuseOnInsert() {
        DivisionHashST<String> st = new DivisionHashST<String>(13); // p=13? largestPrime<=13 =13
        st.put(1, "a");  // hash1 -> slot1
        st.put(14, "b"); // 14%13=1 -> slot1占用 -> slot2
        st.put(27, "c"); // 27%13=1 -> slot3
        assertEquals(1, st.slotOf(1));
        assertEquals(2, st.slotOf(14));
        assertEquals(3, st.slotOf(27));
        st.delete(14);           // slot2 变墓碑
        assertEquals(1, st.tombstones());
        assertEquals(2, st.size());
        st.put(40, "d");         // 40%13=1 -> 应复用 slot2 墓碑
        assertEquals(2, st.slotOf(40), "new key should reuse the tombstone slot");
        assertEquals(0, st.tombstones());
        // 链仍未断
        assertEquals("a", st.get(1));
        assertEquals("c", st.get(27));
    }

    // 大量冲突:一串同哈希关键字应连续线性探测,地址 = (h + t) mod m
    @Test
    void heavyCollisions_followLinearProbeSequence() {
        int m = 16, p = 13;
        DivisionHashST<String> st = new DivisionHashST<String>(m, p);
        int h = 1; // 选 h=1: keys 1,14,27,40,... (%13==1)
        List<Integer> expectedSlots = new ArrayList<Integer>();
        int idx = h;
        for (int t = 0; t < 6; t++) {
            expectedSlots.add(idx % m);
            idx++;
        }
        for (int t = 0; t < 6; t++) {
            int key = h + t * p; // 均 ≡1 (mod 13)
            assertEquals(1, st.hash(key));
            st.put(key, "k" + t);
            assertEquals(expectedSlots.get(t), st.slotOf(key),
                    "slot for colliding key " + key);
        }
        // 第 t 个元素插入探测次数 = t+1
        for (int t = 0; t < 6; t++) {
            st.get(h + t * p);
            assertEquals(t + 1, st.lastProbes(), "probe count grows linearly under clustering");
        }
    }

    // unsuccessfulProbeSum 在全表填满时的兜底(不越界、不溢出)
    @Test
    void fullTable_unsuccessfulSumBounded() {
        DivisionHashST<String> st = new DivisionHashST<String>(5); // p=5
        for (int k = 0; k < 5; k++) st.put(k, "v" + k); // 填满
        assertEquals(5, st.size());
        // 表满,失败查找最多 m 次比较
        long sum = st.unsuccessfulProbeSum();
        assertTrue(sum <= 5L * 5L, "unsuccessful sum should stay bounded by m*m, got " + sum);
        assertTrue(st.averageUnsuccessfulProbes() <= 5.0 + 1e-9);
    }

    // ================= DirectAddressHashST =================

    // 负系数 a (a<0) 仍应零冲突且地址落在 [0,capacity)
    @Test
    void negativeCoefficient_zeroCollisionInRange() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(0, 9, -2, 100);
        java.util.Set<Integer> seen = new java.util.HashSet<Integer>();
        for (int k = 0; k <= 9; k++) {
            int h = st.hash(k);
            assertTrue(h >= 0 && h < st.capacity(), "addr out of range key " + k + " h " + h);
            assertTrue(seen.add(h), "collision for a<0 at key " + k);
            st.put(k, "v" + k);
            assertEquals("v" + k, st.get(k));
        }
        // keys() 反推关键字正确(升地址序 => 因 a<0 关键字降序)
        List<Integer> ks = new ArrayList<Integer>();
        for (Integer k : st.keys()) ks.add(k);
        assertEquals(10, ks.size());
        for (int i = 1; i < ks.size(); i++) {
            assertTrue(st.hash(ks.get(i - 1)) < st.hash(ks.get(i)),
                    "keys() not in address-ascending order");
        }
    }

    // capacity 上限边界:恰为 2^26 允许,超过抛 IAE
    @Test
    void capacityBoundary_atLimit() {
        int MAX = 1 << 26; // 67108864
        DirectAddressHashST<String> ok = new DirectAddressHashST<String>(0, MAX - 1); // size=MAX
        assertEquals(MAX, ok.capacity());
        assertThrows(IllegalArgumentException.class,
                () -> new DirectAddressHashST<String>(0, MAX)); // size=MAX+1
    }

    // 整型全域区间不应因 int 溢出而崩溃,应作为"地址空间过大"抛 IAE
    @Test
    void fullIntRange_rejectedNotOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> new DirectAddressHashST<String>(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    // 单值区间 minKey==maxKey 且 a!=1: capacity=1
    @Test
    void singleValueRange_generalCoefficient() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(5, 5, 3, 7);
        assertEquals(1, st.capacity());
        st.put(5, "only");
        assertEquals("only", st.get(5));
        assertEquals(1, st.size());
    }
}
