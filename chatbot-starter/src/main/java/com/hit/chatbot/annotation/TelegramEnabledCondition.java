package com.hit.chatbot.annotation;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class TelegramEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String telegramEnable = context.getEnvironment().getProperty("telegram.enable");
        if (BooleanUtils.toBoolean(telegramEnable)) {
            return ConditionOutcome.match("Telegram is enabled");
        }
        return ConditionOutcome.noMatch("Telegram is not enabled");
    }
}