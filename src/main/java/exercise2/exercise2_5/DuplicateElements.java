package exercise2.exercise2_5;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class DuplicateElements {

    // 1. 判断数组中是否存在重复元素
    public static boolean hasDuplicates(Comparable[] arr) {
        if (arr == null || arr.length <= 1) return false;
        Comparable[] sorted = arr.clone();
        Arrays.sort(sorted);
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i].compareTo(sorted[i-1]) == 0) {
                return true;
            }
        }
        return false;
    }

    // 2. 统计数组中不重复的元素个数
    public static int countDistinct(Comparable[] arr) {
        if (arr == null || arr.length == 0) return 0;
        Comparable[] sorted = arr.clone();
        Arrays.sort(sorted);
        int distinct = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i].compareTo(sorted[i-1]) != 0) {
                distinct++;
            }
        }
        return distinct;
    }

    // 3. 找出出现次数最多的元素及其次数
    public static <T extends Comparable<T>> Pair<T, Integer> findMostFrequent(T[] arr) {
        if (arr == null || arr.length == 0) {
            return new Pair<>(null, 0);
        }
        T[] sorted = arr.clone();
        Arrays.sort(sorted);

        T mostFreq = sorted[0];
        int maxCount = 1;
        int currentCount = 1;

        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i].compareTo(sorted[i-1]) == 0) {
                currentCount++;
            } else {
                currentCount = 1;
            }
            if (currentCount > maxCount) {
                maxCount = currentCount;
                mostFreq = sorted[i];
            }
        }
        return new Pair<>(mostFreq, maxCount);
    }

    // 4. 获取所有重复元素（去重后的重复元素集合）
    public static <T extends Comparable<T>> List<T> getDuplicateValues(T[] arr) {
        List<T> duplicates = new ArrayList<>();
        if (arr == null || arr.length <= 1) return duplicates;
        T[] sorted = arr.clone();
        Arrays.sort(sorted);

        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i].compareTo(sorted[i-1]) == 0) {
                // 避免重复添加同一个重复值
                if (duplicates.isEmpty() || sorted[i].compareTo(duplicates.get(duplicates.size()-1)) != 0) {
                    duplicates.add(sorted[i]);
                }
            }
        }
        return duplicates;
    }

    // 辅助类：存储元素和其出现次数
    public static class Pair<K, V> {
        private K key;
        private V value;
        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
        public K getKey() { return key; }
        public V getValue() { return value; }
    }

    // 测试
    public static void main(String[] args) {
        Integer[] arr = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5};

        System.out.println("是否存在重复元素: " + hasDuplicates(arr));
        System.out.println("不重复元素个数: " + countDistinct(arr));

        Pair<Integer, Integer> mostFreq = findMostFrequent(arr);
        System.out.println("出现最频繁的元素: " + mostFreq.getKey() + "，次数: " + mostFreq.getValue());

        List<Integer> duplicates = getDuplicateValues(arr);
        System.out.println("重复元素集合: " + duplicates);

        // 打印所有不同元素
        System.out.print("所有不同元素: ");
        Arrays.stream(arr).distinct().forEach(x -> System.out.print(x + " "));
        System.out.println();
    }
}