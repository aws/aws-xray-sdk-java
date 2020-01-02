package com.amazonaws.xray.spring.aop;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.exceptions.SegmentNotFoundException;
import com.amazonaws.xray.strategy.ContextMissingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Map;
import java.util.Optional;

import static com.amazonaws.xray.AWSXRay.getCurrentSegmentOptional;


/**
 * Allows for use of this library without Spring Data JPA being in the classpath.
 * For projects using Spring Data JPA, consider using {@link AbstractXRayInterceptor} instead.
 */
public abstract class BaseAbstractXRayInterceptor {

    private static final Log logger = LogFactory.getLog(BaseAbstractXRayInterceptor.class);

    private static ContextMissingStrategy getContextMissingStrategy() {
        return AWSXRay.getGlobalRecorder().getContextMissingStrategy();
    }

    private static Segment getCurrentSegment() {
        Optional<Segment> segment = getCurrentSegmentOptional();
        if (segment.isPresent()) {
            return segment.get();
        }
        ContextMissingStrategy contextMissingStrategy = getContextMissingStrategy();
        contextMissingStrategy.contextMissing("No segment in progress.", SegmentNotFoundException.class);
        return null;
    }

    /**
     * @param pjp the proceeding join point
     * @return the result of the method being wrapped
     * @throws Throwable
     */
    @Around("xrayTracedClasses() || xrayEnabledClasses()")
    public Object traceAroundMethods(ProceedingJoinPoint pjp) throws Throwable {
        return this.processXRayTrace(pjp);
    }

    protected Object processXRayTrace(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Subsegment subsegment = AWSXRay.beginSubsegment(pjp.getSignature().getName());
            if(subsegment != null) {
                subsegment.setMetadata(generateMetadata(pjp, subsegment));
            }
            return XRayInterceptorUtils.conditionalProceed(pjp);
        } catch (Exception e) {
            AWSXRay.getCurrentSegment().addException(e);
            throw e;
        } finally {
            logger.trace("Ending Subsegment");
            AWSXRay.endSubsegment();
        }
    }

    protected abstract void xrayEnabledClasses();

    @Pointcut("execution(* XRayTraced+.*(..))")
    protected void xrayTracedClasses() {
    }

    protected Map<String, Map<String, Object>> generateMetadata(ProceedingJoinPoint pjp, Subsegment subsegment) {
        return XRayInterceptorUtils.generateMetadata(pjp, subsegment);
    }
}
