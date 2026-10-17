package com.hit.spring.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@RequiredArgsConstructor
public class EventPublisherImpl implements EventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(BaseAppEvent event) {
        log.trace("{} publish event {}", event.getSource(), event);
        publisher.publishEvent(event);
    }
}
