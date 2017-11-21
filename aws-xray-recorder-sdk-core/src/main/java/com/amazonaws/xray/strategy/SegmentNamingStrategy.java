package com.amazonaws.xray.strategy;

import javax.servlet.http.HttpServletRequest;

import com.amazonaws.xray.entities.StringValidator;

public interface SegmentNamingStrategy {

    /**
     * Environment variable key used to override the default segment name used by implementors of {@code SegmentNamingStrategy}.
     * Takes precedence over any system property, web.xml configuration value, or constructor value used for a fixed segment name.
     */
    public static final String NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY = "AWS_XRAY_TRACING_NAME";

    /**
     * System property key used to override the default segment name used by implementors of {@code SegmentNamingStrategy}.
     * Takes precedence over any web.xml configuration value or constructor value used for a fixed segment name.
     */
    public static final String NAME_OVERRIDE_SYSTEM_PROPERTY_KEY = "com.amazonaws.xray.strategy.tracingName";

    public String nameForRequest(HttpServletRequest request);

    default String getOverrideName() {
        String environmentNameOverrideValue = System.getenv(NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY);
        String systemNameOverrideValue = System.getProperty(NAME_OVERRIDE_SYSTEM_PROPERTY_KEY);
        if (StringValidator.isNotNullOrBlank(environmentNameOverrideValue)) {
            return environmentNameOverrideValue;
        } else if (StringValidator.isNotNullOrBlank(systemNameOverrideValue)) {
            return systemNameOverrideValue;
        }
        return null;
    }
}
