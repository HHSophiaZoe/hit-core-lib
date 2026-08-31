package com.hit.spring.core.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;

public class EpochSecondToInstantDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        long epochSeconds;
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            epochSeconds = Long.parseLong(parser.getText());
        } else {
            epochSeconds = parser.getLongValue();
        }
        return Instant.ofEpochSecond(epochSeconds);
    }
}
