package cn.exercise.algs4.datastructure.binarytree;

import cn.exercise.algs4.datastructure.binarytree.BinaryTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BinaryTree 的 JUnit5 单元测试
 *
 * 由于 Node 是 BinaryTree 的 private static 内部类，所有涉及 Node 类型的操作均通过反射完成。
 */
@DisplayName("BinaryTree 二叉树测试")
class BinaryTreeTest {

    private BinaryTree tree;
    private Class<?> nodeClass;
    private Class<?> nodeArrayClass;
    private Constructor<?> nodeConstructor;
    private Field lChildField;
    private Field rChildField;
    private Field dataField;

    /** 所有遍历方法名 */
    private static final String[] TRAVERSAL_METHODS = {
            "preorderTraversalRecursive",
            "inorderTraversalRecursive",
            "postorderTraversalRecursive",
            "preorderTraversalStack",
            "inorderTraversalStack",
            "postorderTraversalStack",
            "breadthFirstTraversal"
    };

    @BeforeEach
    void setUp() throws Exception {
        tree = new BinaryTree();

        // 反射获取私有静态内部类 Node
        nodeClass = Class.forName("cn.exercise.algs4.datastructure.binarytree.BinaryTree$Node");
        nodeConstructor = nodeClass.getDeclaredConstructor(Object.class);
        nodeConstructor.setAccessible(true);

        dataField = nodeClass.getDeclaredField("data");
        dataField.setAccessible(true);
        lChildField = nodeClass.getDeclaredField("lChild");
        lChildField.setAccessible(true);
        rChildField = nodeClass.getDeclaredField("rChild");
        rChildField.setAccessible(true);

        nodeArrayClass = Array.newInstance(nodeClass, 0).getClass();
    }

    // ==================== 反射辅助方法 ====================

    /** 创建一个 Node 实例 */
    private Object createNode(Object data) throws Exception {
        return nodeConstructor.newInstance(data);
    }

    /** 设置左子节点 */
    private void setLeft(Object parent, Object child) throws Exception {
        lChildField.set(parent, child);
    }

    /** 设置右子节点 */
    private void setRight(Object parent, Object child) throws Exception {
        rChildField.set(parent, child);
    }

    /** 获取节点数据 */
    private Object getData(Object node) throws Exception {
        return dataField.get(node);
    }

    /** 获取左子节点 */
    private Object getLeft(Object node) throws Exception {
        return lChildField.get(node);
    }

    /** 获取右子节点 */
    private Object getRight(Object node) throws Exception {
        return rChildField.get(node);
    }

    /** 反射调用遍历方法（因为 Node 类型私有，不能直接调用） */
    private void invokeTraversal(String methodName, Object root) {
        try {
            Method method = BinaryTree.class.getDeclaredMethod(methodName, nodeClass);
            method.invoke(tree, root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke " + methodName, e);
        }
    }

    /** 捕获遍历输出 */
    private String captureTraversal(String methodName, Object root) {
        return captureOutput(() -> invokeTraversal(methodName, root));
    }

    /** 捕获 System.out 输出 */
    private String captureOutput(Runnable runnable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(baos));
        try {
            runnable.run();
        } finally {
            System.setOut(original);
        }
        return baos.toString().trim();
    }

    /** 检查所有遍历方法对空 root 不抛异常 */
    private void assertAllTraversalsHandleNull() {
        for (String methodName : TRAVERSAL_METHODS) {
            assertDoesNotThrow(() -> invokeTraversal(methodName, null),
                    "方法 " + methodName + " 应处理 null root");
        }
    }

    /** 创建指定长度的 Node[] 数组（反射） */
    private Object createNodeArray(int length) {
        return Array.newInstance(nodeClass, length);
    }

    /** 设置 Node[] 数组元素 */
    private void setNodeArrayElement(Object array, int index, Object node) {
        Array.set(array, index, node);
    }

    /** 反射调用 buildByPreAndInOrder */
    private Object invokeBuildByPreAndInOrder(Object preArr, Object inArr) throws Exception {
        Method method = BinaryTree.class.getDeclaredMethod(
                "buildByPreAndInOrder", nodeArrayClass, nodeArrayClass);
        return method.invoke(tree, preArr, inArr);
    }

