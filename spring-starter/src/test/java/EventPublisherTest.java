import com.hit.spring.SpringStarterConfig;
import com.hit.spring.context.TrackingContext;
import com.hit.spring.core.event.EventPublisher;
import data.event.AppOrderCreateEvent;
import data.event.AppOrderUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest(classes = {
        SpringStarterConfig.class,
        EventPublisherTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.threads.virtual.enabled=true",
        "app.task.executor.enable=true",
        "app.task.executor.thread-name-prefix=unit-test-"
})
public class EventPublisherTest {

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private EventCaptor eventCaptor;

    private static final String CORRELATION_ID_DEFAULT = "unit-test";

    @AfterEach
    public void cleanup() {
        TrackingContext.clearTrackingContext();
        log.info("[EventPublisherTest] cleanup");
    }

    @Test
    public void testPublisher() throws InterruptedException {
        TrackingContext.setCorrelationId(CORRELATION_ID_DEFAULT);

        AppOrderCreateEvent orderCreate = new AppOrderCreateEvent(this)
                .setOrderId("1")
                .setOrderName("Order Create");
        publisher.publish(orderCreate);

        AppOrderUpdateEvent orderUpdate = new AppOrderUpdateEvent(this)
                .setOrderId("2")
                .setOrderName("Order Update");
        publisher.publish(orderUpdate);

        // Verify events received
        eventCaptor.awaitEvents(2, 5, TimeUnit.SECONDS);

        List<AppOrderCreateEvent> orderCreateEvents = eventCaptor.getOrderCreateEvents();
        List<AppOrderUpdateEvent> orderUpdateEvents = eventCaptor.getOrderUpdateEvents();

        Assertions.assertEquals(1, orderCreateEvents.size());
        Assertions.assertEquals(1, orderUpdateEvents.size());

        AppOrderCreateEvent receivedOrderCreate = orderCreateEvents.getFirst();
        AppOrderUpdateEvent receivedOrderUpdate = orderUpdateEvents.getFirst();

        Assertions.assertEquals(orderCreate.getOrderId(), receivedOrderCreate.getOrderId());
        Assertions.assertEquals(orderCreate.getOrderName(), receivedOrderCreate.getOrderName());
        Assertions.assertEquals(CORRELATION_ID_DEFAULT, receivedOrderCreate.getCorrelationId());

        Assertions.assertEquals(orderUpdate.getOrderId(), receivedOrderUpdate.getOrderId());
        Assertions.assertEquals(orderUpdate.getOrderName(), receivedOrderUpdate.getOrderName());
        Assertions.assertEquals(CORRELATION_ID_DEFAULT, receivedOrderUpdate.getCorrelationId());
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean(destroyMethod = "reset")
        public EventCaptor eventCaptor() {
            return new EventCaptor();
        }
    }

    @Slf4j
    public static class EventCaptor {

        private final List<AppOrderCreateEvent> orderCreateEvents = new CopyOnWriteArrayList<>();
        private final List<AppOrderUpdateEvent> orderUpdateEvents = new CopyOnWriteArrayList<>();
        private final CountDownLatch eventLatch = new CountDownLatch(2);

        @Async("appTaskExecutor")
        @EventListener(AppOrderCreateEvent.class)
        public void handleOrderCreateEvent(AppOrderCreateEvent dto) {
            log.info("[EventCaptor] AppOrderCreateEvent: {}", dto);
            orderCreateEvents.add(dto);
            eventLatch.countDown();
        }

        @Async("appTaskExecutor")
        @EventListener(AppOrderUpdateEvent.class)
        public void handleOrderUpdateEvent(AppOrderUpdateEvent dto) {
            log.info("[EventCaptor] AppOrderUpdateEvent: {}", dto);
            orderUpdateEvents.add(dto);
            eventLatch.countDown();
        }

        public void awaitEvents(int expectedCount, long timeout, TimeUnit unit) throws InterruptedException {
            boolean received = eventLatch.await(timeout, unit);
            if (!received) {
                throw new AssertionError("Expected " + expectedCount + " events but only received " +
                        (orderCreateEvents.size() + orderUpdateEvents.size()) + " within timeout");
            }
        }

        public List<AppOrderCreateEvent> getOrderCreateEvents() {
            return new ArrayList<>(orderCreateEvents);
        }

        public List<AppOrderUpdateEvent> getOrderUpdateEvents() {
            return new ArrayList<>(orderUpdateEvents);
        }

        public void reset() {
            orderCreateEvents.clear();
            orderUpdateEvents.clear();
            log.info("[EventCaptor] cleared");
        }
    }
}