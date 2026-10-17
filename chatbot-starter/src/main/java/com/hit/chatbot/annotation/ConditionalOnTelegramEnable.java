package com.hit.chatbot.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Matches when {@code chatbot.telegram.enable=true}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ConditionalOnBooleanProperty("chatbot.telegram.enable")
public @interface ConditionalOnTelegramEnable {
}
