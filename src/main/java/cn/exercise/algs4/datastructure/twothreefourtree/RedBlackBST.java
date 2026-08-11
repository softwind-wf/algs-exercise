/******************************************************************************
 *  Compilation:  javac RedBlackBST.java
 *  Execution:    java RedBlackBST < input.txt
 *  Dependencies: StdIn.java StdOut.java
 *  Data files:   https://algs4.cs.princeton.edu/33balanced/tinyST.txt
 *
 *  A symbol table implemented using a left-leaning red-black BST.
 *  This is the 2-3 version.
 *
 *  Note: commented out assertions because DrJava now enables assertions
 *        by default.
 *
 *  % more tinyST.txt
 *  S E A R C H E X A M P L E
 *
 *  % java RedBlackBST < tinyST.txt
 *  A 8
 *  C 4
 *  E 12
 *  H 5
 *  L 11
 *  M 9
 *  P 10
 *  R 3
 *  S 0
 *  X 7
 *
 ******************************************************************************/

package cn.exercise.algs4.datastructure.twothreefourtree;

import edu.princeton.cs.algs4.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *  The {@code BST} class represents an ordered symbol table of generic
 *  key-value pairs.
 *  It supports the usual <em>put</em>, <em>get</em>, <em>contains</em>,
 *  <em>delete</em>, <em>size</em>, and <em>is-empty</em> methods.
 *  It also provides ordered methods for finding the <em>minimum</em>,
 *  <em>maximum</em>, <em>floor</em>, and <em>ceiling</em>.
 *  It also provides a <em>keys</em> method for iterating over all of the keys.
 *  A symbol table implements the <em>associative array</em> abstraction:
 *  when associating a value with a key that is already in the symbol table,
 *  the convention is to replace the old value with the new value.
 *  Unlike {@link java.util.Map}, this class uses the convention that
 *  values cannot be {@code null}—setting the
 *  value associated with a key to {@code null} is equivalent to deleting the key
 *  from the symbol table.
 *  <p>
 *  It requires that
 *  the key type implements the {@code Comparable} interface and calls the
 *  {@code compareTo()} and method to compare two keys. It does not call either
 *  {@code equals()} or {@code hashCode()}.
 *  <p>
 *  This implementation uses a <em>left-leaning red-black BST</em>.
 *  The <em>put</em>, <em>get</em>, <em>contains</em>, <em>remove</em>,
 *  <em>minimum</em>, <em>maximum</em>, <em>ceiling</em>, <em>floor</em>,
 *  <em>rank</em>, and <em>select</em> operations each take
 *  &Theta;(log <em>n</em>) time in the worst case, where <em>n</em> is the
 *  number of key-value pairs in the symbol table.
 *  The <em>size</em>, and <em>is-empty</em> operations take &Theta;(1) time.
 *  The <em>keys</em> methods take
 *  <em>O</em>(log <em>n</em> + <em>m</em>) time, where <em>m</em> is
 *  the number of keys returned by the iterator.
 *  Construction takes &Theta;(1) time.
 *  <p>
 *  For alternative implementations of the symbol table API, see {@link ST},
 *  {@link BinarySearchST}, {@link SequentialSearchST}, {@link BST},
 *  {@link SeparateChainingHashST}, {@link LinearProbingHashST}, and
 *  {@link AVLTreeST}.
 *  For additional documentation, see
 *  <a href="https://algs4.cs.princeton.edu/33balanced">Section 3.3</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */

public class RedBlackBST<Key extends Comparable<Key>, Value> {

    private static final boolean RED   = true;
    private static final boolean BLACK = false;

    private Node root;     // root of the BST

    // 操作过程跟踪(供可视化,默认关闭):trace != null 时逐帧记录
    private List<TraceStep<Key>> trace;
    private final List<Integer> curPath = new ArrayList<>();   // 当前下潜路径(0=左,1=右)

    // BST helper node data type
    private class Node {
        private Key key;           // key
        private Value val;         // associated data
        private Node left, right;  // links to left and right subtrees
        private boolean color;     // color of parent link
        private int size;          // subtree count

        public Node(Key key, Value val, boolean color, int size) {
            this.key = key;
            this.val = val;
            this.color = color;
            this.size = size;
        }
    }

