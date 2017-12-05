package com.amazonaws.xray.spring.aop;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Configurable
public class XRayJDBCInterceptor{

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Around("queryExecution()")
    public Object traceSQL(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Subsegment subsegment = AWSXRay.beginSubsegment(pjp.getSignature().getName());
            this.generateMetadata(pjp,subsegment);
            return XRayInterceptorUtils.conditionalProceed(pjp);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            AWSXRay.getCurrentSegment().addException(e);
            throw e;
        } finally {
            LOGGER.trace("Ending Subsegment");
            AWSXRay.endSubsegment();
        }
    }

    @Pointcut("execution(public !void java.sql.Statement.execute*(java.lang.String))")
    private void queryExecution(){}


    public Map<String, Map<String, Object>> generateMetadata(ProceedingJoinPoint pjp, Subsegment subsegment) throws Exception{
        final Map<String, Map<String, Object>> metadata = new HashMap<>();
        final Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("Class", pjp.getTarget().getClass().getSimpleName());
        metadata.put("ClassInfo", classInfo);
        String arg = (String) pjp.getArgs()[0];
        subsegment.putSql("SQL Statement",arg);
        return metadata;
    }
}
