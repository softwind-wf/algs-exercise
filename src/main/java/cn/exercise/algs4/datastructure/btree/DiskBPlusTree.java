package cn.exercise.algs4.datastructure.btree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 磁盘版 B+ 树 —— 演示数据库索引是如何在磁盘上工作的
 *
 * 核心思想:树的每个节点 = 磁盘上一页(固定大小、定长槽位),节点间用"页号"
 * 而不是内存指针相连。访问某个孩子,必须先把它那一页从磁盘读进内存(记一次磁盘读)。
 *
 * 页布局(int 数组,定长):
 *   [0]         isLeaf(1/0)
 *   [1]         n(本页键数)
 *   [2 ..]      键    (maxKeys 槽)
 *   [.. ..]     值    (maxKeys 槽,仅叶子使用:记录本体就存在叶子页里)
 *   [.. ..]     子页号 (maxKeys+1 槽,仅内部节点使用)
 *   最后一格    下一个叶子页号(仅叶子使用:B+ 树的范围扫描就靠它)
 *
 * 磁盘对象 Disk 用内存 Map 模拟"文件系统里的页文件",每次 read/write 都计数,
 * 并记录读页顺序,便于观察一次查询到底访问了哪几页。
 *
 * 演示目的(与 cn.exercise.algs4.datastructure.btree.BPlusTree 对照):
 *  - 点查询:树高多少层,就读多少页 —— 磁盘访问次数 = 树高;
 *  - 范围查询:先沿树定位起点,再沿叶子链表"顺序读"连续页;
 *  - 记录只存在叶子页里(内部页只是索引)。
 *
 * 注意:本演示省略了真实数据库的"空闲页回收/写回日志/缓冲池",聚焦 I/O 模型本身。
 */
public class DiskBPlusTree {

    // ---- 页布局(与 t 相关) ----
    private final int maxKeys;        // 2t-1:每页最多键数
    private final int minKeys;        // t-1
    private final int idxIsLeaf;      // 0
    private final int idxN;           // 1
    private final int idxKeys;        // 2
    private final int idxVals;        // 2 + maxKeys
    private final int idxChildren;    // 2 + 2*maxKeys
    private final int idxNext;        // 2 + 2*maxKeys + (maxKeys+1)
    private final int pageInts;       // 页占多少 int 槽

    private int rootPage = -1;        // 根所在的页号(-1 = 空树)
    private int size = 0;

    /** 磁盘对象:模拟固定大小页文件,统计并记录每次 I/O。 */
    public static class Disk {
        public final Map<Integer, int[]> pages = new HashMap<>();   // 页号 -> 页内容
        public final List<Integer> readOrder = new ArrayList<>();   // 本次操作的读页顺序
        public long reads = 0;
        public long writes = 0;
        private int nextPageId = 0;

        /** 分配一个新页并写盘(记为一次写)。返回页号。 */
        public int allocPage(int[] data) {
            int id = nextPageId++;
            pages.put(id, data.clone());
            writes++;
            return id;
        }

        /** 读一页(记为一次磁盘读)。返回副本,修改后须 write 回去。 */
        public int[] read(int id) {
            reads++;
            readOrder.add(id);
            return pages.get(id).clone();
        }

        /** 把修改后的页写回磁盘(记为一次写)。 */
        public void write(int id, int[] data) {
            writes++;
            pages.put(id, data.clone());
        }

        public int pageCount() {
            return pages.size();
        }

        public void resetIo() {
            reads = 0;
            writes = 0;
            readOrder.clear();
        }
    }

    public final Disk disk = new Disk();

    public DiskBPlusTree() {
        this(2);
    }

    public DiskBPlusTree(int t) {
        if (t < 2) {
            throw new IllegalArgumentException("最小度数 t 必须 >= 2,实际 " + t);
        }
        this.maxKeys = 2 * t - 1;
        this.minKeys = t - 1;
        this.idxIsLeaf = 0;
        this.idxN = 1;
        this.idxKeys = 2;
        this.idxVals = idxKeys + maxKeys;
        this.idxChildren = idxVals + maxKeys;
        this.idxNext = idxChildren + (maxKeys + 1);
        this.pageInts = idxNext + 1;
    }

