package com.hit.kafka.config;

import com.hit.kafka.config.properties.KafkaAdminProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = {"messaging.kafka.admin.enable"},
        havingValue = "true"
)
public class KafkaAdminClientConfig {

    private final KafkaAdminProperties kafkaAdminProperties;

    @Bean
    @ConditionalOnMissingBean
    public AdminClient adminClient(ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaAdminProperties.getBootstrapServers());
        config.putAll(kafkaAdminProperties.buildProperties(sslBundles.getIfAvailable()));
        return AdminClient.create(config);
    }
}
