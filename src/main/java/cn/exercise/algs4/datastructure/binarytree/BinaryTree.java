package cn.exercise.algs4.datastructure.binarytree;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * Chapter6_Tree
 * com.ds.binarytree
 * Node.java
 * 二叉树的节点代码定义
 */
public class BinaryTree {
    // 通过静态内部类定义二叉树的节点类型
    private static class Node {
        Object data;        // 数据域
        Node lChild;       // 左子节点指针域
        Node rChild;       // 右子节点指针域

        public Node(Object data) {
            this.data = data;
            this.lChild = null;
            this.rChild = null;
        }
    }

    //===================== 一、递归遍历：先序、中序、后序 =====================
    /**
     * 先序遍历（递归）根 -> 左 -> 右
     */
    public void preorderTraversalRecursive(Node root) {
        // 递归出口
        if (root == null) {
            return;
        }
        //1. 优先遍历根节点
        System.out.print(root.data + " ");
        //2. 然后递归遍历左子树
        preorderTraversalRecursive(root.lChild);
        //3. 最后递归遍历右子树
        preorderTraversalRecursive(root.rChild);
    }

    /**
     * 中序遍历（递归）左 -> 根 -> 右
     */
    public void inorderTraversalRecursive(Node root) {
        //递归出口
        if (root == null) {
            return;
        }
        //1.优先递归遍历左子树
        inorderTraversalRecursive(root.lChild);
        //2.然后遍历根节点
        System.out.print(root.data + " ");
        //3.最后递归遍历右子树
        inorderTraversalRecursive(root.rChild);
    }

    /**
     * 后序遍历（递归）左 -> 右 -> 根
     */
    public void postorderTraversalRecursive(Node root) {
        //递归出口
        if (root == null) {
            return;
        }
        //1.优先递归遍历左子树
        postorderTraversalRecursive(root.lChild);
        //2.然后递归遍历右子树
        postorderTraversalRecursive(root.rChild);
        //3.最后遍历根节点
        System.out.print(root.data + " ");
    }

    //===================== 二、迭代遍历（栈实现） =====================


