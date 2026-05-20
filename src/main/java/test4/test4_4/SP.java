package test4.test4_4;

public interface SP<DrectedEdge>{
    double distTo(int v);
    boolean hasPathTo(int v);
    Iterable<DrectedEdge> pathTo(int v);
}
