package cn.exercise.algs4.datastructure.hash.independent;

import cn.exercise.algs4.datastructure.hash.DirectAddressHashST;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 独立盲测:DirectAddressHashST。
 * 本文件的断言仅依据任务书第 1、2 节的接口契约与规格编写,写作时尚未阅读实现。
 */
class DirectAddressBlindTest {

    // ---- 3.1 hash 正确性:2 参构造 H(key)=key-minKey,边界与内部 ----
    @Test
    void hash_matchesLinearIndex_forTwoArgConstructor() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(5, 15);
        assertEquals(0, st.hash(5));        // minKey -> 0
        assertEquals(10, st.hash(15));      // maxKey -> capacity-1
        assertEquals(6, st.hash(11));
        // 所有合法关键字地址落在 [0, capacity)
        for (int k = 5; k <= 15; k++) {
            int h = st.hash(k);
            assertTrue(h >= 0 && h < st.capacity(), "hash out of range for key " + k);
        }
    }

    // ---- 契约:capacity() = H(maxKey) - H(minKey) + 1;2 参构造 = maxKey-minKey+1 ----
    @Test
    void capacity_matchesRangeLength() {
        assertEquals(11, new DirectAddressHashST<String>(5, 15).capacity());
        assertEquals(1, new DirectAddressHashST<String>(7, 7).capacity());   // 单值区间
        assertEquals(256, new DirectAddressHashST<String>(0, 255).capacity());
    }

    // ---- 单值区间:minKey==maxKey ----
    @Test
    void singleValueRange_works() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(42, 42);
        assertEquals(1, st.capacity());
        st.put(42, "x");
        assertEquals(1, st.size());
        assertEquals("x", st.get(42));
        assertTrue(st.contains(42));
    }

    // ---- 3.4 零冲突性质:a!=0 时不同关键字地址互不相同 ----
    @Test
    void distinctKeys_mapToDistinctAddresses_withGeneralConstructor() {
        // H(key)=a*key+b, a>0;区间内所有整型关键字
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(10, 20, 3, 100);
        java.util.Set<Integer> addrs = new java.util.HashSet<Integer>();
        for (int k = 10; k <= 20; k++) {
            int h = st.hash(k);
            assertTrue(h >= 0 && h < st.capacity(), "address out of range for key " + k + " = " + h);
            assertTrue(addrs.add(h), "collision detected, violates zero-collision property at key " + k);
        }
        assertEquals(11, addrs.size());
    }

    @Test
    void coefficientAccessors_returnConstructorArgs() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(0, 100, 7, 5);
        assertEquals(7L, st.coefficientA());
        assertEquals(5L, st.constantB());
    }

    // ---- 3.3 基本操作组合行为 ----
    @Test
    void putGetDeleteContainsSizeIsEmpty() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(0, 9);
        assertTrue(st.isEmpty());
        assertEquals(0, st.size());
        assertNull(st.get(3));
        assertFalse(st.contains(3));

        st.put(3, "three");
        st.put(7, "seven");
        assertEquals(2, st.size());
        assertFalse(st.isEmpty());
        assertEquals("three", st.get(3));
        assertEquals("seven", st.get(7));

        // 覆盖
        st.put(3, "TWO");
        assertEquals("TWO", st.get(3));
        assertEquals(2, st.size());

        // 删除返回被删值
        assertEquals("seven", st.delete(7));
        assertNull(st.get(7));
        assertFalse(st.contains(7));
        assertEquals(1, st.size());

        // 删除不存在 -> null, 大小不变
        assertNull(st.delete(1));
        assertEquals(1, st.size());
    }

    @Test
    void loadFactor_isSizeOverCapacity() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(0, 9); // capacity 10
        assertEquals(0.0, st.loadFactor(), 1e-9);
        st.put(0, "a");
        st.put(1, "b");
        st.put(2, "c");
        st.put(3, "d");
        st.put(4, "e");
        assertEquals(0.5, st.loadFactor(), 1e-9); // 5/10
    }

    // ---- keys() 按地址升序 ----
    @Test
    void keys_iterateInAddressAscendingOrder() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(5, 15);
        st.put(11, "x"); // addr 6
        st.put(5, "y");  // addr 0
        st.put(15, "z"); // addr 10
        List<Integer> ks = new ArrayList<Integer>();
        for (Integer k : st.keys()) ks.add(k);
        // 地址升序 -> 因 H=k-minKey 单调,等价于 key 升序
        assertEquals(java.util.Arrays.asList(5, 11, 15), ks);
    }

    // ---- 契约:toString() 形如 {k v, k v} ----
    @Test
    void toString_followsContractFormat() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(1, 10);
        st.put(3, "x");
        st.put(5, "y");
        // 地址升序:3(2) 在 5(4) 前
        assertEquals("{3 x, 5 y}", st.toString());
    }

    // ---- 3.2 参数校验:异常类型 ----
    @Test
    void outOfRangeKey_throwsIllegalArgumentException() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(5, 15);
        assertThrows(IllegalArgumentException.class, () -> st.put(4, "x"));   // < minKey
        assertThrows(IllegalArgumentException.class, () -> st.put(16, "x"));  // > maxKey
        assertThrows(IllegalArgumentException.class, () -> st.get(100));
        assertThrows(IllegalArgumentException.class, () -> st.contains(-1));
        assertThrows(IllegalArgumentException.class, () -> st.delete(200));
        assertThrows(IllegalArgumentException.class, () -> st.hash(4));
    }

    @Test
    void nullValue_throwsNullPointerException() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(0, 9);
        assertThrows(NullPointerException.class, () -> st.put(3, null));
    }

    @Test
    void zeroCoefficient_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DirectAddressHashST<String>(0, 10, 0, 5));
    }

    @Test
    void minKeyGreaterThanMaxKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new DirectAddressHashST<String>(15, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new DirectAddressHashST<String>(15, 5, 2, 1));
    }

    // 地址空间过大:a*(maxKey-minKey)+1 极大 -> 应抛 IAE
    @Test
    void excessiveAddressSpace_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DirectAddressHashST<String>(0, 10_000_000, 1000, 0));
    }

    // 通用构造 H=a*key+b:零冲突且 capacity = a*(maxKey-minKey)+1 (a>0)
    @Test
    void generalConstructor_capacityFormula() {
        DirectAddressHashST<String> st = new DirectAddressHashST<String>(2, 8, 4, 1);
        // a*(max-min)+1 = 4*6+1 = 25
        assertEquals(25, st.capacity());
    }
}
