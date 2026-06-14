package exercise1.exercise1_1;

public class DecimalToBinary {
    public static void main(String[] args) {
        int N = 10; // 可以替换成任意正整数
        String s = "";
        
        // 核心转换逻辑
        for (int n = N; n > 0; n /= 2) {
            s = (n % 2) + s;
        }
        
        System.out.println(s); // 输出: 1010
    }
}