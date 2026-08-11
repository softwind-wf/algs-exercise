package cn.exercise.algs4.datastructure.tree.common;

/**
 * 普通二叉排序树(泛型版) —— 公共抽象层的"零代码子类"演示
 * <p>
 * 全部行为(增删查、四种遍历、迭代器、打印、统计)都来自 {@link AbstractBinaryTree} 与 {@link AbstractBST}，
 * 本类自身不写任何逻辑——这直观地展示了"整合公共层"后，普通 BST 的实现成本趋近于零。
 * </p>
 *
 * @param <E> 元素类型，须可比较以保证有序性
 */
public class BinarySearchTree<E extends Comparable<E>> extends AbstractBST<E> {

    public BinarySearchTree() {
        super();
    }
}
