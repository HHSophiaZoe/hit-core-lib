package com.hit.spring.event;

import com.hit.spring.autoconfigure.HitSpringAutoConfiguration;
import com.hit.spring.autoconfigure.HitTaskExecutionAutoConfiguration;
import com.hit.spring.tracing.TrackingContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EventPublisherImplTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, HitSpringAutoConfiguration.class,
                    HitTaskExecutionAutoConfiguration.class))
            .withPropertyValues("spring.threads.virtual.enabled=true", "app.task.executor.enable=true")
            .withUserConfiguration(ListenerConfiguration.class);

    @Test
    void asyncListenerGetsTheEventAndTheTraceIdOfThePublisher() {
        context.run(application -> {
            OrderListener listener = application.getBean(OrderListener.class);
            TrackingContext.setTraceId("publisher-trace");
            try {
                application.getBean(EventPublisher.class).publish(new OrderCreatedEvent(this, "order-1"));
            } finally {
                TrackingContext.clearContext();
            }

            Received received = listener.received().get(5, TimeUnit.SECONDS);
            assertThat(received.event().getOrderId()).isEqualTo("order-1");
            assertThat(received.event().getTraceId()).isEqualTo("publisher-trace");
            // @Async runs on appTaskExecutor (virtual thread) and the trace id follows the task there
            assertThat(received.virtualThread()).isTrue();
            assertThat(received.threadTraceId()).isEqualTo("publisher-trace");
        });
    }

    static class OrderCreatedEvent extends BaseAppEvent {

        private final String orderId;

        OrderCreatedEvent(Object source, String orderId) {
            super(source);
            this.orderId = orderId;
        }

        String getOrderId() {
            return orderId;
        }
    }

    record Received(OrderCreatedEvent event, boolean virtualThread, String threadTraceId) {
    }

    static class OrderListener {

        private final CompletableFuture<Received> received = new CompletableFuture<>();

        // Read through a method: the bean is an @Async proxy, its own fields are not populated
        public CompletableFuture<Received> received() {
            return received;
        }

        @Async
        @EventListener
        public void on(OrderCreatedEvent event) {
            received.complete(new Received(event, Thread.currentThread().isVirtual(), TrackingContext.currentTraceId()));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ListenerConfiguration {

        @Bean
        OrderListener orderListener() {
            return new OrderListener();
        }
    }
}
