package cn.exercise.algs4.datastructure.threadedbinarytree;

import java.util.Stack;

/**
 * Chapter6_Tree
 * com.ds.threadedbinarytree
 * ThreadedBinaryTree.java
 * 线索二叉树结构 —— 支持先序线索化及线索化后的遍历
 */
public class ThreadedBinaryTree {

    // 线索二叉树节点
    public static class Node {
        Object data;            // 数据域

        boolean lThread;        // true = lChild 指向的是前驱（线索），false = lChild 是真实左子
        Node lChild;            // 左子指针域

        boolean rThread;        // true = rChild 指向的是后继（线索），false = rChild 是真实右子
        Node rChild;            // 右子指针域

        Node parent;            // 父节点（后序线索遍历免栈必需，先序/中序不需要）

        public Node(Object data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return String.valueOf(data);
        }
    }

    /**
     * 便捷设置左子关系（自动设置 parent）
     */
    public static void setLeft(Node parent, Node leftChild) {
        parent.lChild = leftChild;
        if (leftChild != null) leftChild.parent = parent;
    }

    /**
     * 便捷设置右子关系（自动设置 parent）
     */
    public static void setRight(Node parent, Node rightChild) {
        parent.rChild = rightChild;
        if (rightChild != null) rightChild.parent = parent;
    }

    // 用于通过递归实现二叉树线索化过程中保存前驱节点的全局变量
    private Node pre = null;

    /**
     * 先序线索化递归方法
     *
     * 先序遍历顺序：根 → 左子树 → 右子树
     * 线索规则：
     *   左子为 null → lThread=true，指向先序前驱
     *   右子为 null → rThread=true，指向先序后继
     *
     * 调用前必须将 pre 置为 null（可在外部 reset()）
     */
    public void preorderThreadingRecursive(Node root) {
        if(root == null) {
            return;
        }

        // 如果当前节点的左子节点为空，则左子指针域线索化指向前驱节点
        if(root.lChild == null) {
            root.lThread = true;
            root.lChild = pre;
        }

        /*
        如果当前节点的前驱节点存在且前驱节点的右子节点为空
        则前驱节点的右子指针域线索化指向当前节点
        */
        if(pre != null && pre.rChild == null) {
            pre.rThread = true;
            pre.rChild = root;
        }

        //将当前节点更新为下一节点的前驱节点
        pre = root;

        if(!root.lThread) {
            preorderThreadingRecursive(root.lChild);
        }

        if(!root.rThread) {
            preorderThreadingRecursive(root.rChild);
        }
    }


    /**
     * 先序线索化（栈实现，非递归版）
     *
     * 与递归版逻辑完全等价，但用显式栈避免递归深度限制。
     *
     * ⚠️ 注意：压入子节点时必须判断 lThread/rThread，
     *    不能用 lChild != null 替代，否则线索指针会被误当作真实子节点重新入栈导致死循环。
     */
    public void preorderThreadingStack(Node root) {
        if(root == null) {
            return;
        }

        //用于保存前驱节点的局部变量
        Node pre = null;

        Stack<Node> stack = new Stack<>();
        stack.push(root);

        while(!stack.isEmpty()) {
            Node node = stack.pop();

            // 1) 当前节点左子为空 → 线索化指向前驱
            if(node.lChild == null) {
                node.lThread = true;
                node.lChild = pre;
            }

            // 2) 前驱节点右子为空 → 线索化指向当前节点
            if(pre != null && pre.rChild == null) {
                pre.rThread = true;
                pre.rChild = node;
            }

            // 3) 更新前驱
            pre = node;

            // 4) 压入子节点（栈 LIFO，先压右再压左，保证左先出）
            if(node.rChild != null) {
                stack.push(node.rChild);
            }

            // ★ 必须用 !node.lThread 而非 node.lChild != null
            //    因为 lChild 可能已在上方被改写为线程指针
            if(!node.lThread) {
                stack.push(node.lChild);
            }
        }
    }

