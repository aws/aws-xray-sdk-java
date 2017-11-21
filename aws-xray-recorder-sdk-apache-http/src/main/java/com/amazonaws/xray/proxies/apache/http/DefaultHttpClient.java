package com.amazonaws.xray.proxies.apache.http;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Subsegment;

/*
 * @deprecated Apache 4.3
 *
 * Wraps and overrides {@code org.apache.http.impl.client.DefaultHttpClient}'s execute() methods. Accesses the global recorder upon each invocation to generate {@code Segment}s.
 *
 * Only overrides those signatures which directly invoke doExecute. Other execute() signatures are wrappers which call these overriden methods.
 *
 */
@Deprecated
public class DefaultHttpClient extends org.apache.http.impl.client.DefaultHttpClient {

    private AWSXRayRecorder getRecorder() {
        return AWSXRay.getGlobalRecorder();
    }

    @Override
    public CloseableHttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        Subsegment subsegment = getRecorder().beginSubsegment(target.getHostName());
        try {
            if (null != subsegment) {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(target, request));
            }
            CloseableHttpResponse response = super.execute(target, request, context);
            if (null != subsegment) {
                TracedResponseHandler.addResponseInformation(subsegment, response);
            }
            return response;
        } catch (Exception e) {
            if (null != subsegment) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            if (null != subsegment) {
                getRecorder().endSubsegment();
            }
        }
    }

    @Override
    public CloseableHttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
        Subsegment subsegment = getRecorder().beginSubsegment(TracedHttpClient.determineTarget(request).getHostName());
        try {
            if (null != subsegment) {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(request));
            }
            CloseableHttpResponse response = super.execute(request, context);
            if (null != subsegment) {
                TracedResponseHandler.addResponseInformation(subsegment, response);
            }
            return response;
        } catch (Exception e) {
            if (null != subsegment) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            if (null != subsegment) {
                getRecorder().endSubsegment();
            }
        }
    }

    @Override
    public CloseableHttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        Subsegment subsegment = getRecorder().beginSubsegment(target.getHostName());
        try {
            if (null != subsegment) {
                TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(target, request));
            }
            CloseableHttpResponse response = super.execute(target, request);
            if (null != subsegment) {
                TracedResponseHandler.addResponseInformation(subsegment, response);
            }
            return response;
        } catch (Exception e) {
            if (null != subsegment) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            if (null != subsegment) {
                getRecorder().endSubsegment();
            }
        }
    }

}
