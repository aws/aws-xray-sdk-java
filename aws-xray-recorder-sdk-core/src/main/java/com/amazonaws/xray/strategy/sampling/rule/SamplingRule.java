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

package com.amazonaws.xray.strategy.sampling.rule;

import com.amazonaws.xray.entities.SearchPattern;
import com.amazonaws.xray.strategy.sampling.reservoir.Reservoir;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SamplingRule {

    @Nullable
    private String host;
    @Nullable
    private String serviceName;
    @Nullable
    private String httpMethod;
    @Nullable
    private String urlPath;
    private int fixedTarget = -1;
    private float rate = -1.0f;

    private Reservoir reservoir;

    public SamplingRule() {
        this.reservoir = new Reservoir();
    }

    /**
     * Constructs a new {@code SamplingRule}. Patterns are supported in the {@code host}, {@code httpMethod}, and {@code urlPath}
     * parameters. Patterns are matched using the {@link com.amazonaws.xray.entities.SearchPattern} class.
     *
     * @param host
     *            the host name for which the rule should apply
     * @param serviceName
     *            the service name for which the rule should apply
     * @param httpMethod
     *            the http method for which the rule should apply
     * @param urlPath
     *            the urlPath for which the rule should apply
     * @param fixedTarget
     *            the number of traces per any given second for which the sampling rule will sample
     * @param rate
     *            the rate at which the rule should sample, after the fixedTarget has been reached
     */
    public SamplingRule(String host, String serviceName, String httpMethod, String urlPath, int fixedTarget, float rate) {
        this.host = host;
        this.serviceName = serviceName;
        this.httpMethod = httpMethod;
        this.urlPath = urlPath;
        this.fixedTarget = fixedTarget;
        this.rate = rate;

        this.reservoir = new Reservoir(fixedTarget);
    }

    /**
     * @return the serviceName
     */
    @Nullable
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @param serviceName
     *            the serviceName to set
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * @return the host
     */
    @Nullable
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            the host to set
     */
    public void setHost(@Nullable String host) {
        this.host = host;
    }

    /**
     * @return the httpMethod
     */
    @Nullable
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * @param httpMethod
     *            the httpMethod to set
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * @return the urlPath
     */
    @Nullable
    public String getUrlPath() {
        return urlPath;
    }

    /**
     * @param urlPath
     *            the urlPath to set
     */
    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    /**
     * @return the fixedTarget
     */
    public int getFixedTarget() {
        return fixedTarget;
    }

    /**
     * @param fixedTarget
     *            the fixedTarget to set
     */
    public void setFixedTarget(int fixedTarget) {
        this.fixedTarget = fixedTarget;
        this.reservoir = new Reservoir(fixedTarget);
    }

    /**
     * @return the rate
     */
    public float getRate() {
        return rate;
    }

    /**
     * @param rate
     *            the rate to set
     */
    public void setRate(float rate) {
        this.rate = rate;
    }

    /**
     * @return the reservoir
     */
    public Reservoir getReservoir() {
        return reservoir;
    }

    @Override
    public String toString() {
        return "\n\thost: " + host + "\n\thttp_method: " + httpMethod + "\n\turl_path: " + urlPath + "\n\tfixed_target: "
               + fixedTarget + "\n\trate: " + rate;
    }

    /**
     * Determines whether or not this sampling rule applies to the incoming request based on some of the request's parameters. Any
     * null parameters provided will be considered an implicit match. For example, {@code appliesTo(null, null, null)} will always
     * return {@code true}, for any rule.
     *
     * @param requestHost
     *            the host name for the incoming request.
     * @param requestPath
     *            the path from the incoming request
     * @param requestMethod
     *            the method used to make the incoming request
     * @return whether or not this rule applies to the incoming request
     */
    public boolean appliesTo(String requestHost, String requestPath, String requestMethod) {
        return (requestHost == null || SearchPattern.wildcardMatch(host, requestHost)) &&
               (requestPath == null || SearchPattern.wildcardMatch(urlPath, requestPath)) &&
               (requestMethod == null || SearchPattern.wildcardMatch(httpMethod, requestMethod));
    }

}
