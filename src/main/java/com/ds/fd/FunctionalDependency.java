package com.ds.fd;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * 一条函数依赖 X → Y（X 为决定因素，Y 为被决定因素）。
 *
 * <p>用 {@code Set<String>} 存属性集，属性名按字典序输出，保证打印结果稳定、可 diff。
 * 本类不可变；{@code equals/hashCode} 按"属性集合"比较，因此 {@code AB → C} 与 {@code BA → C} 视为同一条。
 */
public final class FunctionalDependency {

    private final Set<String> determinant;   // X
    private final Set<String> dependent;     // Y

    public FunctionalDependency(Set<String> determinant, Set<String> dependent) {
        this.determinant = Collections.unmodifiableSet(new LinkedHashSet<String>(determinant));
        this.dependent = Collections.unmodifiableSet(new LinkedHashSet<String>(dependent));
    }

    /** 便捷构造：{@code of("CG", "H")} 表示 CG → H（一个字符一个属性）。 */
    public static FunctionalDependency of(String lhs, String rhs) {
        return new FunctionalDependency(chars(lhs), chars(rhs));
    }

    public static Set<String> chars(String attrs) {
        Set<String> set = new LinkedHashSet<String>();
        for (int i = 0; i < attrs.length(); i++) {
            set.add(String.valueOf(attrs.charAt(i)));
        }
        return set;
    }

    public Set<String> determinant() {
        return determinant;
    }

    public Set<String> dependent() {
        return dependent;
    }

    /** 平凡依赖：右边 ⊆ 左边（例如 AB → A）。讨论范式时一律忽略。 */
    public boolean isTrivial() {
        return determinant.containsAll(dependent);
    }

    private static String join(Set<String> attrs) {
        StringBuilder sb = new StringBuilder();
        for (String a : new TreeSet<String>(attrs)) {
            sb.append(a);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return join(determinant) + " -> " + join(dependent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FunctionalDependency)) {
            return false;
        }
        FunctionalDependency other = (FunctionalDependency) o;
        return determinant.equals(other.determinant) && dependent.equals(other.dependent);
    }

    @Override
    public int hashCode() {
        return 31 * determinant.hashCode() + dependent.hashCode();
    }
}
