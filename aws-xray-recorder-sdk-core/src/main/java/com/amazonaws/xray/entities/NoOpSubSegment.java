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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.nullness.qual.Nullable;

class NoOpSubSegment implements Subsegment {

    private final Segment parentSegment;
    private final AWSXRayRecorder creator;
    private final boolean shouldPropagate;

    private volatile Entity parent;

    NoOpSubSegment(Segment parentSegment, AWSXRayRecorder creator) {
        this(parentSegment, creator, true);
    }

    NoOpSubSegment(Segment parentSegment, AWSXRayRecorder creator, boolean shouldPropagate) {
        this.parentSegment = parentSegment;
        this.creator = creator;
        this.shouldPropagate = shouldPropagate;
        parent = parentSegment;
    }

    @Override
    public boolean end() {
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public void setId(String id) {
    }

    @Override
    public double getStartTime() {
        return 0;
    }

    @Override
    public void setStartTime(double startTime) {
    }

    @Override
    public double getEndTime() {
        return 0;
    }

    @Override
    public void setEndTime(double endTime) {
    }

    @Override
    public boolean isFault() {
        return false;
    }

    @Override
    public void setFault(boolean fault) {
    }

    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public void setError(boolean error) {
    }

    @Override
    public String getNamespace() {
        return "";
    }

    @Override
    public void setNamespace(String namespace) {
    }

    @Override
    public ReentrantLock getSubsegmentsLock() {
        return NoOpReentrantLock.get();
    }

    @Override
    public void setSubsegmentsLock(ReentrantLock subsegmentsLock) {
    }

    @Override
    public Cause getCause() {
        // It should not be common for this to be called on an unsampled segment so we lazily initialize here.
        return new Cause(NoOpList.get(), NoOpList.get());
    }

    @Override
    public Map<String, Object> getHttp() {
        return NoOpMap.get();
    }

    @Override
    public void setHttp(Map<String, Object> http) {
    }

    @Override
    public Map<String, Object> getAws() {
        return NoOpMap.get();
    }

    @Override
    public void setAws(Map<String, Object> aws) {
    }

    @Override
    public Map<String, Object> getSql() {
        return NoOpMap.get();
    }

    @Override
    public void setSql(Map<String, Object> sql) {
    }

    @Override
    public Map<String, Map<String, Object>> getMetadata() {
        return NoOpMap.get();
    }

    @Override
    public void setMetadata(Map<String, Map<String, Object>> metadata) {
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return NoOpMap.get();
    }

    @Override
    public void setAnnotations(Map<String, Object> annotations) {
    }

    @Override
    public Entity getParent() {
        return parent;
    }

    @Override
    public void setParent(Entity parent) {
        this.parent = parent;
    }

    @Override
    public boolean isThrottle() {
        return false;
    }

    @Override
    public void setThrottle(boolean throttle) {
    }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public void setInProgress(boolean inProgress) {
    }

    @Override
    public TraceID getTraceId() {
        return parentSegment.getTraceId();
    }

    @Override
    public void setTraceId(TraceID traceId) {
    }

    @Override
    public @Nullable String getParentId() {
        return null;
    }

    @Override
    public void setParentId(@Nullable String parentId) {
    }

    @Override
    public AWSXRayRecorder getCreator() {
        return creator;
    }

    @Override
    public void setCreator(AWSXRayRecorder creator) {
    }

    @Override
    public Segment getParentSegment() {
        return parentSegment;
    }

    @Override
    public List<Subsegment> getSubsegments() {
        return NoOpList.get();
    }

    @Override
    public List<Subsegment> getSubsegmentsCopy() {
        return NoOpList.get();
    }

    @Override
    public void addSubsegment(Subsegment subsegment) {
    }

    @Override
    public void addException(Throwable exception) {
    }

    @Override
    public int getReferenceCount() {
        return 0;
    }

    @Override
    public LongAdder getTotalSize() {
        // It should not be common for this to be called on an unsampled segment so we lazily initialize here.
        return new LongAdder();
    }

    @Override
    public void incrementReferenceCount() {
    }

    @Override
    public boolean decrementReferenceCount() {
        return false;
    }

    @Override
    public void putHttp(String key, Object value) {
    }

    @Override
    public void putAllHttp(Map<String, Object> all) {
    }

    @Override
    public void putAws(String key, Object value) {
    }

    @Override
    public void putAllAws(Map<String, Object> all) {
    }

    @Override
    public void putSql(String key, Object value) {
    }

    @Override
    public void putAllSql(Map<String, Object> all) {
    }

    @Override
    public void putAnnotation(String key, String value) {
    }

    @Override
    public void putAnnotation(String key, Number value) {
    }

    @Override
    public void putAnnotation(String key, Boolean value) {
    }

    @Override
    public void putMetadata(String key, Object object) {
    }

    @Override
    public void putMetadata(String namespace, String key, Object object) {
    }

    @Override
    public void removeSubsegment(Subsegment subsegment) {
    }

    @Override
    public boolean isEmitted() {
        return false;
    }

    @Override
    public void setEmitted(boolean emitted) {
    }

    @Override
    public boolean compareAndSetEmitted(boolean current, boolean next) {
        return false;
    }

    @Override
    public String serialize() {
        return "";
    }

    @Override
    public String prettySerialize() {
        return "";
    }

    @Override
    public void setParentSegment(Segment parentSegment) {
    }

    @Override
    public Set<String> getPrecursorIds() {
        return NoOpSet.get();
    }

    @Override
    public void setPrecursorIds(Set<String> precursorIds) {
    }

    @Override
    public void addPrecursorId(String precursorId) {
    }

    @Override
    public boolean shouldPropagate() {
        return this.shouldPropagate;
    }

    @Override
    public String streamSerialize() {
        return "";
    }

    @Override
    public String prettyStreamSerialize() {
        return "";
    }

    @Override
    public void close() {
    }
}
