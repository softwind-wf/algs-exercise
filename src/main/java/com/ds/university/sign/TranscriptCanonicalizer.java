/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.sign;

import com.ds.university.vo.TranscriptRowVO;
import com.ds.university.vo.TranscriptVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 成绩单规范化（canonical）JSON：同样的数据必须生成逐字节相同的字符串。
 *
 * <p>为什么要自己拼：签名保护的是字节，若字段顺序 / 数字格式 / 空值写法不稳定，
 * 同一条成绩单两次签出的哈希会不同，验证端永远对不上。规则：
 * <ul>
 *   <li>字段顺序固定（与 {@link #canonicalJson} 里的顺序一致）；</li>
 *   <li>明细行按 (year, semester, courseId) 排序，与数据库返回顺序无关；</li>
 *   <li>数字用 BigDecimal 去掉多余 0（4.0 → "4"、3.37 → "3.37"）；</li>
 *   <li>字符串做最小转义（" \ 控制字符），null 写 null。</li>
 * </ul>
 *
 * <p>生产环境若已有统一编码规范，可替换为 JSON Canonicalization（JCS）实现，但必须保持确定性。
 */
public final class TranscriptCanonicalizer {

    /** 签名格式版本；改变字段集合时必须升级，避免旧签名被按新结构重放解释 */
    public static final String SCHEMA_VERSION = "v1";

    private static final String TYPE = "TRANSCRIPT";

    private TranscriptCanonicalizer() {
    }

    /**
     * 生成被签名的规范化 JSON。
     *
     * @param transcript 成绩单（行顺序无关，内部会排序）
     * @param serialNo   本次签发流水号（防重放）
     * @param issuedAt   签发时间（ISO-8601 带时区）
     */
    public static String canonicalJson(TranscriptVO transcript, String serialNo, String issuedAt) {
        List<TranscriptRowVO> rows = new ArrayList<>();
        if (transcript.getRows() != null) {
            rows.addAll(transcript.getRows());
        }
        rows.sort(Comparator
                .comparing(TranscriptRowVO::getYear, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TranscriptRowVO::getSemester, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TranscriptRowVO::getCourseId, Comparator.nullsLast(Comparator.naturalOrder())));

        StringBuilder json = new StringBuilder(512);
        json.append('{');
        json.append("\"type\":").append(quote(TYPE)).append(',');
        json.append("\"version\":").append(quote(SCHEMA_VERSION)).append(',');
        json.append("\"studentId\":").append(quote(transcript.getStudentId())).append(',');
        json.append("\"studentName\":").append(quote(transcript.getStudentName())).append(',');
        json.append("\"deptName\":").append(quote(transcript.getDeptName())).append(',');
        json.append("\"rows\":[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            TranscriptRowVO row = rows.get(i);
            json.append('{')
                    .append("\"courseId\":").append(quote(row.getCourseId())).append(',')
                    .append("\"title\":").append(quote(row.getTitle())).append(',')
                    .append("\"credits\":").append(number(row.getCredits())).append(',')
                    .append("\"semester\":").append(quote(row.getSemester())).append(',')
                    .append("\"year\":").append(number(row.getYear())).append(',')
                    .append("\"grade\":").append(quote(row.getGrade())).append(',')
                    .append("\"gradePoint\":").append(number(row.getGradePoint()))
                    .append('}');
        }
        json.append("],");
        json.append("\"courseCount\":").append(transcript.getCourseCount()).append(',');
        json.append("\"earnedCredits\":").append(transcript.getEarnedCredits()).append(',');
        json.append("\"gpa\":").append(number(transcript.getGpa())).append(',');
        json.append("\"issuedAt\":").append(quote(issuedAt)).append(',');
        json.append("\"serialNo\":").append(quote(serialNo));
        json.append('}');
        return json.toString();
    }

    /** 数字：整数直接输出；小数用 BigDecimal 去尾零，保证 4.0 与 4.00 得到同一字符串。 */
    private static String number(Number value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        return BigDecimal.valueOf(value.doubleValue()).stripTrailingZeros().toPlainString();
    }

    /** 最小 JSON 字符串转义（不依赖第三方库，避免版本差异影响确定性）。 */
    private static String quote(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(text.length() + 2);
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
            }
        }
        out.append('"');
        return out.toString();
    }
}