    /**
     * Initializes an empty symbol table.
     */
    public RedBlackBST() {
    }

   /***************************************************************************
    *  Node helper methods.
    ***************************************************************************/
    // is node x red; false if x is null ?
    private boolean isRed(Node x) {
        if (x == null) return false;
        return x.color == RED;
    }

    // number of node in subtree rooted at x; 0 if x is null
    private int size(Node x) {
        if (x == null) return 0;
        return x.size;
    }


    /**
     * Returns the number of key-value pairs in this symbol table.
     * @return the number of key-value pairs in this symbol table
     */
    public int size() {
        return size(root);
    }

   /**
     * Is this symbol table empty?
     * @return {@code true} if this symbol table is empty and {@code false} otherwise
     */
    public boolean isEmpty() {
        return root == null;
    }


   /***************************************************************************
    *  Standard BST search.
    ***************************************************************************/

    /**
     * Returns the value associated with the given key.
     * @param key the key
     * @return the value associated with the given key if the key is in the symbol table
     *     and {@code null} if the key is not in the symbol table
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to get() is null");
        return get(root, key);
    }

    // value associated with the given key in subtree rooted at x; null if no such key
    private Value get(Node x, Key key) {
        while (x != null) {
            int cmp = key.compareTo(x.key);
            if      (cmp < 0) x = x.left;
            else if (cmp > 0) x = x.right;
            else              return x.val;
        }
        return null;
    }

    /**
     * Does this symbol table contain the given key?
     * @param key the key
     * @return {@code true} if this symbol table contains {@code key} and
     *     {@code false} otherwise
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public boolean contains(Key key) {
        return get(key) != null;
    }

   /***************************************************************************
    *  Red-black tree insertion.
    ***************************************************************************/

    /**
     * Inserts the specified key-value pair into the symbol table, overwriting the old
     * value with the new value if the symbol table already contains the specified key.
     * Deletes the specified key (and its associated value) from this symbol table
     * if the specified value is {@code null}.
     *
     * @param key the key
     * @param val the value
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public void put(Key key, Value val) {
        if (key == null) throw new IllegalArgumentException("first argument to put() is null");
        if (val == null) {
            delete(key);
            return;
        }

        boolean emptyTree = (root == null);
        root = put(root, key, val);
        if (emptyTree) {
            record("空树:创建红节点 " + key + " 作为根", new Hl(HlKind.INSERT, new int[0]));
        }
        if (isRed(root)) {
            root.color = BLACK;
            record("根置黑:红黑树根必须为黑, " + key + " 由红变黑", new Hl(HlKind.ROOT, new int[0]));
        }
        // assert check();
    }

    // insert the key-value pair in the subtree rooted at h
    private Node put(Node h, Key key, Value val) {
        if (h == null) return new Node(key, val, RED, 1);

        int cmp = key.compareTo(h.key);
        if (cmp < 0) {
            boolean wasNull = (h.left == null);
            curPath.add(0);
            h.left = put(h.left, key, val);
            curPath.remove(curPath.size() - 1);
            if (wasNull) {
                record("红节点 " + key + " 挂到 " + h.key + " 的左子树(新键先为红)",
                        new Hl(HlKind.INSERT, hlPath(0)));
            }
        } else if (cmp > 0) {
            boolean wasNull = (h.right == null);
            curPath.add(1);
            h.right = put(h.right, key, val);
            curPath.remove(curPath.size() - 1);
            if (wasNull) {
                record("红节点 " + key + " 挂到 " + h.key + " 的右子树(新键先为红)",
                        new Hl(HlKind.INSERT, hlPath(1)));
            }
        } else {
            h.val = val;
        }

        // fix-up any right-leaning links
        if (isRed(h.right) && !isRed(h.left))      h = rotateLeft(h);
        if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
        if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);
        h.size = size(h.left) + size(h.right) + 1;

        return h;
    }

   /***************************************************************************
    *  Red-black tree deletion.
    ***************************************************************************/

