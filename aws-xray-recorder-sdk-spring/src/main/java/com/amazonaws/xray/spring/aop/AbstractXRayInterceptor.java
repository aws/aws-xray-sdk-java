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
