package test6;

import java.util.Comparator;

// 事件：记录碰撞发生时间、参与粒子，用于优先队列排序
public class Event implements Comparable<Event> {
    private final double time;
    private final Particle a, b;
    private final int countA, countB;
    // 碰撞类型: 0=粒子碰撞, 1=垂直墙, 2=水平墙
    private final int wallType;

    public Event(double t, Particle a, Particle b) {
        this(t, a, b, 0);
    }

    // 墙体碰撞构造，wallType: 1=垂直, 2=水平
    public Event(double t, Particle a, Particle b, int wallType) {
        this.time = t;
        this.a = a;
        this.b = b;
        this.wallType = wallType;
        countA = (a != null) ? a.count() : -1;
        countB = (b != null) ? b.count() : -1;
    }

    // 按时间升序：先发生的事件排在队首
    @Override
    public int compareTo(Event that) {
        return Double.compare(this.time, that.time);
    }

    // 判断事件是否有效：预测的碰撞没有被更早的碰撞打断
    public boolean isValid() {
        if (a != null && a.count() != countA) return false;
        if (b != null && b.count() != countB) return false;
        return true;
    }

    public double getTime() { return time; }
    public Particle getA() { return a; }
    public Particle getB() { return b; }
    public int getWallType() { return wallType; }
}