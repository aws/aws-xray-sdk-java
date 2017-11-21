package com.amazonaws.xray.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class StackTraceElementSerializer extends JsonSerializer<StackTraceElement> {

    public StackTraceElementSerializer() {
        super();
    }

    @Override
    public void serialize(StackTraceElement element, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("path", element.getFileName());
        jsonGenerator.writeNumberField("line", element.getLineNumber());
        jsonGenerator.writeStringField("label", element.getClassName() + "." + element.getMethodName());
        jsonGenerator.writeEndObject();
    }

}
