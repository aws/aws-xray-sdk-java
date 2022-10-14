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
import com.amazonaws.xray.internal.SamplingStrategyOverride;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SubsegmentImpl extends EntityImpl implements Subsegment {
    private static final Log logger = LogFactory.getLog(SubsegmentImpl.class);

    @Nullable
    private String namespace;

    private Segment parentSegment;

    private Set<String> precursorIds;

    private boolean shouldPropagate;

    @JsonIgnore
    private boolean isSampled;

    @JsonIgnore
    private boolean isRecording;

    @JsonIgnore
    private SamplingStrategyOverride samplingStrategyOverride;

    @SuppressWarnings("nullness")
    private SubsegmentImpl() {
        super();
    } // default constructor for jackson

    public SubsegmentImpl(AWSXRayRecorder creator, String name, Segment parentSegment) {
        this(creator, name, parentSegment, SamplingStrategyOverride.DISABLED);
    }
    @Deprecated
    public SubsegmentImpl(AWSXRayRecorder creator,
                          String name,
                          Segment parentSegment,
                          SamplingStrategyOverride samplingStrategyOverride) {
        super(creator, name);
        this.parentSegment = parentSegment;
        parentSegment.incrementReferenceCount();
        this.precursorIds = new HashSet<>();
        this.shouldPropagate = true;

        this.isSampled = samplingStrategyOverride == SamplingStrategyOverride.DISABLED ?
                parentSegment.isSampled() :
                false;
        this.isRecording = isSampled;
        this.samplingStrategyOverride = samplingStrategyOverride;
    }

    @Override
    public boolean end() {
        if (logger.isDebugEnabled()) {
            logger.debug("Subsegment named '" + getName() + "' ending. Parent segment named '" + parentSegment.getName()
                         + "' has reference count " + parentSegment.getReferenceCount());
        }

        if (getEndTime() < Double.MIN_NORMAL) {
            setEndTime(System.currentTimeMillis() / 1000d);
        }
        setInProgress(false);
        boolean shouldEmit = parentSegment.decrementReferenceCount() && isSampled();
        if (shouldEmit) {
            checkAlreadyEmitted();
            setEmitted(true);
        }
        return shouldEmit;
    }

    @Override
    @Nullable
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(String namespace) {
        checkAlreadyEmitted();
        this.namespace = namespace;
    }

    @Override
    public Segment getParentSegment() {
        return parentSegment;
    }

    @Override
    public void setParentSegment(Segment parentSegment) {
        checkAlreadyEmitted();
        this.parentSegment = parentSegment;
    }

    @Override
    public void addPrecursorId(String precursorId) {
        checkAlreadyEmitted();
        this.precursorIds.add(precursorId);
    }

    @Override
    public Set<String> getPrecursorIds() {
        return precursorIds;
    }

    @Override
    public void setPrecursorIds(Set<String> precursorIds) {
        checkAlreadyEmitted();
        this.precursorIds = precursorIds;
    }

    @Override
    public boolean shouldPropagate() {
        return shouldPropagate;
    }

    private ObjectNode getStreamSerializeObjectNode() {
        ObjectNode obj = (ObjectNode) mapper.valueToTree(this);
        obj.put("type", "subsegment");
        obj.put("parent_id", getParent().getId());
        obj.put("trace_id", parentSegment.getTraceId().toString());
        return obj;
    }

    @Override
    public String streamSerialize() {
        String ret = "";
        try {
            ret = mapper.writeValueAsString(getStreamSerializeObjectNode());
        } catch (JsonProcessingException jpe) {
            logger.error("Exception while serializing entity.", jpe);
        }
        return ret;
    }

    @Override
    public String prettyStreamSerialize() {
        String ret = "";
        try {
            ret = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(getStreamSerializeObjectNode());
        } catch (JsonProcessingException jpe) {
            logger.error("Exception while serializing entity.", jpe);
        }
        return ret;
    }

    @Override
    public void close() {
        getCreator().endSubsegment();
    }

    @Override
    @JsonIgnore
    public boolean isSampled() {
        return isSampled;
    }

    @Override
    @JsonIgnore
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    @JsonIgnore
    public void setSampledFalse() {
        checkAlreadyEmitted();
        isSampled = false;
        isRecording = false;
    }

    @Override
    @Deprecated
    public SamplingStrategyOverride getSamplingStrategyOverride() {
        return samplingStrategyOverride;
    }
}
