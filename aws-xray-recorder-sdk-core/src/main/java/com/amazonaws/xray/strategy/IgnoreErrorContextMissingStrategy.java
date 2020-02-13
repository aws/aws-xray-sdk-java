package com.amazonaws.xray.strategy;

public class IgnoreErrorContextMissingStrategy implements ContextMissingStrategy {
    public static final String OVERRIDE_VALUE = "IGNORE_ERROR";

    /**
     * Ignore the error
     * @param message not used
     * @param exceptionClass not used
     */
    @Override
    public void contextMissing(String message, Class<? extends RuntimeException> exceptionClass) {
        // do nothing
    }

}
