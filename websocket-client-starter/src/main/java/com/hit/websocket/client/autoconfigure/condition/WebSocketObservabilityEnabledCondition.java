package com.hit.websocket.client.autoconfigure.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class WebSocketObservabilityEnabledCondition extends SpringBootCondition {

    private static final String PROPERTY = "websocket-client.observability.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean enabled = context.getEnvironment().getProperty(PROPERTY, Boolean.class, false);
        return enabled ? ConditionOutcome.match("WebSocket client observability is enabled")
                : ConditionOutcome.noMatch("WebSocket client observability is disabled");
    }
}
