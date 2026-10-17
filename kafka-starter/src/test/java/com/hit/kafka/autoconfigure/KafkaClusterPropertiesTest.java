package com.hit.kafka.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaClusterPropertiesTest {

    @Test
    void unsetFieldsAreNotSentSoKafkaDefaultsApply() {
        KafkaClusterProperties properties = bind(Map.of());

        // Only what Spring Boot sets by default: brokers, String (de)serializers, isolation level
        assertThat(properties.buildProducerProperties())
                .containsOnlyKeys("bootstrap.servers", "key.serializer", "value.serializer");
        assertThat(properties.buildConsumerProperties())
                .containsOnlyKeys("bootstrap.servers", "key.deserializer", "value.deserializer", "isolation.level");
    }

    @Test
    void bootstrapServersIsACommaSeparatedString() {
        KafkaClusterProperties properties = bind(Map.of("messaging.kafka.clusters.main.bootstrap-servers", "kafka-1:9092,kafka-2:9092"));

        for (Map<String, Object> config : List.of(properties.buildProducerProperties(),
                properties.buildConsumerProperties(), properties.buildAdminProperties())) {
            assertThat(config).containsEntry("bootstrap.servers", "kafka-1:9092,kafka-2:9092");
        }
        assertThat(new KafkaClusterProperties().buildProducerProperties()).containsEntry("bootstrap.servers", "localhost:9092");
    }

    @Test
    void producerFieldsMapToKafkaKeys() {
        Map<String, Object> config = bind(Map.of(
                "messaging.kafka.clusters.main.producer.delivery-timeout", "20s",
                "messaging.kafka.clusters.main.producer.request-timeout", "10000",
                "messaging.kafka.clusters.main.producer.linger", "15ms",
                "messaging.kafka.clusters.main.producer.max-block", "5s",
                "messaging.kafka.clusters.main.producer.enable-idempotence", "false",
                "messaging.kafka.clusters.main.producer.max-in-flight-requests", "1",
                "messaging.kafka.clusters.main.producer.max-request-size", "2MB")).buildProducerProperties();

        assertThat(config)
                .containsEntry("delivery.timeout.ms", 20000)
                .containsEntry("request.timeout.ms", 10000)
                .containsEntry("linger.ms", 15)
                .containsEntry("max.block.ms", 5000L)
                .containsEntry("enable.idempotence", false)
                .containsEntry("max.in.flight.requests.per.connection", 1)
                .containsEntry("max.request.size", 2 * 1024 * 1024);
    }

    @Test
    void saslAppliesToEveryClient() {
        KafkaClusterProperties properties = bind(Map.of(
                "messaging.kafka.clusters.main.security.protocol", "SASL_SSL",
                "messaging.kafka.clusters.main.sasl.mechanism", "SCRAM-SHA-512",
                "messaging.kafka.clusters.main.sasl.jaas-config", "login required;"));

        for (Map<String, Object> config : List.of(properties.buildProducerProperties(),
                properties.buildConsumerProperties(), properties.buildAdminProperties())) {
            assertThat(config).containsEntry("security.protocol", "SASL_SSL")
                    .containsEntry("sasl.mechanism", "SCRAM-SHA-512")
                    .containsEntry("sasl.jaas.config", "login required;");
        }
    }

    @Test
    void clusterPropertiesLoseToTypedFieldsWhichLoseToClientProperties() {
        Map<String, Object> config = bind(Map.of(
                "messaging.kafka.clusters.main.properties[linger.ms]", "1",
                "messaging.kafka.clusters.main.properties[request.timeout.ms]", "1",
                "messaging.kafka.clusters.main.properties[compression.type]", "lz4",
                "messaging.kafka.clusters.main.producer.linger", "20ms",
                "messaging.kafka.clusters.main.producer.request-timeout", "10s",
                "messaging.kafka.clusters.main.producer.properties[request.timeout.ms]", "7000")).buildProducerProperties();

        assertThat(config)
                .containsEntry("compression.type", "lz4")
                .containsEntry("linger.ms", 20)
                .containsEntry("request.timeout.ms", "7000");
    }

    @Test
    void everyClusterBindsUnderItsName() {
        HitKafkaProperties properties = bindAll(Map.of(
                "messaging.kafka.clusters.market.bootstrap-servers", "market:9092",
                "messaging.kafka.clusters.market.producer.linger", "50ms",
                "messaging.kafka.clusters.market.consumer.group-id", "market"));

        KafkaClusterProperties market = properties.getClusters().get("market");
        assertThat(market.buildProducerProperties()).containsEntry("bootstrap.servers", "market:9092")
                .containsEntry("linger.ms", 50);
        assertThat(market.buildConsumerProperties()).containsEntry("group.id", "market");
    }

    @Test
    void clustersKeyHasADescriptionForTheIde() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/META-INF/spring-configuration-metadata.json")) {
            JsonNode clusters = null;
            for (JsonNode property : JsonMapper.builder().build().readTree(in).get("properties")) {
                if (property.get("name").stringValue().equals("messaging.kafka.clusters")) {
                    clusters = property;
                }
            }
            assertThat(clusters).isNotNull();
            assertThat(clusters.get("type").stringValue())
                    .isEqualTo("java.util.Map<java.lang.String,com.hit.kafka.autoconfigure.KafkaClusterProperties>");
            assertThat(clusters.get("description").stringValue()).contains("qualifier");
        }
    }

    /**
     * The cluster "main", or an empty one when nothing is set.
     */
    private static KafkaClusterProperties bind(Map<String, String> values) {
        return bindAll(values).getClusters().getOrDefault("main", new KafkaClusterProperties());
    }

    private static HitKafkaProperties bindAll(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind(HitKafkaProperties.PREFIX, HitKafkaProperties.class)
                .orElseGet(HitKafkaProperties::new);
    }
}
