package test3;

public class RedBlackTree {
    // 颜色定义
    private static final boolean RED = true;
    private static final boolean BLACK = false;

    // 节点类
    private class Node {
        int key;
        Node left, right, parent;
        boolean color;

        public Node(int key) {
            this.key = key;
            this.left = NIL;
            this.right = NIL;
            this.parent = NIL;
            this.color = RED; // 新节点默认红色
        }
    }

    // 哨兵节点 NIL（所有空指针都指向它）
    private final Node NIL;
    private Node root;

    public RedBlackTree() {
        // 初始化哨兵
        NIL = new Node(0);
        NIL.color = BLACK;
        NIL.left = NIL;
        NIL.right = NIL;
        NIL.parent = NIL;

        root = NIL;
    }

    // ------------------------------
    // 辅助：获取节点颜色（NIL 视为黑）
    // ------------------------------
    private boolean getColor(Node node) {
        if (node == NIL) return BLACK;
        return node.color;
    }

    // ------------------------------
    // 左旋
    // ------------------------------
    private void rotateLeft(Node x) {
        Node y = x.right;
        x.right = y.left;

        if (y.left != NIL) {
            y.left.parent = x;
        }

        y.parent = x.parent;

        if (x.parent == NIL) {
            root = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }

        y.left = x;
        x.parent = y;
    }

    // ------------------------------
    // 右旋
    // ------------------------------
    private void rotateRight(Node x) {
        Node y = x.left;
        x.left = y.right;

        if (y.right != NIL) {
            y.right.parent = x;
        }

        y.parent = x.parent;

        if (x.parent == NIL) {
            root = y;
        } else if (x == x.parent.right) {
            x.parent.right = y;
        } else {
            x.parent.left = y;
        }

        y.right = x;
        x.parent = y;
    }

    // ------------------------------
    // 插入
    // ------------------------------
    public void insert(int key) {
        Node z = new Node(key);
        Node y = NIL;
        Node x = root;

        while (x != NIL) {
            y = x;
            if (z.key < x.key) {
                x = x.left;
            } else {
                x = x.right;
            }
        }

        z.parent = y;

        if (y == NIL) {
            root = z;
        } else if (z.key < y.key) {
            y.left = z;
        } else {
            y.right = z;
        }

        insertFixup(z);
    }

