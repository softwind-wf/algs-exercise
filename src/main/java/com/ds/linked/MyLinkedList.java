package com.ds.linked;

/**
Chapter2_ArrayAndLinkedList
自定义单链表封装类型
 */
public class MyLinkedList<E> {

    //通过静态内部类定义链表节点类型
    private static class Node<E> {
        E data;
        Node<E> next;
    }

    //链表结构头节点
    private Node<E> head = new Node<>();

    //表示链表中保存数据节点数量的全局变量
    private int size;

    //在单链表中按照下标查询节点并返回节点保存数据,单链表节点下标从0开始计算
    public E get(int index) {
        //1.判断目标下标是否越界
        if (index >= size || index < 0) {
            throw new IllegalArgumentException("链表下标越界:" + index);
        }

        /*
        2.创建步进器,从头节点开始遍历
        从头节点开始遍历是为了防止只有头节点,而没有后续节点的情况
         */
        int step = -1;
        Node<E> current = head;

        while (step < index) {
            //current变量取得后继节点内存地址
            //current变量指向下一节点
            current = current.next;
            //步进器自增
            step++;
        }
        //循环结束后,current变量指向下标为index的节点

        //3.返回节点保存数据
        return current.data;
    }

    public void traversal() {
        Node<E> current = head;
        while (current.next != null) {
            current = current.next;
            System.out.println(current.data);
        }
    }

    public void insert(E data, int index) { 
        if (index > size || index < 0){
            throw new IllegalArgumentException("链表下标越界:" + index);
        }

        // 1. 找到插入位置的前驱节点
        // 因为要在 index 位置插入，所以需要找到 index-1 位置的节点
        // 如果 index 为 0，则前驱节点就是头节点 head
        Node<E> prev = head;
        for (int i = 0; i < index; i++) {
            prev = prev.next;
        }

        // 2. 创建新节点
        Node<E> newNode = new Node<>();
        newNode.data = data;
        
        // 3. 将新节点插入到 prev 之后
        // 先让新节点指向 prev 原来的下一个节点
        newNode.next = prev.next;
        // 再让 prev 指向新节点
        prev.next = newNode;

        // 4. 链表大小加1
        size++;
    }

    public E remove(int index){
        if (index >= size || index < 0){
            throw new IllegalArgumentException("链表下标越界:" + index);
        }
        Node<E> prev = head;
        for (int i = 0; i < index; i++) {
            prev = prev.next;
        }
        Node<E> removed = prev.next;
        prev.next = removed.next;
        removed.next = null;
        size--;

        return removed.data;

    }

    public E removeLast(){
        if (isEmpty()){
            return null;
        }
        Node<E> prev = head;
        for (int i = 0; i < size - 1; i++) {
            prev = prev.next;
        }
        Node<E> removed = prev.next;
        prev.next = null;
        size--;
        return removed.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Node<E> cur = head.next;
        while (cur != null) {
            sb.append(cur.data).append(" ");
            cur = cur.next;
        }
        return sb.toString();
    }
    
    public void add(E data) { 
        insert(data, 0);
    }

    public void update(int index, E data){
        if (index >= size || index < 0) {
            throw new IllegalArgumentException("链表下标越界:" + index);
        }
        Node<E> cur = head;
        for (int i = 0; i < index; i++) {
            cur = cur.next;
        }
        cur.next.data=data;
    }

    public void addLast(E data) {
        insert(data, size);
    }

    public E getFirst() {
        if (isEmpty()) {
            return null;
        }
        return get(0);
    }

    public E getLast() {
        if (isEmpty()) {
            return null;
        }
        return get(size - 1);
    }

    public boolean contains(E data) {
        return indexOf(data) != -1;
    }

    public int indexOf(E data) {
        Node<E> cur = head.next;
        int index = 0;
        while (cur != null) {
            if (data == null ? cur.data == null : data.equals(cur.data)) {
                return index;
            }
            cur = cur.next;
            index++;
        }
        return -1;
    }

    public E removeFirst() {
        return remove(0);
    }

    public boolean remove(E data) {
        Node<E> prev = head;
        Node<E> cur = head.next;
        int index = 0;
        while (cur != null) {
            if (data == null ? cur.data == null : data.equals(cur.data)) {
                prev.next = cur.next;
                cur.next = null;
                size--;
                return true;
            }
            prev = cur;
            cur = cur.next;
            index++;
        }
        return false;
    }

    public void clear() {
        head.next = null;
        size = 0;
    }
    
    public static void main(String[] args) {
        MyLinkedList<Integer> list = new MyLinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        System.out.println(list);
        System.out.println(list.remove(2));
        System.out.println(list);
        System.out.println(list.remove(0));
        System.out.println(list);
        list.update(1, 10);
        System.out.println(list);
    }
    
    

}