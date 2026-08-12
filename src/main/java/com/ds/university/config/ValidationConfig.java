package com.ds.university.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import javax.validation.Validator;

/**
 * Bean Validation 统一拦截配置。
 *
 * <p>service 加 {@code @Validated} 后，方法参数上的约束注解（@NotBlank/@Min 等）
 * 由 MethodValidationPostProcessor 织入校验；校验失败抛出的
 * {@link javax.validation.ConstraintViolationException} 由
 * {@link ValidationExceptionAspect} 统一转译为 BusinessException(PARAM_ERROR)，
 * 复用现有的 flash 错误提示 / Result.error 渲染流程。</p>
 */
@Configuration
public class ValidationConfig {

    /**
     * 方法级校验后处理器。
     *
     * <p>显式设为最低优先级，让校验拦截器位于 advice 链最内层，
     * 保证 {@link ValidationExceptionAspect}（最高优先级、最外层）能捕获并转译异常。</p>
     *
     * <p>本 Bean 占据 Boot 自动配置的 @ConditionalOnMissingBean 位置，
     * Boot 不会再创建默认实例。</p>
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor(Validator validator) {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator);
        processor.setOrder(Ordered.LOWEST_PRECEDENCE);
        return processor;
    }
}
