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

package com.amazonaws.xray.contexts;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.ThreadLocalStorage;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface SegmentContext {
    /**
     * @deprecated Will be removed.
     */
    @Deprecated
    Log logger = LogFactory.getLog(SegmentContext.class);

    default Segment beginSegment(AWSXRayRecorder recorder, Segment segment) {
        return segment;
    }

    default void endSegment(AWSXRayRecorder recorder) {
    }

    @Nullable
    default Entity getTraceEntity() {
        return ThreadLocalStorage.get();
    }

    default void setTraceEntity(@Nullable Entity entity) {
        if (entity != null && entity.getCreator() != null) {
            entity.getCreator().getSegmentListeners().stream().filter(Objects::nonNull).forEach(l -> {
                l.onSetEntity(ThreadLocalStorage.get(), entity);
            });
        }
        ThreadLocalStorage.set(entity);
    }

    default void clearTraceEntity() {
        Entity oldEntity = ThreadLocalStorage.get();
        if (oldEntity != null && oldEntity.getCreator() != null) {
            oldEntity.getCreator().getSegmentListeners().stream().filter(Objects::nonNull).forEach(l -> {
                l.onClearEntity(oldEntity);
            });
        }
        ThreadLocalStorage.clear();
    }

    Subsegment beginSubsegment(AWSXRayRecorder recorder, String name);

    void endSubsegment(AWSXRayRecorder recorder);
}
