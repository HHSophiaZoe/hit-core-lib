package com.hit.spring.json.deserializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpochSecondToInstantDeserializerTest {

    private final EpochSecondToInstantDeserializer deserializer = new EpochSecondToInstantDeserializer();

    @Test
    void shouldDeserializeNumericEpochSecond() throws Exception {
        assertEquals(Instant.ofEpochSecond(1_725_000_000L), deserialize("1725000000"));
    }

    @Test
    void shouldDeserializeStringEpochSecond() throws Exception {
        assertEquals(Instant.ofEpochSecond(1_725_000_000L), deserialize("\"1725000000\""));
    }

    private Instant deserialize(String json) throws Exception {
        try (JsonParser parser = JsonMapper.builder().build().createParser(json)) {
            parser.nextToken();
            return deserializer.deserialize(parser, null);
        }
    }
}
