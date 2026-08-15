/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.util;

/**
 * 全文检索关键字工具：
 * <ul>
 *   <li>清洗：仅保留字母（含中文）/数字/下划线/空格，剔除 MySQL FULLTEXT 布尔模式的
 *       运算符字符（+ - &gt; &lt; ( ) ~ * " @），避免用户输入破坏查询语法；</li>
 *   <li>分支：ngram 解析器最小词长为 2，1 字符关键字无法分词，回退 LIKE 查询。</li>
 * </ul>
 */
public final class SearchText {

    private SearchText() {
    }

    /** 清洗关键字用于 MATCH...AGAINST（布尔模式） */
    public static String sanitizeForBoolean(String keyword) {
        if (keyword == null) {
            return "";
        }
        return keyword.replaceAll("[^\\p{L}\\p{N}_ ]+", " ").trim();
    }

    /** 是否走全文索引（长度 >= 2）；否则回退 LIKE */
    public static boolean useFulltext(String keyword) {
        return keyword != null && keyword.length() >= 2;
    }
}