    /**
     * 二叉树中序线索化 递归实现
     */
    public void inorderThreadingRecursive(Node root) {
        if (root == null) {
            return;
        }

        //中序先递归左子树，中序线索化去掉了 !root.lThread 判断
        inorderThreadingRecursive(root.lChild);

        //当前节点左孩子为空，左指针做线索，指向前驱
        if (root.lChild == null) {
            root.lThread = true;
            root.lChild = pre;
        }

        //前驱右孩子为空，前驱右指针做线索，指向当前节点
        if (pre != null && pre.rChild == null) {
            pre.rThread = true;
            pre.rChild = root;
        }

        //更新前驱为当前节点
        pre = root;

        //递归右子树
        inorderThreadingRecursive(root.rChild);
    }

    /**
     * * 二叉树中序线索化 栈非递归实现
     */
    public void inorderThreadingStack(Node root) {
        if (root == null) {
            return;
        }

        Node pre = null;
        Stack<Node> stack = new Stack<>();
        Node node = root;

        while (node != null || !stack.isEmpty()) {
            //一路往左压栈
            while (node != null) {
                stack.push(node);
                node = node.lChild;
            }

            node = stack.pop();

            //左空则加左线索
            if (node.lChild == null) {
                node.lThread = true;
                node.lChild = pre;
            }

            //前驱右空则加右线索
            if (pre != null && pre.rChild == null) {
                pre.rThread = true;
                pre.rChild = node;
            }

            pre = node;
            //转向右子树
            node = node.rChild;
        }
    }

    //==================== 后序线索化（递归版）====================
    public void postorderThreadingRecursive(Node root) {
        if(root == null) {
            return;
        }

        //递归左子树
        postorderThreadingRecursive(root.lChild);
        //递归右子树
        postorderThreadingRecursive(root.rChild);

        //左孩子为空，建立左线索
        if(root.lChild == null) {
            root.lThread = true;
            root.lChild = pre;
        }

        //前驱右孩子为空，给前驱建立右线索
        if(pre != null && pre.rChild == null) {
            pre.rThread = true;
            pre.rChild = root;
        }

        //更新前驱
        pre = root;
    }

    //==================== 后序线索化（栈非递归版）====================
    public void postorderThreadingStack(Node root) {
        if(root == null) {
            return;
        }

        Stack<Node> stack = new Stack<>();
        Node pre = null;
        Node node = root;

        while(node != null || !stack.isEmpty()) {
            while(node != null) {
                stack.push(node);
                node = node.lChild;
            }

            node = stack.peek();
            //判断右子树是否已经访问过
            if(node.rChild == null || node.rChild == pre) {
                node = stack.pop();

                //左空，加左线索
                if(node.lChild == null) {
                    node.lThread = true;
                    node.lChild = pre;
                }

                if(pre != null && pre.rChild == null) {
                    pre.rThread = true;
                    pre.rChild = node;
                }

                pre = node;
                node = null;
            } else {
                node = node.rChild;
            }
        }
    }

    // ====================== 辅助方法 ======================

    /**
     * 重置线索化状态（每次调用线索化前执行）
     */
    public void resetPre() {
        pre = null;
    }

    /**
     * 先序遍历线索二叉树（利用线索，非递归 O(n) 时间复杂度）
     *
     * 核心思路：
     *   处理当前节点后，
     *     如果有左子（!lThread）→ 向左走
     *     否则 → 沿右指针走（可能是线索后继，也可以是右子）
     */
    public static void traversePreorder(Node root) {
        if (root == null) return;

        Node current = root;
        while (current != null) {
            System.out.print(current.data + " ");

            if (!current.lThread) {
                // 有真实的左子 → 向左
                current = current.lChild;
            } else {
                // 左指针是线索（或左子为空），走右指针（线索后继 或 右子）
                current = current.rChild;
            }
        }
    }

