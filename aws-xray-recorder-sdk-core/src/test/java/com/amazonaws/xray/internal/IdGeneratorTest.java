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

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class IdGeneratorTest {
    @Test
    public void testEntityIdPadding() {
        Assert.assertEquals("0000000000000123", new TestIdGenerator(0x123L).newEntityId());
        Assert.assertEquals(Long.toString(Long.MAX_VALUE, 16), new TestIdGenerator(Long.MAX_VALUE).newEntityId());
    }

    private static class TestIdGenerator extends IdGenerator {
        private final long entityId;

        private TestIdGenerator(long entityId) {
            this.entityId = entityId << 1; // offset the signed right shift in `IdGenerator`
        }

        @Override
        public String newTraceId() {
            return "trace id";
        }

        @Override
        protected long getRandomEntityId() {
            return entityId;
        }
    }
}
