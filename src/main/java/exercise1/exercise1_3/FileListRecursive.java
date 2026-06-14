package exercise1.exercise1_3;

import java.io.File;

public class FileListRecursive {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java FileListRecursive <directory>");
            return;
        }
        File root = new File(args[0]);
        if (!root.exists() || !root.isDirectory()) {
            System.out.println("Invalid directory: " + args[0]);
            return;
        }
        listFiles(root, 0);
    }

    private static void listFiles(File dir, int depth) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            // 打印缩进
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.println(file.getName());

            if (file.isDirectory()) {
                // 递归处理子目录，深度+1
                listFiles(file, depth + 1);
            }
        }
    }
}