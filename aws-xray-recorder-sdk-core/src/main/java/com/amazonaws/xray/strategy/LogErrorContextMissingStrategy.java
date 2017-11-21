package com.amazonaws.xray.strategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LogErrorContextMissingStrategy implements ContextMissingStrategy {
    private static final Log logger = LogFactory.getLog(LogErrorContextMissingStrategy .class);

    public static final String OVERRIDE_VALUE = "LOG_ERROR";

    /**
     * Logs {@code message} on the {#code error} level.
     * @param message the message to log
     * @param exceptionClass the type of exception suppressed in favor of logging {@code message}
     */
    @Override
    public void contextMissing(String message, Class<? extends RuntimeException> exceptionClass) {
        logger.error("Suppressing AWS X-Ray context missing exception (" + exceptionClass.getSimpleName() + "): " + message);
    }

}
