package com.hit.chatbot.annotation;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Order(-2147483608)
public class DiscordEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enable = context.getEnvironment().getProperty("chatbot.discord.enable");
        if (BooleanUtils.toBoolean(enable)) {
            return ConditionOutcome.match("Discord chat is enabled");
        }
        return ConditionOutcome.noMatch("Discord chat is not enabled");
    }

}