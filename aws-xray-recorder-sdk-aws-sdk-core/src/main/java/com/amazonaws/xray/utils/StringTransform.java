package com.amazonaws.xray.utils;

public class StringTransform {
    private static final String REGEX = "([a-z])([A-Z]+)";
    private static final String REPLACE = "$1_$2";

    public static String toSnakeCase(String camelCase) {
        return camelCase.replaceAll(REGEX, REPLACE).toLowerCase();
    }
}