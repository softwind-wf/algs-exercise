package com.ds.binarysortedtree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BinarySortedTree 的 JUnit5 单元测试
 *
 * 由于 Node 是 BinarySortedTree 的 private static 内部类，
 * 且 root 字段为 private，所有树结构的验证均通过反射完成。
 */
@DisplayName("BinarySortedTree 二叉排序树测试")
class BinarySortedTreeTest {

    private BinarySortedTree tree;
    private Field rootField;
    private Field dataField;
    private Field parentField;
    private Field lChildField;
    private Field rChildField;

    @BeforeEach
    void setUp() throws Exception {
        tree = new BinarySortedTree();

        // 反射获取私有字段 root
        rootField = BinarySortedTree.class.getDeclaredField("root");
        rootField.setAccessible(true);

        // 反射获取私有静态内部类 Node 的各字段
        Class<?> nodeClass = Class.forName("com.ds.binarysortedtree.BinarySortedTree$Node");
        dataField = nodeClass.getDeclaredField("data");
        dataField.setAccessible(true);
        parentField = nodeClass.getDeclaredField("parent");
        parentField.setAccessible(true);
        lChildField = nodeClass.getDeclaredField("lChild");
        lChildField.setAccessible(true);
        rChildField = nodeClass.getDeclaredField("rChild");
        rChildField.setAccessible(true);
    }

    // ==================== 反射辅助方法 ====================

    /** 获取根节点 */
    private Object root() throws Exception {
        return rootField.get(tree);
    }

    /** 获取节点数据，node 为 null 时返回 null */
    private Integer data(Object node) throws Exception {
        return node == null ? null : (Integer) dataField.get(node);
    }

    /** 获取父节点 */
    private Object parent(Object node) throws Exception {
        return parentField.get(node);
    }

    /** 获取左子节点 */
    private Object left(Object node) throws Exception {
        return node == null ? null : lChildField.get(node);
    }

    /** 获取右子节点 */
    private Object right(Object node) throws Exception {
        return node == null ? null : rChildField.get(node);
    }

    /** 中序遍历（左→根→右），BST 的中序遍历结果应为升序 */
    private List<Integer> inorder() throws Exception {
        List<Integer> result = new ArrayList<>();
        inorder(root(), result);
        return result;
    }

    private void inorder(Object node, List<Integer> result) throws Exception {
        if (node == null) {
            return;
        }
        inorder(left(node), result);
        result.add(data(node));
        inorder(right(node), result);
    }

    /** 统计树中节点个数 */
    private int countNodes() throws Exception {
        return countNodes(root());
    }

    private int countNodes(Object node) throws Exception {
        if (node == null) {
            return 0;
        }
        return 1 + countNodes(left(node)) + countNodes(right(node));
    }

    /**
     * 递归校验整棵树：
     * 1. 每个节点的 parent 指针正确；
     * 2. 满足 BST 性质（min < data < max）。
     */
    private void assertValidBst(Object node, Object expectedParent, Integer min, Integer max) throws Exception {
        if (node == null) {
            return;
        }
        int value = data(node);
        assertSame(expectedParent, parent(node),
                "节点 " + value + " 的 parent 指针不正确");
        if (min != null) {
            assertTrue(value > min, "节点 " + value + " 应大于 " + min);
        }
        if (max != null) {
            assertTrue(value < max, "节点 " + value + " 应小于 " + max);
        }
        assertValidBst(left(node), node, min, value);
        assertValidBst(right(node), node, value, max);
    }

    /** 批量插入，并断言每次都插入成功 */
    private void addAll(int... values) {
        for (int v : values) {
            assertTrue(tree.add(v), "插入 " + v + " 应成功");
        }
    }

    // ==================== 一、空树与首次插入 ====================

    @Test
    @DisplayName("新创建的空树 root 为 null")
    void testNewTreeIsEmpty() throws Exception {
        assertNull(root(), "新树的 root 应为 null");
        assertEquals(0, countNodes());
        assertTrue(inorder().isEmpty());
    }

    @Test
    @DisplayName("向空树添加元素：成为根节点")
    void testAddToEmptyTree() throws Exception {
        assertTrue(tree.add(50));

        Object root = root();
        assertNotNull(root, "插入后 root 不应为 null");
        assertEquals(50, data(root));
        assertNull(parent(root), "根节点的 parent 应为 null");
        assertNull(left(root), "根节点的左孩子应为 null");
        assertNull(right(root), "根节点的右孩子应为 null");
        assertEquals(1, countNodes());
    }

    // ==================== 二、左右子树插入位置 ====================

    @Test
    @DisplayName("比根小的值插入左子树")
    void testAddSmallerGoesLeft() throws Exception {
        addAll(50, 30);

        Object root = root();
        assertEquals(50, data(root));
        assertNull(right(root), "不应有右孩子");

        Object leftChild = left(root);
        assertNotNull(leftChild, "30 应成为根的左孩子");
        assertEquals(30, data(leftChild));
        assertSame(root, parent(leftChild), "左孩子的 parent 应指向根");
    }

    @Test
    @DisplayName("比根大的值插入右子树")
    void testAddLargerGoesRight() throws Exception {
        addAll(50, 70);

        Object root = root();
        assertEquals(50, data(root));
        assertNull(left(root), "不应有左孩子");

        Object rightChild = right(root);
        assertNotNull(rightChild, "70 应成为根的右孩子");
        assertEquals(70, data(rightChild));
        assertSame(root, parent(rightChild), "右孩子的 parent 应指向根");
    }

