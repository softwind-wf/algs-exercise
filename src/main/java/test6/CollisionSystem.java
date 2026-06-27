package test6;

import edu.princeton.cs.algs4.MinPQ;
import edu.princeton.cs.algs4.StdDraw;

public class CollisionSystem {
    private MinPQ<Event> pq;
    private double t = 0.0;
    private Particle[] particles;
    private final double limitMax;

    public CollisionSystem(Particle[] particles, double limit) {
        this.particles = particles.clone();
        this.limitMax = limit;
    }

    // 预测单个粒子所有碰撞，加入优先队列
    private void predictCollisions(Particle a) {
        if (a == null) return;
        // 垂直墙碰撞
        double dtX = a.timeToHitVerticalWall();
        if (t + dtX <= limitMax) {
            pq.insert(new Event(t + dtX, a, null, 1));  // wallType=1 垂直
        }
        // 水平墙碰撞
        double dtY = a.timeToHitHorizontalWall();
        if (t + dtY <= limitMax) {
            pq.insert(new Event(t + dtY, a, null, 2));  // wallType=2 水平
        }
        // 粒子之间碰撞
        for (Particle b : particles) {
            double dt = a.timeToHit(b);
            if (t + dt <= limitMax) {
                pq.insert(new Event(t + dt, a, b));
            }
        }
    }

    // 启动事件驱动模拟
    public void simulate() {
        pq = new MinPQ<>();
        // 初始化全部粒子的碰撞事件
        for (Particle p : particles) {
            predictCollisions(p);
        }

        // 窗口初始化
        StdDraw.setCanvasSize(600, 600);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
        StdDraw.enableDoubleBuffering();

        // 主事件循环
        while (!pq.isEmpty()) {
            Event event = pq.delMin();
            double eventTime = event.getTime();
            Particle a = event.getA();
            Particle b = event.getB();

            // 无效碰撞直接跳过（已经被更早碰撞抵消）
            if (!event.isValid()) continue;

            // 把所有粒子移动到本次事件发生的时刻
            double deltaT = eventTime - t;
            for (Particle p : particles) {
                p.move(deltaT);
            }
            t = eventTime;

            // 处理碰撞物理
            if (b != null) {
                // 粒子和粒子碰撞
                a.bounceOff(b);
            } else if (event.getWallType() == 1) {
                // 垂直墙碰撞
                a.bounceOffVerticalWall();
            } else {
                // 水平墙碰撞
                a.bounceOffHorizontalWall();
            }

            // 重新计算碰撞后的粒子新事件
            predictCollisions(a);
            predictCollisions(b);

            // 刷新画面
            StdDraw.clear();
            for (Particle p : particles) {
                p.draw();
            }
            StdDraw.show();
            StdDraw.pause(100);
        }
        System.out.println("模拟结束，到达时间上限");
    }

    // 程序入口
    public static void main(String[] args) {
        int N = 5;
        double totalSimTime = 200;
        Particle[] arr = new Particle[N];
        for (int i = 0; i < N; i++) {
            arr[i] = new Particle();
        }
        CollisionSystem sys = new CollisionSystem(arr, totalSimTime);
        sys.simulate();
    }


}