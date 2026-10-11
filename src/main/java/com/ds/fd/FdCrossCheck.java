package com.ds.fd;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;

/**
 * 用例批量体检：读用例文件 → 对每个用例算两种 F⁺ → 写出规范化结果。
 *
 * <p>典型用途：
 * <ul>
 *   <li>改了自己的 FD 例子后，看一眼两种算法的条数与缺口是否符合预期；</li>
 *   <li>把结果文件留档，回归时逐行 diff（哈希列保证集合逐位可比）。</li>
 * </ul>
 *
 * <p>运行：
 * <pre>
 * java -Dfile.encoding=UTF-8 -cp target\fd-classes com.ds.fd.FdCrossCheck \
 *      src\main\resources\fd\fd-cases.txt target\fd-crosscheck\result.txt
 * </pre>
 */
public final class FdCrossCheck {

    private FdCrossCheck() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("用法: FdCrossCheck <用例文件> <输出文件>");
            System.exit(2);
        }
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        List<FdSpec.Case> cases = FdSpec.load(in);

        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            w.write("# fd report: " + in + "\n");
            w.write("cases=" + cases.size() + "\n");
            w.write("# case <名> rounds=<图8-7迭代轮数> fplus=<图8-7条数> attrFplus=<X⁺法条数>"
                    + " nontrivial=<非平凡条数> delta=<缺口> hash=<X⁺法规范化SHA-256>\n");
            for (FdSpec.Case c : cases) {
                FdCalculator.Result naive = FdCalculator.closureNaive(c.attrs(), c.fds());
                Set<FunctionalDependency> byAttr = FdCalculator.closureByAttributeClosures(c.attrs(), c.fds());
                int nontrivial = 0;
                for (FunctionalDependency fd : byAttr) {
                    if (!fd.isTrivial()) {
                        nontrivial++;
                    }
                }
                w.write("case " + c.name()
                        + " rounds=" + naive.totalRounds()
                        + " fplus=" + naive.fplus().size()
                        + " attrFplus=" + byAttr.size()
                        + " nontrivial=" + nontrivial
                        + " delta=" + (byAttr.size() - naive.fplus().size())
                        + " hash=" + CanonicalHash.sha256(byAttr)
                        + "\n");
            }
        }
        System.out.println("[FdCrossCheck] 写出 " + cases.size() + " 个用例 -> " + out);
    }
}