    /**
     * 递归先序遍历（忽略线索，只走真实子节点）
     */
    public static void traversePreorderRecursive(Node root) {
        if (root == null) return;
        System.out.print(root.data + " ");
        if (!root.lThread) traversePreorderRecursive(root.lChild);
        if (!root.rThread) traversePreorderRecursive(root.rChild);
    }

    //中序线索遍历（利用线索，非递归 O(n) 时间复杂度）
    //对于中序线索二叉树执行中序遍历的方法
    public void inorderThreadedTraversal(Node root) {
        if(root == null) {
            return;
        }

        //表示正在处理的当前节点的变量
        Node cur = root;

        while(cur != null) {
            /*
             * 如果当前节点存在左子树，则找到左子树中最左下的节点
             * 该节点为对以当前节点为根的(子)树进行中序遍历的起始节点
             */
            while(!cur.lThread && cur.lChild != null) {
                cur = cur.lChild;
            }

            //找到当前(子)树中序遍历的起点后对起点节点进行处理
            System.out.print(cur.data + ", ");

            /*
             * 若中序遍历的起点节点不存在右子树，则沿起点节点的后继线索回退到起点节点的父节点，并对其父节点进行处理
             * 如果其父节点同样不存在右子树，则循环执行这一步骤沿后继线索回退，直到某一父节点存在右子树为止
             */
            while(cur.rThread && cur.rChild != null) {
                cur = cur.rChild;
                System.out.print(cur.data + ", ");
            }

            //回退到存在右子树的节点，对其右子树重复上述完整流程
            cur = cur.rChild;
        }
    }



    //==================== 后序线索遍历（无栈版，需要 parent 指针）====================

    /**
     * 后序遍历后序线索二叉树 —— 纯线索版，零额外空间（无需栈，仅需 parent 指针）。
     *
     * <p>算法：</p>
     * <ol>
     *   <li>找后序第一个节点：一直走真实左子，若无左子则走真实右子，直到两者皆无线索 = 叶子</li>
     *   <li>寻后继：
     *     <ul>
     *       <li>{@code rThread == true} → 直接沿线索到后继（O(1)）</li>
     *       <li>{@code rThread == false}（有真实右子）→ 沿 {@code parent} 回退：
     *         <ul>
     *           <li>若是<em>左子</em>且父节点有真实右子 → 进父节点右子树找第一个后序节点</li>
     *           <li>否则（右子 或 父无右子）→ 后继就是父节点</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     * </ol>
     */
    public static void postorderThreadedTraversal(Node root) {
        if (root == null) return;

        // 1) 找后序第一个节点：最左最下的叶子
        Node current = firstPostorderNode(root);

        // 2) 沿后继遍历
        while (current != null) {
            System.out.print(current.data + " ");
            current = postorderSuccessor(current);
        }
    }

    /**
     * 找以 node 为根的子树中后序第一个节点
     * （一直向左，无左则向右，直到叶子）
     */
    private static Node firstPostorderNode(Node node) {
        while (true) {
            while (!node.lThread) {
                node = node.lChild;
            }
            // 有真实右子才向右走，否则（rThread=true 或 rChild==null）到达叶子
            if (!node.rThread && node.rChild != null) {
                node = node.rChild;
            } else {
                break;
            }
        }
        return node;
    }

    /**
     * 找后序后继（纯线索驱动，无栈）
     *
     * 核心规则：
     * - rThread=true  → 直接沿线索
     * - rThread=false → 有真实右子，说明当前节点是其父节点的左子，
     *                   且父节点有右子树 → 进右子树找第一个后序节点
     */
    private static Node postorderSuccessor(Node node) {
        // 情况 1：右指针是线索 → 直达后继
        if (node.rThread) {
            return node.rChild;
        }

        // 情况 2：有真实右子 → 需要 parent 回退
        Node parent = node.parent;
        if (parent == null) {
            return null; // 根节点，没有后继
        }

        // 如果是父节点的真实左子（!lThread）且父节点有真实右子
        // → 进入父节点的右子树，找第一个后序节点
        // ★ 必须加 !parent.lThread 判断：线索化可能将 parent.lChild 改写为线程指针，
        //   若不加此判断，右斜树中 A.lThread=true→B（线索）会被误认为 A 有左子 B
        if (!parent.lThread && parent.lChild == node
                && !parent.rThread && parent.rChild != null) {
            return firstPostorderNode(parent.rChild);
        }

        // 否则（右子、或父无右子树），后继就是父节点
        return parent;
    }

