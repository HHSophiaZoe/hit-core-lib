package com.hit.chatbot.annotation;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatBotMessageListener {
    
    /**
     * Platform: TELEGRAM, DISCORD
     */
    Platform[] platforms() default {Platform.TELEGRAM, Platform.DISCORD};
    
    /**
     * Chat ID hoặc Channel ID để filter message
     * Hỗ trợ:
     * - Property placeholder: "${telegram.chat.id}"
     * - SpEL: "#{@beanName.getChatId()}"
     */
    @Language("SpEL")
    String[] ids() default {};
    
    /**
     * Filter command: /start, /help, !ping, etc.
     * Hỗ trợ:
     * - Giá trị trực tiếp: "/start"
     * - Property placeholder: "${bot.commands.start}"
     * - SpEL: "#{@commandConfig.getStartCommand()}"
     */
    String[] commands() default {};
    
    enum Platform {
        TELEGRAM, DISCORD
    }
}