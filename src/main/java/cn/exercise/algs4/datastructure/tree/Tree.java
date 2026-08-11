package cn.exercise.algs4.datastructure.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Stack;

public class Tree {

    // 静态内部类：多叉树节点定义
    private static class Node {
        Object data;
        List<Node> children;

        public Node(Object data) {
            this.data = data;
            this.children = new ArrayList<>();
        }
    }

    // ====================== 先序遍历 - 递归实现 ======================
    public void preorderTraversalRecursive(Node root) {
        // 递归出口
        if (root == null) {
            return;
        }
        // 1. 先访问当前根节点
        System.out.print(root.data + " ");
        // 2. 递归遍历所有子节点
        if (root.children != null && !root.children.isEmpty()) {
            for (Node child : root.children) {
                preorderTraversalRecursive(child);
            }
        }
    }

    // ====================== 先序遍历 - 栈迭代实现 ======================
    public void preorderTraversalStack(Node root) {
        if (root == null) {
            return;
        }
        Stack<Node> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node node = stack.pop();
            System.out.print(node.data + " ");

            List<Node> children = node.children;
            // 逆序入栈，保证遍历顺序和递归一致
            if (children != null && !children.isEmpty()) {
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
            }
        }
    }

    // ====================== 后序遍历 - 递归实现 ======================
    public void postorderTraversalRecursive(Node root) {
        if (root == null) {
            return;
        }
        // 先递归访问所有子节点
        if (root.children != null && !root.children.isEmpty()) {
            for (Node child : root.children) {
                postorderTraversalRecursive(child);
            }
        }
        // 最后访问当前节点
        System.out.print(root.data + " ");
    }

    // ====================== 后序遍历 - 双栈迭代实现 ======================
    public void postorderTraversalStack(Node root) {
        if (root == null) {
            return;
        }
        Stack<Node> operationStack = new Stack<>();
        Stack<Node> resultStack = new Stack<>();
        operationStack.push(root);

        while (!operationStack.isEmpty()) {
            Node node = operationStack.pop();
            resultStack.push(node);

            List<Node> children = node.children;
            if (children != null && !children.isEmpty()) {
                for (Node child : children) {
                    operationStack.push(child);
                }
            }
        }

        // 依次输出结果栈
        while (!resultStack.isEmpty()) {
            System.out.print(resultStack.pop().data + " ");
        }
    }

    // ====================== 广度优先（层序BFS）遍历 - 队列实现 ======================
    public void breadthFirstTraversal(Node root) {
        if (root == null) {
            return;
        }
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            System.out.print(node.data + " ");

            if (node.children != null && !node.children.isEmpty()) {
                for (Node child : node.children) {
                    queue.offer(child);
                }
            }
        }
    }

    // ============ 测试主方法 ============
    public static void main(String[] args) {
        // 构建课本里的树：A(B(E,F), C, D(G))
        Node A = new Node("A");
        Node B = new Node("B");
        Node C = new Node("C");
        Node D = new Node("D");
        Node E = new Node("E");
        Node F = new Node("F");
        Node G = new Node("G");

        A.children.add(B);
        A.children.add(C);
        A.children.add(D);
        B.children.add(E);
        B.children.add(F);
        D.children.add(G);

        Tree tree = new Tree();

        System.out.println("先序递归：");
        tree.preorderTraversalRecursive(A);
        System.out.println("\n先序栈迭代：");
        tree.preorderTraversalStack(A);

        System.out.println("\n\n后序递归：");
        tree.postorderTraversalRecursive(A);
        System.out.println("\n后序双栈迭代：");
        tree.postorderTraversalStack(A);

        System.out.println("\n\n层序BFS遍历：");
        tree.breadthFirstTraversal(A);
    }
}
