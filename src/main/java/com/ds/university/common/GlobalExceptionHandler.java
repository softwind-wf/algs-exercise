package com.ds.university.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理：页面请求统一跳转到 error 视图。
 * 后续新增 JSON 接口时，可在此补充 @ResponseBody 的处理方法。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, Model model) {
        model.addAttribute("errorCode", e.getCode());
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("系统异常", e);
        model.addAttribute("errorCode", ErrorCode.INTERNAL_ERROR.getCode());
        model.addAttribute("message", ErrorCode.INTERNAL_ERROR.getMessage());
        return "error";
    }
}