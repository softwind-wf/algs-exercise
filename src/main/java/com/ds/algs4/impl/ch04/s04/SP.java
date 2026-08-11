package com.ds.algs4.impl.ch04.s04;

public interface SP<DrectedEdge>{
    double distTo(int v);
    boolean hasPathTo(int v);
    Iterable<DrectedEdge> pathTo(int v);
}
