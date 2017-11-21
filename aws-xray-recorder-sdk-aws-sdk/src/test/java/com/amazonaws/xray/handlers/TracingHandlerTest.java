package com.amazonaws.xray.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.apache.client.impl.ConnectionManagerAwareHttpClient;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Segment;

@FixMethodOrder(MethodSorters.JVM)
public class TracingHandlerTest {

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testLambdaInvokeSubsegmentContainsFunctionName() {
        // Setup test
        AWSLambda lambda = AWSLambdaClientBuilder.standard().withRequestHandlers(new TracingHandler()).withRegion(Regions.US_EAST_1).withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("fake", "fake"))).build();

        AmazonHttpClient amazonHttpClient = new AmazonHttpClient(new ClientConfiguration());
        ConnectionManagerAwareHttpClient apacheHttpClient = Mockito.mock(ConnectionManagerAwareHttpClient.class);
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        BasicHttpEntity responseBody = new BasicHttpEntity();
        responseBody.setContent(new ByteArrayInputStream("null".getBytes(StandardCharsets.UTF_8))); // Lambda returns "null" on successful fn. with no return value
        httpResponse.setEntity(responseBody);

        try {
            Mockito.doReturn(httpResponse).when(apacheHttpClient).execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpContext.class));
        } catch (IOException e) { }

        Whitebox.setInternalState(amazonHttpClient, "httpClient", apacheHttpClient);
        Whitebox.setInternalState(lambda, "client", amazonHttpClient);

        // Test logic
        Segment segment = AWSXRay.beginSegment("test");

        InvokeRequest request = new InvokeRequest();
        request.setFunctionName("testFunctionName");
        InvokeResult r = lambda.invoke(request);
        System.out.println(r.getStatusCode());
        System.out.println(r);

        Assert.assertEquals(1, segment.getSubsegments().size());
        Assert.assertEquals("Invoke", segment.getSubsegments().get(0).getAws().get("operation"));
        System.out.println(segment.getSubsegments().get(0).getAws());
        Assert.assertEquals("testFunctionName", segment.getSubsegments().get(0).getAws().get("function_name"));
    }

}
