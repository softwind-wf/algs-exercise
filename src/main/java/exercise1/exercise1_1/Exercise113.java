package exercise1.exercise1_1;

import edu.princeton.cs.algs4.StdOut;

public class Exercise113 {
    public static void main(String[] args){

        // 可以添加参数个数检查
        if (args.length < 3) {
            StdOut.println("Please provide exactly 3 arguments");
            return;
        }

        int a=Integer.parseInt(args[0]);
        int b=Integer.parseInt(args[1]);
        int c=Integer.parseInt(args[2]);

        if(a==b&&a==c){
            StdOut.println("equal");

        }else{
            StdOut.println("not equal");
        }

















    }
















}
