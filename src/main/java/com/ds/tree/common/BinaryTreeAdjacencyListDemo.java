package com.ds.tree.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

/**
 * 二叉搜索树 × 邻接表 数据持久化演示
 *
 * <p>核心思想：树是"非线性"结构，数据库行是"线性"的 —— 持久化就是把树线性化。</p>
 * <p>这里采用【邻接表】(Adjacency List) 方案：每行一个节点，记录父节点 id 与左右标记：</p>
 * <pre>
 *   bst_nodes 表（每行一个节点）
 *   ┌────┬─────────┬─────────┬─────────┐
 *   │ id │ value   │ parent_id│ is_left │
 *   ├────┼─────────┼─────────┼─────────┤
 *   │ 1  │  50     │  NULL   │  NULL   │  ← 根
 *   │ 2  │  30     │   1     │   1     │  ← 50 的左孩子
 *   │ 3  │  70     │   1     │   0     │  ← 50 的右孩子
 *   │ ...│         │         │         │
 *   └────┴─────────┴─────────┴─────────┘
 * </pre>
 *
 * <p>持久化：BFS 层序遍历，访问每个节点 INSERT 一行（id 由分配序号、父节点 id 用 Map 记录）。</p>
 * <p>恢复：SELECT 全表 → ① 每行 new 一个 Node 放进 HashMap&lt;id,Node&gt;
 * ② 遍历每行把 parent/left/right 指针接上 → ③ 根节点挂到新树的 root 字段。</p>
 *
 * <p>需要与 {@link AbstractBinaryTree} 同包：其 {@code Node}、{@code root}、{@code size}
 * 均为 protected/包可见成员，只有同包才能直接访问。</p>
 *
 * <p>连接信息从 {@code src/main/resources/db.properties} 读取。</p>
 */
public class BinaryTreeAdjacencyListDemo {

    private static final String DB_NAME = "tree_demo";

    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   二叉搜索树 × 邻接表 持久化 演示             ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        // ── 1. 建一棵二叉搜索树 ──
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        int[] data = {50, 30, 70, 20, 40, 60, 80, 35, 65};
        for (int v : data) {
            tree.insert(v);
        }
        System.out.println("🌳 原始二叉搜索树（" + tree.size() + " 个节点）:");
        tree.printTree();
        System.out.println("   中序: " + tree.inorder());

        // ── 2. 建库建表（邻接表） ──
        Properties props = loadDbConfig();
        initDb(props);

        // ── 3. BFS 层序 → 邻接表 持久化 ──
        int treeId = persistAsAdjacencyList(tree, props);
        System.out.println("💾 已按邻接表写入 " + DB_NAME + ".bst_nodes（tree_id=" + treeId + "）");

        // ── 4. 展示"扁平"的邻接表行 ──
        printTable(props, treeId);

        // ── 5. 从数据库恢复成一棵树 ──
        BinarySearchTree<Integer> restored = restoreFromDb(treeId, props);
        System.out.println("🌲 已从邻接表恢复二叉搜索树");

        // ── 6. 一致性校验：原树 vs 恢复树 ──
        verify(tree, restored);

