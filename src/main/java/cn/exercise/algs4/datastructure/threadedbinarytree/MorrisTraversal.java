package cn.exercise.algs4.datastructure.threadedbinarytree;

/**
 * Morris 遍历 —— 利用线索（thread）实现 O(1) 额外空间的二叉树遍历。
 *
 * <p>核心思想：对于每个有左子树的节点，找到其左子树中最右下的节点，
 * 临时将其 rChild 指向当前节点（建立线索），遍历完后还原。</p>
 *
 * <p>三种遍历：</p>
 * <ul>
 *   <li>先序：在建立线索之前打印当前节点</li>
 *   <li>中序：在删除线索之后（左子树遍历完）打印当前节点</li>
 *   <li>后序：使用 dummy 根节点，在线索删除时逆序访问左子树的右边界</li>
 * </ul>
 */
public class MorrisTraversal {

    public static class Node {
        Object data;
        Node lChild;
        Node rChild;

        public Node(Object data) {
            this.data = data;
        }

        /** 无参构造（用于 dummy 节点） */
        public Node() {
        }

        @Override
        public String toString() {
            return String.valueOf(data);
        }
    }

    // ====================== 工具方法 ======================

    public static void setLeft(Node parent, Node left) {
        parent.lChild = left;
    }

    public static void setRight(Node parent, Node right) {
        parent.rChild = right;
    }

    // ====================== 递归对照版本 ======================

    public static void preorderRecursive(Node root) {
        if (root == null) return;
        System.out.print(root.data + " ");
        preorderRecursive(root.lChild);
        preorderRecursive(root.rChild);
    }

    public static void inorderRecursive(Node root) {
        if (root == null) return;
        inorderRecursive(root.lChild);
        System.out.print(root.data + " ");
        inorderRecursive(root.rChild);
    }

    public static void postorderRecursive(Node root) {
        if (root == null) return;
        postorderRecursive(root.lChild);
        postorderRecursive(root.rChild);
        System.out.print(root.data + " ");
    }

    // ====================== 递归收集（用于验证） ======================

    private static void collectPreorder(Node node, java.util.List<String> list) {
        if (node == null) return;
        list.add(String.valueOf(node.data));
        collectPreorder(node.lChild, list);
        collectPreorder(node.rChild, list);
    }

    private static void collectInorder(Node node, java.util.List<String> list) {
        if (node == null) return;
        collectInorder(node.lChild, list);
        list.add(String.valueOf(node.data));
        collectInorder(node.rChild, list);
    }

    private static void collectPostorder(Node node, java.util.List<String> list) {
        if (node == null) return;
        collectPostorder(node.lChild, list);
        collectPostorder(node.rChild, list);
        list.add(String.valueOf(node.data));
    }

    // ====================== 先序 Morris ======================

    public void preorderMorrisTraversal(Node root) {
        if(root == null)
            return;

        Node cur = root;
        Node pre = null;

        while(cur != null) {
            if(cur.lChild == null) {
                System.out.print(cur.data + " ");
                cur = cur.rChild;
            } else {
                pre = cur.lChild;
                while(pre.rChild != null && pre.rChild != cur) {
                    pre = pre.rChild;
                }

                if(pre.rChild == null) {
                    System.out.print(cur.data + " ");
                    pre.rChild = cur;
                    cur = cur.lChild;
                }
                else {
                    pre.rChild = null;
                    cur = cur.rChild;
                }
            }
        }
    }

    // ====================== 中序 Morris ======================

    public void inorderMorrisTraversal(Node root) {
        if(root == null) {
            return;
        }

        Node cur = root;
        Node pre = null;

        while(cur != null) {
            if(cur.lChild == null) {
                System.out.print(cur.data + " ");
                cur = cur.rChild;
            } else {
                pre = cur.lChild;
                while(pre.rChild != null && pre.rChild != cur) {
                    pre = pre.rChild;
                }

                if(pre.rChild == null) {
                    pre.rChild = cur;
                    cur = cur.lChild;
                }
                else {
                    System.out.print(cur.data + " ");
                    pre.rChild = null;
                    cur = cur.rChild;
                }
            }
        }
    }

    // ====================== 后序 Morris ======================

