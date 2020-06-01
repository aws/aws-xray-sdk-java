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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.ThreadLocalStorage;
import com.amazonaws.xray.exceptions.AlreadyEmittedException;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Entity extends AutoCloseable {

    public static String generateId() {
        String id = Long.toString(ThreadLocalStorage.getRandom().nextLong() >>> 1, 16);
        while (id.length() < 16) {
            id = '0' + id;
        }
        return id;
    }

    public String getName();

    /**
     * @return the id
     */
    public String getId();

    /**
     * @param id
     *            the id to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setId(String id);

    /**
     * @return the startTime
     */
    public double getStartTime();

    /**
     * @param startTime
     *            the startTime to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setStartTime(double startTime);

    /**
     * @return the endTime
     */
    public double getEndTime();

    /**
     * @param endTime
     *            the endTime to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setEndTime(double endTime);

    /**
     * @return the fault
     */
    public boolean isFault();

    /**
     * @param fault
     *            the fault to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setFault(boolean fault);

    /**
     * @return the error
     */
    public boolean isError();

    /**
     * Sets the error value of the entity.
     *
     * @param error
     *            the error to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setError(boolean error);

    /**
     * @return the namespace
     */
    public String getNamespace();

    /**
     * @param namespace
     *            the namespace to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setNamespace(String namespace);

    /**
     * @return the subsegmentsLock
     */
    public ReentrantLock getSubsegmentsLock();

    /**
     * @param subsegmentsLock
     *            the subsegmentsLock to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setSubsegmentsLock(ReentrantLock subsegmentsLock);

    /**
     * @return the cause
     */
    public Cause getCause();

    /**
     * @return the http
     */
    public Map<String, Object> getHttp();

    /**
     * @param http
     *            the http to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setHttp(Map<String, Object> http);

    /**
     * @return the aws
     */
    public Map<String, Object> getAws();

    /**
     * @param aws
     *            the aws to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setAws(Map<String, Object> aws);

    /**
     * @return the sql
     */
    public Map<String, Object> getSql();

    /**
     * @param sql
     *            the sql to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setSql(Map<String, Object> sql);

    /**
     * @return the metadata
     */
    public Map<String, Map<String, Object>> getMetadata();

    /**
     * @param metadata
     *            the metadata to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setMetadata(Map<String, Map<String, Object>> metadata);

    /**
     * @return the annotations
     */
    public Map<String, Object> getAnnotations();

    /**
     * @param annotations
     *            the annotations to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setAnnotations(Map<String, Object> annotations);

    /**
     * @return the parent
     */
    public Entity getParent();

    /**
     * @param parent
     *            the parent to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setParent(Entity parent);

    /**
     * @return the throttle
     */
    public boolean isThrottle();

    /**
     * Sets the throttle value. When setting to true, error is also set to true and fault set to false.
     *
     * @param throttle
     *            the throttle to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setThrottle(boolean throttle);

    /**
     * @return the inProgress
     */
    public boolean isInProgress();

    /**
     * @param inProgress
     *            the inProgress to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setInProgress(boolean inProgress);

    /**
     * @return the traceId
     */
    public TraceID getTraceId();

    /**
     * @param traceId
     *            the traceId to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setTraceId(TraceID traceId);

    /**
     * @return the parentId
     */
    public String getParentId();

    /**
     * @param parentId
     *            the parentId to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setParentId(String parentId);

    /**
     * @return the creator
     */
    public AWSXRayRecorder getCreator();

    /**
     * @param creator
     *            the creator to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void setCreator(AWSXRayRecorder creator);

    @JsonIgnore
    public abstract Segment getParentSegment();

    /**
     * @return the subsegments
     */
    public List<Subsegment> getSubsegments();

    /**
     * Adds a subsegment.
     *
     * @param subsegment
     *            the subsegment to add
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void addSubsegment(Subsegment subsegment);

    /**
     * Adds an exception to the entity's cause and sets fault to true.
     *
     * @param exception
     *            the exception to add
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void addException(Throwable exception);

    /**
     * Returns the reference count of the segment. This number represents how many open subsegments are children of this segment. The segment is emitted when its reference count reaches 0.
     *
     * @return the reference count
     */
    public int getReferenceCount();

    /**
     * @return the totalSize
     */
    public LongAdder getTotalSize();

    /**
     * Increments the subsegment-reference counter.
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    public void incrementReferenceCount();

    /**
     * Decrements the subsegment-reference counter.
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     * @return true if the segment is no longer in progress and the reference count is less than or equal to zero.
     */
    public boolean decrementReferenceCount();

    /**
     * Puts HTTP information.
     *
     * @param key
     *            the key under which the HTTP information is stored
     * @param value
     *            the HTTP information
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putHttp(String key, Object value);

    /**
     * Puts HTTP information.
     *
     * @param all
     *            the HTTP information to put
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putAllHttp(Map<String, Object> all);

    /**
     * Puts AWS information.
     *
     * @param key
     *            the key under which the AWS information is stored
     * @param value
     *            the AWS information
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putAws(String key, Object value);

    /**
     * Puts AWS information.
     *
     * @param all
     *            the AWS information to put
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putAllAws(Map<String, Object> all);

    /**
     * Puts SQL information.
     *
     * @param key
     *            the key under which the SQL information is stored
     * @param value
     *            the SQL information
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putSql(String key, Object value);

    /**
     * Puts SQL information.
     *
     * @param all
     *            the SQL information to put
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putAllSql(Map<String, Object> all);

    /**
     * Puts a String annotation.
     *
     * @param key
     *            the key under which the annotation is stored
     * @param value
     *            the String annotation
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putAnnotation(String key, String value);

    /**
     * Puts a Number annotation.
     *
     * @param key
     *            the key under which the annotation is stored
     * @param value
     *            the Number annotation
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putAnnotation(String key, Number value);

    /**
     * Puts a Boolean annotation.
     *
     * @param key
     *            the key under which the annotation is stored
     * @param value
     *            the Boolean annotation
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putAnnotation(String key, Boolean value);

    /**
     * Puts metadata under the namespace 'default'.
     *
     * @param key
     *            the key under which the metadata is stored
     * @param object
     *            the metadata
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putMetadata(String key, Object object);

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
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putMetadata(String namespace, String key, Object object);

    /**
     * Removes a subsegment from the subsegment list. Decrements the total size of the parentSegment. Marks the removed subsegment as emitted future modification on this subsegment may raise an AlreadyEmittedException.
     *
     * @param subsegment
     *      the subsegment to remove
     */
    public void removeSubsegment(Subsegment subsegment);

    public boolean isEmitted();

    public void setEmitted(boolean emitted);

    public String serialize();

    public String prettySerialize();

}
