package cn.exercise.algs4.datastructure.tree.common;

import cn.exercise.algs4.datastructure.tree.common.AbstractMultiTree;
import cn.exercise.algs4.datastructure.tree.common.ListTree;
import cn.exercise.algs4.datastructure.tree.common.SiblingTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多叉树抽象层测试 —— 同一棵树对 ListTree(孩子列表)与 SiblingTree(左孩子右兄弟)双跑，
 * 验证三个存储原语之上的全部遍历/统计逻辑在两种表示下行为一致
 */
@DisplayName("AbstractMultiTree 多叉树抽象层(ListTree + SiblingTree)")
class AbstractMultiTreeTest {

    /** 向空树中填充一棵 A(B(E,F), C, D(G)) */
    private static void buildInto(AbstractMultiTree<String> tree) {
        AbstractMultiTree.Node<String> a = tree.setRoot("A");
        AbstractMultiTree.Node<String> b = tree.addChild(a, "B");
        tree.addChild(a, "C");
        AbstractMultiTree.Node<String> d = tree.addChild(a, "D");
        tree.addChild(b, "E");
        tree.addChild(b, "F");
        tree.addChild(d, "G");
    }

    /** 两种实现的同一棵树 */
    private static AbstractMultiTree<String>[] newTrees() {
        ListTree<String> lt = new ListTree<>();
        buildInto(lt);
        SiblingTree<String> st = new SiblingTree<>();
        buildInto(st);
        return new AbstractMultiTree[]{lt, st};
    }

    /** 提取孩子列表的 data */
    private static List<String> dataOf(List<AbstractMultiTree.Node<String>> nodes) {
        List<String> data = new ArrayList<>();
        for (AbstractMultiTree.Node<String> n : nodes) {
            data.add(n.data);
        }
        return data;
    }

    @Nested
    @DisplayName("遍历")
    class TraversalTest {

        @Test
        @DisplayName("先/后/层序与手工期望一致(两种实现)")
        void allTraversals() {
            for (AbstractMultiTree<String> tree : newTrees()) {
                assertEquals(Arrays.asList("A", "B", "E", "F", "C", "D", "G"), tree.preorder(),
                        "先序错误");
                assertEquals(Arrays.asList("E", "F", "B", "C", "G", "D", "A"), tree.postorder(),
                        "后序错误");
                assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F", "G"), tree.levelOrder(),
                        "层序错误");
            }
        }

        @Test
        @DisplayName("栈迭代版与递归版一致")
        void stackMatchesRecursive() {
            for (AbstractMultiTree<String> tree : newTrees()) {
                assertEquals(tree.preorder(), tree.preorderStack());
                assertEquals(tree.postorder(), tree.postorderStack());
            }
        }

        @Test
        @DisplayName("空树遍历为空")
        void emptyTraversals() {
            for (AbstractMultiTree<?> tree : new AbstractMultiTree[]{new ListTree<>(), new SiblingTree<>()}) {
                assertEquals(Collections.emptyList(), tree.preorder());
                assertEquals(Collections.emptyList(), tree.postorder());
                assertEquals(Collections.emptyList(), tree.levelOrder());
            }
        }
    }

    @Nested
    @DisplayName("结构与统计")
    class StatsTest {

        @Test
        @DisplayName("size / height / isLeaf / getRoot")
        void stats() {
            for (AbstractMultiTree<String> tree : newTrees()) {
                assertEquals(7, tree.size());
                assertEquals(2, tree.height(), "A→B→E 深度为 2");
                assertFalse(tree.isLeaf(tree.getRoot()));
                // A 的第二个孩子 C 是叶子
                List<AbstractMultiTree.Node<String>> aChildren = tree.childrenOf(tree.getRoot());
                assertTrue(tree.isLeaf(aChildren.get(1)), "C 是叶子");
                assertFalse(tree.isLeaf(aChildren.get(0)), "B 不是叶子");
            }
        }

        @Test
        @DisplayName("空树与单节点边界")
        void boundary() {
            for (AbstractMultiTree<?> tree : new AbstractMultiTree[]{new ListTree<>(), new SiblingTree<>()}) {
                assertTrue(tree.isEmpty());
                assertEquals(-1, tree.height());
                assertEquals(0, tree.size());
            }
            for (AbstractMultiTree<String> single : new AbstractMultiTree[]{new ListTree<>("S"), new SiblingTree<>("S")}) {
                assertEquals(1, single.size());
                assertEquals(0, single.height());
                assertTrue(single.isLeaf(single.getRoot()));
                assertEquals(Arrays.asList("S"), single.preorder());
            }
        }
    }

    @Nested
    @DisplayName("迭代器与输出")
    class IteratorTest {

        @Test
        @DisplayName("迭代器按先序输出,与 preorder 一致")
        void iteratorIsPreorder() {
            for (AbstractMultiTree<String> tree : newTrees()) {
                List<String> collected = new ArrayList<>();
                for (Iterator<String> it = tree.iterator(); it.hasNext(); ) {
                    collected.add(it.next());
                }
                assertEquals(tree.preorder(), collected);
            }
        }

        @Test
        @DisplayName("迭代器越界抛出异常")
        void iteratorExhausted() {
            ListTree<Integer> t = new ListTree<>(1);
            Iterator<Integer> it = t.iterator();
            assertEquals(Integer.valueOf(1), it.next());
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test
        @DisplayName("toString 为先序序列")
        void toStringIsPreorder() {
            for (AbstractMultiTree<String> tree : newTrees()) {
                assertEquals("[A, B, E, F, C, D, G]", tree.toString());
            }
        }
    }

    @Nested
    @DisplayName("两种表示对拍")
    class CrossCheckTest {

        @Test
        @DisplayName("同一棵树在 ListTree 与 SiblingTree 下行为完全一致")
        void twoImplsAgree() {
            ListTree<String> lt = new ListTree<>();
            buildInto(lt);
            SiblingTree<String> st = new SiblingTree<>();
            buildInto(st);
            assertEquals(lt.size(), st.size());
            assertEquals(lt.height(), st.height());
            assertEquals(lt.preorder(), st.preorder());
            assertEquals(lt.postorder(), st.postorder());
            assertEquals(lt.levelOrder(), st.levelOrder());
            assertEquals(lt.toString(), st.toString());
            // 孩子结构逐层一致
            assertEquals(dataOf(lt.childrenOf(lt.getRoot())), dataOf(st.childrenOf(st.getRoot())));
        }

        @Test
        @DisplayName("SiblingTree 左孩子右兄弟链结构正确")
        void siblingChainCorrect() {
            SiblingTree<String> tree = new SiblingTree<>();
            buildInto(tree);
            AbstractMultiTree.Node<String> a = tree.getRoot();
            // childrenOf 走 firstChild/nextSibling 链收集
            List<AbstractMultiTree.Node<String>> aChildren = tree.childrenOf(a);
            assertEquals(3, aChildren.size());
            assertEquals(Arrays.asList("B", "C", "D"), dataOf(aChildren));
            assertEquals(Arrays.asList("E", "F"), dataOf(tree.childrenOf(aChildren.get(0))));
            assertTrue(tree.childrenOf(aChildren.get(1)).isEmpty(), "C 应为叶子");
            assertEquals(Arrays.asList("G"), dataOf(tree.childrenOf(aChildren.get(2))));
            // parent 指针维护正确
            assertEquals(a, aChildren.get(2).parent);
        }
    }
}
