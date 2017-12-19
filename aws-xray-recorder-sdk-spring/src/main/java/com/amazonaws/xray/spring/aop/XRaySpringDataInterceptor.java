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

    @Pointcut("execution(public !void java.sql.Statement.execute*(java.lang.String))")
    private void queryExecution() {
    }

}
