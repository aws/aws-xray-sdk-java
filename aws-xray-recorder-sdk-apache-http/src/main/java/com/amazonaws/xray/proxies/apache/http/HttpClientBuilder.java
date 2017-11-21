package com.amazonaws.xray.proxies.apache.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.CloseableHttpClient;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;

/**
 * Wraps and overrides some of {@code org.apache.http.impl.client.HttpClientBuilder}'s methods. Will build a TracedHttpClient wrapping the usual CloseableHttpClient result. Uses the global recorder by default, with an option to provide an alternative.
 *
 */
public class HttpClientBuilder extends org.apache.http.impl.client.HttpClientBuilder {
    private static final Log logger = LogFactory.getLog(HttpClientBuilder.class);

    private AWSXRayRecorder recorder;

    public static HttpClientBuilder create() {
        HttpClientBuilder newBuilder = new HttpClientBuilder();
        newBuilder.setRecorder(AWSXRay.getGlobalRecorder());
        return newBuilder;
    }

    public HttpClientBuilder setRecorder(AWSXRayRecorder recorder) {
        this.recorder = recorder;
        return this;
    }

    @Override
    public CloseableHttpClient build() {
        CloseableHttpClient result = super.build();
        return new TracedHttpClient(result, recorder);
    }
}