    /**
     * 先序遍历 栈迭代实现
     */
    public void preorderTraversalStack(Node root) {
        //防止空树的情况
        if (root == null) {
            return;
        }
        //创建先序遍历使用的栈结构
        Stack<Node> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            System.out.print(node.data + " ");
            //先压右孩子，再压左孩子（栈后进先出）
            if(node.rChild != null){
                stack.push(node.rChild);
            }
            if(node.lChild != null){
                stack.push(node.lChild);
            }
        }
    }

    /**
     * 中序遍历 栈迭代实现
     */
    public void inorderTraversalStack(Node root) {
        if (root == null) {
            return;
        }
        Stack<Node> stack = new Stack<>();
        Node node = root;
        while (node != null || !stack.isEmpty()) {
            //一路向左，把左链全部入栈
            while (node != null) {
                stack.push(node);
                node = node.lChild;
            }
            //弹出节点，访问
            node = stack.pop();
            System.out.print(node.data + " ");
            //转向右子树
            node = node.rChild;
        }
    }

    /**
     * 后序遍历 栈迭代实现
     */
    public void postorderTraversalStack(Node root) {
        if (root == null) {
            return;
        }
        Stack<Node> stack = new Stack<>();
        Node node = root;
        Node lastVisitNode = null;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.lChild;
            }
            //查看栈顶元素，不弹出
            node = stack.peek();
            //右孩子为空 或者 右孩子已经访问过，则访问当前节点
            if (node.rChild == null || node.rChild == lastVisitNode) {
                stack.pop();
                System.out.print(node.data + " ");
                lastVisitNode = node;
                node = null;
            } else {
                node = node.rChild;
            }
        }
    }

    //===================== 三、层序遍历（队列实现） =====================


    /**
     * 广度优先（层序）遍历
     */
    public void breadthFirstTraversal(Node root) {
        //防止空树的情况
        if (root == null) {
            return;
        }
        //创建用于广度优先遍历的队列
        Queue<Node> queue = new LinkedList<>();
        //算法开始前，将树根加入队列
        queue.offer(root);
        //如果队列为空，则广度遍历结束
        while (!queue.isEmpty()) {
            //3.将队列头节点出队列，对该节点进行操作
            Node node = queue.poll();
            System.out.print(node.data + " ");
            //4.若该节点有左子，将左子加入队列
            if (node.lChild != null) {
                queue.offer(node.lChild);
            }
            //5.若该节点有右子，将右子加入队列
            if (node.rChild != null) {
                queue.offer(node.rChild);
            }
        }
    }

    //===================== 四、根据【先序+中序】构建二叉树 =====================
    /**
     * 通过先序序列+中序序列构建二叉树的方法
     * @param preorder 先序数组
     * @param inorder 中序数组
     * @return 根节点
     */
    public Node buildByPreAndInOrder(Node[] preorder, Node[] inorder) {
        //判空
        if (preorder == null || inorder == null
                || preorder.length == 0 || inorder.length == 0) {
            return null;
        }
        if (preorder.length != inorder.length) {
            return null;
        }
        //调用递归内层方法
        return buildByPreAndInOrderInner(preorder, inorder,
                0, preorder.length - 1,
                0, inorder.length - 1);
    }

    /**
     * 递归内层函数
     */
    private Node buildByPreAndInOrderInner(Node[] preorder, Node[] inorder,
                                            int preStart, int preEnd,
                                            int inStart, int inEnd) {
        //递归终止条件
        if (preStart > preEnd || inStart > inEnd) {
            return null;
        }
        //在先序序列中确定当前子树的根节点
        Node rootVal = preorder[preStart];
        Node root = new Node(rootVal.data);

        //在中序里找到根的下标
        int rootIndex = findRootIndexInInorder(rootVal, inorder, inStart, inEnd);

        //递归构建左子树
        root.lChild = buildByPreAndInOrderInner(preorder, inorder,
                preStart + 1, preStart + rootIndex - inStart,
                inStart, rootIndex - 1);
        //递归构建右子树
        root.rChild = buildByPreAndInOrderInner(preorder, inorder,
                preStart + rootIndex - inStart + 1, preEnd,
                rootIndex + 1, inEnd);
        return root;
    }

    /**
     * 在中序序列的指定范围内查找根节点下标
     */
    private int findRootIndexInInorder(Node rootVal, Node[] inorder, int inStart, int inEnd) {
        for (int i = inStart; i <= inEnd; i++) {
            if (rootVal.data.equals(inorder[i].data)) {
                return i;
            }
        }
        return -1;
    }

    //===================== 五、根据【后序+中序】构建二叉树 =====================
    /**
     * 通过后序序列+中序序列构建二叉树
     * @param postorder 后序数组
     * @param inorder 中序数组
     * @return 树根
     */
    public Node buildByPostAndInOrder(Node[] postorder, Node[] inorder) {
        //判空
        if (postorder == null || inorder == null
                || postorder.length == 0 || inorder.length == 0) {
            return null;
        }
        if (postorder.length != inorder.length) {
            return null;
        }
        return buildByPostAndInOrderInner(postorder, inorder,
                0, postorder.length - 1,
                0, inorder.length - 1);
    }

    private Node buildByPostAndInOrderInner(Node[] postorder, Node[] inorder,
                                            int postStart, int postEnd,
                                            int inStart, int inEnd) {
        if (postStart > postEnd || inStart > inEnd) {
            return null;
        }
        //后序最后一位是根
        Node rootVal = postorder[postEnd];
        Node root = new Node(rootVal.data);

        //中序找下标
        int rootIndex = findRootIndexInInorder(rootVal, inorder, inStart, inEnd);

        //左子树范围
        int leftSize = rootIndex - inStart;
        //递归左、右
        root.lChild = buildByPostAndInOrderInner(postorder, inorder,
                postStart, postStart + leftSize - 1,
                inStart, rootIndex - 1);

        root.rChild = buildByPostAndInOrderInner(postorder, inorder,
                postStart + leftSize, postEnd - 1,
                rootIndex + 1, inEnd);

        return root;
    }

}
