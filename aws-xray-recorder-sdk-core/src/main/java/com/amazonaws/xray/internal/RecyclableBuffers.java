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

/**
 * {@link ThreadLocal} buffers for use when creating new derived objects such as {@link String}s.
 * These buffers are reused within a single thread - it is _not safe_ to use the buffer to generate
 * multiple derived objects at the same time because the same memory will be used. In general, you
 * should get a temporary buffer, fill it with data, and finish by converting into the derived
 * object within the same method to avoid multiple usages of the same buffer.
 */
public final class RecyclableBuffers {

    private static final ThreadLocal<StringBuilder> STRING_BUILDER = new ThreadLocal<>();

    /**
     * A {@link ThreadLocal} {@link StringBuilder}. Take care when filling a large value into this buffer
     * because the memory will remain for the lifetime of the thread.
     */
    public static StringBuilder stringBuilder() {
        StringBuilder buffer = STRING_BUILDER.get();
        if (buffer == null) {
            buffer = new StringBuilder();
            STRING_BUILDER.set(buffer);
        }
        buffer.setLength(0);
        return buffer;
    }

    private RecyclableBuffers() {
    }
}
