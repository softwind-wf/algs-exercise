package cn.exercise.algs4.impl.ch03;

/**
 * 二叉排序树 (BST) 测试类
 * 演示插入、查找、删除、遍历等核心操作
 */
public class BinarySearchTreeTest {
    
    public static void main(String[] args) {
        System.out.println("=== 二叉排序树 (BST) 功能测试 ===\n");
        
        // 创建一个整数键、字符串值的 BST
        BST<Integer, String> bst = new BST<>();
        
        // 1. 测试插入
        System.out.println("1. 插入键值对:");
        int[] keys = {50, 30, 70, 20, 40, 60, 80, 10, 25, 35, 45};
        String[] values = {"五十", "三十", "七十", "二十", "四十", "六十", "八十", "十", "二十五", "三十五", "四十五"};
        
        for (int i = 0; i < keys.length; i++) {
            bst.put(keys[i], values[i]);
            System.out.println("  插入: " + keys[i] + " -> " + values[i]);
        }
        System.out.println("  树的大小: " + bst.size());
        
        // 2. 测试查找
        System.out.println("\n2. 查找操作:");
        System.out.println("  查找 40: " + bst.get(40));
        System.out.println("  查找 50: " + bst.get(50));
        System.out.println("  查找 99: " + bst.get(99)); // 不存在的键
        
        // 3. 测试最值
        System.out.println("\n3. 最值查询:");
        System.out.println("  最小键: " + bst.min());
        System.out.println("  最大键: " + bst.max());
        
        // 4. 测试向下/向上取整
        System.out.println("\n4. 取整操作:");
        System.out.println("  floor(33): " + bst.floor(33));     // 小于等于33的最大键
        System.out.println("  floor(35): " + bst.floor(35));     // 等于35
        System.out.println("  ceiling(33): " + bst.ceiling(33)); // 大于等于33的最小键
        System.out.println("  ceiling(35): " + bst.ceiling(35)); // 等于35
        
        // 5. 测试排名和选择
        System.out.println("\n5. 排名与选择:");
        System.out.println("  rank(20): " + bst.rank(20));       // 小于20的键的数量
        System.out.println("  rank(50): " + bst.rank(50));       // 小于50的键的数量
        System.out.println("  select(0): " + bst.select(0));     // 排名0的键（最小）
        System.out.println("  select(5): " + bst.select(5));     // 排名5的键
        
        // 6. 测试中序遍历（自然得到有序序列）
        System.out.println("\n6. 中序遍历（有序输出）:");
        System.out.print("  所有键: ");
        for (Integer key : bst.keys()) {
            System.out.print(key + " ");
        }
        System.out.println();
        
        // 7. 测试范围查询
        System.out.println("\n7. 范围查询 [25, 60]:");
        System.out.print("  范围内键: ");
        for (Integer key : bst.keys(25, 60)) {
            System.out.print(key + " ");
        }
        System.out.println();
        
        // 8. 测试删除最小/最大
        System.out.println("\n8. 删除最值:");
        System.out.println("  删除前大小: " + bst.size());
        System.out.println("  删除最小键: " + bst.min());
        bst.deleteMin();
        System.out.println("  删除后大小: " + bst.size());
        System.out.println("  新的最小键: " + bst.min());
        
        System.out.println("  删除最大键: " + bst.max());
        bst.deleteMax();
        System.out.println("  删除后大小: " + bst.size());
        System.out.println("  新的最大键: " + bst.max());
        
        // 9. 测试删除任意结点（包括度为0、1、2的情况）
        System.out.println("\n9. 删除指定键:");
        System.out.println("  当前树的大小: " + bst.size());
        System.out.print("  当前所有键: ");
        for (Integer key : bst.keys()) {
            System.out.print(key + " ");
        }
        System.out.println();
        
        // 删除叶子结点（度为0）
        System.out.println("\n  删除叶子结点 10:");
        bst.delete(10);
        System.out.println("  删除后大小: " + bst.size());
        System.out.print("  剩余键: ");
        for (Integer key : bst.keys()) {
            System.out.print(key + " ");
        }
        System.out.println();
        
        // 删除只有一个子结点的结点（度为1）
        System.out.println("\n  删除只有一个子结点的 30:");
        bst.delete(30);
        System.out.println("  删除后大小: " + bst.size());
        System.out.print("  剩余键: ");
        for (Integer key : bst.keys()) {
            System.out.print(key + " ");
        }
        System.out.println();
        
        // 删除有两个子结点的结点（度为2）
        System.out.println("\n  删除有两个子结点的 50:");
        bst.delete(50);
        System.out.println("  删除后大小: " + bst.size());
        System.out.print("  剩余键: ");
        for (Integer key : bst.keys()) {
            System.out.print(key + " ");
        }
        System.out.println();
        
        // 10. 更新已有键的值
        System.out.println("\n10. 更新值:");
        System.out.println("  40 的旧值: " + bst.get(40));
        bst.put(40, "四十（已更新）");
        System.out.println("  40 的新值: " + bst.get(40));
        
        System.out.println("\n=== 测试完成 ===");
    }
}