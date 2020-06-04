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

    private BigInteger number;
    private long startTime;

    public TraceID() {
        this(Instant.now().getEpochSecond());
    }

    public TraceID(long startTime) {
        number = new BigInteger(96, ThreadLocalStorage.getRandom());
        this.startTime = startTime;
    }

    public static TraceID fromString(String string) {
        string = string.trim();
        TraceID traceId = new TraceID();

        long startTime = 0;
        BigInteger number = null;

        int delimiterIndex;

        // Skip version number
        delimiterIndex = string.indexOf(DELIMITER);
        if (delimiterIndex < 0) {
            return traceId;
        }

        int valueStartIndex = delimiterIndex + 1;
        delimiterIndex = string.indexOf(DELIMITER, valueStartIndex);
        if (delimiterIndex < 0) {
            return traceId;
        } else {
            startTime = Long.valueOf(string.substring(valueStartIndex, delimiterIndex), 16);
        }

        valueStartIndex = delimiterIndex + 1;
        delimiterIndex = string.indexOf(DELIMITER, valueStartIndex);
        if (delimiterIndex < 0) {
            // End of string
            delimiterIndex = string.length();
        }
        number = new BigInteger(string.substring(valueStartIndex, delimiterIndex), 16);

        traceId.setStartTime(startTime);
        traceId.setNumber(number);

        return traceId;
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

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + ((number == null) ? 0 : number.hashCode());
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TraceID)) {
            return false;
        }
        TraceID other = (TraceID) obj;
        return Objects.equals(number, other.number) && startTime == other.startTime;
    }
}
