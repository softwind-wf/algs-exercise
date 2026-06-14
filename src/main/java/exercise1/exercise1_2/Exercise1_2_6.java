package exercise1.exercise1_2;

import edu.princeton.cs.algs4.StdOut;

public class Exercise1_2_6 {
    public static boolean isRotation(String s,String t){
        return s.length()==t.length()&&(t+t).indexOf(s)!=-1;

    }

    public static void main(String[] args){

        String s="ACTGBACG";
        String t="TGACGDAC";

        StdOut.println(isRotation(s,t));

    }




}
