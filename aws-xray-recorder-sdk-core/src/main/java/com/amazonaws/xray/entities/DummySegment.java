package com.amazonaws.xray.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

import com.amazonaws.xray.AWSXRayRecorder;

public class DummySegment implements Segment {

    private Cause cause = new Cause();
    private Map<String, Object> map = new ConcurrentHashMap<>();
    private Map<String, Map<String, Object>> metadataMap = new ConcurrentHashMap<>();
    private List<Subsegment> list = new ArrayList<>();
    private LongAdder longAdder = new LongAdder();
    private ReentrantLock lock = new ReentrantLock();

    private AWSXRayRecorder creator;
    private TraceID traceId;

    public DummySegment(AWSXRayRecorder creator) {
        this(creator, new TraceID());
    }

    public DummySegment(AWSXRayRecorder creator, TraceID traceId) {
        this.creator = creator;
        this.traceId = traceId;
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
    public void setId(String id) { }

    @Override
    public double getStartTime() {
        return 0;
    }

    @Override
    public void setStartTime(double startTime) { }

    @Override
    public double getEndTime() {
        return 0;
    }

    @Override
    public void setEndTime(double endTime) { }

    @Override
    public boolean isFault() {
        return false;
    }

    @Override
    public void setFault(boolean fault) { }

    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public void setError(boolean error) { }

    @Override
    public String getNamespace() {
        return "";
    }

    @Override
    public void setNamespace(String namespace) { }

    @Override
    public Cause getCause() {
        return cause;
    }

    @Override
    public Map<String, Object> getHttp() {
        return map;
    }

    @Override
    public void setHttp(Map<String, Object> http) { }

    @Override
    public Map<String, Object> getAws() {
        return map;
    }

    @Override
    public void setAws(Map<String, Object> aws) { }

    @Override
    public Map<String, Object> getSql() {
        return map;
    }

    @Override
    public void setSql(Map<String, Object> sql) { }

    @Override
    public Map<String, Map<String, Object>> getMetadata() {
            return metadataMap;
    }

    @Override
    public void setMetadata(Map<String, Map<String, Object>> metadata) { }

    @Override
    public void setAnnotations(Map<String, Object> annotations) { }

    @Override
    public Entity getParent() {
        return this;
    }

    @Override
    public void setParent(Entity parent) { }

    @Override
    public boolean isThrottle() {
        return false;
    }

    @Override
    public void setThrottle(boolean throttle) { }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public void setInProgress(boolean inProgress) { }

    @Override
    public TraceID getTraceId() {
        return traceId;
    }

    @Override
    public void setTraceId(TraceID traceId) { }

    /**
     * @return the creator
     */
    public AWSXRayRecorder getCreator() {
        return creator;
    }

    /**
     * @param creator the creator to set
     */
    public void setCreator(AWSXRayRecorder creator) {
        this.creator = creator;
    }

    @Override
    public String getParentId() {
        return "";
    }

    @Override
    public void setParentId(String parentId) { }

    @Override
    public List<Subsegment> getSubsegments() {
        return list;
    }

    @Override
    public void addSubsegment(Subsegment subsegment) { }

    @Override
    public void addException(Throwable exception) { }

    @Override
    public void putHttp(String key, Object value) { }

    @Override
    public void putAllHttp(Map<String, Object> all) { }

    @Override
    public void putAws(String key, Object value) { }

    @Override
    public void putAllAws(Map<String, Object> all) { }

    @Override
    public void putSql(String key, Object value) { }

    @Override
    public void putAllSql(Map<String, Object> all) { }

    @Override
    public void putAnnotation(String key, String value) { }

    @Override
    public void putAnnotation(String key, Number value) { }

    @Override
    public void putAnnotation(String key, Boolean value) { }

    @Override
    public void putMetadata(String key, Object object) { }

    @Override
    public void putMetadata(String namespace, String key, Object object) { }

    @Override
    public boolean isEmitted() {
        return false;
    }

    @Override
    public void setEmitted(boolean emitted) { }

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
    public void putService(String key, Object object) { }

    @Override
    public boolean isSampled() {
        return false;
    }

    @Override
    public void setSampled(boolean sampled) { }

    @Override
    public int getReferenceCount() {
        return 0;
    }

    @Override
    public LongAdder getTotalSize() {
        return longAdder;
    }

    @Override
    public void incrementReferenceCount() { }

    @Override
    public boolean decrementReferenceCount() {
        return false;
    }

    @Override
    public String getResourceArn() {
        return "";
    }

    @Override
    public void setResourceArn(String resourceArn) { }

    @Override
    public String getUser() {
        return "";
    }

    @Override
    public void setUser(String user) { }

    @Override
    public String getOrigin() {
        return "";
    }

    @Override
    public void setOrigin(String origin) { }

    @Override
    public Map<String, Object> getService() {
        return map;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return map;
    }

    @Override
    public Segment getParentSegment() {
        return this;
    }

    public void close() { }

    @Override
    public ReentrantLock getSubsegmentsLock() {
        return lock;
    }

    @Override
    public void setSubsegmentsLock(ReentrantLock subsegmentsLock) { }

    @Override
    public void putAllService(Map<String, Object> all) { }

    @Override
    public void setService(Map<String, Object> service) { }

    @Override
    public void removeSubsegment(Subsegment subsegment) { }

    @Override
    public void setRuleName(String name) {}

}
