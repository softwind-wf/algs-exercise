package exercise1.exercise1_3;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

public class FileListQueue1 {
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

        Queue<File> queue = new LinkedList<>();
        queue.add(root);

        // 预编译文件分隔符的正则表达式（兼容Windows和Linux/Mac）
        String separatorRegex = File.separator.equals("\\") ? "\\\\" : File.separator;
        // 获取根目录的路径深度（用于计算子文件的缩进）
        int rootDepth = root.getAbsolutePath().split(separatorRegex).length;

        while (!queue.isEmpty()) {
            File current = queue.poll();
            File[] files = current.listFiles();
            if (files == null) continue;

            for (File file : files) {
                // 计算当前文件的路径深度，减去根目录深度得到缩进层级
                int depth = file.getAbsolutePath().split(separatorRegex).length - rootDepth;
                // 打印缩进
                for (int i = 0; i < depth; i++) {
                    System.out.print("  ");
                }
                System.out.println(file.getName());

                if (file.isDirectory()) {
                    queue.add(file);
                }
            }
        }
    }
}