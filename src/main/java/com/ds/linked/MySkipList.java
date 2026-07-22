package com.ds.linked;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 跳跃表（Skip List）—— 基于多层索引链表的概率型数据结构（带 LRU 缓存）
 *
 * 核心思想：
 * 在有序链表的基础上增加多层"快速通道"索引。
 * 底层（第 0 层）包含所有元素，每向上一层节点数约减半。
 * 查找时从顶层开始，逐层下降，每次跳过大量节点，实现 O(log n) 的平均时间复杂度。
 *
 * LRU 缓存层：
 * 在跳表之上增加一个基于 HashMap + 双向链表的 LRU 缓存。
 * 热点 key 的重复查询可直接 O(1) 命中缓存，跳过 O(log n) 的跳表遍历。
 * 缓存与跳表保持一致性：put/remove 操作会自动失效对应缓存条目。
 *
 * 特点：
 *   - 平均时间复杂度：查找(有缓存命中时 O(1)) / 插入/删除均为 O(log n)
 *   - 最坏时间复杂度：O(n)（概率极低）
 *   - 空间复杂度：O(n + cacheSize)
 *   - 缓存一致性：写操作自动失效缓存，保证数据正确
 *
 * @param <K> 键类型（必须可比较）
 * @param <V> 值类型
 */
public class MySkipList<K extends Comparable<K>, V> {

    // ==================== 节点内部类 ====================

    /**
     * 跳跃表节点
     * 每个节点持有 key、value 以及一个 forward 指针数组。
     * forward[i] 指向第 i 层中当前节点的下一个节点。
     */
    private static class Node<K extends Comparable<K>, V> {
        K key;
        V value;
        Node<K, V>[] forward;  // forward[i] = 第 i 层的后继节点

        @SuppressWarnings("unchecked")
        Node(K key, V value, int level) {
            this.key = key;
            this.value = value;
            this.forward = new Node[level + 1];  // level 从 0 开始
        }

        public K getKey() {
            return key;
        }



        public V getValue() {
            return value;
        }

    }

    // ==================== LRU 缓存节点 ====================

    /**
     * LRU 缓存双向链表节点
     * head ↔ ... ↔ tail: head 端 = 最近使用，tail 端 = 最久未使用
     */
    private static class CacheNode<K, V> {
        K key;
        V value;
        CacheNode<K, V> prev;
        CacheNode<K, V> next;

        CacheNode(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    // ==================== 常量与成员变量 ====================

    private static final int MAX_LEVEL = 16;        // 最大层数（支持约 2^16 = 65536 个元素）
    private static final double P = 0.5;             // 升级概率
    private static final int DEFAULT_CACHE_SIZE = 64; // 默认缓存容量

    // ==== 跳表核心 ====
    private final Node<K, V> head;       // 头节点（哨兵，持有 MAX_LEVEL 层指针）
    private int level;                   // 当前实际最大层数
    private int size;                    // 元素个数
    private final Random random;         // 随机数生成器

    // ==== LRU 缓存 ====
    private final int cacheCapacity;                         // 缓存容量上限
    private final Map<K, CacheNode<K, V>> cacheMap;          // key → 缓存节点（O(1) 查找）
    private CacheNode<K, V> cacheHead;                       // 哨兵头（最近使用端）
    private CacheNode<K, V> cacheTail;                       // 哨兵尾（最久未使用端）
    private long cacheHits;                                  // 缓存命中次数
    private long cacheMisses;                                // 缓存未命中次数

    // ==================== 构造方法 ====================

    public MySkipList() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * @param cacheCapacity LRU 缓存容量，0 表示禁用缓存
     */
    public MySkipList(int cacheCapacity) {
        this.head = new Node<>(null, null, MAX_LEVEL);
        this.level = 0;
        this.size = 0;
        this.random = new Random();
        this.cacheCapacity = cacheCapacity;
        this.cacheMap = cacheCapacity > 0 ? new HashMap<>() : null;
        if (cacheCapacity > 0) {
            // 哨兵节点简化边界处理
            this.cacheHead = new CacheNode<>(null, null);
            this.cacheTail = new CacheNode<>(null, null);
            cacheHead.next = cacheTail;
            cacheTail.prev = cacheHead;
        }
        this.cacheHits = 0;
        this.cacheMisses = 0;
    }

    // ==================== 基础查询 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 获取当前跳跃表的实际层数
     */
    public int level() {
        return level;
    }

    // ==================== 查找操作 ====================

    /**
     * 根据 key 查找对应的 value（优先查询 LRU 缓存）
     *
     * @param key 要查找的键
     * @return 对应的值，不存在返回 null
     */
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }

