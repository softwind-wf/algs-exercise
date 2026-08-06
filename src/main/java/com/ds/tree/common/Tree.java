package com.ds.tree.common;

import java.util.Iterator;
import java.util.List;

/**
 * 通用树统一接口 —— 所有"按值组织"的树结构的最小公共契约
 * <p>
 * 声明的是每棵树(二叉树、AVL、红黑、B 树……)都具备的操作：
 * 增、删、统计、高度、中序/层序遍历与迭代。
 * 前序/后序等二叉树专属操作、contains/有序查找等有序树专属操作
 * 不放入此接口——它们不是"所有树"的共性。
 * </p>
 *
 * @param <E> 元素类型
 */
public interface Tree<E> extends Iterable<E> {

    /**
     * 向树中添加数据(不允许重复)
     *
     * @param value 待添加的数据
     * @return true 添加成功；false 值已存在，添加失败
     */
    boolean insert(E value);

    /**
     * 从树中删除数据
     *
     * @param value 待删除的数据
     * @return true 删除成功；false 值不存在
     */
    boolean remove(E value);

    /** 返回树中元素个数 */
    int size();

    /** 判断树是否为空 */
    boolean isEmpty();

    /** 清空树中所有元素 */
    void clear();

    /**
     * 计算树的高度
     *
     * @return 树的高度；空树返回 -1
     */
    int height();

    /** 中序遍历(有序树为升序序列) */
    List<E> inorder();

    /** 层序遍历(广度优先) */
    List<E> levelOrder();

    /** 返回迭代器(按树的自然次序) */
    @Override
    Iterator<E> iterator();
}
