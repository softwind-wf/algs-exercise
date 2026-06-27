// D:\downloads\algs4-master\algs4-master\src\main\java\test6\test6_3\WebIndex.java
package test6.test6_3;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WebIndex {
    private static final String DATA_FILE = "webindex.dat";
    private static final String META_FILE = "webindex.meta";

    private final int m;
    private final Map<String, Set<String>> invertedIndex = new TreeMap<>();
    private final Map<Integer, Page> cache = new HashMap<>();
    private int pageCount = 0;
    private int rootIdx = -1;

    public WebIndex(int m) {
        this.m = m;
    }

    // ========== 索引构建 ==========

    public void indexPage(String url, String content) {
        Set<String> words = extractWords(content);
        for (String word : words) {
            invertedIndex.computeIfAbsent(word, k -> new TreeSet<>()).add(url);
        }
        System.out.println("已索引: " + url + " (" + words.size() + " 个关键词)");
    }

    private Set<String> extractWords(String text) {
        String cleaned = text.replaceAll("<[^>]+>", " ").toLowerCase();
        Set<String> words = new TreeSet<>();
        for (String word : cleaned.split("[^a-z0-9\\u4e00-\\u9fa5]+")) {
            if (word.length() >= 2) words.add(word);
        }
        return words;
    }

    // ========== close(): 排序并写入 B-树 ==========

    public void close() {
        if (invertedIndex.isEmpty()) {
            System.out.println("索引为空，无需写入");
            return;
        }

        cache.clear();
        pageCount = 0;

        List<String> sortedKeys = new ArrayList<>(invertedIndex.keySet());
        Collections.sort(sortedKeys);

        System.out.println("\n构建B树... 共 " + sortedKeys.size() + " 个关键词");

        rootIdx = allocatePage(true);
        Page root = cache.get(rootIdx);

        for (String key : sortedKeys) {
            String value = String.join(",", invertedIndex.get(key));
            addToLeaf(root, key, value);
            if (root.isFull()) {
                Page left = root;
                Page right = root.split(cache, allocatePage(left.isBottom), m);
                rootIdx = allocatePage(false);
                root = cache.get(rootIdx);
                root.addChild(left.st.min(), left.index);
                root.addChild(right.st.min(), right.index);
            }
        }

        saveToDisk();
        System.out.println("B树已写入磁盘 (共 " + pageCount + " 个结点)");
        printTree();
    }

    private void addToLeaf(Page h, String key, String value) {
        if (h.isExternal()) {
            h.st.put(key, value);
            return;
        }
        Page child = h.next(key, cache);
        addToLeaf(child, key, value);
        if (child.isFull()) {
            Page newChild = child.split(cache, allocatePage(child.isBottom), m);
            h.addChild(newChild.st.min(), newChild.index);
        }
    }

    private int allocatePage(boolean isLeaf) {
        int idx = pageCount++;
        cache.put(idx, new Page(isLeaf, idx, m));
        return idx;
    }

    // ========== 搜索 ==========

    public String search(String keyword) {
        if (!loadFromDisk()) return null;
        Page root = cache.get(rootIdx);
        Page result = search(root, keyword.toLowerCase());
        if (result != null && result.contains(keyword.toLowerCase())) {
            return result.getLeafValue(keyword.toLowerCase());
        }
        return null;
    }

    private Page search(Page h, String key) {
        if (h.isExternal()) return h;
        Page child = h.next(key, cache);
        if (child == null) return null;
        return search(child, key);
    }

    // ========== 磁盘持久化 ==========

    private void saveToDisk() {
        try {
            // 写元数据
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(META_FILE)))) {
                pw.println(rootIdx);
                pw.println(pageCount);
                pw.println(m);
            }
            // 写页面数据
            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(Paths.get(DATA_FILE))))) {
                dos.writeInt(pageCount);
                for (int i = 0; i < pageCount; i++) {
                    Page p = cache.get(i);
                    byte[] data = Page.serialize(p);
                    dos.writeInt(data.length);
                    dos.write(data);
                }
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private boolean loadFromDisk() {
        if (!cache.isEmpty() && rootIdx >= 0) return true;
        try {
            Path metaPath = Paths.get(META_FILE);
            Path dataPath = Paths.get(DATA_FILE);
            if (!Files.exists(metaPath) || !Files.exists(dataPath)) return false;

            List<String> metaLines = Files.readAllLines(metaPath);
            rootIdx = Integer.parseInt(metaLines.get(0));
            pageCount = Integer.parseInt(metaLines.get(1));
            int savedM = Integer.parseInt(metaLines.get(2));

            cache.clear();
            try (DataInputStream dis = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(dataPath)))) {
                int count = dis.readInt();
                for (int i = 0; i < count; i++) {
                    int len = dis.readInt();
                    byte[] data = new byte[len];
                    dis.readFully(data);
                    Page p = Page.deserialize(data, savedM);
                    cache.put(p.index, p);
                }
            }
            return true;
        } catch (IOException e) { return false; }
    }

    // ========== 打印树结构 ==========

    private void printTree() {
        if (rootIdx < 0) return;
        Page root = cache.get(rootIdx);
        printTree(root, "");
    }

    private void printTree(Page h, String indent) {
        h.print(indent);
        if (!h.isExternal()) {
            List<String> keys = h.st.keyList();
            for (String k : keys) {
                Integer childIdx = (Integer) h.st.get(k);
                if (childIdx != null) {
                    printTree(cache.get(childIdx), indent + "  ");
                }
            }
        }
    }

    // ========== 主程序 ==========

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法:");
            System.out.println("  索引模式: java WebIndex <m> < index.txt");
            System.out.println("  搜索模式: java WebIndex search <keyword>");
            System.out.println();
            System.out.println("index.txt 格式 (每个网页):");
            System.out.println("  <URL>");
            System.out.println("  网页文本内容(可多行)");
            System.out.println("  <END>");
            return;
        }

        if ("search".equals(args[0]) && args.length >= 2) {
            doSearch(args[1]);
            return;
        }

        int m = Integer.parseInt(args[0]);
        WebIndex index = new WebIndex(m);

        System.out.println("=== 从标准输入读取网页 ===");
        System.out.println("(输入 <END> 结束每个网页，输入 Ctrl+D 结束所有输入)");
        System.out.println();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            String currentUrl = null;
            StringBuilder currentContent = new StringBuilder();

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (currentUrl == null) {
                    currentUrl = line;
                    currentContent = new StringBuilder();
                } else if ("<END>".equalsIgnoreCase(line)) {
                    index.indexPage(currentUrl, currentContent.toString());
                    currentUrl = null;
                } else {
                    currentContent.append(line).append(" ");
                }
            }
            if (currentUrl != null) {
                index.indexPage(currentUrl, currentContent.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        index.close();
    }

    private static void doSearch(String keywordFile) {
        WebIndex index = new WebIndex(4);
        if (!index.loadFromDisk()) {
            System.out.println("错误: 未找到索引文件，请先运行索引模式");
            return;
        }

        System.out.println("=== 搜索关键词 ===");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(keywordFile)))) {
            String word;
            while ((word = br.readLine()) != null) {
                word = word.trim();
                if (word.isEmpty()) continue;
                System.out.print("搜索 \"" + word + "\": ");
                String result = index.search(word);
                if (result != null) {
                    System.out.println(result);
                } else {
                    System.out.println("(未找到)");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
