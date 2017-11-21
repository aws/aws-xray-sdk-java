package com.amazonaws.xray.strategy;

public interface ContextMissingStrategy {

    /**
     * Environment variable key used to override the default {@code ContextMissingStrategy} used in new instances of {@code AWSXRayRecorder}. Valid values for this environment variable are (case-insensitive) {@code RUNTIME_ERROR} and {@code LOG_ERROR}. Invalid values will be ignored.
     * Takes precedence over any system property or builder value used for the {@code DefaultContextMissingStrategy}.
     */
    public static final String CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY = "AWS_XRAY_CONTEXT_MISSING";

    /**
     * System property key used to override the default {@code ContextMissingStrategy} used in new instances of {@code AWSXRayRecorder}. Valid values for this system property are (case-insensitive) {@code RUNTIME_ERROR} and {@code LOG_ERROR}. Invalid values will be ignored.
     * Takes precedence over any builder value used for the {@code DefaultContextMissingStrategy}.
     */
    public static final String CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY = "com.amazonaws.xray.strategy.contextMissingStrategy";

    public void contextMissing(String message, Class<? extends RuntimeException> exceptionClass);
}
