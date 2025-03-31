package com.hit.kafka.config.properties.batch;

import com.hit.kafka.config.properties.KafkaConsumerProperties;
import com.hit.kafka.config.properties.KafkaListenerProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Primary
@Configuration
@SuppressWarnings({"java:S1185"})
@ConfigurationProperties(prefix = "messaging.kafka.batch")
public class KafkaBatchProperties {

    private Boolean enable;

    private Consumer consumer;

    private Listener listener;

    @Setter
    @Getter
    public static class Consumer extends KafkaConsumerProperties {

    }

    @Getter
    @Setter
    public static class Listener extends KafkaListenerProperties {

    }

}
