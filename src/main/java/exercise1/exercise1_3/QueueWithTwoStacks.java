package exercise1.exercise1_3;

public class QueueWithTwoStacks<Item> {
    private Stack1<Item> inStack = new Stack1<>();
    private Stack1<Item> outStack = new Stack1<>();

    public void enqueue(Item item) {
        inStack.push(item);
    }

    public Item dequeue() {
        if (outStack.isEmpty()) {
            while (!inStack.isEmpty()) {
                outStack.push(inStack.pop());
            }
        }
        if (outStack.isEmpty()) {
            throw new RuntimeException("Queue underflow");
        }
        return outStack.pop();
    }

    public boolean isEmpty() {
        return inStack.isEmpty() && outStack.isEmpty();
    }
}