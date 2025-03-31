package com.hit.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Primary
@Configuration
@SuppressWarnings({"java:S1185"})
@ConfigurationProperties(
        prefix = "messaging.kafka.consumer"
)
public class KafkaConsumerProperties extends KafkaProperties.Consumer {

    public KafkaConsumerProperties() {
        super();
    }

    private int maxPollIntervalMs = 300000;

    private int fetchMinBytes = 65536;

    private int fetchMaxWaitMs = 500;

    @Override
    public KafkaProperties.Ssl getSsl() {
        return super.getSsl();
    }

    @Override
    public KafkaProperties.Security getSecurity() {
        return super.getSecurity();
    }

    @Override
    public Duration getAutoCommitInterval() {
        return super.getAutoCommitInterval();
    }

    @Override
    public void setAutoCommitInterval(Duration autoCommitInterval) {
        super.setAutoCommitInterval(autoCommitInterval);
    }

    @Override
    public String getAutoOffsetReset() {
        return super.getAutoOffsetReset();
    }

    @Override
    public void setAutoOffsetReset(String autoOffsetReset) {
        super.setAutoOffsetReset(autoOffsetReset);
    }

    @Override
    public List<String> getBootstrapServers() {
        return super.getBootstrapServers();
    }

    @Override
    public void setBootstrapServers(List<String> bootstrapServers) {
        super.setBootstrapServers(bootstrapServers);
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
    public Boolean getEnableAutoCommit() {
        return super.getEnableAutoCommit();
    }

    @Override
    public void setEnableAutoCommit(Boolean enableAutoCommit) {
        super.setEnableAutoCommit(enableAutoCommit);
    }

    @Override
    public Duration getFetchMaxWait() {
        return super.getFetchMaxWait();
    }

    @Override
    public void setFetchMaxWait(Duration fetchMaxWait) {
        super.setFetchMaxWait(fetchMaxWait);
    }

    @Override
    public DataSize getFetchMinSize() {
        return super.getFetchMinSize();
    }

    @Override
    public void setFetchMinSize(DataSize fetchMinSize) {
        super.setFetchMinSize(fetchMinSize);
    }

    @Override
    public String getGroupId() {
        return super.getGroupId();
    }

    @Override
    public void setGroupId(String groupId) {
        super.setGroupId(groupId);
    }

    @Override
    public Duration getHeartbeatInterval() {
        return super.getHeartbeatInterval();
    }

    @Override
    public void setHeartbeatInterval(Duration heartbeatInterval) {
        super.setHeartbeatInterval(heartbeatInterval);
    }

    @Override
    public KafkaProperties.IsolationLevel getIsolationLevel() {
        return super.getIsolationLevel();
    }

    @Override
    public void setIsolationLevel(KafkaProperties.IsolationLevel isolationLevel) {
        super.setIsolationLevel(isolationLevel);
    }

    @Override
    public Class<?> getKeyDeserializer() {
        return super.getKeyDeserializer();
    }

    @Override
    public void setKeyDeserializer(Class<?> keyDeserializer) {
        super.setKeyDeserializer(keyDeserializer);
    }

    @Override
    public Class<?> getValueDeserializer() {
        return super.getValueDeserializer();
    }

    @Override
    public void setValueDeserializer(Class<?> valueDeserializer) {
        super.setValueDeserializer(valueDeserializer);
    }

    @Override
    public Integer getMaxPollRecords() {
        return super.getMaxPollRecords();
    }

    @Override
    public void setMaxPollRecords(Integer maxPollRecords) {
        super.setMaxPollRecords(maxPollRecords);
    }

    @Override
    public Map<String, Object> buildProperties() {
        return super.buildProperties();
    }

}
