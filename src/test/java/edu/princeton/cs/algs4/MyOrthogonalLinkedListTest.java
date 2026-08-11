package edu.princeton.cs.algs4;

import cn.exercise.algs4.datastructure.linked.MyOrthogonalLinkedList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MyOrthogonalLinkedList 十字链表测试")
public class MyOrthogonalLinkedListTest {

    // ==================== 构造方法测试 ====================

    @Nested
    @DisplayName("构造方法")
    class ConstructionTests {

        @Test
        @DisplayName("默认构造：创建 10×10 空矩阵")
        void testDefaultConstructor() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>();
            assertEquals(10, matrix.rows());
            assertEquals(10, matrix.cols());
            assertEquals(0, matrix.size());
            assertTrue(matrix.isEmpty());
        }

        @Test
        @DisplayName("指定行列构造")
        void testSizedConstructor() {
            MyOrthogonalLinkedList<String> matrix = new MyOrthogonalLinkedList<>(3, 5);
            assertEquals(3, matrix.rows());
            assertEquals(5, matrix.cols());
            assertEquals(0, matrix.size());
            assertTrue(matrix.isEmpty());
        }

        @Test
        @DisplayName("非法行列参数应抛异常")
        void testInvalidDimensions() {
            assertThrows(IllegalArgumentException.class, () -> new MyOrthogonalLinkedList<>(0, 5));
            assertThrows(IllegalArgumentException.class, () -> new MyOrthogonalLinkedList<>(5, 0));
            assertThrows(IllegalArgumentException.class, () -> new MyOrthogonalLinkedList<>(-1, 5));
            assertThrows(IllegalArgumentException.class, () -> new MyOrthogonalLinkedList<>(5, -1));
        }