    /**
     * Removes the smallest key and associated value from the symbol table.
     * @throws NoSuchElementException if the symbol table is empty
     */
    public void deleteMin() {
        if (isEmpty()) throw new NoSuchElementException("BST underflow");

        // if both children of root are black, set root to red
        if (!isRed(root.left) && !isRed(root.right))
            root.color = RED;

        root = deleteMin(root);
        if (!isEmpty()) root.color = BLACK;
        // assert check();
    }

    // delete the key-value pair with the minimum key rooted at h
    private Node deleteMin(Node h) {
        if (h.left == null) {
            record("删除最小键 " + h.key + "(该节点无左孩子, 直接摘除)", new Hl(HlKind.DELETE, hlPath()));
            return null;
        }

        if (!isRed(h.left) && !isRed(h.left.left))
            h = moveRedLeft(h);

        curPath.add(0);
        h.left = deleteMin(h.left);
        curPath.remove(curPath.size() - 1);
        return balance(h);
    }


    /**
     * Removes the largest key and associated value from the symbol table.
     * @throws NoSuchElementException if the symbol table is empty
     */
    public void deleteMax() {
        if (isEmpty()) throw new NoSuchElementException("BST underflow");

        // if both children of root are black, set root to red
        if (!isRed(root.left) && !isRed(root.right))
            root.color = RED;

        root = deleteMax(root);
        if (!isEmpty()) root.color = BLACK;
        // assert check();
    }

    // delete the key-value pair with the maximum key rooted at h
    private Node deleteMax(Node h) {
        if (isRed(h.left))
            h = rotateRight(h);

        if (h.right == null) {
            record("删除最大键 " + h.key + "(该节点无右孩子, 直接摘除)", new Hl(HlKind.DELETE, hlPath()));
            return null;
        }

        if (!isRed(h.right) && !isRed(h.right.left))
            h = moveRedRight(h);

        curPath.add(1);
        h.right = deleteMax(h.right);
        curPath.remove(curPath.size() - 1);

        return balance(h);
    }

    /**
     * Removes the specified key and its associated value from this symbol table
     * (if the key is in this symbol table).
     *
     * @param  key the key
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public void delete(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to delete() is null");
        if (!contains(key)) {
            record("键 " + key + " 不存在, 删除失败");
            return;
        }

        // if both children of root are black, set root to red
        if (!isRed(root.left) && !isRed(root.right)) {
            root.color = RED;
            record("根临时置红(删除时让根可参与颜色翻转)", new Hl(HlKind.ROOT, new int[0]));
        }

        root = delete(root, key);
        if (!isEmpty() && isRed(root)) {
            root.color = BLACK;
            record("根置黑", new Hl(HlKind.ROOT, new int[0]));
        }
        // assert check();
    }

    // delete the key-value pair with the given key rooted at h
    private Node delete(Node h, Key key) {
        // assert get(h, key) != null;

        if (key.compareTo(h.key) < 0)  {
            if (!isRed(h.left) && !isRed(h.left.left))
                h = moveRedLeft(h);
            curPath.add(0);
            h.left = delete(h.left, key);
            curPath.remove(curPath.size() - 1);
        }
        else {
            if (isRed(h.left))
                h = rotateRight(h);
            if (key.compareTo(h.key) == 0 && (h.right == null)) {
                record("删除键 " + h.key + "(无右子树, 直接摘除)", new Hl(HlKind.DELETE, hlPath()));
                return null;
            }
            if (!isRed(h.right) && !isRed(h.right.left))
                h = moveRedRight(h);
            if (key.compareTo(h.key) == 0) {
                Node x = min(h.right);
                record("用右子树最小键 " + x.key + " 替换被删键 " + h.key
                                + "(再删掉右子树最小键, 保持 BST 中序不变)",
                        new Hl(HlKind.SUCCESSOR, hlPath()));
                h.key = x.key;
                h.val = x.val;
                // h.val = get(h.right, min(h.right).key);
                // h.key = min(h.right).key;
                curPath.add(1);
                h.right = deleteMin(h.right);
                curPath.remove(curPath.size() - 1);
            }
            else {
                curPath.add(1);
                h.right = delete(h.right, key);
                curPath.remove(curPath.size() - 1);
            }
        }
        return balance(h);
    }

   /***************************************************************************
    *  Red-black tree helper functions.
    ***************************************************************************/