    /** 反射调用 buildByPostAndInOrder */
    private Object invokeBuildByPostAndInOrder(Object postArr, Object inArr) throws Exception {
        Method method = BinaryTree.class.getDeclaredMethod(
                "buildByPostAndInOrder", nodeArrayClass, nodeArrayClass);
        return method.invoke(tree, postArr, inArr);
    }

    /** 填充数组 */
    private void fillArray(Object array, Object[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            setNodeArrayElement(array, i, nodes[i]);
        }
    }

    // ==================== 测试用树结构 ====================

    /**
     * 标准测试树：
     * <pre>
     *        1
     *       / \
     *      2   3
     *     / \   \
     *    4   5   6
     * </pre>
     */
    private Object buildSampleTree() throws Exception {
        Object n1 = createNode(1);
        Object n2 = createNode(2);
        Object n3 = createNode(3);
        Object n4 = createNode(4);
        Object n5 = createNode(5);
        Object n6 = createNode(6);
        setLeft(n1, n2);
        setRight(n1, n3);
        setLeft(n2, n4);
        setRight(n2, n5);
        setRight(n3, n6);
        return n1;
    }

    /**
     * 完全二叉树：
     * <pre>
     *        1
     *       / \
     *      2   3
     *     / \ / \
     *    4  5 6  7
     * </pre>
     */
    private Object buildCompleteTree() throws Exception {
        Object n1 = createNode(1);
        Object n2 = createNode(2);
        Object n3 = createNode(3);
        Object n4 = createNode(4);
        Object n5 = createNode(5);
        Object n6 = createNode(6);
        Object n7 = createNode(7);
        setLeft(n1, n2);
        setRight(n1, n3);
        setLeft(n2, n4);
        setRight(n2, n5);
        setLeft(n3, n6);
        setRight(n3, n7);
        return n1;
    }

    /**
     * 字符串数据的树：
     * <pre>
     *        A
     *       / \
     *      B   C
     *     / \
     *    D   E
     * </pre>
     */
    private Object buildStringTree() throws Exception {
        Object a = createNode("A");
        Object b = createNode("B");
        Object c = createNode("C");
        Object d = createNode("D");
        Object e = createNode("E");
        setLeft(a, b);
        setRight(a, c);
        setLeft(b, d);
        setRight(b, e);
        return a;
    }

    // ==================== 一、递归遍历测试 ====================

    @Test
    @DisplayName("先序递归遍历：根→左→右")
    void testPreorderRecursive() throws Exception {
        assertEquals("1 2 4 5 3 6",
                captureTraversal("preorderTraversalRecursive", buildSampleTree()));
    }

    @Test
    @DisplayName("中序递归遍历：左→根→右")
    void testInorderRecursive() throws Exception {
        assertEquals("4 2 5 1 3 6",
                captureTraversal("inorderTraversalRecursive", buildSampleTree()));
    }

    @Test
    @DisplayName("后序递归遍历：左→右→根")
    void testPostorderRecursive() throws Exception {
        assertEquals("4 5 2 6 3 1",
                captureTraversal("postorderTraversalRecursive", buildSampleTree()));
    }

    @Test
    @DisplayName("String 节点：先序递归")
    void testStringTreePreorder() throws Exception {
        assertEquals("A B D E C",
                captureTraversal("preorderTraversalRecursive", buildStringTree()));
    }

    @Test
    @DisplayName("String 节点：中序递归")
    void testStringTreeInorder() throws Exception {
        assertEquals("D B E A C",
                captureTraversal("inorderTraversalRecursive", buildStringTree()));
    }

    @Test
    @DisplayName("String 节点：后序递归")
    void testStringTreePostorder() throws Exception {
        assertEquals("D E B C A",
                captureTraversal("postorderTraversalRecursive", buildStringTree()));
    }

    @Test
    @DisplayName("完全二叉树：先序递归")
    void testCompleteTreePreorder() throws Exception {
        assertEquals("1 2 4 5 3 6 7",
                captureTraversal("preorderTraversalRecursive", buildCompleteTree()));
    }

    @Test
    @DisplayName("完全二叉树：中序递归")
    void testCompleteTreeInorder() throws Exception {
        assertEquals("4 2 5 1 6 3 7",
                captureTraversal("inorderTraversalRecursive", buildCompleteTree()));
    }

