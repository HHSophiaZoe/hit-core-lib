package com.hit.kafka.config;

import com.hit.kafka.config.properties.KafkaProducerProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Getter
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = {"messaging.kafka.enable"},
        havingValue = "true"
)
public class KafkaProducerConfig {

    private final KafkaProducerProperties kafkaProducerProperties;

    @Bean("kafkaEventProducerFactory")
    public ProducerFactory<String, String> producerFactory(ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> properties = new HashMap<>();
        // put default props
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, kafkaProducerProperties.getDeliveryTimeout());
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, kafkaProducerProperties.getRequestTimeout());
        if (kafkaProducerProperties.getEnableIdempotence() != null) {
            properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, kafkaProducerProperties.getEnableIdempotence());
        }
        // put config props
        properties.putAll(kafkaProducerProperties.buildProperties(sslBundles.getIfAvailable()));
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean("defaultKafkaTemplate")
    @Primary
    public KafkaTemplate<String, String> kafkaTemplate(ObjectProvider<SslBundles> sslBundles) {
        return new KafkaTemplate<>(this.producerFactory(sslBundles));
    }
}