        // ── 7. 进阶：MySQL 8 递归 CTE 直接在库内查子树 ──
        demoRecursiveCte(props, treeId, 30);
    }

    // ====================== 持久化（树 → 邻接表行） ======================

    /**
     * BFS 层序遍历整棵树，为每个节点分配自增序号作为 id，
     * 以邻接表方式（id, value, parent_id, is_left）逐行写入 bst_nodes 表。
     *
     * @return 本次持久化使用的 tree_id（库中最大 +1）
     */
    private static int persistAsAdjacencyList(BinarySearchTree<Integer> tree, Properties props) throws Exception {
        int treeId;
        String url = jdbcUrl(props);
        try (Connection conn = DriverManager.getConnection(url, props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(tree_id), 0) + 1 FROM " + DB_NAME + ".bst_nodes")) {
            rs.next();
            treeId = rs.getInt(1);
        }

        // 节点引用 → 分配的 id（Node 未重写 equals，用普通 HashMap 即可）
        Map<AbstractBinaryTree.Node<Integer>, Integer> idMap = new HashMap<>();
        int nextId = 1;

        String insertSql = "INSERT INTO " + DB_NAME + ".bst_nodes (tree_id, id, node_value, parent_id, is_left) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url, props.getProperty("user"), props.getProperty("password"));
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            // BFS：根入队，边出队边为左右孩子分配 id 并入队
            Queue<AbstractBinaryTree.Node<Integer>> queue = new LinkedList<>();
            queue.offer(tree.root);
            idMap.put(tree.root, nextId++);

            while (!queue.isEmpty()) {
                AbstractBinaryTree.Node<Integer> cur = queue.poll();
                int curId = idMap.get(cur);

                // 从 parent 指针推导：父节点 id + 左右标记（根为 NULL）
                Integer parentId = (cur.parent == null) ? null : idMap.get(cur.parent);
                Integer isLeft = (cur.parent == null) ? null : (cur == cur.parent.left ? 1 : 0);

                ps.setInt(1, treeId);
                ps.setInt(2, curId);
                ps.setInt(3, cur.data);
                if (parentId == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, parentId);
                if (isLeft == null)  ps.setNull(5, Types.TINYINT);  else ps.setInt(5, isLeft);
                ps.addBatch();

                if (cur.left != null) { idMap.put(cur.left, nextId++); queue.offer(cur.left); }
                if (cur.right != null) { idMap.put(cur.right, nextId++); queue.offer(cur.right); }
            }
            ps.executeBatch();
        }
        return treeId;
    }

    // ====================== 恢复（邻接表行 → 树） ======================

    /**
     * 从 bst_nodes 表读回邻接表行，重建二叉搜索树。
     * <p>两趟法：</p>
     * <ol>
     *     <li>每行 new 一个 Node 放进 HashMap&lt;id, Node&gt;；</li>
     *     <li>遍历每行，把 parent / left / right 指针接上，parent_id 为 NULL 的行即根。</li>
     * </ol>
     */
    private static BinarySearchTree<Integer> restoreFromDb(int treeId, Properties props) throws Exception {
        String sql = "SELECT id, node_value, parent_id, is_left FROM " + DB_NAME + ".bst_nodes "
                + "WHERE tree_id = ? ORDER BY id";
        Map<Integer, AbstractBinaryTree.Node<Integer>> nodes = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(props),
                props.getProperty("user"), props.getProperty("password"));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, treeId);

            // 第一遍：只建节点对象、填数据
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AbstractBinaryTree.Node<Integer> node =
                            new AbstractBinaryTree.Node<>(rs.getInt("node_value"));
                    nodes.put(rs.getInt("id"), node);
                }
            }

            // 第二遍：接指针（重开一次查询，或复用上面结果——这里重开以保证游标简单）
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int parentId = rs.getInt("parent_id");
                    if (rs.wasNull()) {
                        continue; // 根：parent_id 为 NULL
                    }
                    AbstractBinaryTree.Node<Integer> node = nodes.get(id);
                    AbstractBinaryTree.Node<Integer> parent = nodes.get(parentId);
                    node.parent = parent;
                    int isLeft = rs.getInt("is_left");
                    if (isLeft == 1) {
                        parent.left = node;
                    } else {
                        parent.right = node;
                    }
                }
            }
        }

        // 找出根并挂到新树（Node/root/size 均为 protected/包可见，同包可直接访问）
        BinarySearchTree<Integer> restored = new BinarySearchTree<>();
        for (Map.Entry<Integer, AbstractBinaryTree.Node<Integer>> e : nodes.entrySet()) {
            if (e.getValue().parent == null) {
                restored.root = e.getValue();
                break;
            }
        }
        restored.size = nodes.size();
        return restored;
    }

    // ====================== 验证 ======================

    /** 原树与恢复树在三种遍历上的对比 + 树形打印 */
    private static void verify(BinarySearchTree<Integer> original, BinarySearchTree<Integer> restored) {
        System.out.println("\n──────────────── 一致性校验 ────────────────");
        System.out.println("  原树   中序: " + original.inorder());
        System.out.println("  恢复树 中序: " + restored.inorder());
        System.out.println("  中序一致: " + (original.inorder().equals(restored.inorder()) ? "✅" : "❌"));
        System.out.println("  前序一致: " + (original.preorder().equals(restored.preorder()) ? "✅" : "❌"));
        System.out.println("  层序一致: " + (original.levelOrder().equals(restored.levelOrder()) ? "✅" : "❌"));
        System.out.println("  节点数一致: " + (original.size() == restored.size() ? "✅" : "❌"));

        System.out.println("\n  恢复树结构（横向打印）:");
        restored.printTree();
    }

    // ====================== 查询演示 ======================

    /** 打印 bst_nodes 表中该树的所有邻接表行（展示"扁平"的数据） */
    private static void printTable(Properties props, int treeId) throws Exception {
        System.out.println("\n──────────────── 邻接表行（扁平的数据库数据） ────────────────");
        System.out.println("  id | 值  | parent_id | is_left(1左/0右)");
        System.out.println("  ────────────────────────────────────────");
        String sql = "SELECT id, node_value, parent_id, is_left FROM " + DB_NAME + ".bst_nodes "
                + "WHERE tree_id = ? ORDER BY id";
        try (Connection conn = DriverManager.getConnection(jdbcUrl(props),
                props.getProperty("user"), props.getProperty("password"));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, treeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int parentId = rs.getInt("parent_id");
                    String parent = rs.wasNull() ? "NULL" : String.valueOf(parentId);
                    int isLeft = rs.getInt("is_left");
                    String left = rs.wasNull() ? "NULL" : (isLeft == 1 ? "左" : "右");
                    System.out.printf("  %-4d| %-3d | %-9s | %s%n",
                            rs.getInt("id"), rs.getInt("node_value"), parent, left);
                }
            }
        }
    }

    /** 进阶：MySQL 8 递归 CTE —— 一条 SQL 直接在库内查出以某值为根的子树 */
    private static void demoRecursiveCte(Properties props, int treeId, int rootValue) throws Exception {
        System.out.println("\n──────────────── 递归 CTE 库内查子树（以 " + rootValue + " 为根） ────────────────");
        String sql = "WITH RECURSIVE subtree AS ("
                + "  SELECT id, node_value, parent_id, is_left, 0 AS depth "
                + "  FROM " + DB_NAME + ".bst_nodes WHERE tree_id = ? AND node_value = ?"
                + "  UNION ALL"
                + "  SELECT n.id, n.node_value, n.parent_id, n.is_left, s.depth + 1 "
                + "  FROM " + DB_NAME + ".bst_nodes n JOIN subtree s ON n.parent_id = s.id"
                + ") SELECT node_value, depth FROM subtree ORDER BY depth, id";
        try (Connection conn = DriverManager.getConnection(jdbcUrl(props),
                props.getProperty("user"), props.getProperty("password"));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, treeId);
            ps.setInt(2, rootValue);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("  · 值=" + rs.getInt("node_value")
                            + "  距根深度=" + rs.getInt("depth"));
                }
            }
        }
    }

    // ====================== 基础设施 ======================

    /** 从 db.properties 读取连接配置：优先 classpath，其次相对路径兜底 */
    private static Properties loadDbConfig() throws IOException {
        Properties props = new Properties();
        String[] candidates = {"/db.properties", "src/main/resources/db.properties", "db.properties"};
        for (String path : candidates) {
            InputStream in = null;
            if (path.startsWith("/")) {
                in = BinaryTreeAdjacencyListDemo.class.getResourceAsStream(path);
            }
            if (in == null) {
                java.io.File f = new java.io.File(path);
                if (f.exists()) {
                    in = new FileInputStream(f);
                }
            }
            if (in != null) {
                try (InputStream is = in) {
                    props.load(is);
                }
                return props;
            }
        }
        throw new IOException("找不到 db.properties，请确认文件存在于 src/main/resources/");
    }

    /** 连接 URL（不带库名，便于建库） */
    private static String jdbcUrl(Properties props) {
        return "jdbc:mysql://localhost:" + props.getProperty("port", "3306")
                + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";
    }

    /** 建库建表（邻接表：每行一个节点） */
    private static void initDb(Properties props) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl(props),
                props.getProperty("user"), props.getProperty("password"));
             Statement st = conn.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME
                    + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            st.execute("CREATE TABLE IF NOT EXISTS " + DB_NAME + ".bst_nodes ("
                    + "  tree_id INT NOT NULL COMMENT '树实例标识',"
                    + "  id INT NOT NULL COMMENT '节点分配序号(树内自增)',"
                    + "  node_value INT NOT NULL COMMENT '节点值',"
                    + "  parent_id INT NULL COMMENT '父节点id(NULL=根)',"
                    + "  is_left TINYINT NULL COMMENT '1=父的左孩子, 0=父的右孩子, NULL=根',"
                    + "  PRIMARY KEY (tree_id, id),"
                    + "  KEY idx_value (node_value)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二叉树邻接表'");
        }
        System.out.println("🛠 库表就绪: " + DB_NAME + ".bst_nodes");
    }
}