    @Test
    @DisplayName("完全二叉树：后序递归")
    void testCompleteTreePostorder() throws Exception {
        assertEquals("4 5 2 6 7 3 1",
                captureTraversal("postorderTraversalRecursive", buildCompleteTree()));
    }

    // ==================== 二、栈迭代遍历测试 ====================

    @Test
    @DisplayName("先序栈遍历：根→左→右")
    void testPreorderStack() throws Exception {
        assertEquals("1 2 4 5 3 6",
                captureTraversal("preorderTraversalStack", buildSampleTree()));
    }

    @Test
    @DisplayName("中序栈遍历：左→根→右")
    void testInorderStack() throws Exception {
        assertEquals("4 2 5 1 3 6",
                captureTraversal("inorderTraversalStack", buildSampleTree()));
    }

    @Test
    @DisplayName("后序栈遍历：左→右→根")
    void testPostorderStack() throws Exception {
        assertEquals("4 5 2 6 3 1",
                captureTraversal("postorderTraversalStack", buildSampleTree()));
    }

    @Test
    @DisplayName("递归与栈遍历结果一致（完全二叉树）")
    void testRecursiveMatchesIterative() throws Exception {
        Object root = buildCompleteTree();

        String preRec = captureTraversal("preorderTraversalRecursive", root);
        String preStk = captureTraversal("preorderTraversalStack", root);
        assertEquals(preRec, preStk, "先序遍历：递归和栈结果应一致");

        String inRec = captureTraversal("inorderTraversalRecursive", root);
        String inStk = captureTraversal("inorderTraversalStack", root);
        assertEquals(inRec, inStk, "中序遍历：递归和栈结果应一致");

        String postRec = captureTraversal("postorderTraversalRecursive", root);
        String postStk = captureTraversal("postorderTraversalStack", root);
        assertEquals(postRec, postStk, "后序遍历：递归和栈结果应一致");
    }

    // ==================== 三、层序遍历测试 ====================

    @Test
    @DisplayName("层序遍历：逐层从左到右")
    void testBFS() throws Exception {
        assertEquals("1 2 3 4 5 6",
                captureTraversal("breadthFirstTraversal", buildSampleTree()));
    }

    @Test
    @DisplayName("完全二叉树层序遍历")
    void testCompleteTreeBFS() throws Exception {
        assertEquals("1 2 3 4 5 6 7",
                captureTraversal("breadthFirstTraversal", buildCompleteTree()));
    }

    // ==================== 四、边界情况测试 ====================

    @Test
    @DisplayName("空树（null）的所有遍历不抛异常")
    void testNullRootTraversals() {
        assertAllTraversalsHandleNull();
    }

    @Test
    @DisplayName("单节点树：所有遍历结果一致")
    void testSingleNodeTree() throws Exception {
        Object single = createNode(42);
        String expected = "42";

        for (String methodName : TRAVERSAL_METHODS) {
            assertEquals(expected, captureTraversal(methodName, single),
                    "方法 " + methodName + " 单节点结果不正确");
        }
    }

    @Test
    @DisplayName("只有左子树的链式结构")
    void testLeftSkewedTree() throws Exception {
        //    1
        //   /
        //  2
        // /
        //3
        Object n1 = createNode(1);
        Object n2 = createNode(2);
        Object n3 = createNode(3);
        setLeft(n1, n2);
        setLeft(n2, n3);

        assertEquals("1 2 3", captureTraversal("preorderTraversalRecursive", n1));
        assertEquals("3 2 1", captureTraversal("inorderTraversalRecursive", n1));
        assertEquals("3 2 1", captureTraversal("postorderTraversalRecursive", n1));
        assertEquals("1 2 3", captureTraversal("breadthFirstTraversal", n1));
    }

    @Test
    @DisplayName("只有右子树的链式结构")
    void testRightSkewedTree() throws Exception {
        //1
        // \
        //  2
        //   \
        //    3
        Object n1 = createNode(1);
        Object n2 = createNode(2);
        Object n3 = createNode(3);
        setRight(n1, n2);
        setRight(n2, n3);

        assertEquals("1 2 3", captureTraversal("preorderTraversalRecursive", n1));
        assertEquals("1 2 3", captureTraversal("inorderTraversalRecursive", n1));
        assertEquals("3 2 1", captureTraversal("postorderTraversalRecursive", n1));
        assertEquals("1 2 3", captureTraversal("breadthFirstTraversal", n1));
    }

