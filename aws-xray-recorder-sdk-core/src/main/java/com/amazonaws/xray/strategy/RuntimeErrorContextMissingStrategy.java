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
