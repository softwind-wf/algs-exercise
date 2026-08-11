package cn.exercise.algs4.datastructure.tree.common;

import cn.exercise.algs4.datastructure.tree.common.AbstractBinaryTree;
import cn.exercise.algs4.datastructure.tree.common.BinarySearchTree;
import cn.exercise.algs4.datastructure.tree.common.RedBlackTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 泛型红黑树测试 —— 验证"第三种平衡策略"只需覆盖 insert/remove 即可挂到公共抽象层
 */
@DisplayName("RedBlackTree 泛型红黑树(整合可行性)")
class RedBlackTreeTest {

    private RedBlackTree<Integer> tree;

    @BeforeEach
    void setUp() {
        tree = new RedBlackTree<>();
    }

    // ==================== 红黑五性质校验(同包访问 color/root) ====================

    /** 校验根黑、无连续红链(红节点不能有红孩子)、每条根叶路径黑节点数相同 */
    private static boolean isRedBlackValid(RedBlackTree<?> t) {
        if (t.root == null) {
            return true;
        }
        if (t.root.color) {
            return false;                       // 性质1：根必须为黑
        }
        int[] blackCount = {-1};
        return checkRB(t.root, 0, blackCount);
    }

    private static boolean checkRB(AbstractBinaryTree.Node<?> n, int blacks, int[] blackCount) {
        if (n == null) {
            if (blackCount[0] == -1) {
                blackCount[0] = blacks;
            }
            return blacks == blackCount[0];     // 性质4：所有根叶路径黑节点数相同
        }
        if (!n.color) {
            blacks++;
        } else {
            if ((n.left != null && n.left.color) || (n.right != null && n.right.color)) {
                return false;                   // 性质3：红节点不能有红孩子
            }
        }
        return checkRB(n.left, blacks, blackCount) && checkRB(n.right, blacks, blackCount);
    }

    @Nested
    @DisplayName("有序性验证")
    class OrderTest {

        @Test
        @DisplayName("升序插入后中序仍升序")
        void ascendingInsertStaysSorted() {
            for (int i = 1; i <= 500; i++) {
                assertTrue(tree.insert(i));
            }
            List<Integer> expected = new ArrayList<>();
            for (int i = 1; i <= 500; i++) {
                expected.add(i);
            }
            assertEquals(expected, tree.inorder());
        }

        @Test
        @DisplayName("随机插入后与排序集合一致")
        void randomInsertMatchesSortedSet() {
            Random rnd = new Random(42);
            List<Integer> inserted = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                int v = rnd.nextInt(100_000);
                if (tree.insert(v)) {
                    inserted.add(v);
                }
            }
            Collections.sort(inserted);
            assertEquals(inserted, tree.inorder());
            assertEquals(inserted.size(), tree.size());
        }

        @Test
        @DisplayName("contains / findMin / findMax 正确")
        void queries() {
            for (int v : new int[]{10, 20, 5, 30, 15, 25}) {
                tree.insert(v);
            }
            assertTrue(tree.contains(15));
            assertFalse(tree.contains(99));
            assertEquals(5, tree.findMin());
            assertEquals(30, tree.findMax());
            assertNull(tree.get(99));
        }
    }

    @Nested
    @DisplayName("红黑性质验证")
    class PropertyTest {

        @Test
        @DisplayName("升序插入 1000 个元素后满足红黑五性质")
        void propertiesHoldAfterAscendingInsert() {
            for (int i = 1; i <= 1000; i++) {
                tree.insert(i);
            }
            assertTrue(isRedBlackValid(tree), "升序插入后红黑性质被破坏");
            // 红黑树高度 <= 2*log2(n+1)，约 20；退化成链则是 1000
            assertTrue(tree.height() <= 25, "红黑树高度异常: " + tree.height());
        }

        @Test
        @DisplayName("随机插入+随机删除后仍满足红黑五性质且数据正确")
        void propertiesHoldAfterRandomDelete() {
            Random rnd = new Random(7);
            List<Integer> present = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                int v = rnd.nextInt(50_000);
                if (tree.insert(v)) {
                    present.add(v);
                }
            }
            Collections.shuffle(present, rnd);
            for (int i = 0; i < 400; i++) {
                assertTrue(tree.remove(present.get(i)), "删除 " + present.get(i) + " 失败");
            }
            assertTrue(isRedBlackValid(tree), "删除后红黑性质被破坏");
            assertTrue(tree.height() <= 25, "删除后高度异常: " + tree.height());
            // 剩余数据与删除结果一致
            List<Integer> remaining = present.subList(400, present.size());
            Collections.sort(remaining);
            assertEquals(remaining, tree.inorder());
        }
    }

    @Nested
    @DisplayName("与普通 BST 的差异对比")
    class ContrastTest {

        @Test
        @DisplayName("升序插入:BST 退化,红黑树保持平衡(第三种策略同样有效)")
        void beatsBstOnAscending() {
            BinarySearchTree<Integer> bst = new BinarySearchTree<>();
            for (int i = 1; i <= 500; i++) {
                bst.insert(i);
                tree.insert(i);
            }
            assertEquals(499, bst.height());
            assertTrue(tree.height() <= 20, "红黑树升序插入高度应接近 2*log2(n),实际 " + tree.height());
            assertEquals(bst.inorder(), tree.inorder());   // 同一抽象层、同一种树逻辑
        }
    }
}
