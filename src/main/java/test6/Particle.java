package test6;

import edu.princeton.cs.algs4.StdDraw;

public class Particle {
    // 粒子基础属性
    double rx, ry;       // 当前坐标 [0,1] 单位空间
    private double vx, vy;       // 速度分量
    final double radius; // 粒子半径
    private final double mass;   // 粒子质量
    private int collisionCount;  // 累计碰撞次数

    // 构造1：随机粒子（无参，书本默认构造）
    public Particle() {
        rx = Math.random();
        ry = Math.random();
        vx = Math.random() - 0.5;
        vy = Math.random() - 0.5;
        radius = 0.01;
        mass = 0.001;
        collisionCount = 0;
    }

    // 构造2：自定义位置、速度、半径、质量
    public Particle(double rx, double ry, double vx, double vy, double radius, double mass) {
        this.rx = rx;
        this.ry = ry;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.mass = mass;
        this.collisionCount = 0;
    }

    // 根据时间dt移动粒子，不检测碰撞
    public void move(double dt) {
        rx += vx * dt;
        ry += vy * dt;
    }

    // 绘制粒子（StdDraw绘图工具，算法4配套）
    public void draw() {
        StdDraw.filledCircle(rx, ry, radius);
    }

    // 返回该粒子总共发生过的碰撞次数
    public int count() {
        return collisionCount;
    }

    // ========== 碰撞预测：返回多久后与粒子b相撞，无穷大代表不会碰撞 ==========
    public double timeToHit(Particle b) {
        if (this == b) return Double.POSITIVE_INFINITY;
        double dx = b.rx - this.rx;
        double dy = b.ry - this.ry;
        double dvx = b.vx - this.vx;
        double dvy = b.vy - this.vy;
        double dvdr = dx * dvx + dy * dvy;
        // 互相远离，不可能碰撞
        if (dvdr > 0) return Double.POSITIVE_INFINITY;

        double dvSq = dvx * dvx + dvy * dvy;
        double drSq = dx * dx + dy * dy;
        double sigma = this.radius + b.radius;
        double distMin = sigma * sigma;
        double delta = dvdr * dvdr - dvSq * (drSq - distMin);
        // 无解，不会碰撞
        if (delta < 0) return Double.POSITIVE_INFINITY;

        double t = -(dvdr + Math.sqrt(delta)) / dvSq;
        return t < 1e-9 ? Double.POSITIVE_INFINITY : t;
    }

    // 距离水平墙体（上下边界 y=0 / y=1）碰撞的时间
    public double timeToHitHorizontalWall() {
        if (vy > 0) return (1.0 - ry - radius) / vy;
        else if (vy < 0) return (radius - ry) / vy;
        else return Double.POSITIVE_INFINITY;
    }

    // 距离垂直墙体（左右边界 x=0 / x=1）碰撞的时间
    public double timeToHitVerticalWall() {
        if (vx > 0) return (1.0 - rx - radius) / vx;
        else if (vx < 0) return (radius - rx) / vx;
        else return Double.POSITIVE_INFINITY;
    }

    // ========== 碰撞后更新速度 ==========
    // 和另一个粒子弹性碰撞，更新自身速度
    public void bounceOff(Particle b) {
        double dx = b.rx - this.rx;
        double dy = b.ry - this.ry;
        double dvx = b.vx - this.vx;
        double dvy = b.vy - this.vy;
        double dvdr = dx * dvx + dy * dvy;
        double dist = this.radius + b.radius;
        double impulse = 2 * b.mass * dvdr / ((this.mass + b.mass) * dist);

        this.vx -= impulse * dx / dist;
        this.vy -= impulse * dy / dist;
        b.vx += impulse * dx / dist;
        b.vy += impulse * dy / dist;

        this.collisionCount++;
        b.collisionCount++;
    }

    // 碰撞水平墙，反转y速度
    public void bounceOffHorizontalWall() {
        vy = -vy;
        collisionCount++;
    }

    // 碰撞垂直墙，反转x速度
    public double bounceOffVerticalWall() {
        vx = -vx;
        collisionCount++;
        return vx;
    }
}