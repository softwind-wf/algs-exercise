package com.ds.university.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

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

    /**
     * 兜底：正常路径下 service 层的约束违例已被 ValidationExceptionAspect 转译为 BusinessException，
     * 此处仅拦截绕过切面的漏网场景（如未来新增非 service 包的 @Validated 调用）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(ConstraintViolationException e, Model model) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining("；"));
        model.addAttribute("errorCode", ErrorCode.PARAM_ERROR.getCode());
        model.addAttribute("message", message);
        return "error";
    }

    @ExceptionHandler(ForbiddenException.class)
    public String handleForbidden(ForbiddenException e, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("errorCode", HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("message", e.getMessage());
        return "error/403";
    }

    /** 上传文件超过 Spring multipart 上限（进入 Controller 前即被拦截），给出友好提示 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException e, Model model) {
        model.addAttribute("errorCode", ErrorCode.PARAM_ERROR.getCode());
        model.addAttribute("message", "上传的图片大小超过限制（最大 5MB）");
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