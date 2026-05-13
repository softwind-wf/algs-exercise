package exercise3.exercise3_5;

public class MathSETArray {
    private final boolean[] present; // 标记元素是否在集合中
    private final int universeSize;  // 全集大小（假设元素是 0 ~ universeSize-1 的整数）

    // 构造函数：全集为 0 ~ universeSize-1，初始为空集
    public MathSETArray(int universeSize) {
        this.universeSize = universeSize;
        this.present = new boolean[universeSize]; // 默认都是 false（不在集合中）
    }

    // 将 key 加入集合
    public void add(int key) {
        if (key < 0 || key >= universeSize) {
            throw new IllegalArgumentException("Key out of universe range");
        }
        present[key] = true;
    }

    // 补集：所有在全集但不在当前集合中的元素
    public MathSETArray complement() {
        MathSETArray comp = new MathSETArray(universeSize);
        for (int i = 0; i < universeSize; i++) {
            if (!this.contains(i)) {
                comp.add(i);
            }
        }
        return comp;
    }

    // 并集：将 a 中所有不在当前集合的元素加入当前集合
    public void union(MathSETArray a) {
        if (a.universeSize != this.universeSize) {
            throw new IllegalArgumentException("Universes must be the same size");
        }
        for (int i = 0; i < universeSize; i++) {
            if (a.contains(i)) {
                this.add(i);
            }
        }
    }

    // 交集：删除当前集合中所有不在 a 中的元素
    public void intersection(MathSETArray a) {
        if (a.universeSize != this.universeSize) {
            throw new IllegalArgumentException("Universes must be the same size");
        }
        for (int i = 0; i < universeSize; i++) {
            if (!a.contains(i)) {
                this.delete(i);
            }
        }
    }

    // 将 key 从集合中删除
    public void delete(int key) {
        if (key < 0 || key >= universeSize) {
            throw new IllegalArgumentException("Key out of universe range");
        }
        present[key] = false;
    }

    // 判断 key 是否在集合中
    public boolean contains(int key) {
        if (key < 0 || key >= universeSize) {
            throw new IllegalArgumentException("Key out of universe range");
        }
        return present[key];
    }

    // 判断集合是否为空
    public boolean isEmpty() {
        return size() == 0;
    }

    // 返回集合中元素的数量
    public int size() {
        int count = 0;
        for (boolean b : present) {
            if (b) count++;
        }
        return count;
    }

    // 测试用例
    public static void main(String[] args) {
        // 全集为 0~9
        MathSETArray setA = new MathSETArray(10);
        MathSETArray setB = new MathSETArray(10);

        // 初始化集合A：{0,1,2,3,4}
        for (int i = 0; i < 5; i++) {
            setA.add(i);
        }
        // 初始化集合B：{4,5,6,7,8}
        for (int i = 4; i < 9; i++) {
            setB.add(i);
        }

        System.out.println("集合A的元素数：" + setA.size()); // 5
        System.out.println("集合B的元素数：" + setB.size()); // 5

        // 交集：A ∩ B = {4}
        setA.intersection(setB);
        System.out.println("A和B的交集元素数：" + setA.size()); // 1
        System.out.println("A包含4吗？" + setA.contains(4)); // true

        // 重置A，测试并集：A ∪ B = {0,1,2,3,4,5,6,7,8}
        setA = new MathSETArray(10);
        for (int i = 0; i < 5; i++) {
            setA.add(i);
        }
        setA.union(setB);
        System.out.println("A和B的并集元素数：" + setA.size()); // 9

        // 测试补集：全集 - A = {9}
        MathSETArray comp = setA.complement();
        System.out.println("A的补集元素数：" + comp.size()); // 1
        System.out.println("补集包含9吗？" + comp.contains(9)); // true
    }
}