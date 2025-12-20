package com.hit.chatbot.annotation;

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
     */
    String[] ids() default {};
    
    /**
     * Filter command: /start, /help, !ping, etc.
     */
    String[] commands() default {};
    
    enum Platform {
        TELEGRAM, DISCORD
    }
}