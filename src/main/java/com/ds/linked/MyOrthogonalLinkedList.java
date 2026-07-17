package com.ds.linked;

/**
 * 十字链表 —— 用于高效存储和操作稀疏矩阵
 * <p>
 * 核心思想：
 * 只存储矩阵中的非零元素，每个非零元素作为一个节点，
 * 同时链接到所在行和所在列的链表中，形成十字交叉结构。</p>
 * <p>
 * 节点结构：
 *   - row, col : 该元素在矩阵中的行号和列号
 *   - data     : 元素的值
 *   - right    : 指向同一行中下一个非零元素
 *   - down     : 指向同一列中下一个非零元素</p>
 * <p>
 * 优势：
 *   - 空间效率 O(非零元素个数)，远优于稠密存储的 O(行×列)
 *   - 按行或按列遍历同样高效
 *   - 插入/删除任意位置的元素均为 O(行+列) 而非 O(行×列)</p>
 *
 * @param <E> 矩阵元素类型
 */
public class MyOrthogonalLinkedList<E> {

    // ==================== 十字链表节点内部类 ====================

    /**
     * 十字链表节点
     * 每个节点代表稀疏矩阵中的一个非零元素，
     * 同时属于所在行的行链表和所在列的列链表
     */
    public static class OrthogonalNode<E> {
        int row;                  // 行号（从 0 开始）
        int col;                  // 列号（从 0 开始）
        E data;                   // 元素值
        OrthogonalNode<E> right;  // 指向同一行中下一个节点
        OrthogonalNode<E> down;   // 指向同一列中下一个节点

        OrthogonalNode(int row, int col, E data) {
            this.row = row;
            this.col = col;
            this.data = data;
            this.right = null;
            this.down = null;
        }
    }

    // ==================== 成员变量 ====================

    private final int rows;                     // 矩阵总行数
    private final int cols;                     // 矩阵总列数
    private OrthogonalNode<E>[] rowHead;  // 行头节点数组（行链表的哨兵头节点）
    private OrthogonalNode<E>[] colHead;  // 列头节点数组（列链表的哨兵头节点）
    private int size;                     // 非零元素个数

    private static final int DEFAULT_CAPACITY = 10;

    // ==================== 构造方法 ====================

