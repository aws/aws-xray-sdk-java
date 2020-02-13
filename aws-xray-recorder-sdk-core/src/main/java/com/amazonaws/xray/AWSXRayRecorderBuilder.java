package com.amazonaws.xray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.listeners.SegmentListener;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ECSPlugin;
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.plugins.ElasticBeanstalkPlugin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.contexts.SegmentContextResolverChain;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.plugins.Plugin;
import com.amazonaws.xray.strategy.ContextMissingStrategy;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.PrioritizationStrategy;
import com.amazonaws.xray.strategy.RuntimeErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.StreamingStrategy;
import com.amazonaws.xray.strategy.ThrowableSerializationStrategy;
import com.amazonaws.xray.strategy.sampling.SamplingStrategy;

public class AWSXRayRecorderBuilder {
    private static final Log logger =
        LogFactory.getLog(AWSXRayRecorderBuilder.class);

    private final Collection<Plugin> plugins;

    private SamplingStrategy samplingStrategy;
    private StreamingStrategy streamingStrategy;
    private PrioritizationStrategy prioritizationStrategy;
    private ThrowableSerializationStrategy throwableSerializationStrategy;
    private ContextMissingStrategy contextMissingStrategy;

    private SegmentContextResolverChain segmentContextResolverChain;

    private Emitter emitter;

    private final Collection<SegmentListener> segmentListeners;
    private static final Map<String, Integer> originPriority;

    static {
        originPriority = new HashMap<>();
        originPriority.put(ElasticBeanstalkPlugin.ORIGIN, 0);
        originPriority.put(EKSPlugin.ORIGIN, 1);
        originPriority.put(ECSPlugin.ORIGIN, 2);
        originPriority.put(EC2Plugin.ORIGIN, 3);
    }

    private AWSXRayRecorderBuilder() {
        plugins = new HashSet<>();
        segmentListeners = new ArrayList<>();
    }

    public static Optional<ContextMissingStrategy> contextMissingStrategyFromEnvironmentVariable() {
        String contextMissingStrategyOverrideValue = System.getenv(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY);
        return getContextMissingStrategy(contextMissingStrategyOverrideValue);
    }

