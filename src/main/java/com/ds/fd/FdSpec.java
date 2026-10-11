package com.ds.fd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FD 用例文件的读取器（差分测试与不变量测试共用同一份输入格式）。
 *
 * <pre>
 * case 用例名
 * attrs ABC            # 属性集，一个字符一个属性
 * n 2                  # FD 条数（仅供人工核对，解析按逐行进行）
 * A B                  # A -> B
 * A C                  # A -> C
 * end
 * </pre>
 *
 * <p>以 {@code #} 开头的行与空行一律忽略。
 */
public final class FdSpec {

    private FdSpec() {
    }

    /** 一个用例：属性集 + FD 集合。 */
    public static final class Case {
        private final String name;
        private final Set<String> attrs;
        private final List<FunctionalDependency> fds;

        public Case(String name, Set<String> attrs, List<FunctionalDependency> fds) {
            this.name = name;
            this.attrs = attrs;
            this.fds = fds;
        }

        public String name() {
            return name;
        }

        public Set<String> attrs() {
            return attrs;
        }

        public List<FunctionalDependency> fds() {
            return fds;
        }
    }

    public static List<Case> load(Path file) throws IOException {
        return parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    public static List<Case> parse(String text) {
        List<Case> cases = new ArrayList<Case>();
        String name = null;
        Set<String> attrs = null;
        List<FunctionalDependency> fds = new ArrayList<FunctionalDependency>();

        for (String raw : text.split("\r?\n")) {
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("#")) {
                continue;
            }
            if (s.startsWith("case ")) {
                name = s.substring(5).trim();
                attrs = null;
                fds = new ArrayList<FunctionalDependency>();
            } else if (s.startsWith("attrs ")) {
                attrs = FunctionalDependency.chars(s.substring(6).trim());
            } else if (s.startsWith("n ")) {
                continue;
            } else if (s.equals("end")) {
                if (name == null || attrs == null) {
                    throw new IllegalArgumentException("用例缺少 case 或 attrs 行");
                }
                cases.add(new Case(name, attrs, fds));
                name = null;
                attrs = null;
            } else {
                String[] parts = s.split("\\s+");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("无法解析 FD 行: " + s);
                }
                fds.add(FunctionalDependency.of(parts[0], parts[1]));
            }
        }
        return cases;
    }
}
