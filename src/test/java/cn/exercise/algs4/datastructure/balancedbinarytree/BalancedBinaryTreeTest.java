package cn.exercise.algs4.datastructure.balancedbinarytree;

import cn.exercise.algs4.datastructure.balancedbinarytree.BalancedBinaryTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 平衡二叉树（AVL Tree）JUnit 5 单元测试
 *
 * @author kevin
 * @version 1.0
 */
@DisplayName("BalancedBinaryTree 测试")
class BalancedBinaryTreeTest {

    private BalancedBinaryTree tree;

    @BeforeEach
    void setUp() {
        tree = new BalancedBinaryTree();
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证 AVL 性质：任意节点左右子树高度差不超过 1
     * 通过中序遍历检查排序性 + 检查树高度上界来间接验证
     */
    private void assertAVLProperty(BalancedBinaryTree t) {
        // 中序遍历必须有序
        List<Integer> inorder = t.inorder();
        for (int i = 1; i < inorder.size(); i++) {
            assertTrue(inorder.get(i) > inorder.get(i - 1),
                    "中序遍历应严格递增，但在索引 " + i + " 处违反: " + inorder);
        }
        // AVL 树高度不超过 1.44 * log2(n+2) - 0.328
        if (t.size() > 0) {
            double maxExpectedHeight = 1.44 * (Math.log(t.size() + 2) / Math.log(2)) - 0.328;
            assertTrue(t.height() <= Math.ceil(maxExpectedHeight),
                    "树高度 " + t.height() + " 超过 AVL 理论上界 " + Math.ceil(maxExpectedHeight)
                            + "（n=" + t.size() + "）");
        }
    }

    // ==================== 构造方法测试 ====================

    @Nested
    @DisplayName("构造方法")
    class ConstructorTests {

        @Test
        @DisplayName("空构造：树为空")
        void emptyConstructor() {
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("批量构造：依次插入后仍保持 AVL 性质")
        void bulkConstructor() {
            BalancedBinaryTree t = new BalancedBinaryTree(5, 3, 7, 1, 4, 6, 8, 2);
            assertEquals(8, t.size());
            assertAVLProperty(t);
        }

        @Test
        @DisplayName("批量构造：含重复值，重复值应被忽略")
        void bulkConstructorWithDuplicates() {
            BalancedBinaryTree t = new BalancedBinaryTree(3, 3, 1, 1, 2, 2);
            assertEquals(3, t.size());
        }
    }

    // ==================== 增删查测试 ====================

    @Nested
    @DisplayName("add / contains / get / remove")
    class CRUDTests {

        @Test
        @DisplayName("添加单个元素")
        void addSingle() {
            assertTrue(tree.add(10));
            assertEquals(1, tree.size());
            assertTrue(tree.contains(10));
        }

        @Test
        @DisplayName("添加重复元素返回 false")
        void addDuplicate() {
            tree.add(10);
            assertFalse(tree.add(10));
            assertEquals(1, tree.size());
        }

        @Test
        @DisplayName("contains 不存在的元素返回 false")
        void containsNotExists() {
            tree.add(10);
            assertFalse(tree.contains(20));
        }

        @Test
        @DisplayName("get 存在的元素返回值")
        void getExists() {
            tree.add(42);
            assertEquals(42, tree.get(42));
        }

        @Test
        @DisplayName("get 不存在的元素返回 null")
        void getNotExists() {
            assertNull(tree.get(99));
        }

        @Test
        @DisplayName("删除叶节点")
        void removeLeaf() {
            tree.add(2);
            tree.add(1);
            tree.add(3);
            assertTrue(tree.remove(1));
            assertEquals(2, tree.size());
            assertFalse(tree.contains(1));
            assertAVLProperty(tree);
        }

        @Test
        @DisplayName("删除只有左子树的节点")
        void removeNodeWithLeftChildOnly() {
            tree.add(3);
            tree.add(1);
            tree.add(2);
            assertTrue(tree.remove(3));
            assertEquals(2, tree.size());
            assertFalse(tree.contains(3));
            assertAVLProperty(tree);
        }

        @Test
        @DisplayName("删除只有右子树的节点")
        void removeNodeWithRightChildOnly() {
            tree.add(1);
            tree.add(3);
            tree.add(2);
            assertTrue(tree.remove(1));
            assertEquals(2, tree.size());
            assertFalse(tree.contains(1));
            assertAVLProperty(tree);
        }

        @Test
        @DisplayName("删除同时有左右子树的节点")
        void removeNodeWithBothChildren() {
            tree.add(5);
            tree.add(3);
            tree.add(7);
            tree.add(2);
            tree.add(4);
            tree.add(6);
            tree.add(8);
            assertTrue(tree.remove(5));
            assertEquals(6, tree.size());
            assertFalse(tree.contains(5));
            assertAVLProperty(tree);
        }

        @Test
        @DisplayName("删除根节点")
        void removeRoot() {
            tree.add(5);
            assertTrue(tree.remove(5));
            assertTrue(tree.isEmpty());
        }

        @Test
        @DisplayName("删除不存在的元素返回 false")
        void removeNotExists() {
            tree.add(10);
            assertFalse(tree.remove(20));
            assertEquals(1, tree.size());
        }

        @Test
        @DisplayName("清空树")
        void clear() {
            tree.add(1);
            tree.add(2);
            tree.add(3);
            tree.clear();
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
        }
    }

    // ==================== AVL 平衡性验证 ====================

    @Nested
    @DisplayName("AVL 平衡性")
    class AVLBalanceTests {

        @Test
        @DisplayName("LL 型旋转：递增序列插入")
        void llRotation() {
            // 依次插入 1,2,3 触发 LL 型失衡（右旋）
            tree.add(1);
            tree.add(2);
            tree.add(3);
            assertAVLProperty(tree);
            assertEquals(Arrays.asList(1, 2, 3), tree.inorder());
        }

        @Test
        @DisplayName("RR 型旋转：递减序列插入")
        void rrRotation() {
            // 依次插入 3,2,1 触发 RR 型失衡（左旋）
            tree.add(3);
            tree.add(2);
            tree.add(1);
            assertAVLProperty(tree);
            assertEquals(Arrays.asList(1, 2, 3), tree.inorder());
        }

        @Test
        @DisplayName("LR 型旋转")
        void lrRotation() {
            // 依次插入 3,1,2 触发 LR 型失衡（先左旋再右旋）
            tree.add(3);
            tree.add(1);
            tree.add(2);
            assertAVLProperty(tree);
            assertEquals(Arrays.asList(1, 2, 3), tree.inorder());
        }

        @Test
        @DisplayName("RL 型旋转")
        void rlRotation() {
            // 依次插入 1,3,2 触发 RL 型失衡（先右旋再左旋）
            tree.add(1);
            tree.add(3);
            tree.add(2);
            assertAVLProperty(tree);
            assertEquals(Arrays.asList(1, 2, 3), tree.inorder());
        }

        @Test
        @DisplayName("大量随机插入后仍保持 AVL 性质")
        void randomInserts() {
            Random rand = new Random(42);
            Set<Integer> inserted = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                int val = rand.nextInt(10000);
                if (inserted.add(val)) {
                    tree.add(val);
                }
            }
            assertEquals(inserted.size(), tree.size());
            assertAVLProperty(tree);
        }

        @Test
        @DisplayName("大量随机删除后仍保持 AVL 性质")
        void randomDeletes() {
            Random rand = new Random(123);
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                values.add(i);
            }
            Collections.shuffle(values, rand);
            for (int v : values) {
                tree.add(v);
            }
            assertEquals(500, tree.size());

            // 随机删除一半
            Collections.shuffle(values, rand);
            for (int i = 0; i < 250; i++) {
                assertTrue(tree.remove(values.get(i)));
            }
            assertEquals(250, tree.size());
            assertAVLProperty(tree);

            // 剩余元素应仍有序
            List<Integer> remaining = tree.inorder();
            for (int i = 1; i < remaining.size(); i++) {
                assertTrue(remaining.get(i) > remaining.get(i - 1));
            }
        }
    }

