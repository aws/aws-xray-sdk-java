package com.amazonaws.xray.interceptors;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Cause;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@FixMethodOrder(MethodSorters.JVM)
@RunWith(MockitoJUnitRunner.class)
public class TracingInterceptorTest {

    @Before
    public void setup() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());

        AWSXRay.setGlobalRecorder(
                AWSXRayRecorderBuilder.standard()
                        .withEmitter(blankEmitter)
                        .build()
        );
        AWSXRay.clearTraceEntity();
        AWSXRay.beginSegment("test");
    }

    @After
    public void teardown() {
        AWSXRay.endSegment();
    }

    private SdkHttpClient mockSdkHttpClient(SdkHttpResponse response) throws Exception {
        return mockSdkHttpClient(response, "OK");
    }

    private SdkHttpClient mockSdkHttpClient(SdkHttpResponse response, String body) throws Exception {
        ExecutableHttpRequest abortableCallable = Mockito.mock(ExecutableHttpRequest.class);
        SdkHttpClient mockClient = Mockito.mock(SdkHttpClient.class);

        Mockito.when(mockClient.prepareRequest(Mockito.any())).thenReturn(abortableCallable);
        Mockito.when(abortableCallable.call()).thenReturn(HttpExecuteResponse.builder()
                .response(response)
                .responseBody(AbortableInputStream.create(
                        new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))
                ))
                .build()
        );
        return mockClient;
    }

    private SdkAsyncHttpClient mockSdkAsyncHttpClient(SdkHttpResponse response) {
        SdkAsyncHttpClient mockClient = Mockito.mock(SdkAsyncHttpClient.class);
        Mockito.when(mockClient.execute(Mockito.any(AsyncExecuteRequest.class))).thenAnswer((Answer<CompletableFuture<Void>>) invocationOnMock -> {
            SdkAsyncHttpResponseHandler handler = invocationOnMock.getArgumentAt(0, AsyncExecuteRequest.class).responseHandler();
            handler.onHeaders(response);
            handler.onStream(new EmptyPublisher<>());

            return CompletableFuture.completedFuture(null);
        });

        return mockClient;
    }

    private SdkHttpResponse generateLambdaInvokeResponse(int statusCode) {
        return SdkHttpResponse.builder()
                .statusCode(statusCode)
                .putHeader("x-amz-request-id", "1111-2222-3333-4444")
                .putHeader("x-amz-id-2", "extended")
                .putHeader("Content-Length", "2")
                .putHeader("X-Amz-Function-Error", "Failure")
                .build();
    }

    @Test
    public void testResponseDescriptors() throws Exception {
        String responseBody = "{\"LastEvaluatedTableName\":\"baz\",\"TableNames\":[\"foo\",\"bar\",\"baz\"]}";
        SdkHttpResponse mockResponse = SdkHttpResponse.builder()
                .statusCode(200)
                .putHeader("x-amzn-requestid", "1111-2222-3333-4444")
                .putHeader("Content-Length", "84")
                .putHeader("Content-Type", "application/x-amz-json-1.0")
                .build();
        SdkHttpClient mockClient = mockSdkHttpClient(mockResponse, responseBody);

        DynamoDbClient client = DynamoDbClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();

        Segment segment = AWSXRay.getCurrentSegment();
        client.listTables(ListTablesRequest.builder()
                .limit(3)
                .build()
        );

        Assert.assertEquals(1, segment.getSubsegments().size());
        Subsegment subsegment = segment.getSubsegments().get(0);
        Map<String, Object> awsStats = subsegment.getAws();
        Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");

        Assert.assertEquals("ListTables", awsStats.get("operation"));
        Assert.assertEquals(3, awsStats.get("limit"));
        Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
        Assert.assertEquals(3, awsStats.get("table_count"));
        Assert.assertEquals("us-west-42", awsStats.get("region"));
        Assert.assertEquals(0, awsStats.get("retries"));
        Assert.assertEquals(84L, httpResponseStats.get("content_length"));
        Assert.assertEquals(200, httpResponseStats.get("status"));
        Assert.assertEquals(false, subsegment.isInProgress());
    }

    @Test
    public void testLambdaInvokeSubsegmentContainsFunctionName() throws Exception {
        SdkHttpClient mockClient = mockSdkHttpClient(generateLambdaInvokeResponse(200));

        LambdaClient client = LambdaClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();


        Segment segment = AWSXRay.getCurrentSegment();

        client.invoke(InvokeRequest.builder()
                .functionName("testFunctionName")
                .build()
        );

        Assert.assertEquals(1, segment.getSubsegments().size());
        Subsegment subsegment = segment.getSubsegments().get(0);
        Map<String, Object> awsStats = subsegment.getAws();
        Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");

        Assert.assertEquals("Invoke", awsStats.get("operation"));
        Assert.assertEquals("testFunctionName", awsStats.get("function_name"));
        Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
        Assert.assertEquals("extended", awsStats.get("id_2"));
        Assert.assertEquals("Failure", awsStats.get("function_error"));
        Assert.assertEquals("us-west-42", awsStats.get("region"));
        Assert.assertEquals(0, awsStats.get("retries"));
        Assert.assertEquals(2L, httpResponseStats.get("content_length"));
        Assert.assertEquals(200, httpResponseStats.get("status"));
        Assert.assertEquals(false, subsegment.isInProgress());
    }

    @Test
    public void testAsyncLambdaInvokeSubsegmentContainsFunctionName() {
        SdkAsyncHttpClient mockClient = mockSdkAsyncHttpClient(generateLambdaInvokeResponse(200));

        LambdaAsyncClient client = LambdaAsyncClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();

        Segment segment = AWSXRay.getCurrentSegment();

        client.invoke(InvokeRequest.builder()
                .functionName("testFunctionName")
                .build()
        ).join();

        Assert.assertEquals(1, segment.getSubsegments().size());
        Subsegment subsegment = segment.getSubsegments().get(0);
        Map<String, Object> awsStats = subsegment.getAws();
        Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");

        Assert.assertEquals("Invoke", awsStats.get("operation"));
        Assert.assertEquals("testFunctionName", awsStats.get("function_name"));
        Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
        Assert.assertEquals("extended", awsStats.get("id_2"));
        Assert.assertEquals("Failure", awsStats.get("function_error"));
        Assert.assertEquals("us-west-42", awsStats.get("region"));
        Assert.assertEquals(0, awsStats.get("retries"));
        Assert.assertEquals(2L, httpResponseStats.get("content_length"));
        Assert.assertEquals(200, httpResponseStats.get("status"));
        Assert.assertEquals(false, subsegment.isInProgress());
    }

    @Test
    public void test400Exception() throws Exception {
        SdkHttpClient mockClient = mockSdkHttpClient(generateLambdaInvokeResponse(400));

        LambdaClient client = LambdaClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();


        Segment segment = AWSXRay.getCurrentSegment();

        try {
            client.invoke(InvokeRequest.builder()
                    .functionName("testFunctionName")
                    .build()
            );
        } catch (Exception e) {
            // ignore SDK errors
        } finally {
            Assert.assertEquals(1, segment.getSubsegments().size());
            Subsegment subsegment = segment.getSubsegments().get(0);
            Map<String, Object> awsStats = subsegment.getAws();
            Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");
            Cause cause = subsegment.getCause();

            Assert.assertEquals("Invoke", awsStats.get("operation"));
            Assert.assertEquals("testFunctionName", awsStats.get("function_name"));
            Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
            Assert.assertEquals("extended", awsStats.get("id_2"));
            Assert.assertEquals("us-west-42", awsStats.get("region"));
            Assert.assertEquals(0, awsStats.get("retries"));
            Assert.assertEquals(2L, httpResponseStats.get("content_length"));
            Assert.assertEquals(400, httpResponseStats.get("status"));
            Assert.assertEquals(false, subsegment.isInProgress());
            Assert.assertEquals(true, subsegment.isError());
            Assert.assertEquals(false, subsegment.isThrottle());
            Assert.assertEquals(false, subsegment.isFault());
            Assert.assertEquals(1, cause.getExceptions().size());
            Assert.assertEquals(true, cause.getExceptions().get(0).isRemote());
        }
    }

    @Test
    public void testAsync400Exception() {
        SdkAsyncHttpClient mockClient = mockSdkAsyncHttpClient(generateLambdaInvokeResponse(400));

        LambdaAsyncClient client = LambdaAsyncClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();

        Segment segment = AWSXRay.getCurrentSegment();

        try {
            client.invoke(InvokeRequest.builder()
                    .functionName("testFunctionName")
                    .build()
            ).get();
        } catch (Exception e) {
            // ignore exceptions
        } finally {
            Assert.assertEquals(1, segment.getSubsegments().size());
            Subsegment subsegment = segment.getSubsegments().get(0);
            Map<String, Object> awsStats = subsegment.getAws();
            Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");
            Cause cause = subsegment.getCause();

            Assert.assertEquals("Invoke", awsStats.get("operation"));
            Assert.assertEquals("testFunctionName", awsStats.get("function_name"));
            Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
            Assert.assertEquals("extended", awsStats.get("id_2"));
            Assert.assertEquals("us-west-42", awsStats.get("region"));
            Assert.assertEquals(0, awsStats.get("retries"));
            Assert.assertEquals(2L, httpResponseStats.get("content_length"));
            Assert.assertEquals(400, httpResponseStats.get("status"));
            Assert.assertEquals(false, subsegment.isInProgress());
            Assert.assertEquals(true, subsegment.isError());
            Assert.assertEquals(false, subsegment.isThrottle());
            Assert.assertEquals(false, subsegment.isFault());
            Assert.assertEquals(1, cause.getExceptions().size());
            Assert.assertEquals(true, cause.getExceptions().get(0).isRemote());
        }
    }

    @Test
    public void testThrottledException() throws Exception {
        SdkHttpClient mockClient = mockSdkHttpClient(generateLambdaInvokeResponse(429));

        LambdaClient client = LambdaClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();


        Segment segment = AWSXRay.getCurrentSegment();

        try {
            client.invoke(InvokeRequest.builder()
                    .functionName("testFunctionName")
                    .build()
            );
        } catch (Exception e) {
            // ignore SDK errors
        } finally {
            Assert.assertEquals(1, segment.getSubsegments().size());
            Subsegment subsegment = segment.getSubsegments().get(0);
            Map<String, Object> awsStats = subsegment.getAws();
            Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");
            Cause cause = subsegment.getCause();

            Assert.assertEquals("Invoke", awsStats.get("operation"));
            Assert.assertEquals("testFunctionName", awsStats.get("function_name"));
            Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
            Assert.assertEquals("extended", awsStats.get("id_2"));
            Assert.assertEquals("us-west-42", awsStats.get("region"));
            // throttled requests are retried
            Assert.assertEquals(3, awsStats.get("retries"));
            Assert.assertEquals(2L, httpResponseStats.get("content_length"));
            Assert.assertEquals(429, httpResponseStats.get("status"));
            Assert.assertEquals(true, subsegment.isError());
            Assert.assertEquals(true, subsegment.isThrottle());
            Assert.assertEquals(false, subsegment.isFault());
            Assert.assertEquals(1, cause.getExceptions().size());
            Assert.assertEquals(true, cause.getExceptions().get(0).isRemote());
        }
    }

    @Test
    public void testAsyncThrottledException() {
        SdkAsyncHttpClient mockClient = mockSdkAsyncHttpClient(generateLambdaInvokeResponse(429));

        LambdaAsyncClient client = LambdaAsyncClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();

        Segment segment = AWSXRay.getCurrentSegment();

        try {
            client.invoke(InvokeRequest.builder()
                    .functionName("testFunctionName")
                    .build()
            ).get();
        } catch (Exception e) {
            // ignore exceptions
        } finally {
            Assert.assertEquals(1, segment.getSubsegments().size());
            Subsegment subsegment = segment.getSubsegments().get(0);
            Map<String, Object> awsStats = subsegment.getAws();
            Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");
            Cause cause = subsegment.getCause();

            Assert.assertEquals("Invoke", awsStats.get("operation"));
            Assert.assertEquals("testFunctionName", awsStats.get("function_name"));
            Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
            Assert.assertEquals("extended", awsStats.get("id_2"));
            Assert.assertEquals("us-west-42", awsStats.get("region"));
            // throttled requests are retried
            Assert.assertEquals(3, awsStats.get("retries"));
            Assert.assertEquals(2L, httpResponseStats.get("content_length"));
            Assert.assertEquals(429, httpResponseStats.get("status"));
            Assert.assertEquals(true, subsegment.isError());
            Assert.assertEquals(true, subsegment.isThrottle());
            Assert.assertEquals(false, subsegment.isFault());
            Assert.assertEquals(1, cause.getExceptions().size());
            Assert.assertEquals(true, cause.getExceptions().get(0).isRemote());
        }
    }

    @Test
    public void test500Exception() throws Exception {
        SdkHttpClient mockClient = mockSdkHttpClient(generateLambdaInvokeResponse(500));

        LambdaClient client = LambdaClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();


        Segment segment = AWSXRay.getCurrentSegment();

        try {
            client.invoke(InvokeRequest.builder()
                    .functionName("testFunctionName")
                    .build()
            );
        } catch (Exception e) {
            // ignore SDK errors
        } finally {
            Assert.assertEquals(1, segment.getSubsegments().size());
            Subsegment subsegment = segment.getSubsegments().get(0);
            Map<String, Object> awsStats = subsegment.getAws();
            Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");
            Cause cause = subsegment.getCause();

            Assert.assertEquals("Invoke", awsStats.get("operation"));
            Assert.assertEquals("testFunctionName", awsStats.get("function_name"));
            Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
            Assert.assertEquals("extended", awsStats.get("id_2"));
            Assert.assertEquals("us-west-42", awsStats.get("region"));
            // 500 exceptions are retried
            Assert.assertEquals(3, awsStats.get("retries"));
            Assert.assertEquals(2L, httpResponseStats.get("content_length"));
            Assert.assertEquals(500, httpResponseStats.get("status"));
            Assert.assertEquals(false, subsegment.isError());
            Assert.assertEquals(false, subsegment.isThrottle());
            Assert.assertEquals(true, subsegment.isFault());
            Assert.assertEquals(1, cause.getExceptions().size());
            Assert.assertEquals(true, cause.getExceptions().get(0).isRemote());
        }
    }

    @Test
    public void testAsync500Exception() {
        SdkAsyncHttpClient mockClient = mockSdkAsyncHttpClient(generateLambdaInvokeResponse(500));

        LambdaAsyncClient client = LambdaAsyncClient.builder()
                .httpClient(mockClient)
                .endpointOverride(URI.create("http://example.com"))
                .region(Region.of("us-west-42"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create("key", "secret", "session")
                ))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
                )
                .build();

        Segment segment = AWSXRay.getCurrentSegment();

        try {
            client.invoke(InvokeRequest.builder()
                    .functionName("testFunctionName")
                    .build()
            ).get();
        } catch (Exception e) {
            // ignore exceptions
        } finally {
            Assert.assertEquals(1, segment.getSubsegments().size());
            Subsegment subsegment = segment.getSubsegments().get(0);
            Map<String, Object> awsStats = subsegment.getAws();
            Map<String, Object> httpResponseStats = (Map<String, Object>)subsegment.getHttp().get("response");
            Cause cause = subsegment.getCause();

            Assert.assertEquals("Invoke", awsStats.get("operation"));
            Assert.assertEquals("testFunctionName", awsStats.get("function_name"));
            Assert.assertEquals("1111-2222-3333-4444", awsStats.get("request_id"));
            Assert.assertEquals("extended", awsStats.get("id_2"));
            Assert.assertEquals("us-west-42", awsStats.get("region"));
            // 500 exceptions are retried
            Assert.assertEquals(3, awsStats.get("retries"));
            Assert.assertEquals(2L, httpResponseStats.get("content_length"));
            Assert.assertEquals(500, httpResponseStats.get("status"));
            Assert.assertEquals(false, subsegment.isError());
            Assert.assertEquals(false, subsegment.isThrottle());
            Assert.assertEquals(true, subsegment.isFault());
            Assert.assertEquals(1, cause.getExceptions().size());
            Assert.assertEquals(true, cause.getExceptions().get(0).isRemote());
        }
    }
}

