// D:\downloads\algs4-master\algs4-master\src\main\java\test6\DiskManager.java
package test6.test6_2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DiskManager<Key extends Comparable<Key>> {
    private static final String DEFAULT_FILE = "btree.dat";

    private final String filePath;
    private final Map<Integer, byte[]> store = new HashMap<>();
    private final Map<Integer, Page<Key>> cache = new HashMap<>();
    private int pageCount = 0;
    private int rootIdx = -1;

    public DiskManager() { this(DEFAULT_FILE); }

    public DiskManager(String filePath) {
        this.filePath = filePath;
        loadFromDisk();
    }

    public int allocatePage(boolean isLeaf) {
        int idx = pageCount++;
        Page<Key> p = new Page<>(isLeaf, idx);
        cache.put(idx, p);
        return idx;
    }

    public Page<Key> getPage(int idx) {
        if (cache.containsKey(idx)) return cache.get(idx);
        if (!store.containsKey(idx)) throw new RuntimeException("页 " + idx + " 不存在");
        Page<Key> p = deserialize(store.get(idx));
        cache.put(idx, p);
        return p;
    }

    public void writePage(Page<Key> p) {
        store.put(p.pageIndex, serialize(p));
        cache.put(p.pageIndex, p);
        flushToDisk();
    }

    public void close() { flushToDisk(); }

    public int getRootIdx() { return rootIdx; }

    public void setRootIdx(int idx) {
        this.rootIdx = idx;
        flushToDisk();
    }

    public boolean hasExistingTree() { return rootIdx >= 0; }

    private byte[] serialize(Page<Key> p) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(p.pageIndex);
            dos.writeBoolean(p.isBottom);
            List<Key> keys = p.st.keyList();
            dos.writeInt(keys.size());
            for (Key k : keys) {
                dos.writeUTF(k.toString());
                if (!p.isBottom) {
                    Integer childIdx = p.st.get(k);
                    dos.writeInt(childIdx != null ? childIdx : -1);
                }
            }
            dos.flush();
            return bos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private Page<Key> deserialize(byte[] data) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            int idx = dis.readInt();
            boolean isLeaf = dis.readBoolean();
            int n = dis.readInt();
            Page<Key> p = new Page<>(isLeaf, idx);
            for (int i = 0; i < n; i++) {
                @SuppressWarnings("unchecked")
                Key k = (Key) dis.readUTF();
                if (isLeaf) {
                    p.st.put(k, null);
                } else {
                    int childIdx = dis.readInt();
                    p.st.put(k, childIdx);
                }
            }
            return p;
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private void loadFromDisk() {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            rootIdx = dis.readInt();
            int count = dis.readInt();
            for (int p = 0; p < count; p++) {
                int len = dis.readInt();
                byte[] data = new byte[len];
                dis.readFully(data);
                Page<Key> page = deserialize(data);
                store.put(page.pageIndex, data);
                pageCount = Math.max(pageCount, page.pageIndex + 1);
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private void flushToDisk() {
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(path)))) {
                dos.writeInt(rootIdx);
                dos.writeInt(store.size());
                for (Map.Entry<Integer, byte[]> entry : store.entrySet()) {
                    byte[] data = entry.getValue();
                    dos.writeInt(data.length);
                    dos.write(data);
                }
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}
