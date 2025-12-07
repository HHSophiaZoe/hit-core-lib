package com.hit.spring.core.event;

import com.hit.spring.context.TrackingContext;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
@ToString
public abstract class BaseAppEvent extends ApplicationEvent {

    private String correlationId;

    public BaseAppEvent(Object source) {
        super(source);
        this.correlationId = TrackingContext.getCorrelationId();
    }
}
