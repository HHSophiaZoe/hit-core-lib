package com.hit.spring.core.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisherImpl implements EventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(BaseAppEvent event) {
        log.info("{} publish event {}", event.getSource(), event);
        publisher.publishEvent(event);
    }
}