    // make a left-leaning link lean to the right
    private Node rotateRight(Node h) {
        assert (h != null) && isRed(h.left);
        // assert (h != null) && isRed(h.left) &&  !isRed(h.right);  // for insertion only
        Node x = h.left;
        boolean hWasRed = isRed(h);
        record("右旋前: " + h.key + "(" + colorName(h) + ") 与其红左孩子 " + x.key + " 形成连续红链,需右旋",
                new Hl(HlKind.ROTATE, hlPath()), new Hl(HlKind.ROTATE, hlPath(0)));
        h.left = x.right;
        x.right = h;
        x.color = h.color;
        h.color = RED;
        x.size = h.size;
        h.size = size(h.left) + size(h.right) + 1;
        relinkAfterRotate(x);                          // 父指针就地修正,保证快照完整
        record("右旋完成: " + x.key + " 上移为子树根(继承原 " + h.key + " 的" + (hWasRed ? "红" : "黑")
                        + "色), " + h.key + " 变为红",
                new Hl(HlKind.ROTATE, hlPath()), new Hl(HlKind.ROTATE, hlPath(1)));
        return x;
    }

    // make a right-leaning link lean to the left
    private Node rotateLeft(Node h) {
        assert (h != null) && isRed(h.right);
        // assert (h != null) && isRed(h.right) && !isRed(h.left);  // for insertion only
        Node x = h.right;
        boolean hWasRed = isRed(h);
        record("左旋前: " + h.key + "(" + colorName(h) + ") 与其红右孩子 " + x.key + " 构成右倾红链,需左旋",
                new Hl(HlKind.ROTATE, hlPath()), new Hl(HlKind.ROTATE, hlPath(1)));
        h.right = x.left;
        x.left = h;
        x.color = h.color;
        h.color = RED;
        x.size = h.size;
        h.size = size(h.left) + size(h.right) + 1;
        relinkAfterRotate(x);                          // 父指针就地修正,保证快照完整
        record("左旋完成: " + x.key + " 上移为子树根(继承原 " + h.key + " 的" + (hWasRed ? "红" : "黑")
                        + "色), " + h.key + " 变为红",
                new Hl(HlKind.ROTATE, hlPath()), new Hl(HlKind.ROTATE, hlPath(0)));
        return x;
    }

    /**
     * 旋转把子树根换成了 newRoot,但调用方要到递归返回后才会重新挂接父指针。
     * 这里按 curPath 就地修正:根则更新 root,否则把父节点的孩子指针指向 newRoot,
     * 让随后 record() 的快照从根出发就完整。调用方之后赋的是同一个值,无副作用。
     */
    private void relinkAfterRotate(Node newRoot) {
        if (curPath.isEmpty()) {
            root = newRoot;
            return;
        }
        Node parent = root;
        for (int i = 0; i < curPath.size() - 1; i++) {
            parent = curPath.get(i) == 0 ? parent.left : parent.right;
        }
        int dir = curPath.get(curPath.size() - 1);
        if (dir == 0) {
            parent.left = newRoot;
        } else {
            parent.right = newRoot;
        }
    }

    // flip the colors of a node and its two children
    private void flipColors(Node h) {
        // h must have opposite color of its two children
        // assert (h != null) && (h.left != null) && (h.right != null);
        // assert (!isRed(h) &&  isRed(h.left) &&  isRed(h.right))
        //    || (isRed(h)  && !isRed(h.left) && !isRed(h.right));
        record("颜色翻转前: " + h.key + "=" + colorName(h) + ", 左 " + keyStr(h.left) + "=" + colorName(h.left)
                        + ", 右 " + keyStr(h.right) + "=" + colorName(h.right)
                        + " (两个红孩子压在同一层, 相当于 4-节点, 拆分并上推)",
                new Hl(HlKind.FLIP, hlPath()), new Hl(HlKind.FLIP, hlPath(0)), new Hl(HlKind.FLIP, hlPath(1)));
        h.color = !h.color;
        h.left.color = !h.left.color;
        h.right.color = !h.right.color;
        record("颜色翻转完成: " + h.key + " 变为 " + colorName(h) + ", 左孩子变为 " + colorName(h.left)
                        + ", 右孩子变为 " + colorName(h.right),
                new Hl(HlKind.FLIP, hlPath()), new Hl(HlKind.FLIP, hlPath(0)), new Hl(HlKind.FLIP, hlPath(1)));
    }

