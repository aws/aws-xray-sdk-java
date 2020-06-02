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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Configurable;

@Aspect
@Configurable
public class XRaySpringDataInterceptor {

    private static final Log logger = LogFactory.getLog(XRaySpringDataInterceptor.class);

    @Around("queryExecution()")
    public Object traceSQL(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Subsegment subsegment = AWSXRay.beginSubsegment(pjp.getSignature().getName());
            XRayInterceptorUtils.generateMetadata(pjp, subsegment);
            return XRayInterceptorUtils.conditionalProceed(pjp);
        } catch (Exception e) {
            logger.error(e.getMessage());
            AWSXRay.getCurrentSegment().addException(e);
            throw e;
        } finally {
            logger.trace("Ending Subsegment");
            AWSXRay.endSubsegment();
        }
    }

    // TODO(anuraaga): Pretty sure a no-op Pointcut is safe to remove but not sure, magic can be tricky to reason about.
    @SuppressWarnings("UnusedMethod")
    @Pointcut("execution(public !void java.sql.Statement.execute*(java.lang.String))")
    private void queryExecution() {
    }

}
