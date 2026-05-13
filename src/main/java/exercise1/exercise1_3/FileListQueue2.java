package exercise1.exercise1_3;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

public class FileListQueue2 {
    // 队列中存储的元素，包含文件和其缩进深度
    private static class QueueEntry {
        File file;
        int depth;
        QueueEntry(File file, int depth) {
            this.file = file;
            this.depth = depth;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java FileListQueue <directory>");
            return;
        }
        File root = new File(args[0]);
        if (!root.exists() || !root.isDirectory()) {
            System.out.println("Invalid directory: " + args[0]);
            return;
        }

        Queue<QueueEntry> queue = new LinkedList<>();
        // 根目录的深度为0
        queue.add(new QueueEntry(root, 0));

        while (!queue.isEmpty()) {
            QueueEntry entry = queue.poll();
            File currentDir = entry.file;
            int currentDepth = entry.depth;

            File[] files = currentDir.listFiles();
            if (files == null) continue;

            for (File file : files) {
                // 打印缩进
                for (int i = 0; i < currentDepth + 1; i++) {
                    System.out.print("  ");
                }
                System.out.println(file.getName());

                if (file.isDirectory()) {
                    // 子文件夹的深度是当前深度+1
                    queue.add(new QueueEntry(file, currentDepth + 1));
                }
            }
        }
    }
}