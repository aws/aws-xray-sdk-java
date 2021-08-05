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

package com.amazonaws.xray.interceptors;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.EntityDataKeys;
import com.amazonaws.xray.entities.EntityHeaderKeys;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.handlers.config.AWSOperationHandler;
import com.amazonaws.xray.handlers.config.AWSOperationHandlerManifest;
import com.amazonaws.xray.handlers.config.AWSServiceHandlerManifest;
import com.amazonaws.xray.utils.StringTransform;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;

public class TracingInterceptor implements ExecutionInterceptor {

    /**
     * @deprecated For internal use only.
     */
    @Deprecated
    @SuppressWarnings("checkstyle:ConstantName")
    // TODO(anuraaga): Make private in next major version and rename.
    public static final ExecutionAttribute<Subsegment> entityKey = new ExecutionAttribute("AWS X-Ray Entity");

    private static final Log logger = LogFactory.getLog(TracingInterceptor.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private static final URL DEFAULT_OPERATION_PARAMETER_WHITELIST =
        TracingInterceptor.class.getResource("/com/amazonaws/xray/interceptors/DefaultOperationParameterWhitelist.json");

    private static final String UNKNOWN_REQUEST_ID = "UNKNOWN";
    private static final List<String> REQUEST_ID_KEYS = Arrays.asList("x-amz-request-id", "x-amzn-requestid");

    private AWSServiceHandlerManifest awsServiceHandlerManifest;
    private AWSXRayRecorder recorder;
    private final String accountId;

    public TracingInterceptor() {
        this(null, null, null);
    }

    public TracingInterceptor(AWSXRayRecorder recorder, String accountId, URL parameterWhitelist) {
        this.recorder = recorder;
        this.accountId = accountId;
        initInterceptorManifest(parameterWhitelist);
    }

    private void initInterceptorManifest(URL parameterWhitelist) {
        if (parameterWhitelist != null) {
            try {
                awsServiceHandlerManifest = MAPPER.readValue(parameterWhitelist, AWSServiceHandlerManifest.class);
                return;
            } catch (IOException e) {
                logger.error(
                        "Unable to parse operation parameter whitelist at " + parameterWhitelist.getPath()
                        + ". Falling back to default operation parameter whitelist at "
                        + TracingInterceptor.DEFAULT_OPERATION_PARAMETER_WHITELIST.getPath() + ".",
                        e
                );
            }
        }
        try {
            awsServiceHandlerManifest = MAPPER.readValue(
                TracingInterceptor.DEFAULT_OPERATION_PARAMETER_WHITELIST, AWSServiceHandlerManifest.class);
        } catch (IOException e) {
            logger.error(
                    "Unable to parse default operation parameter whitelist at "
                    + TracingInterceptor.DEFAULT_OPERATION_PARAMETER_WHITELIST.getPath()
                    + ". This will affect this handler's ability to capture AWS operation parameter information.",
                    e
            );
        }
    }

    private AWSOperationHandler getOperationHandler(ExecutionAttributes executionAttributes) {
        String serviceName = executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME);
        String operationName = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);

