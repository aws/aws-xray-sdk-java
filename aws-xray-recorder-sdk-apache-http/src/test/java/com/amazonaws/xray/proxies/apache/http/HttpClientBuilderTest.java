package com.amazonaws.xray.proxies.apache.http;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.proxies.apache.http.HttpClientBuilder;

import static org.junit.Assert.assertEquals;

@FixMethodOrder(MethodSorters.JVM)
public class HttpClientBuilderTest {

    @Before
    public void setupAWSXRay() {
        // Prevent accidental publish to Daemon
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testConstructorProtected() throws NoSuchMethodException {
        // Since the constructor is protected and this is in the same package, we have to test this using reflection.
        Constructor clientBuilderConstructor = HttpClientBuilder.class.getDeclaredConstructor();
        assertEquals(Modifier.PROTECTED, clientBuilderConstructor.getModifiers()); // PROTECTED = 4;
    }
}
