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

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.exceptions.AlreadyEmittedException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Entity extends AutoCloseable {

    /**
     * @deprecated Use the {@link com.amazonaws.xray.internal.IdGenerator ID generator} configured on this
     * entity's creator instead
     */
    @Deprecated
    static String generateId() {
        return AWSXRay.getGlobalRecorder().getIdGenerator().newEntityId();
    }

    /**
     * Immediately runs the provided {@link Runnable} with this {@link Segment} as the current entity.
     */
    default void run(Runnable runnable) {
        run(runnable, getCreator());
    }

    /**
     * Immediately runs the provided {@link Runnable} with this {@link Segment} as the current entity.
     */
    default void run(Runnable runnable, AWSXRayRecorder recorder) {
        Entity previous = recorder.getTraceEntity();
        recorder.setTraceEntity(this);
        try {
            runnable.run();
        } finally {
            recorder.setTraceEntity(previous);
        }
    }

    String getName();

    /**
     * @return the id
     */
    String getId();

    /**
     * @param id
     *            the id to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setId(String id);

    /**
     * @return the startTime
     */
    double getStartTime();

    /**
     * @param startTime
     *            the startTime to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setStartTime(double startTime);

    /**
     * @return the endTime
     */
    double getEndTime();

    /**
     * @param endTime
     *            the endTime to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setEndTime(double endTime);

    /**
     * @return the fault
     */
    boolean isFault();

    /**
     * @param fault
     *            the fault to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setFault(boolean fault);

    /**
     * @return the error
     */
    boolean isError();

    /**
     * Sets the error value of the entity.
     *
     * @param error
     *            the error to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setError(boolean error);

    /**
     * @return the namespace
     */
    @Nullable
    String getNamespace();

    /**
     * @param namespace
     *            the namespace to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setNamespace(String namespace);

    /**
     * @return an unused {@link ReentrantLock}
     *
     * @deprecated This is for internal use of the SDK and will be made private.
     */
    @Deprecated
    ReentrantLock getSubsegmentsLock();

    /**
     * @param subsegmentsLock
     *            the subsegmentsLock to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     * @deprecated This is for internal use of the SDK and will be made private
     */
    @Deprecated
    void setSubsegmentsLock(ReentrantLock subsegmentsLock);

    /**
     * @return the cause
     */
    Cause getCause();

    /**
     * @return the http
     */
    Map<String, Object> getHttp();

    /**
     * @param http
     *            the http to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setHttp(Map<String, Object> http);

    /**
     * @return the aws
     */
    Map<String, Object> getAws();

    /**
     * @param aws
     *            the aws to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setAws(Map<String, Object> aws);

    /**
     * @return the sql
     */
    Map<String, Object> getSql();

    /**
     * @param sql
     *            the sql to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setSql(Map<String, Object> sql);

    /**
     * @return the metadata
     */
    Map<String, Map<String, Object>> getMetadata();

    /**
     * @param metadata
     *            the metadata to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setMetadata(Map<String, Map<String, Object>> metadata);

    /**
     * @return the annotations
     */
    Map<String, Object> getAnnotations();

    /**
     * @param annotations
     *            the annotations to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setAnnotations(Map<String, Object> annotations);

    /**
     * @return the parent
     */
    Entity getParent();

    /**
     * @param parent
     *            the parent to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setParent(Entity parent);

    /**
     * @return the throttle
     */
    boolean isThrottle();

    /**
     * Sets the throttle value. When setting to true, error is also set to true and fault set to false.
     *
     * @param throttle
     *            the throttle to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setThrottle(boolean throttle);

    /**
     * @return the inProgress
     */
    boolean isInProgress();

    /**
     * @param inProgress
     *            the inProgress to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setInProgress(boolean inProgress);

    /**
     * @return the traceId
     */
    TraceID getTraceId();

    /**
     * @param traceId
     *            the traceId to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setTraceId(TraceID traceId);

    /**
     * @return the parentId
     */
    @Nullable
    String getParentId();

    /**
     * @param parentId
     *            the parentId to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setParentId(@Nullable String parentId);

    /**
     * @return the creator
     */
    AWSXRayRecorder getCreator();

    /**
     * @param creator
     *            the creator to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void setCreator(AWSXRayRecorder creator);

    @JsonIgnore
    Segment getParentSegment();

    /**
     * @return the subsegments
     *
     * @deprecated Use {@link #getSubsegmentsCopy()}.
     */
    @Deprecated
    List<Subsegment> getSubsegments();

    /**
     * Returns a copy of the currently added subsegments. Updates to the returned {@link List} will not be reflected in the
     * {@link Entity}.
     */
    List<Subsegment> getSubsegmentsCopy();

    /**
     * Adds a subsegment.
     *
     * @param subsegment
     *            the subsegment to add
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void addSubsegment(Subsegment subsegment);

    /**
     * Adds an exception to the entity's cause and sets fault to true.
     *
     * @param exception
     *            the exception to add
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void addException(Throwable exception);

    /**
     * Returns the reference count of the segment. This number represents how many open subsegments are children of this segment.
     * The segment is emitted when its reference count reaches 0.
     *
     * @return the reference count
     */
    int getReferenceCount();

    /**
     * @return the totalSize
     */
    LongAdder getTotalSize();

    /**
     * Increments the subsegment-reference counter.
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    void incrementReferenceCount();

    /**
     * Decrements the subsegment-reference counter.
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     * @return true if the segment is no longer in progress and the reference count is less than or equal to zero.
     */
    boolean decrementReferenceCount();

    /**
     * Puts HTTP information.
     *
     * @param key
     *            the key under which the HTTP information is stored
     * @param value
     *            the HTTP information
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putHttp(String key, Object value);

    /**
     * Puts HTTP information.
     *
     * @param all
     *            the HTTP information to put
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putAllHttp(Map<String, Object> all);

    /**
     * Puts AWS information.
     *
     * @param key
     *            the key under which the AWS information is stored
     * @param value
     *            the AWS information
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putAws(String key, Object value);

    /**
     * Puts AWS information.
     *
     * @param all
     *            the AWS information to put
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putAllAws(Map<String, Object> all);

    /**
     * Puts SQL information.
     *
     * @param key
     *            the key under which the SQL information is stored
     * @param value
     *            the SQL information
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putSql(String key, Object value);

    /**
     * Puts SQL information.
     *
     * @param all
     *            the SQL information to put
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putAllSql(Map<String, Object> all);

    /**
     * Puts a String annotation.
     *
     * @param key
     *            the key under which the annotation is stored
     * @param value
     *            the String annotation
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putAnnotation(String key, String value);

    /**
     * Puts a Number annotation.
     *
     * @param key
     *            the key under which the annotation is stored
     * @param value
     *            the Number annotation
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putAnnotation(String key, Number value);

    /**
     * Puts a Boolean annotation.
     *
     * @param key
     *            the key under which the annotation is stored
     * @param value
     *            the Boolean annotation
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putAnnotation(String key, Boolean value);

    /**
     * Puts metadata under the namespace 'default'.
     *
     * @param key
     *            the key under which the metadata is stored
     * @param object
     *            the metadata
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putMetadata(String key, Object object);

    /**
     * Puts metadata.
     *
     * @param namespace
     *            the namespace under which the metadata is stored
     * @param key
     *            the key under which the metadata is stored
     * @param object
     *            the metadata
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    void putMetadata(String namespace, String key, Object object);

    /**
     * Removes a subsegment from the subsegment list. Decrements the total size of the parentSegment. Marks the removed subsegment
     * as emitted future modification on this subsegment may raise an AlreadyEmittedException.
     *
     * @param subsegment
     *      the subsegment to remove
     */
    void removeSubsegment(Subsegment subsegment);

    boolean isEmitted();

    /**
     * @deprecated Use {@link #compareAndSetEmitted(boolean, boolean)}
     */
    @Deprecated
    void setEmitted(boolean emitted);

    /**
     * Checks whether this {@link Entity} currently has emitted state of {@code current} and if so, set emitted state to
     * {@code next}. Returns {@code true} if the state was updated, or {@code false} otherwise.
     */
    boolean compareAndSetEmitted(boolean current, boolean next);

    String serialize();

    String prettySerialize();

}