    public void postorderMorrisTraversal(Node root) {
        if(root == null) {
            return;
        }

        Node dummy = new Node();
        dummy.lChild = root;
        Node cur = dummy;
        Node pre = null;

        while(cur != null) {
            if(cur.lChild == null) {
                cur = cur.rChild;
            } else {
                pre = cur.lChild;
                while(pre.rChild != null && pre.rChild != cur) {
                    pre = pre.rChild;
                }

                if(pre.rChild == null) {
                    pre.rChild = cur;
                    cur = cur.lChild;
                }
                else {
                    reverseVisit(cur.lChild, pre);
                    pre.rChild = null;
                    cur = cur.rChild;
                }
            }
        }

        dummy.lChild = null;
    }

    private void reverseVisit(Node from, Node to) {
        reverse(from, to);
        Node node = to;
        while(true) {
            System.out.print(node.data + " ");
            if(node == from) {
                break;
            }
            node = node.rChild;
        }
        reverse(to, from);
    }

    private void reverse(Node from, Node to) {
        if(from == to) {
            return;
        }
        Node parent = from;
        Node child = from.rChild;
        Node tmp = null;
        while(parent != to) {
            tmp = child.rChild;
            child.rChild = parent;
            parent = child;
            child = tmp;
        }
    }

    // ====================== 收集 Morris 遍历结果（用于验证） ======================

    /** 收集先序 Morris 遍历结果，不打印到 stdout */
    private java.util.List<String> collectPreorderMorris(Node root) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (root == null) return result;

        Node cur = root;
        Node pre = null;

