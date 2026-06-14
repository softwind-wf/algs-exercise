package exercise1.exercise1_3;

public interface Steque<T> {
    // 在栈顶（头部）添加元素
    void push(T item);
    // 从栈顶（头部）移除并返回元素
    T pop();
    // 在队列尾部添加元素
    void enqueue(T item);
    // 判断是否为空
    boolean isEmpty();
    // 返回元素个数
    int size();

    void pushLeft(T item);

    void pushRight(T item);

    T popLeft();
}