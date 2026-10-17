package com.hit.spring.event;

import com.hit.spring.tracing.TrackingContext;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
@ToString
public abstract class BaseAppEvent extends ApplicationEvent {

    private String traceId;

    public BaseAppEvent(Object source) {
        super(source);
        this.traceId = TrackingContext.getTraceId();
    }
}
