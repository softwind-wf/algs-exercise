package cn.exercise.algs4.datastructure.tree.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 二叉树遍历算法工具类 —— 遍历逻辑与树结构解耦的统一收口
 * <p>
 * 原仓库中先/中/后/层序遍历在 BinaryTree、BinarySortedTree、BalancedBinaryTree、MorrisTraversal、
 * ThreadedBinaryTree 等多处重复实现。本工具把这些算法集中到一处，通过一个极轻量的节点访问器
 * {@link NodeAccessor} 与具体节点类型解耦：只要你能回答"节点的值/左孩子/右孩子是什么"，
 * 就能复用这里的全部遍历算法(递归、栈迭代、Morris)。
 * </p>
 * <p>
 * 三种形态：
 * <ul>
 *     <li>递归版：preorder / inorder / postorder —— 最直观，树深时受调用栈限制；</li>
 *     <li>栈迭代版：preorderStack / inorderStack / postorderStack —— 无递归深度限制，O(h) 空间；</li>
 *     <li>Morris 版：preorderMorris / inorderMorris —— O(1) 额外空间(临时建立线索并还原)。</li>
 * </ul>
 * Morris 需要临时改写右指针，因此要求 {@link MutableNodeAccessor}。遍历结束后线索全部还原，
 * 树的原始结构不变。
 * </p>
 */
public final class TreeTraversals {

    private TreeTraversals() {
        throw new AssertionError("工具类不可实例化");
    }

    /**
     * 只读节点访问器：让遍历算法能读取任意二叉树节点的值、左孩子、右孩子
     *
     * @param <N> 节点类型
     * @param <E> 元素类型
     */
    public interface NodeAccessor<N, E> {
        E data(N node);

        N left(N node);

        N right(N node);
    }

    /**
     * 可变节点访问器：额外支持改写右指针，供 Morris 遍历建立/还原线索
     *
     * @param <N> 节点类型
     * @param <E> 元素类型
     */
    public interface MutableNodeAccessor<N, E> extends NodeAccessor<N, E> {
        void setRight(N node, N right);
    }

    // ==================== 递归版 ====================

    public static <N, E> List<E> preorder(N root, NodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        preorder(root, a, result);
        return result;
    }

    public static <N, E> List<E> inorder(N root, NodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        inorder(root, a, result);
        return result;
    }

    public static <N, E> List<E> postorder(N root, NodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        postorder(root, a, result);
        return result;
    }

    private static <N, E> void preorder(N node, NodeAccessor<N, E> a, List<E> result) {
        if (node == null) {
            return;
        }
        result.add(a.data(node));
        preorder(a.left(node), a, result);
        preorder(a.right(node), a, result);
    }

    private static <N, E> void inorder(N node, NodeAccessor<N, E> a, List<E> result) {
        if (node == null) {
            return;
        }
        inorder(a.left(node), a, result);
        result.add(a.data(node));
        inorder(a.right(node), a, result);
    }

    private static <N, E> void postorder(N node, NodeAccessor<N, E> a, List<E> result) {
        if (node == null) {
            return;
        }
        postorder(a.left(node), a, result);
        postorder(a.right(node), a, result);
        result.add(a.data(node));
    }

    // ==================== 栈迭代版 ====================

    public static <N, E> List<E> preorderStack(N root, NodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Deque<N> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            N node = stack.pop();
            result.add(a.data(node));
            if (a.right(node) != null) {
                stack.push(a.right(node));
            }
            if (a.left(node) != null) {
                stack.push(a.left(node));
            }
        }
        return result;
    }

    public static <N, E> List<E> inorderStack(N root, NodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        Deque<N> stack = new ArrayDeque<>();
        N node = root;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = a.left(node);
            }
            node = stack.pop();
            result.add(a.data(node));
            node = a.right(node);
        }
        return result;
    }

    public static <N, E> List<E> postorderStack(N root, NodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Deque<N> work = new ArrayDeque<>();
        Deque<N> out = new ArrayDeque<>();
        work.push(root);
        while (!work.isEmpty()) {
            N node = work.pop();
            out.push(node);
            if (a.left(node) != null) {
                work.push(a.left(node));
            }
            if (a.right(node) != null) {
                work.push(a.right(node));
            }
        }
        while (!out.isEmpty()) {
            result.add(a.data(out.pop()));
        }
        return result;
    }

    // ==================== 层序(队列) ====================

    public static <N, E> List<E> levelOrder(N root, NodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<N> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            N node = queue.poll();
            result.add(a.data(node));
            if (a.left(node) != null) {
                queue.offer(a.left(node));
            }
            if (a.right(node) != null) {
                queue.offer(a.right(node));
            }
        }
        return result;
    }

    // ==================== Morris 版(O(1) 空间) ====================

    public static <N, E> List<E> inorderMorris(N root, MutableNodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        N cur = root;
        N pre = null;
        while (cur != null) {
            if (a.left(cur) == null) {
                result.add(a.data(cur));
                cur = a.right(cur);
            } else {
                pre = a.left(cur);
                while (a.right(pre) != null && a.right(pre) != cur) {
                    pre = a.right(pre);
                }
                if (a.right(pre) == null) {
                    a.setRight(pre, cur);       // 建立线索
                    cur = a.left(cur);
                } else {
                    result.add(a.data(cur));
                    a.setRight(pre, null);      // 还原线索
                    cur = a.right(cur);
                }
            }
        }
        return result;
    }

    public static <N, E> List<E> preorderMorris(N root, MutableNodeAccessor<N, E> a) {
        List<E> result = new ArrayList<>();
        N cur = root;
        N pre = null;
        while (cur != null) {
            if (a.left(cur) == null) {
                result.add(a.data(cur));
                cur = a.right(cur);
            } else {
                pre = a.left(cur);
                while (a.right(pre) != null && a.right(pre) != cur) {
                    pre = a.right(pre);
                }
                if (a.right(pre) == null) {
                    result.add(a.data(cur));    // 先序在建线索前访问当前节点
                    a.setRight(pre, cur);
                    cur = a.left(cur);
                } else {
                    a.setRight(pre, null);      // 还原线索
                    cur = a.right(cur);
                }
            }
        }
        return result;
    }
}
