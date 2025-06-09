package com.hit.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Primary
@Configuration
@ConfigurationProperties(prefix = "messaging.kafka")
public class KafkaProperties {

    private Boolean enable;

    public static final class Properties extends HashMap<String, Object> {
        <V> java.util.function.Consumer<V> in(String key) {
            return (value) -> this.put(key, value);
        }

        Properties with(org.springframework.boot.autoconfigure.kafka.KafkaProperties.Ssl ssl,
                        org.springframework.boot.autoconfigure.kafka.KafkaProperties.Security security,
                        Map<String, String> properties, SslBundles sslBundles) {
            this.putAll(ssl.buildProperties(sslBundles));
            this.putAll(security.buildProperties());
            this.putAll(properties);
            return this;
        }
    }
}