    // ---------- 页槽位访问 ----------
    private int keySlot(int i) {
        return idxKeys + i;
    }

    private int valSlot(int i) {
        return idxVals + i;
    }

    private int childSlot(int i) {
        return idxChildren + i;
    }

    private int keyAt(int[] p, int i) {
        return p[keySlot(i)];
    }

    private void setKey(int[] p, int i, int v) {
        p[keySlot(i)] = v;
    }

    private int valueAt(int[] p, int i) {
        return p[valSlot(i)];
    }

    private void setValue(int[] p, int i, int v) {
        p[valSlot(i)] = v;
    }

    private int childAt(int[] p, int i) {
        return p[childSlot(i)];
    }

    private void setChild(int[] p, int i, int v) {
        p[childSlot(i)] = v;
    }

    private int nOf(int[] p) {
        return p[idxN];
    }

    private boolean isLeaf(int[] p) {
        return p[idxIsLeaf] == 1;
    }

    private int[] newLeafPage() {
        int[] p = new int[pageInts];
        p[idxIsLeaf] = 1;
        p[idxN] = 0;
        p[idxNext] = -1;
        return p;
    }

    private int[] newInternalPage() {
        int[] p = new int[pageInts];
        p[idxIsLeaf] = 0;
        p[idxN] = 0;
        for (int i = 0; i <= maxKeys; i++) {
            p[childSlot(i)] = -1;
        }
        return p;
    }

    // ---------- 路由与查找 ----------

    /** 内部节点路由:返回应下潜的孩子下标(分隔键 keys[i] = 右子树最小键)。 */
    private int findChild(int[] p, int key) {
        int i = 0;
        while (i < nOf(p) && key >= keyAt(p, i)) {
            i++;
        }
        return i;
    }

    /** 叶子内定位:第一个键 >= key 的下标(用于查找与插入定位)。 */
    private int findLeafPos(int[] p, int key) {
        int i = 0;
        while (i < nOf(p) && key > keyAt(p, i)) {
            i++;
        }
        return i;
    }

    // ==================== 插入 ====================

    /** 插入 (key, value) 记录。返回 true 成功;false 键已存在。 */
    public boolean insert(int key, int value) {
        if (rootPage < 0) {
            int[] p = newLeafPage();
            p[idxN] = 1;
            setKey(p, 0, key);
            setValue(p, 0, value);
            rootPage = disk.allocPage(p);
            size++;
            return true;
        }
        int[] root = disk.read(rootPage);
        if (nOf(root) == maxKeys) {
            rootPage = splitRoot(root);
            root = disk.read(rootPage);
        }
        Node cur = new Node(rootPage, root);
        while (true) {
            if (isLeaf(cur.p)) {
                int i = findLeafPos(cur.p, key);
                if (i < nOf(cur.p) && keyAt(cur.p, i) == key) {
                    return false;                       // 重复键
                }
                insertKeyValue(cur, i, key, value);
                disk.write(cur.pageId, cur.p);          // 叶子页写回磁盘
                size++;
                return true;
            }
            int i = findChild(cur.p, key);
            int childId = childAt(cur.p, i);
            int[] child = disk.read(childId);
            if (nOf(child) == maxKeys) {
                splitChild(cur, i, child);              // 满孩子:先分裂,再重新定位
                continue;
            }
            cur = new Node(childId, child);
        }
    }

