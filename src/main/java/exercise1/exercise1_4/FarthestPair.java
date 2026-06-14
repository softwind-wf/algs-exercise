package exercise1.exercise1_4;

public class FarthestPair {


        // 新增：找最遥远的一对
        public static void findFarthestPair(double[] a) {
            if (a == null || a.length < 2) {
                System.out.println("数组长度必须 ≥ 2");
                return;
            }

            double min = a[0];
            double max = a[0];

            for (double v : a) {
                if (v < min) min = v;
                if (v > max) max = v;
            }

            System.out.printf("最遥远的一对数是 %.6f 和 %.6f，差值为 %.6f\n", min, max, max - min);
        }

        public static void main(String[] args) {
            double[] test = {5.3, 9.8, 1.2, 3.5, 7.1, 4.2, 2.8};



            System.out.println("\n=== 最遥远的一对 ===");
            findFarthestPair(test);
        }
    }



