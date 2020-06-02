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

package com.amazonaws.xray.strategy;

import java.lang.reflect.InvocationTargetException;

public class RuntimeErrorContextMissingStrategy implements ContextMissingStrategy {

    public static final String OVERRIDE_VALUE = "RUNTIME_ERROR";

    /**
     * Constructs an instance of {@code exceptionClass} and throws it.
     * @param message the message to use when constructing an instance of {@code exceptionClass}
     * @param exceptionClass the type of exception thrown due to missing context
     */
    @Override
    public void contextMissing(String message, Class<? extends RuntimeException> exceptionClass) {
        try {
            throw exceptionClass.getConstructor(String.class).newInstance(message);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(message);
        }
    }

}