    // ==================== 三、重复元素 ====================

    @Test
    @DisplayName("添加与根相同的值：返回 false")
    void testAddDuplicateRoot() throws Exception {
        addAll(50);
        assertFalse(tree.add(50), "重复插入根节点的值应返回 false");
        assertEquals(1, countNodes(), "重复插入不应改变节点数");
    }

    @Test
    @DisplayName("添加重复值：返回 false 且树结构不变")
    void testAddDuplicateDoesNotModifyTree() throws Exception {
        addAll(50, 30, 70);

        assertFalse(tree.add(30), "重复插入 30 应返回 false");
        assertFalse(tree.add(70), "重复插入 70 应返回 false");
        assertEquals(3, countNodes(), "重复插入不应改变节点数");
        assertEquals(Arrays.asList(30, 50, 70), inorder(), "中序遍历结果不应改变");
    }

    // ==================== 四、标准树结构验证 ====================

    /**
     * 插入 50, 30, 70, 20, 40, 60, 80，应得到：
     * <pre>
     *        50
     *       /  \
     *      30   70
     *     / \   / \
     *    20 40 60 80
     * </pre>
     */
    @Test
    @DisplayName("插入序列 50,30,70,20,40,60,80：结构正确")
    void testBuildSampleTreeStructure() throws Exception {
        addAll(50, 30, 70, 20, 40, 60, 80);

        Object root = root();
        assertEquals(50, data(root));

        Object n30 = left(root);
        Object n70 = right(root);
        assertEquals(30, data(n30));
        assertEquals(70, data(n70));

        assertEquals(20, data(left(n30)));
        assertEquals(40, data(right(n30)));
        assertEquals(60, data(left(n70)));
        assertEquals(80, data(right(n70)));

        // 叶子节点不应再有孩子
        assertNull(left(left(n30)));
        assertNull(right(right(n70)));

        assertEquals(7, countNodes());
    }

    @Test
    @DisplayName("中序遍历结果为升序")
    void testInorderIsSorted() throws Exception {
        addAll(50, 30, 70, 20, 40, 60, 80);
        assertEquals(Arrays.asList(20, 30, 40, 50, 60, 70, 80), inorder());
    }

    @Test
    @DisplayName("所有节点的 parent 指针正确且满足 BST 性质")
    void testParentPointersAndBstProperty() throws Exception {
        addAll(50, 30, 70, 20, 40, 60, 80);
        assertValidBst(root(), null, null, null);
    }

    // ==================== 五、退化（链式）结构 ====================

    @Test
    @DisplayName("递减插入形成左斜树")
    void testLeftSkewedTree() throws Exception {
        addAll(50, 40, 30, 20, 10);

        Object cur = root();
        for (int expected = 50; expected >= 10; expected -= 10) {
            assertNotNull(cur);
            assertEquals(expected, data(cur));
            assertNull(right(cur), "左斜树不应有右孩子");
            cur = left(cur);
        }
        assertNull(cur, "左斜树末端应为 null");
        assertEquals(5, countNodes());
        assertEquals(Arrays.asList(10, 20, 30, 40, 50), inorder());
    }

    @Test
    @DisplayName("递增插入形成右斜树")
    void testRightSkewedTree() throws Exception {
        addAll(10, 20, 30, 40, 50);

        Object cur = root();
        for (int expected = 10; expected <= 50; expected += 10) {
            assertNotNull(cur);
            assertEquals(expected, data(cur));
            assertNull(left(cur), "右斜树不应有左孩子");
            cur = right(cur);
        }
        assertNull(cur, "右斜树末端应为 null");
        assertEquals(5, countNodes());
        assertEquals(Arrays.asList(10, 20, 30, 40, 50), inorder());
    }

    // ==================== 六、特殊值 ====================

    @Test
    @DisplayName("支持负数与零")
    void testAddNegativeAndZero() throws Exception {
        addAll(0, -50, 50, -100, -30, 30, 100);
        assertEquals(Arrays.asList(-100, -50, -30, 0, 30, 50, 100), inorder());
        assertValidBst(root(), null, null, null);
    }

    @Test
    @DisplayName("支持 int 边界值")
    void testAddIntegerEdgeValues() throws Exception {
        addAll(0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(Arrays.asList(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), inorder());
        assertValidBst(root(), null, null, null);
    }

    // ==================== 七、随机数据一致性校验 ====================

    @Test
    @DisplayName("随机插入：与 TreeSet 结果一致，且无重复")
    void testRandomInsertionsMatchTreeSet() throws Exception {
        Random random = new Random(42);
        TreeSet<Integer> expected = new TreeSet<>();

        for (int i = 0; i < 200; i++) {
            int value = random.nextInt(1000);
            boolean added = tree.add(value);
            assertEquals(expected.add(value), added,
                    "插入 " + value + " 的返回值应与 TreeSet 一致");
        }

        assertEquals(expected.size(), countNodes(), "节点数应与 TreeSet 一致");
        assertEquals(new ArrayList<>(expected), inorder(), "中序遍历应与 TreeSet 升序一致");
        assertValidBst(root(), null, null, null);
    }
}