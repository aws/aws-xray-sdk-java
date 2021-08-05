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

package com.amazonaws.xray.proxies.apache.http;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceHeader.SampleDecision;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

@SuppressWarnings("deprecation")
public class TracedHttpClient extends CloseableHttpClient {

    private final CloseableHttpClient wrappedClient;

    private final AWSXRayRecorder recorder;

    /**
     * Constructs a TracedHttpClient instance using the provided client and global recorder.
     *
     * @param wrappedClient
     *            the HTTP client to wrap
     */
    public TracedHttpClient(final CloseableHttpClient wrappedClient) {
        this(wrappedClient, AWSXRay.getGlobalRecorder());
    }

    /**
     * Constructs a TracedHttpClient instance using the provided client and provided recorder.
     *
     * @param wrappedClient
     *            the HTTP client to wrap
     * @param recorder
     *            the recorder instance to use when generating subsegments around calls made by {@code wrappedClient}
     */
    public TracedHttpClient(
            final CloseableHttpClient wrappedClient,
            AWSXRayRecorder recorder) {
        this.wrappedClient = wrappedClient;
        this.recorder = recorder;
    }

    public static HttpHost determineTarget(final HttpUriRequest request) throws ClientProtocolException {
        // A null target may be acceptable if there is a default target.
        // Otherwise, the null target is detected in the director.
        HttpHost target = null;

        final URI requestUri = request.getURI();
        if (requestUri.isAbsolute()) {
            target = URIUtils.extractHost(requestUri);
            if (target == null) {
                throw new ClientProtocolException("URI does not specify a valid host name: "
                        + requestUri);
            }
        }
        return target;
    }

    public static String getUrl(HttpUriRequest request) {
        return request.getURI().toString();
    }

    public static String getUrl(HttpHost target, HttpRequest request) {
        String uri = request.getRequestLine().getUri();

        try {
            URI requestUri = new URI(uri);
            if (requestUri.isAbsolute()) {
                return requestUri.toString();
            }
        } catch (URISyntaxException ex) {
            // Not a valid URI
        }

        return target.toURI() + uri;
    }

    public static void addRequestInformation(Subsegment subsegment, HttpRequest request, String url) {
        subsegment.setNamespace(Namespace.REMOTE.toString());
        Segment parentSegment = subsegment.getParentSegment();

        if (subsegment.shouldPropagate()) {
            TraceHeader header = new TraceHeader(parentSegment.getTraceId(),
                parentSegment.isSampled() ? subsegment.getId() : null,
                parentSegment.isSampled() ? SampleDecision.SAMPLED : SampleDecision.NOT_SAMPLED);
            request.addHeader(TraceHeader.HEADER_KEY, header.toString());
        }

        Map<String, Object> requestInformation = new HashMap<>();

        requestInformation.put("url", url);
        requestInformation.put("method", request.getRequestLine().getMethod());

        subsegment.putHttp("request", requestInformation);
    }

    @FunctionalInterface
    public interface HttpSupplier<R> { //Necessary to define a get() method that may throw checked exceptions
        R get() throws IOException, ClientProtocolException;
    }

    private <R> R wrapHttpSupplier(Subsegment subsegment, HttpSupplier<R> supplier) throws IOException, ClientProtocolException {
        try {
            return supplier.get();
        } catch (Exception e) {
            if (null != subsegment) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            if (null != subsegment) {
                recorder.endSubsegment();
            }
        }
    }

    @Override
    public CloseableHttpResponse execute(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws IOException, ClientProtocolException {
        Subsegment subsegment = recorder.beginSubsegment(target.getHostName());
        return wrapHttpSupplier(
            subsegment, () -> {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(target, request));
                CloseableHttpResponse response = wrappedClient.execute(target, request, context);
                TracedResponseHandler.addResponseInformation(subsegment, response);
                return response;
            });
    }

