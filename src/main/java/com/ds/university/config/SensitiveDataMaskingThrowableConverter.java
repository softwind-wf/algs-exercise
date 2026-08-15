/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ds.university.util.SensitiveDataUtil;

/**
 * Logback 异常堆栈脱敏转换器：%maskEx —— 异常堆栈可能携带 SQL 参数/用户输入，
 * 渲染完成后整体掩码。在 logback-spring.xml 中注册：<conversionRule conversionWord="maskEx" .../>
 */
public class SensitiveDataMaskingThrowableConverter extends ExtendedThrowableProxyConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return SensitiveDataUtil.mask(super.convert(event));
    }
}
