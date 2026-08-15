/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.common;

/**
 * 403 禁止访问：已登录但角色不匹配、CSRF 校验失败等场景抛出，
 * 由 GlobalExceptionHandler 统一渲染友好的 403 错误页（HTTP 状态码保持 403）。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
