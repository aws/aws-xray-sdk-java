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

package com.amazonaws.xray.javax.servlet;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.StringValidator;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceHeader.SampleDecision;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.strategy.DynamicSegmentNamingStrategy;
import com.amazonaws.xray.strategy.FixedSegmentNamingStrategy;
import com.amazonaws.xray.strategy.SegmentNamingStrategy;
import com.amazonaws.xray.strategy.sampling.SamplingRequest;
import com.amazonaws.xray.strategy.sampling.SamplingResponse;
import com.amazonaws.xray.strategy.sampling.SamplingStrategy;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AWSXRayServletFilter implements javax.servlet.Filter {

    private static final Log logger = LogFactory.getLog(AWSXRayServletFilter.class);

    @Nullable
    private String segmentOverrideName;
    @Nullable
    private String segmentDefaultName;

    @MonotonicNonNull
    private SegmentNamingStrategy segmentNamingStrategy;
    @MonotonicNonNull
    private AWSXRayRecorder recorder;
    private final AWSXRayServletAsyncListener listener;

    /**
     * Warning: this no-args constructor should not be used directly. This constructor is made available for use from within
     * {@code web.xml} and other declarative file-based instantiations.
     */
    public AWSXRayServletFilter() {
        this((SegmentNamingStrategy) null);
    }

    public AWSXRayServletFilter(String fixedSegmentName) {
        this(new FixedSegmentNamingStrategy(fixedSegmentName));
    }

    public AWSXRayServletFilter(@Nullable SegmentNamingStrategy segmentNamingStrategy) {
        this(segmentNamingStrategy, null);
    }

    // TODO(anuraaga): Better define lifecycle relationship between this listener and the filter.
    @SuppressWarnings("nullness")
    public AWSXRayServletFilter(@Nullable SegmentNamingStrategy segmentNamingStrategy, @Nullable AWSXRayRecorder recorder) {
        // Will be configured by web.xml otherwise.
        if (segmentNamingStrategy != null) {
            this.segmentNamingStrategy = segmentNamingStrategy;
        }
        // Will be configured by web.xml otherwise.
        if (recorder != null) {
            this.recorder = recorder;
        }
        this.listener = new AWSXRayServletAsyncListener(this, recorder);
    }

    /**
     * @return the segmentOverrideName
     */
    @Nullable
    public String getSegmentOverrideName() {
        return segmentOverrideName;
    }

    /**
     * @param segmentOverrideName
     *            the segmentOverrideName to set
     */
    public void setSegmentOverrideName(String segmentOverrideName) {
        this.segmentOverrideName = segmentOverrideName;
    }

    /**
     * @return the segmentDefaultName
     */
    @Nullable
    public String getSegmentDefaultName() {
        return segmentDefaultName;
    }

    /**
     * @param segmentDefaultName
     *            the segmentDefaultName to set
     */
    public void setSegmentDefaultName(String segmentDefaultName) {
        this.segmentDefaultName = segmentDefaultName;
    }

    /**
     *
     *
     * @param config
     *  the filter configuration. There are various init-params which may be passed on initialization. The values in init-params
     *  will create segment naming strategies which override those passed in constructors.
     *
     * <ul>
     *
     * <li>
     * <b>fixedName</b> A String value used as the fixedName parameter for a created
     * {@link com.amazonaws.xray.strategy.FixedSegmentNamingStrategy}. Used only if the {@code dynamicNamingFallbackName}
     * init-param is not set.
     * </li>
     *
     * <li>
     * <b>dynamicNamingFallbackName</b> A String value used as the fallbackName parameter for a created
     * {@link com.amazonaws.xray.strategy.DynamicSegmentNamingStrategy}.
     * </li>
     *
     * <li>
     * <b>dynamicNamingRecognizedHosts</b> A String value used as the recognizedHosts parameter for a created
     * {@link com.amazonaws.xray.strategy.DynamicSegmentNamingStrategy}.
     * </li>
     *
     * </ul>
     *
     * @throws ServletException
     *  when a segment naming strategy is not provided in constructor arguments nor in init-params.
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        String fixedName = config.getInitParameter("fixedName");
        String dynamicNamingFallbackName = config.getInitParameter("dynamicNamingFallbackName");
        String dynamicNamingRecognizedHosts = config.getInitParameter("dynamicNamingRecognizedHosts");
        if (StringValidator.isNotNullOrBlank(dynamicNamingFallbackName)) {
            if (StringValidator.isNotNullOrBlank(dynamicNamingRecognizedHosts)) {
                segmentNamingStrategy = new DynamicSegmentNamingStrategy(dynamicNamingFallbackName, dynamicNamingRecognizedHosts);
            } else {
                segmentNamingStrategy = new DynamicSegmentNamingStrategy(dynamicNamingFallbackName);
            }
        } else if (StringValidator.isNotNullOrBlank(fixedName)) {
            segmentNamingStrategy = new FixedSegmentNamingStrategy(fixedName);
        } else if (null == segmentNamingStrategy) {
            throw new ServletException(
                "The AWSXRayServletFilter requires either a fixedName init-param or an instance of SegmentNamingStrategy. "
                + "Add an init-param tag to the AWSXRayServletFilter's declaration in web.xml, using param-name: 'fixedName'. "
                + "Alternatively, pass an instance of SegmentNamingStrategy to the AWSXRayServletFilter constructor.");
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(
        ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug("AWSXRayServletFilter is beginning to process request: " + request.toString());
        }
        Segment segment = preFilter(request, response);

        try {
            chain.doFilter(request, response);
        } catch (Throwable e) {
            segment.addException(e);
            throw e;
        } finally {
            if (request.isAsyncStarted()) {
                request.setAttribute(AWSXRayServletAsyncListener.ENTITY_ATTRIBUTE_KEY, segment);
                try {
                    request.getAsyncContext().addListener(listener);
                    if (recorder != null) {
                        recorder.clearTraceEntity();
                    }
                } catch (IllegalStateException ise) {
                    // race condition that occurs when async processing finishes before adding the listener
                    postFilter(request, response);
                }
            } else {
                postFilter(request, response);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("AWSXRayServletFilter is finished processing request: " + request.toString());
            }
        }
    }

    @Nullable
    private HttpServletRequest castServletRequest(ServletRequest request) {
        try {
            return (HttpServletRequest) request;
        } catch (ClassCastException cce) {
            logger.warn("Unable to cast ServletRequest to HttpServletRequest.", cce);
        }
        return null;
    }

    @Nullable
    private HttpServletResponse castServletResponse(ServletResponse response) {
        try {
            return (HttpServletResponse) response;
        } catch (ClassCastException cce) {
            logger.warn("Unable to cast ServletResponse to HttpServletResponse.", cce);
        }
        return null;
    }

    private Optional<TraceHeader> getTraceHeader(HttpServletRequest request) {
        String traceHeaderString = request.getHeader(TraceHeader.HEADER_KEY);
        if (null != traceHeaderString) {
            return Optional.of(TraceHeader.fromString(traceHeaderString));
        }
        return Optional.empty();
    }

    private Optional<String> getHost(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Host"));
    }

    private Optional<String> getClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getRemoteAddr());
    }

    private Optional<String> getXForwardedFor(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null) {
            return Optional.of(forwarded.split(",")[0].trim());
        }
        return Optional.empty();
    }

    private Optional<String> getUserAgent(HttpServletRequest request) {
        String userAgentHeaderString = request.getHeader("User-Agent");
        if (null != userAgentHeaderString) {
            return Optional.of(userAgentHeaderString);
        }
        return Optional.empty();
    }

    private Optional<Integer> getContentLength(HttpServletResponse response) {
        String contentLengthString = response.getHeader("Content-Length");
        if (null != contentLengthString && !contentLengthString.isEmpty()) {
            try {
                return Optional.of(Integer.parseInt(contentLengthString));
            } catch (NumberFormatException nfe) {
                logger.debug("Unable to parse Content-Length header from HttpServletResponse.", nfe);
            }
        }
        return Optional.empty();
    }

    private String getSegmentName(HttpServletRequest httpServletRequest) {
        if (segmentNamingStrategy == null) {
            throw new RuntimeException(
                "The AWSXRayServletFilter requires either a fixedName init-param or an instance of SegmentNamingStrategy. "
                + "Add an init-param tag to the AWSXRayServletFilter's declaration in web.xml, using param-name: 'fixedName'. "
                + "Alternatively, pass an instance of SegmentNamingStrategy to the AWSXRayServletFilter constructor.");
        }
        return segmentNamingStrategy.nameForRequest(httpServletRequest);
    }

    private SamplingResponse fromSamplingStrategy(HttpServletRequest httpServletRequest) {
        AWSXRayRecorder recorder = getRecorder();
        SamplingRequest samplingRequest = new SamplingRequest(
            getSegmentName(httpServletRequest),
            getHost(httpServletRequest).orElse(null), httpServletRequest.getRequestURI(), httpServletRequest.getMethod(),
            recorder.getOrigin());
        SamplingResponse sample = recorder.getSamplingStrategy().shouldTrace(samplingRequest);
        return sample;
    }

    private SampleDecision getSampleDecision(SamplingResponse sample) {
        if (sample.isSampled()) {
            logger.debug("Sampling strategy decided SAMPLED.");
            return SampleDecision.SAMPLED;
        } else {
            logger.debug("Sampling strategy decided NOT_SAMPLED.");
            return SampleDecision.NOT_SAMPLED;
        }
    }

    private AWSXRayRecorder getRecorder() {
        if (recorder == null) {
            recorder = AWSXRay.getGlobalRecorder();
        }
        return recorder;
    }

    public Segment preFilter(ServletRequest request, ServletResponse response) {
        AWSXRayRecorder recorder = getRecorder();
        HttpServletRequest httpServletRequest = castServletRequest(request);
        if (httpServletRequest == null) {
            logger.warn("Null value for incoming HttpServletRequest. Beginning NoOpSegment.");
            return recorder.beginNoOpSegment();
        }

        Optional<TraceHeader> incomingHeader = getTraceHeader(httpServletRequest);
        SamplingStrategy samplingStrategy = recorder.getSamplingStrategy();

        if (logger.isDebugEnabled() && incomingHeader.isPresent()) {
            logger.debug("Incoming trace header received: " + incomingHeader.get().toString());
        }

        SamplingResponse samplingResponse = fromSamplingStrategy(httpServletRequest);

        SampleDecision sampleDecision = incomingHeader.isPresent()
                                        ? incomingHeader.get().getSampled() : getSampleDecision(samplingResponse);
        if (SampleDecision.REQUESTED.equals(sampleDecision) || SampleDecision.UNKNOWN.equals(sampleDecision)) {
            sampleDecision = getSampleDecision(samplingResponse);
        }

        TraceID traceId = null;
        String parentId = null;
        if (incomingHeader.isPresent()) {
            TraceHeader header = incomingHeader.get();
            traceId = header.getRootTraceId();
            parentId = header.getParentId();
        }

        final Segment created;
        if (SampleDecision.SAMPLED.equals(sampleDecision)) {
            String segmentName = getSegmentName(httpServletRequest);
            created = traceId != null
                      ? recorder.beginSegment(segmentName, traceId, parentId)
                      : recorder.beginSegment(segmentName);
            if (samplingResponse.getRuleName().isPresent()) {
                logger.debug("Sampling strategy decided to use rule named: " + samplingResponse.getRuleName().get() + ".");
                created.setRuleName(samplingResponse.getRuleName().get());
            }
        } else { //NOT_SAMPLED
            String segmentName = getSegmentName(httpServletRequest);
            if (samplingStrategy.isForcedSamplingSupported()) {
                created = traceId != null
                          ? recorder.beginSegment(segmentName, traceId, parentId)
                          : recorder.beginSegment(segmentName);
                created.setSampled(false);
            } else {
                logger.debug("Creating Dummy Segment");
                created = traceId != null ? recorder.beginNoOpSegment(traceId) : recorder.beginNoOpSegment();
            }
        }

        Map<String, Object> requestAttributes = new HashMap<String, Object>();
        requestAttributes.put("url", httpServletRequest.getRequestURL().toString());
        requestAttributes.put("method", httpServletRequest.getMethod());

        Optional<String> userAgent = getUserAgent(httpServletRequest);
        if (userAgent.isPresent()) {
            requestAttributes.put("user_agent", userAgent.get());
        }

        Optional<String> xForwardedFor = getXForwardedFor(httpServletRequest);
        if (xForwardedFor.isPresent()) {
            requestAttributes.put("client_ip", xForwardedFor.get());
            requestAttributes.put("x_forwarded_for", true);
        } else {
            Optional<String> clientIp = getClientIp(httpServletRequest);
            if (clientIp.isPresent()) {
                requestAttributes.put("client_ip", clientIp.get());
            }
        }

        created.putHttp("request", requestAttributes);

        HttpServletResponse httpServletResponse = castServletResponse(response);
        if (httpServletResponse == null) {
            return created;
        }

        final TraceHeader responseHeader;
        if (incomingHeader.isPresent()) {
            // create a new header, and use the incoming header so we know what to do in regards to sending back the sampling
            // decision.
            responseHeader = new TraceHeader(created.getTraceId());
            if (SampleDecision.REQUESTED == incomingHeader.get().getSampled()) {
                responseHeader.setSampled(created.isSampled() ? SampleDecision.SAMPLED : SampleDecision.NOT_SAMPLED);
            }
        } else {
            // Create a new header, we're the tracing root. We wont return the sampling decision.
            responseHeader = new TraceHeader(created.getTraceId());
        }
        httpServletResponse.addHeader(TraceHeader.HEADER_KEY, responseHeader.toString());

        return created;
    }

    public void postFilter(ServletRequest request, ServletResponse response) {
        AWSXRayRecorder recorder = getRecorder();
        Segment segment = recorder.getCurrentSegment();
        if (null != segment) {
            HttpServletResponse httpServletResponse = castServletResponse(response);

            if (null != httpServletResponse) {
                Map<String, Object> responseAttributes = new HashMap<String, Object>();

                int responseCode = httpServletResponse.getStatus();
                switch (responseCode / 100) {
                    case 4:
                        segment.setError(true);
                        if (responseCode == 429) {
                            segment.setThrottle(true);
                        }
                        break;
                    case 5:
                        segment.setFault(true);
                        break;
                    default:
                        break;
                }
                responseAttributes.put("status", responseCode);


                Optional<Integer> contentLength = getContentLength(httpServletResponse);
                if (contentLength.isPresent()) {
                    responseAttributes.put("content_length", contentLength.get());
                }

                segment.putHttp("response", responseAttributes);
            }

            recorder.endSegment();
        }
    }
}