    /**
     * 创建指定行列数的空十字链表
     */
    public MyOrthogonalLinkedList(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("行数和列数必须大于 0");
        }
        this.rows = rows;
        this.cols = cols;
        this.size = 0;
        initHeads();
    }

    /**
     * 创建默认行列数（10×10）的空十字链表
     */
    public MyOrthogonalLinkedList() {
        this(DEFAULT_CAPACITY, DEFAULT_CAPACITY);
    }

    /**
     * 从二维数组构造十字链表
     * 只将非 null 元素加入链表
     */
    public MyOrthogonalLinkedList(E[][] matrix) {
        this(matrix.length, matrix[0].length);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] != null) {
                    insert(i, j, matrix[i][j]);
                }
            }
        }
    }

    /**
     * 初始化行头和列头哨兵节点
     */
    @SuppressWarnings("unchecked")
    private void initHeads() {
        rowHead = new OrthogonalNode[rows];
        colHead = new OrthogonalNode[cols];

        // 为每一行创建头节点（哨兵），头节点本身不存储数据
        for (int i = 0; i < rows; i++) {
            rowHead[i] = new OrthogonalNode<>(i, -1, null);
            rowHead[i].right = rowHead[i];  // 自环，表示空行
        }

        // 为每一列创建头节点（哨兵），头节点本身不存储数据
        for (int j = 0; j < cols; j++) {
            colHead[j] = new OrthogonalNode<>(-1, j, null);
            colHead[j].down = colHead[j];   // 自环，表示空列
        }
    }

    // ==================== 基础查询 ====================

    public int size() {
        return size;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // ==================== 插入操作 ====================

    /**
     * 在指定位置插入一个元素
     * 如果该位置已有元素，则覆盖旧值
     *
     * @param row  行号
     * @param col  列号
     * @param data 元素值（为 null 时相当于删除）
     */
    public void insert(int row, int col, E data) {
        checkBounds(row, col);

        if (data == null) {
            remove(row, col);
            return;
        }

        // 先检查该位置是否已有节点
        OrthogonalNode<E> existing = findNode(row, col);
        if (existing != null) {
            existing.data = data;  // 覆盖
            return;
        }

        OrthogonalNode<E> newNode = new OrthogonalNode<>(row, col, data);

        // ===== 1. 插入行链表 =====
        OrthogonalNode<E> rowPrev = rowHead[row];
        // 在行链表中找到插入位置的前驱：行内按列号升序
        while (rowPrev.right != rowHead[row] && rowPrev.right.col < col) {
            rowPrev = rowPrev.right;
        }
        newNode.right = rowPrev.right;
        rowPrev.right = newNode;

        // ===== 2. 插入列链表 =====
        OrthogonalNode<E> colPrev = colHead[col];
        // 在列链表中找到插入位置的前驱：列内按行号升序
        while (colPrev.down != colHead[col] && colPrev.down.row < row) {
            colPrev = colPrev.down;
        }
        newNode.down = colPrev.down;
        colPrev.down = newNode;

        size++;
    }

    // ==================== 查找操作 ====================

    /**
     * 获取指定位置的元素值
     *
     * @return 元素值，若该位置没有非零元素则返回 null
     */
    public E get(int row, int col) {
        checkBounds(row, col);
        OrthogonalNode<E> node = findNode(row, col);
        return node != null ? node.data : null;
    }

    /**
     * 在十字链表中查找指定位置的节点（内部方法）
     *
     * @return 找到的节点，不存在返回 null
     */
    private OrthogonalNode<E> findNode(int row, int col) {
        // 按行查找（利用行内按列号有序的特点提前终止）
        OrthogonalNode<E> current = rowHead[row].right;
        while (current != rowHead[row]) {
            if (current.col == col) {
                return current;
            }
            if (current.col > col) {
                break;  // 行内按列号升序，超过目标列号即可停止
            }
            current = current.right;
        }
        return null;
    }

    // ==================== 删除操作 ====================

    /**
     * 删除指定位置的元素
     *
     * @return 被删除的元素值，若该位置没有元素则返回 null
     */
    public E remove(int row, int col) {
        checkBounds(row, col);

        // ===== 1. 从行链表中删除 =====
        OrthogonalNode<E> rowPrev = rowHead[row];
        while (rowPrev.right != rowHead[row] && rowPrev.right.col < col) {
            rowPrev = rowPrev.right;
        }
        if (rowPrev.right == rowHead[row] || rowPrev.right.col != col) {
            return null;  // 该位置没有节点
        }
        OrthogonalNode<E> target = rowPrev.right;
        rowPrev.right = target.right;

        // ===== 2. 从列链表中删除 =====
        OrthogonalNode<E> colPrev = colHead[col];
        while (colPrev.down != colHead[col] && colPrev.down.row < row) {
            colPrev = colPrev.down;
        }
        // 此时 colPrev.down 必然指向 target
        colPrev.down = target.down;

        // ===== 3. 清理引用，帮助 GC =====
        target.right = null;
        target.down = null;

        size--;
        return target.data;
    }

    // ==================== 按行/列遍历 ====================

    /**
     * 遍历并打印第 row 行的所有非零元素
     */
    public void traversalRow(int row) {
        checkRowBounds(row);

        System.out.print("第 " + row + " 行: ");
        OrthogonalNode<E> current = rowHead[row].right;
        if (current == rowHead[row]) {
            System.out.println("(空)");
            return;
        }
        while (current != rowHead[row]) {
            System.out.print("[" + current.col + "]=" + current.data + " ");
            current = current.right;
        }
        System.out.println();
    }

    /**
     * 遍历并打印第 col 列的所有非零元素
     */
    public void traversalCol(int col) {
        checkColBounds(col);

        System.out.print("第 " + col + " 列: ");
        OrthogonalNode<E> current = colHead[col].down;
        if (current == colHead[col]) {
            System.out.println("(空)");
            return;
        }
        while (current != colHead[col]) {
            System.out.print("[" + current.row + "]=" + current.data + " ");
            current = current.down;
        }
        System.out.println();
    }

    /**
     * 遍历整个矩阵（按行优先顺序打印所有非零元素）
     */
    public void traversal() {
        System.out.println("========== 十字链表遍历（行优先）==========");
        for (int i = 0; i < rows; i++) {
            traversalRow(i);
        }
        System.out.println("==========================================");
    }

    // ==================== 获取行/列的所有元素 ====================

    /**
     * 获取指定行的所有元素（稠密数组形式，零元素位置为 null）
     */
    @SuppressWarnings("unchecked")
    public E[] getRow(int row) {
        checkRowBounds(row);
        Object[] result = new Object[cols];
        OrthogonalNode<E> current = rowHead[row].right;
        while (current != rowHead[row]) {
            result[current.col] = current.data;
            current = current.right;
        }
        return (E[]) result;
    }

    /**
     * 获取指定列的所有元素（稠密数组形式，零元素位置为 null）
     */
    @SuppressWarnings("unchecked")
    public E[] getCol(int col) {
        checkColBounds(col);
        Object[] result = new Object[rows];
        OrthogonalNode<E> current = colHead[col].down;
        while (current != colHead[col]) {
            result[current.row] = current.data;
            current = current.down;
        }
        return (E[]) result;
    }

    // ==================== 矩阵运算 ====================

    /**
     * 矩阵转置：返回一个新的十字链表，其行列互换
     * 原地转置只需交换 row/col 字段并重建行列索引，这里为清晰起见采用新建方式
     *
     * @return 转置后的新十字链表
     */
    public MyOrthogonalLinkedList<E> transpose() {
        MyOrthogonalLinkedList<E> result = new MyOrthogonalLinkedList<>(cols, rows);

        for (int i = 0; i < rows; i++) {
            OrthogonalNode<E> current = rowHead[i].right;
            while (current != rowHead[i]) {
                result.insert(current.col, current.row, current.data);
                current = current.right;
            }
        }

        return result;
    }

    /**
     * 获取矩阵中指定行的非零元素个数
     */
    public int rowSize(int row) {
        checkRowBounds(row);
        int count = 0;
        OrthogonalNode<E> current = rowHead[row].right;
        while (current != rowHead[row]) {
            count++;
            current = current.right;
        }
        return count;
    }

    /**
     * 获取矩阵中指定列的非零元素个数
     */
    public int colSize(int col) {
        checkColBounds(col);
        int count = 0;
        OrthogonalNode<E> current = colHead[col].down;
        while (current != colHead[col]) {
            count++;
            current = current.down;
        }
        return count;
    }

    // ==================== 工具操作 ====================

    /**
     * 清空矩阵
     */
    public void clear() {
        for (int i = 0; i < rows; i++) {
            // 断开行链表中的所有节点引用（列链表可随之回收）
            OrthogonalNode<E> current = rowHead[i].right;
            rowHead[i].right = rowHead[i];  // 恢复自环
            while (current != rowHead[i]) {
                OrthogonalNode<E> next = current.right;
                current.right = null;
                current.down = null;
                current = next;
            }
        }
        for (int j = 0; j < cols; j++) {
            colHead[j].down = colHead[j];   // 恢复自环
        }
        size = 0;
    }

    /**
     * 判断指定位置是否存在非零元素
     */
    public boolean contains(int row, int col) {
        return findNode(row, col) != null;
    }

    /**
     * 将十字链表转换为稠密二维数组（返回 Object[][]）
     */
    public Object[][] toDenseMatrix() {
        Object[][] matrix = new Object[rows][cols];
        for (int i = 0; i < rows; i++) {
            OrthogonalNode<E> current = rowHead[i].right;
            while (current != rowHead[i]) {
                matrix[current.row][current.col] = current.data;
                current = current.right;
            }
        }
        return matrix;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "矩阵为空 (" + rows + "×" + cols + ")";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("十字链表矩阵 (").append(rows).append("×").append(cols)
          .append(", 非零元素: ").append(size).append(")\n");

        for (int i = 0; i < rows; i++) {
            sb.append("行 ").append(i).append(": ");
            OrthogonalNode<E> current = rowHead[i].right;
            if (current == rowHead[i]) {
                sb.append("(空)");
            } else {
                while (current != rowHead[i]) {
                    sb.append("(").append(current.row).append(",")
                      .append(current.col).append(")=")
                      .append(current.data).append(" ");
                    current = current.right;
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ==================== 边界检查 ====================

    private void checkBounds(int row, int col) {
        checkRowBounds(row);
        checkColBounds(col);
    }

    private void checkRowBounds(int row) {
        if (row < 0 || row >= rows) {
            throw new IllegalArgumentException("行号越界: " + row + " (有效范围: 0~" + (rows - 1) + ")");
        }
    }

    private void checkColBounds(int col) {
        if (col < 0 || col >= cols) {
            throw new IllegalArgumentException("列号越界: " + col + " (有效范围: 0~" + (cols - 1) + ")");
        }
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 十字链表测试 ==========\n");

        // 1. 基本插入与查询
        System.out.println("--- 1. 基本插入与查询 ---");
        MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
        matrix.insert(0, 1, 100);
        matrix.insert(0, 3, 200);
        matrix.insert(1, 2, 300);
        matrix.insert(2, 0, 400);
        matrix.insert(2, 4, 500);
        matrix.insert(3, 3, 600);
        matrix.insert(4, 1, 700);
        matrix.insert(4, 4, 800);

        System.out.println("size: " + matrix.size());
        System.out.println("get(2, 0): " + matrix.get(2, 0));
        System.out.println("get(2, 1): " + matrix.get(2, 1) + " (应为 null)");
        System.out.println("contains(4, 4): " + matrix.contains(4, 4));
        System.out.println("contains(0, 0): " + matrix.contains(0, 0));
        System.out.println();

        // 2. 遍历
        System.out.println("--- 2. 完整遍历 ---");
        matrix.traversal();
        System.out.println();

        // 3. 按行遍历
        System.out.println("--- 3. 按行遍历 ---");
        matrix.traversalRow(0);
        matrix.traversalRow(2);
        matrix.traversalRow(4);
        System.out.println();

        // 4. 按列遍历
        System.out.println("--- 4. 按列遍历 ---");
        matrix.traversalCol(1);
        matrix.traversalCol(4);
        System.out.println();

        // 5. 获取整行/整列
        System.out.println("--- 5. 获取整行/整列 ---");
        Object[] row2 = matrix.getRow(2);
        System.out.print("第2行: ");
        for (Object o : row2) {
            System.out.print(o + " ");
        }
        System.out.println();
        Object[] col4 = matrix.getCol(4);
        System.out.print("第4列: ");
        for (Object o : col4) {
            System.out.print(o + " ");
        }
        System.out.println("\n");

        // 6. 删除操作
        System.out.println("--- 6. 删除操作 ---");
        System.out.println("删除前 size: " + matrix.size());
        System.out.println("remove(2, 0): " + matrix.remove(2, 0));
        System.out.println("remove(2, 0)再次: " + matrix.remove(2, 0) + " (应为 null)");
        System.out.println("删除后 size: " + matrix.size());
        System.out.println("删除后第2行: ");
        matrix.traversalRow(2);
        System.out.println();

        // 7. 覆盖更新
        System.out.println("--- 7. 覆盖更新 ---");
        System.out.println("get(0,1) 原值: " + matrix.get(0, 1));
        matrix.insert(0, 1, 999);
        System.out.println("insert(0,1,999) 后: " + matrix.get(0, 1));
        System.out.println("size(应不变): " + matrix.size());
        System.out.println();

        // 8. insert null 相当于删除
        System.out.println("--- 8. insert null = 删除 ---");
        System.out.println("contains(4,1) 删除前: " + matrix.contains(4, 1));
        matrix.insert(4, 1, null);
        System.out.println("insert(4,1,null) 后: " + matrix.contains(4, 1));
        System.out.println();

        // 9. 矩阵转置
        System.out.println("--- 9. 矩阵转置 ---");
        System.out.println("原矩阵:");
        System.out.println(matrix);
        MyOrthogonalLinkedList<Integer> transposed = matrix.transpose();
        System.out.println("转置矩阵:");
        System.out.println(transposed);
        System.out.println("转置后: rows=" + transposed.rows() + ", cols=" + transposed.cols());
        System.out.println("转置后 get(1,0): " + transposed.get(1, 0)
                + " (原矩阵 get(0,1): " + matrix.get(0, 1) + ")");
        System.out.println();

        // 10. 从二维数组构造
        System.out.println("--- 10. 从二维数组构造 ---");
        Integer[][] arr = {
            {1,    null, 2,    null},
            {null, 3,    null, null},
            {null, null, null, 4   },
        };
        MyOrthogonalLinkedList<Integer> fromArray = new MyOrthogonalLinkedList<>(arr);
        System.out.println(fromArray);
        System.out.println("size: " + fromArray.size());
        System.out.println();

        // 11. 转换为稠密矩阵
        System.out.println("--- 11. 转换为稠密矩阵 ---");
        Object[][] dense = fromArray.toDenseMatrix();
        for (Object[] objects : dense) {
            for (Object object : objects) {
                System.out.print((object == null ? "." : object) + "\t");
            }
            System.out.println();
        }
        System.out.println();

        // 12. 清空测试
        System.out.println("--- 12. 清空 ---");
        matrix.clear();
        System.out.println("清空后 isEmpty: " + matrix.isEmpty());
        System.out.println("清空后 size: " + matrix.size());
        System.out.println(matrix);
        System.out.println();

        // 13. 边界测试
        System.out.println("--- 13. 边界测试 ---");
        try {
            matrix.insert(100, 0, 1);
        } catch (IllegalArgumentException e) {
            System.out.println("预期异常: " + e.getMessage());
        }
        try {
            matrix.get(-1, 0);
        } catch (IllegalArgumentException e) {
            System.out.println("预期异常: " + e.getMessage());
        }

        System.out.println();
        System.out.println("========== 测试完成 ==========");
    }
}
