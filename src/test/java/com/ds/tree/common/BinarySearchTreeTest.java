package com.ds.tree.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 泛型普通二叉排序树测试 —— 验证"公共抽象层"可承载完整的 BST 行为
 */
@DisplayName("BinarySearchTree 泛型普通二叉排序树(整合可行性)")
class BinarySearchTreeTest {

    private BinarySearchTree<Integer> tree;

    @BeforeEach
    void setUp() {
        tree = new BinarySearchTree<>();
    }

    @Nested
    @DisplayName("增删查")
    class CrudTest {

        @Test
        @DisplayName("插入后中序升序")
        void inorderAfterInsert() {
            tree.insert(5);
            tree.insert(3);
            tree.insert(8);
            tree.insert(1);
            tree.insert(4);
            tree.insert(7);
            assertEquals(Arrays.asList(1, 3, 4, 5, 7, 8), tree.inorder());
        }

        @Test
        @DisplayName("重复插入返回 false")
        void duplicateInsertRejected() {
            assertTrue(tree.insert(5));
            assertFalse(tree.insert(5));
            assertEquals(1, tree.size());
        }

        @Test
        @DisplayName("null 插入抛出异常")
        void nullInsertThrows() {
            assertThrows(IllegalArgumentException.class, () -> tree.insert(null));
        }

        @Test
        @DisplayName("删除叶子/单子/双子节点")
        void removeVariousShapes() {
            for (int v : new int[]{50, 30, 70, 20, 40, 60, 80}) {
                tree.insert(v);
            }
            assertTrue(tree.remove(20));                    // 叶子
            assertTrue(tree.remove(70));                    // 双子(前驱替换)
            assertTrue(tree.remove(30));                    // 单左子
            assertFalse(tree.remove(999));                  // 不存在
            assertEquals(Arrays.asList(40, 50, 60, 80), tree.inorder());
        }

        @Test
        @DisplayName("contains / get 查询")
        void containsAndGet() {
            tree.insert(10);
            tree.insert(20);
            assertTrue(tree.contains(10));
            assertFalse(tree.contains(15));
            assertEquals(20, tree.get(20));
            assertNull(tree.get(15));
        }

        @Test
        @DisplayName("findMin / findMax")
        void minMax() {
            tree.insert(42);
            tree.insert(7);
            tree.insert(99);
            assertEquals(7, tree.findMin());
            assertEquals(99, tree.findMax());
            assertNull(new BinarySearchTree<Integer>().findMin());
        }
    }

    @Nested
    @DisplayName("遍历与迭代")
    class TraversalTest {

        @Test
        @DisplayName("四种遍历与手工期望一致")
        void allTraversals() {
            //          5
            //        /   \
            //       3     8
            //      / \   /
            //     1   4 7
            tree.insert(5);
            tree.insert(3);
            tree.insert(8);
            tree.insert(1);
            tree.insert(4);
            tree.insert(7);
            assertEquals(Arrays.asList(5, 3, 1, 4, 8, 7), tree.preorder());
            assertEquals(Arrays.asList(1, 3, 4, 5, 7, 8), tree.inorder());
            assertEquals(Arrays.asList(1, 4, 3, 7, 8, 5), tree.postorder());
            assertEquals(Arrays.asList(5, 3, 8, 1, 4, 7), tree.levelOrder());
        }

        @Test
        @DisplayName("foreach 迭代与中序一致")
        void foreachIteration() {
            for (int v : new int[]{9, 1, 5, 3, 7}) {
                tree.insert(v);
            }
            List<Integer> collected = new ArrayList<>();
            for (Integer v : tree) {
                collected.add(v);
            }
            assertEquals(Arrays.asList(1, 3, 5, 7, 9), collected);
        }

        @Test
        @DisplayName("迭代器越界抛出异常")
        void iteratorExhaustedThrows() {
            tree.insert(1);
            Iterator<Integer> it = tree.iterator();
            it.next();
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
        }
    }

    @Nested
    @DisplayName("统计与边界")
    class BoundaryTest {

        @Test
        @DisplayName("size / isEmpty / clear / height")
        void stats() {
            assertTrue(tree.isEmpty());
            tree.insert(5);
            tree.insert(3);
            tree.insert(8);
            assertEquals(3, tree.size());
            assertEquals(1, tree.height());    // 5(左右3,8),根高1
            tree.clear();
            assertTrue(tree.isEmpty());
            assertEquals(-1, tree.height());
        }

        @Test
        @DisplayName("toString 输出中序")
        void toStringIsInorder() {
            tree.insert(2);
            tree.insert(1);
            tree.insert(3);
            assertEquals("[1, 2, 3]", tree.toString());
        }
    }
}
