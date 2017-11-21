package com.amazonaws.xray.entities;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.AWSXRayRecorder;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class SegmentImpl extends EntityImpl implements Segment {
    private static final Log logger =
        LogFactory.getLog(SegmentImpl.class);


    protected String resourceArn;
    protected String user;
    protected String origin;

    protected Map<String, Object> service;

    @JsonIgnore
    private boolean sampled;

    @SuppressWarnings("unused")
    private SegmentImpl() {
        super();
    } // default constructor for jackson

    public SegmentImpl(AWSXRayRecorder creator, String name) {
        this(creator, name, new TraceID());
    }

    public SegmentImpl(AWSXRayRecorder creator, String name, TraceID traceId) {
        super(creator, name);
        setTraceId(traceId);

        this.service = new ConcurrentHashMap<>();

        this.sampled = true;
    }

    @Override
    public boolean end() {
        setEndTime(Instant.now().toEpochMilli() / 1000.0d);
        setInProgress(false);
        boolean shouldEmit = referenceCount.intValue() <= 0;
        if (shouldEmit) {
            checkAlreadyEmitted();
            setEmitted(true);
        }
        return shouldEmit;
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
    public Segment getParentSegment() {
        return this;
    }

    public void close() {
        getCreator().endSegment();
    }

}
