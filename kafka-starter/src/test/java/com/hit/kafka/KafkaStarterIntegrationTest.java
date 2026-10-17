package com.hit.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.backoff.FixedBackOff;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Two real Kafka brokers (KRaft) through Testcontainers, as the clusters main and market. With two clusters there is
 * no default: every template is injected by qualifier and every listener names its container factory.
 */
@Testcontainers
@SpringBootTest(classes = KafkaStarterIntegrationTest.Beans.class, properties = {
        "messaging.kafka.clusters.main.consumer.group-id=main-it",
        "messaging.kafka.clusters.main.consumer.auto-offset-reset=earliest",
        "messaging.kafka.clusters.main.producer.delivery-timeout=20s",
        "messaging.kafka.clusters.main.producer.request-timeout=10s",
        "messaging.kafka.clusters.market.consumer.group-id=market-it",
        "messaging.kafka.clusters.market.consumer.auto-offset-reset=earliest",
        "messaging.kafka.clusters.market.consumer.max-poll-interval=40s"
})
class KafkaStarterIntegrationTest {

    // Started and closed by the @Testcontainers extension
    @SuppressWarnings("resource")
    @Container
    static final KafkaContainer MAIN = new KafkaContainer("apache/kafka-native:4.1.0");

    // Started and closed by the @Testcontainers extension
    @SuppressWarnings("resource")
    @Container
    static final KafkaContainer MARKET = new KafkaContainer("apache/kafka-native:4.1.0");

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        registry.add("messaging.kafka.clusters.main.bootstrap-servers", MAIN::getBootstrapServers);
        registry.add("messaging.kafka.clusters.market.bootstrap-servers", MARKET::getBootstrapServers);
    }

    record Event(String symbol, Instant at, LocalDate tradingDate, long qty) {
    }

    @Autowired
    private TestPublisher publisher;

    @Autowired
    @Qualifier("marketKafkaTemplate")
    private KafkaTemplate<String, String> marketTemplate;

    @Autowired
    private Received received;

    @Autowired
    private Failures failures;

    @Test
    void publisherSendsJsonThroughThePrimaryClusterToADefaultListener() throws Exception {
        Event event = new Event("VN30F1M", Instant.parse("2026-10-17T02:15:00Z"), LocalDate.of(2026, 10, 17), 3);

        assertThat(publisher.pushSync("orders", "k1", event, Map.of("source", "it"))).isTrue();

        String payload = received.mainOrders.poll(30, TimeUnit.SECONDS);
        assertThat(payload).isNotNull();
        JsonNode json = JsonMapper.builder().build().readTree(payload);
        assertThat(json.get("symbol").stringValue()).isEqualTo("VN30F1M");
        assertThat(json.get("at").stringValue()).isEqualTo("2026-10-17T02:15:00Z");
        assertThat(json.get("tradingDate").stringValue()).isEqualTo("2026-10-17");
        assertThat(json.get("qty").longValue()).isEqualTo(3);
    }

    @Test
    void clustersAreIndependent() throws Exception {
        marketTemplate.send("ticks", "from-market").get(30, TimeUnit.SECONDS);

        assertThat(received.marketTicks.poll(30, TimeUnit.SECONDS)).isEqualTo("from-market");
        // Same topic name on the primary cluster: nothing was sent there
        assertThat(received.mainTicks.poll(2, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void batchListenerReceivesAList() throws Exception {
        for (int i = 0; i < 3; i++) {
            marketTemplate.send("ticks-batch", "tick-" + i);
        }
        marketTemplate.flush();

        List<String> all = new java.util.ArrayList<>();
        while (all.size() < 3) {
            List<String> batch = received.marketBatches.poll(30, TimeUnit.SECONDS);
            assertThat(batch).as("batch after %s", all).isNotNull();
            all.addAll(batch);
        }
        assertThat(all).containsExactly("tick-0", "tick-1", "tick-2");
    }

    @Test
    void applicationErrorHandlerServesBothClusters() throws Exception {
        publisher.pushSync("fail-main", "boom");
        marketTemplate.send("fail-market", "boom").get(30, TimeUnit.SECONDS);

        List<String> topics = List.of(failures.topics.poll(30, TimeUnit.SECONDS), failures.topics.poll(30, TimeUnit.SECONDS));
        assertThat(topics).containsExactlyInAnyOrder("fail-main", "fail-market");
    }

    @Test
    void springBootKafkaBeansAreLeftOut(@Autowired org.springframework.context.ApplicationContext context) {
        assertThat(context.containsBean("kafkaListenerContainerFactory")).isFalse();
        assertThat(context.containsBean("kafkaTemplate")).isFalse();
        assertThat(context.getBeansOfType(KafkaTemplate.class)).containsOnlyKeys("mainKafkaTemplate", "marketKafkaTemplate");
    }

    @Test
    void eachAdminWorksOnItsOwnBroker(@Autowired @Qualifier("mainKafkaAdmin") KafkaAdmin mainAdmin,
                                      @Autowired @Qualifier("marketKafkaAdmin") KafkaAdmin marketAdmin) {
        marketAdmin.createOrModifyTopics(new NewTopic("market-only", 1, (short) 1));

        assertThat(marketAdmin.describeTopics("market-only")).containsKey("market-only");
        assertThatThrownBy(() -> mainAdmin.describeTopics("market-only"))
                .hasRootCauseInstanceOf(UnknownTopicOrPartitionException.class);
    }

    static class TestPublisher extends KafkaPublisher {
        TestPublisher(KafkaTemplate<String, String> kafkaTemplate) {
            super(kafkaTemplate);
        }
    }

    static class Received {
        final BlockingQueue<String> mainOrders = new LinkedBlockingQueue<>();
        final BlockingQueue<String> mainTicks = new LinkedBlockingQueue<>();
        final BlockingQueue<String> marketTicks = new LinkedBlockingQueue<>();
        final BlockingQueue<List<String>> marketBatches = new LinkedBlockingQueue<>();

        @KafkaListener(topics = "orders", containerFactory = "mainKafkaListenerContainerFactory")
        public void onOrder(String message) {
            mainOrders.add(message);
        }

        @KafkaListener(topics = "ticks", groupId = "main-ticks", containerFactory = "mainKafkaListenerContainerFactory")
        public void onMainTick(String message) {
            mainTicks.add(message);
        }

        @KafkaListener(topics = "ticks", containerFactory = "marketKafkaListenerContainerFactory")
        public void onMarketTick(String message) {
            marketTicks.add(message);
        }

        @KafkaListener(topics = "ticks-batch", batch = "true", containerFactory = "marketKafkaListenerContainerFactory")
        public void onMarketBatch(List<String> messages) {
            marketBatches.add(messages);
        }

        @KafkaListener(topics = "fail-main", containerFactory = "mainKafkaListenerContainerFactory")
        public void failOnMain(String message) {
            throw new IllegalStateException(message);
        }

        @KafkaListener(topics = "fail-market", containerFactory = "marketKafkaListenerContainerFactory")
        public void failOnMarket(String message) {
            throw new IllegalStateException(message);
        }
    }

    static class Failures {
        final BlockingQueue<String> topics = new LinkedBlockingQueue<>();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Beans {

        @Bean
        TestPublisher testPublisher(@Qualifier("mainKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
            return new TestPublisher(kafkaTemplate);
        }

        @Bean
        Received received() {
            return new Received();
        }

        @Bean
        Failures failures() {
            return new Failures();
        }

        /**
         * No retry; the recoverer records which topic failed.
         */
        @Bean
        CommonErrorHandler errorHandler(Failures failures) {
            return new DefaultErrorHandler((ConsumerRecord<?, ?> record, Exception e) -> failures.topics.add(record.topic()),
                    new FixedBackOff(0, 0));
        }
    }
}
