package exercise1.exercise1_4;

import exercise1.exercise1_3.Stack;
import exercise1.exercise1_3.Steque;
import exercise1.exercise1_3.Steque1;

import java.util.NoSuchElementException;

public class DequeWithStackAndSteque<T> {
    private Steque<T> steque = new Steque1<>();
    private Stack<T> stack = new Stack<>();

    public void addFirst(T item) {
        steque.pushLeft(item);
    }

    public void addLast(T item) {
        steque.pushRight(item);
    }

    public T removeFirst() {
        if (isEmpty()) throw new NoSuchElementException();
        return steque.popLeft();
    }

    public T removeLast() {
        if (isEmpty()) throw new NoSuchElementException();
        // 将 steque 中除最后一个元素外全部移到 stack 中
        while (steque.size() > 1) {
            stack.push(steque.popLeft());
        }
        // 取出最后一个元素
        T last = steque.popLeft();
        // 将 stack 中的元素移回 steque
        while (!stack.isEmpty()) {
            steque.pushLeft(stack.pop());
        }
        return last;
    }

    public boolean isEmpty() {
        return steque.isEmpty();
    }

    public int size() {
        return steque.size();
    }
}