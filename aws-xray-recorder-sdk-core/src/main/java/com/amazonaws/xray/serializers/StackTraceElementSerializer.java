/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.xray.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class StackTraceElementSerializer extends JsonSerializer<StackTraceElement> {

    public StackTraceElementSerializer() {
        super();
    }

    @Override
    public void serialize(
        StackTraceElement element, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        String filename = element.getFileName();
        if (filename != null) {
            jsonGenerator.writeStringField("path", filename);
        } else {
            jsonGenerator.writeNullField("path");
        }
        jsonGenerator.writeNumberField("line", element.getLineNumber());
        jsonGenerator.writeStringField("label", element.getClassName() + "." + element.getMethodName());
        jsonGenerator.writeEndObject();
    }

}
