package com.hit.kafka.autoconfigure;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bean wiring; nothing connects to a broker. With clusters, Spring Boot's Kafka auto-configuration is left out by
 * {@link KafkaClustersImportFilter}, which only acts under @EnableAutoConfiguration, so these runners do not list it.
 */
class HitKafkaAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, HitKafkaAutoConfiguration.class));

    private final ApplicationContextRunner twoClusters = runner.withPropertyValues(
            "messaging.kafka.clusters.main.bootstrap-servers=main-1:9092",
            "messaging.kafka.clusters.main.consumer.group-id=main-group",
            "messaging.kafka.clusters.market.bootstrap-servers=market-1:9092",
            "messaging.kafka.clusters.market.consumer.group-id=market-group");

    @Test
    void withoutClustersSpringBootConfiguresKafka() {
        runner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBeans(KafkaTemplate.class).containsOnlyKeys("kafkaTemplate");
                    assertThat(context).doesNotHaveBean("mainKafkaTemplate");
                });
    }

    @Test
    void singleClusterAlsoAnswersToTheDefaultNames() {
        runner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
                .withPropertyValues("messaging.kafka.clusters.main.bootstrap-servers=main-1:9092")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    // Even next to Spring Boot's auto-configuration, which backs off
                    assertThat(context).getBeans(KafkaTemplate.class).containsOnlyKeys("mainKafkaTemplate");
                    assertThat(context).getBeans(KafkaAdmin.class).containsOnlyKeys("mainKafkaAdmin");
                    assertThat(context.getBean(KafkaTemplate.class)).isSameAs(context.getBean("mainKafkaTemplate"));
                    assertThat(context.getBean("kafkaTemplate")).isSameAs(context.getBean("mainKafkaTemplate"));
                    assertThat(context.getBean("kafkaListenerContainerFactory"))
                            .isSameAs(context.getBean("mainKafkaListenerContainerFactory"));
                });
    }

    @Test
    void severalClustersHaveNoDefault() {
        twoClusters.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBeans(KafkaTemplate.class).containsOnlyKeys("mainKafkaTemplate", "marketKafkaTemplate");
            assertThat(context).getBeans(ProducerFactory.class).containsOnlyKeys("mainKafkaProducerFactory", "marketKafkaProducerFactory");
            assertThat(context).getBeans(ConsumerFactory.class).containsOnlyKeys("mainKafkaConsumerFactory", "marketKafkaConsumerFactory");
            assertThat(context).getBeans(KafkaAdmin.class).containsOnlyKeys("mainKafkaAdmin", "marketKafkaAdmin");
            assertThat(context).getBeans(ConcurrentKafkaListenerContainerFactory.class)
                    .containsOnlyKeys("mainKafkaListenerContainerFactory", "marketKafkaListenerContainerFactory");
            assertThat(context).doesNotHaveBean("kafkaTemplate").doesNotHaveBean("kafkaListenerContainerFactory");
            assertThatThrownBy(() -> context.getBean(KafkaTemplate.class)).isInstanceOf(NoUniqueBeanDefinitionException.class);
        });
    }

    @Test
    void applicationBeanUnderADefaultNameIsKept() {
        KafkaTemplate<Object, Object> mine = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of()));
        runner.withPropertyValues("messaging.kafka.clusters.main.bootstrap-servers=main-1:9092")
                .withBean("kafkaTemplate", KafkaTemplate.class, () -> mine)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("kafkaTemplate")).isSameAs(mine);
                    assertThat(context.getBean("mainKafkaTemplate")).isNotSameAs(mine);
                });
    }

    @Test
    void newTopicBeansAreRefusedWithSeveralClusters() {
        twoClusters.withBean("orders", NewTopic.class, () -> new NewTopic("orders", 1, (short) 1))
                .run(context -> assertThat(context).getFailure()
                        .hasStackTraceContaining("NewTopic beans [orders] cannot be used with several Kafka clusters")
                        .hasStackTraceContaining("[mainKafkaAdmin, marketKafkaAdmin]"));
    }

    @Test
    void onlyASingleClusterCreatesNewTopicBeans() {
        runner.withPropertyValues("messaging.kafka.clusters.main.bootstrap-servers=main-1:9092")
                .run(context -> assertThat(ReflectionTestUtils.getField(context.getBean("mainKafkaAdmin"), "autoCreate")).isEqualTo(true));
        twoClusters.run(context -> {
            assertThat(ReflectionTestUtils.getField(context.getBean("mainKafkaAdmin"), "autoCreate")).isEqualTo(false);
            assertThat(ReflectionTestUtils.getField(context.getBean("marketKafkaAdmin"), "autoCreate")).isEqualTo(false);
        });
    }

    @Test
    void eachClusterUsesItsOwnSettings() {
        twoClusters.withPropertyValues(
                        "messaging.kafka.clusters.market.producer.acks=all",
                        "messaging.kafka.clusters.market.producer.linger=20ms",
                        "messaging.kafka.clusters.market.producer.transaction-id-prefix=tx-",
                        "messaging.kafka.clusters.market.consumer.max-poll-interval=40s",
                        "messaging.kafka.clusters.market.consumer.properties[session.timeout.ms]=30000",
                        "messaging.kafka.clusters.market.template.default-topic=ticks")
                .run(context -> {
                    DefaultKafkaProducerFactory<?, ?> mainProducer = context.getBean("mainKafkaProducerFactory", DefaultKafkaProducerFactory.class);
                    DefaultKafkaProducerFactory<?, ?> marketProducer = context.getBean("marketKafkaProducerFactory", DefaultKafkaProducerFactory.class);
                    assertThat(mainProducer.getConfigurationProperties())
                            .containsEntry("bootstrap.servers", "main-1:9092")
                            .doesNotContainKey("linger.ms");
                    assertThat(marketProducer.getConfigurationProperties())
                            .containsEntry("bootstrap.servers", "market-1:9092")
                            .containsEntry("acks", "all")
                            .containsEntry("linger.ms", 20);
                    assertThat(marketProducer.getTransactionIdPrefix()).isEqualTo("tx-");
                    assertThat(mainProducer.getTransactionIdPrefix()).isNull();

                    DefaultKafkaConsumerFactory<?, ?> marketConsumer = context.getBean("marketKafkaConsumerFactory", DefaultKafkaConsumerFactory.class);
                    assertThat(marketConsumer.getConfigurationProperties())
                            .containsEntry("group.id", "market-group")
                            .containsEntry("max.poll.interval.ms", 40000)
                            .containsEntry("session.timeout.ms", "30000");
                    assertThat(context.getBean("marketKafkaTemplate", KafkaTemplate.class).getDefaultTopic()).isEqualTo("ticks");
                    assertThat(context.getBean("marketKafkaAdmin", KafkaAdmin.class).getConfigurationProperties())
                            .containsEntry("bootstrap.servers", "market-1:9092");

                    // Transactions reach the listener container without a transaction manager bean
                    assertThat(context.getBean("marketKafkaListenerContainerFactory", ConcurrentKafkaListenerContainerFactory.class)
                            .getContainerProperties().getKafkaAwareTransactionManager()).isNotNull();
                    assertThat(context.getBean("mainKafkaListenerContainerFactory", ConcurrentKafkaListenerContainerFactory.class)
                            .getContainerProperties().getKafkaAwareTransactionManager()).isNull();
                });
    }

    @Test
    void listenerPropertiesReachTheContainer() {
        twoClusters.withPropertyValues(
                        "messaging.kafka.clusters.market.listener.type=batch",
                        "messaging.kafka.clusters.market.listener.ack-mode=manual",
                        "messaging.kafka.clusters.market.listener.concurrency=3",
                        "messaging.kafka.clusters.market.listener.auth-exception-retry-interval=5s")
                .run(context -> {
                    ConcurrentKafkaListenerContainerFactory<?, ?> market = context.getBean(
                            "marketKafkaListenerContainerFactory", ConcurrentKafkaListenerContainerFactory.class);
                    ContainerProperties container = market.getContainerProperties();
                    assertThat(market.isBatchListener()).isTrue();
                    assertThat(container.getAckMode()).isEqualTo(ContainerProperties.AckMode.MANUAL);
                    assertThat(ReflectionTestUtils.getField(market, "concurrency")).isEqualTo(3);
                    assertThat(container.getAuthExceptionRetryInterval()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(context.getBean("mainKafkaListenerContainerFactory",
                            ConcurrentKafkaListenerContainerFactory.class).isBatchListener()).isNotEqualTo(Boolean.TRUE);
                });
    }

    @Test
    void applicationBeansApplyToEveryCluster() {
        List<Object> customized = new CopyOnWriteArrayList<>();
        CommonErrorHandler errorHandler = new DefaultErrorHandler();
        RecordInterceptor<Object, Object> interceptor = (record, consumer) -> record;
        twoClusters.withBean(DefaultKafkaProducerFactoryCustomizer.class, () -> customized::add)
                .withBean(CommonErrorHandler.class, () -> errorHandler)
                .withBean(RecordInterceptor.class, () -> interceptor)
                .run(context -> {
                    assertThat(customized).containsExactlyInAnyOrder(
                            context.getBean("mainKafkaProducerFactory"), context.getBean("marketKafkaProducerFactory"));
                    for (String cluster : List.of("main", "market")) {
                        Object factory = context.getBean(cluster + "KafkaListenerContainerFactory");
                        assertThat(ReflectionTestUtils.getField(factory, "commonErrorHandler")).as(cluster).isSameAs(errorHandler);
                        assertThat(ReflectionTestUtils.getField(factory, "recordInterceptor")).as(cluster).isSameAs(interceptor);
                    }
                });
    }

    @Test
    void listenersRunOnVirtualThreadsWhenEnabled() {
        twoClusters.withPropertyValues("spring.threads.virtual.enabled=true")
                .run(context -> {
                    for (String cluster : List.of("main", "market")) {
                        var executor = context.getBean(cluster + "KafkaListenerContainerFactory", ConcurrentKafkaListenerContainerFactory.class)
                                .getContainerProperties().getListenerTaskExecutor();
                        assertThat(executor).as(cluster).isInstanceOf(SimpleAsyncTaskExecutor.class);
                        assertThat(((SimpleAsyncTaskExecutor) executor).getThreadNamePrefix()).isEqualTo(cluster + "Kafka-");
                    }
                });
        twoClusters.run(context -> assertThat(context.getBean("marketKafkaListenerContainerFactory",
                ConcurrentKafkaListenerContainerFactory.class).getContainerProperties().getListenerTaskExecutor()).isNull());
    }
}