    @Test
    @DisplayName("每个节点只有一个子节点的混合树")
    void testSingleChildTree() throws Exception {
        //      1
        //     / \
        //    2   3
        //   /   /
        //  4   5
        Object n1 = createNode(1);
        Object n2 = createNode(2);
        Object n3 = createNode(3);
        Object n4 = createNode(4);
        Object n5 = createNode(5);
        setLeft(n1, n2);
        setRight(n1, n3);
        setLeft(n2, n4);
        setLeft(n3, n5);

        assertEquals("1 2 4 3 5", captureTraversal("preorderTraversalRecursive", n1));
        assertEquals("4 2 1 5 3", captureTraversal("inorderTraversalRecursive", n1));
        assertEquals("4 2 5 3 1", captureTraversal("postorderTraversalRecursive", n1));
        assertEquals("1 2 3 4 5", captureTraversal("breadthFirstTraversal", n1));
    }

    // ==================== 五、先序+中序构建二叉树测试 ====================

    @Test
    @DisplayName("根据先序+中序构建标准二叉树")
    void testBuildByPreAndInOrder() throws Exception {
        // 先序：1 2 4 5 3 6    中序：4 2 5 1 3 6
        Object preArr = createNodeArray(6);
        Object inArr = createNodeArray(6);
        fillArray(preArr, new Object[]{
                createNode(1), createNode(2), createNode(4),
                createNode(5), createNode(3), createNode(6)
        });
        fillArray(inArr, new Object[]{
                createNode(4), createNode(2), createNode(5),
                createNode(1), createNode(3), createNode(6)
        });

        Object root = invokeBuildByPreAndInOrder(preArr, inArr);

        assertNotNull(root);
        assertEquals(1, getData(root));
        assertEquals("1 2 4 5 3 6", captureTraversal("preorderTraversalRecursive", root));
        assertEquals("4 2 5 1 3 6", captureTraversal("inorderTraversalRecursive", root));
    }

    @Test
    @DisplayName("根据先序+中序构建只有左子树")
    void testBuildByPreAndInOrderLeftSkewed() throws Exception {
        // 先序：1 2 3    中序：3 2 1
        Object preArr = createNodeArray(3);
        Object inArr = createNodeArray(3);
        fillArray(preArr, new Object[]{createNode(1), createNode(2), createNode(3)});
        fillArray(inArr, new Object[]{createNode(3), createNode(2), createNode(1)});

        Object root = invokeBuildByPreAndInOrder(preArr, inArr);

        assertNotNull(root);
        assertEquals(1, getData(root));
        assertNotNull(getLeft(root));
        assertNull(getRight(root));
        assertEquals("1 2 3", captureTraversal("preorderTraversalRecursive", root));
    }

    @Test
    @DisplayName("根据先序+中序构建只有右子树")
    void testBuildByPreAndInOrderRightSkewed() throws Exception {
        // 先序：1 2 3    中序：1 2 3
        Object preArr = createNodeArray(3);
        Object inArr = createNodeArray(3);
        fillArray(preArr, new Object[]{createNode(1), createNode(2), createNode(3)});
        fillArray(inArr, new Object[]{createNode(1), createNode(2), createNode(3)});

        Object root = invokeBuildByPreAndInOrder(preArr, inArr);

        assertNotNull(root);
        assertNull(getLeft(root));
        assertNotNull(getRight(root));
        assertEquals("1 2 3", captureTraversal("preorderTraversalRecursive", root));
    }

    @Test
    @DisplayName("先序+中序构建：null 返回 null")
    void testBuildByPreAndInOrderNullInput() throws Exception {
        Method method = BinaryTree.class.getDeclaredMethod(
                "buildByPreAndInOrder", nodeArrayClass, nodeArrayClass);
        assertNull(method.invoke(tree, (Object) null, (Object) null));

        Object emptyArr = createNodeArray(0);
        assertNull(method.invoke(tree, emptyArr, emptyArr));
    }

    @Test
    @DisplayName("先序+中序构建：长度不匹配返回 null")
    void testBuildByPreAndInOrderLengthMismatch() throws Exception {
        Object preArr = createNodeArray(3);
        Object inArr = createNodeArray(2);
        assertNull(invokeBuildByPreAndInOrder(preArr, inArr));
    }

