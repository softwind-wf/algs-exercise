package test1_3;

import java.util.Scanner;

public class FixedCapacityStackOfStrings {
    private String[] a; // 栈元素数组
    private int N;      // 栈中元素数量

    // 构造函数：创建容量为 cap 的空栈
    public FixedCapacityStackOfStrings(int cap) {
        a = new String[cap];
        N = 0;
    }

    // 判断栈是否为空
    public boolean isEmpty() {
        return N == 0;
    }

    public boolean isFull(){return N==a.length;}

    // 返回栈中元素数量
    public int size() {
        return N;
    }

    // 向栈中添加一个字符串
    public void push(String item) {
        a[N++] = item;
    }

    // 删除并返回最近添加的字符串
    public String pop() {
        return a[--N];
    }

    // 主函数：测试栈的功能
    public static void main(String[] args) {
        FixedCapacityStackOfStrings s;
        s = new FixedCapacityStackOfStrings(100);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String item = scanner.next();
            if (!item.equals("-")) {
                s.push(item);
            } else if (!s.isEmpty()) {
                System.out.print(s.pop() + " ");
            }
        }
        System.out.println("(" + s.size() + " left on stack)");
        scanner.close();
    }
}