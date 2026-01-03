package com.hit.spring.core.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.hit.common.util.TimeUtils;

import java.io.IOException;
import java.time.LocalDateTime;

public class EpochSecondToLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        long epochSeconds;
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            epochSeconds = Long.parseLong(p.getText());
        } else {
            epochSeconds = p.getLongValue();
        }

        return TimeUtils.toLocalDateTime(epochSeconds);
    }
}