    public static Optional<ContextMissingStrategy> contextMissingStrategyFromSystemProperty() {
        String contextMissingStrategyOverrideValue = System.getProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY);
        return getContextMissingStrategy(contextMissingStrategyOverrideValue);
    }

    private static Optional<ContextMissingStrategy> getContextMissingStrategy(String contextMissingStrategyOverrideValue) {
        if (null != contextMissingStrategyOverrideValue) {
            if (contextMissingStrategyOverrideValue.equalsIgnoreCase(LogErrorContextMissingStrategy.OVERRIDE_VALUE)) {
                return Optional.of(new LogErrorContextMissingStrategy());
            } else if (contextMissingStrategyOverrideValue.equalsIgnoreCase(RuntimeErrorContextMissingStrategy.OVERRIDE_VALUE)) {
                return Optional.of(new RuntimeErrorContextMissingStrategy());
            } else if (contextMissingStrategyOverrideValue.equalsIgnoreCase(IgnoreErrorContextMissingStrategy.OVERRIDE_VALUE)) {
                return Optional.of(new IgnoreErrorContextMissingStrategy());
            }
        }
        return Optional.empty();
    }

    /**
     * @return A new builder instance with all defaults set.
     */
    public static AWSXRayRecorderBuilder standard() {
        return new AWSXRayRecorderBuilder();
    }

    /**
     * @return An instance of {@code AWSXRayRecorder} using the {@link com.amazonaws.xray.strategy.sampling.DefaultSamplingStrategy}, {@link com.amazonaws.xray.strategy.DefaultPrioritizationStrategy}, along with other default strategies and settings.
     */
    public static AWSXRayRecorder defaultRecorder() {
        return standard().build();
    }

    /**
     * Adds a plugin to the list of plugins which the builder will execute at build time. This method is execution-order sensitive. Values overriden by plugins later in the list will be kept (e.g. origin).
     *
     * @param plugin
     * the plugin to add
     * @return
     * the builder instance, for chaining
     */
    public AWSXRayRecorderBuilder withPlugin(Plugin plugin) {
        plugins.add(plugin);
        return this;
    }

    public AWSXRayRecorderBuilder withSamplingStrategy(SamplingStrategy samplingStrategy) {
        this.samplingStrategy = samplingStrategy;
        return this;
    }

    public AWSXRayRecorderBuilder withStreamingStrategy(StreamingStrategy streamingStrategy) {
        this.streamingStrategy = streamingStrategy;
        return this;
    }

    public AWSXRayRecorderBuilder withPrioritizationStrategy(PrioritizationStrategy prioritizationStrategy) {
        this.prioritizationStrategy = prioritizationStrategy;
        return this;
    }

    public AWSXRayRecorderBuilder withThrowableSerializationStrategy(ThrowableSerializationStrategy throwableSerializationStrategy) {
        this.throwableSerializationStrategy = throwableSerializationStrategy;
        return this;
    }

    public AWSXRayRecorderBuilder withEmitter(Emitter emitter) {
        this.emitter = emitter;
        return this;
    }

    public AWSXRayRecorderBuilder withSegmentContextResolverChain(SegmentContextResolverChain segmentContextResolverChain) {
        this.segmentContextResolverChain = segmentContextResolverChain;
        return this;
    }

    /**
     * Adds a SegmentListener to the list of segment listeners that will be attached to the recorder at build time.
     *
     * @param segmentListener
     * the SegmentListener to add
     * @return
     * the builder instance, for chaining
     */
    public AWSXRayRecorderBuilder withSegmentListener(SegmentListener segmentListener) {
        this.segmentListeners.add(segmentListener);
        return this;
    }

    /**
     * Prepares this builder to build an instance of {@code AWSXRayRecorder} with the provided context missing strategy. This value will be overriden at {@code build()} time if either the environment
     * variable with key {@code AWS_XRAY_CONTEXT_MISSING} or system property with key {@code com.amazonaws.xray.strategy.contextMissingStrategy} are set to a valid value.
     *
     * @param contextMissingStrategy
     *            the context missing strategy to be used if both the environment variable with key {@code AWS_XRAY_CONTEXT_MISSING} or system property with key {@code com.amazonaws.xray.strategy.contextMissingStrategy} are not set to a valid value
     * @return
     *            the builder instance, for chaining
     * @see ContextMissingStrategy
     */
    public AWSXRayRecorderBuilder withContextMissingStrategy(ContextMissingStrategy contextMissingStrategy) {
        this.contextMissingStrategy = contextMissingStrategy;
        return this;
    }

    /**
     * Adds all implemented plugins to the builder instance rather than requiring them to be individually added. The recorder will only reflect metadata from
     * plugins that are enabled, which is checked in the build method below.
     *
     * @return
     * The builder instance, for chaining
     */
    public AWSXRayRecorderBuilder withDefaultPlugins() {
        plugins.add(new EC2Plugin());
        plugins.add(new ECSPlugin());
        plugins.add(new EKSPlugin());
        plugins.add(new ElasticBeanstalkPlugin());
        return this;
    }

    /**
     * Constructs and returns an AWSXRayRecorder with the provided configuration.
     *
     * @return a configured instance of AWSXRayRecorder
     */
    public AWSXRayRecorder build() {
        AWSXRayRecorder client = new AWSXRayRecorder();

        if (null != samplingStrategy) {
            client.setSamplingStrategy(samplingStrategy);
        }
        if (null != streamingStrategy) {
            client.setStreamingStrategy(streamingStrategy);
        }
        if (null != prioritizationStrategy) {
            client.setPrioritizationStrategy(prioritizationStrategy);
        }
        if (null != throwableSerializationStrategy) {
            client.setThrowableSerializationStrategy(throwableSerializationStrategy);
        }
        if (null != contextMissingStrategy && !AWSXRayRecorderBuilder.contextMissingStrategyFromEnvironmentVariable().isPresent() && !AWSXRayRecorderBuilder.contextMissingStrategyFromSystemProperty().isPresent()) {
            client.setContextMissingStrategy(contextMissingStrategy);
        }
        if (null != segmentContextResolverChain) {
            client.setSegmentContextResolverChain(segmentContextResolverChain);
        }

        if (null != emitter) {
            client.setEmitter(emitter);
        }

        if (!segmentListeners.isEmpty()) {
            client.addAllSegmentListeners(segmentListeners);
        }

        plugins.stream().filter(Objects::nonNull).filter(p -> p.isEnabled()).forEach(plugin -> {
            logger.info("Collecting trace metadata from " + plugin.getClass().getName() + ".");

            try {
                Map<String, Object> runtimeContext = plugin.getRuntimeContext();
                if (!runtimeContext.isEmpty()) {
                    client.putRuntimeContext(plugin.getServiceName(), runtimeContext);

                    /**
                     * Given several enabled plugins, the recorder should resolve a single one that's most representative of this environment
                     * Resolution order: EB > EKS > ECS > EC2
                     * EKS > ECS because the ECS plugin checks for an environment variable whereas the EKS plugin checks for a kubernetes authentication file, which is a stronger enable condition
                     */
                    if (client.getOrigin() == null || originPriority.get(plugin.getOrigin()) < originPriority.get(client.getOrigin())) {
                        client.setOrigin(plugin.getOrigin());
                    }
                } else {
                    logger.warn(plugin.getClass().getName() + " plugin returned empty runtime context data. The recorder will not be setting segment origin or runtime context values from this plugin.");
                }
            } catch (Exception e) {
                logger.warn("Failed to get runtime context from "+plugin.getClass().getName()+".", e);
            }

            try {
                Set<AWSLogReference> logReferences = plugin.getLogReferences();
                if(Objects.nonNull(logReferences)) {
                    if (!logReferences.isEmpty()) {
                        client.addAllLogReferences(logReferences);
                    } else {
                        logger.warn(plugin.getClass().getName() + " plugin returned empty Log References. The recorder will not reflect the logs from this plugin.");
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to get log references from "+plugin.getClass().getName()+".", e);
            }
        });

        return client;
    }
}
