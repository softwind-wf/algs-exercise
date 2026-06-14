package test;

import edu.princeton.cs.algs4.StdRandom;
import edu.princeton.cs.algs4.StdOut;

public class TestVisualAccumulator {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java TestVisualAccumulator <number_of_trials>");
            return;
        }

        int T = Integer.parseInt(args[0]);
        VisualAccumulator a = new VisualAccumulator(T, 1.0);

        for (int t = 0; t < T; t++) {
            a.addDataValue(StdRandom.random());
        }

        StdOut.println(a);
    }
}