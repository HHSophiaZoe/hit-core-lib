package com.hit.cache.annotation;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Order(-2147483608)
public class InternalCacheEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String telegramEnable = context.getEnvironment().getProperty("cache.internal.enable");
        if (BooleanUtils.toBoolean(telegramEnable)) {
            return ConditionOutcome.match("Internal cache is enabled");
        }
        return ConditionOutcome.noMatch("Internal cache is not enabled");
    }

}