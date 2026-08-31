package com.hit.spring.core.json.deserializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
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
        try (JsonParser parser = new JsonFactory().createParser(json)) {
            parser.nextToken();
            return deserializer.deserialize(parser, null);
        }
    }
}
