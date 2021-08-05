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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SubsegmentImpl extends EntityImpl implements Subsegment {
    private static final Log logger = LogFactory.getLog(SubsegmentImpl.class);

    @Nullable
    @GuardedBy("lock")
    private String namespace;

    @GuardedBy("lock")
    private Segment parentSegment;

    @GuardedBy("lock")
    private Set<String> precursorIds;

    @GuardedBy("lock")
    private boolean shouldPropagate;

    @SuppressWarnings("nullness")
    private SubsegmentImpl() {
        super();
    } // default constructor for jackson

    public SubsegmentImpl(AWSXRayRecorder creator, String name, Segment parentSegment) {
        super(creator, name);
        this.parentSegment = parentSegment;
        parentSegment.incrementReferenceCount();
        this.precursorIds = new HashSet<>();
        this.shouldPropagate = true;
    }

    @Override
    public boolean end() {
        synchronized (lock) {
            if (logger.isDebugEnabled()) {
                logger.debug("Subsegment named '" + getName() + "' ending. Parent segment named '" + parentSegment.getName()
                             + "' has reference count " + parentSegment.getReferenceCount());
            }

            if (getEndTime() < Double.MIN_NORMAL) {
                setEndTime(System.currentTimeMillis() / 1000d);
            }
            setInProgress(false);
            boolean shouldEmit = parentSegment.decrementReferenceCount() && parentSegment.isSampled();
            if (shouldEmit) {
                checkAlreadyEmitted();
                ended = true;
            }
            return shouldEmit;
        }
    }

    @Override
    @Nullable
    public String getNamespace() {
        synchronized (lock) {
            return namespace;
        }
    }

    @Override
    public void setNamespace(String namespace) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.namespace = namespace;
        }
    }

    @Override
    public Segment getParentSegment() {
        synchronized (lock) {
            return parentSegment;
        }
    }

    @Override
    public void setParentSegment(Segment parentSegment) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.parentSegment = parentSegment;
        }
    }

    @Override
    public void addPrecursorId(String precursorId) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.precursorIds.add(precursorId);
        }
    }

    @Override
    public Set<String> getPrecursorIds() {
        synchronized (lock) {
            return precursorIds;
        }
    }

    @Override
    public void setPrecursorIds(Set<String> precursorIds) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.precursorIds = precursorIds;
        }
    }

    @Override
    public boolean shouldPropagate() {
        synchronized (lock) {
            return shouldPropagate;
        }
    }

    private ObjectNode getStreamSerializeObjectNode() {
        synchronized (lock) {
            ObjectNode obj = (ObjectNode) mapper.valueToTree(this);
            obj.put("type", "subsegment");
            obj.put("parent_id", getParent().getId());
            obj.put("trace_id", parentSegment.getTraceId().toString());
            return obj;
        }
    }

    @Override
    public String streamSerialize() {
        synchronized (lock) {
            String ret = "";
            try {
                ret = mapper.writeValueAsString(getStreamSerializeObjectNode());
            } catch (JsonProcessingException jpe) {
                logger.error("Exception while serializing entity.", jpe);
            }
            return ret;
        }
    }

    @Override
    public String prettyStreamSerialize() {
        synchronized (lock) {
            String ret = "";
            try {
                ret = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(getStreamSerializeObjectNode());
            } catch (JsonProcessingException jpe) {
                logger.error("Exception while serializing entity.", jpe);
            }
            return ret;
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            getCreator().endSubsegment();
        }
    }
}
