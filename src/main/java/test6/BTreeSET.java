package test6;

public class BTreeSET<Key extends Comparable<Key>> {
    private Page<Key> root;
    private final Key sentinel;

    public BTreeSET(Key sentinel) {
        this.sentinel = sentinel;
        root = new Page<>(true);
        System.out.println("插入哨兵：" + sentinel);
        add(sentinel);
        root.printPage("根初始");
    }

    public boolean contains(Key key) {
        System.out.println("\n查找：" + key);
        boolean res = contains(root, key);
        System.out.println("查找结果：" + res);
        return res;
    }

    private boolean contains(Page<Key> h, Key key) {
        h.printPage("当前遍历");
        if (h.isExternal()) {
            return h.contains(key);
        }
        Page<Key> nextP = h.next(key);
        if (nextP == null) return false;
        return contains(nextP, key);
    }

    // BTreeSET.java - 修复根分裂
    public void add(Key key) {
        System.out.println("\n插入：" + key);
        add(root, key);
        if (root.isFull()) {
            Page<Key> left = root;
            Page<Key> right = root.split();
            root = new Page<>(false);
            // 注意：这里left和right都已经是最新的状态
            root.add(left);
            root.add(right);
            System.out.println("根分裂，新根生成");
            root.printPage("新根");
        }
        root.printPage("插入后根");
    }

    private void add(Page<Key> h, Key key) {
        if (h.isExternal()) {
            h.add(key);
            return;
        }
        Page<Key> nextP = h.next(key);
        add(nextP, key);
        if (nextP.isFull()) {
            Page<Key> newChild = nextP.split();
            h.add(newChild);
            nextP.close();
        }
    }

    public static void main(String[] args) {
        String sentinel = "!";
        BTreeSET<String> btree = new BTreeSET<>(sentinel);

        btree.add("A");
        btree.add("B");
        btree.add("C");

        boolean r1 = btree.contains("B");
        boolean r2 = btree.contains("D");
        System.out.println("\n最终输出：");
        System.out.println(r1);
        System.out.println(r2);
    }
}