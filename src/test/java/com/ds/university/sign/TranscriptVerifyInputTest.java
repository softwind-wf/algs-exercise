/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.sign;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验签输入解析：整段 JSON / 分开粘贴 / 误粘贴转义 payload 三种情况。 */
class TranscriptVerifyInputTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String PAYLOAD = "{\"type\":\"TRANSCRIPT\",\"studentId\":\"00128\",\"gpa\":3.5}";
    private static final String SIGNATURE = "QUJDRA==";

    @Test
    @DisplayName("整个 signed JSON：自动取出 payload 与 signature")
    void parseWholeDocument() {
        String document = "{\"studentId\":\"00128\",\"payload\":" + quote(PAYLOAD)
                + ",\"signature\":\"" + SIGNATURE + "\"}";
        TranscriptVerifyInput input = TranscriptVerifyInput.parse(mapper, document, null, null);
        assertNotNull(input);
        assertEquals(PAYLOAD, input.payload());
        assertEquals(SIGNATURE, input.signature());
    }

    @Test
    @DisplayName("分别粘贴 payload 与 signature")
    void parseSeparateFields() {
        TranscriptVerifyInput input = TranscriptVerifyInput.parse(mapper, null, PAYLOAD, SIGNATURE);
        assertNotNull(input);
        assertEquals(PAYLOAD, input.payload());
        assertEquals(SIGNATURE, input.signature());
    }

    @Test
    @DisplayName("从 JSON 文件里直接复制的转义 payload：能识别并反转义")
    void unescapeCopiedPayload() {
        String escaped = PAYLOAD.replace("\"", "\\\"");
        assertTrue(escaped.contains("\\\""));
        String unescaped = TranscriptVerifyInput.tryUnescape(mapper, escaped);
        assertNotNull(unescaped);
        assertEquals(PAYLOAD, unescaped);
    }

    @Test
    @DisplayName("误把 payload 贴进整段 JSON 框：仍能识别")
    void documentMayBePayload() {
        TranscriptVerifyInput input = TranscriptVerifyInput.parse(mapper, PAYLOAD, null, SIGNATURE);
        assertNotNull(input);
        assertEquals(PAYLOAD, input.payload());
        assertEquals(SIGNATURE, input.signature());
    }

    @Test
    @DisplayName("无法识别的输入 / 缺签名：返回 null")
    void invalidInput() {
        assertNull(TranscriptVerifyInput.parse(mapper, "hello world", null, null));
        assertNull(TranscriptVerifyInput.parse(mapper, null, PAYLOAD, null));
        assertNull(TranscriptVerifyInput.parse(mapper, null, null, SIGNATURE));
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
