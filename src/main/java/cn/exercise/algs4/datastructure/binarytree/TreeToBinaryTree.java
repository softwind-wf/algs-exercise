package cn.exercise.algs4.datastructure.binarytree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TreeToBinaryTree {

    static class TreeNode {
        Object data;
        List<TreeNode> children;
    }
    static class BinaryTreeNode {
        Object data;
        BinaryTreeNode lChild;
        BinaryTreeNode rChild;
    }


    //多叉树转换为二叉树的方法
    public BinaryTreeNode treeToBinaryTree(TreeNode root) {
        if (root == null) {
            return null;
        }

        //将多叉树的节点转换为二叉树的节点
        BinaryTreeNode newNode = new BinaryTreeNode();
        newNode.data = root.data;

        //递归地将多叉树的子节点转换为二叉树节点
        if (!root.children.isEmpty()) {
            //原多叉树节点的第1个子节点为转换后二叉树的左子节点
            newNode.lChild = treeToBinaryTree(root.children.get(0));

            //循环将多叉树的兄弟节点转换为右子树中的节点
            BinaryTreeNode curNode = newNode.lChild;
            for (int i = 1; i < root.children.size(); i++) {
                curNode.rChild = treeToBinaryTree(root.children.get(i));
                curNode = curNode.rChild;
            }
        }

        return newNode;
    }

    //二叉树转多叉树的方法
    public TreeNode binaryTreeToTree(BinaryTreeNode root) {
        if (root == null) {
            return null;
        }

        //将二叉树的树根节点转换为多叉树节点
        TreeNode treeRoot = new TreeNode();
        treeRoot.data = root.data;
        treeRoot.children = new ArrayList<>();

        //递归地对二叉树根的左子树进行转换
        binaryTreeToTreeInner(root.lChild, treeRoot);
        return treeRoot;
    }

    /**
     * 二叉树转多叉树的递归过程
     * btNode 为待转换的二叉树节点
     * parent 为二叉树节点转换成多叉树节点后归属的父级节点
     */
    private void binaryTreeToTreeInner(BinaryTreeNode btNode, TreeNode parent) {
        if (btNode == null) {
            return;
        }

        //创建当前节点对应的多叉树节点，并添加到多叉树的 children 中
        TreeNode treeNode = new TreeNode();
        treeNode.data = btNode.data;
        treeNode.children = new ArrayList<>();
        parent.children.add(treeNode);

        //递归处理左子树和右子树
        binaryTreeToTreeInner(btNode.lChild, treeNode);
        binaryTreeToTreeInner(btNode.rChild, parent);
    }

    //多叉树森林转换为二叉树的方法
    public BinaryTreeNode forestToBinaryTree(TreeNode[] forest) {
        if (forest == null || forest.length == 0) {
            return null;
        }

        //将森林中的第一棵树转换为二叉树
        BinaryTreeNode root = treeToBinaryTree(forest[0]);


        //将森林中后续的所有多叉树分别转换为二叉树,并将后一棵二叉树作为前一棵二叉树树根的右子树

        BinaryTreeNode curNode = root;
        for(int i = 1; i < forest.length; i++) {
            curNode.rChild = treeToBinaryTree(forest[i]);
            curNode = curNode.rChild;
        }

        //返回森林中第一棵树转换得到二叉树的树根
        return root;
    }

    //二叉树转多叉树森林的方法
    public TreeNode[] binaryTreeToForest(BinaryTreeNode root) {
        if(root == null) {
            return null;
        }

        List<TreeNode> forestList = new ArrayList<>();

        // 先转换根节点
        forestList.add(binaryTreeToTree(root));

        // 然后沿着右子树链逐个断开并转换（每棵树的根由右指针串联）
        BinaryTreeNode cur = root.rChild;
        while (cur != null) {
            BinaryTreeNode next = cur.rChild;
            cur.rChild = null;
            forestList.add(binaryTreeToTree(cur));
            cur = next;
        }

        return forestList.toArray(new TreeNode[0]);
    }

    // ==================== 辅助：构建示例多叉树 ====================

    /**
     * <pre>
     *          A
     *      /   |   \
     *     B    C    D
     *    / \       / \
     *   E   F     G   H
     *        \
     *         I
     * </pre>
     */
    private static TreeNode buildSampleTree() {
        TreeNode a = new TreeNode(); a.data = "A";
        TreeNode b = new TreeNode(); b.data = "B";
        TreeNode c = new TreeNode(); c.data = "C";
        TreeNode d = new TreeNode(); d.data = "D";
        TreeNode e = new TreeNode(); e.data = "E";
        TreeNode f = new TreeNode(); f.data = "F";
        TreeNode g = new TreeNode(); g.data = "G";
        TreeNode h = new TreeNode(); h.data = "H";
        TreeNode i = new TreeNode(); i.data = "I";

        a.children = Arrays.asList(b, c, d);
        b.children = Arrays.asList(e, f);
        c.children = new ArrayList<>();
        d.children = Arrays.asList(g, h);
        e.children = new ArrayList<>();
        f.children = Arrays.asList(i);
        g.children = new ArrayList<>();
        h.children = new ArrayList<>();
        i.children = new ArrayList<>();

        return a;
    }

    // ==================== 打印多叉树（深度优先，缩进表示层级） ====================

    private static void printMultiWayTree(TreeNode node, String prefix, boolean isLast) {
        if (node == null) return;

        System.out.println(prefix + (isLast ? "└── " : "├── ") + node.data);

        if (!node.children.isEmpty()) {
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            for (int i = 0; i < node.children.size(); i++) {
                printMultiWayTree(node.children.get(i), childPrefix,
                        i == node.children.size() - 1);
            }
        }
    }

    // ==================== 打印二叉树（树形结构） ====================

    /**
     * 用树形连线打印二叉树，├── 和 └── 区分左/右子树
     *
     * 输出示例：
     *        1
     *       ├── 2
     *       │   ├── 4
     *       │   └── 5
     *       └── 3
     *           └── 6
     */
    private static void printBinaryTree(BinaryTreeNode node, String prefix, boolean isTail) {
        if (node == null) return;

        // 打印当前节点 + 连线
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node.data);

        // 无子节点则停止
        if (node.lChild == null && node.rChild == null) return;

        // 子节点的前缀：当前节点是尾节点（最后一个子节点）时用空格，否则用竖线
        String childPrefix = prefix + (isTail ? "    " : "│   ");

        // 先打印左孩子（如果有右孩子，则左孩子不是最后一个）
        if (node.lChild != null) {
            printBinaryTree(node.lChild, childPrefix, node.rChild == null);
        }
        // 再打印右孩子（右孩子总是最后一个兄弟节点）
        if (node.rChild != null) {
            printBinaryTree(node.rChild, childPrefix, true);
        }
    }

    // ==================== main 实验入口 ====================

    public static void main(String[] args) {
        TreeToBinaryTree converter = new TreeToBinaryTree();

        // ========== 1. 单棵树转换 ==========
        {
            TreeNode root = buildSampleTree();

            System.out.println("========== [测试1] 单棵树转换 ==========");
            System.out.println("\n原始多叉树：");
            printMultiWayTree(root, "", true);

            BinaryTreeNode binaryRoot = converter.treeToBinaryTree(root);

            System.out.println("\n转换后二叉树（左=第一子，右=下一兄弟）：");
            printBinaryTree(binaryRoot, "", true);

            System.out.println("\n逆向转换回多叉树：");
            TreeNode backToTree = converter.binaryTreeToTree(binaryRoot);
            printMultiWayTree(backToTree, "", true);

            boolean match = treesEqual(root, backToTree);
            System.out.println("\n双向转换一致性: " + (match ? "✓ 一致" : "✗ 不一致"));
        }

        // ========== 2. 森林转换 ==========
        {
            System.out.println("\n\n========== [测试2] 森林转换 ==========");

            TreeNode x = new TreeNode(); x.data = "X";
            TreeNode y = new TreeNode(); y.data = "Y";
            TreeNode z = new TreeNode(); z.data = "Z";
            x.children = Arrays.asList(y, z);
            y.children = new ArrayList<>();
            z.children = new ArrayList<>();

            TreeNode p = new TreeNode(); p.data = "P";
            TreeNode q = new TreeNode(); q.data = "Q";
            p.children = Arrays.asList(q);
            q.children = new ArrayList<>();

            TreeNode m = new TreeNode(); m.data = "M";
            TreeNode n = new TreeNode(); n.data = "N";
            TreeNode o = new TreeNode(); o.data = "O";
            m.children = Arrays.asList(n, o);
            n.children = new ArrayList<>();
            o.children = new ArrayList<>();

            TreeNode[] forest = new TreeNode[]{x, p, m};

            System.out.println("森林中的三棵树：X(Y,Z), P(Q), M(N,O)");

            BinaryTreeNode forestRoot = converter.forestToBinaryTree(forest);

            System.out.println("\n森林转换后的二叉树（树根间通过右子树串联）：");
            printBinaryTree(forestRoot, "", true);

            System.out.print("\n先序遍历: ");
            printPreorder(forestRoot);
            System.out.println("  (期望: X Y Z P Q M N O)");
        }

        // ========== 3. 自定义二叉树 → 多叉树 ==========
        {
            System.out.println("\n\n========== [测试3] 自定义二叉树结构 → 多叉树 ==========");
            // 构建一个二叉树（左孩子右兄弟形式）：
            //         A
            //        /
            //       B
            //      / \
            //     C   D
            //          \
            //           E
            // 预期还原为多叉树: A(B(C), D(E)) — 或 A(C, D(E))？
            // 不对，得按左孩子右兄弟规则来解读：
            // A 左=B (第一子), B 左=C (第一子), B 右=D (B的兄弟), D 右=E (D的兄弟)
            // 所以多叉树: A 的子节点 = [B, D]
            //            B 的子节点 = [C]
            //            D 的子节点 = []
            //            C 无子, E 无子
            // 但 D 的右=E，所以 E 是 D 的兄弟 → E 也是 A 的子节点
            // 所以多叉树: A 的子节点 = [B, D, E]
            //            B 的子节点 = [C]

            BinaryTreeNode a = new BinaryTreeNode(); a.data = "A";
            BinaryTreeNode b = new BinaryTreeNode(); b.data = "B";
            BinaryTreeNode c = new BinaryTreeNode(); c.data = "C";
            BinaryTreeNode d = new BinaryTreeNode(); d.data = "D";
            BinaryTreeNode e = new BinaryTreeNode(); e.data = "E";
            a.lChild = b;
            b.lChild = c;
            b.rChild = d;
            d.rChild = e;

            System.out.println("输入二叉树：");
            printBinaryTree(a, "", true);

            TreeNode multiRoot = converter.binaryTreeToTree(a);
            System.out.println("\n还原为多叉树：");
            printMultiWayTree(multiRoot, "", true);

            System.out.print("\n多叉树先序遍历: ");
            printTreePreorder(multiRoot);
            System.out.println("  (期望: A B C D E)");
        }

        // ========== 4. 边界测试 ==========
        {
            System.out.println("\n========== [测试4] 边界测试 ==========");

            TreeNode result = converter.binaryTreeToTree(null);
            System.out.println("null 二叉树 → " + (result == null ? "null ✓" : "非空 ✗"));

            BinaryTreeNode single = new BinaryTreeNode(); single.data = "S";
            TreeNode singleTree = converter.binaryTreeToTree(single);
            System.out.print("单节点 S → ");
            printTreePreorder(singleTree);
            System.out.println("  (期望: S)");
            System.out.println("  子节点数: " + (singleTree.children.isEmpty() ? "0 ✓" : "非空 ✗"));
        }

        // ========== 5. binaryTreeToForest 测试 ==========
        {
            System.out.println("\n========== [测试5] binaryTreeToForest 测试 ==========");

            // --- 5a. 森林 → 二叉树 → 回森林（双向验证）---
            {
                System.out.println("\n--- 5a. 森林 → 二叉树 → 回森林（双向验证）---");

                TreeNode x = new TreeNode(); x.data = "X";
                TreeNode y = new TreeNode(); y.data = "Y";
                TreeNode z = new TreeNode(); z.data = "Z";
                x.children = Arrays.asList(y, z);
                y.children = new ArrayList<>();
                z.children = new ArrayList<>();

                TreeNode p = new TreeNode(); p.data = "P";
                p.children = new ArrayList<>();

                TreeNode m = new TreeNode(); m.data = "M";
                TreeNode n = new TreeNode(); n.data = "N";
                m.children = Arrays.asList(n);
                n.children = new ArrayList<>();

                TreeNode[] originalForest = new TreeNode[]{x, p, m};

                System.out.println("原始森林: X(Y,Z), P, M(N)");

                BinaryTreeNode binRoot = converter.forestToBinaryTree(originalForest);
                System.out.println("\n森林→二叉树:");
                printBinaryTree(binRoot, "", true);

                TreeNode[] restoredForest = converter.binaryTreeToForest(binRoot);
                System.out.println("\n二叉树→回森林 (" + restoredForest.length + " 棵树):");
                for (int i = 0; i < restoredForest.length; i++) {
                    System.out.print("  树" + (i+1) + ": ");
                    printTreePreorder(restoredForest[i]);
                    System.out.println();
                }

                boolean match = (restoredForest.length == originalForest.length);
                for (int i = 0; i < restoredForest.length && match; i++) {
                    match = treesEqual(originalForest[i], restoredForest[i]);
                }
                System.out.println("双向转换一致: " + (match ? "✓ 是" : "✗ 否"));
            }

            // --- 5b. 直接构造带右链的二叉树 → 森林 ---
            {
                System.out.println("\n--- 5b. 直接构造带右链的二叉树 → 森林 ---");
                // 二叉树（根右链 = 森林中的独立树）:
                //        R
                //       / \
                //      A   C
                //     /     \
                //    B       D
                //
                // R.rChild = C (第二棵树), C.rChild = D (第三棵树)
                // 预期森林: R(A(B)), C, D

                BinaryTreeNode r = new BinaryTreeNode(); r.data = "R";
                BinaryTreeNode a = new BinaryTreeNode(); a.data = "A";
                BinaryTreeNode b = new BinaryTreeNode(); b.data = "B";
                BinaryTreeNode c = new BinaryTreeNode(); c.data = "C";
                BinaryTreeNode d = new BinaryTreeNode(); d.data = "D";
                r.lChild = a;
                a.lChild = b;
                r.rChild = c;
                c.rChild = d;

                System.out.println("输入二叉树:");
                printBinaryTree(r, "", true);

                TreeNode[] forest2 = converter.binaryTreeToForest(r);
                System.out.println("\n输出森林 (" + forest2.length + " 棵树):");
                for (int i = 0; i < forest2.length; i++) {
                    System.out.print("  树" + (i+1) + ": ");
                    printTreePreorder(forest2[i]);
                    System.out.println();
                }
                System.out.println("  (期望: 树1=R A B, 树2=C, 树3=D)");
            }

            // --- 5c. 单棵树（没有右孩子）---
            {
                System.out.println("\n--- 5c. 单棵树（无右子树链）---");
                BinaryTreeNode single = new BinaryTreeNode(); single.data = "S";
                BinaryTreeNode s1 = new BinaryTreeNode(); s1.data = "S1";
                single.lChild = s1;

                TreeNode[] forest3 = converter.binaryTreeToForest(single);
                System.out.println("输出森林: " + forest3.length + " 棵树");
                System.out.print("  树1: ");
                printTreePreorder(forest3[0]);
                System.out.println("  (期望: S S1)");
            }

            // --- 5d. null 输入 ---
            {
                System.out.println("\n--- 5d. null 输入 ---");
                TreeNode[] forest4 = converter.binaryTreeToForest(null);
                System.out.println("null → " + (forest4 == null ? "null ✓" : "非空 ✗"));
            }
        }
    }

    /** 先序遍历二叉树（简单辅助） */
    private static void printPreorder(BinaryTreeNode node) {
        if (node == null) return;
        System.out.print(node.data + " ");
        printPreorder(node.lChild);
        printPreorder(node.rChild);
    }

    /** 先序遍历多叉树（验证辅助） */
    private static void printTreePreorder(TreeNode node) {
        if (node == null) return;
        System.out.print(node.data + " ");
        if (node.children != null) {
            for (TreeNode child : node.children) {
                printTreePreorder(child);
            }
        }
    }

    /** 比较两棵多叉树结构是否完全一致 */
    private static boolean treesEqual(TreeNode a, TreeNode b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (!a.data.equals(b.data)) return false;
        if (a.children.size() != b.children.size()) return false;
        for (int i = 0; i < a.children.size(); i++) {
            if (!treesEqual(a.children.get(i), b.children.get(i))) return false;
        }
        return true;
    }
}
