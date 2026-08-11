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