    // Assuming that h is red and both h.left and h.left.left
    // are black, make h.left or one of its children red.
    private Node moveRedLeft(Node h) {
        // assert (h != null);
        // assert isRed(h) && !isRed(h.left) && !isRed(h.left.left);

        record("moveRedLeft: 左子树两个孩子都黑, 下潜前先腾出一个红节点",
                new Hl(HlKind.TARGET, hlPath()));
        flipColors(h);
        if (isRed(h.right.left)) {
            curPath.add(1);                       // 让 rotateRight 的记录指向 h.right 而非 h
            h.right = rotateRight(h.right);
            curPath.remove(curPath.size() - 1);
            h = rotateLeft(h);
            flipColors(h);
        }
        record("moveRedLeft 完成: 左子树获得一个红节点, 可继续下潜", new Hl(HlKind.TARGET, hlPath()));
        return h;
    }

    // Assuming that h is red and both h.right and h.right.left
    // are black, make h.right or one of its children red.
    private Node moveRedRight(Node h) {
        // assert (h != null);
        // assert isRed(h) && !isRed(h.right) && !isRed(h.right.left);
        record("moveRedRight: 右子树两个孩子都黑, 下潜前先腾出一个红节点",
                new Hl(HlKind.TARGET, hlPath()));
        flipColors(h);
        if (isRed(h.left.left)) {
            h = rotateRight(h);
            flipColors(h);
        }
        record("moveRedRight 完成: 右子树获得一个红节点, 可继续下潜", new Hl(HlKind.TARGET, hlPath()));
        return h;
    }

    // restore red-black tree invariant
    private Node balance(Node h) {
        // assert (h != null);

        if (isRed(h.right) && !isRed(h.left))    h = rotateLeft(h);
        if (isRed(h.left) && isRed(h.left.left)) h = rotateRight(h);
        if (isRed(h.left) && isRed(h.right))     flipColors(h);

        h.size = size(h.left) + size(h.right) + 1;
        return h;
    }


   /***************************************************************************
    *  Utility functions.
    ***************************************************************************/

    /**
     * Returns the height of the BST (for debugging).
     * @return the height of the BST (a 1-node tree has height 0)
     */
    public int height() {
        return height(root);
    }
    private int height(Node x) {
        if (x == null) return -1;
        return 1 + Math.max(height(x.left), height(x.right));
    }

   /***************************************************************************
    *  Ordered symbol table methods.
    ***************************************************************************/

    /**
     * Returns the smallest key in the symbol table.
     * @return the smallest key in the symbol table
     * @throws NoSuchElementException if the symbol table is empty
     */
    public Key min() {
        if (isEmpty()) throw new NoSuchElementException("calls min() with empty symbol table");
        return min(root).key;
    }

    // the smallest key in subtree rooted at x; null if no such key
    private Node min(Node x) {
        // assert x != null;
        if (x.left == null) return x;
        else                return min(x.left);
    }

    /**
     * Returns the largest key in the symbol table.
     * @return the largest key in the symbol table
     * @throws NoSuchElementException if the symbol table is empty
     */
    public Key max() {
        if (isEmpty()) throw new NoSuchElementException("calls max() with empty symbol table");
        return max(root).key;
    }

    // the largest key in the subtree rooted at x; null if no such key
    private Node max(Node x) {
        // assert x != null;
        if (x.right == null) return x;
        else                 return max(x.right);
    }