    /**
     * 后序线索遍历 —— 带栈版本（兼容无 parent 指针的情况）
     *
     * 线程安全，但违背线索二叉树免栈的初衷，仅作对比参考。
     */
    public static void postorderThreadedTraversalWithStack(Node root) {
        if (root == null) return;

        Stack<Node> stack = new Stack<>();
        Node current = root;
        Node lastVisited = null;

        while (!stack.isEmpty() || current != null) {
            if (current != null) {
                stack.push(current);
                current = current.lThread ? null : current.lChild;
            } else {
                Node node = stack.peek();
                if (!node.rThread && node.rChild != null && node.rChild != lastVisited) {
                    current = node.rChild;
                } else {
                    System.out.print(node.data + " ");
                    lastVisited = stack.pop();
                }
            }
        }
    }

    /**
     * 递归后序遍历（忽略线索，只走真实子节点）
     */
    public static void traversePostorderRecursive(Node root) {
        if (root == null) return;
        if (!root.lThread) traversePostorderRecursive(root.lChild);
        if (!root.rThread) traversePostorderRecursive(root.rChild);
        System.out.print(root.data + " ");
    }


    /**
     * 以易于阅读的形式打印树结构（含线索标记）
     */
    public static void printTree(Node node, String prefix, boolean isTail) {
        if (node == null) return;

        String threadInfo = "";
        if (node.lThread) threadInfo += " L↑";
        if (node.rThread && node.rChild == null) threadInfo += " R→null";
        else if (node.rThread) threadInfo += " R→" + node.rChild.data;

        System.out.println(prefix + (isTail ? "└── " : "├── ")
                + node.data + threadInfo);

        if (node.lThread && node.rThread) return; // 叶子，线索已标

        String childPrefix = prefix + (isTail ? "    " : "│   ");
        boolean hasRight = !node.rThread && node.rChild != null;

        // 优先打印左子（真实左子优先于线索左子）
        if (!node.lThread && node.lChild != null) {
            printTree(node.lChild, childPrefix, !hasRight);
        }
        if (!node.rThread && node.rChild != null) {
            printTree(node.rChild, childPrefix, true);
        }
    }

    // ====================== main — 测试验证 ======================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   先序线索二叉树 — 测试演示        ║");
        System.out.println("╚══════════════════════════════════════╝");

        // ---------- 测试 1：标准完全二叉树 ----------
        System.out.println("\n【测试 1】标准二叉树（4层）");
        /*
                  A
                /   \
               B     C
              / \   / \
             D   E F   G
        */
        Node n1_A = new Node("A");
        Node n1_B = new Node("B");
        Node n1_C = new Node("C");
        Node n1_D = new Node("D");
        Node n1_E = new Node("E");
        Node n1_F = new Node("F");
        Node n1_G = new Node("G");

        setLeft(n1_A, n1_B); setRight(n1_A, n1_C);
        setLeft(n1_B, n1_D); setRight(n1_B, n1_E);
        setLeft(n1_C, n1_F); setRight(n1_C, n1_G);

        testThreading("标准二叉树", n1_A);

        // ---------- 测试 2：左斜树 ----------
        System.out.println("\n【测试 2】左斜树");
        /*
                A
               /
              B
             /
            C
        */
        Node n2_A = new Node("A");
        Node n2_B = new Node("B");
        Node n2_C = new Node("C");
        setLeft(n2_A, n2_B);
        setLeft(n2_B, n2_C);

        testThreading("左斜树", n2_A);

