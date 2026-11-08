package cn.exercise.algs4.datastructure.gist;

import java.util.List;

/**
 * GiST 的“可扩展部分”:由具体数据类型实现的一组回调。
 *
 * <p>GiST 之所以叫 <b>Generalized</b> Search Tree,是因为树的骨架(平衡、多路、按页分裂)
 * 与“键是什么、怎么算包围、怎么判命中”完全解耦。把这四个回调换成几何类型,
 * 这棵树就变成 R 树;换成标量区间,就是区间树;换成文本签名,就是签名树。</p>
 *
 * @param <K> 键(树的包围键)类型
 * @param <Q> 查询条件类型
 */
public interface GiSTOps<K, Q> {

    /**
     * 求两个键的“包围键”:结果必须覆盖两个输入键(对区间是并集,对几何是外接矩形)。
     */
    K union(K left, K right);

    /**
     * 把一个新键放进已有键之下需要付出的扩张代价。
     * 插入时每一层都选代价最小的子树下潜。
     */
    double penalty(K current, K addition);

    /**
     * 该键是否<b>可能</b>命中查询。返回 false 即可整棵子树剪掉;
     * 返回 true 只是“可能”,叶子条目还要再判定一次(真实 GiST 里叫 recheck)。
     */
    boolean consistent(K key, Q query);

    /** 两个键是否等价(用于判定覆盖是否已经足够,避免无谓的键更新)。 */
    boolean equal(K left, K right);

    /**
     * 页满时的分裂:返回与 {@code keys} 等长的布尔数组,
     * {@code true} 表示该条目留在原页,{@code false} 表示挪到新页。
     * 两组都必须非空。
     */
    boolean[] pickSplit(List<K> keys);
}