        @Test
        @DisplayName("从二维数组构造")
        void testFrom2DArray() {
            Integer[][] arr = {
                {1,    null, 2,    null},
                {null, 3,    null, null},
                {null, null, null, 4   },
            };
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(arr);
            assertEquals(3, matrix.rows());
            assertEquals(4, matrix.cols());
            assertEquals(4, matrix.size());

            assertEquals(1, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 2));
            assertEquals(3, matrix.get(1, 1));
            assertEquals(4, matrix.get(2, 3));
            assertNull(matrix.get(0, 1));
        }

        @Test
        @DisplayName("从全 null 二维数组构造")
        void testFromAllNullArray() {
            Integer[][] arr = {
                {null, null},
                {null, null},
            };
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(arr);
            assertEquals(0, matrix.size());
            assertTrue(matrix.isEmpty());
        }
    }

    // ==================== 插入操作测试 ====================

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入单个元素")
        void testInsertSingleElement() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(2, 3, 100);
            assertEquals(1, matrix.size());
            assertEquals(100, matrix.get(2, 3));
            assertTrue(matrix.contains(2, 3));
        }

        @Test
        @DisplayName("插入多个元素")
        void testInsertMultipleElements() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(0, 1, 10);
            matrix.insert(2, 3, 20);
            matrix.insert(4, 0, 30);
            matrix.insert(1, 4, 40);

            assertEquals(4, matrix.size());
            assertEquals(10, matrix.get(0, 1));
            assertEquals(20, matrix.get(2, 3));
            assertEquals(30, matrix.get(4, 0));
            assertEquals(40, matrix.get(1, 4));
        }

        @Test
        @DisplayName("在同行插入多个元素（按列号有序）")
        void testInsertMultipleInSameRow() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 10);
            matrix.insert(0, 5, 50);
            matrix.insert(0, 1, 10);
            matrix.insert(0, 8, 80);
            matrix.insert(0, 3, 30);

            assertEquals(4, matrix.size());
            assertEquals(10, matrix.get(0, 1));
            assertEquals(30, matrix.get(0, 3));
            assertEquals(50, matrix.get(0, 5));
            assertEquals(80, matrix.get(0, 8));
            assertEquals(4, matrix.rowSize(0));
        }

        @Test
        @DisplayName("在同列插入多个元素（按行号有序）")
        void testInsertMultipleInSameCol() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(10, 5);
            matrix.insert(7, 2, 70);
            matrix.insert(1, 2, 10);
            matrix.insert(5, 2, 50);
            matrix.insert(3, 2, 30);

            assertEquals(4, matrix.size());
            assertEquals(10, matrix.get(1, 2));
            assertEquals(30, matrix.get(3, 2));
            assertEquals(50, matrix.get(5, 2));
            assertEquals(70, matrix.get(7, 2));
            assertEquals(4, matrix.colSize(2));
        }

        @Test
        @DisplayName("覆盖已有元素（值更新，size 不变）")
        void testInsertOverwrite() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(2, 3, 100);
            assertEquals(1, matrix.size());

            matrix.insert(2, 3, 999);
            assertEquals(1, matrix.size());
            assertEquals(999, matrix.get(2, 3));
        }

        @Test
        @DisplayName("insert null 等同于删除")
        void testInsertNullIsRemove() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(2, 3, 100);
            assertEquals(1, matrix.size());
            assertTrue(matrix.contains(2, 3));

            matrix.insert(2, 3, null);
            assertEquals(0, matrix.size());
            assertFalse(matrix.contains(2, 3));
            assertNull(matrix.get(2, 3));
        }

        @Test
        @DisplayName("insert null 到空位置不报错")
        void testInsertNullToEmptyPosition() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(2, 3, null);
            assertEquals(0, matrix.size());
            assertNull(matrix.get(2, 3));
        }
    }

    // ==================== 查询操作测试 ====================

    @Nested
    @DisplayName("查询操作")
    class QueryTests {

        @Test
        @DisplayName("get 空位置返回 null")
        void testGetEmptyPosition() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertNull(matrix.get(0, 0));
            assertNull(matrix.get(4, 4));

            matrix.insert(0, 0, 1);
            assertNull(matrix.get(0, 1));
            assertNull(matrix.get(1, 0));
        }

        @Test
        @DisplayName("contains 正确判断元素存在性")
        void testContains() {
            MyOrthogonalLinkedList<String> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertFalse(matrix.contains(0, 0));

            matrix.insert(0, 0, "hello");
            assertTrue(matrix.contains(0, 0));
            assertFalse(matrix.contains(0, 1));
        }

        @Test
        @DisplayName("size/rows/cols 查询正确")
        void testBasicQueries() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 7);
            assertEquals(3, matrix.rows());
            assertEquals(7, matrix.cols());
            assertEquals(0, matrix.size());
            assertTrue(matrix.isEmpty());

            matrix.insert(1, 3, 42);
            assertEquals(1, matrix.size());
            assertFalse(matrix.isEmpty());
        }
    }

    // ==================== 删除操作测试 ====================

    @Nested
    @DisplayName("删除操作")
    class RemoveTests {

        @Test
        @DisplayName("删除存在的元素")
        void testRemoveExisting() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(2, 3, 100);
            matrix.insert(2, 4, 200);

            Integer removed = matrix.remove(2, 3);
            assertEquals(100, removed);
            assertEquals(1, matrix.size());
            assertFalse(matrix.contains(2, 3));
            assertTrue(matrix.contains(2, 4));
        }

        @Test
        @DisplayName("删除不存在的元素返回 null")
        void testRemoveNonExisting() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertNull(matrix.remove(2, 3));
            assertEquals(0, matrix.size());

            matrix.insert(2, 3, 100);
            assertNull(matrix.remove(2, 4));
            assertEquals(1, matrix.size());
        }

        @Test
        @DisplayName("删除一行中的第一个和最后一个元素")
        void testRemoveFirstAndLastInRow() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 10);
            matrix.insert(0, 1, 10);
            matrix.insert(0, 3, 30);
            matrix.insert(0, 5, 50);
            matrix.insert(0, 7, 70);

            assertEquals(10, matrix.remove(0, 1)); // 删除第一个
            assertEquals(3, matrix.size());
            assertEquals(70, matrix.remove(0, 7)); // 删除最后一个
            assertEquals(2, matrix.size());
            assertEquals(30, matrix.get(0, 3));
            assertEquals(50, matrix.get(0, 5));
        }

        @Test
        @DisplayName("删除一列中的元素后列链表仍正确")
        void testRemoveFromColumn() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(10, 5);
            matrix.insert(1, 2, 10);
            matrix.insert(3, 2, 30);
            matrix.insert(5, 2, 50);

            matrix.remove(3, 2);
            assertEquals(2, matrix.size());
            assertEquals(10, matrix.get(1, 2));
            assertEquals(50, matrix.get(5, 2));
            assertNull(matrix.get(3, 2));
            assertEquals(2, matrix.colSize(2));
        }

        @Test
        @DisplayName("删除所有元素后矩阵为空")
        void testRemoveAllElements() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 3);
            matrix.insert(0, 0, 1);
            matrix.insert(1, 1, 2);
            matrix.insert(2, 2, 3);

            matrix.remove(0, 0);
            matrix.remove(1, 1);
            matrix.remove(2, 2);

            assertEquals(0, matrix.size());
            assertTrue(matrix.isEmpty());
            assertNull(matrix.get(0, 0));
            assertNull(matrix.get(1, 1));
            assertNull(matrix.get(2, 2));
        }
    }

    // ==================== getRow / getCol 测试 ====================

    @Nested
    @DisplayName("获取整行/整列")
    class RowColArrayTests {

        @Test
        @DisplayName("getRow 正确返回稠密数组")
        void testGetRow() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 5);
            matrix.insert(1, 0, 10);
            matrix.insert(1, 2, 30);
            matrix.insert(1, 4, 50);

            Object[] row1 = matrix.getRow(1);
            assertEquals(5, row1.length);
            assertEquals(10, row1[0]);
            assertNull(row1[1]);
            assertEquals(30, row1[2]);
            assertNull(row1[3]);
            assertEquals(50, row1[4]);
        }

        @Test
        @DisplayName("getRow 空行返回全 null 数组")
        void testGetEmptyRow() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 5);
            Object[] row0 = matrix.getRow(0);
            assertEquals(5, row0.length);
            for (Object o : row0) {
                assertNull(o);
            }
        }

        @Test
        @DisplayName("getCol 正确返回稠密数组")
        void testGetCol() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(4, 3);
            matrix.insert(0, 1, 10);
            matrix.insert(2, 1, 30);
            matrix.insert(3, 1, 40);

            Object[] col1 = matrix.getCol(1);
            assertEquals(4, col1.length);
            assertEquals(10, col1[0]);
            assertNull(col1[1]);
            assertEquals(30, col1[2]);
            assertEquals(40, col1[3]);
        }

        @Test
        @DisplayName("getCol 空列返回全 null 数组")
        void testGetEmptyCol() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(4, 3);
            Object[] col2 = matrix.getCol(2);
            assertEquals(4, col2.length);
            for (Object o : col2) {
                assertNull(o);
            }
        }
    }

    // ==================== rowSize / colSize 测试 ====================

    @Nested
    @DisplayName("行/列大小统计")
    class RowColSizeTests {

        @Test
        @DisplayName("rowSize 正确统计行内非零元素个数")
        void testRowSize() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 10);
            assertEquals(0, matrix.rowSize(0));

            matrix.insert(0, 1, 10);
            assertEquals(1, matrix.rowSize(0));

            matrix.insert(0, 5, 50);
            matrix.insert(0, 8, 80);
            assertEquals(3, matrix.rowSize(0));

            matrix.remove(0, 5);
            assertEquals(2, matrix.rowSize(0));
        }

        @Test
        @DisplayName("colSize 正确统计列内非零元素个数")
        void testColSize() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(10, 5);
            assertEquals(0, matrix.colSize(0));

            matrix.insert(1, 0, 10);
            assertEquals(1, matrix.colSize(0));

            matrix.insert(5, 0, 50);
            matrix.insert(8, 0, 80);
            assertEquals(3, matrix.colSize(0));

            matrix.remove(5, 0);
            assertEquals(2, matrix.colSize(0));
        }
    }

    // ==================== 转置测试 ====================

    @Nested
    @DisplayName("矩阵转置")
    class TransposeTests {

        @Test
        @DisplayName("转置后行列互换")
        void testTransposeDimensions() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 5);
            matrix.insert(0, 1, 10);
            matrix.insert(2, 4, 20);

            MyOrthogonalLinkedList<Integer> transposed = matrix.transpose();
            assertEquals(5, transposed.rows());
            assertEquals(3, transposed.cols());
            assertEquals(2, transposed.size());
        }

        @Test
        @DisplayName("转置后元素位置互换")
        void testTransposeElementPositions() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 4);
            matrix.insert(0, 1, 10);
            matrix.insert(1, 3, 30);
            matrix.insert(2, 0, 20);

            MyOrthogonalLinkedList<Integer> transposed = matrix.transpose();
            assertEquals(10, transposed.get(1, 0));
            assertEquals(30, transposed.get(3, 1));
            assertEquals(20, transposed.get(0, 2));
        }

        @Test
        @DisplayName("空矩阵转置后仍为空")
        void testTransposeEmpty() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(4, 6);
            MyOrthogonalLinkedList<Integer> transposed = matrix.transpose();
            assertEquals(6, transposed.rows());
            assertEquals(4, transposed.cols());
            assertEquals(0, transposed.size());
            assertTrue(transposed.isEmpty());
        }

        @Test
        @DisplayName("转置不影响原矩阵")
        void testTransposeDoesNotAffectOriginal() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 3);
            matrix.insert(0, 1, 10);
            matrix.insert(1, 2, 20);

            matrix.transpose();
            assertEquals(10, matrix.get(0, 1));
            assertEquals(20, matrix.get(1, 2));
            assertEquals(3, matrix.rows());
            assertEquals(3, matrix.cols());
        }

        @Test
        @DisplayName("方阵转置两次等于原矩阵")
        void testDoubleTranspose() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(4, 4);
            matrix.insert(0, 1, 10);
            matrix.insert(1, 2, 20);
            matrix.insert(2, 3, 30);
            matrix.insert(3, 0, 40);

            MyOrthogonalLinkedList<Integer> doubleTransposed = matrix.transpose().transpose();
            assertEquals(4, doubleTransposed.rows());
            assertEquals(4, doubleTransposed.cols());
            assertEquals(10, doubleTransposed.get(0, 1));
            assertEquals(20, doubleTransposed.get(1, 2));
            assertEquals(30, doubleTransposed.get(2, 3));
            assertEquals(40, doubleTransposed.get(3, 0));
        }
    }

    // ==================== clear 测试 ====================

    @Nested
    @DisplayName("清空操作")
    class ClearTests {

        @Test
        @DisplayName("clear 清空所有元素")
        void testClear() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(0, 1, 10);
            matrix.insert(2, 3, 20);
            matrix.insert(4, 4, 30);
            assertEquals(3, matrix.size());

            matrix.clear();
            assertEquals(0, matrix.size());
            assertTrue(matrix.isEmpty());
            assertNull(matrix.get(0, 1));
            assertNull(matrix.get(2, 3));
            assertNull(matrix.get(4, 4));
        }

        @Test
        @DisplayName("clear 后可以重新插入")
        void testInsertAfterClear() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(0, 1, 10);
            matrix.clear();
            matrix.insert(0, 1, 99);
            assertEquals(1, matrix.size());
            assertEquals(99, matrix.get(0, 1));
        }

        @Test
        @DisplayName("clear 空矩阵不报错")
        void testClearEmpty() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.clear();
            assertEquals(0, matrix.size());
            assertTrue(matrix.isEmpty());
        }
    }

    // ==================== toDenseMatrix 测试 ====================

    @Nested
    @DisplayName("转换为稠密矩阵")
    class ToDenseMatrixTests {

        @Test
        @DisplayName("toDenseMatrix 正确转换")
        void testToDenseMatrix() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 3);
            matrix.insert(0, 0, 1);
            matrix.insert(0, 2, 3);
            matrix.insert(1, 1, 5);
            matrix.insert(2, 0, 7);

            Object[][] dense = matrix.toDenseMatrix();
            assertEquals(3, dense.length);
            assertEquals(3, dense[0].length);

            assertEquals(1, dense[0][0]);
            assertNull(dense[0][1]);
            assertEquals(3, dense[0][2]);

            assertNull(dense[1][0]);
            assertEquals(5, dense[1][1]);
            assertNull(dense[1][2]);

            assertEquals(7, dense[2][0]);
            assertNull(dense[2][1]);
            assertNull(dense[2][2]);
        }

        @Test
        @DisplayName("空矩阵 toDenseMatrix 全 null")
        void testToDenseMatrixEmpty() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(2, 3);
            Object[][] dense = matrix.toDenseMatrix();
            assertEquals(2, dense.length);
            assertEquals(3, dense[0].length);
            for (Object[] row : dense) {
                for (Object o : row) {
                    assertNull(o);
                }
            }
        }
    }

    // ==================== toString 测试 ====================

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("空矩阵 toString 包含维度信息")
        void testToStringEmpty() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 5);
            String str = matrix.toString();
            assertTrue(str.contains("为空"));
            assertTrue(str.contains("3") && str.contains("5"));
        }

        @Test
        @DisplayName("非空矩阵 toString 包含元素信息")
        void testToStringNonEmpty() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(3, 3);
            matrix.insert(0, 0, 100);
            String str = matrix.toString();
            assertTrue(str.contains("100"));
            assertTrue(str.contains("(0,0)"));
            assertFalse(str.contains("为空"));
        }
    }

    // ==================== 边界/异常测试 ====================

    @Nested
    @DisplayName("边界与异常")
    class BoundaryAndExceptionTests {

        @Test
        @DisplayName("行号越界抛异常")
        void testRowOutOfBounds() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertThrows(IllegalArgumentException.class, () -> matrix.insert(-1, 0, 1));
            assertThrows(IllegalArgumentException.class, () -> matrix.insert(5, 0, 1));
            assertThrows(IllegalArgumentException.class, () -> matrix.get(-1, 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.get(5, 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.remove(-1, 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.remove(5, 0));
        }

        @Test
        @DisplayName("列号越界抛异常")
        void testColOutOfBounds() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertThrows(IllegalArgumentException.class, () -> matrix.insert(0, -1, 1));
            assertThrows(IllegalArgumentException.class, () -> matrix.insert(0, 5, 1));
            assertThrows(IllegalArgumentException.class, () -> matrix.get(0, -1));
            assertThrows(IllegalArgumentException.class, () -> matrix.get(0, 5));
            assertThrows(IllegalArgumentException.class, () -> matrix.remove(0, -1));
            assertThrows(IllegalArgumentException.class, () -> matrix.remove(0, 5));
        }

        @Test
        @DisplayName("getRow 越界抛异常")
        void testGetRowOutOfBounds() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertThrows(IllegalArgumentException.class, () -> matrix.getRow(-1));
            assertThrows(IllegalArgumentException.class, () -> matrix.getRow(5));
        }

        @Test
        @DisplayName("getCol 越界抛异常")
        void testGetColOutOfBounds() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertThrows(IllegalArgumentException.class, () -> matrix.getCol(-1));
            assertThrows(IllegalArgumentException.class, () -> matrix.getCol(5));
        }

        @Test
        @DisplayName("traversalRow 越界抛异常")
        void testTraversalRowOutOfBounds() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertThrows(IllegalArgumentException.class, () -> matrix.traversalRow(-1));
            assertThrows(IllegalArgumentException.class, () -> matrix.traversalRow(5));
        }

        @Test
        @DisplayName("traversalCol 越界抛异常")
        void testTraversalColOutOfBounds() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertThrows(IllegalArgumentException.class, () -> matrix.traversalCol(-1));
            assertThrows(IllegalArgumentException.class, () -> matrix.traversalCol(5));
        }

        @Test
        @DisplayName("rowSize/colSize 越界抛异常")
        void testRowColSizeOutOfBounds() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            assertThrows(IllegalArgumentException.class, () -> matrix.rowSize(-1));
            assertThrows(IllegalArgumentException.class, () -> matrix.rowSize(5));
            assertThrows(IllegalArgumentException.class, () -> matrix.colSize(-1));
            assertThrows(IllegalArgumentException.class, () -> matrix.colSize(5));
        }
    }

    // ==================== 通用场景测试 ====================

    @Nested
    @DisplayName("综合场景")
    class IntegrationTests {

        @Test
        @DisplayName("使用 String 类型")
        void testWithStringType() {
            MyOrthogonalLinkedList<String> matrix = new MyOrthogonalLinkedList<>(3, 3);
            matrix.insert(0, 0, "hello");
            matrix.insert(1, 1, "world");
            matrix.insert(2, 2, "java");

            assertEquals("hello", matrix.get(0, 0));
            assertEquals("world", matrix.get(1, 1));
            assertEquals("java", matrix.get(2, 2));

            assertEquals("hello", matrix.remove(0, 0));
            assertNull(matrix.get(0, 0));
        }

        @Test
        @DisplayName("使用 Double 类型")
        void testWithDoubleType() {
            MyOrthogonalLinkedList<Double> matrix = new MyOrthogonalLinkedList<>(3, 3);
            matrix.insert(0, 1, 3.14);
            matrix.insert(1, 2, 2.718);
            matrix.insert(2, 0, 1.618);

            assertEquals(3.14, matrix.get(0, 1), 0.001);
            assertEquals(2.718, matrix.get(1, 2), 0.001);
            assertEquals(1.618, matrix.get(2, 0), 0.001);
            assertEquals(3, matrix.size());
        }

        @Test
        @DisplayName("插入-删除-再插入同一位置")
        void testInsertRemoveReinsert() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            matrix.insert(2, 2, 42);
            assertEquals(42, matrix.remove(2, 2));
            assertNull(matrix.get(2, 2));

            matrix.insert(2, 2, 84);
            assertEquals(84, matrix.get(2, 2));
            assertEquals(1, matrix.size());
        }

        @Test
        @DisplayName("大规模稀疏矩阵性能测试")
        void testLargeSparseMatrix() {
            int n = 100;
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(n, n);

            // 对角线插入
            for (int i = 0; i < n; i++) {
                matrix.insert(i, i, i * 10);
            }
            assertEquals(n, matrix.size());

            // 验证对角线
            for (int i = 0; i < n; i++) {
                assertEquals(i * 10, matrix.get(i, i));
            }

            // 删除所有
            for (int i = 0; i < n; i++) {
                matrix.remove(i, i);
            }
            assertEquals(0, matrix.size());
            assertTrue(matrix.isEmpty());
        }

        @Test
        @DisplayName("十字结构验证：同行同列元素交叉正确")
        void testCrossStructure() {
            MyOrthogonalLinkedList<Integer> matrix = new MyOrthogonalLinkedList<>(5, 5);
            // 第2行：列1, 列3
            matrix.insert(2, 1, 21);
            matrix.insert(2, 3, 23);
            // 第2列：行0, 行2, 行4
            matrix.insert(0, 1, 1);
            matrix.insert(4, 1, 41);

            // 第2行第1列应该同时出现在行列遍历中
            assertEquals(2, matrix.rowSize(2));
            assertEquals(3, matrix.colSize(1));
            assertEquals(21, matrix.get(2, 1));
        }
    }
}
