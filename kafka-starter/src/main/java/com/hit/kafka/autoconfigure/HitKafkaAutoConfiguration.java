package com.hit.kafka.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka on the clusters of messaging.kafka.clusters.&lt;name&gt;.*, each configured with the keys of spring.kafka.*
 * plus typed client settings, and getting &lt;name&gt;KafkaTemplate, &lt;name&gt;KafkaListenerContainerFactory... When
 * clusters are set, {@link KafkaClustersImportFilter} leaves out Spring Boot's Kafka auto-configuration; without
 * them Spring Boot configures Kafka from spring.kafka.* as usual.
 */
@EnableKafka
@AutoConfiguration(before = KafkaAutoConfiguration.class)
@EnableConfigurationProperties(HitKafkaProperties.class)
@Import(KafkaClustersRegistrar.class)
public class HitKafkaAutoConfiguration {
}
