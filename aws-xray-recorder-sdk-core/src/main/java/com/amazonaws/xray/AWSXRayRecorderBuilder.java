package com.amazonaws.xray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.amazonaws.xray.entities.AWSLogReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.contexts.SegmentContextResolverChain;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.plugins.Plugin;
import com.amazonaws.xray.strategy.ContextMissingStrategy;
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

    private AWSXRayRecorderBuilder() {
        plugins = new ArrayList<>();
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

        plugins.stream().filter(Objects::nonNull).forEach(plugin -> {
            Map<String, Object> runtimeContext = plugin.getRuntimeContext();
            if (!runtimeContext.isEmpty()) {
                client.putRuntimeContext(plugin.getServiceName(), runtimeContext);
                client.setOrigin(plugin.getOrigin());
            } else {
                logger.warn(plugin.getClass().getName() + " plugin returned empty runtime context data. The recorder will not be setting segment origin or runtime context values from this plugin.");
            }

            Set<AWSLogReference> logReferences = plugin.getLogReferences();
            if (logReferences != null && !logReferences.isEmpty()) {
                client.addAllLogReferences(logReferences);
            } else {
                logger.warn(plugin.getClass().getName() + " plugin returned empty Log References. The recorder will not reflect the logs from this plugin.");
            }
        });

        return client;
    }
}
