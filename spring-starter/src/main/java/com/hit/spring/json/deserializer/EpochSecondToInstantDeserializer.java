package com.hit.spring.json.deserializer;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Instant;

public class EpochSecondToInstantDeserializer extends ValueDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) {
        long epochSeconds;
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            epochSeconds = Long.parseLong(parser.getString());
        } else {
            epochSeconds = parser.getLongValue();
        }
        return Instant.ofEpochSecond(epochSeconds);
    }
}
