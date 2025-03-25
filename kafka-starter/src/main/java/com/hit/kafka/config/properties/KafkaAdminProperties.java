package com.hit.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Primary
@Configuration
@SuppressWarnings({"java:S1185"})
@ConfigurationProperties(prefix = "messaging.kafka.admin")
public class KafkaAdminProperties extends KafkaProperties.Admin {

    private Boolean enable = false;

    private List<String> bootstrapServers;

    public KafkaAdminProperties() {
        super();
    }

    @Override
    public KafkaProperties.Ssl getSsl() {
        return super.getSsl();
    }

    @Override
    public KafkaProperties.Security getSecurity() {
        return super.getSecurity();
    }

    @Override
    public String getClientId() {
        return super.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        super.setClientId(clientId);
    }

    @Override
    public Duration getCloseTimeout() {
        return super.getCloseTimeout();
    }

    @Override
    public void setCloseTimeout(Duration closeTimeout) {
        super.setCloseTimeout(closeTimeout);
    }

    @Override
    public Duration getOperationTimeout() {
        return super.getOperationTimeout();
    }

    @Override
    public void setOperationTimeout(Duration operationTimeout) {
        super.setOperationTimeout(operationTimeout);
    }

    @Override
    public boolean isFailFast() {
        return super.isFailFast();
    }

    @Override
    public void setFailFast(boolean failFast) {
        super.setFailFast(failFast);
    }

    @Override
    public boolean isModifyTopicConfigs() {
        return super.isModifyTopicConfigs();
    }

    @Override
    public void setModifyTopicConfigs(boolean modifyTopicConfigs) {
        super.setModifyTopicConfigs(modifyTopicConfigs);
    }

    @Override
    public boolean isAutoCreate() {
        return super.isAutoCreate();
    }

    @Override
    public void setAutoCreate(boolean autoCreate) {
        super.setAutoCreate(autoCreate);
    }

    @Override
    public Map<String, String> getProperties() {
        return super.getProperties();
    }

    @Override
    public Map<String, Object> buildProperties() {
        return super.buildProperties();
    }
}
