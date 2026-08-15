/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.util;

import java.util.regex.Pattern;

/**
 * 日志脱敏工具：在日志输出环节统一掩码敏感信息。
 * 覆盖：BCrypt 密码哈希、手机号、身份证号、password= 类参数、Session ID、CSRF token、Bearer token。
 * 规则按顺序应用；脱敏是尽力而为，永远不要依赖日志保存敏感信息。
 */
public final class SensitiveDataUtil {

    private SensitiveDataUtil() {
    }

    /** BCrypt 哈希（$2a$10$... 等） */
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    /** 身份证号：18 位，保留前 6 + 后 4（先于手机号执行，避免手机号规则误匹配身份证内的连续数字） */
    private static final Pattern ID_CARD = Pattern.compile("(\\d{6})\\d{8}(\\d{3}[0-9Xx])");

    /** 手机号：1[3-9] 开头 11 位（前后非数字，避免匹配更长数字串），保留前 3 + 后 4 */
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");

    /** password=xxx / password":"xxx" / password:'xxx' */
    private static final Pattern PASSWORD = Pattern.compile(
            "(?i)(password[\"']?\\s*[:=]\\s*[\"']?)[^&\\s\"',;]+");

    /** JSESSIONID=xxx / JSESSIONID:xxx（保留原大小写前缀） */
    private static final Pattern SESSION = Pattern.compile("(?i)(jsessionid[:=])[A-Za-z0-9]+");

    /** _csrf=xxx / csrfToken=xxx / csrf_token":"xxx" */
    private static final Pattern CSRF = Pattern.compile(
            "(?i)(_?csrf([_-]?token)?[\"']?\\s*[:=]\\s*[\"']?)[A-Za-z0-9+/=_-]+");

    /** Bearer token */
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._-]+");

    /** 掩码替换后的展示 */
    private static final String MASK = "***";

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        result = BCRYPT.matcher(result).replaceAll(MASK);
        result = ID_CARD.matcher(result).replaceAll("$1********$2");
        result = PHONE.matcher(result).replaceAll("$1****$2");
        result = PASSWORD.matcher(result).replaceAll("$1" + MASK);
        result = SESSION.matcher(result).replaceAll("$1" + MASK);
        result = CSRF.matcher(result).replaceAll("$1" + MASK);
        result = BEARER.matcher(result).replaceAll("$1" + MASK);
        return result;
    }
}
