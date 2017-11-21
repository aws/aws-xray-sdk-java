package com.amazonaws.xray.entities;

public class StringValidator {

    public static boolean isNotNullOrBlank(String string) {
        return null != string && !string.trim().isEmpty();
    }

    public static boolean isNullOrBlank(String string) {
        return null == string || string.trim().isEmpty();
    }

    public static void throwIfNullOrBlank(String string, String validationErrorMessage) {
        if (null == string || string.trim().isEmpty()) {
            throw new RuntimeException(validationErrorMessage);
        }
    }

}
