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

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.exceptions.AlreadyEmittedException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;

public interface Segment extends Entity {

    static Segment noOp(TraceID traceId, AWSXRayRecorder recorder) {
        return new NoOpSegment(traceId, recorder);
    }

    /**
     * Ends the segment. Sets the end time to the current time. Sets inProgress to false.
     *
     * @return true if 1) the reference count is less than or equal to zero and 2) sampled is true
     */
    boolean end();

    /**
     * Returns {@code} if this {@link Segment} is recording events and will be emitted. Any operations on a {@link Segment}
     * which is not recording are effectively no-op.
     */
    @JsonIgnore
    boolean isRecording();

    /**
     * @return the sampled
     */
    boolean isSampled();

    /**
     * @param sampled
     *            the sampled to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void setSampled(boolean sampled);

    /**
     * @return the resourceArn
     */
    String getResourceArn();

    /**
     * @param resourceArn
     *            the resourceArn to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void setResourceArn(String resourceArn);

    /**
     * @return the user
     */
    String getUser();

    /**
     * @param user
     *            the user to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void setUser(String user);

    /**
     * @return the origin
     */
    String getOrigin();

    /**
     * @param origin
     *            the origin to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void setOrigin(String origin);

    /**
     * @return the service
     */
    Map<String, Object> getService();

    /**
     * @param service
     *            the service to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void setService(Map<String, Object> service);

    /**
     * @return the annotations
     */
    @Override
    Map<String, Object> getAnnotations();

    /**
     * Puts information about this service.
     *
     * @param key
     *            the key under which the service information is stored
     * @param object
     *            the service information
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putService(String key, Object object);

    /**
     * Puts information about this service.
     *
     * @param all
     *            the service information to set.
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putAllService(Map<String, Object> all);

    void setRuleName(String name);

    @Override
    Segment getParentSegment();

    @Override
    void close();

}
