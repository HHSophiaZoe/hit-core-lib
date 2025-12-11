package com.hit.chatbot.annotation;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class DiscordEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("bot.discord.enable");
        return BooleanUtils.toBoolean(enabled);
    }

}