        if (awsServiceHandlerManifest == null) {
            return null;
        }
        AWSOperationHandlerManifest operationManifest = awsServiceHandlerManifest.getOperationHandlerManifest(serviceName);
        if (operationManifest == null) {
            return null;
        }
        return operationManifest.getOperationHandler(operationName);
    }

    private HashMap<String, Object> extractRequestParameters(
        Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        HashMap<String, Object> parameters = new HashMap<>();

        AWSOperationHandler operationHandler = getOperationHandler(executionAttributes);
        if (operationHandler == null) {
            return parameters;
        }

        if (operationHandler.getRequestParameters() != null) {
            operationHandler.getRequestParameters().forEach((parameterName) -> {
                SdkRequest request = context.request();
                Optional<Object> parameterValue = request.getValueForField(parameterName, Object.class);
                if (parameterValue.isPresent()) {
                    parameters.put(StringTransform.toSnakeCase(parameterName), parameterValue.get());
                }
            });
        }

        if (operationHandler.getRequestDescriptors() != null) {
            operationHandler.getRequestDescriptors().forEach((key, descriptor) -> {
                if (descriptor.isMap() && descriptor.shouldGetKeys()) {
                    SdkRequest request = context.request();
                    Optional<Map> parameterValue = request.getValueForField(key, Map.class);
                    if (parameterValue.isPresent()) {
                        String renameTo = descriptor.getRenameTo() != null ? descriptor.getRenameTo() : key;
                        parameters.put(StringTransform.toSnakeCase(renameTo), parameterValue.get().keySet());
                    }
                } else if (descriptor.isList() && descriptor.shouldGetCount()) {
                    SdkRequest request = context.request();
                    Optional<List> parameterValue = request.getValueForField(key, List.class);
                    if (parameterValue.isPresent()) {
                        String renameTo = descriptor.getRenameTo() != null ? descriptor.getRenameTo() : key;
                        parameters.put(StringTransform.toSnakeCase(renameTo), parameterValue.get().size());
                    }
                }
            });
        }

        return parameters;
    }

    private HashMap<String, Object> extractResponseParameters(
        Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        HashMap<String, Object> parameters = new HashMap<>();

        AWSOperationHandler operationHandler = getOperationHandler(executionAttributes);
        if (operationHandler == null) {
            return parameters;
        }

        if (operationHandler.getResponseParameters() != null) {
            operationHandler.getResponseParameters().forEach((parameterName) -> {
                SdkResponse response = context.response();
                Optional<Object> parameterValue = response.getValueForField(parameterName, Object.class);
                if (parameterValue.isPresent()) {
                    parameters.put(StringTransform.toSnakeCase(parameterName), parameterValue.get());
                }
            });
        }

        if (operationHandler.getResponseDescriptors() != null) {
            operationHandler.getResponseDescriptors().forEach((key, descriptor) -> {
                if (descriptor.isMap() && descriptor.shouldGetKeys()) {
                    SdkResponse response = context.response();
                    Optional<Map> parameterValue = response.getValueForField(key, Map.class);
                    if (parameterValue.isPresent()) {
                        String renameTo = descriptor.getRenameTo() != null ? descriptor.getRenameTo() : key;
                        parameters.put(StringTransform.toSnakeCase(renameTo), parameterValue.get().keySet());
                    }
                } else if (descriptor.isList() && descriptor.shouldGetCount()) {
                    SdkResponse response = context.response();
                    Optional<List> parameterValue = response.getValueForField(key, List.class);
                    if (parameterValue.isPresent()) {
                        String renameTo = descriptor.getRenameTo() != null ? descriptor.getRenameTo() : key;
                        parameters.put(StringTransform.toSnakeCase(renameTo), parameterValue.get().size());
                    }
                }
            });
        }

        return parameters;
    }

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        AWSXRayRecorder recorder = getRecorder();
        Entity origin = recorder.getTraceEntity();

        Subsegment subsegment = recorder.beginSubsegment(executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME));
        subsegment.setNamespace(Namespace.AWS.toString());
        subsegment.putAws(EntityDataKeys.AWS.OPERATION_KEY,
                          executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME));
        Region region = executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
        if (region != null) {
            subsegment.putAws(EntityDataKeys.AWS.REGION_KEY, region.id());
        }
        subsegment.putAllAws(extractRequestParameters(context, executionAttributes));
        if (accountId != null) {
            subsegment.putAws(EntityDataKeys.AWS.ACCOUNT_ID_SUBSEGMENT_KEY, accountId);
        }

        recorder.setTraceEntity(origin);
        // store the subsegment in the AWS SDK's executionAttributes so it can be accessed across threads
        executionAttributes.putAttribute(entityKey, subsegment);
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpRequest httpRequest = context.httpRequest();

        Subsegment subsegment = executionAttributes.getAttribute(entityKey);
        if (!subsegment.shouldPropagate()) {
            return httpRequest;
        }

        boolean isSampled = subsegment.getParentSegment().isSampled();
        TraceHeader header = new TraceHeader(
                subsegment.getParentSegment().getTraceId(),
                isSampled ? subsegment.getId() : null,
                isSampled ? TraceHeader.SampleDecision.SAMPLED : TraceHeader.SampleDecision.NOT_SAMPLED
        );

        return httpRequest.toBuilder().appendHeader(TraceHeader.HEADER_KEY, header.toString()).build();
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        Subsegment subsegment = executionAttributes.getAttribute(entityKey);
        if (subsegment == null) {
            return;
        }

        Map<String, Object> awsProperties = subsegment.getAws();
        // beforeTransmission is run before every API call attempt
        // default value is set to -1 and will always be -1 on the first API call attempt
        // this value will be incremented by 1, so initial run will have a stored retryCount of 0
        int retryCount = (int) awsProperties.getOrDefault(EntityDataKeys.AWS.RETRIES_KEY, -1);
        awsProperties.put(EntityDataKeys.AWS.RETRIES_KEY, retryCount + 1);
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        Subsegment subsegment = executionAttributes.getAttribute(entityKey);
        if (subsegment == null) {
            return;
        }

        populateRequestId(subsegment, context);
        populateSubsegmentWithResponse(subsegment, context.httpResponse());
        subsegment.putAllAws(extractResponseParameters(context, executionAttributes));

        getRecorder().endSubsegment(subsegment);
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        Subsegment subsegment = executionAttributes.getAttribute(entityKey);
        if (subsegment == null) {
            return;
        }

        populateSubsegmentException(subsegment, context);
        populateRequestId(subsegment, context);
        if (context.httpResponse().isPresent()) {
            populateSubsegmentWithResponse(subsegment, context.httpResponse().get());
        }

        getRecorder().endSubsegment(subsegment);
    }

    private HashMap<String, Object> extractHttpResponseParameters(SdkHttpResponse httpResponse) {
        HashMap<String, Object> parameters = new HashMap<>();
        HashMap<String, Object> responseData = new HashMap<>();

        responseData.put(EntityDataKeys.HTTP.STATUS_CODE_KEY, httpResponse.statusCode());

        try {
            if (httpResponse.headers().containsKey(EntityHeaderKeys.HTTP.CONTENT_LENGTH_HEADER)) {
                responseData.put(EntityDataKeys.HTTP.CONTENT_LENGTH_KEY, Long.parseLong(
                        httpResponse.headers().get(EntityHeaderKeys.HTTP.CONTENT_LENGTH_HEADER).get(0))
                );
            }
        } catch (NumberFormatException e) {
            logger.warn("Unable to parse Content-Length header.", e);
        }

        parameters.put(EntityDataKeys.HTTP.RESPONSE_KEY, responseData);
        return parameters;
    }

    private void setRemoteForException(Subsegment subsegment, Throwable exception) {
        subsegment.getCause().getExceptions().forEach((e) -> {
            if (e.getThrowable() == exception) {
                e.setRemote(true);
            }
        });
    }

    private void populateSubsegmentException(Subsegment subsegment, Context.FailedExecution context) {
        Throwable exception = context.exception();
        subsegment.addException(exception);

        int statusCode = -1;
        if (exception instanceof SdkServiceException) {
            statusCode = ((SdkServiceException) exception).statusCode();
            subsegment.getCause().setMessage(exception.getMessage());
            if (((SdkServiceException) exception).isThrottlingException()) {
                subsegment.setThrottle(true);
                // throttling errors are considered client-side errors
                subsegment.setError(true);
            }
            setRemoteForException(subsegment, exception);
        } else if (context.httpResponse().isPresent()) {
            statusCode = context.httpResponse().get().statusCode();
        }

        if (statusCode == -1) {
            return;
        }

        if (statusCode >= 400 && statusCode < 500) {
            subsegment.setFault(false);
            subsegment.setError(true);
            if (statusCode == 429) {
                subsegment.setThrottle(true);
            }
        } else if (statusCode >= 500) {
            subsegment.setFault(true);
        }
    }

    private void populateRequestId(
            Subsegment subsegment,
            Optional<SdkResponse> response,
            Optional<SdkHttpResponse> httpResponse,
            Throwable exception
    ) {
        String requestId = null;

        if (exception != null) {
            requestId = extractRequestIdFromThrowable(exception);
        }
        if (requestId == null || requestId.equals(UNKNOWN_REQUEST_ID)) {
            requestId = extractRequestIdFromResponse(response);
        }
        if (requestId == null || requestId.equals(UNKNOWN_REQUEST_ID)) {
            requestId = extractRequestIdFromHttp(httpResponse);
        }
        if (requestId != null && !requestId.equals(UNKNOWN_REQUEST_ID)) {
            subsegment.putAws(EntityDataKeys.AWS.REQUEST_ID_KEY, requestId);
        }
    }

    private void populateRequestId(Subsegment subsegment, Context.FailedExecution context) {
        populateRequestId(subsegment, context.response(), context.httpResponse(), context.exception());
    }

    private void populateRequestId(Subsegment subsegment, Context.AfterExecution context) {
        populateRequestId(subsegment, Optional.of(context.response()), Optional.of(context.httpResponse()), null);
    }


    private String extractRequestIdFromHttp(Optional<SdkHttpResponse> httpResponse) {
        if (!httpResponse.isPresent()) {
            return null;
        }
        return extractRequestIdFromHttp(httpResponse.get());
    }

    private String extractRequestIdFromHttp(SdkHttpResponse httpResponse) {
        Map<String, List<String>> headers = httpResponse.headers();
        Set<String> headerKeys = headers.keySet();
        String requestIdKey = headerKeys
                .stream()
                .filter(key -> REQUEST_ID_KEYS.contains(key.toLowerCase()))
                .findFirst()
                .orElse(null);
        return requestIdKey != null ? headers.get(requestIdKey).get(0) : null;
    }

    private String extractExtendedRequestIdFromHttp(SdkHttpResponse httpResponse) {
        Map<String, List<String>> headers = httpResponse.headers();
        return headers.containsKey(EntityHeaderKeys.AWS.EXTENDED_REQUEST_ID_HEADER) ?
                headers.get(EntityHeaderKeys.AWS.EXTENDED_REQUEST_ID_HEADER).get(0) : null;
    }

    private String extractRequestIdFromThrowable(Throwable exception) {
        if (exception instanceof SdkServiceException) {
            return ((SdkServiceException) exception).requestId();
        }
        return null;
    }

    private String extractRequestIdFromResponse(Optional<SdkResponse> response) {
        if (response.isPresent()) {
            return extractRequestIdFromResponse(response.get());
        }

        return null;
    }

    private String extractRequestIdFromResponse(SdkResponse response) {
        if (response instanceof AwsResponse) {
            return ((AwsResponse) response).responseMetadata().requestId();
        }

        return null;
    }

    private void populateSubsegmentWithResponse(Subsegment subsegment, SdkHttpResponse httpResponse) {
        if (subsegment == null || httpResponse == null) {
            return;
        }

        String extendedRequestId = extractExtendedRequestIdFromHttp(httpResponse);
        if (extendedRequestId != null) {
            subsegment.putAws(EntityDataKeys.AWS.EXTENDED_REQUEST_ID_KEY, extendedRequestId);
        }
        subsegment.putAllHttp(extractHttpResponseParameters(httpResponse));
    }

    private AWSXRayRecorder getRecorder() {
        if (recorder == null) {
            recorder = AWSXRay.getGlobalRecorder();
        }
        return recorder;
    }
}
