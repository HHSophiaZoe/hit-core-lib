package com.hit.spring.core.event;

public interface EventPublisher {

    void publish(BaseAppEvent event);

}