        // ---------- 测试 3：右斜树 ----------
        System.out.println("\n【测试 3】右斜树");
        /*
            A
             \
              B
               \
                C
        */
        Node n3_A = new Node("A");
        Node n3_B = new Node("B");
        Node n3_C = new Node("C");
        setRight(n3_A, n3_B);
        setRight(n3_B, n3_C);

        testThreading("右斜树", n3_A);

        // ---------- 测试 4：单节点 ----------
        System.out.println("\n【测试 4】单节点");
        Node n4_A = new Node("A");
        testThreading("单节点", n4_A);

        // ---------- 测试 5：空树 ----------
        System.out.println("\n【测试 5】空树");
        testThreading("空树", null);

        // ---------- 测试 6：非满二叉树 ----------
        System.out.println("\n【测试 6】非满二叉树（缺右子）");
        /*
                A
               / \
              B   C
             /
            D
        */
        Node n6_A = new Node("A");
        Node n6_B = new Node("B");
        Node n6_C = new Node("C");
        Node n6_D = new Node("D");
        setLeft(n6_A, n6_B); setRight(n6_A, n6_C);
        setLeft(n6_B, n6_D);

        testThreading("缺右子树", n6_A);

        // ====================== 后序线索化与遍历测试 ======================
        System.out.println("\n\n╔══════════════════════════════════════╗");
        System.out.println("║   后序线索二叉树 — 遍历测试         ║");
        System.out.println("╚══════════════════════════════════════╝");

        // 重新创建树（前续测试已线索化过，需新节点）
        Node p1_A = new Node("A");
        Node p1_B = new Node("B");
        Node p1_C = new Node("C");
        Node p1_D = new Node("D");
        Node p1_E = new Node("E");
        Node p1_F = new Node("F");
        Node p1_G = new Node("G");
        setLeft(p1_A, p1_B); setRight(p1_A, p1_C);
        setLeft(p1_B, p1_D); setRight(p1_B, p1_E);
        setLeft(p1_C, p1_F); setRight(p1_C, p1_G);

        testPostorderThreading("标准二叉树", p1_A);

        Node p2_A = new Node("A");
        Node p2_B = new Node("B");
        Node p2_C = new Node("C");
        setLeft(p2_A, p2_B);
        setLeft(p2_B, p2_C);
        testPostorderThreading("左斜树", p2_A);

        Node p3_A = new Node("A");
        Node p3_B = new Node("B");
        Node p3_C = new Node("C");
        setRight(p3_A, p3_B);
        setRight(p3_B, p3_C);
        testPostorderThreading("右斜树", p3_A);

        Node p4_A = new Node("A");
        testPostorderThreading("单节点", p4_A);

        testPostorderThreading("空树", null);

