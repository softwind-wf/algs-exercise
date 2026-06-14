package exercise1.exercise1_3;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

public class FileListQueue {

    // 队列中存储的条目：文件 + 缩进深度
    private static class Entry {
        File file;
        int depth;

        Entry(File file, int depth) {
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

        // 先打印根目录（深度0）
        System.out.println(root.getName());

        Queue<Entry> queue = new LinkedList<>();
        // 根目录的子内容深度从1开始
        queue.add(new Entry(root, 0));

        while (!queue.isEmpty()) {
            Entry entry = queue.poll();
            File currentDir = entry.file;
            int currentDepth = entry.depth;

            File[] children = currentDir.listFiles();
            if (children == null) continue;

            for (File child : children) {
                // 打印缩进
                for (int i = 0; i < currentDepth + 1; i++) {
                    System.out.print("  ");
                }
                System.out.println(child.getName());

                if (child.isDirectory()) {
                    // 子文件夹的深度+1，入队
                    queue.add(new Entry(child, currentDepth + 1));
                }
            }
        }
    }
}