package com.ds.sort;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * 泛型快速排序：支持 Comparable 和 Comparator 两种比较方式。
 * 不稳定排序，原地排序，平均时间复杂度 O(n log n)。
 */
public class QuickSortGeneric {

    private static final Random RAND = new Random();

    // ==================== Comparable 版本 ====================

    /** 基础版（Lomuto 分区 + 尾递归优化） */
    public static <T extends Comparable<? super T>> void sort(T[] a) {
        shuffle(a);
        sortComparable(a, 0, a.length - 1);
    }

    private static <T extends Comparable<? super T>> void sortComparable(T[] a, int lo, int hi) {
        while (lo < hi) {
            int p = partition(a, lo, hi);
            // 优先递归较小侧，保证栈深度 O(log n)
            if (p - lo < hi - p) {
                sortComparable(a, lo, p - 1);
                lo = p + 1;
            } else {
                sortComparable(a, p + 1, hi);
                hi = p - 1;
            }
        }
    }

    private static <T extends Comparable<? super T>> int partition(T[] a, int lo, int hi) {
        T pivot = a[hi];
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (a[j].compareTo(pivot) <= 0) {   // 原来是 a[j] <= pivot
                i++;
                swap(a, i, j);
            }
        }
        swap(a, i + 1, hi);
        return i + 1;
    }

    /** 三向切分版（大量重复元素时高效） */
    public static <T extends Comparable<? super T>> void sort3Way(T[] a) {
        shuffle(a);
        sort3WayComparable(a, 0, a.length - 1);
    }

    private static <T extends Comparable<? super T>> void sort3WayComparable(T[] a, int lo, int hi) {
        if (lo >= hi) return;

        int lt = lo, gt = hi, i = lo;
        T pivot = a[lo];

        while (i <= gt) {
            int cmp = a[i].compareTo(pivot);
            if (cmp < 0) {          // 原来是 a[i] < pivot
                swap(a, lt++, i++);
            } else if (cmp > 0) {   // 原来是 a[i] > pivot
                swap(a, i, gt--);
            } else {
                i++;
            }
        }
        sort3WayComparable(a, lo, lt - 1);
        sort3WayComparable(a, gt + 1, hi);
    }

    /** 双轴快排版 */
    public static <T extends Comparable<? super T>> void sortDualPivot(T[] a) {
        shuffle(a);
        sortDualPivotComparable(a, 0, a.length - 1);
    }

    private static <T extends Comparable<? super T>> void sortDualPivotComparable(T[] a, int lo, int hi) {
        if (lo >= hi) return;
        if (hi - lo <= 27) {
            insertionSort(a, lo, hi);
            return;
        }
        // 确保 pivot1 <= pivot2
        if (a[lo].compareTo(a[hi]) > 0) {   // 原来是 a[lo] > a[hi]
            swap(a, lo, hi);
        }
        T pivot1 = a[lo], pivot2 = a[hi];

        int lt = lo + 1, gt = hi - 1, k = lt;
        while (k <= gt) {
            if (a[k].compareTo(pivot1) < 0) {        // a[k] < pivot1
                swap(a, k, lt++);
            } else if (a[k].compareTo(pivot2) > 0) { // a[k] > pivot2
                while (k < gt && a[gt].compareTo(pivot2) > 0) gt--;
                swap(a, k, gt--);
                if (a[k].compareTo(pivot1) < 0) {
                    swap(a, k, lt++);
                }
            }
            k++;
        }
        lt--; gt++;
        swap(a, lo, lt);
        swap(a, hi, gt);

        sortDualPivotComparable(a, lo, lt - 1);
        sortDualPivotComparable(a, lt + 1, gt - 1);
        sortDualPivotComparable(a, gt + 1, hi);
    }

    // ==================== Comparator 版本 ====================

    /** 基础版 + 自定义比较器 */
    public static <T> void sort(T[] a, Comparator<? super T> cmp) {
        shuffle(a);
        sortComparator(a, 0, a.length - 1, cmp);
    }

    private static <T> void sortComparator(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        while (lo < hi) {
            int p = partition(a, lo, hi, cmp);
            if (p - lo < hi - p) {
                sortComparator(a, lo, p - 1, cmp);
                lo = p + 1;
            } else {
                sortComparator(a, p + 1, hi, cmp);
                hi = p - 1;
            }
        }
    }

    private static <T> int partition(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        T pivot = a[hi];
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (cmp.compare(a[j], pivot) <= 0) {
                i++;
                swap(a, i, j);
            }
        }
        swap(a, i + 1, hi);
        return i + 1;
    }

    // ==================== 辅助方法 ====================

    private static <T extends Comparable<? super T>> void insertionSort(T[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            T key = a[i];
            int j = i - 1;
            while (j >= lo && a[j].compareTo(key) > 0) {  // 原来是 a[j] > key
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    private static <T> void swap(T[] a, int i, int j) {
        T tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    private static <T> void shuffle(T[] a) {
        int n = a.length;
        for (int i = n - 1; i > 0; i--) {
            int j = RAND.nextInt(i + 1);
            swap(a, i, j);
        }
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        // ----- Comparable 版：String 默认按字典序 -----
        String[] words = {"banana", "apple", "cherry", "date", "elderberry"};
        QuickSortGeneric.sort(words);
        System.out.println("String排序:  " + Arrays.toString(words));

        // ----- Comparator 版：按长度排序 -----
        String[] words2 = {"kiwi", "grape", "blueberry", "fig", "apricot"};
        QuickSortGeneric.sort(words2, Comparator.comparingInt(String::length));
        System.out.println("按长度排序:  " + Arrays.toString(words2));

        // ----- Comparator 版：按长度降序，等长按字典序 -----
        String[] words3 = {"dog", "cat", "elephant", "ant", "bee", "fox"};
        QuickSortGeneric.sort(words3,
                Comparator.comparingInt(String::length).reversed()
                        .thenComparing(Comparator.naturalOrder()));
        System.out.println("长度降序:    " + Arrays.toString(words3));

        // ----- 三向切分：大量重复 -----
        String[] colors = {"red", "blue", "red", "green", "blue", "red", "blue", "green"};
        QuickSortGeneric.sort3Way(colors);
        System.out.println("大量重复:    " + Arrays.toString(colors));

        // ----- 双轴快排 -----
        Integer[] nums = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        QuickSortGeneric.sortDualPivot(nums);
        System.out.println("双轴快排:    " + Arrays.toString(nums));

        // ----- 边界测试 -----
        Integer[] empty = {};
        QuickSortGeneric.sort(empty);
        System.out.println("空数组:      " + Arrays.toString(empty));

        Integer[] single = {42};
        QuickSortGeneric.sort(single);
        System.out.println("单元素:      " + Arrays.toString(single));

        Integer[] sorted = {1, 2, 3, 4, 5};
        QuickSortGeneric.sort(sorted);
        System.out.println("已有序:      " + Arrays.toString(sorted));

        Integer[] reversed = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        QuickSortGeneric.sortDualPivot(reversed);
        System.out.println("逆序排序:    " + Arrays.toString(reversed));

        // ----- Comparator 排序自定义对象 -----
        Person[] people = {
                new Person("Alice", 30),
                new Person("Bob", 25),
                new Person("Charlie", 35),
                new Person("Diana", 25),
        };
        // 按年龄排序，同龄按姓名排序
        QuickSortGeneric.sort(people,
                Comparator.comparingInt(Person::age).thenComparing(Person::name));
        System.out.println("自定义对象:  " + Arrays.toString(people));
    }

     static class Person {
        public String name;
        public int age;
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        @Override
        public String toString() {
            return name + "(" + age + ")";
        }

         public int age() {
            return age;
         }

         public String name() {
            return name;
         }
     }
}
