package com.hit.kafka.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What a full Spring Boot application does with two clusters, before any broker is reached: a missing qualifier or
 * containerFactory stops the startup.
 */
class KafkaClustersStartupTest {

    private static final String[] TWO_CLUSTERS = {
            "messaging.kafka.clusters.main.bootstrap-servers=localhost:1",
            "messaging.kafka.clusters.market.bootstrap-servers=localhost:2",
            "messaging.kafka.clusters.main.listener.auto-startup=false",
            "messaging.kafka.clusters.market.listener.auto-startup=false"};

    @Test
    void startsWhenEverythingNamesItsCluster() {
        try (ConfigurableApplicationContext context = run(Explicit.class)) {
            assertThat(context.getBean(Explicit.class)).isNotNull();
        }
    }

    @Test
    void templateWithoutQualifierStopsTheStartup() {
        assertThatThrownBy(() -> run(TemplateWithoutQualifier.class))
                .hasStackTraceContaining("expected single matching bean but found 2")
                .hasStackTraceContaining("mainKafkaTemplate,marketKafkaTemplate");
    }

    @Test
    void listenerWithoutContainerFactoryStopsTheStartup() {
        assertThatThrownBy(() -> run(ListenerWithoutFactory.class))
                .hasStackTraceContaining("kafkaListenerContainerFactory");
    }

    @Test
    void springBootKafkaWouldFailWithTwoClusters() {
        // Why KafkaClustersImportFilter leaves it out: its default listener factory wants a single ConsumerFactory
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HitKafkaAutoConfiguration.class, KafkaAutoConfiguration.class))
                .withPropertyValues(TWO_CLUSTERS)
                .run(context -> assertThat(context).getFailure()
                        .hasStackTraceContaining("kafkaListenerContainerFactory")
                        .hasStackTraceContaining("expected single matching bean but found 2"));
    }

    private static ConfigurableApplicationContext run(Class<?> source) {
        return new SpringApplicationBuilder(source).web(WebApplicationType.NONE).properties(TWO_CLUSTERS).run();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Explicit {

        @Bean
        Object user(@Qualifier("marketKafkaTemplate") KafkaTemplate<String, String> template) {
            return template;
        }

        @KafkaListener(topics = "t", containerFactory = "mainKafkaListenerContainerFactory")
        void listen(String message) {
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TemplateWithoutQualifier {

        @Bean
        Object user(KafkaTemplate<String, String> template) {
            return template;
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class ListenerWithoutFactory {

        @KafkaListener(topics = "t")
        void listen(String message) {
        }
    }
}
