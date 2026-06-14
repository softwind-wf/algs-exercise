package exercise1.exercise1_3;

public class TwoStacksInDeque<Item> {
    private final Deque<Item> deque;
    private int sizeA; // 栈A（左端）的大小
    private int sizeB; // 栈B（右端）的大小

    public TwoStacksInDeque() {
        deque = new Deque<>(); // 假设这是你实现的双向队列
        sizeA = 0;
        sizeB = 0;
    }

    // 栈A操作
    public void pushA(Item item) {
        deque.pushLeft(item);
        sizeA++;
    }

    public Item popA() {
        if (sizeA == 0) {
            throw new RuntimeException("栈A为空");
        }
        Item item = deque.popLeft();
        sizeA--;
        return item;
    }

    public boolean isEmptyA() {
        return sizeA == 0;
    }

    public int sizeA() {
        return sizeA;
    }

    // 栈B操作
    public void pushB(Item item) {
        deque.pushRight(item);
        sizeB++;
    }

    public Item popB() {
        if (sizeB == 0) {
            throw new RuntimeException("栈B为空");
        }
        Item item = deque.popRight();
        sizeB--;
        return item;
    }

    public boolean isEmptyB() {
        return sizeB == 0;
    }

    public int sizeB() {
        return sizeB;
    }
}