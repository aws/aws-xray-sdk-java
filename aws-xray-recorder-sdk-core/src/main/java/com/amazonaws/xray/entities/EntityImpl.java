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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The base class from which {@code Segment} and {@code Subsegment} extend.
 *
 */
public abstract class EntityImpl implements Entity {

    /**
     * @deprecated For internal use only.
     */
    @SuppressWarnings("checkstyle:ConstantName")
    @Deprecated
    protected static final ObjectMapper mapper = new ObjectMapper()
        .findAndRegisterModules()
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private static final Log logger = LogFactory.getLog(EntityImpl.class);

    private static final String DEFAULT_METADATA_NAMESPACE = "default";

    protected final Object lock = new Object();
    private final String name;

    /*
     * Reference counter to track how many subsegments are in progress on this entity. Starts with a value of 0.
     */
    @JsonIgnore
    protected int referenceCount;

    @JsonIgnore
    protected LongAdder totalSize;

    @GuardedBy("lock")
    private String id;
    @GuardedBy("lock")
    @Nullable
    private String parentId;
    @GuardedBy("lock")
    private double startTime;

    @JsonInclude(Include.NON_DEFAULT)
    @JsonSerialize(using = ToStringSerializer.class)
    @GuardedBy("lock")
    private TraceID traceId;

    @JsonInclude(Include.NON_DEFAULT)
    @GuardedBy("lock")
    private double endTime;

    @JsonInclude(Include.NON_DEFAULT)
    @GuardedBy("lock")
    private boolean fault;
    @JsonInclude(Include.NON_DEFAULT)
    @GuardedBy("lock")
    private boolean error;
    @JsonInclude(Include.NON_DEFAULT)
    @GuardedBy("lock")
    private boolean throttle;
    @JsonInclude(Include.NON_DEFAULT)
    @GuardedBy("lock")
    private boolean inProgress;

    @Nullable
    @GuardedBy("lock")
    private String namespace;

    // TODO(anuraaga): Check final for other variables, for now this is most important since it's also a lock.
    private final List<Subsegment> subsegments;

    @GuardedBy("lock")
    private Cause cause;
    @GuardedBy("lock")
    private Map<String, Object> http;
    @GuardedBy("lock")
    private Map<String, Object> aws;
    @GuardedBy("lock")
    private Map<String, Object> sql;

    @GuardedBy("lock")
    private Map<String, Map<String, Object>> metadata;
    @GuardedBy("lock")
    private Map<String, Object> annotations;

    @JsonIgnore
    @GuardedBy("lock")
    private Entity parent;
    @JsonIgnore
    @GuardedBy("lock")
    private AWSXRayRecorder creator;
    @JsonIgnore
    @GuardedBy("lock")
    private ReentrantLock subsegmentsLock;

    @JsonIgnore
    @GuardedBy("lock")
    private boolean emitted = false;

    @JsonIgnore
    @GuardedBy("lock")
    protected boolean ended = false;