    // ==================== 统计信息测试 ====================

    @Nested
    @DisplayName("统计信息")
    class StatsTests {

        @Test
        @DisplayName("size 和 isEmpty")
        void sizeAndIsEmpty() {
            assertTrue(tree.isEmpty());
            tree.add(1);
            assertFalse(tree.isEmpty());
            assertEquals(1, tree.size());
            tree.add(2);
            assertEquals(2, tree.size());
        }

        @Test
        @DisplayName("findMin 和 findMax")
        void findMinAndMax() {
            assertNull(tree.findMin());
            assertNull(tree.findMax());

            tree.add(5);
            tree.add(3);
            tree.add(8);
            tree.add(1);
            tree.add(9);
            assertEquals(1, (int) tree.findMin());
            assertEquals(9, (int) tree.findMax());
        }

        @Test
        @DisplayName("height：空树返回 -1")
        void heightEmpty() {
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("height：单节点返回 0")
        void heightSingleNode() {
            tree.add(1);
            assertEquals(0, tree.height());
        }

        @Test
        @DisplayName("height：AVL 树高度在合理范围内")
        void heightInBalance() {
            for (int i = 1; i <= 100; i++) {
                tree.add(i);
            }
            // 100 个节点的 AVL 树高度不超过 6（log2(100) ≈ 6.64）
            assertTrue(tree.height() <= 7);
        }
    }

    // ==================== 遍历测试 ====================

    @Nested
    @DisplayName("遍历")
    class TraversalTests {

