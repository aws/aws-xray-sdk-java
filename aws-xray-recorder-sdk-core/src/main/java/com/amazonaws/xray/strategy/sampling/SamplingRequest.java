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

package com.amazonaws.xray.strategy.sampling;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the input request to the sampler. Contains attributes relevant to
 * making sampling decisions.
 */
public class SamplingRequest {

    private static final String ARN_SEPARATOR = ":";
    private static final int ACCOUNT_INDEX = 4;

    private String roleArn;

    @Nullable
    private String resourceArn;

    @Nullable
    private final String service;

    @Nullable
    private final String host;

    @Nullable
    private final String method;

    @Nullable
    private final String url;

    @Nullable
    private String serviceType;

    private final Map<String, String> attributes;

    /**
     * @param roleArn
     *            the role of the customer requesting a sampling decision. Must
     *            not be null.
     * @param resourceArn
     *            the resource for which a sampling decision is being requested.
     *            Ex. "arn:aws:execute-api:us-east-1:1234566789012:qsxrty/test/GET/foo/bar/*".
     * @param service
     *            the service name for which a sampling decision is being requested.
     *            Ex. "www.foo.com".
     * @param host
     *            the host name extracted from the incoming request Host header.
     * @param method
     *            the Http Method extracted from the Request-Line.
     * @param url
     *            the URL extracted from the Request-Line.
     * @param serviceType
     *            the service type.
     * @param attributes
     *            list of key-value pairs generated on a per-request basis.
     */
    public SamplingRequest(
            String roleArn,
            @Nullable String resourceArn,
            @Nullable String service,
            @Nullable String host,
            @Nullable String method,
            @Nullable String url,
            @Nullable String serviceType,
            @Nullable Map<String, String> attributes
    ) {
        Objects.requireNonNull(roleArn, "RoleARN can not be null");

        this.roleArn = roleArn;
        this.resourceArn = resourceArn;
        this.service = service;
        this.host = host;
        this.method = method;
        this.url = url;
        this.serviceType = serviceType;
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
    }

    public SamplingRequest(
        @Nullable String service,
        @Nullable String host,
        @Nullable String url,
        @Nullable String method,
        @Nullable String serviceType) {
        this.service = service;
        this.host = host;
        this.url = url;
        this.method = method;
        this.serviceType = serviceType;

        roleArn = "";
        attributes = Collections.emptyMap();
    }

    public Optional<String> getAccountId() {
        String[] splits = roleArn.split(ARN_SEPARATOR, ACCOUNT_INDEX + 2);

        if (splits.length < ACCOUNT_INDEX + 2) {
            return Optional.empty();
        }

        return Optional.of(splits[ACCOUNT_INDEX]);
    }

    public String getRoleARN() {
        return roleArn;
    }

    public Optional<String> getResourceARN() {
        return Optional.ofNullable(resourceArn);
    }

    public Optional<String> getService() {
        return Optional.ofNullable(service);
    }

    public Optional<String> getMethod() {
        return Optional.ofNullable(method);
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    public Optional<String> getServiceType() {
        return Optional.ofNullable(serviceType);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
}