    /** 分裂根:根无父页可承接,单独处理,树增高一层。返回新根页号。 */
    private int splitRoot(int[] old) {
        if (isLeaf(old)) {
            int[] right = newLeafPage();
            for (int j = 0; j < nOf(old) - minKeys; j++) {      // 右半取 keys[minKeys..]
                setKey(right, j, keyAt(old, minKeys + j));
                setValue(right, j, valueAt(old, minKeys + j));
            }
            right[idxN] = nOf(old) - minKeys;
            right[idxNext] = old[idxNext];
            int rightId = disk.allocPage(right);

            int[] left = newLeafPage();
            left[idxN] = minKeys;                               // 左半 keys[0..t-2]
            for (int j = 0; j < minKeys; j++) {
                setKey(left, j, keyAt(old, j));
                setValue(left, j, valueAt(old, j));
            }
            left[idxNext] = rightId;                            // 叶子链表:左 -> 右
            int leftId = disk.allocPage(left);

            int[] newRoot = newInternalPage();
            newRoot[idxN] = 1;
            setKey(newRoot, 0, keyAt(right, 0));                // 分隔键 = 右叶最小键(拷贝)
            setChild(newRoot, 0, leftId);
            setChild(newRoot, 1, rightId);
            return disk.allocPage(newRoot);
        } else {
            int[] right = newInternalPage();
            right[idxN] = minKeys;
            for (int j = 0; j < minKeys; j++) {
                setKey(right, j, keyAt(old, minKeys + 1 + j));  // keys[t..2t-2]
            }
            for (int j = 0; j <= minKeys; j++) {
                setChild(right, j, childAt(old, minKeys + 1 + j)); // children[t..2t-1]
            }
            int rightId = disk.allocPage(right);

            int[] left = newInternalPage();
            left[idxN] = minKeys;
            for (int j = 0; j < minKeys; j++) {
                setKey(left, j, keyAt(old, j));                 // keys[0..t-2]
            }
            for (int j = 0; j <= minKeys; j++) {
                setChild(left, j, childAt(old, j));             // children[0..t-1]
            }
            int leftId = disk.allocPage(left);

            int[] newRoot = newInternalPage();
            newRoot[idxN] = 1;
            setKey(newRoot, 0, keyAt(old, minKeys));            // 中间键 keys[t-1] 上浮(移动)
            setChild(newRoot, 0, leftId);
            setChild(newRoot, 1, rightId);
            return disk.allocPage(newRoot);
        }
    }

    /** 分裂 parent 的第 i 个满孩子,分隔键上浮进 parent。parent 内容在内存中,修改后写回。 */
    private void splitChild(Node parent, int i, int[] child) {
        int childId = childAt(parent.p, i);
        if (isLeaf(child)) {
            int[] right = newLeafPage();
            for (int j = 0; j < nOf(child) - minKeys; j++) {    // 右半取 keys[minKeys..2t-2]
                setKey(right, j, keyAt(child, minKeys + j));
                setValue(right, j, valueAt(child, minKeys + j));
            }
            right[idxN] = nOf(child) - minKeys;
            right[idxNext] = child[idxNext];
            int rightId = disk.allocPage(right);

            child[idxN] = minKeys;                              // 左半 keys[0..t-2]
            child[idxNext] = rightId;
            disk.write(childId, child);

            insertSeparator(parent, i, keyAt(right, 0), rightId);   // 分隔键 = 右叶最小键(拷贝)
        } else {
            int[] right = newInternalPage();
            right[idxN] = minKeys;
            for (int j = 0; j < minKeys; j++) {
                setKey(right, j, keyAt(child, minKeys + 1 + j));    // keys[t..2t-2]
            }
            for (int j = 0; j <= minKeys; j++) {
                setChild(right, j, childAt(child, minKeys + 1 + j)); // children[t..2t-1]
            }
            int rightId = disk.allocPage(right);

            child[idxN] = minKeys;                                  // 左半 keys[0..t-2],children[0..t-1]
            disk.write(childId, child);

            insertSeparator(parent, i, keyAt(child, minKeys), rightId); // 中间键上浮(移动)
        }
        disk.write(parent.pageId, parent.p);
    }

