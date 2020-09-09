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

import com.amazonaws.xray.ThreadLocalStorage;
import com.amazonaws.xray.internal.RecyclableBuffers;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TraceID {

    private static final String INVALID_START_TIME = "00000000";
    private static final String INVALID_NUMBER = "000000000000000000000000";

    private static final TraceID INVALID = new TraceID(INVALID_START_TIME, INVALID_NUMBER);

    /**
     * Returns a new {@link TraceID} which represents the start of a new trace.
     */
    public static TraceID create() {
        return new TraceID();
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
     * @deprecated Use {@link #create()}.
     */
    @Deprecated
    public TraceID() {
        this(Instant.now().getEpochSecond());
    }

    /**
     * @deprecated Use {@link #create()}.
     */
    @Deprecated
    public TraceID(long startTime) {
        SecureRandom random = ThreadLocalStorage.getRandom();

        // nextBytes much faster than calling nextInt multiple times when using SecureRandom
        byte[] randomBytes = RecyclableBuffers.bytes(12);
        random.nextBytes(randomBytes);
        numberHex = bytesToBase16String(randomBytes);
        this.startTimeHex = intToBase16String((int) startTime);
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

    private static final int BYTE_BASE16 = 2;
    private static final String ALPHABET = "0123456789abcdef";
    private static final char[] ENCODING = buildEncodingArray();

    private static char[] buildEncodingArray() {
        char[] encoding = new char[512];
        for (int i = 0; i < 256; ++i) {
            encoding[i] = ALPHABET.charAt(i >>> 4);
            encoding[i | 0x100] = ALPHABET.charAt(i & 0xF);
        }
        return encoding;
    }

    private static String bytesToBase16String(byte[] bytes) {
        char[] dest = RecyclableBuffers.chars(24);
        for (int i = 0; i < 12; i++) {
            byteToBase16(bytes[i], dest, i * BYTE_BASE16);
        }

        return new String(dest, 0, 24);
    }

    private static String numberToBase16String(int hi, long lo) {
        char[] dest = RecyclableBuffers.chars(24);

        byteToBase16((byte) (hi >> 24 & 0xFFL), dest, 0);
        byteToBase16((byte) (hi >> 16 & 0xFFL), dest, BYTE_BASE16);
        byteToBase16((byte) (hi >> 8 & 0xFFL), dest, 2 * BYTE_BASE16);
        byteToBase16((byte) (hi & 0xFFL), dest, 3 * BYTE_BASE16);

        byteToBase16((byte) (lo >> 56 & 0xFFL), dest, 4 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 48 & 0xFFL), dest, 5 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 40 & 0xFFL), dest, 6 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 32 & 0xFFL), dest, 7 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 24 & 0xFFL), dest, 8 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 16 & 0xFFL), dest, 9 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 8 & 0xFFL), dest, 10 * BYTE_BASE16);
        byteToBase16((byte) (lo & 0xFFL), dest, 11 * BYTE_BASE16);

        return new String(dest, 0, 24);
    }


    private static String intToBase16String(long value) {
        char[] dest = RecyclableBuffers.chars(8);
        byteToBase16((byte) (value >> 24 & 0xFFL), dest, 0);
        byteToBase16((byte) (value >> 16 & 0xFFL), dest, BYTE_BASE16);
        byteToBase16((byte) (value >> 8 & 0xFFL), dest, 2 * BYTE_BASE16);
        byteToBase16((byte) (value & 0xFFL), dest, 3 * BYTE_BASE16);
        return new String(dest, 0, 8);
    }

    private static void byteToBase16(byte value, char[] dest, int destOffset) {
        int b = value & 0xFF;
        dest[destOffset] = ENCODING[b];
        dest[destOffset + 1] = ENCODING[b | 0x100];
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
