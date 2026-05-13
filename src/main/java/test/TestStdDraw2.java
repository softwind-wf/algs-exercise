package test;

import edu.princeton.cs.algs4.StdDraw;

public class TestStdDraw2 {
    public static void main(String[] args) {
        StdDraw.setCanvasSize(600, 600);
        StdDraw.setScale(0, 10); // 坐标范围 0-10
        StdDraw.enableDoubleBuffering(); // 开启双缓冲防闪烁
        
        double x = 1.0; // 小球初始x坐标
        double y = 5.0; // 小球初始y坐标
        double speed = 0.1; // 移动速度
        
        while (true) { // 无限循环实现动画
            // 1. 清空画布
            StdDraw.clear(StdDraw.LIGHT_GRAY);
            
            // 2. 画小球（实心圆）
            StdDraw.setPenColor(StdDraw.ORANGE);
            StdDraw.filledCircle(x, y, 0.5); // 半径0.5
            
            // 3. 刷新画面（双缓冲必须调用show）
            StdDraw.show();
            // 4. 暂停20毫秒（控制帧率，约50帧/秒）
            StdDraw.pause(20);
            
            // 5. 更新小球位置（碰到边界反弹）
            x += speed;
            if (x > 9.5 || x < 0.5) {
                speed = -speed;
            }
        }
    }
}