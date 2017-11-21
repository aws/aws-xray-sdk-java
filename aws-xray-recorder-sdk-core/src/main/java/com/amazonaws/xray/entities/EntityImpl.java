package com.amazonaws.xray.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.exceptions.AlreadyEmittedException;
import com.amazonaws.xray.serializers.CauseSerializer;
import com.amazonaws.xray.serializers.StackTraceElementSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * The base class from which {@code Segment} and {@code Subsegment} extend.
 *
 */
public abstract class EntityImpl implements Entity {
    private static final Log logger = LogFactory.getLog(EntityImpl.class);

    private static final String DEFAULT_METADATA_NAMESPACE = "default";

    private String name;
    private String id;
    private String parentId;
    private double startTime;

    @JsonInclude(Include.NON_DEFAULT)
    @JsonSerialize(using = ToStringSerializer.class)
    private TraceID traceId;

    @JsonInclude(Include.NON_DEFAULT)
    private double endTime;

    @JsonInclude(Include.NON_DEFAULT)
    private boolean fault;
    @JsonInclude(Include.NON_DEFAULT)
    private boolean error;
    @JsonInclude(Include.NON_DEFAULT)
    private boolean throttle;
    @JsonInclude(Include.NON_DEFAULT)
    private boolean inProgress;

    private String namespace;

    private List<Subsegment> subsegments;

    private Cause cause;
    private Map<String, Object> http;
    private Map<String, Object> aws;
    private Map<String, Object> sql;

    private Map<String, Map<String, Object>> metadata;
    private Map<String, Object> annotations;

    @JsonIgnore
    private Entity parent;
    @JsonIgnore
    private AWSXRayRecorder creator;
    @JsonIgnore
    private ReentrantLock subsegmentsLock;

    /*
     * Reference counter to track how many subsegments are in progress on this entity. Starts with a value of 0.
     */
    @JsonIgnore
    protected LongAdder referenceCount;

    @JsonIgnore
    protected LongAdder totalSize;

    @JsonIgnore
    private boolean emitted = false;

