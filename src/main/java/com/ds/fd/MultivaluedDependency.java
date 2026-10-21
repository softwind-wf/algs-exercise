package com.ds.fd;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * 一条多值依赖 X -&gt;-&gt; Y（教材 8.6.1 节，4NF 的判据对象）。
 *
 * <p>语义：对 R 的任何取值，给定 X 的值后，Y 能取到的值集合与 R−X−Y 能取到的值集合
 * <b>相互独立</b> —— 表现为"X 的每个值对应 Y 的一组值，而这组值与其余属性的取值
 * 无关地重复出现"（教材 8.6 节的教师例子：ID -&gt;-&gt; dept_name 与 ID -&gt;-&gt; street,city
 * 各自独立，宽表里就出现两组地址信息的笛卡尔积式冗余）。
 *
 * <p>存储与打印约定同 {@link FunctionalDependency}：属性按字典序输出，结果稳定、可 diff；
 * {@code equals/hashCode} 按"属性集合"比较，因此 {@code AB -&gt;-&gt; C} 与 {@code BA -&gt;-&gt; C}
 * 视为同一条。本类不可变。
 */
public final class MultivaluedDependency {

    private final Set<String> determinant;   // X
    private final Set<String> dependent;     // Y

    public MultivaluedDependency(Set<String> determinant, Set<String> dependent) {
        this.determinant = Collections.unmodifiableSet(new LinkedHashSet<String>(determinant));
        this.dependent = Collections.unmodifiableSet(new LinkedHashSet<String>(dependent));
    }

    /** 便捷构造：{@code of("ID", "dept_name")} 表示 ID -&gt;-&gt; dept_name（一个字符一个属性）。 */
    public static MultivaluedDependency of(String lhs, String rhs) {
        return new MultivaluedDependency(FunctionalDependency.chars(lhs), FunctionalDependency.chars(rhs));
    }

    public Set<String> determinant() {
        return determinant;
    }

    public Set<String> dependent() {
        return dependent;
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
        return join(determinant) + " ->-> " + join(dependent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MultivaluedDependency)) {
            return false;
        }
        MultivaluedDependency other = (MultivaluedDependency) o;
        return determinant.equals(other.determinant) && dependent.equals(other.dependent);
    }

    @Override
    public int hashCode() {
        return 31 * determinant.hashCode() + dependent.hashCode();
    }
}
