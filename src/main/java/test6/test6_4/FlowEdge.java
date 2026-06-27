package test6.test6_4;

public class FlowEdge {
    // 边的起点
    private final int v;
    // 边的终点
    private final int w;
    // 容量
    private final double capacity;
    // 流量
    private double flow;

    // 构造函数：创建一条从v到w、容量为capacity的流边，初始流量0
    public FlowEdge(int v, int w, double capacity) {
        this.v = v;
        this.w = w;
        this.capacity = capacity;
        this.flow = 0.0;
    }

    // 返回边的起点
    public int from() {
        return v;
    }

    // 返回边的终点
    public int to() {
        return w;
    }

    // 返回边的总容量
    public double capacity() {
        return capacity;
    }

    // 返回当前已使用流量
    public double flow() {
        return flow;
    }

    // 给定顶点vertex，返回边上另一端的顶点
    public int other(int vertex) {
        if (vertex == v) return w;
        else if (vertex == w) return v;
        else throw new RuntimeException("Inconsistent edge");
    }

    // 查询从vertex出发，这条边剩余可增广的容量（剩余容量）
    public double residualCapacityTo(int vertex) {
        // 反向边：从终点w回退到起点v，剩余容量=当前流量（可以退流）
        if (vertex == v) {
            return flow;
        }
        // 正向边：从起点v到终点w，剩余容量=总容量-已用流量
        else if (vertex == w) {
            return capacity - flow;
        }
        else {
            throw new RuntimeException("Inconsistent edge");
        }
    }

    // 沿着vertex方向，增加delta大小的剩余流量（正向加流/反向退流）
    public void addResidualFlowTo(int vertex, double delta) {
        // 反向：往回退流，总流量减少delta
        if (vertex == v) {
            flow -= delta;
        }
        // 正向：向前增流，总流量增加delta
        else if (vertex == w) {
            flow += delta;
        }
        else {
            throw new RuntimeException("Inconsistent edge");
        }
    }

    // 格式化输出边信息：起点->终点 容量 流量
    @Override
    public String toString() {
        return String.format("%d->%d %.2f %.2f", v, w, capacity, flow);
    }
}