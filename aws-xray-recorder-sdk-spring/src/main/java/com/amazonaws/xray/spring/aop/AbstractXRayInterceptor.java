package com.amazonaws.xray.spring.aop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;


public abstract class AbstractXRayInterceptor extends BaseAbstractXRayInterceptor {

    private static final Log logger = LogFactory.getLog(AbstractXRayInterceptor.class);

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
