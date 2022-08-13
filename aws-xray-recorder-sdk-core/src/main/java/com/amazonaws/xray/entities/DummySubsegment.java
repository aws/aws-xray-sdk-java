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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @deprecated Use {@link Subsegment#noOp(AWSXRayRecorder, boolean)}.
 */
@Deprecated
public class DummySubsegment implements Subsegment {

    private Cause cause = new Cause();
    private Map<String, Object> map = new ConcurrentHashMap<>();
    private Map<String, Map<String, Object>> metadataMap = new ConcurrentHashMap<>();
    private List<Subsegment> list = new ArrayList<>();
    private Set<String> set = new HashSet<>();
    private ReentrantLock lock = new ReentrantLock();

    private AWSXRayRecorder creator;
    private TraceID traceId;
    private Segment parentSegment;

    public DummySubsegment(AWSXRayRecorder creator) {
        this(creator, TraceID.create(creator));
    }

    public DummySubsegment(AWSXRayRecorder creator, TraceID traceId) {
        this.creator = creator;
        this.traceId = traceId;
        this.parentSegment = new DummySegment(creator);
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
    public Cause getCause() {
        return cause;
    }

    @Override
    public Map<String, Object> getHttp() {
        return map;
    }

    @Override
    public void setHttp(Map<String, Object> http) {
    }

    @Override
    public Map<String, Object> getAws() {
        return map;
    }

    @Override
    public void setAws(Map<String, Object> aws) {
    }

    @Override
    public Map<String, Object> getSql() {
        return map;
    }

    @Override
    public void setSql(Map<String, Object> sql) {
    }

    @Override
    public Map<String, Map<String, Object>> getMetadata() {
        return metadataMap;
    }

    @Override
    public void setMetadata(Map<String, Map<String, Object>> metadata) {
    }

    @Override
    public void setAnnotations(Map<String, Object> annotations) {
    }

    @Override
    public Entity getParent() {
        return this;
    }

    @Override
    public void setParent(Entity parent) {
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
        return traceId;
    }

    @Override
    public void setTraceId(TraceID traceId) {
    }

    /**
     * @return the creator
     */
    @Override
    public AWSXRayRecorder getCreator() {
        return creator;
    }

    /**
     * @param creator the creator to set
     */
    @Override
    public void setCreator(AWSXRayRecorder creator) {
        this.creator = creator;
    }

    @Override
    public String getParentId() {
        return "";
    }

    @Override
    public void setParentId(@Nullable String parentId) {
    }

    @Override
    public List<Subsegment> getSubsegments() {
        return list;
    }

    @Override
    public List<Subsegment> getSubsegmentsCopy() {
        return new ArrayList<>(list);
    }

    @Override
    public void addSubsegment(Subsegment subsegment) {
    }

    @Override
    public void addException(Throwable exception) {
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
    public boolean end() {
        return false;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return map;
    }

    @Override
    public Segment getParentSegment() {
        return parentSegment;
    }

    @Override
    public void close() {
    }

    @Override
    public void setParentSegment(Segment parentSegment) {
    }

    @Override
    public Set<String> getPrecursorIds() {
        return set;
    }

    @Override
    public void setPrecursorIds(Set<String> precursorIds) {
    }

    @Override
    public void addPrecursorId(String precursorId) {
    }

    @Override
    public boolean shouldPropagate() {
        return false;
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
    public ReentrantLock getSubsegmentsLock() {
        return lock;
    }

    @Override
    public void setSubsegmentsLock(ReentrantLock subsegmentsLock) {
    }

    @Override
    public int getReferenceCount() {
        return 0;
    }

    @Override
    public LongAdder getTotalSize() {
        return parentSegment.getTotalSize();
    }

    @Override
    public void incrementReferenceCount() {
    }

    @Override
    public boolean decrementReferenceCount() {
        return false;
    }

    @Override
    public void removeSubsegment(Subsegment subsegment) {
    }

}
