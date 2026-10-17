package com.hit.kafka.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * messaging.kafka.*: the Kafka clusters of the application.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = HitKafkaProperties.PREFIX)
public class HitKafkaProperties {

    public static final String PREFIX = "messaging.kafka";

    public static final String CLUSTERS = PREFIX + ".clusters";

    /**
     * Kafka clusters by name, each with the keys of spring.kafka.* plus typed client settings. The name prefixes the
     * beans of the cluster, e.g. mainKafkaTemplate and mainKafkaListenerContainerFactory, so write it in camelCase.
     * With one cluster its beans are also the default ones; with several, every KafkaTemplate injection needs a
     * qualifier and every KafkaListener a containerFactory. Without clusters, Spring Boot configures Kafka from
     * spring.kafka.*.
     */
    private Map<String, KafkaClusterProperties> clusters = new LinkedHashMap<>();
}
