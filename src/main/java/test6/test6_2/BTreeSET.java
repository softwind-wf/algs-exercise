package test6.test6_2;

public class BTreeSET<Key extends Comparable<Key>> {
    private int rootIdx;
    private final Key sentinel;
    private final DiskManager<Key> disk;

    public BTreeSET(Key sentinel, DiskManager<Key> disk) {
        this.sentinel = sentinel;
        this.disk = disk;
        if (disk.hasExistingTree()) {
            this.rootIdx = disk.getRootIdx();
            System.out.println("从磁盘加载已有B树，rootIdx=" + rootIdx);
        } else {
            rootIdx = disk.allocatePage(true);
            Page<Key> root = disk.getPage(rootIdx);
            System.out.println("插入哨兵：" + sentinel);
            addInternal(root, sentinel);
            disk.writePage(root);
            disk.setRootIdx(rootIdx);
            root.printPage("根初始");
        }
    }

    public boolean contains(Key key) {
        System.out.println("\n查找：" + key);
        Page<Key> root = disk.getPage(rootIdx);
        boolean res = contains(root, key);
        System.out.println("查找结果：" + res);
        return res;
    }

    private boolean contains(Page<Key> h, Key key) {
        h.printPage("当前遍历");
        if (h.isExternal()) return h.contains(key);
        Page<Key> nextP = h.next(key, disk);
        if (nextP == null) return false;
        return contains(nextP, key);
    }

    public void add(Key key) {
        System.out.println("\n插入：" + key);
        Page<Key> root = disk.getPage(rootIdx);
        addInternal(root, key);
        if (root.isFull()) {
            Page<Key> left = root;
            Page<Key> right = root.split(disk);
            rootIdx = disk.allocatePage(false);
            Page<Key> newRoot = disk.getPage(rootIdx);
            newRoot.st.put(left.keys().iterator().next(), left.pageIndex);
            newRoot.st.put(right.keys().iterator().next(), right.pageIndex);
            disk.writePage(newRoot);
            disk.setRootIdx(rootIdx);
            System.out.println("根分裂，新根#" + rootIdx);
            newRoot.printPage("新根");
        }
        disk.getPage(rootIdx).printPage("插入后根");
    }

    private void addInternal(Page<Key> h, Key key) {
        if (h.isExternal()) {
            h.add(key);
            disk.writePage(h);
            return;
        }
        Page<Key> nextP = h.next(key, disk);
        addInternal(nextP, key);
        if (nextP.isFull()) {
            Page<Key> newChild = nextP.split(disk);
            h.st.put(newChild.keys().iterator().next(), newChild.pageIndex);
            disk.writePage(h);
        }
    }

    public void close() { disk.close(); }

    public static void main(String[] args) {
        String filePath = "btree.dat";

        DiskManager<String> disk = new DiskManager<>(filePath);
        String sentinel = "!";
        BTreeSET<String> btree = new BTreeSET<>(sentinel, disk);

        btree.add("A");
        btree.add("B");
        btree.add("C");
        btree.add("D");
        btree.add("E");
        btree.add("F");

        boolean r1 = btree.contains("B");
        boolean r2 = btree.contains("D");
        boolean r3 = btree.contains("G");
        System.out.println("\n最终输出：");
        System.out.println("contains B = " + r1);
        System.out.println("contains D = " + r2);
        System.out.println("contains G = " + r3);

        btree.close();

        System.out.println("\n--- 从磁盘重新加载 ---");
        DiskManager<String> disk2 = new DiskManager<>(filePath);
        BTreeSET<String> btree2 = new BTreeSET<>(sentinel, disk2);
        System.out.println("contains B = " + btree2.contains("B"));
        System.out.println("contains D = " + btree2.contains("D"));
        System.out.println("contains G = " + btree2.contains("G"));
        btree2.close();
    }
}
