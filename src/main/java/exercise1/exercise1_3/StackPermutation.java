package exercise1.exercise1_3;

import java.util.Stack;

public class StackPermutation {

    public static boolean isGenerable(int[] permutation) {
        int n = permutation.length;
        Stack<Integer> stack = new Stack<>();
        int j = 0; // 指向目标排列的当前元素

        for (int i = 0; i < n; i++) {
            stack.push(i); // 按顺序压入 0, 1, ..., n-1

            // 栈顶与目标元素匹配时弹出
            while (!stack.isEmpty() && stack.peek() == permutation[j]) {
                stack.pop();
                j++;
            }
        }

        // 如果所有元素都匹配成功，栈必然为空
        return stack.isEmpty();
    }

    public static void main(String[] args) {
        // 测试用例1：可生成的排列
        int[] validPermutation = {2, 1, 0, 3};
        System.out.println(isGenerable(validPermutation)); // 输出 true

        // 测试用例2：不可生成的排列
        int[] invalidPermutation = {3, 1, 2, 0};
        System.out.println(isGenerable(invalidPermutation)); // 输出 false
    }
}