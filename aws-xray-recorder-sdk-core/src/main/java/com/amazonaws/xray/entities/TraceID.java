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
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;

public class TraceID {

    private static final char VERSION = '1';
    private static final char DELIMITER = '-';
    private static final String HEX_PREFIX = "0x";

    private BigInteger number;
    private long startTime;

    public static TraceID fromString(String string) {
        TraceID traceId = new TraceID();
        String[] parts = string.trim().split(DELIMITER + "");

        if (parts.length >= 3) {
            traceId.setStartTime(Long.decode(HEX_PREFIX + parts[1]));
            traceId.setNumber(new BigInteger(parts[2], 16));
        }

        return traceId;
    }

    public TraceID() {
        this(Instant.now().getEpochSecond());
    }

    public TraceID(long startTime) {
        number = new BigInteger(96, ThreadLocalStorage.getRandom());
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        String paddedNumber = number.toString(16);
        while (paddedNumber.length() < 24) {
            paddedNumber = '0' + paddedNumber;
        }
        return "" + VERSION + DELIMITER + Long.toHexString(startTime) + DELIMITER + paddedNumber;
    }

    /**
     * @return the number
     */
    public BigInteger getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(BigInteger number) {
        this.number = number;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    private static final int PRIME = 31;
    @Override
    public int hashCode() {
        int result = 1;
        result = PRIME * result + ((number == null) ? 0 : number.hashCode());
        result = PRIME * result + (int) (startTime ^ (startTime >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof TraceID)) {
            return false;
        }
        TraceID other = (TraceID) obj;
        return Objects.equals(number, other.number) && startTime == other.startTime;
    }
}
