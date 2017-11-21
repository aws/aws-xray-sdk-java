package com.amazonaws.xray.entities;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.AWSXRayRecorder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SubsegmentImpl extends EntityImpl implements Subsegment {
    private static final Log logger = LogFactory.getLog(SubsegmentImpl.class);

    private String namespace;

    private Segment parentSegment;

    private Set<String> precursorIds;

    private SubsegmentImpl() {
        super();
    } // default constructor for jackson

    public SubsegmentImpl(AWSXRayRecorder creator, String name, Segment parentSegment) {
        super(creator, name);
        this.parentSegment = parentSegment;
        parentSegment.incrementReferenceCount();
        this.precursorIds = new HashSet<>();
    }

    @Override
    public boolean end() {
        if (logger.isDebugEnabled()) {
            logger.debug("Subsegment named '" + getName() + "' ending. Parent segment named '" + parentSegment.getName() + "' has reference count " + parentSegment.getReferenceCount());
        }
        setEndTime(Instant.now().toEpochMilli() / 1000.0d);
        setInProgress(false);
        boolean shouldEmit = parentSegment.decrementReferenceCount() && parentSegment.isSampled();
        if (shouldEmit) {
            checkAlreadyEmitted();
            setEmitted(true);
        }
        return shouldEmit;
    }

    @Override
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

    public void close() {
        getCreator().endSubsegment();
    }
}
