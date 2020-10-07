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

import static com.amazonaws.xray.utils.ByteUtils.intToBase16String;
import static com.amazonaws.xray.utils.ByteUtils.numberToBase16String;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.internal.TimeUtils;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TraceID {

    private static final String INVALID_START_TIME = "00000000";
    private static final String INVALID_NUMBER = "000000000000000000000000";

    private static final TraceID INVALID = new TraceID(INVALID_START_TIME, INVALID_NUMBER);

    /**
     * Returns a new {@link TraceID} which represents the start of a new trace. This new ID
     * is generated according to the settings provided by the global AWSXRayRecorder instance
     * returned by {@link AWSXRay#getGlobalRecorder}.
     *
     * @see #create(AWSXRayRecorder)
     */
    public static TraceID create() {
        return new TraceID(TimeUtils.currentEpochSecond(), AWSXRay.getGlobalRecorder());
    }

    /**
     * Returns a new {@code TraceID} which represents the start of a new trace. This new
     * ID is generated according to the settings provided by the AWSXRayRecorder instance
     * that created it.
     */
    public static TraceID create(AWSXRayRecorder creator) {
        return new TraceID(TimeUtils.currentEpochSecond(), creator);
    }

    /**
     * Returns the {@link TraceID} parsed out of the {@link String}. If the parse fails, a new {@link TraceID} will be returned,
     * effectively restarting the trace.
     */
    public static TraceID fromString(String xrayTraceId) {
        xrayTraceId = xrayTraceId.trim();

        if (xrayTraceId.length() != TRACE_ID_LENGTH) {
            return TraceID.create();
        }

        // Check version trace id version
        if (xrayTraceId.charAt(0) != VERSION) {
            return TraceID.create();
        }

        // Check delimiters
        if (xrayTraceId.charAt(TRACE_ID_DELIMITER_INDEX_1) != DELIMITER
            || xrayTraceId.charAt(TRACE_ID_DELIMITER_INDEX_2) != DELIMITER) {
            return TraceID.create();
        }

        String startTimePart = xrayTraceId.substring(TRACE_ID_DELIMITER_INDEX_1 + 1, TRACE_ID_DELIMITER_INDEX_2);
        if (!isHex(startTimePart)) {
            return TraceID.create();
        }
        String randomPart = xrayTraceId.substring(TRACE_ID_DELIMITER_INDEX_2 + 1, TRACE_ID_LENGTH);
        if (!isHex(randomPart)) {
            return TraceID.create();
        }

        return new TraceID(startTimePart, randomPart);
    }

    /**
     * Returns an invalid {@link TraceID} which can be used when an ID is needed outside the context of a trace, for example for
     * an unsampled segment.
     */
    public static TraceID invalid() {
        return INVALID;
    }

    private static final int TRACE_ID_LENGTH = 35;
    private static final int TRACE_ID_DELIMITER_INDEX_1 = 1;
    private static final int TRACE_ID_DELIMITER_INDEX_2 = 10;

    private static final char VERSION = '1';
    private static final char DELIMITER = '-';

    private String numberHex;
    private String startTimeHex;

    /**
     * @deprecated Use {@link #create()} or {@link #create(AWSXRayRecorder)}
     */
    @Deprecated
    public TraceID() {
        this(TimeUtils.currentEpochSecond());
    }

    /**
     * @deprecated Use {@link #create()} or {@link #create(AWSXRayRecorder)}
     */
    @Deprecated
    public TraceID(long startTime) {
        this(startTime, AWSXRay.getGlobalRecorder());
    }

    private TraceID(long startTime, AWSXRayRecorder creator) {
        this(intToBase16String((int) startTime), creator.getIdGenerator().newTraceId());
    }

    private TraceID(String startTimeHex, String numberHex) {
        this.startTimeHex = startTimeHex;
        this.numberHex = numberHex;
    }

    @Override
    public String toString() {
        return "" + VERSION + DELIMITER + startTimeHex + DELIMITER + numberHex;
    }

    /**
     * @return the number
     *
     * @deprecated use {@link #getNumberAsHex()}.
     */
    @Deprecated
    public BigInteger getNumber() {
        return new BigInteger(numberHex, 16);
    }

    /**
     * Returns the number component of this {@link TraceID} as a hexadecimal string.
     */
    public String getNumberAsHex() {
        return numberHex;
    }

    /**
     * @param number the number to set
     *
     * @deprecated TraceID is effectively immutable and this will be removed
     */
    @Deprecated
    public void setNumber(@Nullable BigInteger number) {
        if (number != null) {
            this.numberHex = numberToBase16String(number.shiftRight(64).intValue(), number.longValue());
        }
    }

    /**
     * @return the startTime
     *
     * @deprecated Use {@link #getStartTimeAsHex()}.
     */
    public long getStartTime() {
        return Long.parseLong(startTimeHex, 16);
    }

    /**
     * Returns the start time of this {@link TraceID} as a hexadecimal string representing the number of seconds since
     * the epoch.
     */
    public String getStartTimeAsHex() {
        return startTimeHex;
    }

    /**
     * @param startTime the startTime to set
     *
     * @deprecated TraceID is effectively immutable and this will be removed
     */
    @Deprecated
    public void setStartTime(long startTime) {
        this.startTimeHex = intToBase16String(startTime);
    }

    @Override
    public int hashCode() {
        return 31 * numberHex.hashCode() + startTimeHex.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TraceID)) {
            return false;
        }
        TraceID other = (TraceID) obj;
        return numberHex.equals(other.numberHex) && startTimeHex.equals(other.startTimeHex);
    }

    // Visible for testing
    static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!isDigit(c) && !isLowercaseHexCharacter(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLowercaseHexCharacter(char b) {
        return 'a' <= b && b <= 'f';
    }

    private static boolean isDigit(char b) {
        return '0' <= b && b <= '9';
    }
}
