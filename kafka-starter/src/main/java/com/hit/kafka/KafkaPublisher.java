package com.hit.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hit.kafka.extension.ListenableFutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@ConditionalOnProperty(
        value = {"messaging.kafka.enable"},
        havingValue = "true"
)
public abstract class KafkaPublisher {

    protected final KafkaTemplate<String, String> kafkaTemplate;

    protected ObjectMapper objectMapper;

    protected KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    public <T> void pushAsync(String topic, T message) {
        this.sendMessageAsync(topic, null, message, new HashMap<>(), null);
    }

    public <T> void pushAsync(String topic, String key, T message, Map<String, String> headers) {
        this.sendMessageAsync(topic, key, message, headers,null);
    }

    public <T> void pushAsync(String topic, T message, ListenableFutureCallback<String> callback) {
        this.sendMessageAsync(topic, null, message, new HashMap<>(), callback);
    }

    public <T> void pushAsync(String topic, T message, Map<String, String> headers) {
        this.sendMessageAsync(topic, null, message, headers, null);
    }

    public <T> boolean pushSync(String topic, T message) {
        return this.sendMessageSync(topic, null, message, new HashMap<>());
    }

    public <T> boolean pushSync(String topic, T message, Map<String, String> headers) {
        return this.sendMessageSync(topic, null, message, headers);
    }

    public <T> boolean pushSync(String topic, String key, T message, Map<String, String> headers) {
        return this.sendMessageSync(topic, key, message, headers);
    }

    protected <T> void sendMessageAsync(final String topic, String key, T data, Map<String, String> headers, final ListenableFutureCallback<String> callback) {
        final String message;
        try {
            message = this.objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException var6) {
            log.error("Exception when parse data to json {}", var6.getMessage());
            return;
        }

        ProducerRecord<String, String> rc = new ProducerRecord<>(topic, key, message);
        this.addHeader(headers, rc);
        CompletableFuture<SendResult<String, String>> future = this.kafkaTemplate.send(rc);
        if (callback != null) {
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.debug("xxxx> Unable to send message=[ {} ] to topic: {} FAIL !!! Reason: {}", message, topic, ex.getMessage(), ex);
                    callback.onFailure(ex);
                } else {
                    log.debug("===> Sent message=[ {} ] with offset=[ {} ] to topic: {} SUCCESS !!!", message, result.getRecordMetadata().offset(), topic);
                    callback.onSuccess(message);
                }
            });
        }
    }

    protected <T> boolean sendMessageSync(final String topic, String key, T data, Map<String, String> headers) {
        final String message;
        try {
            message = this.objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException var10) {
            log.error("Exception when parse data to json {}", var10.getMessage());
            return false;
        }

        ProducerRecord<String, String> rc = new ProducerRecord<>(topic, key, message);
        this.addHeader(headers, rc);
        CompletableFuture<SendResult<String, String>> future = this.kafkaTemplate.send(rc);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("===> Sent message=[ {} ] with offset=[ {} ] to topic: {} SUCCESS !!!", message, result.getRecordMetadata().offset(), topic);
            } else {
                log.debug("xxxx> Unable to send message=[ {} ] to topic: {} FAIL !!! Reason: {}", message, topic, ex.getMessage(), ex);
            }
        });

        try {
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException var9) {
            log.error("Send message sync exception: {}", var9.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void addHeader(Map<String, String> headers, ProducerRecord<String, String> rc) {
        headers.forEach((key, value) -> rc.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8))));
    }
}
