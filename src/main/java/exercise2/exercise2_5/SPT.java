package exercise2.exercise2_5;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdOut;

/**
 * 2.5.12 调度问题 (SPT)
 * 最短处理时间优先 (Shortest Processing Time First) 调度
 * 功能：从标准输入读取任务名和运行时间，按运行时间升序输出调度计划
 * 目标：最小化任务完成的平均时间
 *
 * @author Robert Sedgewick (原书)
 * @author 豆包 (代码实现)
 */
public class SPT {

    // 静态内部类：表示一个任务，包含名称和运行时间
    private static class Job implements Comparable<Job> {
        private final String name;  // 任务名称
        private final double time;  // 运行时间

        public Job(String name, double time) {
            this.name = name;
            this.time = time;
        }

        // 核心比较逻辑：按运行时间升序排列 (SPT 核心)
        @Override
        public int compareTo(Job that) {
            return Double.compare(this.time, that.time);
        }

        @Override
        public String toString() {
            return name + " " + time;
        }
    }

    // 禁止实例化
    private SPT() {}

    public static void main(String[] args) {
        // 1. 读取标准输入数据
        // 输入格式假设：每一行包含两个字段，[任务名] [运行时间]
        In in = new In();
        // 由于输入行数未知，使用临时列表存储
        java.util.List<Job> jobList = new java.util.ArrayList<>();

        while (!in.isEmpty()) {
            // 读取一行并分割为字符串数组
            String[] fields = in.readLine().split("\\s+");
            if (fields.length < 2) continue; // 跳过无效行
            String name = fields[0];
            double time;
            try {
                time = Double.parseDouble(fields[1]);
            } catch (NumberFormatException e) {
                StdOut.println("警告：时间格式错误，已跳过该行 -> " + fields[0]);
                continue;
            }
            jobList.add(new Job(name, time));
        }

        // 2. 将列表转换为数组以便排序
        Job[] jobs = jobList.toArray(new Job[0]);

        // 3. 执行 SPT 排序 (核心步骤：按时间升序)
        java.util.Arrays.sort(jobs);

        // 4. 打印调度计划
        StdOut.println("===== SPT 调度计划 (按运行时间升序) =====");
        StdOut.printf("%-10s %s%n", "任务名", "运行时间");
        for (Job job : jobs) {
            StdOut.printf("%-10s %.2f%n", job.name, job.time);
        }

        // 5. (可选) 计算并打印平均完成时间
        // 完成时间 = 累计执行时间，平均 = 总完成时间 / 任务数
        double totalCompletionTime = 0.0;
        double elapsed = 0.0;
        for (Job job : jobs) {
            elapsed += job.time;
            totalCompletionTime += elapsed;
        }
        double avgCompletionTime = totalCompletionTime / jobs.length;
        StdOut.printf("%n平均完成时间：%.2f 单位时间%n", avgCompletionTime);
    }
}