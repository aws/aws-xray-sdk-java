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

import static com.amazonaws.xray.utils.ByteUtils.bytesToBase16String;

import com.amazonaws.xray.ThreadLocalStorage;

/**
 * Generates for IDs using a cryptographically secure random number generator.
 * This can be much more expensive than the alternative {@linkplain FastIdGenerator}. This
 * generator should only be used if your application relies on AWS X-Ray IDs being
 * generated from a cryptographically secure random number source.
 *
 * <p>This class is internal-only and its API may receive breaking changes at any time. Do not directly
 * depend on or use this class.
 *
 * @see FastIdGenerator
 */
public final class SecureIdGenerator extends IdGenerator {
    @Override
    public String newTraceId() {
        // nextBytes much faster than calling nextInt multiple times when using SecureRandom
        byte[] randomBytes = RecyclableBuffers.bytes(12);
        ThreadLocalStorage.getRandom().nextBytes(randomBytes);
        return bytesToBase16String(randomBytes);
    }

    @Override
    protected long getRandomEntityId() {
        return ThreadLocalStorage.getRandom().nextLong();
    }
}