    /** 在 parent 页的 keys[i] 处插入分隔键,并把新页挂在 children[i+1]。 */
    private void insertSeparator(Node parent, int i, int sep, int rightId) {
        int[] p = parent.p;
        int n = nOf(p);
        for (int j = n; j > i; j--) {
            setKey(p, j, keyAt(p, j - 1));
        }
        for (int j = n + 1; j > i + 1; j--) {
            setChild(p, j, childAt(p, j - 1));
        }
        setKey(p, i, sep);
        setChild(p, i + 1, rightId);
        p[idxN] = n + 1;
    }

    /** 往叶子页 keys[i] 处插入 (key,value)(叶子保证未满)。 */
    private void insertKeyValue(Node leaf, int i, int key, int value) {
        int[] p = leaf.p;
        int n = nOf(p);
        for (int j = n; j > i; j--) {
            setKey(p, j, keyAt(p, j - 1));
            setValue(p, j, valueAt(p, j - 1));
        }
        setKey(p, i, key);
        setValue(p, i, value);
        p[idxN] = n + 1;
    }

    // ==================== 查询 ====================

    /** 点查询:沿树下行,每层读一页,最后在叶子页里找记录。返回 value;不存在返回 null。 */
    public Integer get(int key) {
        if (rootPage < 0) {
            return null;
        }
        int[] p = disk.read(rootPage);
        while (!isLeaf(p)) {
            p = disk.read(childAt(p, findChild(p, key)));
        }
        int i = findLeafPos(p, key);
        return (i < nOf(p) && keyAt(p, i) == key) ? valueAt(p, i) : null;
    }

    public boolean contains(int key) {
        return get(key) != null;
    }

    /** 范围查询:沿树定位到 lo 所在的叶子页,然后沿叶子链表顺序读页。 */
    public List<Integer> keysInRange(int lo, int hi) {
        List<Integer> result = new ArrayList<>();
        if (rootPage < 0 || lo > hi) {
            return result;
        }
        int[] p = disk.read(rootPage);
        while (!isLeaf(p)) {
            p = disk.read(childAt(p, findChild(p, lo)));
        }
        while (true) {
            for (int i = 0; i < nOf(p); i++) {
                int k = keyAt(p, i);
                if (k > hi) {
                    return result;
                }
                if (k >= lo) {
                    result.add(k);
                }
            }
            int next = p[idxNext];
            if (next < 0) {
                return result;
            }
            p = disk.read(next);            // 顺序读下一个叶子页
        }
    }

    // ==================== 统计 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** 树高(空树 -1;单页 0)。磁盘点查询的读页次数 = 树高 + 1(根页)。 */
    public int height() {
        if (rootPage < 0) {
            return -1;
        }
        int h = 0;
        int[] p = disk.read(rootPage);
        while (!isLeaf(p)) {
            h++;
            p = disk.read(childAt(p, 0));
        }
        return h;
    }

    // ==================== 页布局打印 ====================

    /** 打印整棵"磁盘页布局":页号 + 内容 + 孩子页号/叶子链表。 */
    public void printTree() {
        if (rootPage < 0) {
            System.out.println("  (empty)");
            return;
        }
        printPage(rootPage, 0);
    }

    private void printPage(int pageId, int depth) {
        int[] p = disk.read(pageId);
        String ind = indent(depth);
        StringBuilder sb = new StringBuilder(ind).append("page ").append(pageId)
                .append(" [").append(isLeaf(p) ? "LEAF" : "INT ").append(" ").append(nOf(p)).append(" keys]: ");
        for (int i = 0; i < nOf(p); i++) {
            if (i > 0) {
                sb.append("  ");
            }
            sb.append(keyAt(p, i));
            if (isLeaf(p)) {
                sb.append("->").append(valueAt(p, i));
            }
        }
        if (isLeaf(p)) {
            sb.append("    (next leaf = ").append(p[idxNext]).append(")");
        } else {
            sb.append("    (children pages: ");
            for (int i = 0; i <= nOf(p); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(childAt(p, i));
            }
            sb.append(")");
        }
        System.out.println(sb);
        if (!isLeaf(p)) {
            for (int i = 0; i <= nOf(p); i++) {
                printPage(childAt(p, i), depth + 1);
            }
        }
    }

