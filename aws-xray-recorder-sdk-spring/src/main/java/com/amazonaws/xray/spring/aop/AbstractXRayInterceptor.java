package com.amazonaws.xray.spring.aop;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.exceptions.SegmentNotFoundException;
import com.amazonaws.xray.strategy.ContextMissingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;

public abstract class AbstractXRayInterceptor {

    private static final Log logger = LogFactory.getLog(AbstractXRayInterceptor.class);

    private static ContextMissingStrategy getContextMissingStrategy() {
        return AWSXRay.getGlobalRecorder().getContextMissingStrategy();
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
        boolean endSegment = false;
        try {
            if (AWSXRay.getCurrentSegment() != null) {
                logger.trace("Current segment exists");
            }
        } catch (SegmentNotFoundException snfe) {
            ContextMissingStrategy contextMissingStrategy = getContextMissingStrategy();
            contextMissingStrategy.contextMissing("Context Missing from Spring Interceptor", snfe.getClass());
            endSegment = true;
        }

        try {
            Subsegment subsegment = AWSXRay.beginSubsegment(pjp.getSignature().getName());
            subsegment.setMetadata(XRayInterceptorUtils.generateMetadata(pjp, subsegment));
            return XRayInterceptorUtils.conditionalProceed(pjp);
        } catch (Exception e) {
            AWSXRay.getCurrentSegment().addException(e);
            throw e;
        } finally {
            logger.trace("Ending Subsegment");
            AWSXRay.endSubsegment();
            if (endSegment) {
                logger.trace("Ending Segment");
                AWSXRay.endSegment();
            }
        }
    }


    protected abstract void xrayEnabledClasses();

    @Pointcut("execution(* XRayTraced+.*(..))")
    protected void xrayTracedClasses() {
    }

    @Pointcut("execution(public !void org.springframework.data.repository.Repository+.*(..))")
    protected void springRepositories() {
    }

    /**
     * @param pjp the proceeding join point
     * @return the result of the method being wrapped
     * @throws Throwable
     */
    @Around("springRepositories()")
    public Object traceAroundRepositoryMethods(ProceedingJoinPoint pjp) throws Throwable {
        logger.trace("Advising repository");
        boolean hasClassAnnotation = false;

        for (Class<?> i : pjp.getTarget().getClass().getInterfaces()) {
            if (i.getAnnotation(XRayEnabled.class) != null) {
                hasClassAnnotation = true;
                break;
            }
        }

        if (hasClassAnnotation) {
            return this.processXRayTrace(pjp);
        } else {
            return XRayInterceptorUtils.conditionalProceed(pjp);
        }
    }

}