        Node p6_A = new Node("A");
        Node p6_B = new Node("B");
        Node p6_C = new Node("C");
        Node p6_D = new Node("D");
        setLeft(p6_A, p6_B); setRight(p6_A, p6_C);
        setLeft(p6_B, p6_D);
        testPostorderThreading("缺右子树", p6_A);
    }

    /**
     * 测试线索化的辅助方法
     */
    private static void testThreading(String label, Node root) {
        ThreadedBinaryTree tbt = new ThreadedBinaryTree();

        // 先打印原始树的递归先序遍历
        System.out.print("  原始先序（递归）: ");
        traversePreorderRecursive(root);
        System.out.println();

        // 执行先序线索化
        tbt.resetPre();
        tbt.preorderThreadingStack(root);

        // 打印线索标记的树结构
        System.out.println("  线索化树结构（L↑=左线索, R→X=右线索指向X）:");
        printTree(root, "  ", true);

        // 利用线索进行遍历
        System.out.print("  线索遍历（非递归）: ");
        traversePreorder(root);
        System.out.println();

        // 验证一致性：比较两种遍历结果
        java.util.List<String> origList = new java.util.ArrayList<>();
        java.util.List<String> threadedList = new java.util.ArrayList<>();

        // 收集递归遍历结果
        collectRecursive(root, origList);
        // 收集线索遍历结果
        Node c = root;
        while (c != null) {
            threadedList.add(String.valueOf(c.data));
            if (!c.lThread) c = c.lChild;
            else c = c.rChild;
        }

        boolean match = origList.equals(threadedList);
        System.out.println("  一致性验证: " + (match ? "✅ 通过" : "❌ 失败"));
        if (!match) {
            System.out.println("    期望: " + origList);
            System.out.println("    实际: " + threadedList);
        }

        // 打印线索细节
        printThreadingDetail(root);
    }

    /**
     * 递归收集先序遍历结果（仅走真实子节点，忽略线索）
     */
    private static void collectRecursive(Node node, java.util.List<String> list) {
        if (node == null) return;
        list.add(String.valueOf(node.data));
        if (!node.lThread) collectRecursive(node.lChild, list);
        if (!node.rThread) collectRecursive(node.rChild, list);
    }

    /**
     * 递归收集后序遍历结果（仅走真实子节点，忽略线索）
     */
    private static void collectPostorderRecursive(Node node, java.util.List<String> list) {
        if (node == null) return;
        if (!node.lThread) collectPostorderRecursive(node.lChild, list);
        if (!node.rThread) collectPostorderRecursive(node.rChild, list);
        list.add(String.valueOf(node.data));
    }

    /**
     * 后序线索化 + 遍历的一致性测试
     */
    private static void testPostorderThreading(String label, Node root) {
        ThreadedBinaryTree tbt = new ThreadedBinaryTree();

        // 后序线索化
        tbt.resetPre();
        tbt.postorderThreadingStack(root);

        // 打印线索树结构
        System.out.println("\n" + label + " 线索化结构（L↑=左线索, R→X=右线索）:");
        printTree(root, "  ", true);

        // 递归后序遍历（参考结果）
        System.out.print("  递归后序: ");
        traversePostorderRecursive(root);
        System.out.println();

        // 线索后序遍历（被测）
        System.out.print("  线索后序: ");
        postorderThreadedTraversal(root);
        System.out.println();

        // 验证一致性
        java.util.List<String> refList = new java.util.ArrayList<>();
        java.util.List<String> travList = new java.util.ArrayList<>();
        collectPostorderRecursive(root, refList);

        // 收集线索遍历结果（与 postorderThreadedTraversal 同逻辑：无栈，仅 parent + 线索）
        if (root != null) {
            Node c = firstPostorderNode(root);
            while (c != null) {
                travList.add(String.valueOf(c.data));
                c = postorderSuccessor(c);
            }
        }

        boolean match = refList.equals(travList);
        System.out.println("  一致性验证: " + (match ? "✅ 通过" : "❌ 失败"));
        if (!match) {
            System.out.println("    期望: " + refList);
            System.out.println("    实际: " + travList);
        }

        printThreadingDetail(root);
    }

    /**
     * 打印每个节点的线索信息
     */
    private static void printThreadingDetail(Node root) {
        if (root == null) return;
        System.out.println("  线索明细:");
        printDetailRecursive(root, "    ");
    }

    private static void printDetailRecursive(Node node, String indent) {
        if (node == null) return;

        System.out.print(indent + node.data + ": ");
        if (node.lThread) {
            System.out.print("左线索→" + (node.lChild == null ? "null" : node.lChild.data) + " ");
        } else {
            System.out.print("左子=" + (node.lChild == null ? "null" : node.lChild.data) + " ");
        }
        if (node.rThread) {
            System.out.print("右线索→" + (node.rChild == null ? "null" : node.rChild.data));
        } else {
            System.out.print("右子=" + (node.rChild == null ? "null" : node.rChild.data));
        }
        System.out.println();

        // 只递归打印真实子节点
        if (!node.lThread) printDetailRecursive(node.lChild, indent);
        if (!node.rThread) printDetailRecursive(node.rChild, indent);
    }
}
