package com.hit.cache.config.serializer;

public interface RedisSerializer extends org.springframework.data.redis.serializer.RedisSerializer<Object> {

    <R> String serializeToJson(R value);

    <R> byte[] serializeToRawJson(R value);

    <R> R deserialize(String value, Class<R> type);

    <R> R deserializeRaw(byte[] raw, Class<R> type);

}
