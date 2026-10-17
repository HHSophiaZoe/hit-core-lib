package com.hit.spring.event;

public interface EventPublisher {

    void publish(BaseAppEvent event);

}