    @Test
    @DisplayName("先序+中序构建：String 类型数据")
    void testBuildByPreAndInOrderString() throws Exception {
        // 先序：A B D E C    中序：D B E A C
        Object preArr = createNodeArray(5);
        Object inArr = createNodeArray(5);
        fillArray(preArr, new Object[]{
                createNode("A"), createNode("B"), createNode("D"),
                createNode("E"), createNode("C")
        });
        fillArray(inArr, new Object[]{
                createNode("D"), createNode("B"), createNode("E"),
                createNode("A"), createNode("C")
        });

        Object root = invokeBuildByPreAndInOrder(preArr, inArr);
        assertNotNull(root);
        assertEquals("A", getData(root));
        assertEquals("A B D E C", captureTraversal("preorderTraversalRecursive", root));
        assertEquals("D B E A C", captureTraversal("inorderTraversalRecursive", root));
    }

    // ==================== 六、后序+中序构建二叉树测试 ====================

    @Test
    @DisplayName("根据后序+中序构建标准二叉树")
    void testBuildByPostAndInOrder() throws Exception {
        // 后序：4 5 2 6 3 1    中序：4 2 5 1 3 6
        Object postArr = createNodeArray(6);
        Object inArr = createNodeArray(6);
        fillArray(postArr, new Object[]{
                createNode(4), createNode(5), createNode(2),
                createNode(6), createNode(3), createNode(1)
        });
        fillArray(inArr, new Object[]{
                createNode(4), createNode(2), createNode(5),
                createNode(1), createNode(3), createNode(6)
        });

        Object root = invokeBuildByPostAndInOrder(postArr, inArr);

        assertNotNull(root);
        assertEquals(1, getData(root));
        assertEquals("1 2 4 5 3 6", captureTraversal("preorderTraversalRecursive", root));
        assertEquals("4 2 5 1 3 6", captureTraversal("inorderTraversalRecursive", root));
        assertEquals("4 5 2 6 3 1", captureTraversal("postorderTraversalRecursive", root));
    }

    @Test
    @DisplayName("根据后序+中序构建完全二叉树")
    void testBuildByPostAndInOrderCompleteTree() throws Exception {
        // 后序：4 5 2 6 7 3 1    中序：4 2 5 1 6 3 7
        Object postArr = createNodeArray(7);
        Object inArr = createNodeArray(7);
        fillArray(postArr, new Object[]{
                createNode(4), createNode(5), createNode(2),
                createNode(6), createNode(7), createNode(3), createNode(1)
        });
        fillArray(inArr, new Object[]{
                createNode(4), createNode(2), createNode(5),
                createNode(1), createNode(6), createNode(3), createNode(7)
        });

        Object root = invokeBuildByPostAndInOrder(postArr, inArr);

        assertNotNull(root);
        assertEquals("1 2 4 5 3 6 7", captureTraversal("preorderTraversalRecursive", root));
        assertEquals("4 2 5 1 6 3 7", captureTraversal("inorderTraversalRecursive", root));
        assertEquals("4 5 2 6 7 3 1", captureTraversal("postorderTraversalRecursive", root));
    }

    @Test
    @DisplayName("后序+中序构建：null/空数组返回 null")
    void testBuildByPostAndInOrderNullInput() throws Exception {
        Method method = BinaryTree.class.getDeclaredMethod(
                "buildByPostAndInOrder", nodeArrayClass, nodeArrayClass);
        assertNull(method.invoke(tree, (Object) null, (Object) null));

        Object emptyArr = createNodeArray(0);
        assertNull(method.invoke(tree, emptyArr, emptyArr));
    }

    @Test
    @DisplayName("后序+中序构建：长度不匹配返回 null")
    void testBuildByPostAndInOrderLengthMismatch() throws Exception {
        Object postArr = createNodeArray(3);
        Object inArr = createNodeArray(2);
        assertNull(invokeBuildByPostAndInOrder(postArr, inArr));
    }