    @Override
    public CloseableHttpResponse execute(
            final HttpUriRequest request,
            final HttpContext context) throws IOException, ClientProtocolException {
        Subsegment subsegment = recorder.beginSubsegment(determineTarget(request).getHostName());
        return wrapHttpSupplier(
            subsegment, () -> {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(request));
                CloseableHttpResponse response = wrappedClient.execute(request, context);
                TracedResponseHandler.addResponseInformation(subsegment, response);
                return response;
            });
    }

    @Override
    public CloseableHttpResponse execute(
            final HttpUriRequest request) throws IOException, ClientProtocolException {
        Subsegment subsegment = recorder.beginSubsegment(determineTarget(request).getHostName());
        return wrapHttpSupplier(
            subsegment, () -> {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(request));
                CloseableHttpResponse response = wrappedClient.execute(request);
                TracedResponseHandler.addResponseInformation(subsegment, response);
                return response;
            });
    }

    @Override
    public CloseableHttpResponse execute(
            final HttpHost target,
            final HttpRequest request) throws IOException, ClientProtocolException {
        Subsegment subsegment = recorder.beginSubsegment(target.getHostName());
        return wrapHttpSupplier(
            subsegment, () -> {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(target, request));
                CloseableHttpResponse response = wrappedClient.execute(target, request);
                TracedResponseHandler.addResponseInformation(subsegment, response);
                return response;
            });
    }

    @Override
    public <T> T execute(final HttpUriRequest request,
            final ResponseHandler<? extends T> responseHandler) throws IOException,
            ClientProtocolException {
        Subsegment subsegment = recorder.beginSubsegment(determineTarget(request).getHostName());
        return wrapHttpSupplier(subsegment, () -> {
            TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(request));
            TracedResponseHandler<? extends T> wrappedHandler = new TracedResponseHandler<>(responseHandler);
            return wrappedClient.execute(request, wrappedHandler);
        });
    }

    @Override
    public <T> T execute(final HttpUriRequest request,
            final ResponseHandler<? extends T> responseHandler, final HttpContext context)
            throws IOException, ClientProtocolException {
        Subsegment subsegment = recorder.beginSubsegment(determineTarget(request).getHostName());
        return wrapHttpSupplier(
            subsegment, () -> {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(request));
                TracedResponseHandler<? extends T> wrappedHandler = new TracedResponseHandler<>(responseHandler);
                return wrappedClient.execute(request, wrappedHandler, context);
            });
    }

    @Override
    public <T> T execute(final HttpHost target, final HttpRequest request,
            final ResponseHandler<? extends T> responseHandler) throws IOException,
            ClientProtocolException {
        Subsegment subsegment = recorder.beginSubsegment(target.getHostName());
        return wrapHttpSupplier(
            subsegment, () -> {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(target, request));
                TracedResponseHandler<? extends T> wrappedHandler = new TracedResponseHandler<>(responseHandler);
                return wrappedClient.execute(target, request, wrappedHandler);
            });
    }

    @Override
    public <T> T execute(final HttpHost target, final HttpRequest request,
            final ResponseHandler<? extends T> responseHandler, final HttpContext context)
            throws IOException, ClientProtocolException {
        Subsegment subsegment = recorder.beginSubsegment(target.getHostName());
        return wrapHttpSupplier(
            subsegment, () -> {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(target, request));
                TracedResponseHandler<? extends T> wrappedHandler = new TracedResponseHandler<>(responseHandler);
                return wrappedClient.execute(target, request, wrappedHandler, context);
            });
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return wrappedClient.getConnectionManager();
    }

    @Override
    public HttpParams getParams() {
        return wrappedClient.getParams();
    }

    @Override
    public void close() throws IOException {
        wrappedClient.close();
    }

    @Override
    protected CloseableHttpResponse doExecute(
        HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        // gross hack to call the wrappedClient's doExecute...
        // see line 67 of Apache's CloseableHttpClient
        return wrappedClient.execute(httpHost, httpRequest, httpContext);
    }

}