    private static String indent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    // ==================== 结构校验(页级) ====================

    /** 校验 B+ 树磁盘不变量。null 表示合法。(会读所有页,测试后请 resetIo。) */
    public String checkStructure() {
        if (rootPage < 0) {
            return size == 0 ? null : "root 为空但 size=" + size;
        }
        int[] leafDepth = {-1};
        int[] count = {0};
        int[] leafCount = {0};
        String err = checkNode(rootPage, Integer.MIN_VALUE, false, Integer.MAX_VALUE, 0, true, leafDepth, count, leafCount);
        if (err != null) {
            return err;
        }
        // 叶子链表一致性
        int chainKeys = 0;
        int chainLeaves = 0;
        int prev = Integer.MIN_VALUE;
        int cur = firstLeafPage();
        while (cur >= 0) {
            int[] p = disk.read(cur);
            chainKeys += nOf(p);
            chainLeaves++;
            for (int i = 0; i < nOf(p); i++) {
                if (keyAt(p, i) <= prev) {
                    return "叶子链表跨叶无序";
                }
                prev = keyAt(p, i);
            }
            cur = p[idxNext];
        }
        if (chainKeys != count[0]) {
            return "叶子链表键数 " + chainKeys + " != 树中键数 " + count[0];
        }
        if (chainLeaves != leafCount[0]) {
            return "叶子链表页数 " + chainLeaves + " != 树中叶子数 " + leafCount[0];
        }
        return count[0] == size ? null : "实际键数 " + count[0] + " 与 size " + size + " 不一致";
    }

    private String checkNode(int pageId, int lo, boolean loInc, int hi, int depth, boolean isRoot,
                             int[] leafDepth, int[] count, int[] leafCount) {
        int[] p = disk.read(pageId);
        int n = nOf(p);
        int min = isRoot ? 1 : minKeys;
        if (n < min || n > maxKeys) {
            return "页" + pageId + " 键数 " + n + " 超出 [" + min + "," + maxKeys + "]";
        }
        for (int i = 1; i < n; i++) {
            if (keyAt(p, i) <= keyAt(p, i - 1)) {
                return "页" + pageId + " 键未严格递增";
            }
        }
        for (int i = 0; i < n; i++) {
            int k = keyAt(p, i);
            if (k < lo || (k == lo && !loInc) || k >= hi) {
                return "页" + pageId + " 键值越界 (lo=" + lo + ", hi=" + hi + ")";
            }
        }
        if (isLeaf(p)) {
            leafCount[0]++;
            count[0] += n;
            if (leafDepth[0] == -1) {
                leafDepth[0] = depth;
            } else if (leafDepth[0] != depth) {
                return "叶子深度不一致:期望 " + leafDepth[0] + ",实际 " + depth;
            }
            return null;
        }
        for (int i = 0; i < n; i++) {
            int rm = subtreeMin(childAt(p, i + 1));
            if (keyAt(p, i) != rm) {
                return "页" + pageId + " 分隔键 " + keyAt(p, i) + " != 右子树最小键 " + rm;
            }
        }
        for (int i = 0; i <= n; i++) {
            boolean clInc = (i == 0) ? loInc : true;
            int clLo = (i == 0) ? lo : keyAt(p, i - 1);
            int clHi = (i == n) ? hi : keyAt(p, i);
            String err = checkNode(childAt(p, i), clLo, clInc, clHi, depth + 1, false, leafDepth, count, leafCount);
            if (err != null) {
                return err;
            }
        }
        return null;
    }

    private int firstLeafPage() {
        if (rootPage < 0) {
            return -1;
        }
        int pageId = rootPage;
        int[] p = disk.read(pageId);
        while (!isLeaf(p)) {
            pageId = childAt(p, 0);
            p = disk.read(pageId);
        }
        return pageId;
    }