    /**
     * Returns the largest key in the symbol table less than or equal to {@code key}.
     * @param key the key
     * @return the largest key in the symbol table less than or equal to {@code key}
     * @throws NoSuchElementException if there is no such key
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public Key floor(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to floor() is null");
        if (isEmpty()) throw new NoSuchElementException("calls floor() with empty symbol table");
        Node x = floor(root, key);
        if (x == null) throw new NoSuchElementException("argument to floor() is too small");
        else           return x.key;
    }

    // the largest key in the subtree rooted at x less than or equal to the given key
    private Node floor(Node x, Key key) {
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp == 0) return x;
        if (cmp < 0)  return floor(x.left, key);
        Node t = floor(x.right, key);
        if (t != null) return t;
        else           return x;
    }

    /**
     * Returns the smallest key in the symbol table greater than or equal to {@code key}.
     * @param key the key
     * @return the smallest key in the symbol table greater than or equal to {@code key}
     * @throws NoSuchElementException if there is no such key
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public Key ceiling(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to ceiling() is null");
        if (isEmpty()) throw new NoSuchElementException("calls ceiling() with empty symbol table");
        Node x = ceiling(root, key);
        if (x == null) throw new NoSuchElementException("argument to ceiling() is too large");
        else           return x.key;
    }

    // the smallest key in the subtree rooted at x greater than or equal to the given key
    private Node ceiling(Node x, Key key) {
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp == 0) return x;
        if (cmp > 0)  return ceiling(x.right, key);
        Node t = ceiling(x.left, key);
        if (t != null) return t;
        else           return x;
    }

    /**
     * Return the key in the symbol table of a given {@code rank}.
     * This key has the property that there are {@code rank} keys in
     * the symbol table that are smaller. In other words, this key is the
     * ({@code rank}+1)st smallest key in the symbol table.
     *
     * @param  rank the order statistic
     * @return the key in the symbol table of given {@code rank}
     * @throws IllegalArgumentException unless {@code rank} is between 0 and
     *        <em>n</em>–1
     */
    public Key select(int rank) {
        if (rank < 0 || rank >= size()) {
            throw new IllegalArgumentException("argument to select() is invalid: " + rank);
        }
        return select(root, rank);
    }

    // Return key in BST rooted at x of given rank.
    // Precondition: rank is in legal range.
    private Key select(Node x, int rank) {
        if (x == null) return null;
        int leftSize = size(x.left);
        if      (leftSize > rank) return select(x.left,  rank);
        else if (leftSize < rank) return select(x.right, rank - leftSize - 1);
        else                      return x.key;
    }

    /**
     * Return the number of keys in the symbol table strictly less than {@code key}.
     * @param key the key
     * @return the number of keys in the symbol table strictly less than {@code key}
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public int rank(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to rank() is null");
        return rank(key, root);
    }

    // number of keys less than key in the subtree rooted at x
    private int rank(Key key, Node x) {
        if (x == null) return 0;
        int cmp = key.compareTo(x.key);
        if      (cmp < 0) return rank(key, x.left);
        else if (cmp > 0) return 1 + size(x.left) + rank(key, x.right);
        else              return size(x.left);
    }

   /***************************************************************************
    *  Range count and range search.
    ***************************************************************************/

    /**
     * Returns all keys in the symbol table in ascending order as an {@code Iterable}.
     * To iterate over all of the keys in the symbol table named {@code st},
     * use the foreach notation: {@code for (Key key : st.keys())}.
     * @return all keys in the symbol table in ascending order
     */
    public Iterable<Key> keys() {
        if (isEmpty()) return new Queue<Key>();
        return keys(min(), max());
    }

    /**
     * Returns all keys in the symbol table in the given range in ascending order,
     * as an {@code Iterable}.
     *
     * @param  lo minimum endpoint
     * @param  hi maximum endpoint
     * @return all keys in the symbol table between {@code lo}
     *    (inclusive) and {@code hi} (inclusive) in ascending order
     * @throws IllegalArgumentException if either {@code lo} or {@code hi}
     *    is {@code null}
     */
    public Iterable<Key> keys(Key lo, Key hi) {
        if (lo == null) throw new IllegalArgumentException("first argument to keys() is null");
        if (hi == null) throw new IllegalArgumentException("second argument to keys() is null");

        Queue<Key> queue = new Queue<Key>();
        // if (isEmpty() || lo.compareTo(hi) > 0) return queue;
        keys(root, queue, lo, hi);
        return queue;
    }

    // add the keys between lo and hi in the subtree rooted at x
    // to the queue
    private void keys(Node x, Queue<Key> queue, Key lo, Key hi) {
        if (x == null) return;
        int cmplo = lo.compareTo(x.key);
        int cmphi = hi.compareTo(x.key);
        if (cmplo < 0) keys(x.left, queue, lo, hi);
        if (cmplo <= 0 && cmphi >= 0) queue.enqueue(x.key);
        if (cmphi > 0) keys(x.right, queue, lo, hi);
    }

