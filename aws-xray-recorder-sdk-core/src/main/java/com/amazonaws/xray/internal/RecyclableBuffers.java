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

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link ThreadLocal} buffers for use when creating new derived objects such as {@link String}s.
 * These buffers are reused within a single thread - it is _not safe_ to use the buffer to generate
 * multiple derived objects at the same time because the same memory will be used. In general, you
 * should get a temporary buffer, fill it with data, and finish by converting into the derived
 * object within the same method to avoid multiple usages of the same buffer.
 */
public final class RecyclableBuffers {

    private static final ThreadLocal<@Nullable StringBuilder> STRING_BUILDER = new ThreadLocal<>();

    @SuppressWarnings("nullness:type.argument.type.incompatible")
    private static final ThreadLocal<char[]> CHARS = new ThreadLocal<>();

    @SuppressWarnings("nullness:type.argument.type.incompatible")
    private static final ThreadLocal<byte[]> BYTES = new ThreadLocal<>();

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

    /**
     * A {@link ThreadLocal} {@code char[]} of length {@code length}. The array is not zeroed in any way - every character of
     * a resulting {@link String} must be set explicitly. The array returned may be longer than {@code length} - always explicitly
     * set the length when using the result, for example by calling {@link String#valueOf(char[], int, int)}.
     */
    public static char[] chars(int length) {
        char[] buffer = CHARS.get();
        if (buffer == null || buffer.length < length) {
            buffer = new char[length];
            CHARS.set(buffer);
        }
        return buffer;
    }

    /**
     * A {@link ThreadLocal} {@code byte[]} of length {@code length}. The array is not zeroed in any way.
     */
    public static byte[] bytes(int length) {
        byte[] buffer = BYTES.get();
        if (buffer == null || buffer.length < length) {
            buffer = new byte[length];
            BYTES.set(buffer);
        }
        return buffer;
    }

    private RecyclableBuffers() {
    }
}
