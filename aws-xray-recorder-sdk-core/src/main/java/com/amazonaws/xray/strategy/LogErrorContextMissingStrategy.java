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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LogErrorContextMissingStrategy implements ContextMissingStrategy {

    public static final String OVERRIDE_VALUE = "LOG_ERROR";

    private static final Log logger = LogFactory.getLog(LogErrorContextMissingStrategy .class);

    /**
     * Logs {@code message} on the {@code error} level, and a stacktrace at {@code debug} level.
     * @param message the message to log
     * @param exceptionClass the type of exception suppressed in favor of logging {@code message}
     */
    @Override
    public void contextMissing(String message, Class<? extends RuntimeException> exceptionClass) {
        logger.error("Suppressing AWS X-Ray context missing exception (" + exceptionClass.getSimpleName() + "): " + message);
        if (logger.isDebugEnabled()) {
            logger.debug("Attempted to find context at:", new RuntimeException(message));
        }
    }
}
