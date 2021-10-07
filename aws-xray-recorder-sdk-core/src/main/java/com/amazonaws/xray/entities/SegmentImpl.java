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
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.HashMap;
import java.util.Map;

public class SegmentImpl extends EntityImpl implements Segment {

    @GuardedBy("lock")
    protected String resourceArn;
    @GuardedBy("lock")
    protected String user;
    @GuardedBy("lock")
    protected String origin;

    @GuardedBy("lock")
    protected Map<String, Object> service;

    @JsonIgnore
    private volatile boolean sampled;

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

        this.service = new HashMap<>();

        this.sampled = true;
    }

    @Override
    public boolean end() {
        synchronized (lock) {
            if (getEndTime() < Double.MIN_NORMAL) {
                setEndTime(System.currentTimeMillis() / 1000d);
            }

            setInProgress(false);
            boolean shouldEmit = referenceCount <= 0;
            if (shouldEmit) {
                checkAlreadyEmitted();
                ended = true;
            }
            return shouldEmit;
        }
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
        synchronized (lock) {
            checkAlreadyEmitted();
        }
        this.sampled = sampled;
    }

    @Override
    public String getResourceArn() {
        synchronized (lock) {
            return resourceArn;
        }
    }

    @Override
    public void setResourceArn(String resourceArn) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.resourceArn = resourceArn;
        }
    }

    @Override
    public String getUser() {
        synchronized (lock) {
            return user;
        }
    }

    @Override
    public void setUser(String user) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.user = user;
        }
    }

    @Override
    public String getOrigin() {
        synchronized (lock) {
            return origin;
        }
    }

    @Override
    public void setOrigin(String origin) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.origin = origin;
        }
    }

    @Override
    public Map<String, Object> getService() {
        synchronized (lock) {
            return service;
        }
    }

    @Override
    public void setService(Map<String, Object> service) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.service = service;
        }
    }

    @Override
    public void putService(String key, Object object) {
        synchronized (lock) {
            checkAlreadyEmitted();
            service.put(key, object);
        }
    }

    @Override
    public void putAllService(Map<String, Object> all) {
        synchronized (lock) {
            checkAlreadyEmitted();
            service.putAll(all);
        }
    }

    @Override
    public void setRuleName(String ruleName) {
        synchronized (lock) {
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
    }

    @Override
    public Segment getParentSegment() {
        return this;
    }

    @Override
    public void close() {
        synchronized (lock) {
            getCreator().endSegment();
        }
    }

}
