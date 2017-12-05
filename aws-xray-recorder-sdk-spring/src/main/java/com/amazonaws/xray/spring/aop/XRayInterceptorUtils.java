package com.amazonaws.xray.spring.aop;

import org.aspectj.lang.ProceedingJoinPoint;

public class XRayInterceptorUtils {

    public static Object conditionalProceed(ProceedingJoinPoint pjp) throws Throwable {
        if (pjp.getArgs().length == 0) {
            return pjp.proceed();
        } else {
            return pjp.proceed(pjp.getArgs());
        }
    }


}
