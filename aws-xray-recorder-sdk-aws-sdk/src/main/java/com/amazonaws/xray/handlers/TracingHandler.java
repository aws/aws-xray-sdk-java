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

package com.amazonaws.xray.handlers;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.handlers.HandlerContextKey;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.retry.RetryUtils;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.EntityDataKeys;
import com.amazonaws.xray.entities.EntityHeaderKeys;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceHeader.SampleDecision;
import com.amazonaws.xray.handlers.config.AWSOperationHandler;
import com.amazonaws.xray.handlers.config.AWSOperationHandlerManifest;
import com.amazonaws.xray.handlers.config.AWSServiceHandlerManifest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extension of {@code RequestHandler2} that intercepts requests made by {@code AmazonWebServiceClient}s and generates
 * corresponding subsegments. Operation-level customization of this request handler is by default performed based on the
 * information contained in the file at {@code "/com/amazonaws/xray/handlers/DefaultOperationParameterWhitelist.json")}.
 */
public class TracingHandler extends RequestHandler2 {

    private static final Log logger = LogFactory.getLog(TracingHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private static final URL DEFAULT_OPERATION_PARAMETER_WHITELIST =
        TracingHandler.class.getResource("/com/amazonaws/xray/handlers/DefaultOperationParameterWhitelist.json");

    private static final String GETTER_METHOD_NAME_PREFIX = "get";

    private static final String S3_SERVICE_NAME = "Amazon S3";
    private static final String S3_PRESIGN_REQUEST = "GeneratePresignedUrl";
    private static final String S3_REQUEST_ID_HEADER_KEY = "x-amz-request-id";

    private static final String XRAY_SERVICE_NAME = "AWSXRay";
    private static final String XRAY_SAMPLING_RULE_REQUEST = "GetSamplingRules";
    private static final String XRAY_SAMPLING_TARGET_REQUEST = "GetSamplingTargets";

    private static final String REQUEST_ID_SUBSEGMENT_KEY = "request_id";

    private static final HandlerContextKey<Entity> ENTITY_KEY = new HandlerContextKey<>("AWS X-Ray Entity");
    private static final HandlerContextKey<Long> EXECUTING_THREAD_KEY = new HandlerContextKey<>("AWS X-Ray Executing Thread ID");

    private static final String TO_SNAKE_CASE_REGEX = "([a-z])([A-Z]+)";
    private static final String TO_SNAKE_CASE_REPLACE = "$1_$2";

    private final String accountId;

    private AWSServiceHandlerManifest awsServiceHandlerManifest;
    
    private AWSXRayRecorder recorder;

    public TracingHandler() {
        this(AWSXRay.getGlobalRecorder(), null, null);
    }
    
    public TracingHandler(AWSXRayRecorder recorder) {
        this(recorder, null, null);
    }

    public TracingHandler(String accountId) {
        this(AWSXRay.getGlobalRecorder(), accountId, null);
    }

    public TracingHandler(AWSXRayRecorder recorder, String accountId) {
        this(recorder, accountId, null);
    }

    public TracingHandler(URL operationParameterWhitelist) {
        this(AWSXRay.getGlobalRecorder(), null, operationParameterWhitelist);
    }

    public TracingHandler(AWSXRayRecorder recorder, URL operationParameterWhitelist) {
        this(recorder, null, operationParameterWhitelist);
    }

    public TracingHandler(String accountId, URL operationParameterWhitelist) {
        this(AWSXRay.getGlobalRecorder(), accountId, operationParameterWhitelist);
    }

    public TracingHandler(AWSXRayRecorder recorder, String accountId, URL operationParameterWhitelist) {
        this.recorder = recorder;
        this.accountId = accountId;
        initRequestManifest(operationParameterWhitelist);
    }

    private void initRequestManifest(URL operationParameterWhitelist) {
        if (null != operationParameterWhitelist) {
            try {
                awsServiceHandlerManifest = MAPPER.readValue(operationParameterWhitelist, AWSServiceHandlerManifest.class);
                return;
            } catch (IOException e) {
                logger.error("Unable to parse operation parameter whitelist at " + operationParameterWhitelist.getPath()
                             + ". Falling back to default operation parameter whitelist at "
                             + TracingHandler.DEFAULT_OPERATION_PARAMETER_WHITELIST.getPath()
                             + ".", e);
            }
        }
        try {
            awsServiceHandlerManifest = MAPPER.readValue(
                TracingHandler.DEFAULT_OPERATION_PARAMETER_WHITELIST, AWSServiceHandlerManifest.class);
        } catch (IOException e) {
            logger.error("Unable to parse default operation parameter whitelist at "
                         + TracingHandler.DEFAULT_OPERATION_PARAMETER_WHITELIST.getPath()
                         + ". This will affect this handler's ability to capture AWS operation parameter information.", e);
        }
    }

    private boolean isSubsegmentDuplicate(Optional<Subsegment> subsegment, Request<?> request) {
        return subsegment.isPresent() &&
            Namespace.AWS.toString().equals(subsegment.get().getNamespace()) &&
            (null != extractServiceName(request) && extractServiceName(request).equals(subsegment.get().getName()));
    }

    @Override
    public AmazonWebServiceRequest beforeExecution(AmazonWebServiceRequest request) {
        lazyLoadRecorder();
        request.addHandlerContext(ENTITY_KEY, recorder.getTraceEntity());
        request.addHandlerContext(EXECUTING_THREAD_KEY, Thread.currentThread().getId());
        return request;
    }

    @Override
    public void beforeRequest(Request<?> request) {
        String serviceName = extractServiceName(request);
        String operationName = extractOperationName(request);

        if (S3_SERVICE_NAME.equals(serviceName) && S3_PRESIGN_REQUEST.equals(operationName)) {
            return;
        }

        if (XRAY_SERVICE_NAME.equals(serviceName) && (XRAY_SAMPLING_RULE_REQUEST.equals(operationName)
                || XRAY_SAMPLING_TARGET_REQUEST.equals(operationName))) {
            return;
        }

        if (isSubsegmentDuplicate(recorder.getCurrentSubsegmentOptional(), request)) {
            return;
        }
        Entity entityContext = request.getHandlerContext(ENTITY_KEY);
        if (null != entityContext) {
            recorder.setTraceEntity(entityContext);
        }
        Subsegment currentSubsegment = recorder.beginSubsegment(serviceName);
        currentSubsegment.putAllAws(extractRequestParameters(request));
        currentSubsegment.putAws(EntityDataKeys.AWS.OPERATION_KEY, operationName);
        if (null != accountId) {
            currentSubsegment.putAws(EntityDataKeys.AWS.ACCOUNT_ID_SUBSEGMENT_KEY, accountId);
        }
        currentSubsegment.setNamespace(Namespace.AWS.toString());

        if (recorder.getCurrentSegment() != null && recorder.getCurrentSubsegment().shouldPropagate()) {
            TraceHeader header =
                new TraceHeader(recorder.getCurrentSegment().getTraceId(),
                                recorder.getCurrentSegment().isSampled() ? currentSubsegment.getId() : null,
                                recorder.getCurrentSegment().isSampled() ? SampleDecision.SAMPLED : SampleDecision.NOT_SAMPLED);
            request.addHeader(TraceHeader.HEADER_KEY, header.toString());
        }
    }

    private String extractServiceName(Request<?> request) {
        return request.getServiceName();
    }

    private String extractOperationName(Request<?> request) {
        String ret = request.getOriginalRequest().getClass().getSimpleName();
        ret = ret.substring(0, ret.length() - 7); // remove 'Request'
        return ret;
    }

    private static String toSnakeCase(String camelCase) {
        return camelCase.replaceAll(TO_SNAKE_CASE_REGEX, TO_SNAKE_CASE_REPLACE).toLowerCase();
    }

    private HashMap<String, Object> extractRequestParameters(Request<?> request) {
        HashMap<String, Object> ret = new HashMap<>();
        if (null == awsServiceHandlerManifest) {
            return ret;
        }

        AWSOperationHandlerManifest serviceHandler =
            awsServiceHandlerManifest.getOperationHandlerManifest(extractServiceName(request));
        if (null == serviceHandler) {
            return ret;
        }

        AWSOperationHandler operationHandler = serviceHandler.getOperationHandler(extractOperationName(request));
        if (null == operationHandler) {
            return ret;
        }

        Object originalRequest = request.getOriginalRequest();

        if (null != operationHandler.getRequestParameters()) {
            operationHandler.getRequestParameters().forEach(parameterName -> {
                try {
                    Object parameterValue = originalRequest
                        .getClass().getMethod(GETTER_METHOD_NAME_PREFIX + parameterName).invoke(originalRequest);
                    if (null != parameterValue) {
                        ret.put(TracingHandler.toSnakeCase(parameterName), parameterValue);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    logger.error("Error getting request parameter: " + parameterName, e);
                }
            });
        }

        if (null != operationHandler.getRequestDescriptors()) {
            operationHandler.getRequestDescriptors().forEach((requestKeyName, requestDescriptor) -> {
                try {
                    if (requestDescriptor.isMap() && requestDescriptor.shouldGetKeys()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parameterValue =
                            (Map<String, Object>) originalRequest
                                .getClass()
                                .getMethod(GETTER_METHOD_NAME_PREFIX + requestKeyName).invoke(originalRequest);
                        if (null != parameterValue) {
                            String renameTo =
                                null != requestDescriptor.getRenameTo() ? requestDescriptor.getRenameTo() : requestKeyName;
                            ret.put(TracingHandler.toSnakeCase(renameTo), parameterValue.keySet());
                        }
                    } else if (requestDescriptor.isList() && requestDescriptor.shouldGetCount()) {
                        @SuppressWarnings("unchecked")
                        List<Object> parameterValue =
                            (List<Object>) originalRequest
                                .getClass().getMethod(GETTER_METHOD_NAME_PREFIX + requestKeyName).invoke(originalRequest);
                        if (null != parameterValue) {
                            String renameTo =
                                null != requestDescriptor.getRenameTo() ? requestDescriptor.getRenameTo() : requestKeyName;
                            ret.put(TracingHandler.toSnakeCase(renameTo), parameterValue.size());
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
                    logger.error("Error getting request parameter: " + requestKeyName, e);
                }
            });
        }

        return ret;
    }

    private HashMap<String, Object> extractResponseParameters(Request<?> request, Object response) {
        HashMap<String, Object> ret = new HashMap<>();
        if (null == awsServiceHandlerManifest) {
            return ret;
        }

        AWSOperationHandlerManifest serviceHandler =
            awsServiceHandlerManifest.getOperationHandlerManifest(extractServiceName(request));
        if (null == serviceHandler) {
            return ret;
        }

        AWSOperationHandler operationHandler = serviceHandler.getOperationHandler(extractOperationName(request));
        if (null == operationHandler) {
            return ret;
        }

        if (null != operationHandler.getResponseParameters()) {
            operationHandler.getResponseParameters().forEach(parameterName -> {
                try {
                    Object parameterValue = response
                        .getClass().getMethod(GETTER_METHOD_NAME_PREFIX + parameterName).invoke(response);
                    if (null != parameterValue) {
                        ret.put(TracingHandler.toSnakeCase(parameterName), parameterValue);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    logger.error("Error getting response parameter: " + parameterName, e);
                }
            });
        }

        if (null != operationHandler.getResponseDescriptors()) {
            operationHandler.getResponseDescriptors().forEach((responseKeyName, responseDescriptor) -> {
                try {
                    if (responseDescriptor.isMap() && responseDescriptor.shouldGetKeys()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parameterValue =
                            (Map<String, Object>) response
                                .getClass().getMethod(GETTER_METHOD_NAME_PREFIX + responseKeyName).invoke(response);
                        if (null != parameterValue) {
                            String renameTo =
                                null != responseDescriptor.getRenameTo() ? responseDescriptor.getRenameTo() : responseKeyName;
                            ret.put(TracingHandler.toSnakeCase(renameTo), parameterValue.keySet());
                        }
                    } else if (responseDescriptor.isList() && responseDescriptor.shouldGetCount()) {
                        @SuppressWarnings("unchecked")
                        List<Object> parameterValue =
                            (List<Object>) response
                                .getClass().getMethod(GETTER_METHOD_NAME_PREFIX + responseKeyName).invoke(response);
                        if (null != parameterValue) {
                            String renameTo =
                                null != responseDescriptor.getRenameTo() ? responseDescriptor.getRenameTo() : responseKeyName;
                            ret.put(TracingHandler.toSnakeCase(renameTo), parameterValue.size());
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
                    logger.error("Error getting request parameter: " + responseKeyName, e);
                }
            });
        }

        return ret;
    }

    private HashMap<String, Object> extractHttpResponseInformation(AmazonServiceException ase) {
        HashMap<String, Object> ret = new HashMap<>();
        HashMap<String, Object> response = new HashMap<>();

        response.put(EntityDataKeys.HTTP.STATUS_CODE_KEY, ase.getStatusCode());
        try {
            if (null != ase.getHttpHeaders() && null != ase.getHttpHeaders().get(EntityHeaderKeys.HTTP.CONTENT_LENGTH_HEADER)) {
                response.put(EntityDataKeys.HTTP.CONTENT_LENGTH_KEY,
                             Long.parseLong(ase.getHttpHeaders().get(EntityHeaderKeys.HTTP.CONTENT_LENGTH_HEADER)));
            }
        } catch (NumberFormatException nfe) {
            logger.warn("Unable to parse Content-Length header.", nfe);
        }

        ret.put(EntityDataKeys.HTTP.RESPONSE_KEY, response);
        return ret;
    }

    private HashMap<String, Object> extractHttpResponseInformation(HttpResponse httpResponse) {
        HashMap<String, Object> ret = new HashMap<>();
        HashMap<String, Object> response = new HashMap<>();

        response.put(EntityDataKeys.HTTP.STATUS_CODE_KEY, httpResponse.getStatusCode());
        try {
            if (null != httpResponse.getHeaders().get(EntityHeaderKeys.HTTP.CONTENT_LENGTH_HEADER)) {
                response.put(EntityDataKeys.HTTP.CONTENT_LENGTH_KEY,
                             Long.parseLong(httpResponse.getHeaders().get(EntityHeaderKeys.HTTP.CONTENT_LENGTH_HEADER)));
            }
        } catch (NumberFormatException nfe) {
            logger.warn("Unable to parse Content-Length header.", nfe);
        }

        ret.put(EntityDataKeys.HTTP.RESPONSE_KEY, response);
        return ret;
    }

    @Override
    public void afterResponse(Request<?> request, Response<?> response) {
        if (isSubsegmentDuplicate(recorder.getCurrentSubsegmentOptional(), request)) {
            Optional<Subsegment> currentSubsegmentOptional = recorder.getCurrentSubsegmentOptional();
            if (!currentSubsegmentOptional.isPresent()) {
                return;
            }
            Subsegment currentSubsegment = currentSubsegmentOptional.get();
            populateAndEndSubsegment(currentSubsegment, request, response, null);
        }
    }

    @Override
    public void afterError(Request<?> request, Response<?> response, Exception e) {
        if (isSubsegmentDuplicate(recorder.getCurrentSubsegmentOptional(), request)) {
            Optional<Subsegment> currentSubsegmentOptional = recorder.getCurrentSubsegmentOptional();
            if (!currentSubsegmentOptional.isPresent()) {
                return;
            }
            Subsegment currentSubsegment = currentSubsegmentOptional.get();

            int statusCode = -1;

            if (null != response) {
                statusCode = response.getHttpResponse().getStatusCode();
            } else {
                if (e instanceof AmazonServiceException) {
                    AmazonServiceException ase = (AmazonServiceException) e;
                    statusCode = ase.getStatusCode();
                    // The S3 client will throw and re-swallow AmazonServiceExceptions if they have these status codes. Customers
                    // will never see the exceptions in their application code but they still travel through our
                    // TracingHandler#afterError method. We special case these status codes in order to prevent addition of the
                    // full exception object to the current subsegment. Instead, we'll just add any exception error message to the
                    // current subsegment's cause's message.
                    if ((304 == statusCode || 412 == statusCode) && S3_SERVICE_NAME.equals(ase.getServiceName())) {
                        populateAndEndSubsegment(currentSubsegment, request, response, ase);
                        return;
                    }
                    if (RetryUtils.isThrottlingException(ase)) {
                        currentSubsegment.setError(true);
                        currentSubsegment.setThrottle(true);
                    }
                }
            }

            if (-1 != statusCode) {
                int statusCodePrefix = statusCode / 100;
                if (4 == statusCodePrefix) {
                    currentSubsegment.setError(true);
                    if (429 == statusCode) {
                        currentSubsegment.setThrottle(true);
                    }
                }
            }

            currentSubsegment.addException(e);

            if (e instanceof AmazonServiceException) {
                populateAndEndSubsegment(currentSubsegment, request, response, (AmazonServiceException) e);
            } else {
                populateAndEndSubsegment(currentSubsegment, request, response);
            }
        }
    }

    private void populateAndEndSubsegment(Subsegment currentSubsegment, Request<?> request, Response<?> response) {
        if (null != response) {
            String requestId = null;
            if (response.getAwsResponse() instanceof AmazonWebServiceResult<?>) {
                // Not all services return responses extending AmazonWebServiceResult (e.g. S3)
                ResponseMetadata metadata = ((AmazonWebServiceResult<?>) response.getAwsResponse()).getSdkResponseMetadata();
                if (null != metadata) {
                    requestId = metadata.getRequestId();
                    if (null != requestId) {
                        currentSubsegment.putAws(REQUEST_ID_SUBSEGMENT_KEY, requestId);
                    }
                }
            } else if (null != response.getHttpResponse()) { // S3 does not follow request id header convention
                if (null != response.getHttpResponse().getHeader(S3_REQUEST_ID_HEADER_KEY)) {
                    currentSubsegment.putAws(REQUEST_ID_SUBSEGMENT_KEY,
                                             response.getHttpResponse().getHeader(S3_REQUEST_ID_HEADER_KEY));
                }
                if (null != response.getHttpResponse().getHeader(EntityHeaderKeys.AWS.EXTENDED_REQUEST_ID_HEADER)) {
                    currentSubsegment.putAws(EntityDataKeys.AWS.EXTENDED_REQUEST_ID_KEY,
                                             response.getHttpResponse().getHeader(
                                                 EntityHeaderKeys.AWS.EXTENDED_REQUEST_ID_HEADER));
                }
            }
            currentSubsegment.putAllAws(extractResponseParameters(request, response.getAwsResponse()));
            currentSubsegment.putAllHttp(extractHttpResponseInformation(response.getHttpResponse()));
        }

        finalizeSubsegment(request);
    }

    private void populateAndEndSubsegment(
        Subsegment currentSubsegment, Request<?> request, Response<?> response, AmazonServiceException ase) {
        if (null != response) {
            populateAndEndSubsegment(currentSubsegment, request, response);
            return;
        } else if (null != ase) {
            if (null != ase.getRequestId()) {
                currentSubsegment.putAws(REQUEST_ID_SUBSEGMENT_KEY, ase.getRequestId());
            }
            if (null != ase.getHttpHeaders() &&
                null != ase.getHttpHeaders().get(EntityHeaderKeys.AWS.EXTENDED_REQUEST_ID_HEADER)) {
                currentSubsegment.putAws(EntityDataKeys.AWS.EXTENDED_REQUEST_ID_KEY,
                                         ase.getHttpHeaders().get(EntityHeaderKeys.AWS.EXTENDED_REQUEST_ID_HEADER));
            }
            if (null != ase.getErrorMessage()) {
                currentSubsegment.getCause().setMessage(ase.getErrorMessage());
            }
            currentSubsegment.putAllHttp(extractHttpResponseInformation(ase));
        }

        finalizeSubsegment(request);
    }

    private void finalizeSubsegment(Request<?> request) {
        recorder.endSubsegment();

        Long executingThreadContext = request.getHandlerContext(EXECUTING_THREAD_KEY);
        if (executingThreadContext != null && Thread.currentThread().getId() != executingThreadContext) {
            recorder.clearTraceEntity();
        }
    }

    private void lazyLoadRecorder() {
        if (recorder != null) {
            return;
        }
        recorder = AWSXRay.getGlobalRecorder();
    }

}
