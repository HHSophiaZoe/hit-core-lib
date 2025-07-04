package com.hit.kafka.config;

import com.hit.kafka.config.properties.KafkaConsumerProperties;
import com.hit.kafka.config.properties.batch.KafkaBatchProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = {"messaging.kafka.enable"},
        havingValue = "true"
)
public class KafkaConsumerConfig {

    private final KafkaConsumerProperties kafkaConsumerProperties;

    private final KafkaBatchProperties kafkaBatchProperties;

    @Bean
    public ConsumerFactory<String, Object> defaultConsumerFactory(ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> properties = new HashMap<>();
        // put default props
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "default");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, kafkaConsumerProperties.getMaxPollIntervalMs());
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, kafkaConsumerProperties.getMaxPollIntervalMs());
        // put config props
        properties.putAll(kafkaConsumerProperties.buildProperties(sslBundles.getIfAvailable()));
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    @ConditionalOnProperty(value = {"messaging.kafka.batch.enable"}, havingValue = "true")
    public ConsumerFactory<String, Object> consumerBatchFactory(ObjectProvider<SslBundles> sslBundles) {
        KafkaBatchProperties.Consumer consumerProperties = kafkaBatchProperties.getConsumer();
        Map<String, Object> properties = new HashMap<>();
        // put default props
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-batch-default");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumerProperties.getMaxPollIntervalMs());
        // put config props
        properties.putAll(consumerProperties.buildProperties(sslBundles.getIfAvailable()));
        return new DefaultKafkaConsumerFactory<>(properties);
    }

}
