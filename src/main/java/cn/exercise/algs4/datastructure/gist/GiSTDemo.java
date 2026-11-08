package cn.exercise.algs4.datastructure.gist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * GiST 区间树演示:建树、看树形、回答一次重叠查询,并与暴力扫描对账。
 *
 * <p>运行方式(编译后):</p>
 * <pre>java -cp target/classes cn.exercise.algs4.datastructure.gist.GiSTDemo</pre>
 */
public final class GiSTDemo {

    /**
     * 演示用的页容量,取小值让分裂早出现、树形可观察
     */
    private static final int MAX_ENTRIES_PER_PAGE = 4;

    private GiSTDemo() {
    }

    public static void main(String[] args) {
        GiST<Interval, String, Interval> tree = new GiST<>(new IntervalGiSTOps(), MAX_ENTRIES_PER_PAGE);

        Random random = new Random(20261108L);
        int total = 300;
        List<Interval> insertedKeys = new ArrayList<>(total);
        List<String> insertedValues = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            long start = random.nextInt(10000);
            Interval key = Interval.of(start, start + 1 + random.nextInt(200));
            String value = "rec-" + i;
            tree.insert(key, value);
            insertedKeys.add(key);
            insertedValues.add(value);
        }

        System.out.println("== 建树 ==");
        System.out.println("插入 " + total + " 条随机区间,每页最多 " + MAX_ENTRIES_PER_PAGE + " 条");
        System.out.println("size=" + tree.size()
                + ", height=" + tree.height()
                + ", nodes=" + tree.nodeCount()
                + ", leaves=" + tree.leafCount()
                + ", coverage=" + tree.checkCoverage()
                + ", balanced=" + tree.checkBalanced());
        System.out.println();
        System.out.println("== 树的前两层(内部页的 keys 就是包围区间) ==");
        System.out.print(tree.printTree(1));
        System.out.println();

        Interval query = Interval.of(5000, 5100);
        List<String> hits = tree.search(query);
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            if (insertedKeys.get(i).overlaps(query)) {
                expected.add(insertedValues.get(i));
            }
        }
        boolean same = hits.size() == expected.size()
                && new HashSet<>(hits).equals(new HashSet<>(expected));

        System.out.println("== 查询 ==");
        System.out.println("查询区间 " + query + ",与 [lo, hi] 重叠即命中");
        System.out.println("GiST 命中 " + hits.size() + " 条 : " + hits);
        System.out.println("暴力扫描命中 " + expected.size() + " 条 : " + expected);
        System.out.println("结果一致 = " + same);
        System.out.println("本次查询:访问节点 " + tree.lastVisitedNodes()
                + " 个,包围键判定 " + tree.lastConsistentChecks()
                + " 次;暴力扫描需逐条比较 " + total + " 次");
    }
}
