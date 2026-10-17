package com.hit.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Base publisher: serializes the message to JSON and sends it as a String record with String headers.
 */
@Slf4j
public abstract class KafkaPublisher {

    protected final KafkaTemplate<String, String> kafkaTemplate;

    protected final ObjectMapper objectMapper;

    /**
     * Uses a JSON mapper of its own: ISO dates for {@code java.time}, {@code yyyy-MM-dd HH:mm:ss} for {@code Date}.
     */
    protected KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this(kafkaTemplate, JsonMapper.builder()
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .build());
    }

    protected KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> CompletableFuture<SendResult<String, String>> pushAsync(String topic, T message) {
        return this.sendMessageAsync(topic, null, message, Map.of());
    }

    public <T> CompletableFuture<SendResult<String, String>> pushAsync(String topic, T message, Map<String, String> headers) {
        return this.sendMessageAsync(topic, null, message, headers);
    }

    public <T> CompletableFuture<SendResult<String, String>> pushAsync(String topic, String key, T message, Map<String, String> headers) {
        return this.sendMessageAsync(topic, key, message, headers);
    }

    public <T> boolean pushSync(String topic, T message) {
        return this.sendMessageSync(topic, null, message, Map.of());
    }

    public <T> boolean pushSync(String topic, T message, Map<String, String> headers) {
        return this.sendMessageSync(topic, null, message, headers);
    }

    public <T> boolean pushSync(String topic, String key, T message, Map<String, String> headers) {
        return this.sendMessageSync(topic, key, message, headers);
    }

    /**
     * Sends without waiting; the returned future completes with the broker result. A message that cannot be
     * serialized gives a failed future.
     */
    protected <T> CompletableFuture<SendResult<String, String>> sendMessageAsync(String topic, String key, T data,
                                                                                 Map<String, String> headers) {
        final String message;
        try {
            message = this.objectMapper.writeValueAsString(data);
        } catch (JacksonException e) {
            log.error("Cannot serialize message for topic {}: {}", topic, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
        return this.kafkaTemplate.send(this.createRecord(topic, key, message, headers))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Send to topic {} failed: {}", topic, ex.getMessage(), ex);
                    } else {
                        log.debug("Sent to topic {} partition {} offset {}: {}", topic,
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), message);
                    }
                });
    }

    /**
     * Sends and waits for the broker acknowledgement; false when serialization or sending fails.
     */
    protected <T> boolean sendMessageSync(String topic, String key, T data, Map<String, String> headers) {
        try {
            this.sendMessageAsync(topic, key, data, headers).get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while sending message to topic {}", topic);
            return false;
        } catch (ExecutionException e) {
            // Already logged by sendMessageAsync
            return false;
        }
    }

    private ProducerRecord<String, String> createRecord(String topic, String key, String message, Map<String, String> headers) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
        headers.forEach((name, value) -> record.headers().add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8))));
        return record;
    }
}
