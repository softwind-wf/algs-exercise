package com.ds.array;

/**
 * Chapter2_ArrayAndLinkedList
 * 数组的创建与使用
 */
public class ArrayCreationAndUsing {

    // 通过指定长度创建数组
    public void createArrayByLength() {
        int[] arrayInt = new int[10];          // 默认值:0
        double[] arrayDouble = new double[10]; // 默认值:0.0
        char[] arrayChar = new char[10];       // 默认值:字符编码0
        boolean[] arrayBoolean = new boolean[10];// 默认值:false
        Object[] arrayObject = new Object[10]; // 默认值:null
    }

    // 通过指定内容创建数组
    public void createArrayByContent() {
        int[] arrayInt = new int[]{1, 2, 3, 4, 5, 6, 7};
        double[] arrayDouble = new double[]{3.14, 0.618, 1.414, 1.71828};
        char[] arrayChar = new char[]{'A', 'B', 'C', 'D', 'E'};
        boolean[] arrayBoolean = new boolean[]{true, false, true, false};
        String[] arrayString = new String[]{"ABC", "BCD", "CDE", "DEF"};
    }

    // 通过简写的方式为数组指定元素
    public void createArrayByContentSimplify() {
        int[] arrayInt = {1, 2, 3, 4, 5, 6, 7};
        double[] arrayDouble = {3.14, 0.618, 1.414, 1.71828};
        char[] arrayChar = {'A', 'B', 'C', 'D', 'E'};
        boolean[] arrayBoolean = {true, false, true, false};
        String[] arrayString = {"ABC", "BCD", "CDE", "DEF"};
    }

    // 通过数组名称加点的方式访问数组长度
    public void visitArrayLength() {
        int[] array1 = new int[]{3, 6, 1, 8, 5};
        System.out.println(array1.length); // 输出5

        int[] array2 = new int[10];
        System.out.println(array2.length); // 输出10
    }

    // 通过元素下标访问数组元素
    public void visitArrayElementByIndex() {
        int[] array = new int[]{3, 1, 6, 0, 9, 7, 2, 5, 8, 4};

        System.out.println(array[0]); // 输出 3
        System.out.println(array[1]); // 输出 1
        System.out.println(array[2]); // 输出 6
        System.out.println(array[3]); // 输出 0
        System.out.println(array[4]); // 输出 9

        // 通过循环控制变量作为元素下标，遍历数组元素
        for (int i = 0; i < array.length; i++) {
            System.out.println(array[i]);
        }

        // 抛出数组下标越界异常 ArrayIndexOutOfBoundsException
        // System.out.println(array[100]);
    }



        //创建多维数组
        public void createMultidimensionalArray() {
            //创建每行等长的动态二维数组:3行5列
            int[][] array1 = new int[3][5];

            //创建每行长度不相等的动态二维数组
            int[][] array2 = new int[5][];        //第1行长度为1
            array2[0] = new int[1];               //第2行长度为2
            array2[1] = new int[2];               //第3行长度为3
            array2[2] = new int[3];               //第4行长度为4
            array2[3] = new int[4];               //第5行长度为5
            array2[4] = new int[5];

            //创建每行等长的静态二维数组:3行5列
            int[][] array3 = new int[][]{
                    {3, 2, 3, 1, 6},
                    {5, 7, 6, 2, 5},
                    {6, 8, 1, 5, 2}
            };

            //创建每行长度不相等的静态二维数组
            int[][] array4 = new int[][]{
                    {1},
                    {1, 1},
                    {1, 2, 1},
                    {1, 3, 3, 1},
                    {1, 4, 6, 4, 1}
            };

            //动态静态混用的二维数组创建方式
            int[][] array5 = new int[5][];
            array5[0] = new int[]{1};
            array5[1] = new int[]{1, 1};
            array5[2] = new int[]{1, 2, 1};
            array5[3] = new int[]{1, 3, 3, 1};
            array5[4] = new int[]{1, 4, 6, 4, 1};
        }

        //使用循环嵌套遍历多维数组
        public void visitMultidimensionalArray() {
            //创建每行长度不相等的静态二维数组
            int[][] array = new int[][]{
                    {1},
                    {1, 1},
                    {1, 2, 1},
                    {1, 3, 3, 1},
                    {1, 4, 6, 4, 1}
            };

            //外层循环控制二维数组的行
            for(int i = 0; i < array.length; i++) {
                //内层循环控制行下的每个元素
                for(int j = 0; j < array[i].length; j++) {
                    System.out.print(array[i][j]);
                }
                System.out.println();
            }
        }


    public static void main(String[] args) {
        ArrayCreationAndUsing arrayCreationAndUsing = new ArrayCreationAndUsing();
        arrayCreationAndUsing.createArrayByLength();
        arrayCreationAndUsing.createArrayByContent();
        arrayCreationAndUsing.createArrayByContentSimplify();
        arrayCreationAndUsing.visitArrayLength();
        arrayCreationAndUsing.visitArrayElementByIndex();
        arrayCreationAndUsing.createMultidimensionalArray();
        arrayCreationAndUsing.visitMultidimensionalArray();
    }

}