    /**
     * Returns the number of keys in the symbol table in the given range.
     *
     * @param  lo minimum endpoint
     * @param  hi maximum endpoint
     * @return the number of keys in the symbol table between {@code lo}
     *    (inclusive) and {@code hi} (inclusive)
     * @throws IllegalArgumentException if either {@code lo} or {@code hi}
     *    is {@code null}
     */
    public int size(Key lo, Key hi) {
        if (lo == null) throw new IllegalArgumentException("first argument to size() is null");
        if (hi == null) throw new IllegalArgumentException("second argument to size() is null");

        if (lo.compareTo(hi) > 0) return 0;
        if (contains(hi)) return rank(hi) - rank(lo) + 1;
        else              return rank(hi) - rank(lo);
    }


   /***************************************************************************
    *  Operation trace (for visualization, disabled by default).
    *  Only active when trace != null (via putTraced / deleteTraced);
    *  normal put()/delete() are completely unaffected.
    ***************************************************************************/

    /** 树快照节点:key + 颜色 + 左右孩子,供可视化只读绘制。 */
    public static class NodeView<K> {
        public K key;
        public boolean red;
        public NodeView<K> left, right;

        public NodeView(K key, boolean red) {
            this.key = key;
            this.red = red;
        }
    }

    /** 高亮类型 */
    public enum HlKind { ROTATE, FLIP, INSERT, DELETE, TARGET, SUCCESSOR, ROOT }

    /** 一条高亮:path 从根到目标节点(0=左孩子, 1=右孩子) */
    public static class Hl {
        public final HlKind kind;
        public final int[] path;

        public Hl(HlKind kind, int[] path) {
            this.kind = kind;
            this.path = path;
        }
    }

    /** 操作过程中的一步:树快照 + 中文说明 + 高亮列表 */
    public static class TraceStep<K> {
        public final String desc;
        public final NodeView<K> root;
        public final List<Hl> hl;

        public TraceStep(String desc, NodeView<K> root, List<Hl> hl) {
            this.desc = desc;
            this.root = root;
            this.hl = hl;
        }
    }

    /** 深拷贝整棵树(供快照) */
    public NodeView<Key> snapshot() {
        return copyView(root);
    }

    private NodeView<Key> copyView(Node n) {
        if (n == null) {
            return null;
        }
        NodeView<Key> v = new NodeView<>(n.key, isRed(n));
        v.left = copyView(n.left);
        v.right = copyView(n.right);
        return v;
    }

    /** 插入并记录全过程步骤(可视化用) */
    public List<TraceStep<Key>> putTraced(Key key, Value val) {
        trace = new ArrayList<>();
        curPath.clear();
        put(key, val);
        record("插入完成: " + key + " 已就位, 红黑树重新平衡, size=" + size());
        List<TraceStep<Key>> out = trace;
        trace = null;
        return out;
    }

    /** 删除并记录全过程步骤(可视化用) */
    public List<TraceStep<Key>> deleteTraced(Key key) {
        trace = new ArrayList<>();
        curPath.clear();
        delete(key);
        record("删除完成: " + key + (contains(key) ? " 未删除(不存在)" : " 已删除")
                + ", 红黑树重新平衡, size=" + size());
        List<TraceStep<Key>> out = trace;
        trace = null;
        return out;
    }

    /** 记录一步:深拷贝当前树作为快照。trace 为 null 时直接返回。 */
    private void record(String desc, Hl... hls) {
        if (trace == null) {
            return;
        }
        List<Hl> list = new ArrayList<>();
        for (Hl h : hls) {
            if (h != null) {
                list.add(h);
            }
        }
        trace.add(new TraceStep<>(desc, snapshot(), list));
    }

    /** 把 curPath 与 tail 拼成一条从根出发的高亮路径(0=左 1=右) */
    private int[] hlPath(int... tail) {
        int[] p = new int[curPath.size() + tail.length];
        for (int k = 0; k < curPath.size(); k++) {
            p[k] = curPath.get(k);
        }
        System.arraycopy(tail, 0, p, curPath.size(), tail.length);
        return p;
    }

