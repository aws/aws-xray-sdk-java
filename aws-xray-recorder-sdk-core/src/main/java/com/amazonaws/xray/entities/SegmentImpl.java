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
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SegmentImpl extends EntityImpl implements Segment {

    protected String resourceArn;
    protected String user;
    protected String origin;

    protected Map<String, Object> service;

    @JsonIgnore
    private boolean sampled;

    @SuppressWarnings({ "unused", "nullness" })
    private SegmentImpl() {
        super();
    } // default constructor for jackson

    // TODO(anuraaga): Refactor the entity relationship. There isn't a great reason to use a type hierarchy for data classes and
    // it makes the code to hard to reason about e.g., nullness.
    @SuppressWarnings("nullness")
    public SegmentImpl(AWSXRayRecorder creator, String name) {
        this(creator, name, TraceID.create(creator));
    }

    // TODO(anuraaga): Refactor the entity relationship. There isn't a great reason to use a type hierarchy for data classes and
    // it makes the code to hard to reason about e.g., nullness.
    @SuppressWarnings("nullness")
    public SegmentImpl(AWSXRayRecorder creator, String name, TraceID traceId) {
        super(creator, name);
        if (traceId == null) {
            traceId = TraceID.create(creator);
        }
        setTraceId(traceId);

        this.service = new ConcurrentHashMap<>();

        this.sampled = true;
    }

    @Override
    public boolean end() {
        if (getEndTime() < Double.MIN_NORMAL) {
            setEndTime(Instant.now().toEpochMilli() / 1000.0d);
        }

        setInProgress(false);
        boolean shouldEmit = referenceCount.intValue() <= 0;
        if (shouldEmit) {
            checkAlreadyEmitted();
            setEmitted(true);
        }
        return shouldEmit;
    }

    @Override
    public boolean isRecording() {
        return true;
    }

    @Override
    public boolean isSampled() {
        return sampled;
    }

    @Override
    public void setSampled(boolean sampled) {
        checkAlreadyEmitted();
        this.sampled = sampled;
    }

    @Override
    public String getResourceArn() {
        return resourceArn;
    }

    @Override
    public void setResourceArn(String resourceArn) {
        checkAlreadyEmitted();
        this.resourceArn = resourceArn;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setUser(String user) {
        checkAlreadyEmitted();
        this.user = user;
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(String origin) {
        checkAlreadyEmitted();
        this.origin = origin;
    }

    @Override
    public Map<String, Object> getService() {
        return service;
    }

    @Override
    public void setService(Map<String, Object> service) {
        checkAlreadyEmitted();
        this.service = service;
    }

    @Override
    public void putService(String key, Object object) {
        checkAlreadyEmitted();
        service.put(key, object);
    }

    @Override
    public void putAllService(Map<String, Object> all) {
        checkAlreadyEmitted();
        service.putAll(all);
    }

    @Override
    public void setRuleName(String ruleName) {
        checkAlreadyEmitted();
        if (getAws().get("xray") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> a = (Map<String, Object>) getAws().get("xray");
            HashMap<String, Object> referA = new HashMap<>();
            if (a != null) {
                referA.putAll(a);
            }
            referA.put("rule_name", ruleName);
            this.putAws("xray", referA);
        }
    }

    @Override
    public Segment getParentSegment() {
        return this;
    }

    @Override
    public void close() {
        getCreator().endSegment();
    }

}
