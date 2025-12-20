package com.hit.cache.config.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class RedisSerializerImpl implements RedisSerializer {

    private final ObjectMapper objectMapper;

    @Override
    public <R> String serializeToJson(R value) {
        if (value instanceof String string) {
            return string;
        } else {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception ex) {
                throw new SerializationException("Could not write JSON: %s".formatted(ex.getMessage()), ex);
            }
        }
    }

    @Override
    public <R> byte[] serializeToRawJson(R value) {
        if (value instanceof String string) {
            return string.getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                return objectMapper.writeValueAsBytes(value);
            } catch (Exception ex) {
                throw new SerializationException("Could not write JSON: %s".formatted(ex.getMessage()), ex);
            }
        }
    }

    @Override
    public <R> R deserialize(String value, Class<R> type) {
        if (value == null) {
            return null;
        }

        if (String.class.equals(type)) {
            return type.cast(value);
        }

        try {
            return objectMapper.readValue(value, type);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON:%s ".formatted(ex.getMessage()), ex);
        }
    }

    @Override
    public <R> R deserializeRaw(byte[] value, Class<R> type) {
        if (value == null) {
            return null;
        }

        if (String.class.equals(type)) {
            return type.cast(new String(value, StandardCharsets.UTF_8));
        }

        try {
            return objectMapper.readValue(value, type);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON:%s ".formatted(ex.getMessage()), ex);
        }
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        return this.serializeToRawJson(value);
    }

    @Override
    public Object deserialize(byte[] value) throws SerializationException {
        return this.deserializeRaw(value, Object.class);
    }
}
