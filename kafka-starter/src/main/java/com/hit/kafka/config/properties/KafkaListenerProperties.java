package com.hit.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.listener.ContainerProperties;

import java.time.Duration;

@Getter
@Setter
@Primary
@Configuration
@SuppressWarnings({"java:S1185"})
@ConfigurationProperties(prefix = "messaging.kafka.listener")
public class KafkaListenerProperties extends KafkaProperties.Listener {

    public KafkaListenerProperties() {
        super();
    }

    @Override
    public Type getType() {
        return super.getType();
    }

    @Override
    public void setType(Type type) {
        super.setType(type);
    }

    @Override
    public ContainerProperties.AckMode getAckMode() {
        return super.getAckMode();
    }

    @Override
    public void setAckMode(ContainerProperties.AckMode ackMode) {
        super.setAckMode(ackMode);
    }

    @Override
    public Boolean getAsyncAcks() {
        return super.getAsyncAcks();
    }

    @Override
    public void setAsyncAcks(Boolean asyncAcks) {
        super.setAsyncAcks(asyncAcks);
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
    public Integer getConcurrency() {
        return super.getConcurrency();
    }

    @Override
    public void setConcurrency(Integer concurrency) {
        super.setConcurrency(concurrency);
    }

    @Override
    public Duration getPollTimeout() {
        return super.getPollTimeout();
    }

    @Override
    public void setPollTimeout(Duration pollTimeout) {
        super.setPollTimeout(pollTimeout);
    }

    @Override
    public Float getNoPollThreshold() {
        return super.getNoPollThreshold();
    }

    @Override
    public void setNoPollThreshold(Float noPollThreshold) {
        super.setNoPollThreshold(noPollThreshold);
    }

    @Override
    public Integer getAckCount() {
        return super.getAckCount();
    }

    @Override
    public void setAckCount(Integer ackCount) {
        super.setAckCount(ackCount);
    }

    @Override
    public Duration getAckTime() {
        return super.getAckTime();
    }

    @Override
    public void setAckTime(Duration ackTime) {
        super.setAckTime(ackTime);
    }

    @Override
    public Duration getIdleBetweenPolls() {
        return super.getIdleBetweenPolls();
    }

    @Override
    public void setIdleBetweenPolls(Duration idleBetweenPolls) {
        super.setIdleBetweenPolls(idleBetweenPolls);
    }

    @Override
    public Duration getIdleEventInterval() {
        return super.getIdleEventInterval();
    }

    @Override
    public void setIdleEventInterval(Duration idleEventInterval) {
        super.setIdleEventInterval(idleEventInterval);
    }

    @Override
    public Duration getIdlePartitionEventInterval() {
        return super.getIdlePartitionEventInterval();
    }

    @Override
    public void setIdlePartitionEventInterval(Duration idlePartitionEventInterval) {
        super.setIdlePartitionEventInterval(idlePartitionEventInterval);
    }

    @Override
    public Duration getMonitorInterval() {
        return super.getMonitorInterval();
    }

    @Override
    public void setMonitorInterval(Duration monitorInterval) {
        super.setMonitorInterval(monitorInterval);
    }

    @Override
    public Boolean getLogContainerConfig() {
        return super.getLogContainerConfig();
    }

    @Override
    public void setLogContainerConfig(Boolean logContainerConfig) {
        super.setLogContainerConfig(logContainerConfig);
    }

    @Override
    public boolean isMissingTopicsFatal() {
        return super.isMissingTopicsFatal();
    }

    @Override
    public void setMissingTopicsFatal(boolean missingTopicsFatal) {
        super.setMissingTopicsFatal(missingTopicsFatal);
    }

    @Override
    public boolean isImmediateStop() {
        return super.isImmediateStop();
    }

    @Override
    public void setImmediateStop(boolean immediateStop) {
        super.setImmediateStop(immediateStop);
    }

    @Override
    public boolean isAutoStartup() {
        return super.isAutoStartup();
    }

    @Override
    public void setAutoStartup(boolean autoStartup) {
        super.setAutoStartup(autoStartup);
    }

    @Override
    public Boolean getChangeConsumerThreadName() {
        return super.getChangeConsumerThreadName();
    }

    @Override
    public void setChangeConsumerThreadName(Boolean changeConsumerThreadName) {
        super.setChangeConsumerThreadName(changeConsumerThreadName);
    }

    @Override
    public boolean isObservationEnabled() {
        return super.isObservationEnabled();
    }

    @Override
    public void setObservationEnabled(boolean observationEnabled) {
        super.setObservationEnabled(observationEnabled);
    }
}
