package com.hit.spring.core.event;

import com.hit.spring.core.data.event.BaseAppEvent;

public interface EventPublisher {

    void publish(BaseAppEvent event);

}
