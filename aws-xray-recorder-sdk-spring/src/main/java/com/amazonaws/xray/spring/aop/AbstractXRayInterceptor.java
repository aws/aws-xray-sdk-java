package com.amazonaws.xray.spring.aop;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.exceptions.SegmentNotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractXRayInterceptor {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected Object processXRayTrace(ProceedingJoinPoint pjp) throws Throwable {
        boolean endSegment = false;
        try {
            if (AWSXRay.getCurrentSegment() != null) {
                LOGGER.trace("Current segment exists");
            }
        } catch (SegmentNotFoundException snfe) {
            LOGGER.trace("Creating new segment");
            AWSXRay.beginSegment(pjp.getClass().getSimpleName());
            endSegment = true;
        }

        try {
            Subsegment subsegment = AWSXRay.beginSubsegment(pjp.getSignature().getName());
            subsegment.setMetadata(this.generateMetadata(pjp, subsegment));
            return XRayInterceptorUtils.conditionalProceed(pjp);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            AWSXRay.getCurrentSegment().addException(e);
            throw e;
        } finally {
            LOGGER.trace("Ending Subsegment");
            AWSXRay.endSubsegment();
            if (endSegment) {
                LOGGER.trace("Ending Segment");
                AWSXRay.endSegment();
            }
        }
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

    /**
     * @param pjp the proceeding join point
     * @return the result of the method being wrapped
     * @throws Throwable
     */
    @Around("springRepositories()")
    public Object traceAroundRepositoryMethods(ProceedingJoinPoint pjp) throws Throwable {
        LOGGER.trace("Advising repository");
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


    protected abstract void xrayEnabledClasses();

    @Pointcut("execution(* XRayTraced+.*(..))")
    protected void xrayTracedClasses() {
    }

    @Pointcut("execution(public !void org.springframework.data.repository.Repository+.*(..))")
    protected void springRepositories() {
    }

    protected Map<String, Map<String, Object>> generateMetadata(ProceedingJoinPoint pjp, Subsegment subsegment) throws Exception{
        final Map<String, Map<String, Object>> metadata = new HashMap<>();
        final Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("Class", pjp.getTarget().getClass().getSimpleName());
        metadata.put("ClassInfo", classInfo);
        return metadata;
    }

}