    static {
        /*
         * Inject the CauseSerializer and StackTraceElementSerializer classes into the local mapper such that they will serialize
         * their respective object types.
         */
        mapper.registerModule(new SimpleModule() {
            private static final long serialVersionUID = 545800949242254918L;

            @Override
            public void setupModule(SetupContext setupContext) {
                super.setupModule(setupContext);
                setupContext.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public JsonSerializer<?> modifySerializer(
                        SerializationConfig serializationConfig,
                        BeanDescription beanDescription,
                        JsonSerializer<?> jsonSerializer) {
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

    // default constructor for Jackson, so it can understand the default values to compare when using the Include.NON_DEFAULT
    // annotation.
    @SuppressWarnings("nullness")
    protected EntityImpl() {
        // TODO(anuraaga): Check this is working as intended, empty lists are currently serialized.
        subsegments = null;
        name = null;
    }

    // TODO(anuraaga): Refactor the entity relationship. There isn't a great reason to use a type hierarchy for data classes and
    // it makes the code to hard to reason about e.g., nullness.
    @SuppressWarnings("nullness")
    protected EntityImpl(AWSXRayRecorder creator, String name) {
        StringValidator.throwIfNullOrBlank(name, "(Sub)segment name cannot be null or blank.");
        validateNotNull(creator);

        this.creator = creator;
        this.name = name;
        this.subsegments = Collections.synchronizedList(new ArrayList<>());
        this.subsegmentsLock = new ReentrantLock();
        this.cause = new Cause();
        this.http = new HashMap<>();
        this.aws = new HashMap<>();
        this.sql = new HashMap<>();
        this.annotations = new HashMap<>();
        this.metadata = new HashMap<>();
        this.startTime = System.currentTimeMillis() / 1000d;
        this.id = creator.getIdGenerator().newEntityId();
        this.inProgress = true;
        this.referenceCount = 0;
        this.totalSize = new LongAdder();
    }

    /**
     * Checks if the entity has already been ended.
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the
     *             AWSXRayRecorder used to create this entity is configured to throw exceptions
     *
     */
    protected void checkAlreadyEmitted() {
        synchronized (lock) {
            if (ended || emitted) {
                getCreator().getContextMissingStrategy().contextMissing("Segment " + getName() + " has already been emitted.",
                                                                        AlreadyEmittedException.class);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        synchronized (lock) {
            return id;
        }
    }

    @Override
    public void setId(String id) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.id = id;
        }
    }

    @Override
    public double getStartTime() {
        synchronized (lock) {
            return startTime;
        }
    }

    @Override
    public void setStartTime(double startTime) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.startTime = startTime;
        }
    }

    @Override
    public double getEndTime() {
        synchronized (lock) {
            return endTime;
        }
    }

    @Override
    public void setEndTime(double endTime) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.endTime = endTime;
        }
    }

    @Override
    public boolean isFault() {
        synchronized (lock) {
            return fault;
        }
    }

    @Override
    public void setFault(boolean fault) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.fault = fault;
        }
    }

    @Override
    public boolean isError() {
        synchronized (lock) {
            return error;
        }
    }

    @Override
    public void setError(boolean error) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.error = error;
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
    public ReentrantLock getSubsegmentsLock() {
        synchronized (lock) {
            return subsegmentsLock;
        }
    }

    @Override
    public void setSubsegmentsLock(ReentrantLock subsegmentsLock) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.subsegmentsLock = subsegmentsLock;
        }
    }

    @Override
    public Cause getCause() {
        synchronized (lock) {
            return cause;
        }
    }

    @Override
    public Map<String, Object> getHttp() {
        synchronized (lock) {
            return http;
        }
    }

    @Override
    public void setHttp(Map<String, Object> http) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.http = http;
        }
    }

    @Override
    public Map<String, Object> getAws() {
        synchronized (lock) {
            return aws;
        }
    }

    @Override
    public void setAws(Map<String, Object> aws) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.aws = aws;
        }
    }

    @Override
    public Map<String, Object> getSql() {
        synchronized (lock) {
            return sql;
        }
    }

    @Override
    public void setSql(Map<String, Object> sql) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.sql = sql;
        }
    }

    @Override
    public Map<String, Map<String, Object>> getMetadata() {
        synchronized (lock) {
            return metadata;
        }
    }

    @Override
    public void setMetadata(Map<String, Map<String, Object>> metadata) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.metadata = metadata;
        }
    }

    @Override
    public Map<String, Object> getAnnotations() {
        synchronized (lock) {
            return annotations;
        }
    }

    @Override
    public void setAnnotations(Map<String, Object> annotations) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.annotations = annotations;
        }
    }

    @Override
    public Entity getParent() {
        synchronized (lock) {
            return parent;
        }
    }

    @Override
    public void setParent(Entity parent) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.parent = parent;
        }
    }

    /**
     * @return the creator
     */
    @Override
    public AWSXRayRecorder getCreator() {
        synchronized (lock) {
            return creator;
        }
    }

    /**
     * @param creator the creator to set
     */
    @Override
    public void setCreator(AWSXRayRecorder creator) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.creator = creator;
        }
    }

    @Override
    public boolean isThrottle() {
        synchronized (lock) {
            return throttle;
        }
    }

    @Override
    public void setThrottle(boolean throttle) {
        synchronized (lock) {
            checkAlreadyEmitted();
            if (throttle) {
                this.fault = false;
                this.error = true;
            }
            this.throttle = throttle;
        }
    }

    @Override
    public boolean isInProgress() {
        synchronized (lock) {
            return inProgress;
        }
    }

    @Override
    public void setInProgress(boolean inProgress) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.inProgress = inProgress;
        }
    }

    @Override
    public TraceID getTraceId() {
        synchronized (lock) {
            return traceId;
        }
    }

    @Override
    @EnsuresNonNull("this.traceId")
    public void setTraceId(TraceID traceId) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.traceId = traceId;
        }
    }

    @Override
    @Nullable
    public String getParentId() {
        synchronized (lock) {
            return parentId;
        }
    }

    @Override
    public void setParentId(@Nullable String parentId) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.parentId = parentId;
        }
    }


    @JsonIgnore
    @Override
    public abstract Segment getParentSegment();

    @Override
    public List<Subsegment> getSubsegments() {
        return subsegments;
    }

    @JsonIgnore
    @Override
    public List<Subsegment> getSubsegmentsCopy() {
        return new ArrayList<>(subsegments);
    }

    @Override
    public void addSubsegment(Subsegment subsegment) {
        synchronized (lock) {
            checkAlreadyEmitted();
        }
        subsegments.add(subsegment);
    }

    @Override
    public void addException(Throwable exception) {
        synchronized (lock) {
            checkAlreadyEmitted();
            setFault(true);
            cause.addExceptions(creator.getThrowableSerializationStrategy()
                                       .describeInContext(this, exception, subsegments));
        }
    }

    @Override
    public void putHttp(String key, Object value) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(key);
            validateNotNull(value);
            http.put(key, value);
        }
    }

    @Override
    public void putAllHttp(Map<String, Object> all) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(all);
            http.putAll(all);
        }
    }

    @Override
    public void putAws(String key, Object value) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(key);
            validateNotNull(value);
            aws.put(key, value);
        }
    }

    @Override
    public void putAllAws(Map<String, Object> all) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(all);
            aws.putAll(all);
        }
    }

    @Override
    public void putSql(String key, Object value) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(key);
            validateNotNull(value);
            sql.put(key, value);
        }
    }

    @Override
    public void putAllSql(Map<String, Object> all) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(all);
            sql.putAll(all);
        }
    }

    @Override
    public void putAnnotation(String key, String value) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(key);
            validateNotNull(value);
            annotations.put(key, value);
        }
    }

    @Override
    public void putAnnotation(String key, Number value) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(key);
            validateNotNull(value);
            annotations.put(key, value);
        }
    }

    @Override
    public void putAnnotation(String key, Boolean value) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(key);
            validateNotNull(value);
            annotations.put(key, value);
        }
    }

    @Override
    public void putMetadata(String key, Object object) {
        synchronized (lock) {
            checkAlreadyEmitted();
            putMetadata(DEFAULT_METADATA_NAMESPACE, key, object);
        }
    }

    @Override
    public void putMetadata(String namespace, String key, Object object) {
        synchronized (lock) {
            checkAlreadyEmitted();
            validateNotNull(namespace);
            validateNotNull(key);
            if (null == object) {
                object = NullNode.instance;
            }
            metadata.computeIfAbsent(namespace, unused -> new HashMap<>()).put(key, object);
        }
    }

    @Override
    public void incrementReferenceCount() {
        synchronized (lock) {
            checkAlreadyEmitted();
            referenceCount++;
        }
        totalSize.increment();
    }

    @Override
    public boolean decrementReferenceCount() {
        synchronized (lock) {
            checkAlreadyEmitted();
            referenceCount--;
            return !isInProgress() && referenceCount <= 0;
        }
    }

    /**
     * Returns the reference count of the segment. This number represents how many open subsegments are children of this segment.
     * The segment is emitted when its reference count reaches 0.
     *
     * @return the reference count
     */
    @Override
    public int getReferenceCount() {
        synchronized (lock) {
            return referenceCount;
        }
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
        synchronized (lock) {
            return emitted;
        }
    }

    /**
     * @param emitted
     *            the emitted to set
     */
    @Override
    public void setEmitted(boolean emitted) {
        synchronized (lock) {
            checkAlreadyEmitted();
            this.emitted = emitted;
        }
    }

    @Override
    public boolean compareAndSetEmitted(boolean current, boolean next) {
        synchronized (lock) {
            if (emitted == current) {
                emitted = next;
                return true;
            }
            return false;
        }
    }

    @Override
    public String serialize() {
        synchronized (lock) {
            try {
                return mapper.writeValueAsString(this);
            } catch (JsonProcessingException jpe) {
                logger.error("Exception while serializing entity.", jpe);
            }
            return "";
        }
    }

    @Override
    public String prettySerialize() {
        synchronized (lock) {
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            } catch (JsonProcessingException jpe) {
                logger.error("Exception while serializing segment.", jpe);
            }
            return "";
        }
    }

    @Override
    public void removeSubsegment(Subsegment subsegment) {
        subsegments.remove(subsegment);
        getParentSegment().getTotalSize().decrement();
    }


    public static void validateNotNull(Object object) {
        if (null == object) {
            throw new NullPointerException();
        }
    }
}
