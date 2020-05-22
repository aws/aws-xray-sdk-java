package com.amazonaws.xray.utils;

import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utilities for validating parameters loosely. By default, validation is disabled. Enable error logging by setting the system
 * property {@code -Dcom.amazonaws.xray.validationMode=log} or throwing exceptions by setting the system property
 * {@code -Dcom.amazonaws.xray.validationMode=throw}.
 */
public final class LooseValidations {
    private static final Log logger = LogFactory.getLog(LooseValidations.class);

    // Visible for testing
    enum ValidationMode {
        NONE,
        LOG,
        THROW,
    }

    private static final ValidationMode VALIDATION_MODE = validationMode();

    /**
     * Returns whether {@code obj} is {@code null}.
     */
    @CheckReturnValue
    public static boolean checkNotNull(Object obj, String message) {
        if (obj != null) {
            return true;
        }
        handleError(() -> new NullPointerException(message));
        return false;
    }

    private static void handleError(Supplier<RuntimeException> exception) {
        switch (VALIDATION_MODE) {
            case LOG:
                logger.error(exception.get());
                break;
            case THROW:
                throw exception.get();
            case NONE:
            default:
                break;
        }
    }

    private static ValidationMode validationMode() {
        return validationMode(System.getProperty("com.amazonaws.xray.validationMode", ""));
    }

    // Visible for testing
    static ValidationMode validationMode(String config) {
        try {
            return ValidationMode.valueOf(config.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ValidationMode.NONE;
        }
    }

    private LooseValidations() {}
}
