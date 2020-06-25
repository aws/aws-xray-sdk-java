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

package com.amazonaws.xray.strategy.sampling.pollers;

import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.services.xray.model.GetSamplingTargetsRequest;
import com.amazonaws.services.xray.model.GetSamplingTargetsResult;
import com.amazonaws.services.xray.model.SamplingStatisticsDocument;
import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import com.amazonaws.xray.strategy.sampling.rand.Rand;
import com.amazonaws.xray.strategy.sampling.rand.RandImpl;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TargetPoller {
    private static final Log logger = LogFactory.getLog(TargetPoller.class);
    private static final long PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private static final long MAX_JITTER_MILLIS = 100;

    private final UnsignedXrayClient client;
    private final CentralizedManifest manifest;
    private final Clock clock;
    private final ScheduledExecutorService executor;

    @Nullable
    private volatile ScheduledFuture<?> pollFuture;

    /**
     * @deprecated Use {@link #TargetPoller(UnsignedXrayClient, CentralizedManifest, Clock)}.
     */
    @Deprecated
    public TargetPoller(CentralizedManifest manifest, AWSXRay unused, Clock clock) {
        this(new UnsignedXrayClient(), manifest, clock);
    }

    public TargetPoller(UnsignedXrayClient client, CentralizedManifest manifest, Clock clock) {
        this.client = client;
        this.manifest = manifest;
        this.clock = clock;
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        pollFuture = executor.scheduleAtFixedRate(() -> {
            try {
                pollManifest();
            } catch (Throwable t) {
                logger.error("Encountered error polling GetSamplingTargets: ", t);
                // Propagate if Error so executor stops executing.
                // TODO(anuraaga): Many Errors aren't fatal, this should probably be more restricted, e.g.
                // https://github.com/openzipkin/brave/blob/master/brave/src/main/java/brave/internal/Throwables.java
                if (t instanceof Error) { throw t; }
            }
        }, PERIOD_MILLIS, getIntervalWithJitter(), TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (pollFuture != null) {
            pollFuture.cancel(true);
        }
        executor.shutdownNow();
    }

    // Visible for testing
    ScheduledExecutorService getExecutor() {
        return executor;
    }

    private void pollManifest() {
        List<SamplingStatisticsDocument> statistics = manifest.snapshots(clock.instant());
        if (statistics.size() == 0) {
            logger.trace("No statistics to report. Not refreshing sampling targets.");
            return;
        }

        logger.debug("Polling sampling targets.");
        GetSamplingTargetsRequest req = new GetSamplingTargetsRequest()
                .withSamplingStatisticsDocuments(statistics);

        GetSamplingTargetsResult result = client.getSamplingTargets(req);
        manifest.putTargets(result.getSamplingTargetDocuments(), clock.instant());
    }

    private long getIntervalWithJitter() {
        Rand random = new RandImpl();
        return Math.round(random.next() * MAX_JITTER_MILLIS) + PERIOD_MILLIS;
    }
}
