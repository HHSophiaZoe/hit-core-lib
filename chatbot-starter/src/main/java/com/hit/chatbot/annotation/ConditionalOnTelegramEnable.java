package com.hit.chatbot.annotation;

import org.springframework.context.annotation.Conditional;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(TelegramEnabledCondition.class)
public @interface ConditionalOnTelegramEnable {
}