        // ==== 1. 查缓存 ====
        V cached = cacheGet(key);
        if (cached != null) {
            return cached;  // 缓存命中，O(1)
        }
        // 缓存未命中计数在 cacheGet 内部累加

        // ==== 2. 查跳表 ====
        Node<K, V> node = findNode(key);
        V value = node != null ? node.value : null;

        // ==== 3. 写入缓存 ====
        if (node != null) {
            cachePut(key, value);
        }

        return value;
    }

    /**
     * 判断是否包含指定 key
     */
    public boolean containsKey(K key) {
        if (key == null) {
            return false;
        }
        return get(key) != null;  // 通过 get() 复用缓存
    }

    /**
     * 查找节点（内部方法）
     * 从顶层开始，逐层向右下方向搜索
     *
     * @param key 目标键
     * @return 找到的节点，不存在返回 null
     */
    private Node<K, V> findNode(K key) {
        Node<K, V> current = head;

        // 从最高层开始，逐层向下查找
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].key.compareTo(key) < 0) {
                current = current.forward[i];  // 在当前层向右移动
            }
        }

        // 到达第 0 层，检查下一个节点是否为目标
        current = current.forward[0];
        if (current != null && current.key.compareTo(key) == 0) {
            return current;
        }
        return null;
    }

    // ==================== 插入操作 ====================

    /**
     * 插入键值对。若 key 已存在则更新 value。
     *
     * @param key   键
     * @param value 值
     */
    public void put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }

        // update[i] 记录在第 i 层，新节点的前驱节点
        @SuppressWarnings("unchecked")
        Node<K, V>[] update = new Node[MAX_LEVEL + 1];

        Node<K, V> current = head;

        // 从最高层开始，逐层查找插入位置的前驱
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].key.compareTo(key) < 0) {
                current = current.forward[i];
            }
            update[i] = current;  // 记录第 i 层的前驱
        }

        // 到达第 0 层
        current = current.forward[0];

        if (current != null && current.key.compareTo(key) == 0) {
            // key 已存在，更新 value，同时更新缓存
            current.value = value;
            cachePut(key, value);  // 直接更新缓存，避免脏读
            return;
        }

        // key 不存在，创建新节点
        int newLevel = randomLevel();

        // 如果新节点的层数超过当前最大层，需要更新更高层的 update 指向 head
        if (newLevel > level) {
            for (int i = level + 1; i <= newLevel; i++) {
                update[i] = head;
            }
            level = newLevel;
        }

        // 创建新节点
        Node<K, V> newNode = new Node<>(key, value, newLevel);

        // 在每一层将新节点插入到前驱之后
        for (int i = 0; i <= newLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }

        size++;
        cachePut(key, value);  // 新 key 也放入缓存
    }

    // ==================== 删除操作 ====================

    /**
     * 删除指定 key 的节点
     *
     * @param key 要删除的键
     * @return 被删除的值，若 key 不存在返回 null
     */
    public V remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }

        @SuppressWarnings("unchecked")
        Node<K, V>[] update = new Node[MAX_LEVEL + 1];

        Node<K, V> current = head;

        // 从最高层开始，逐层查找前驱
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].key.compareTo(key) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }

        // 到达第 0 层
        current = current.forward[0];

        // key 不存在
        if (current == null || current.key.compareTo(key) != 0) {
            return null;
        }

        // 从每一层删除节点
        for (int i = 0; i <= level; i++) {
            if (update[i].forward[i] == current) {
                update[i].forward[i] = current.forward[i];
            } else {
                break;  // 当前层已不包含该节点，更高层也不会包含
            }
        }

        // 断开节点的 forward 引用，帮助 GC
        for (int i = 0; i < current.forward.length; i++) {
            current.forward[i] = null;
        }

        // 更新当前最大层数（如果高层变空）
        while (level > 0 && head.forward[level] == null) {
            level--;
        }

        size--;
        cacheRemove(key);  // 删除缓存中的条目
        return current.value;
    }

    // ==================== LRU 缓存操作 ====================

    /**
     * 从缓存中获取值，不存在返回 null。
     * 命中时将该节点移到链表头部（标记为最近使用）。
     */
    private V cacheGet(K key) {
        if (cacheMap == null) {
            cacheMisses++;
            return null;
        }

        CacheNode<K, V> node = cacheMap.get(key);
        if (node == null) {
            cacheMisses++;
            return null;
        }

        // 命中：移到头部
        cacheHits++;
        moveToHead(node);
        return node.value;
    }

    /**
     * 将键值对写入缓存。
     * 若 key 已存在则更新并移到头部；若缓存已满则淘汰 LRU 条目。
     */
    private void cachePut(K key, V value) {
        if (cacheMap == null || cacheCapacity <= 0) {
            return;
        }

        CacheNode<K, V> node = cacheMap.get(key);
        if (node != null) {
            // key 已存在：更新值并移到头部
            node.value = value;
            moveToHead(node);
        } else {
            // 新 key：创建节点
            CacheNode<K, V> newNode = new CacheNode<>(key, value);
            cacheMap.put(key, newNode);
            addToHead(newNode);

            // 超容淘汰 LRU
            if (cacheMap.size() > cacheCapacity) {
                CacheNode<K, V> lru = removeTail();
                cacheMap.remove(lru.key);
            }
        }
    }

    /**
     * 从缓存中删除指定 key
     */
    private void cacheRemove(K key) {
        if (cacheMap == null) {
            return;
        }
        CacheNode<K, V> node = cacheMap.remove(key);
        if (node != null) {
            removeNode(node);
        }
    }

    /**
     * 清空所有缓存
     */
    public void cacheClear() {
        if (cacheMap == null) {
            return;
        }
        cacheMap.clear();
        cacheHead.next = cacheTail;
        cacheTail.prev = cacheHead;
    }

    // ---- 双向链表操作 ----

    /** 将节点移到链表头部（最近使用端） */
    private void moveToHead(CacheNode<K, V> node) {
        removeNode(node);
        addToHead(node);
    }

    /** 在头部插入节点 */
    private void addToHead(CacheNode<K, V> node) {
        node.prev = cacheHead;
        node.next = cacheHead.next;
        cacheHead.next.prev = node;
        cacheHead.next = node;
    }

    /** 从链表中摘除节点 */
    private void removeNode(CacheNode<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    /** 移除并返回尾部节点（LRU 淘汰目标） */
    private CacheNode<K, V> removeTail() {
        CacheNode<K, V> lru = cacheTail.prev;
        removeNode(lru);
        return lru;
    }

    // ---- 缓存统计 ----

    /**
     * 获取缓存命中次数
     */
    public long cacheHits() {
        return cacheHits;
    }

    /**
     * 获取缓存未命中次数
     */
    public long cacheMisses() {
        return cacheMisses;
    }

    /**
     * 获取缓存命中率
     * @return 命中率（0.0 ~ 1.0），无任何查询时返回 0
     */
    public double cacheHitRate() {
        long total = cacheHits + cacheMisses;
        return total == 0 ? 0.0 : (double) cacheHits / total;
    }

    /**
     * 获取当前缓存中的条目数
     */
    public int cacheSize() {
        return cacheMap != null ? cacheMap.size() : 0;
    }

    /**
     * 获取缓存统计报告
     */
    public String cacheStats() {
        if (cacheMap == null) {
            return "缓存已禁用";
        }
        return String.format(
            "缓存: 容量=%d, 当前=%d, 命中=%d, 未命中=%d, 命中率=%.1f%%",
            cacheCapacity, cacheMap.size(), cacheHits, cacheMisses, cacheHitRate() * 100
        );
    }

    // ==================== 获取首尾元素 ====================

    /**
     * 获取最小的 key
     */
    public K firstKey() {
        if (isEmpty()) {
            return null;
        }
        return head.forward[0].key;
    }

    /**
     * 获取最小的键值对
     */
    public Node<K, V> firstEntry() {
        return head.forward[0];
    }

    /**
     * 获取最大的 key
     */
    public K lastKey() {
        if (isEmpty()) {
            return null;
        }
        Node<K, V> current = head;
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null) {
                current = current.forward[i];
            }
        }
        return current.key;
    }

    // ==================== 范围查询 ====================

    /**
     * 查找小于等于指定 key 的最大键值对（floor）
     */
    public K floorKey(K key) {
        if (key == null || isEmpty()) {
            return null;
        }

        Node<K, V> current = head;
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].key.compareTo(key) < 0) {
                current = current.forward[i];
            }
        }

        // 检查下一个节点是否恰好等于 key
        if (current.forward[0] != null && current.forward[0].key.compareTo(key) == 0) {
            return current.forward[0].key;
        }

        // 否则 current 就是小于 key 的最大节点
        return current != head ? current.key : null;
    }

    /**
     * 查找大于等于指定 key 的最小键值对（ceiling）
     */
    public K ceilingKey(K key) {
        if (key == null || isEmpty()) {
            return null;
        }

        Node<K, V> current = head;
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].key.compareTo(key) < 0) {
                current = current.forward[i];
            }
        }

        Node<K, V> next = current.forward[0];
        return next != null ? next.key : null;
    }

    // ==================== 遍历操作 ====================

    /**
     * 按 key 升序遍历并打印所有元素（第 0 层完整链表）
     */
    public void traversal() {
        if (isEmpty()) {
            System.out.println("跳跃表为空");
            return;
        }
        Node<K, V> current = head.forward[0];
        while (current != null) {
            System.out.print(current.key + "=" + current.value + " ");
            current = current.forward[0];
        }
        System.out.println();
    }

    /**
     * 分层遍历：显示每一层的索引结构
     */
    public void traversalByLevel() {
        System.out.println("========== 跳跃表分层结构 (共 " + (level + 1) + " 层) ==========");
        for (int i = level; i >= 0; i--) {
            System.out.print("第 " + i + " 层: HEAD -> ");
            Node<K, V> current = head.forward[i];
            int count = 0;
            while (current != null) {
                System.out.print(current.key + ":" + current.value + " -> ");
                current = current.forward[i];
                count++;
            }
            System.out.println("NULL (节点数: " + count + ")");
        }
        System.out.println("==============================================");
    }

    /**
     * 获取跳跃表的统计信息
     */
    public String statistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 跳跃表统计信息 ==========\n");
        sb.append("元素总数: ").append(size).append("\n");
        sb.append("当前最大层数: ").append(level).append("\n");
        sb.append("最大层数上限: ").append(MAX_LEVEL).append("\n");
        sb.append("升级概率: ").append(P).append("\n");

        // 统计各层节点数
        sb.append("各层节点数: \n");
        for (int i = level; i >= 0; i--) {
            int count = 0;
            Node<K, V> current = head.forward[i];
            while (current != null) {
                count++;
                current = current.forward[i];
            }
            sb.append("  第 ").append(i).append(" 层: ").append(count).append(" 个节点\n");
        }

        sb.append("理论空间复杂度: O(").append(size).append("), 期望指针数 ≈ ")
          .append(String.format("%.1f", size * (1 / (1 - P)))).append("\n");
        sb.append("===================================");
        return sb.toString();
    }

    // ==================== 随机层数生成 ====================

    /**
     * 随机生成新节点的层数
     *
     * 算法：模拟抛硬币（几何分布），每层有概率 P 升级到下一层。
     * 期望层数 = 1 / (1 - P)，当 P = 0.5 时期望层数为 2。
     *
     * @return 随机生成的层数（0 到 MAX_LEVEL）
     */
    private int randomLevel() {
        int lvl = 0;
        // 当随机数小于 P 且未达最大层时，层数 +1
        while (random.nextDouble() < P && lvl < MAX_LEVEL) {
            lvl++;
        }
        return lvl;
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 跳跃表测试 ==========\n");

        MySkipList<Integer, String> skiplist = new MySkipList<>();

        // 1. 测试插入
        System.out.println("--- 1. 插入元素 ---");
        int[] keys = {3, 6, 9, 2, 11, 1, 4, 15, 8, 7, 5, 14, 12, 13, 10};
        for (int k : keys) {
            skiplist.put(k, "V" + k);
        }
        System.out.println("插入 " + keys.length + " 个元素, size: " + skiplist.size());
        System.out.print("第0层遍历: ");
        skiplist.traversal();
        System.out.println();

        // 2. 分层结构
        System.out.println("--- 2. 分层结构 ---");
        skiplist.traversalByLevel();
        System.out.println();

        // 3. 查找测试
        System.out.println("--- 3. 查找测试 ---");
        System.out.println("get(3): " + skiplist.get(3));
        System.out.println("get(11): " + skiplist.get(11));
        System.out.println("get(8): " + skiplist.get(8));
        System.out.println("get(99): " + skiplist.get(99) + " (应为 null)");
        System.out.println("containsKey(7): " + skiplist.containsKey(7));
        System.out.println("containsKey(100): " + skiplist.containsKey(100));
        System.out.println("firstKey: " + skiplist.firstKey());
        System.out.println("lastKey: " + skiplist.lastKey());
        System.out.println();

        // 4. 更新测试
        System.out.println("--- 4. 更新测试 ---");
        System.out.println("更新前 get(5): " + skiplist.get(5));
        skiplist.put(5, "UPDATED");
        System.out.println("更新后 get(5): " + skiplist.get(5));
        System.out.println("size(应不变): " + skiplist.size());
        System.out.println();

        // 5. 删除测试
        System.out.println("--- 5. 删除测试 ---");
        System.out.println("remove(3): " + skiplist.remove(3));
        System.out.println("remove(8): " + skiplist.remove(8));
        System.out.println("remove(15): " + skiplist.remove(15));
        System.out.println("remove(99): " + skiplist.remove(99) + " (应为 null)");
        System.out.println("删除后 size: " + skiplist.size());
        System.out.print("删除后遍历: ");
        skiplist.traversal();
        System.out.println();

        // 6. floor 和 ceiling 测试
        System.out.println("--- 6. floor/ceiling 测试 ---");
        System.out.println("floorKey(7): " + skiplist.floorKey(7) + " (应为 7)");
        System.out.println("floorKey(8): " + skiplist.floorKey(8) + " (应为 7, 8已被删)");
        System.out.println("floorKey(0): " + skiplist.floorKey(0) + " (应为 null)");
        System.out.println("ceilingKey(7): " + skiplist.ceilingKey(7) + " (应为 7)");
        System.out.println("ceilingKey(8): " + skiplist.ceilingKey(8) + " (应为 9, 8已被删)");
        System.out.println("ceilingKey(99): " + skiplist.ceilingKey(99) + " (应为 null)");
        System.out.println();

        // 7. 删除后分层结构
        System.out.println("--- 7. 删除后分层结构 ---");
        skiplist.traversalByLevel();
        System.out.println();

        // 8. 统计信息
        System.out.println("--- 8. 统计信息 ---");
        System.out.println(skiplist.statistics());
        System.out.println();

        // 9. 大量数据性能测试
        System.out.println("--- 9. 大量数据插入测试 ---");
        MySkipList<Integer, String> big = new MySkipList<>();
        int N = 10000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            big.put(i, "val" + i);
        }
        long end = System.currentTimeMillis();
        System.out.println("插入 " + N + " 个有序元素耗时: " + (end - start) + "ms");
        System.out.println("size: " + big.size());
        System.out.println("level: " + big.level());
        System.out.println();

        // 10. 大量数据查找测试
        System.out.println("--- 10. 大量数据查找测试 ---");
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            int k = (i * 7919) % N;  // 伪随机
            String v = big.get(k);
        }
        end = System.currentTimeMillis();
        System.out.println("10000 次随机查找耗时: " + (end - start) + "ms");
        System.out.println();

        // 11. 边界测试
        System.out.println("--- 11. 边界测试 ---");
        MySkipList<Integer, String> empty = new MySkipList<>();
        System.out.println("空表 firstKey: " + empty.firstKey());
        System.out.println("空表 lastKey: " + empty.lastKey());
        System.out.println("空表 floorKey(1): " + empty.floorKey(1));
        System.out.println("空表 ceilingKey(1): " + empty.ceilingKey(1));
        System.out.println("空表 get(1): " + empty.get(1));
        System.out.println("空表 remove(1): " + empty.remove(1));
        empty.traversal();
        System.out.println();

        // 12. null key 异常测试
        System.out.println("--- 12. null key 异常测试 ---");
        try {
            skiplist.put(null, "xxx");
        } catch (IllegalArgumentException e) {
            System.out.println("预期异常: " + e.getMessage());
        }

        // 13. LRU 缓存测试
        System.out.println("\n========== LRU 缓存测试 ==========\n");

        // 13.1 基本缓存命中
        System.out.println("--- 13.1 缓存命中测试 ---");
        MySkipList<Integer, String> cacheList = new MySkipList<>(4);  // 缓存容量=4
        for (int i = 1; i <= 100; i++) {
            cacheList.put(i, "Val" + i);
        }
        System.out.println("put 100 个元素");

        // 第一次查询 → 未命中
        String v1 = cacheList.get(50);
        System.out.println("第1次 get(50): " + v1);
        System.out.println("  " + cacheList.cacheStats());

        // 重复查询 → 应命中
        for (int i = 0; i < 5; i++) {
            cacheList.get(50);
        }
        System.out.println("再 get(50) 5 次后: " + cacheList.cacheStats());

        // 查询不存在的 key → 计为 miss
        System.out.println("get(999): " + cacheList.get(999));
        System.out.println("  " + cacheList.cacheStats());
        System.out.println();

        // 13.2 缓存淘汰
        System.out.println("--- 13.2 LRU 淘汰测试 ---");
        MySkipList<Integer, String> evictList = new MySkipList<>(3);  // 容量=3
        evictList.put(1, "A");
        evictList.put(2, "B");
        evictList.put(3, "C");
        evictList.put(4, "D");

        evictList.get(1);  // hit: 1 现在在缓存头
        evictList.get(2);  // hit: 2 在头, 1 被挤到下面
        evictList.get(3);  // hit: 3 在头, 2 在下面
        evictList.get(4);  // 淘汰最久未用的（1）, 4 入缓存

        // 再次查询 1 → 应从跳表重新加载到缓存（淘汰 2）
        evictList.get(1);
        System.out.println("经过淘汰后: " + evictList.cacheStats());
        System.out.println("缓存大小: " + evictList.cacheSize());
        System.out.println();

        // 13.3 put 更新 → 缓存同步
        System.out.println("--- 13.3 put 更新 → 缓存同步 ---");
        MySkipList<String, Integer> syncList = new MySkipList<>(8);
        syncList.put("hot", 100);
        syncList.get("hot");  // 缓存加载
        System.out.println("get(hot) 后: " + syncList.cacheStats());

        syncList.put("hot", 200);  // 更新应同步到缓存
        Integer newVal = syncList.get("hot");  // 应缓存命中且值为 200
        System.out.println("put(hot,200) 后 get(hot): " + newVal);
        System.out.println("  " + syncList.cacheStats());
        System.out.println();

        // 13.4 remove → 缓存失效
        System.out.println("--- 13.4 remove → 缓存失效 ---");
        syncList.put("del", 888);
        syncList.get("del");  // 加载到缓存
        System.out.println("get(del) 后: " + syncList.cacheStats());

        syncList.remove("del");  // 删除
        System.out.println("remove(del) 后 get(del): " + syncList.get("del"));
        System.out.println("  " + syncList.cacheStats());
        System.out.println();

        // 13.5 缓存性能对比
        System.out.println("--- 13.5 缓存性能对比 ---");
        int totalKeys = 50000;
        int hotKeys = 100;  // 热点 key 数量

        // 无缓存版本
        MySkipList<Integer, String> noCache = new MySkipList<>(0);  // 禁用缓存
        for (int i = 1; i <= totalKeys; i++) {
            noCache.put(i, "V" + i);
        }
        long t1 = System.nanoTime();
        for (int round = 0; round < 10; round++) {
            for (int k = 1; k <= hotKeys; k++) {
                noCache.get(k);
            }
        }
        long noCacheTime = System.nanoTime() - t1;

        // 有缓存版本
        MySkipList<Integer, String> withCache = new MySkipList<>(hotKeys);
        for (int i = 1; i <= totalKeys; i++) {
            withCache.put(i, "V" + i);
        }
        long t2 = System.nanoTime();
        for (int round = 0; round < 10; round++) {
            for (int k = 1; k <= hotKeys; k++) {
                withCache.get(k);
            }
        }
        long withCacheTime = System.nanoTime() - t2;

        System.out.println("热点查询 100 key × 10 轮 = 1000 次:");
        System.out.printf("  无缓存: %.2f ms\n", noCacheTime / 1e6);
        System.out.printf("  有缓存: %.2f ms\n", withCacheTime / 1e6);
        System.out.printf("  加速比: %.1fx\n", (double) noCacheTime / withCacheTime);
        System.out.println("  缓存统计: " + withCache.cacheStats());
        System.out.println();

        System.out.println();
        System.out.println("========== 测试完成 ==========");
    }
}
