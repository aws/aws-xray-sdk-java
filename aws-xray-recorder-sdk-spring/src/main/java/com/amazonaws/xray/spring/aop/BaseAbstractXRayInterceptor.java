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

package com.amazonaws.xray.spring.aop;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;


/**
 * Allows for use of this library without Spring Data JPA being in the classpath.
 * For projects using Spring Data JPA, consider using {@link AbstractXRayInterceptor} instead.
 */
public abstract class BaseAbstractXRayInterceptor {

    private static final Log logger = LogFactory.getLog(BaseAbstractXRayInterceptor.class);

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
            if (subsegment != null) {
                subsegment.setMetadata(generateMetadata(pjp, subsegment));
            }
            return XRayInterceptorUtils.conditionalProceed(pjp);
        } catch (Exception e) {
            AWSXRay.getCurrentSegmentOptional().ifPresent(x -> x.addException(e));
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
