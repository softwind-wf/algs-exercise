package com.ds.university.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 搜索关键字工具单元测试：布尔模式清洗与全文分支判定。 */
class SearchTextTest {

    @Test
    void sanitizeRemovesBooleanOperators() {
        assertEquals("索引 原理", SearchText.sanitizeForBoolean("索引+原理"));
        assertEquals("数据 结构", SearchText.sanitizeForBoolean("数据-结构()~*\"@"));
        assertEquals("Srinivasan", SearchText.sanitizeForBoolean("Srinivasan"));
    }

    @Test
    void sanitizeKeepsChineseLettersDigitsUnderscore() {
        assertEquals("中文_name 123", SearchText.sanitizeForBoolean("中文_name 123"));
    }

    @Test
    void useFulltextRequiresTwoChars() {
        assertFalse(SearchText.useFulltext(""));
        assertFalse(SearchText.useFulltext("数"));
        assertTrue(SearchText.useFulltext("数据"));
        assertTrue(SearchText.useFulltext("ab"));
    }
}
