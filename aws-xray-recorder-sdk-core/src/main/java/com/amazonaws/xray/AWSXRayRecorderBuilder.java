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

package com.amazonaws.xray;

import com.amazonaws.xray.contexts.SegmentContextResolverChain;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.entities.StringValidator;
import com.amazonaws.xray.listeners.SegmentListener;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ECSPlugin;
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.plugins.ElasticBeanstalkPlugin;
import com.amazonaws.xray.plugins.Plugin;
import com.amazonaws.xray.strategy.ContextMissingStrategy;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.PrioritizationStrategy;
import com.amazonaws.xray.strategy.RuntimeErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.StreamingStrategy;
import com.amazonaws.xray.strategy.ThrowableSerializationStrategy;
import com.amazonaws.xray.strategy.sampling.SamplingStrategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AWSXRayRecorderBuilder {
    private static final Log logger =
        LogFactory.getLog(AWSXRayRecorderBuilder.class);

    private static final Map<String, Integer> ORIGIN_PRIORITY;
    private static final String LOG_GROUP_KEY = "AWS_LOG_GROUP";

    static {
        HashMap<String, Integer> originPriority = new HashMap<>();
        originPriority.put(ElasticBeanstalkPlugin.ORIGIN, 0);
        originPriority.put(EKSPlugin.ORIGIN, 1);
        originPriority.put(ECSPlugin.ORIGIN, 2);
        originPriority.put(EC2Plugin.ORIGIN, 3);
        ORIGIN_PRIORITY = Collections.unmodifiableMap(originPriority);
    }

    private final Set<Plugin> plugins;
    private final List<SegmentListener> segmentListeners;

    @Nullable
    private SamplingStrategy samplingStrategy;
    @Nullable
    private StreamingStrategy streamingStrategy;
    @Nullable
    private PrioritizationStrategy prioritizationStrategy;
    @Nullable
    private ThrowableSerializationStrategy throwableSerializationStrategy;
    @Nullable
    private ContextMissingStrategy contextMissingStrategy;

    @Nullable
    private SegmentContextResolverChain segmentContextResolverChain;

    @Nullable
    private Emitter emitter;

    private boolean useFastIdGenerator = false;
    private boolean forcedTraceIdGeneration = false;


    private AWSXRayRecorderBuilder() {
        plugins = new HashSet<>();
        segmentListeners = new ArrayList<>();
    }

    public static Optional<ContextMissingStrategy> contextMissingStrategyFromEnvironmentVariable() {
        String contextMissingStrategyOverrideValue = System.getenv(
            ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY);
        return getContextMissingStrategy(contextMissingStrategyOverrideValue);
    }

    public static Optional<ContextMissingStrategy> contextMissingStrategyFromSystemProperty() {
        String contextMissingStrategyOverrideValue = System.getProperty(
            ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY);
        return getContextMissingStrategy(contextMissingStrategyOverrideValue);
    }

    private static Optional<ContextMissingStrategy> getContextMissingStrategy(
        @Nullable String contextMissingStrategyOverrideValue) {
        if (contextMissingStrategyOverrideValue != null) {
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
     * @return An instance of {@code AWSXRayRecorder} using the
     * {@link com.amazonaws.xray.strategy.sampling.DefaultSamplingStrategy},
     * {@link com.amazonaws.xray.strategy.DefaultPrioritizationStrategy}, along with other default strategies and settings.
     */
    public static AWSXRayRecorder defaultRecorder() {
        return standard().build();
    }

    /**
     * Adds a plugin to the list of plugins which the builder will execute at build time. This method is execution-order
     * sensitive. Values overriden by plugins later in the list will be kept (e.g. origin).
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

    public AWSXRayRecorderBuilder withThrowableSerializationStrategy(
        ThrowableSerializationStrategy throwableSerializationStrategy) {
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
     * Prepares this builder to build an instance of {@code AWSXRayRecorder} with the provided context missing strategy. This
     * value will be overriden at {@code build()} time if either the environment variable with key
     * {@code AWS_XRAY_CONTEXT_MISSING} or system property with key {@code com.amazonaws.xray.strategy.contextMissingStrategy} are
     * set to a valid value.
     *
     * @param contextMissingStrategy
     *            the context missing strategy to be used if both the environment variable with key
     *            {@code AWS_XRAY_CONTEXT_MISSING} or system property with key
     *            {@code com.amazonaws.xray.strategy.contextMissingStrategy} are not set to a valid value
     * @return
     *            the builder instance, for chaining
     * @see ContextMissingStrategy
     */
    public AWSXRayRecorderBuilder withContextMissingStrategy(ContextMissingStrategy contextMissingStrategy) {
        this.contextMissingStrategy = contextMissingStrategy;
        return this;
    }

    /**
     * Adds all implemented plugins to the builder instance rather than requiring them to be individually added. The recorder will
     * only reflect metadata from plugins that are enabled, which is checked in the build method below.
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
     * Prepares this builder to build an {@code AWSXRayRecorder} which uses a fast but cryptographically insecure
     * random number generator for generating random IDs. This option should be preferred unless your application
     * relies on AWS X-Ray trace IDs being generated from a cryptographically secure random number source.
     *
     * @see #withSecureIdGenerator()
     */
    public AWSXRayRecorderBuilder withFastIdGenerator() {
        this.useFastIdGenerator = true;
        return this;
    }

    /**
     * Prepares this builder to build an {@code AWSXRayRecorder} which uses a cryptographically secure random
     * generator for generating random IDs. Unless your application relies on AWS X-Ray trace IDs
     * being generated from a cryptographically secure random number source, you should prefer
     * to use {@linkplain #withFastIdGenerator() the fast ID generator}.
     *
     * @see #withFastIdGenerator()
     */
    public AWSXRayRecorderBuilder withSecureIdGenerator() {
        this.useFastIdGenerator = false;
        return this;
    }

    /**
     * Prepares this builder to build an {@code AWSXRayRecorder} which creates Trace ID for all Segments
     * even for NoOpSegments or not sampled ones that usually include include a static invalid TraceID.
     * This could be useful for example in case the Trace ID is logged to be able to aggregate all logs from a
     * single request
     */
    public AWSXRayRecorderBuilder withForcedTraceIdGeneration() {
        this.forcedTraceIdGeneration = true;
        return this;
    }

    /**
     * Constructs and returns an AWSXRayRecorder with the provided configuration.
     *
     * @return a configured instance of AWSXRayRecorder
     */
    public AWSXRayRecorder build() {
        AWSXRayRecorder client = new AWSXRayRecorder();

        if (samplingStrategy != null) {
            client.setSamplingStrategy(samplingStrategy);
        }
        if (streamingStrategy != null) {
            client.setStreamingStrategy(streamingStrategy);
        }
        if (prioritizationStrategy != null) {
            client.setPrioritizationStrategy(prioritizationStrategy);
        }
        if (throwableSerializationStrategy != null) {
            client.setThrowableSerializationStrategy(throwableSerializationStrategy);
        }
        ContextMissingStrategy contextMissingStrategy = this.contextMissingStrategy;
        if (contextMissingStrategy != null &&
            !AWSXRayRecorderBuilder.contextMissingStrategyFromEnvironmentVariable().isPresent() &&
            !AWSXRayRecorderBuilder.contextMissingStrategyFromSystemProperty().isPresent()) {
            client.setContextMissingStrategy(contextMissingStrategy);
        }
        if (segmentContextResolverChain != null) {
            client.setSegmentContextResolverChain(segmentContextResolverChain);
        }

        if (emitter != null) {
            client.setEmitter(emitter);
        }

        if (!segmentListeners.isEmpty()) {
            client.addAllSegmentListeners(segmentListeners);
        }

        if (useFastIdGenerator) {
            client.useFastIdGenerator();
        } else {
            client.useSecureIdGenerator();
        }

        if (forcedTraceIdGeneration) {
            client.setForcedTraceIdGeneration(true);
        }

        plugins.stream().filter(Objects::nonNull).filter(p -> p.isEnabled()).forEach(plugin -> {
            logger.info("Collecting trace metadata from " + plugin.getClass().getName() + ".");

            try {
                Map<String, @Nullable Object> runtimeContext = plugin.getRuntimeContext();
                if (!runtimeContext.isEmpty()) {
                    client.putRuntimeContext(plugin.getServiceName(), runtimeContext);

                    /**
                     * Given several enabled plugins, the recorder should resolve a single one that's most representative of this
                     * environment
                     * Resolution order: EB > EKS > ECS > EC2
                     * EKS > ECS because the ECS plugin checks for an environment variable whereas the EKS plugin checks for a
                     * kubernetes authentication file, which is a stronger enable condition
                     */
                    String clientOrigin = client.getOrigin();
                    if (clientOrigin == null ||
                        ORIGIN_PRIORITY.getOrDefault(plugin.getOrigin(), 0) <
                        ORIGIN_PRIORITY.getOrDefault(clientOrigin, 0)) {
                        client.setOrigin(plugin.getOrigin());
                    }
                } else {
                    logger.warn(plugin.getClass().getName() + " plugin returned empty runtime context data. The recorder will "
                                + "not be setting segment origin or runtime context values from this plugin.");
                }
            } catch (Exception e) {
                logger.warn("Failed to get runtime context from " + plugin.getClass().getName() + ".", e);
            }

            try {
                Set<AWSLogReference> logReferences = plugin.getLogReferences();
                if (Objects.nonNull(logReferences)) {
                    if (!logReferences.isEmpty()) {
                        client.addAllLogReferences(logReferences);
                    } else {
                        logger.debug(plugin.getClass().getName() + " plugin returned empty Log References. The recorder will not "
                                    + "reflect the logs from this plugin.");
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to get log references from " + plugin.getClass().getName() + ".", e);
            }
        });

        String logGroupFromEnv = System.getenv(LOG_GROUP_KEY);
        if (StringValidator.isNotNullOrBlank(logGroupFromEnv)) {
            logger.info("Recording log group " + logGroupFromEnv + " from environment variable.");
            AWSLogReference logReference = new AWSLogReference();
            logReference.setLogGroup(logGroupFromEnv);
            client.addAllLogReferences(Collections.singleton(logReference));
        }

        return client;
    }
}