    protected static final ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES).setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    static {
        /*
         * Inject the CauseSerializer and StackTraceElementSerializer classes into the local mapper such that they will serialize their respective object types.
         */
        mapper.registerModule(new SimpleModule() {
            private static final long serialVersionUID = 545800949242254918L;

            @Override
            public void setupModule(SetupContext setupContext) {
                super.setupModule(setupContext);
                setupContext.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public JsonSerializer<?> modifySerializer(SerializationConfig serializationConfig, BeanDescription beanDescription, JsonSerializer<?> jsonSerializer) {
                        Class<?> beanClass = beanDescription.getBeanClass();
                        if (Cause.class.isAssignableFrom(beanClass)) {
                            return new CauseSerializer((JsonSerializer<Object>) jsonSerializer);
                        } else if (StackTraceElement.class.isAssignableFrom(beanClass)) {
                            return new StackTraceElementSerializer();
                        }
                        return jsonSerializer;
                    }
                });
            }
        });
    }

    protected EntityImpl() { } // default constructor for Jackson, so it can understand the default values to compare when using the Include.NON_DEFAULT annotation.

    protected EntityImpl(AWSXRayRecorder creator, String name) {
        StringValidator.throwIfNullOrBlank(name, "(Sub)segment name cannot be null or blank.");
        validateNotNull(creator);

        this.creator = creator;
        this.name = name;
        this.subsegments = new ArrayList<>();
        this.subsegmentsLock = new ReentrantLock();
        this.cause = new Cause();
        this.http = new ConcurrentHashMap<>();
        this.aws = new ConcurrentHashMap<>();
        this.sql = new ConcurrentHashMap<>();
        this.annotations = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
        this.startTime = Instant.now().toEpochMilli() / 1000.0d;
        this.id = Entity.generateId();
        this.inProgress = true;
        this.referenceCount = new LongAdder();
        this.totalSize = new LongAdder();
    }

    /**
     * Checks if the entity has already been emitted to the X-Ray daemon.
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    protected void checkAlreadyEmitted() {
        if (emitted) {
            getCreator().getContextMissingStrategy().contextMissing("Segment " + getName() + " has already been emitted.", AlreadyEmittedException.class);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        checkAlreadyEmitted();
        this.id = id;
    }

    @Override
    public double getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(double startTime) {
        checkAlreadyEmitted();
        this.startTime = startTime;
    }

    @Override
    public double getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(double endTime) {
        checkAlreadyEmitted();
        this.endTime = endTime;
    }

    @Override
    public boolean isFault() {
        return fault;
    }

    @Override
    public void setFault(boolean fault) {
        checkAlreadyEmitted();
        this.fault = fault;
    }

    @Override
    public boolean isError() {
        return error;
    }

    @Override
    public void setError(boolean error) {
        checkAlreadyEmitted();
        this.error = error;
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
    public ReentrantLock getSubsegmentsLock() {
        return subsegmentsLock;
    }

    @Override
    public void setSubsegmentsLock(ReentrantLock subsegmentsLock) {
        checkAlreadyEmitted();
        this.subsegmentsLock = subsegmentsLock;
    }

    @Override
    public Cause getCause() {
        return cause;
    }

    @Override
    public Map<String, Object> getHttp() {
        return http;
    }

    @Override
    public void setHttp(Map<String, Object> http) {
        checkAlreadyEmitted();
        this.http = http;
    }

    @Override
    public Map<String, Object> getAws() {
        return aws;
    }

    @Override
    public void setAws(Map<String, Object> aws) {
        checkAlreadyEmitted();
        this.aws = aws;
    }

    @Override
    public Map<String, Object> getSql() {
        return sql;
    }

    @Override
    public void setSql(Map<String, Object> sql) {
        checkAlreadyEmitted();
        this.sql = sql;
    }

    @Override
    public Map<String, Map<String, Object>> getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(Map<String, Map<String, Object>> metadata) {
        checkAlreadyEmitted();
        this.metadata = metadata;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(Map<String, Object> annotations) {
        checkAlreadyEmitted();
        this.annotations = annotations;
    }

    @Override
    public Entity getParent() {
        return parent;
    }

    @Override
    public void setParent(Entity parent) {
        checkAlreadyEmitted();
        this.parent = parent;
    }

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
        checkAlreadyEmitted();
        this.creator = creator;
    }

    @Override
    public boolean isThrottle() {
        return throttle;
    }

    @Override
    public void setThrottle(boolean throttle) {
        checkAlreadyEmitted();
        if(throttle) {
            this.fault = false;
            this.error = true;
        }
        this.throttle = throttle;
    }

    @Override
    public boolean isInProgress() {
        return inProgress;
    }

    @Override
    public void setInProgress(boolean inProgress) {
        checkAlreadyEmitted();
        this.inProgress = inProgress;
    }

    @Override
    public TraceID getTraceId() {
        return traceId;
    }

    @Override
    public void setTraceId(TraceID traceId) {
        checkAlreadyEmitted();
        this.traceId = traceId;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        checkAlreadyEmitted();
        this.parentId = parentId;
    }


    @JsonIgnore
    public abstract Segment getParentSegment();

    @Override
    public List<Subsegment> getSubsegments() {
        return subsegments;
    }

    @Override
    public void addSubsegment(Subsegment subsegment) {
        checkAlreadyEmitted();
        synchronized(subsegments) {
            subsegments.add(subsegment);
        }
    }

    @Override
    public void addException(Throwable exception) {
        checkAlreadyEmitted();
        setFault(true);
        synchronized (subsegments) {
            cause.addExceptions(creator.getThrowableSerializationStrategy().describeInContext(exception, subsegments));
        }
    }

    @Override
    public void putHttp(String key, Object value) {
        checkAlreadyEmitted();
        validateNotNull(key);
        validateNotNull(value);
        http.put(key, value);
    }

    @Override
    public void putAllHttp(Map<String, Object> all) {
        checkAlreadyEmitted();
        validateNotNull(all);
        http.putAll(all);
    }

    @Override
    public void putAws(String key, Object value) {
        checkAlreadyEmitted();
        validateNotNull(key);
        validateNotNull(value);
        aws.put(key, value);
    }

    @Override
    public void putAllAws(Map<String, Object> all) {
        checkAlreadyEmitted();
        validateNotNull(all);
        aws.putAll(all);
    }

    @Override
    public void putSql(String key, Object value) {
        checkAlreadyEmitted();
        validateNotNull(key);
        validateNotNull(value);
        sql.put(key, value);
    }

    @Override
    public void putAllSql(Map<String, Object> all) {
        checkAlreadyEmitted();
        validateNotNull(all);
        sql.putAll(all);
    }

    @Override
    public void putAnnotation(String key, String value) {
        checkAlreadyEmitted();
        validateNotNull(key);
        validateNotNull(value);
        annotations.put(key, value);
    }

    @Override
    public void putAnnotation(String key, Number value) {
        checkAlreadyEmitted();
        validateNotNull(key);
        validateNotNull(value);
        annotations.put(key, value);
    }

    @Override
    public void putAnnotation(String key, Boolean value) {
        checkAlreadyEmitted();
        validateNotNull(key);
        validateNotNull(value);
        annotations.put(key, value);
    }

    @Override
    public void putMetadata(String key, Object object) {
        checkAlreadyEmitted();
        putMetadata(DEFAULT_METADATA_NAMESPACE, key, object);
    }

    @Override
    public void putMetadata(String namespace, String key, Object object) {
        checkAlreadyEmitted();
        validateNotNull(namespace);
        validateNotNull(key);
        if (null == object) {
            object = NullNode.instance;
        }
        metadata.computeIfAbsent(namespace, (n) -> {
            return new ConcurrentHashMap<String, Object>();
        }).put(key, object);
    }

    @Override
    public void incrementReferenceCount() {
        checkAlreadyEmitted();
        referenceCount.increment();
        totalSize.increment();
    }

    @Override
    public boolean decrementReferenceCount() {
        checkAlreadyEmitted();
        referenceCount.decrement();
        return !isInProgress() && referenceCount.intValue() <= 0;
    }

    /**
     * Returns the reference count of the segment. This number represents how many open subsegments are children of this segment. The segment is emitted when its reference count reaches 0.
     *
     * @return the reference count
     */
    @Override
    public int getReferenceCount() {
        return referenceCount.intValue();
    }

    /**
     * @return the totalSize
     */
    @Override
    public LongAdder getTotalSize() {
        return totalSize;
    }

    /**
     * @return the emitted
     */
    @Override
    public boolean isEmitted() {
        return emitted;
    }

    /**
     * @param emitted
     *            the emitted to set
     */
    @Override
    public void setEmitted(boolean emitted) {
        checkAlreadyEmitted();
        this.emitted = emitted;
    }

    @Override
    public String serialize() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException jpe) {
            logger.error("Exception while serializing entity.", jpe);
        }
        return "";
    }

    @Override
    public String prettySerialize() {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException jpe) {
            logger.error("Exception while serializing segment.", jpe);
        }
        return "";
    }

    @Override
    public void removeSubsegment(Subsegment subsegment) {
        synchronized(subsegments) {
            getSubsegments().remove(subsegment);
        }
        getParentSegment().getTotalSize().decrement();
    }


    public static void validateNotNull(Object object) {
        if (null == object) {
            throw new NullPointerException();
        }
    }

}
