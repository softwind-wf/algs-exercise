package exercise1.exercise1_1;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class GenerateTestFiles {
    public static void main(String[] args) {
        // 生成largeW.txt：有序奇数，10000个
        try (FileWriter wWriter = new FileWriter("largeW.txt")) {
            for (int i = 1; i <= 2000; i += 2) {
                wWriter.write(i + "\n");
            }
            System.out.println("largeW.txt 生成成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 生成largeT.txt：5000个随机整数，范围1-20000，混合存在/不存在于白名单的数值
        try (FileWriter tWriter = new FileWriter("largeT.txt")) {
            Random random = new Random();
            for (int i = 0; i < 5000; i++) {
                // 随机生成1-20000的整数，有一半概率是偶数（不在白名单），一半奇数（在白名单）
                int num = random.nextBoolean() ? random.nextInt(1000000) * 2 + 1 : random.nextInt(1000000) * 2;
                tWriter.write(num + "\n");
            }
            System.out.println("largeT.txt 生成成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}