        @Test
        @DisplayName("中序遍历结果为升序")
        void inorderTraversal() {
            tree.add(5);
            tree.add(3);
            tree.add(7);
            tree.add(1);
            tree.add(4);
            assertEquals(Arrays.asList(1, 3, 4, 5, 7), tree.inorder());
        }

        @Test
        @DisplayName("前序遍历")
        void preorderTraversal() {
            tree.add(5);
            tree.add(3);
            tree.add(7);
            List<Integer> pre = tree.preorder();
            // 前序第一个元素应为根（经过 AVL 调整后根可能变化）
            assertFalse(pre.isEmpty());
            // 排序后应与中序一致
            List<Integer> sorted = new ArrayList<>(pre);
            Collections.sort(sorted);
            assertEquals(sorted, tree.inorder());
        }

        @Test
        @DisplayName("后序遍历")
        void postorderTraversal() {
            tree.add(5);
            tree.add(3);
            tree.add(7);
            List<Integer> post = tree.postorder();
            List<Integer> sorted = new ArrayList<>(post);
            Collections.sort(sorted);
            assertEquals(sorted, tree.inorder());
        }

        @Test
        @DisplayName("层序遍历")
        void levelOrderTraversal() {
            tree.add(5);
            tree.add(3);
            tree.add(7);
            List<Integer> level = tree.levelOrder();
            assertFalse(level.isEmpty());
            assertEquals(tree.size(), level.size());
            List<Integer> sorted = new ArrayList<>(level);
            Collections.sort(sorted);
            assertEquals(sorted, tree.inorder());
        }

        @Test
        @DisplayName("空树遍历返回空列表")
        void emptyTreeTraversal() {
            assertTrue(tree.inorder().isEmpty());
            assertTrue(tree.preorder().isEmpty());
            assertTrue(tree.postorder().isEmpty());
            assertTrue(tree.levelOrder().isEmpty());
        }
    }

    // ==================== 转换与迭代测试 ====================

    @Nested
    @DisplayName("转换与迭代")
    class ConversionTests {

        @Test
        @DisplayName("toArray 返回升序数组")
        void toArrayTest() {
            tree.add(3);
            tree.add(1);
            tree.add(2);
            assertArrayEquals(new int[]{1, 2, 3}, tree.toArray());
        }

        @Test
        @DisplayName("toList 返回升序列表")
        void toListTest() {
            tree.add(3);
            tree.add(1);
            tree.add(2);
            assertEquals(Arrays.asList(1, 2, 3), tree.toList());
        }

        @Test
        @DisplayName("iterator 中序遍历")
        void iteratorTest() {
            tree.add(5);
            tree.add(3);
            tree.add(7);
            tree.add(1);
            tree.add(4);

            List<Integer> result = new ArrayList<>();
            for (int val : tree) {
                result.add(val);
            }
            assertEquals(Arrays.asList(1, 3, 4, 5, 7), result);
        }

        @Test
        @DisplayName("空树 iterator 无元素")
        void emptyIterator() {
            assertFalse(tree.iterator().hasNext());
        }

        @Test
        @DisplayName("toString 返回中序遍历字符串")
        void toStringTest() {
            tree.add(3);
            tree.add(1);
            tree.add(2);
            assertEquals("[1, 2, 3]", tree.toString());
        }
    }

    // ==================== 边界与压力测试 ====================

    @Nested
    @DisplayName("边界与压力")
    class EdgeCaseTests {

        @Test
        @DisplayName("连续递增插入 1000 个元素，验证平衡性")
        void sequentialInserts() {
            for (int i = 1; i <= 1000; i++) {
                tree.add(i);
            }
            assertEquals(1000, tree.size());
            assertAVLProperty(tree);
        }

        @Test
        @DisplayName("连续递减插入 1000 个元素，验证平衡性")
        void reverseSequentialInserts() {
            for (int i = 1000; i >= 1; i--) {
                tree.add(i);
            }
            assertEquals(1000, tree.size());
            assertAVLProperty(tree);
        }

        @Test
        @DisplayName("全部删除后树为空")
        void removeAll() {
            for (int i = 1; i <= 100; i++) {
                tree.add(i);
            }
            for (int i = 1; i <= 100; i++) {
                assertTrue(tree.remove(i));
            }
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("删除后再插入，树仍保持 AVL 性质")
        void removeAndReinsert() {
            for (int i = 1; i <= 50; i++) {
                tree.add(i);
            }
            for (int i = 1; i <= 25; i++) {
                tree.remove(i);
            }
            for (int i = 51; i <= 75; i++) {
                tree.add(i);
            }
            assertEquals(50, tree.size());
            assertAVLProperty(tree);
        }

        @Test
        @DisplayName("负数和零值正常处理")
        void negativeAndZero() {
            tree.add(0);
            tree.add(-5);
            tree.add(3);
            tree.add(-10);
            assertEquals(Arrays.asList(-10, -5, 0, 3), tree.inorder());
            assertAVLProperty(tree);
        }
    }
}
