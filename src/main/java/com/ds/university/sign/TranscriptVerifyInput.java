/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.sign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 验签输入解析：把"用户实际会粘贴的东西"还原成 (payload, signature)。
 *
 * <p>实际使用中三种粘贴方式都会出现：
 * <ol>
 *   <li><b>整个 signed JSON</b>：{"payload":"{...}","signature":"..." ...}（最省事，推荐）；</li>
 *   <li><b>分别粘贴</b> payload 与 signature；</li>
 *   <li><b>误粘贴 JSON 文件里 payload 的原文</b>：内容带 {\" 转义（下载后直接复制常见错误）。</li>
 * </ol>
 *
 * <p>第 1 种用 JSON 解析取出（解析后自动反转义）；第 3 种先按原样验，失败再尝试反转义。
 * 注意：反转义只在"原样验签失败"后尝试，避免改变本来合法的内容。
 */
public final class TranscriptVerifyInput {

    private final String payload;
    private final String signature;

    private TranscriptVerifyInput(String payload, String signature) {
        this.payload = payload;
        this.signature = signature;
    }

    public String payload() {
        return payload;
    }

    public String signature() {
        return signature;
    }

    /** 解析失败返回 null，由调用方给出友好提示 */
    public static TranscriptVerifyInput parse(ObjectMapper mapper, String document,
                                              String payloadParam, String signatureParam) {
        String documentText = trimToEmpty(document);
        String payload = trimToEmpty(payloadParam);
        String signature = trimToEmpty(signatureParam);

        if (!documentText.isEmpty()) {
            String[] extracted = extractFromDocument(mapper, documentText);
            if (extracted != null) {
                payload = extracted[0];
                signature = extracted[1];
            } else if (looksLikeTranscript(documentText)) {
                // 用户把 payload 贴进了"整段 JSON"框：视为 payload，签名仍取 signature 框
                payload = documentText;
            } else {
                return null;
            }
        }

        if (payload.isEmpty() || signature.isEmpty()) {
            return null;
        }
        return new TranscriptVerifyInput(payload, signature);
    }

    /** 若 payload 看起来是"被 JSON 转义的字符串"，返回反转义后的内容；否则返回 null。 */
    public static String tryUnescape(ObjectMapper mapper, String payload) {
        if (payload == null || payload.isEmpty() || !payload.contains("\\\"")) {
            return null;
        }
        try {
            String unescaped = mapper.readValue("\"" + payload + "\"", String.class);
            return unescaped.startsWith("{") ? unescaped : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String[] extractFromDocument(ObjectMapper mapper, String documentText) {
        // 只处理 JSON 对象；若解析失败说明用户贴的是原始 payload
        if (!documentText.startsWith("{")) {
            return null;
        }
        try {
            JsonNode root = mapper.readTree(documentText);
            JsonNode payloadNode = root.get("payload");
            JsonNode signatureNode = root.get("signature");
            if (payloadNode != null && payloadNode.isTextual()
                    && signatureNode != null && signatureNode.isTextual()) {
                return new String[]{payloadNode.asText(), signatureNode.asText()};
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean looksLikeTranscript(String text) {
        return text.startsWith("{") && text.contains("\"type\"") && text.contains("\"studentId\"");
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
