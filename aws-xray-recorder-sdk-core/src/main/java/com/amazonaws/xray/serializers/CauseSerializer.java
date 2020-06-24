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

import com.amazonaws.xray.entities.Cause;
import com.amazonaws.xray.entities.ThrowableDescription;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CauseSerializer extends JsonSerializer<Cause> {

    private final JsonSerializer<Object> objectSerializer;

    /**
     * @deprecated Use {@link #CauseSerializer(JsonSerializer)}.
     */
    @Deprecated
    // This constructor that is breaking our nullness requirements shouldn't be used and will be deleted.
    @SuppressWarnings("nullness")
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
            String causeDescription = first.getCause();
            if (first.getId() == null && causeDescription != null) {
                jsonGenerator.writeString(causeDescription);
                return;
            }
        } 
        objectSerializer.serialize(cause, jsonGenerator, serializerProvider);
    }

    @Override
    public boolean isEmpty(SerializerProvider serializerProvider, @Nullable Cause cause) {
        return cause == null || (cause.getExceptions().isEmpty() && cause.getId() == null && cause.getMessage() == null);
    }

}
