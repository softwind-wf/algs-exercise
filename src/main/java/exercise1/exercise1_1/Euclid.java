package exercise1.exercise1_1;

public class Euclid {
    // 递归计算最大公约数，并打印每次调用的参数
    public static int gcd(int p, int q) {
        System.out.printf("调用 gcd(%d, %d)%n", p, q);
        if (q == 0) return p;
        return gcd(q, p % q);
    }

    public static void main(String[] args) {
        // 从命令行接收两个参数
        if (args.length != 2) {
            System.err.println("用法：java Euclid <p> <q>");
            System.exit(1);
        }
        int p = Integer.parseInt(args[0]);
        int q = Integer.parseInt(args[1]);

        int result = gcd(p, q);
        System.out.printf("最大公约数：%d%n", result);
    }
}