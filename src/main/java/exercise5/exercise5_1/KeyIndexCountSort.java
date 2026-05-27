package exercise5.exercise5_1;

import java.util.*;

public class KeyIndexCountSort {

    public static void main(String[] args) {
        String[] arr = {"she", "sells", "she", "sea", "shore", "sells", "sea"};
        sort(arr);
        System.out.println(Arrays.toString(arr));
    }

    public static void sort(String[] a) {
        int N = a.length;
        if (N <= 1) return;

        // 第一步：收集所有不重复的键，并排序
        Set<String> uniqueSet = new TreeSet<>(); // TreeSet 自动排序
        for (String s : a) uniqueSet.add(s);

        // 第二步：构建有序的符号表
        Map<String, Integer> keyToIndex = new LinkedHashMap<>();
        for (String key : uniqueSet) {
            keyToIndex.put(key, keyToIndex.size());
        }

        int K = keyToIndex.size();
        int[] count = new int[K + 1];
        String[] aux = new String[N];

        // 统计频率
        for (String s : a) count[keyToIndex.get(s) + 1]++;

        // 前缀和
        for (int r = 0; r < K; r++) count[r+1] += count[r];

        // 归位
        for (String s : a) aux[count[keyToIndex.get(s)]++] = s;

        // 复制回原数组
        System.arraycopy(aux, 0, a, 0, N);
    }
}