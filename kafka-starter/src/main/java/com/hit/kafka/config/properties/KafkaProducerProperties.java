package com.hit.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Primary
@Configuration
@SuppressWarnings({"java:S1185"})
@ConfigurationProperties(prefix = "messaging.kafka.producer")
public class KafkaProducerProperties extends KafkaProperties.Producer {

    private int deliveryTimeout = 20000;

    private int requestTimeout = 10000;

    public KafkaProducerProperties() {
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
    public String getAcks() {
        return super.getAcks();
    }

    @Override
    public void setAcks(String acks) {
        super.setAcks(acks);
    }

    @Override
    public DataSize getBatchSize() {
        return super.getBatchSize();
    }

    @Override
    public void setBatchSize(DataSize batchSize) {
        super.setBatchSize(batchSize);
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
    public DataSize getBufferMemory() {
        return super.getBufferMemory();
    }

    @Override
    public void setBufferMemory(DataSize bufferMemory) {
        super.setBufferMemory(bufferMemory);
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
    public String getCompressionType() {
        return super.getCompressionType();
    }

    @Override
    public void setCompressionType(String compressionType) {
        super.setCompressionType(compressionType);
    }

    @Override
    public Class<?> getKeySerializer() {
        return super.getKeySerializer();
    }

    @Override
    public void setKeySerializer(Class<?> keySerializer) {
        super.setKeySerializer(keySerializer);
    }

    @Override
    public Class<?> getValueSerializer() {
        return super.getValueSerializer();
    }

    @Override
    public void setValueSerializer(Class<?> valueSerializer) {
        super.setValueSerializer(valueSerializer);
    }

    @Override
    public Integer getRetries() {
        return super.getRetries();
    }

    @Override
    public void setRetries(Integer retries) {
        super.setRetries(retries);
    }

    @Override
    public String getTransactionIdPrefix() {
        return super.getTransactionIdPrefix();
    }

    @Override
    public void setTransactionIdPrefix(String transactionIdPrefix) {
        super.setTransactionIdPrefix(transactionIdPrefix);
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
