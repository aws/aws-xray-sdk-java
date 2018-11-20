package com.amazonaws.xray.strategy.sampling.pollers;

import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.services.xray.model.GetSamplingTargetsRequest;
import com.amazonaws.services.xray.model.GetSamplingTargetsResult;
import com.amazonaws.services.xray.model.SamplingStatisticsDocument;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;

import com.amazonaws.xray.strategy.sampling.rand.Rand;
import com.amazonaws.xray.strategy.sampling.rand.RandImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TargetPoller {
    private static Log logger = LogFactory.getLog(TargetPoller.class);
    private static final long PERIOD = 10; // Seconds
    private static final long MAX_JITTER = 100; // Milliseconds

    private AWSXRay client;
    private Clock clock;
    private CentralizedManifest manifest;
    private ScheduledExecutorService executor;

    public TargetPoller(CentralizedManifest m, AWSXRay client, Clock clock) {
        this.manifest = m;
        this.client = client;
        this.clock = clock;
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        executor.scheduleAtFixedRate(() -> {
            try {
                pollManifest();
            } catch (Throwable t) {
                logger.error("Encountered error polling GetSamplingTargets: ", t);
                // An Error should not be handled by the application.
                // The executor will die and not abrupt main thread.
                if(t instanceof Error) { throw t; }
            }
        }, PERIOD * 1000, getJitterInterval(), TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void pollManifest() {
        List<SamplingStatisticsDocument> statistics = manifest.snapshots(clock.instant());
        if (statistics.size() == 0) {
            logger.trace("No statistics to report. Not refreshing sampling targets.");
            return;
        }
        GetSamplingTargetsRequest req = new GetSamplingTargetsRequest()
                .withSamplingStatisticsDocuments(statistics);

        GetSamplingTargetsResult result = client.getSamplingTargets(req);
        manifest.putTargets(result.getSamplingTargetDocuments(), clock.instant());
    }

    private long getJitterInterval() {
        Rand random = new RandImpl();
        long interval = Math.round(random.next() * MAX_JITTER) + PERIOD * 1000;
        return interval;
    }
}
