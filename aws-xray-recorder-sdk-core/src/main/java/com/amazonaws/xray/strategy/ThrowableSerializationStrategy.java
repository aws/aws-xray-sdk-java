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

package com.amazonaws.xray.strategy;

import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.ThrowableDescription;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ThrowableSerializationStrategy {
    /**
     * Serializes a {@code Throwable} into a {@code ThrowableDescription}. Uses the provided subsegments to chain exceptions where
     * possible.
     *
     * @param throwable
     *            the Throwable to serialize
     * @param subsegments
     *            the list of subsegment children in which to look for the same {@code Throwable} object, for chaining
     *
     * @return a list of {@code ThrowableDescription}s which represent the provided {@code Throwable}
     */
    List<ThrowableDescription> describeInContext(Throwable throwable, List<Subsegment> subsegments);

    /**
     * Serializes a {@code Throwable} into a {@code ThrowableDescription}. Uses the provided subsegments to chain exceptions where
     * possible.
     *
     * @param entity
     *            the current entity. May be null.
     * @param throwable
     *            the Throwable to serialize
     * @param subsegments
     *            the list of subsegment children in which to look for the same {@code Throwable} object, for chaining
     *
     * @return a list of {@code ThrowableDescription}s which represent the provided {@code Throwable}
     */
    default List<ThrowableDescription> describeInContext(
        @Nullable Entity entity,
        Throwable throwable,
        List<Subsegment> subsegments
    ) {
        return describeInContext(throwable, subsegments);
    }
}
