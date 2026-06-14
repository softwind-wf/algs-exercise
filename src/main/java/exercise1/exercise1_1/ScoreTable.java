package exercise1.exercise1_1;

import java.util.Scanner;

public class ScoreTable {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // 打印表头
        System.out.printf("%-15s %-8s %-8s %-8s\n", "Name", "Num1", "Num2", "Ratio");
        System.out.println("----------------------------------------------");
        
        // 逐行读取输入，直到输入结束
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) break;
            
            // 分割每行数据
            String[] parts = line.split("\\s+");
            if (parts.length != 3) continue;
            
            String name = parts[0];
            int num1 = Integer.parseInt(parts[1]);
            int num2 = Integer.parseInt(parts[2]);
            
            // 计算比值（防止除以0）
            double ratio = (num2 == 0) ? 0.0 : (double) num1 / num2;
            
            // 格式化输出
            System.out.printf("%-15s %-8d %-8d %-8.3f\n", name, num1, num2, ratio);
        }
        
        scanner.close();
    }
}