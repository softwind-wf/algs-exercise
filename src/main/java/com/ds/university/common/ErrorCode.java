package com.ds.university.common;

/**
 * 业务错误码（与开发文档第 7 章保持一致）。
 */
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(4000, "参数错误"),
    PREREQ_NOT_DONE(4001, "先修课程未完成"),
    SECTION_FULL(4002, "开课班容量已满"),
    DUPLICATE_ENROLL(4003, "重复选课"),
    INVALID_GRADE(4004, "成绩取值非法或学生不在名单"),
    PREREQ_CYCLE(4005, "先修关系成环"),
    UNAUTHORIZED(4010, "未登录或无权限"),
    LOGIN_FAILED(4011, "账号或密码错误"),
    USER_DISABLED(4012, "账号已禁用"),
    NOT_FOUND(4040, "资源不存在"),
    INTERNAL_ERROR(5000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}