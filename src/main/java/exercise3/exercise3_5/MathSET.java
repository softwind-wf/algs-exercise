package exercise3.exercise3_5;

import java.util.Collections;
import java.util.HashSet;

public class MathSET<Key> {
    private final HashSet<Key> universe; // 全集（所有可能元素）
    private final HashSet<Key> set;      // 当前集合中的元素

    // 构造函数：用全集初始化集合（初始为空集）
    public MathSET(Key[] universe) {
        this.universe = new HashSet<>();
        Collections.addAll(this.universe, universe);
        this.set = new HashSet<>();
    }

    // 将 key 加入集合
    public void add(Key key) {
        if (!universe.contains(key)) {
            throw new IllegalArgumentException("Key not in universe");
        }
        set.add(key);
    }

    // 补集：所有在 universe 中、但不在当前集合中的元素
    public MathSET<Key> complement() {
        MathSET<Key> comp = new MathSET<>(universe.toArray((Key[]) new Object[0]));
        for (Key key : universe) {
            if (!this.contains(key)) {
                comp.add(key);
            }
        }
        return comp;
    }

    // 并集：将 a 中所有不在当前集合的元素加入当前集合
    public void union(MathSET<Key> a) {
        for (Key key : a.set) {
            this.add(key);
        }
    }

    // 交集：删除当前集合中所有不在 a 中的元素
    public void intersection(MathSET<Key> a) {
        set.retainAll(a.set);
    }

    // 将 key 从集合中删除
    public void delete(Key key) {
        set.remove(key);
    }

    // 判断 key 是否在集合中
    public boolean contains(Key key) {
        return set.contains(key);
    }

    // 判断集合是否为空
    public boolean isEmpty() {
        return set.isEmpty();
    }

    // 返回集合中元素的数量
    public int size() {
        return set.size();
    }

    // 测试用例
    public static void main(String[] args) {
        // 定义全集
        Integer[] universe = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        // 创建两个集合
        MathSET<Integer> setA = new MathSET<>(universe);
        MathSET<Integer> setB = new MathSET<>(universe);

        // 初始化集合A：{1,2,3,4,5}
        for (int i = 1; i <= 5; i++) {
            setA.add(i);
        }
        // 初始化集合B：{4,5,6,7,8}
        for (int i = 4; i <= 8; i++) {
            setB.add(i);
        }

        System.out.println("集合A的元素数：" + setA.size()); // 5
        System.out.println("集合B的元素数：" + setB.size()); // 5

        // 测试交集：A ∩ B = {4,5}
        setA.intersection(setB);
        System.out.println("A和B的交集元素数：" + setA.size()); // 2
        System.out.println("A包含4吗？" + setA.contains(4)); // true

        // 重置A，测试并集：A ∪ B = {1,2,3,4,5,6,7,8}
        setA = new MathSET<>(universe);
        for (int i = 1; i <= 5; i++) {
            setA.add(i);
        }
        setA.union(setB);
        System.out.println("A和B的并集元素数：" + setA.size()); // 8

        // 测试补集：全集 - A = {9,10}
        MathSET<Integer> comp = setA.complement();
        System.out.println("A的补集元素数：" + comp.size()); // 2
        System.out.println("补集包含9吗？" + comp.contains(9)); // true
    }
}