    // ------------------------------
    // 插入修复
    // ------------------------------
    private void insertFixup(Node z) {
        while (getColor(z.parent) == RED) {
            if (z.parent == z.parent.parent.left) {
                // 父是祖父左孩子
                Node y = z.parent.parent.right; // 叔叔

                if (getColor(y) == RED) {
                    // Case 1 叔叔红
                    z.parent.color = BLACK;
                    y.color = BLACK;
                    z.parent.parent.color = RED;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.right) {
                        // Case 2
                        z = z.parent;
                        rotateLeft(z);
                    }
                    // Case 3
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    rotateRight(z.parent.parent);
                }
            } else {
                // 父是祖父右孩子（对称）
                Node y = z.parent.parent.left;

                if (getColor(y) == RED) {
                    // Case 1'
                    z.parent.color = BLACK;
                    y.color = BLACK;
                    z.parent.parent.color = RED;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.left) {
                        // Case 2'
                        z = z.parent;
                        rotateRight(z);
                    }
                    // Case 3'
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    rotateLeft(z.parent.parent);
                }
            }
        }
        root.color = BLACK;
    }

    // ------------------------------
    // 最小值节点
    // ------------------------------
    private Node minimum(Node node) {
        while (node.left != NIL) {
            node = node.left;
        }
        return node;
    }

    // ------------------------------
    // 搜索节点
    // ------------------------------
    private Node searchNode(int key) {
        Node x = root;
        while (x != NIL && x.key != key) {
            if (key < x.key) {
                x = x.left;
            } else {
                x = x.right;
            }
        }
        return x;
    }

    public boolean contains(int key) {
        return searchNode(key) != NIL;
    }

    // ------------------------------
    // 移植（替换子树）
    // ------------------------------
    private void transplant(Node u, Node v) {
        if (u.parent == NIL) {
            root = v;
        } else if (u == u.parent.left) {
            u.parent.left = v;
        } else {
            u.parent.right = v;
        }
        v.parent = u.parent;
    }

    // ------------------------------
    // 删除节点
    // ------------------------------
    public boolean remove(int key) {
        Node z = searchNode(key);
        if (z == NIL) return false;

        deleteNode(z);
        return true;
    }

    private void deleteNode(Node z) {
        Node y = z;
        boolean yOriginalColor = y.color;
        Node x;

        if (z.left == NIL) {
            x = z.right;
            transplant(z, z.right);
        } else if (z.right == NIL) {
            x = z.left;
            transplant(z, z.left);
        } else {
            y = minimum(z.right);
            yOriginalColor = y.color;
            x = y.right;

            if (y.parent == z) {
                x.parent = y;
            } else {
                transplant(y, y.right);
                y.right = z.right;
                y.right.parent = y;
            }

            transplant(z, y);
            y.left = z.left;
            y.left.parent = y;
            y.color = z.color;
        }

        if (yOriginalColor == BLACK) {
            deleteFixup(x);
        }
    }

    // ------------------------------
    // 删除修复
    // ------------------------------
    private void deleteFixup(Node x) {
        while (x != root && getColor(x) == BLACK) {
            if (x == x.parent.left) {
                Node w = x.parent.right;

                if (getColor(w) == RED) {
                    // Case 1
                    w.color = BLACK;
                    x.parent.color = RED;
                    rotateLeft(x.parent);
                    w = x.parent.right;
                }

                if (getColor(w.left) == BLACK && getColor(w.right) == BLACK) {
                    // Case 2
                    w.color = RED;
                    x = x.parent;
                } else {
                    if (getColor(w.right) == BLACK) {
                        // Case 3
                        w.left.color = BLACK;
                        w.color = RED;
                        rotateRight(w);
                        w = x.parent.right;
                    }
                    // Case 4
                    w.color = x.parent.color;
                    x.parent.color = BLACK;
                    w.right.color = BLACK;
                    rotateLeft(x.parent);
                    x = root;
                }
            } else {
                // 对称：x 是右孩子
                Node w = x.parent.left;

                if (getColor(w) == RED) {
                    // Case 1'
                    w.color = BLACK;
                    x.parent.color = RED;
                    rotateRight(x.parent);
                    w = x.parent.left;
                }

                if (getColor(w.right) == BLACK && getColor(w.left) == BLACK) {
                    // Case 2'
                    w.color = RED;
                    x = x.parent;
                } else {
                    if (getColor(w.left) == BLACK) {
                        // Case 3'
                        w.right.color = BLACK;
                        w.color = RED;
                        rotateLeft(w);
                        w = x.parent.left;
                    }
                    // Case 4'
                    w.color = x.parent.color;
                    x.parent.color = BLACK;
                    w.left.color = BLACK;
                    rotateRight(x.parent);
                    x = root;
                }
            }
        }
        x.color = BLACK;
    }

    // ------------------------------
    // 中序遍历（输出有序）
    // ------------------------------
    public void inOrder() {
        inOrder(root);
        System.out.println();
    }

    private void inOrder(Node node) {
        if (node != NIL) {
            inOrder(node.left);
            System.out.print(node.key + " ");
            inOrder(node.right);
        }
    }

    // ------------------------------
    // 测试 main
    // ------------------------------
    public static void main(String[] args) {
        RedBlackTree rbt = new RedBlackTree();

        int[] arr = {10, 20, 30, 15, 25, 5};
        for (int num : arr) {
            rbt.insert(num);
        }

        System.out.println("中序遍历（有序）：");
        rbt.inOrder();

        System.out.println("删除 20");
        rbt.remove(20);
        rbt.inOrder();

        System.out.println("删除 5");
        rbt.remove(5);
        rbt.inOrder();
    }
}