        while (cur != null) {
            if (cur.lChild == null) {
                result.add(String.valueOf(cur.data));
                cur = cur.rChild;
            } else {
                pre = cur.lChild;
                while (pre.rChild != null && pre.rChild != cur) {
                    pre = pre.rChild;
                }
                if (pre.rChild == null) {
                    result.add(String.valueOf(cur.data));
                    pre.rChild = cur;
                    cur = cur.lChild;
                } else {
                    pre.rChild = null;
                    cur = cur.rChild;
                }
            }
        }
        return result;
    }

    /** 收集中序 Morris 遍历结果 */
    private java.util.List<String> collectInorderMorris(Node root) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (root == null) return result;

        Node cur = root;
        Node pre = null;

        while (cur != null) {
            if (cur.lChild == null) {
                result.add(String.valueOf(cur.data));
                cur = cur.rChild;
            } else {
                pre = cur.lChild;
                while (pre.rChild != null && pre.rChild != cur) {
                    pre = pre.rChild;
                }
                if (pre.rChild == null) {
                    pre.rChild = cur;
                    cur = cur.lChild;
                } else {
                    result.add(String.valueOf(cur.data));
                    pre.rChild = null;
                    cur = cur.rChild;
                }
            }
        }
        return result;
    }

    /** 收集后序 Morris 遍历结果 */
    private java.util.List<String> collectPostorderMorris(Node root) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (root == null) return result;

        Node dummy = new Node();
        dummy.lChild = root;
        Node cur = dummy;
        Node preNode = null;

        while (cur != null) {
            if (cur.lChild == null) {
                cur = cur.rChild;
            } else {
                preNode = cur.lChild;
                while (preNode.rChild != null && preNode.rChild != cur) {
                    preNode = preNode.rChild;
                }
                if (preNode.rChild == null) {
                    preNode.rChild = cur;
                    cur = cur.lChild;
                } else {
                    collectReversePath(cur.lChild, preNode, result);
                    preNode.rChild = null;
                    cur = cur.rChild;
                }
            }
        }
        dummy.lChild = null;
        return result;
    }

    /** 收集逆序最右路径（不打印到 stdout，而是加到 list 中） */
    private void collectReversePath(Node from, Node to, java.util.List<String> list) {
        reverse(from, to);
        Node node = to;
        while (true) {
            list.add(String.valueOf(node.data));
            if (node == from) break;
            node = node.rChild;
        }
        reverse(to, from);
    }

    // ====================== main 测试 ======================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     Morris 遍历 — 全面测试             ║");
        System.out.println("╚══════════════════════════════════════════╝");

        // ======= 测试 1：标准二叉树 =======
        System.out.println("\n【测试 1】标准二叉树");
        /*
                  A
                /   \
               B     C
              / \   / \
             D   E F   G
        */
        Node t1 = buildTree1();
        runTest("标准二叉树", t1);

        // ======= 测试 2：左斜树 =======
        System.out.println("\n【测试 2】左斜树");
        /*
                A
               /
              B
             /
            C
        */
        Node t2 = new Node("A");
        setLeft(t2, new Node("B"));
        setLeft(t2.lChild, new Node("C"));
        runTest("左斜树", t2);

        // ======= 测试 3：右斜树 =======
        System.out.println("\n【测试 3】右斜树");
        /*
            A
             \
              B
               \
                C
        */
        Node t3 = new Node("A");
        setRight(t3, new Node("B"));
        setRight(t3.rChild, new Node("C"));
        runTest("右斜树", t3);

        // ======= 测试 4：单节点 =======
        System.out.println("\n【测试 4】单节点");
        runTest("单节点", new Node("A"));

        // ======= 测试 5：空树 =======
        System.out.println("\n【测试 5】空树");
        runTest("空树", null);

        // ======= 测试 6：非满二叉树 =======
        System.out.println("\n【测试 6】非满二叉树（缺右子）");
        /*
                A
               / \
              B   C
             /
            D
        */
        Node t6 = new Node("A");
        setLeft(t6, new Node("B"));
        setRight(t6, new Node("C"));
        setLeft(t6.lChild, new Node("D"));
        runTest("缺右子树", t6);

        // ======= 测试 7：非满二叉树(缺左子) =======
        System.out.println("\n【测试 7】非满二叉树（缺左子）");
        /*
                A
               / \
              B   C
                   \
                    D
        */
        Node t7 = new Node("A");
        setLeft(t7, new Node("B"));
        setRight(t7, new Node("C"));
        setRight(t7.rChild, new Node("D"));
        runTest("缺左子树", t7);

        // ======= 测试 8：复杂 =======
        System.out.println("\n【测试 8】复杂二叉树");
        /*
                A
               /
              B
             / \
            C   D
               /
              E
        */
        Node t8 = new Node("A");
        Node b = new Node("B");
        setLeft(t8, b);
        setLeft(b, new Node("C"));
        Node d = new Node("D");
        setRight(b, d);
        setLeft(d, new Node("E"));
        runTest("复杂树", t8);
    }

    private static Node buildTree1() {
        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");
        Node d = new Node("D");
        Node e = new Node("E");
        Node f = new Node("F");
        Node g = new Node("G");
        setLeft(a, b); setRight(a, c);
        setLeft(b, d); setRight(b, e);
        setLeft(c, f); setRight(c, g);
        return a;
    }

    /** 执行一组完整测试：先序/中序/后序 Morris vs 递归 */
    private static void runTest(String label, Node root) {
        MorrisTraversal mt = new MorrisTraversal();

        // 每种子遍历需要独立的树拷贝（Morris 会修改树结构）
        // 先收集递归参考结果
        java.util.List<String> refPre = new java.util.ArrayList<>();
        java.util.List<String> refIn = new java.util.ArrayList<>();
        java.util.List<String> refPost = new java.util.ArrayList<>();
        collectPreorder(root, refPre);
        collectInorder(root, refIn);
        collectPostorder(root, refPost);

        // --- 先序 ---
        Node copy1 = copyTree(root);
        java.util.List<String> morrisPre = mt.collectPreorderMorris(copy1);
        boolean preOk = refPre.equals(morrisPre);

        // --- 中序 ---
        Node copy2 = copyTree(root);
        java.util.List<String> morrisIn = mt.collectInorderMorris(copy2);
        boolean inOk = refIn.equals(morrisIn);

        // --- 后序 ---
        Node copy3 = copyTree(root);
        java.util.List<String> morrisPost = mt.collectPostorderMorris(copy3);
        boolean postOk = refPost.equals(morrisPost);

        System.out.println("  先序: " + (preOk  ? "✅" : "❌") + "  " + morrisPre);
        System.out.println("  中序: " + (inOk   ? "✅" : "❌") + "  " + morrisIn);
        System.out.println("  后序: " + (postOk ? "✅" : "❌") + "  " + morrisPost);

        if (!preOk || !inOk || !postOk) {
            if (!preOk)  System.out.println("    期望先序: " + refPre);
            if (!inOk)   System.out.println("    期望中序: " + refIn);
            if (!postOk) System.out.println("    期望后序: " + refPost);
        }
    }

    /** 深拷贝二叉树（Morris 会修改指针，每次遍历需要独立副本） */
    private static Node copyTree(Node original) {
        if (original == null) return null;
        Node copy = new Node(original.data);
        copy.lChild = copyTree(original.lChild);
        copy.rChild = copyTree(original.rChild);
        return copy;
    }
}
