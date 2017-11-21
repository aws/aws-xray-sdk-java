package com.amazonaws.xray.serializers;

import java.io.IOException;

import com.amazonaws.xray.entities.Cause;
import com.amazonaws.xray.entities.ThrowableDescription;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class CauseSerializer extends JsonSerializer<Cause> {

    private JsonSerializer<Object> objectSerializer;

    public CauseSerializer() {
        this(null);
    }

    public CauseSerializer(JsonSerializer<Object> objectSerializer) {
        this.objectSerializer = objectSerializer;
    }

    @Override
    public void serialize(Cause cause, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (!cause.getExceptions().isEmpty()) {
            ThrowableDescription first = cause.getExceptions().get(0);
            if (null == first.getId() && null != first.getCause()) {
                jsonGenerator.writeString(first.getCause());
                return;
            }
        } 
        objectSerializer.serialize(cause, jsonGenerator, serializerProvider);
    }

    @Override
    public boolean isEmpty(SerializerProvider serializerProvider, Cause cause) {
        return null == cause || (cause.getExceptions().isEmpty() && null == cause.getId() && null == cause.getMessage());
    }

}
