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

package com.amazonaws.xray.internal;

import java.util.Arrays;

/**
 * An internal base class for unifying the potential ID generators.
 *
 * <p>This class is internal-only and its API may receive breaking changes at any time. Do not directly
 * depend on or use this class.
 */
public abstract class IdGenerator {
    /**
     * @return a new ID suitable for use in a {@link com.amazonaws.xray.entities.TraceID TraceID}
     */
    public abstract String newTraceId();

    /**
     * @return a new ID suitable for use in any {@link com.amazonaws.xray.entities.Entity Entity} implementation
     */
    public final String newEntityId() {
        String id = Long.toString(getRandomEntityId() >>> 1, 16);
        int idLength = id.length();
        if (idLength >= 16) {
            return id;
        }

        StringBuilder idWithPad = new StringBuilder(16);
        int padLength = 16 - idLength;
        char[] pad = RecyclableBuffers.chars(padLength);
        Arrays.fill(pad, 0, padLength, '0');
        idWithPad.append(pad, 0, padLength);
        idWithPad.append(id);
        return idWithPad.toString();
    }

    /**
     * @return a random long to use as an entity ID
     */
    protected abstract long getRandomEntityId();
}
