package com.hit.kafka.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Leaves out Spring Boot's Kafka auto-configuration when messaging.kafka.clusters is set. Otherwise Boot would add
 * beans of its own on spring.kafka.* (localhost:9092), among them a kafkaListenerContainerFactory that fails as soon
 * as there are two clusters, since it looks up the one ConsumerFactory by type.
 */
public class KafkaClustersImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    static final String SPRING_BOOT_KAFKA = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration";

    private static final ConfigurationPropertyName CLUSTERS = ConfigurationPropertyName.of(HitKafkaProperties.CLUSTERS);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean clustersSet = hasClusters(environment);
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matches[i] = !(clustersSet && SPRING_BOOT_KAFKA.equals(autoConfigurationClasses[i]));
        }
        return matches;
    }

    static boolean hasClusters(Environment environment) {
        for (ConfigurationPropertySource source : ConfigurationPropertySources.get(environment)) {
            if (source.containsDescendantOf(CLUSTERS) == ConfigurationPropertyState.PRESENT) {
                return true;
            }
        }
        return false;
    }
}
