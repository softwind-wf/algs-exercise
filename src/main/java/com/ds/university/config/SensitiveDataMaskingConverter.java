package com.ds.university.config;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ds.university.util.SensitiveDataUtil;

/**
 * Logback 消息脱敏转换器：%mask —— 对日志消息统一掩码敏感信息。
 * 在 logback-spring.xml 中注册：<conversionRule conversionWord="mask" .../>
 */
public class SensitiveDataMaskingConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return SensitiveDataUtil.mask(event.getFormattedMessage());
    }
}