    /** 子树最小键:沿最左路径下行到叶子(页级,会读盘)。 */
    private int subtreeMin(int pageId) {
        int[] p = disk.read(pageId);
        while (!isLeaf(p)) {
            p = disk.read(childAt(p, 0));
        }
        return keyAt(p, 0);
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        int t = 2;
        int n = 40;
        DiskBPlusTree tree = new DiskBPlusTree(t);

        System.out.println("=== Disk B+ Tree demo (t=" + t + ", one page holds up to " + (2 * t - 1) + " records) ===");
        System.out.println("Every B+ tree node = ONE fixed-size page on disk; children are referenced by page number.");
        System.out.println();

        // 插入记录
        for (int k = 1; k <= n; k++) {
            tree.insert(k, k * 100);
        }
        System.out.println("After inserting " + n + " records (key=1.." + n + ", value=key*100):");
        System.out.println("  height = " + tree.height() + " levels  (a point query reads " + (tree.height() + 1) + " pages)");
        System.out.println("  total disk pages = " + tree.disk.pageCount());

        System.out.println();
        System.out.println("--- Page layout on disk (leaf pages hold the actual records) ---");
        tree.disk.resetIo();
        tree.printTree();

        System.out.println();
        System.out.println("--- Point lookup: get(37) ---");
        tree.disk.resetIo();
        Integer v = tree.get(37);
        System.out.println("  get(37) = " + v);
        System.out.println("  pages read (top to bottom): " + tree.disk.readOrder);
        System.out.println("  disk reads = " + tree.disk.reads + "   <== exactly height+1, one page per level");

        System.out.println();
        System.out.println("--- Range query: keysInRange(10, 20) ---");
        tree.disk.resetIo();
        List<Integer> range = tree.keysInRange(10, 20);
        System.out.println("  keysInRange(10,20) = " + range);
        System.out.println("  pages read: " + tree.disk.readOrder);
        System.out.println("  disk reads = " + tree.disk.reads + "   <== tree descent to locate 10, then SEQUENTIAL leaf pages");

        System.out.println();
        System.out.println("--- Why page size matters (fanout vs height) ---");
        System.out.println("  Here a page fits only 3 keys (t=2), so height grows fast.");
        System.out.println("  On a real 8KB page (~500 keys/pointer entries), height stays ~4 for BILLIONS of records:");
        System.out.println("  500^4 = 6.25e10  =>  one lookup = ~4 random disk reads.  A red-black tree would need ~30.");

        System.out.println();
        System.out.println("--- Sanity checks ---");
        tree.disk.resetIo();
        String err = tree.checkStructure();
        boolean allOk = true;
        for (int k = 1; k <= n; k++) {
            Integer got = tree.get(k);
            if (got == null || got.intValue() != k * 100) {
                allOk = false;
                break;
            }
        }
        System.out.println("  checkStructure = " + (err == null ? "OK" : "FAIL: " + err));
        System.out.println("  all " + n + " records retrievable by key = " + allOk);

        System.out.println();
        System.out.println("--- Random-order stress (1000 keys, t=2) ---");
        DiskBPlusTree rtree = new DiskBPlusTree(2);
        Random rnd = new Random(12345);
        List<Integer> keys = new ArrayList<>();
        for (int k = 1; k <= 1000; k++) {
            keys.add(k);
        }
        Collections.shuffle(keys, rnd);
        for (int k : keys) {
            rtree.insert(k, k * 10);
        }
        String rerr = rtree.checkStructure();
        boolean rok = true;
        for (int k = 1; k <= 1000; k++) {
            Integer got = rtree.get(k);
            if (got == null || got.intValue() != k * 10) {
                rok = false;
                break;
            }
        }
        System.out.println("  structure = " + (rerr == null ? "OK" : "FAIL: " + rerr)
                + " | height = " + rtree.height() + " | pages = " + rtree.disk.pageCount()
                + " | all 1000 retrievable = " + rok);
    }

    /** 内存游标:当前正在处理的一页(页号 + 内容副本)。 */
    private static class Node {
        final int pageId;
        final int[] p;

        Node(int pageId, int[] p) {
            this.pageId = pageId;
            this.p = p;
        }
    }
}
