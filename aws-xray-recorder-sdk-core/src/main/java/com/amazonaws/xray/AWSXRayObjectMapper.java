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

package com.amazonaws.xray;

import com.amazonaws.xray.entities.Cause;
import com.amazonaws.xray.serializers.CauseSerializer;
import com.amazonaws.xray.serializers.StackTraceElementSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.awt.event.FocusEvent;

public class AWSXRayObjectMapper extends ObjectMapper {

    private static AWSXRayObjectMapper instance = new AWSXRayObjectMapper();

    public static AWSXRayObjectMapper getInstance() {
        return instance;
    }

    private AWSXRayObjectMapper() {
        findAndRegisterModules();
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        registerModule(new SimpleModule() {
            private static final long serialVersionUID = 545800949242254918L;

            @Override
            public void setupModule(SetupContext setupContext) {
                super.setupModule(setupContext);
                setupContext.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public JsonSerializer<?> modifySerializer(
                            SerializationConfig serializationConfig,
                            BeanDescription beanDescription,
                            JsonSerializer<?> jsonSerializer) {
                        Class<?> beanClass = beanDescription.getBeanClass();
                        if (FocusEvent.Cause.class.isAssignableFrom(beanClass)) {
                            return new CauseSerializer((JsonSerializer<Object>) jsonSerializer);
                        } else if (StackTraceElement.class.isAssignableFrom(beanClass)) {
                            return new StackTraceElementSerializer();
                        }
                        return jsonSerializer;
                    }
                });
            }
        });
    }
}
