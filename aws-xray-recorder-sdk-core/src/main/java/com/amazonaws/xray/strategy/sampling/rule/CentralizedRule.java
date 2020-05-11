package com.amazonaws.xray.strategy.sampling.rule;

import com.amazonaws.services.xray.model.SamplingRule;
import com.amazonaws.services.xray.model.SamplingStatisticsDocument;
import com.amazonaws.services.xray.model.SamplingTargetDocument;
import com.amazonaws.xray.strategy.sampling.SamplingRequest;
import com.amazonaws.xray.strategy.sampling.SamplingResponse;
import com.amazonaws.xray.strategy.sampling.rand.Rand;
import com.amazonaws.xray.strategy.sampling.reservoir.CentralizedReservoir;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a customer-defined sampling rule. A rule contains the matchers
 * required to determine if an incoming request can use the rule, and sampling
 * targets which determine the sampling behavior once a request has been
 * matched.
 *
 * A rule also maintains usage statistics which are periodically reported to
 * X-Ray.
 */
public class CentralizedRule implements Rule, Comparable<CentralizedRule> {
    private static final Log logger =
            LogFactory.getLog(CentralizedRule.class);

    public static final String DEFAULT_RULE_NAME = "Default";

    private int priority = 10000; // Default

    // Rule Name identifying this rule.
    private String name;

    private CentralizedReservoir centralizedReservoir;
    private double fixedRate;

    private Statistics statistics;

    // Null for customer default rule.
    private Matchers matchers;

    private Rand rand;

    private ReadWriteLock lock;

    public CentralizedRule(SamplingRule input, Rand rand) {
        this.name = input.getRuleName();
        this.centralizedReservoir = new CentralizedReservoir(input.getReservoirSize());
        this.fixedRate = input.getFixedRate();
        this.statistics = new Statistics();

        if (!input.getRuleName().equals(DEFAULT_RULE_NAME)) {
            this.matchers = new Matchers(input);
            this.priority = input.getPriority();
        }

        this.rand = rand;
        this.lock = new ReentrantReadWriteLock();
    }

    public boolean update(SamplingRule i) {
        boolean rebuild = false;
        Matchers m = new Matchers(i);

        lock.writeLock().lock();
        try {
            fixedRate = i.getFixedRate();

            if (priority != i.getPriority()) {
                rebuild = true;
            }
            priority = i.getPriority();
            fixedRate = i.getFixedRate();
            matchers = m;
            centralizedReservoir.update(i);
        } finally {
            lock.writeLock().unlock();
        }

        return rebuild;
    }

    // Returns true if the rule is due for a target refresh. False otherwise.
    public boolean isStale(Instant now) {
        lock.readLock().lock();
        try {
            return statistics.getRequests() > 0 && centralizedReservoir.isStale(now);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static boolean isValid(SamplingRule rule) {
        if (rule.getRuleName() == null || rule.getPriority() == null
                || rule.getReservoirSize() == null || rule.getFixedRate() == null || rule.getVersion() != 1) {

            logger.error("Detect invalid rule. Please check sampling rule format.");
            return false;
        }

        if (!rule.getResourceARN().equals("*") || !rule.getAttributes().isEmpty()) {
            logger.error("Detect invalid rule. Please check sampling rule format.");
            return false;
        }

        if (rule.getHost() == null || rule.getServiceName() == null || rule.getHTTPMethod() == null || rule.getURLPath() == null || rule.getServiceType() == null) {
            logger.error("Detect invalid rule. Please check sampling rule format.");
            return false;
        }

        if (rule.getRuleName().equals(DEFAULT_RULE_NAME)) {
            return true;
        }

        return true;
    }

    public void update(SamplingTargetDocument t, Instant now) {
        lock.writeLock().lock();
        try {
            centralizedReservoir.update(t, now);
            fixedRate = t.getFixedRate();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public SamplingStatisticsDocument snapshot(Date now) {
        SamplingStatisticsDocument s = new SamplingStatisticsDocument()
                .withRuleName(name)
                .withTimestamp(now);

        lock.writeLock().lock();
        try {
            s.setRequestCount(statistics.getRequests());
            s.setSampledCount(statistics.getSampled());
            s.setBorrowCount(statistics.getBorrowed());

            statistics.reset();
        } finally {
            lock.writeLock().unlock();
        }

        return s;
    }

    public boolean match(SamplingRequest r) {
        lock.readLock().lock();
        try {
            return matchers != null ? matchers.match(r) : true;
        } finally {
            lock.readLock().unlock();
        }
    }

    public SamplingResponse sample(Instant now) {
        SamplingResponse res = new SamplingResponse(name);
        double rn = rand.next();

        lock.writeLock().lock();
        try {
            return doSample(now, res, rn);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private SamplingResponse doSample(Instant now, SamplingResponse res, double random) {
        statistics.incRequest();
        return doSampleCustomerRule(now, res, random);
    }

    private SamplingResponse doSampleCustomerRule(Instant now, SamplingResponse res, double random) {
        if (centralizedReservoir.isExpired(now)) {
            // Attempt to borrow request

            if (centralizedReservoir.isBorrow(now)) {
                logger.debug("Sampling target has expired for rule " + getName() + ". Burrowing a request.");
                statistics.incBorrowed();
                res.setSampled(true);

                return res;
            }

            logger.debug("Sampling target has expired for rule " + getName() + ". Using fixed rate.");
            // Fallback to bernoulli sampling
            if (random < fixedRate) {
                statistics.incSampled();
                res.setSampled(true);

                return res;
            }

            return res;
        }

        // CentralizedReservoir has a valid quota. Consume a unit, if available.
        if (centralizedReservoir.take(now)) {
            statistics.incSampled();
            res.setSampled(true);
            logger.debug("Sampling target has been exhausted for rule " + getName() + ". Using fixed request.");

            return res;
        }

        // Fallback to bernoulli sampling
        if (random < fixedRate) {
            statistics.incSampled();
            res.setSampled(true);

            return res;
        }

        return res;
    }

    public int compareTo(CentralizedRule other) {
        lock.readLock().lock();
        try {
            if (this.priority < other.priority) {
                return -1;
            } else if (this.priority > other.priority) {
                return 1;
            }

            return this.getName().compareTo(other.getName());
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CentralizedRule)) {
            return false;
        }

        CentralizedRule that = (CentralizedRule) o;

        if (priority != that.priority) return false;
        if (Double.compare(that.fixedRate, fixedRate) != 0) return false;
        if (!name.equals(that.name)) return false;
        if (!centralizedReservoir.equals(that.centralizedReservoir)) return false;
        if (!statistics.equals(that.statistics)) return false;
        return Objects.equals(matchers, that.matchers);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name.hashCode();
        result = 31 * result + priority;
        result = 31 * result + centralizedReservoir.hashCode();
        temp = Double.doubleToLongBits(fixedRate);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + statistics.hashCode();
        result = 31 * result + (matchers != null ? matchers.hashCode() : 0);
        return result;
    }

}
