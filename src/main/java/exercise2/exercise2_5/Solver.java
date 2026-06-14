package exercise2.exercise2_5;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.MinPQ;
import edu.princeton.cs.algs4.StdOut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A* 算法解决8字谜题
 * 支持3种启发函数：错位格子数、曼哈顿距离、曼哈顿距离平方和
 */
public class Solver {
    // 搜索节点：存储棋盘、当前步数、父节点、优先级
    private static class SearchNode implements Comparable<SearchNode> {
        Board board;
        int moves;
        SearchNode parent;
        int priority; // g(n) + h(n)

        public SearchNode(Board board, int moves, SearchNode parent, HeuristicType type) {
            this.board = board;
            this.moves = moves;
            this.parent = parent;
            // 计算优先级：当前步数 + 启发函数
            switch (type) {
                case HAMMING:
                    this.priority = moves + board.hamming();
                    break;
                case MANHATTAN:
                    this.priority = moves + board.manhattan();
                    break;
                case MANHATTAN_SQUARED:
                    this.priority = moves + board.manhattanSquared();
                    break;
                default:
                    throw new IllegalArgumentException("未知启发函数类型");
            }
        }

        // 按优先级升序排序（优先队列弹出优先级最小的节点）
        @Override
        public int compareTo(SearchNode o) {
            return Integer.compare(this.priority, o.priority);
        }
    }

    // 启发函数类型枚举
    public enum HeuristicType {
        HAMMING,       // 错位格子数
        MANHATTAN,     // 曼哈顿距离
        MANHATTAN_SQUARED // 曼哈顿距离平方和
    }

    private final SearchNode goalNode; // 目标节点
    private final int moves;           // 最少步数
    private final Iterable<Board> solution; // 解题路径

    // 构造函数：用指定启发函数求解
    public Solver(Board initial, HeuristicType type) {
        if (initial == null) throw new IllegalArgumentException("初始棋盘不能为空");

        // 优先队列（最小堆）：按优先级排序
        MinPQ<SearchNode> pq = new MinPQ<>();
        // closed集合：存储已访问的棋盘，避免重复搜索
        Map<Board, Integer> closed = new HashMap<>();

        // 插入初始节点
        pq.insert(new SearchNode(initial, 0, null, type));
        closed.put(initial, 0);

        SearchNode result = null;
        // A* 主循环
        while (!pq.isEmpty()) {
            // 弹出优先级最小的节点（当前最优路径）
            SearchNode current = pq.delMin();

            // 找到目标，结束搜索
            if (current.board.isGoal()) {
                result = current;
                break;
            }

            // 生成所有邻接节点
            for (Board neighbor : current.board.neighbors()) {
                int newMoves = current.moves + 1;
                // 检查是否已访问，且新步数更优
                if (!closed.containsKey(neighbor) || newMoves < closed.get(neighbor)) {
                    closed.put(neighbor, newMoves);
                    pq.insert(new SearchNode(neighbor, newMoves, current, type));
                }
            }
        }

        this.goalNode = result;
        this.moves = result != null ? result.moves : -1;
        this.solution = buildSolutionPath(result);
    }

    // 从目标节点回溯，构建解题路径
    private Iterable<Board> buildSolutionPath(SearchNode goal) {
        if (goal == null) return null;
        ArrayList<Board> path = new ArrayList<>();
        SearchNode current = goal;
        while (current != null) {
            path.add(current.board);
            current = current.parent;
        }
        // 反转路径，从初始状态到目标状态
        Collections.reverse(path);
        return path;
    }

    // 获取最少步数
    public int moves() {
        return moves;
    }

    // 获取解题路径
    public Iterable<Board> solution() {
        return solution;
    }

    // 测试主类
    public static void main(String[] args) {
        // 从文件读取初始棋盘（格式：第一行3，然后3行3列数字）
        In in = new In(args[0]);
        int n = in.readInt();
        int[][] tiles = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tiles[i][j] = in.readInt();
            }
        }
        Board initial = new Board(tiles);

        // 测试3种启发函数
        testHeuristic(initial, HeuristicType.HAMMING, "错位格子数");
        testHeuristic(initial, HeuristicType.MANHATTAN, "曼哈顿距离");
        testHeuristic(initial, HeuristicType.MANHATTAN_SQUARED, "曼哈顿距离平方和");
    }

    // 测试指定启发函数
    private static void testHeuristic(Board initial, HeuristicType type, String name) {
        StdOut.println("=== 测试启发函数：" + name + " ===");
        long startTime = System.currentTimeMillis();
        Solver solver = new Solver(initial, type);
        long endTime = System.currentTimeMillis();

        if (solver.moves() == -1) {
            StdOut.println("该棋盘无解");
            return;
        }

        StdOut.println("最少步数：" + solver.moves());
        StdOut.println("解题路径：");
        for (Board board : solver.solution()) {
            StdOut.println(board);
        }
        StdOut.println("耗时：" + (endTime - startTime) + "ms\n");
    }
}