    @Test
    @DisplayName("后序+中序构建：String 类型数据")
    void testBuildByPostAndInOrderString() throws Exception {
        // 后序：D E B C A    中序：D B E A C
        Object postArr = createNodeArray(5);
        Object inArr = createNodeArray(5);
        fillArray(postArr, new Object[]{
                createNode("D"), createNode("E"), createNode("B"),
                createNode("C"), createNode("A")
        });
        fillArray(inArr, new Object[]{
                createNode("D"), createNode("B"), createNode("E"),
                createNode("A"), createNode("C")
        });

        Object root = invokeBuildByPostAndInOrder(postArr, inArr);
        assertNotNull(root);
        assertEquals("A", getData(root));
        assertEquals("A B D E C", captureTraversal("preorderTraversalRecursive", root));
        assertEquals("D B E A C", captureTraversal("inorderTraversalRecursive", root));
    }

    // ==================== 七、交叉验证测试 ====================

    @Test
    @DisplayName("递归与栈遍历一致性（多种树结构）")
    void testAllTraversalsConsistency() throws Exception {
        // 完全二叉树
        verifyTraversalConsistency(buildCompleteTree());

        // 左链树
        Object leftSkewed = createNode(1);
        Object cur = leftSkewed;
        for (int i = 2; i <= 5; i++) {
            Object child = createNode(i);
            setLeft(cur, child);
            cur = child;
        }
        verifyTraversalConsistency(leftSkewed);

        // 右链树
        Object rightSkewed = createNode(1);
        cur = rightSkewed;
        for (int i = 2; i <= 5; i++) {
            Object child = createNode(i);
            setRight(cur, child);
            cur = child;
        }
        verifyTraversalConsistency(rightSkewed);
    }

    /** 验证递归与栈遍历一致 */
    private void verifyTraversalConsistency(Object root) {
        assertEquals(captureTraversal("preorderTraversalRecursive", root),
                captureTraversal("preorderTraversalStack", root),
                "先序不一致");
        assertEquals(captureTraversal("inorderTraversalRecursive", root),
                captureTraversal("inorderTraversalStack", root),
                "中序不一致");
        assertEquals(captureTraversal("postorderTraversalRecursive", root),
                captureTraversal("postorderTraversalStack", root),
                "后序不一致");
    }

    @Test
    @DisplayName("两种构建方式生成相同树结构")
    void testBothBuildMethodsProduceSameTree() throws Exception {
        // 树的先序：1 2 4 5 3 6  中序：4 2 5 1 3 6  后序：4 5 2 6 3 1
        Object[] preNodeArr = new Object[]{
                createNode(1), createNode(2), createNode(4),
                createNode(5), createNode(3), createNode(6)
        };
        Object[] inNodeArr1 = new Object[]{
                createNode(4), createNode(2), createNode(5),
                createNode(1), createNode(3), createNode(6)
        };
        Object[] postNodeArr = new Object[]{
                createNode(4), createNode(5), createNode(2),
                createNode(6), createNode(3), createNode(1)
        };
        Object[] inNodeArr2 = new Object[]{
                createNode(4), createNode(2), createNode(5),
                createNode(1), createNode(3), createNode(6)
        };

        Object preArr = createNodeArray(6);
        Object inArr1 = createNodeArray(6);
        Object postArr = createNodeArray(6);
        Object inArr2 = createNodeArray(6);
        fillArray(preArr, preNodeArr);
        fillArray(inArr1, inNodeArr1);
        fillArray(postArr, postNodeArr);
        fillArray(inArr2, inNodeArr2);

        Object rootFromPre = invokeBuildByPreAndInOrder(preArr, inArr1);
        Object rootFromPost = invokeBuildByPostAndInOrder(postArr, inArr2);

        assertEquals(captureTraversal("preorderTraversalRecursive", rootFromPre),
                captureTraversal("preorderTraversalRecursive", rootFromPost),
                "两种构建方式应生成相同树结构");
    }

    @Test
    @DisplayName("构建结果节点指针正确")
    void testBuiltTreeStructure() throws Exception {
        // 先序：2 1    中序：1 2  => 根=2, 左子=1
        Object preArr = createNodeArray(2);
        Object inArr = createNodeArray(2);
        fillArray(preArr, new Object[]{createNode(2), createNode(1)});
        fillArray(inArr, new Object[]{createNode(1), createNode(2)});

        Object root = invokeBuildByPreAndInOrder(preArr, inArr);

        assertNotNull(root);
        assertEquals(2, getData(root));
        assertNull(getRight(root), "右孩子应为 null");
        assertNotNull(getLeft(root), "左孩子不应为 null");
        assertEquals(1, getData(getLeft(root)));
    }
}