    /** 节点颜色描述(防 null) */
    private String colorName(Node n) {
        if (n == null) {
            return "null";
        }
        return n.color == RED ? "红" : "黑";
    }

    /** 节点键描述(防 null) */
    private String keyStr(Node n) {
        return n == null ? "null" : String.valueOf(n.key);
    }


   /***************************************************************************
    *  Check integrity of red-black tree data structure.
    ***************************************************************************/
    private boolean check() {
        if (!isBST())            StdOut.println("Not in symmetric order");
        if (!isSizeConsistent()) StdOut.println("Subtree counts not consistent");
        if (!isRankConsistent()) StdOut.println("Ranks not consistent");
        if (!is23())             StdOut.println("Not a 2-3 tree");
        if (!isBalanced())       StdOut.println("Not balanced");
        return isBST() && isSizeConsistent() && isRankConsistent() && is23() && isBalanced();
    }

    // does this binary tree satisfy symmetric order?
    // Note: this test also ensures that data structure is a binary tree since order is strict
    private boolean isBST() {
        return isBST(root, null, null);
    }

    // is the tree rooted at x a BST with all keys strictly between min and max
    // (if min or max is null, treat as empty constraint)
    // Credit: elegant solution due to Bob Dondero
    private boolean isBST(Node x, Key min, Key max) {
        if (x == null) return true;
        if (min != null && x.key.compareTo(min) <= 0) return false;
        if (max != null && x.key.compareTo(max) >= 0) return false;
        return isBST(x.left, min, x.key) && isBST(x.right, x.key, max);
    }

    // are the size fields correct?
    private boolean isSizeConsistent() { return isSizeConsistent(root); }
    private boolean isSizeConsistent(Node x) {
        if (x == null) return true;
        if (x.size != size(x.left) + size(x.right) + 1) return false;
        return isSizeConsistent(x.left) && isSizeConsistent(x.right);
    }

    // check that ranks are consistent
    private boolean isRankConsistent() {
        for (int i = 0; i < size(); i++)
            if (i != rank(select(i))) return false;
        for (Key key : keys())
            if (key.compareTo(select(rank(key))) != 0) return false;
        return true;
    }

    // Does the tree have no red right links, and at most one (left)
    // red links in a row on any path?
    private boolean is23() { return is23(root); }
    private boolean is23(Node x) {
        if (x == null) return true;
        if (isRed(x.right)) return false;
        if (x != root && isRed(x) && isRed(x.left))
            return false;
        return is23(x.left) && is23(x.right);
    }

    // do all paths from root to leaf have same number of black edges?
    private boolean isBalanced() {
        int black = 0;     // number of black links on path from root to min
        Node x = root;
        while (x != null) {
            if (!isRed(x)) black++;
            x = x.left;
        }
        return isBalanced(root, black);
    }

    // does every path from the root to a leaf have the given number of black links?
    private boolean isBalanced(Node x, int black) {
        if (x == null) return black == 0;
        if (!isRed(x)) black--;
        return isBalanced(x.left, black) && isBalanced(x.right, black);
    }


    /**
     * Unit tests the {@code RedBlackBST} data type.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        RedBlackBST<String, Integer> st = new RedBlackBST<String, Integer>();
        for (int i = 0; !StdIn.isEmpty(); i++) {
            String key = StdIn.readString();
            st.put(key, i);
        }
        StdOut.println();
        for (String s : st.keys())
            StdOut.println(s + " " + st.get(s));
        StdOut.println();
    }
}

/******************************************************************************
 *  Copyright 2002-2025, Robert Sedgewick and Kevin Wayne.
 *
 *  This file is part of algs4.jar, which accompanies the textbook
 *
 *      Algorithms, 4th edition by Robert Sedgewick and Kevin Wayne,
 *      Addison-Wesley Professional, 2011, ISBN 0-321-57351-X.
 *      http://algs4.cs.princeton.edu
 *
 *
 *  algs4.jar is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  algs4.jar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with algs4.jar.  If not, see http://www.gnu.org/licenses.
 ******************************************************************************/
