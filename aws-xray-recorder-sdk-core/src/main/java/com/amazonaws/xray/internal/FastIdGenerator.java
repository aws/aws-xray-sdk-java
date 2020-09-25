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

import com.amazonaws.xray.utils.ByteUtils;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates random IDs using a fast but cryptographically insecure random number
 * generator. This should be the default random generator, unless your application
 * relies on AWS X-Ray trace IDs being generated from a cryptographically secure random number
 * source.
 *
 * <p>This class is internal-only and its API may receive breaking changes at any time. Do not directly
 * depend on or use this class.
 *
 * @see SecureIdGenerator
 */
public final class FastIdGenerator extends IdGenerator {
    @Override
    public String newTraceId() {
        Random random = ThreadLocalRandom.current();
        return ByteUtils.numberToBase16String(random.nextInt(), random.nextLong());
    }

    @Override
    protected long getRandomEntityId() {
        return ThreadLocalRandom.current().nextLong();
    }
}
