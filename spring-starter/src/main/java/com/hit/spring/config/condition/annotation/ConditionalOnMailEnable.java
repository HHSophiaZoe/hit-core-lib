package com.hit.spring.config.condition.annotation;

import com.hit.spring.config.condition.MailEnabledCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(MailEnabledCondition.class)
public @interface ConditionalOnMailEnable {
}