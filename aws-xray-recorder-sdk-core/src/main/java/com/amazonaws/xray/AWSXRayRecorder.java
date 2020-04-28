package com.amazonaws.xray;

import com.amazonaws.xray.contexts.LambdaSegmentContextResolver;
import com.amazonaws.xray.contexts.SegmentContext;
import com.amazonaws.xray.contexts.SegmentContextResolverChain;
import com.amazonaws.xray.contexts.ThreadLocalSegmentContextResolver;
import com.amazonaws.xray.emitters.DefaultEmitter;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.listeners.SegmentListener;
import com.amazonaws.xray.entities.*;
import com.amazonaws.xray.exceptions.SegmentNotFoundException;
import com.amazonaws.xray.exceptions.SubsegmentNotFoundException;
import com.amazonaws.xray.strategy.*;
import com.amazonaws.xray.strategy.sampling.DefaultSamplingStrategy;
import com.amazonaws.xray.strategy.sampling.SamplingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AWSXRayRecorder {

    private static final Log logger = LogFactory.getLog(AWSXRayRecorder.class);

    private static final String PROPERTIES_LOCATION = "/com/amazonaws/xray/sdk.properties";
    private static final String SDK_VERSION_KEY = "awsxrayrecordersdk.version";
    private static final String DEFAULT_SDK_VERSION = "unknown";
    private static final String SDK = "X-Ray for Java";
    private static final String CW_LOGS_KEY = "cloudwatch_logs";


    private static Map<String, Object> SDK_VERSION_INFORMATION;
    private static Map<String, Object> RUNTIME_INFORMATION;

    static {
        SDK_VERSION_INFORMATION = new HashMap<String, Object>();
        RUNTIME_INFORMATION = new HashMap<String, Object>();
        InputStream propertiesStream = AWSXRayRecorder.class.getResourceAsStream(PROPERTIES_LOCATION);
        Properties properties = new Properties();
        try {
            properties.load(propertiesStream);
        } catch (IOException | IllegalArgumentException | NullPointerException e) {
            logger.warn("Unable to detect SDK version.", e);
        }
        String sdkVersion = DEFAULT_SDK_VERSION;
        if (null != properties.getProperty(SDK_VERSION_KEY)) {
            sdkVersion = properties.getProperty(SDK_VERSION_KEY);
        }
        SDK_VERSION_INFORMATION.put("sdk", SDK);
        SDK_VERSION_INFORMATION.put("sdk_version", sdkVersion);


        String javaVersion = System.getProperty("java.version");
        if (null != javaVersion) {
            RUNTIME_INFORMATION.put("runtime_version", javaVersion);
        }

        String javaVmName = System.getProperty("java.vm.name");
        if (null != javaVmName) {
            RUNTIME_INFORMATION.put("runtime", System.getProperty("java.vm.name"));
        }
    }

    private SamplingStrategy samplingStrategy;
    private StreamingStrategy streamingStrategy;
    private PrioritizationStrategy prioritizationStrategy;
    private ThrowableSerializationStrategy throwableSerializationStrategy;
    private ContextMissingStrategy contextMissingStrategy;

    private SegmentContextResolverChain segmentContextResolverChain;

    private Emitter emitter;

    private ArrayList<SegmentListener> segmentListeners;

    private Map<String, Object> awsRuntimeContext;
    private Map<String, Object> serviceRuntimeContext;
    private Set<AWSLogReference> logReferences;


    private String origin;

    public AWSXRayRecorder() {
        samplingStrategy = new DefaultSamplingStrategy();
        streamingStrategy = new DefaultStreamingStrategy();
        prioritizationStrategy = new DefaultPrioritizationStrategy();
        throwableSerializationStrategy = new DefaultThrowableSerializationStrategy();
        contextMissingStrategy = new DefaultContextMissingStrategy();

        logReferences = new HashSet<>();

        Optional<ContextMissingStrategy> environmentContextMissingStrategy = AWSXRayRecorderBuilder.contextMissingStrategyFromEnvironmentVariable();
        Optional<ContextMissingStrategy> systemContextMissingStrategy = AWSXRayRecorderBuilder.contextMissingStrategyFromSystemProperty();
        if (environmentContextMissingStrategy.isPresent()) {
            logger.info("Overriding contextMissingStrategy. Environment variable " + ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY + " has value: \""
                    + System.getenv(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY) + "\".");
            contextMissingStrategy = environmentContextMissingStrategy.get();
        } else if (systemContextMissingStrategy.isPresent()) {
            logger.info("Overriding contextMissingStrategy. System property " + ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY + " has value: \""
                    + System.getProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY) + "\".");
            contextMissingStrategy = systemContextMissingStrategy.get();
        }

        segmentContextResolverChain = new SegmentContextResolverChain();
        segmentContextResolverChain.addResolver(new LambdaSegmentContextResolver());
        segmentContextResolverChain.addResolver(new ThreadLocalSegmentContextResolver());

        segmentListeners = new ArrayList<>();

        awsRuntimeContext = new ConcurrentHashMap<>();
        awsRuntimeContext.put("xray", SDK_VERSION_INFORMATION);

        serviceRuntimeContext = new ConcurrentHashMap<>();
        serviceRuntimeContext.putAll(RUNTIME_INFORMATION);

        try {
            emitter = new DefaultEmitter();
        } catch (SocketException e) {
            throw new RuntimeException("Unable to instantiate AWSXRayRecorder: ", e);
        }
    }

    /**
     * Sends a segment to the emitter if the segment is marked as sampled.
     *
     * @param segment
     *  the segment to send
     * @return
     *  true if the segment was emitted succesfully.
     */
    public boolean sendSegment(Segment segment) {
        if (segment.isSampled()) {
            return emitter.sendSegment(segment);
        }
        return false;
    }

    /**
     * Sends a subsegment to the emitter if the subsegment's parent segment is marked as sampled.
     *
     * @param subsegment
     *  the subsegment to send
     * @return
     *  true if the subsegment was emitted succesfully.
     */
    public boolean sendSubsegment(Subsegment subsegment) {
        if (subsegment.getParentSegment().isSampled()) {
            return emitter.sendSubsegment(subsegment);
        }
        return false;
    }

    /**
     * Begins a segment, passes it to the supplied function, and ends the segment before returning the supplied function's result. Intercepts exceptions, adds them to the segment, and re-throws them.
     *
     * @param <R>
     *            the type of the value returned by {@code function}
     * @param name
     *            the name to use for the created segment
     * @param function
     *            the function to invoke
     * @return the value returned by the supplied function
     */
    public <R> R createSegment(String name, Function<Segment, R> function) {
        Segment segment = beginSegment(name);
        try {
            return function.apply(segment);
        } catch (Exception e) {
            segment.addException(e);
            throw e;
        } finally {
            endSegment();
        }
    }

    /**
     * Begins a segment and passes it to the supplied consumer, and ends the segment before returning the consumer's result. Intercepts exceptions, adds them to the segment, and re-throws them.
     *
     * @param name
     *            the name to use for the created segment
     * @param consumer
     *            the function to invoke
     */
    public void createSegment(String name, Consumer<Segment> consumer) {
        Segment segment = beginSegment(name);
        try {
            consumer.accept(segment);
        } catch (Exception e) {
            segment.addException(e);
            throw e;
        } finally {
            endSegment();
        }
    }

    /**
     * Begins a segment, invokes the provided supplier, and ends the segment before returning the supplier's result. Intercepts exceptions, adds them to the segment, and re-throws them.
     *
     * @param <R>
     *            the type of the value returned by {@code supplier}
     * @param name
     *            the name to use for the created segment
     * @param supplier
     *            the supplier to invoke
     * @return the value returned by the provided supplier
     */
    public <R> R createSegment(String name, Supplier<R> supplier) {
        Segment segment = beginSegment(name);
        try {
            return supplier.get();
        } catch (Exception e) {
            segment.addException(e);
            throw e;
        } finally {
            endSegment();
        }
    }

    /**
     * Begins a segment, runs the provided runnable, and ends the segment before returning the supplier's result. Intercepts exceptions, adds them to the segment, and re-throws them.
     *
     * @param name
     *            the name to use for the created segment
     * @param runnable
     *            the runnable to run
     */
    public void createSegment(String name, Runnable runnable) {
        Segment segment = beginSegment(name);
        try {
            runnable.run();
        } catch (Exception e) {
            segment.addException(e);
            throw e;
        } finally {
            endSegment();
        }
    }


    /**
     * Begins a subsegment, passes it to the supplied function, and ends the subsegment before returning the supplied function's result. Intercepts exceptions, adds them to the subsegment, and re-throws them.
     *
     * @param <R>
     *            the type of the value returned by {@code function}
     * @param name
     *            the name to use for the created subsegment
     * @param function
     *            the function to invoke
     * @return the value returned by the supplied function
     */
    public <R> R createSubsegment(String name, Function<Subsegment, R> function) {
        Subsegment subsegment = beginSubsegment(name);
        try {
            return function.apply(subsegment);
        } catch (Exception e) {
            if (subsegment != null) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            endSubsegment();
        }
    }

    /**
     * Begins a subsegment and passes it to the supplied consumer, and ends the subsegment before returning the consumer's result. Intercepts exceptions, adds them to the subsegment, and re-throws them.
     *
     * @param name
     *            the name to use for the created subsegment
     * @param consumer
     *            the function to invoke
     */
    public void createSubsegment(String name, Consumer<Subsegment> consumer) {
        Subsegment subsegment = beginSubsegment(name);
        try {
            consumer.accept(subsegment);
        } catch (Exception e) {
            if (subsegment != null) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            endSubsegment();
        }
    }

    /**
     * Begins a subsegment, passes it to the provided supplier, and ends the subsegment before returning the supplier's result. Intercepts exceptions, adds them to the subsegment, and re-throws them.
     *
     * @param <R>
     *            the type of the value returned by {@code function}
     * @param name
     *            the name to use for the created subsegment
     * @param supplier
     *            the supplier to invoke
     * @return the value returned by the provided supplier
     */
    public <R> R createSubsegment(String name, Supplier<R> supplier) {
        Subsegment subsegment = beginSubsegment(name);
        try {
            return supplier.get();
        } catch (Exception e) {
            if (subsegment != null) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            endSubsegment();
        }
    }

    /**
     * Begins a subsegment, runs the provided runnable, and ends the subsegment once complete. Intercepts exceptions, adds them to the subsegment, and re-throws them.
     *
     * @param name
     *            the name to use for the created subsegment
     * @param runnable
     *            the runnable to run
     */
    public void createSubsegment(String name, Runnable runnable) {
        Subsegment subsegment = beginSubsegment(name);
        try {
            runnable.run();
        } catch (Exception e) {
            if (subsegment != null) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            endSubsegment();
        }
    }

    public Segment beginSegment(String name) {
        return beginSegment(new SegmentImpl(this, name));
    }

    public Segment beginSegment(String name, TraceID traceId, String parentId) {
        Segment segment = new SegmentImpl(this, name, traceId);
        segment.setParentId(parentId);
        return beginSegment(segment);
    }

    /**
     * Sets the current segment to a new instance of {@code DummySegment}.
     *
     * @return the newly created {@code DummySegment}.
     */
    public Segment beginDummySegment() {
        return beginSegment(new DummySegment(this));
    }

    public Segment beginDummySegment(String name, TraceID traceId) {
        return beginSegment(new DummySegment(this, name, traceId));
    }

    public Segment beginDummySegment(TraceID traceId) {
        return beginSegment(new DummySegment(this, traceId));
    }

    private Segment beginSegment(Segment segment) {
        SegmentContext context = getSegmentContext();
        if (null == context) {
            return null;
        }

        Entity current;
        if ((current = getTraceEntity()) != null) {
            logger.error("Beginning new segment while another segment exists in the segment context. Overwriting current segment named '" + current.getName() + "' to start new segment named '" + segment.getName() + "'.");
        }

        segment.setAws(getAwsRuntimeContext());
        if (null != getOrigin()){
            segment.setOrigin(getOrigin());
        }
        segment.putAllService(getServiceRuntimeContext());

        if(null != logReferences && !logReferences.isEmpty()) {

            segment.putAws(CW_LOGS_KEY, logReferences);
        }

        setTraceEntity(segment);

        segmentListeners.stream()
                .filter(Objects::nonNull)
                .forEach(listener -> listener.onBeginSegment(segment));


        return context.beginSegment(this, segment);
    }

    /**
     * Ends a segment.
     *
     * @throws SegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and no segment is currently in progress
     */
    public void endSegment() {
        SegmentContext context = getSegmentContext();
        if (null != context) {
            context.endSegment(this);
        }

        Entity current;
        if ((current = getTraceEntity()) != null) {
            Segment segment = current.getParentSegment();
            logger.debug("Ending segment named '" + segment.getName() + "'.");

            segmentListeners
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(listener -> listener.beforeEndSegment(segment));

            if(segment.end()) {
                sendSegment(segment);
            } else {
                logger.debug("Not emitting segment named '" + segment.getName() + "' as it parents in-progress subsegments.");
            }

            segmentListeners
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(listener -> listener.afterEndSegment(segment));

            clearTraceEntity();
        } else {
            getContextMissingStrategy().contextMissing("Failed to end segment: segment cannot be found.", SegmentNotFoundException.class);
        }

    }

    /**
     * Ends the provided subsegment. This method doesn't touch context storage.
     *
     * @param subsegment
     *          the subsegment to close.
     */
    public void endSubsegment(Subsegment subsegment) {
        if(subsegment == null) {
            logger.debug("No input subsegment to end. No-op.");
            return;
        }
        boolean rootReady = subsegment.end();
        // First handling the special case where its direct parent is a facade segment
        if(subsegment.getParent() instanceof FacadeSegment) {
            if(((FacadeSegment) subsegment.getParent()).isSampled()) {
                getEmitter().sendSubsegment(subsegment);
            }
            return;
        }
        // Otherwise we check the happy case where the entire segment is ready
        if (rootReady && !(subsegment.getParentSegment() instanceof FacadeSegment)) {
            sendSegment(subsegment.getParentSegment());
            return;
        }
        // If not we try to stream closed subsegments regardless the root segment is facade or real
        if (this.getStreamingStrategy().requiresStreaming(subsegment.getParentSegment())) {
            this.getStreamingStrategy().streamSome(subsegment.getParentSegment(), this.getEmitter());
        }
    }

    /**
     * Begins a subsegment.
     *
     * @param name
     *            the name to use for the created subsegment
     * @throws SegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and no segment is currently in progress
     * @return the newly created subsegment, or {@code null} if {@code contextMissingStrategy} suppresses and no segment is currently in progress
     */
    public Subsegment beginSubsegment(String name) {
        SegmentContext context = getSegmentContext();
        if (null != context) {
            return segmentContextResolverChain.resolve().beginSubsegment(this, name);
        }
        return null;
    }

    /**
     * Ends a subsegment.
     *
     * @throws SegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and no segment is currently in progress
     * @throws SubsegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and no subsegment is currently in progress
     */
    public void endSubsegment() {
        SegmentContext context = getSegmentContext();
        if (null != context) {
            segmentContextResolverChain.resolve().endSubsegment(this);
        }
    }

    /**
     * @throws SegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and there is no segment in progress
     * @return the current segment, or {@code null} if {@code contextMissingStrategy} suppresses exceptions and there is no segment in progress
     */
    public Segment getCurrentSegment() {
        Optional<Segment> segment = getCurrentSegmentOptional();
        if (segment.isPresent()) {
            return segment.get();
        }
        contextMissingStrategy.contextMissing("No segment in progress.", SegmentNotFoundException.class);
        return null;
    }

    /**
     * @return the current segment, or {@code Optional.empty()} if there is no segment
     */
    public Optional<Segment> getCurrentSegmentOptional() {
        SegmentContext context = segmentContextResolverChain.resolve(); // explicitly do not throw context missing exceptions from optional-returning methods
        if (null == context) {
            return Optional.empty();
        }
        Entity current = context.getTraceEntity();
        if (current instanceof Segment) {
            return Optional.of((Segment) current);
        } else if (current instanceof Subsegment) {
            return Optional.of(current.getParentSegment());
        } else {
            return Optional.empty();
        }
    }

    /**
     * @throws SegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and the segment context cannot be found
     * @throws SubsegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and the current segment has no subsegments in progress
     * @return the current subsegment, or {@code null} if {@code contextMissingStrategy} suppresses exceptions and the segment context cannot be found or the segment has no subsegments in progress
     */
    public Subsegment getCurrentSubsegment() {
        SegmentContext context = getSegmentContext();
        if (null == context) {
            return null;
        }
        Entity current = context.getTraceEntity();
        if (null == current) {
            contextMissingStrategy.contextMissing("No segment in progress.", SegmentNotFoundException.class);
        } else if (current instanceof Subsegment) {
            return (Subsegment) current;
        } else {
            contextMissingStrategy.contextMissing("No subsegment in progress.", SubsegmentNotFoundException.class);
        }
        return null;
    }

    /**
     * @return the current subsegment, or {@code Optional.empty()} if there is no subsegment
     */
    public Optional<Subsegment> getCurrentSubsegmentOptional() {
        SegmentContext context = segmentContextResolverChain.resolve(); // explicitly do not throw context missing exceptions from optional-returning methods
        if (null == context) {
            return Optional.empty();
        }
        Entity current = context.getTraceEntity();
        if (current instanceof Subsegment) {
            return Optional.of((Subsegment) current);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Injects the provided {@code Entity} into the current thread's thread local context.
     *
     * @param entity
     *            the {@code Segment} or {@code Subsegment} to inject into the current thread
     *
     * @deprecated use {@link #setTraceEntity(Entity entity)} instead
     */
    @Deprecated
    public void injectThreadLocal(Entity entity) {
        ThreadLocalStorage.set(entity);
    }

    /**
     *
     * @return the Entity object currently stored in the thread's ThreadLocalStorage
     *
     * @deprecated use {@link #getTraceEntity()} instead
     */
    @Deprecated
    public Entity getThreadLocal() {
        return ThreadLocalStorage.get();
    }

    /**
     * @deprecated use {@link #clearTraceEntity()} instead
     */
    @Deprecated
    public void clearThreadLocal() {
        ThreadLocalStorage.clear();
    }

    private SegmentContext getSegmentContext() {
        SegmentContext context = segmentContextResolverChain.resolve();
        if (null == context) {
            contextMissingStrategy.contextMissing("Segment context not found.", SegmentNotFoundException.class);
            return null;
        }
        return context;
    }

    /**
     * Sets the trace entity value using the implementation provided by the SegmentContext resolved from the segmentContextResolverChain.
     *
     * @param entity
     *            the trace entity to set
     */
    public void setTraceEntity(Entity entity) {
        SegmentContext context = getSegmentContext();
        if (null == context) {
            return;
        }
        context.setTraceEntity(entity);
    }

    /**
     * Gets the current trace entity value using the implementation provided by the SegmentContext resolved from the segmentContextResolverChain.
     *
     * @return the current trace entity
     */
    public Entity getTraceEntity() {
        SegmentContext context = getSegmentContext();
        if (null == context) {
            return null;
        }
        return context.getTraceEntity();
    }

    /**
     * Clears the current trace entity value using the implementation provided by the SegmentContext resolved from the segmentContextResolverChain.
     *
     */
    public void clearTraceEntity() {
        SegmentContext context = getSegmentContext();
        if (null == context) {
            return;
        }
        context.clearTraceEntity();
    }

    public void putRuntimeContext(String key, Object value) {
        if (null == value) {
            value = "";
        }
        awsRuntimeContext.put(key, value);
    }

    public void addAllLogReferences(Set<AWSLogReference> logReferences) {
            this.logReferences.addAll(logReferences);
    }

    /**
     * @return the samplingStrategy
     */
    public SamplingStrategy getSamplingStrategy() {
        return samplingStrategy;
    }

    /**
     * @param samplingStrategy the samplingStrategy to set
     */
    public void setSamplingStrategy(SamplingStrategy samplingStrategy) {
        this.samplingStrategy = samplingStrategy;
    }

    /**
     * @return the streamingStrategy
     */
    public StreamingStrategy getStreamingStrategy() {
        return streamingStrategy;
    }

    /**
     * @param streamingStrategy the streamingStrategy to set
     */
    public void setStreamingStrategy(StreamingStrategy streamingStrategy) {
        this.streamingStrategy = streamingStrategy;
    }

    /**
     * @return the prioritizationStrategy
     */
    public PrioritizationStrategy getPrioritizationStrategy() {
        return prioritizationStrategy;
    }

    /**
     * @param prioritizationStrategy the prioritizationStrategy to set
     */
    public void setPrioritizationStrategy(PrioritizationStrategy prioritizationStrategy) {
        this.prioritizationStrategy = prioritizationStrategy;
    }

    /**
     * @return the throwableSerializationStrategy
     */
    public ThrowableSerializationStrategy getThrowableSerializationStrategy() {
        return throwableSerializationStrategy;
    }

    /**
     * @param throwableSerializationStrategy the throwableSerializationStrategy to set
     */
    public void setThrowableSerializationStrategy(ThrowableSerializationStrategy throwableSerializationStrategy) {
        this.throwableSerializationStrategy = throwableSerializationStrategy;
    }

    /**
     * @return the contextMissingStrategy
     */
    public ContextMissingStrategy getContextMissingStrategy() {
        return contextMissingStrategy;
    }

    /**
     * @param contextMissingStrategy the contextMissingStrategy to set
     */
    public void setContextMissingStrategy(ContextMissingStrategy contextMissingStrategy) {
        this.contextMissingStrategy = contextMissingStrategy;
    }
    /**
     * @return the segmentContextResolverChain
     */
    public SegmentContextResolverChain getSegmentContextResolverChain() {
        return segmentContextResolverChain;
    }

    /**
     * @param segmentContextResolverChain the segmentContextResolverChain to set
     */
    public void setSegmentContextResolverChain(SegmentContextResolverChain segmentContextResolverChain) {
        this.segmentContextResolverChain = segmentContextResolverChain;
    }

    /**
     * @return the emitter
     */
    public Emitter getEmitter() {
        return emitter;
    }

    /**
     * @param emitter the emitter to set
     */
    public void setEmitter(Emitter emitter) {
        this.emitter = emitter;
    }

    /**
     * Returns the list of SegmentListeners attached to the recorder
     *
     * @return the SegmentListeners
     */
    public ArrayList<SegmentListener> getSegmentListeners() {
        return segmentListeners;
    }

    /**
     * Adds a single SegmentListener to the recorder
     *
     * @param segmentListener a SegmentListener to add
     */
    public void addSegmentListener(SegmentListener segmentListener) {
        this.segmentListeners.add(segmentListener);
    }

    /**
     * Adds a Collection of SegmentListeners to the recorder
     *
     * @param segmentListeners a Collection of SegmentListeners to add
     */
    public void addAllSegmentListeners(Collection<SegmentListener> segmentListeners) {
        this.segmentListeners.addAll(segmentListeners);
    }


    /**
     * @return the awsRuntimeContext
     */
    public Map<String, Object> getAwsRuntimeContext() {
        return awsRuntimeContext;
    }

    /**
     * @return the serviceRuntimeContext
     */
    public Map<String, Object> getServiceRuntimeContext() {
        return serviceRuntimeContext;
    }

    /**
     * @return the origin
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * Checks whether the current {@code SamplingStrategy} supports forced sampling.
     *
     * @return true if forced sampling is supported and the current segment was changed from not sampled to sampled.
     */
    public boolean forceSamplingOfCurrentSegment() {
        if (samplingStrategy.isForcedSamplingSupported()) {
            Segment segment = getCurrentSegment();
            if (!segment.isSampled()) {
                segment.setSampled(true);
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @throws SegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and no segment or subsegment is currently in progress
     * @return the ID of the {@code Segment} or {@code Subsegment} currently in progress, or {@code null} if {@code contextMissingStrategy} suppresses exceptions and no segment or subsegment is currently in progress
     */
    public String currentEntityId() {
        SegmentContext context = getSegmentContext();
        if (null == context) {
            return null;
        }
        Entity current = context.getTraceEntity();
        if (null != current) {
            return current.getId();
        } else {
            contextMissingStrategy.contextMissing("Failed to get current entity ID: segment or subsegment cannot be found.", SegmentNotFoundException.class);
            return null;
        }
    }

    /**
     *
     * @throws SegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and no segment or subsegment is currently in progress
     * @return the trace ID of the {@code Segment} currently in progress, or {@code null} if {@code contextMissingStrategy} suppresses exceptions and no segment or subsegment is currently in progress
     */
    public TraceID currentTraceId() {
        SegmentContext context = getSegmentContext();
        if (null == context) {
            return null;
        }
        Entity current = context.getTraceEntity();
        if (null != current) {
            return current.getParentSegment().getTraceId();
        } else {
            contextMissingStrategy.contextMissing("Failed to get current trace ID: segment cannot be found.", SegmentNotFoundException.class);
            return null;
        }
    }

    /**
     *
     * @throws SegmentNotFoundException
     *             if {@code contextMissingStrategy} throws exceptions and no segment or subsegment is currently in progress
     * @return the trace ID of the {@code Segment} currently in progress and the ID of the {@code Segment} or {@code Subsegment} in progress, joined with {@code @},
     *             or {@code null} if {@code contextMissingStrategy} suppresses exceptions and no segment or subsegment is currently in progress
     */
    public String currentFormattedId() {
        SegmentContext context = getSegmentContext();
        if (null == context) {
            return null;
        }
        Entity current = context.getTraceEntity();
        if (null != current) {
            TraceID traceId = current.getParentSegment().getTraceId();
            String entityId = current.getId();
            return traceId.toString() + "@" + entityId;
        } else {
            contextMissingStrategy.contextMissing("Failed to get current formatted ID: segment cannot be found.", SegmentNotFoundException.class);
            return null;
        }
    }
}
