package com.ds.linked;

/**
 * Chapter2_ArrayAndLinkedList
 * com.ds.linkedList
 * MyDoubleLinkedList.java
 * 自定义双链表封装类型
 */
public class MyDoubleLinkedList<E> {

    //双链表节点内部类
    private static class Node<E> {
        E data;
        Node<E> prev;
        Node<E> next;
    }

    //创建双链表的头尾哨兵节点
    private Node<E> head = new Node<>();
    private Node<E> tail = new Node<>();

    //表示链表中保存数据节点数量的全局变量
    private int size;

    //在构造器中对头尾节点进行初始化关联
    public MyDoubleLinkedList() {
        head.next = tail;
        tail.prev = head;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    //按照下标获取双链表中保存的元素
    public E get(int index) {
        //1.判断目标下标是否越界
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("链表下标越界:" + index);
        }

        //2.计算判断目标下标距离哪一端更近并进行遍历
        int step;
        Node<E> current;
        if (index < size / 2) {
            //2.1如果目标下标距离头节点一端更近
            step = -1;
            current = head;
            while (step < index) {
                current = current.next;
                step++;
            }
        } else {
            //2.2如果目标下标距离尾节点一端更近
            step = size;
            current = tail;
            while (step > index) {
                current = current.prev;
                step--;
            }
        }

        //3.返回目标下标节点的数据域取值
        return current.data;
    }

    //在双链表中指定下标位置插入元素
    public void insert(E data, int index) {
        //1.判断目标下标是否越界
        if (index < 0 || index > size) {
            throw new IllegalArgumentException("链表下标越界:" + index);
        }

        //2.创建一个新节点,保存目标数据
        Node<E> newNode = new Node<>();
        newNode.data = data;

        //3.判断目标下标距离哪一端更近,找到插入位置的前驱节点
        int step;
        Node<E> current;
        if (index < size / 2) {
            //3.1如果目标下标距离头节点一端更近
            step = -1;
            current = head;
            while (step < index - 1) {
                current = current.next;
                step++;
            }
            //循环结束后,current变量指向下标为 index - 1 的节点
        } else {
            //3.2如果目标下标距离尾节点一端更近
            step = size;
            current = tail;
            while (step > index - 1) {
                current = current.prev;
                step--;
            }
            //循环结束后,current变量指向下标为 index - 1 的节点
        }

        //4.将新节点插入到 current 之后
        newNode.next = current.next;
        newNode.prev = current;
        current.next.prev = newNode;
        current.next = newNode;
        size++;
    }

    //在链表尾部添加元素
    public void add(E data) {
        insert(data, size);
    }

    //在链表头部添加元素
    public void addFirst(E data) {
        insert(data, 0);
    }

    //在链表尾部添加元素
    public void addLast(E data) {
        insert(data, size);
    }

    //获取链表第一个元素
    public E getFirst() {
        if (isEmpty()) {
            return null;
        }
        return get(0);
    }

    //获取链表最后一个元素
    public E getLast() {
        if (isEmpty()) {
            return null;
        }
        return get(size - 1);
    }

    //删除链表第一个元素并返回
    public E removeFirst() {
        if (isEmpty()) {
            return null;
        }
        return remove(0);
    }

    //删除链表最后一个元素并返回
    public E removeLast() {
        if (isEmpty()) {
            return null;
        }
        return remove(size - 1);
    }

    //按照下标删除元素并返回被删除的元素
    public E remove(int index) {
        if (index >= size || index < 0) {
            throw new IllegalArgumentException("链表下标越界:" + index);
        }

        int step;
        Node<E> current;
        if (index < size / 2) {
            //如果目标下标距离头节点一端更近
            step = -1;
            current = head;
            while (step < index) {
                current = current.next;
                step++;
            }
            //循环结束后,current变量指向下标为 index 的节点
        } else {
            //如果目标下标距离尾节点一端更近
            step = size;
            current = tail;
            while (step > index) {
                current = current.prev;
                step--;
            }
            //循环结束后,current变量指向下标为 index 的节点
        }

        E data = current.data;
        current.prev.next = current.next;
        current.next.prev = current.prev;
        current.next = null;
        current.prev = null;
        size--;
        return data;
    }

    //按值删除元素,删除成功返回true,否则返回false
    public boolean remove(E data) {
        Node<E> current = head.next;
        while (current != tail) {
            if (data == null ? current.data == null : data.equals(current.data)) {
                current.prev.next = current.next;
                current.next.prev = current.prev;
                current.next = null;
                current.prev = null;
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    //更新指定下标的元素
    public void update(int index, E data) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("链表下标越界:" + index);
        }
        Node<E> current = head.next;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        current.data = data;
    }

    //判断链表是否包含指定元素
    public boolean contains(E data) {
        return indexOf(data) != -1;
    }

    //查找指定元素第一次出现的下标,不存在返回-1
    public int indexOf(E data) {
        Node<E> current = head.next;
        int index = 0;
        while (current != tail) {
            if (data == null ? current.data == null : data.equals(current.data)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    //清空链表
    public void clear() {
        head.next = tail;
        tail.prev = head;
        size = 0;
    }

    //遍历链表
    public void traversal() {
        Node<E> current = head.next;
        while (current != tail) {
            System.out.println(current.data);
            current = current.next;
        }
    }

    //逆序遍历链表
    public void traversalReverse() {
        Node<E> current = tail.prev;
        while (current != head) {
            System.out.println(current.data);
            current = current.prev;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Node<E> current = head.next;
        while (current != tail) {
            sb.append(current.data).append(" ");
            current = current.next;
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        MyDoubleLinkedList<Integer> list = new MyDoubleLinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        System.out.println("链表内容: " + list);
        System.out.println("size: " + list.size());
        System.out.println("get(2): " + list.get(2));
        System.out.println("getFirst: " + list.getFirst());
        System.out.println("getLast: " + list.getLast());

        list.insert(10, 2);
        System.out.println("在index=2插入10后: " + list);

        System.out.println("remove(2): " + list.remove(2));
        System.out.println("remove后: " + list);

        list.update(1, 20);
        System.out.println("update(1,20)后: " + list);

        System.out.println("contains(20): " + list.contains(20));
        System.out.println("indexOf(4): " + list.indexOf(4));

        System.out.println("removeFirst: " + list.removeFirst());
        System.out.println("removeLast: " + list.removeLast());
        System.out.println("最终链表: " + list);

        list.clear();
        System.out.println("clear后isEmpty: " + list.isEmpty());
    }
}