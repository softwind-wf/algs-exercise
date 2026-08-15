/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * Bean Validation 异常转译切面。
 *
 * <p>service 方法参数约束校验失败会抛出 {@link ConstraintViolationException}，
 * 本切面在 advice 链最外层将其统一转译为 BusinessException(PARAM_ERROR, 约束消息)，
 * 使 controller 现有的 catch (BusinessException) 流程（flash 错误提示 / Result.error）
 * 无需任何改动即可生效。</p>
 *
 * <p>多条约束同时违反时，消息按出现顺序去重后用「；」拼接。</p>
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionAspect {

    @Around("execution(* com.ds.university.service..*(..))")
    public Object translateConstraintViolation(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (ConstraintViolationException e) {
            String message = e.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .collect(Collectors.joining("；"));
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
}
