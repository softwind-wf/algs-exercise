package test1_3;

import java.util.Iterator;
import java.util.Scanner;

public class FixedCapacityStack<Item> implements Iterable<Item> {
    private Item[] a;   // 栈元素数组
    private int N;      // 栈中元素数量

    // 构造函数：创建容量为 cap 的空栈
    public FixedCapacityStack(int cap) {
        // 由于 Java 不允许直接创建泛型数组，使用 Object 数组并进行强制类型转换
        a = (Item[]) new Object[cap];
        N = 0;
    }


    private void resize(int max){
        Item[] temp=(Item[]) new Object[max];
        for(int i=0;i<N;i++){
            temp[i]=a[i];
        }
        a=temp;


    }

    // 判断栈是否为空
    public boolean isEmpty() {
        return N == 0;
    }

    // 返回栈中元素数量
    public int size() {
        return N;
    }

    // 向栈中添加一个元素
    public void push(Item item) {
        if(N==a.length)
            resize(2*a.length);


        a[N++] = item;
    }

    // 删除并返回最近添加的元素
    public Item pop() {

        Item item= a[--N];

        a[N]=null;
        if(N>0&&N==a.length/4)
            resize(a.length/2);

        return item;

    }

    // 主函数：测试栈的功能
    public static void main(String[] args) {
        FixedCapacityStack<String> s;
        s = new FixedCapacityStack<>(100);

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

    @Override
    public Iterator<Item> iterator() {
        return new ReverseArrayIterator();
    }

    private class ReverseArrayIterator implements Iterator<Item>{

        private int i=N;


        @Override
        public boolean hasNext() {
            return i>0;
        }

        @Override
        public Item next() {
            return a[--i];
        }
    }


}