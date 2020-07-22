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

package com.amazonaws.xray.entities;

import com.amazonaws.xray.internal.RecyclableBuffers;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TraceHeader {
    public static final String HEADER_KEY = "X-Amzn-Trace-Id";

    private static final String DELIMITER = ";";
    private static final char EQUALS = '=';

    private static final String ROOT_PREFIX = "Root=";
    private static final String PARENT_PREFIX = "Parent=";
    private static final String SAMPLED_PREFIX = "Sampled=";
    private static final String SELF_PREFIX = "Self=";

    private static final String MALFORMED_ERROR_MESSAGE = "Malformed TraceHeader String input.";

    private static final Log logger =
        LogFactory.getLog(TraceHeader.class);

    public enum SampleDecision {
        SAMPLED("Sampled=1"), NOT_SAMPLED("Sampled=0"), UNKNOWN(""), REQUESTED("Sampled=?");

        private final String value;

        SampleDecision(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static SampleDecision fromString(String text) {
            for (SampleDecision decision : SampleDecision.values()) {
                if (decision.toString().equalsIgnoreCase(text)) {
                    return decision;
                }
            }
            return UNKNOWN;
        }
    }

    @Nullable
    private TraceID rootTraceId;
    @Nullable
    private String parentId;
    private SampleDecision sampled;

    private Map<String, String> additionalParams = new ConcurrentHashMap<>();

    public TraceHeader() {
        this(null, null, SampleDecision.UNKNOWN);
    }

    public TraceHeader(@Nullable TraceID rootTraceId) {
        this(rootTraceId, null, SampleDecision.UNKNOWN);
    }

    public TraceHeader(@Nullable TraceID rootTraceId, @Nullable String parentId) {
        this(rootTraceId, parentId, SampleDecision.UNKNOWN);
    }

    public TraceHeader(@Nullable TraceID rootTraceId, @Nullable String parentId, SampleDecision sampled) {
        this.rootTraceId = rootTraceId;
        this.parentId = parentId;
        this.sampled = sampled;
        if (sampled == null) {
            throw new IllegalArgumentException("Sample decision can not be null.");
        }
    }

    /**
     * Creates a TraceHeader object from a String. Note that this will silently ignore any "Self=" trace ids injected from ALB.
     *
     * @param string
     *            the string from an incoming trace-id header
     * @return the TraceHeader object
     */
    public static TraceHeader fromString(@Nullable String string) {
        TraceHeader traceHeader = new TraceHeader();

        if (string == null) {
            return traceHeader;
        }

        int pos = 0;
        while (pos < string.length()) {
            int delimiterIndex = string.indexOf(';', pos);
            final String part;
            if (delimiterIndex >= 0) {
                part = string.substring(pos, delimiterIndex);
                pos = delimiterIndex + 1;
            } else {
                // Last part.
                part = string.substring(pos);
                pos = string.length();
            }
            String trimmedPart = part.trim();
            int equalsIndex = trimmedPart.indexOf('=');
            if (equalsIndex < 0) {
                logger.error(MALFORMED_ERROR_MESSAGE);
                continue;
            }

            String value = trimmedPart.substring(equalsIndex + 1);

            if (trimmedPart.startsWith(ROOT_PREFIX)) {
                traceHeader.setRootTraceId(TraceID.fromString(value));
            } else if (trimmedPart.startsWith(PARENT_PREFIX)) {
                traceHeader.setParentId(value);
            } else if (trimmedPart.startsWith(SAMPLED_PREFIX)) {
                traceHeader.setSampled(SampleDecision.fromString(trimmedPart));
            } else if (!trimmedPart.startsWith(SELF_PREFIX)) {
                String key = trimmedPart.substring(0, equalsIndex);
                traceHeader.putAdditionalParam(key, value);
            }
        }
        return traceHeader;
    }

    /**
     * Serializes the TraceHeader object into a String.
     *
     * @return the String representation of this TraceHeader
     */
    @Override
    public String toString() {
        StringBuilder buffer = RecyclableBuffers.stringBuilder();
        if (rootTraceId != null) {
            buffer.append(ROOT_PREFIX).append(rootTraceId).append(DELIMITER);
        }
        if (StringValidator.isNotNullOrBlank(parentId)) {
            buffer.append(PARENT_PREFIX).append(parentId).append(DELIMITER);
        }
        buffer.append(sampled).append(DELIMITER);
        additionalParams.forEach((key, value) -> {
            buffer.append(key).append(EQUALS).append(value).append(DELIMITER);
        });
        buffer.setLength(buffer.length() - DELIMITER.length());
        return buffer.toString();
    }

    /**
     * @return the rootTraceId
     */
    @Nullable
    public TraceID getRootTraceId() {
        return rootTraceId;
    }

    /**
     * @param rootTraceId the rootTraceId to set
     */
    public void setRootTraceId(TraceID rootTraceId) {
        this.rootTraceId = rootTraceId;
    }

    /**
     * @return the parentId
     */
    @Nullable
    public String getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * @return the sampled
     */
    public SampleDecision getSampled() {
        return sampled;
    }

    /**
     * Sets the sample decision.
     * @param sampled
     *            the non-null SampleDecision to set
     *
     * @throws IllegalArgumentException
     *             if sampled is null
     */
    public void setSampled(SampleDecision sampled) {
        if (sampled == null) {
            throw new IllegalArgumentException("Sample decision can not be null. Please use SampleDecision.UNKNOWN instead.");
        }
        this.sampled = sampled;
    }

    /**
     * @return the additionalParams
     */
    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    /**
     * @param additionalParams the additionalParams to set
     */
    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }

    /**
     *
     * Puts an additional parameter into the {@code additionalParam} map.
     * @param key
     *  the key to put into
     * @param value
     *  the value to put
     */
    public void putAdditionalParam(String key, String value) {
        additionalParams.put(key, value);
    }
}
