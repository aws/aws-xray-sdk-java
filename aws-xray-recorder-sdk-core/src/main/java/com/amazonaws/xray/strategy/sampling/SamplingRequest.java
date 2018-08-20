package com.amazonaws.xray.strategy.sampling;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the input request to the sampler. Contains attributes relevant to
 * making sampling decisions.
 */
public class SamplingRequest {

    private static final String ARN_SEPARATOR = ":";
    private static final int ACCOUNT_INDEX = 4;

    private String roleARN;

    private String resourceARN;

    private String service;

    private String host;

    private String method;

    private String url;

    private Map<String, String> attributes;

    /**
     * @param roleARN
     *            the role of the customer requesting a sampling decision. Must
     *            not be null.
     * @param resourceARN
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
     * @param attributes
     *            list of key-value pairs generated on a per-request basis.
     */
    public SamplingRequest(
            String roleARN,
            String resourceARN,
            String service,
            String host,
            String method,
            String url,
            Map<String, String> attributes
    ) {
        Objects.requireNonNull(roleARN, "RoleARN can not be null");

        this.roleARN = roleARN;
        this.resourceARN = resourceARN;
        this.service = service;
        this.host = host;
        this.method = method;
        this.url = url;
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
    }

    public SamplingRequest(String service, String host, String method, String url) {
        this.service = service;
        this.host = host;
        this.method = method;
        this.url = url;
    }

    public Optional<String> getAccountId() {
        String[] splits = roleARN.split(ARN_SEPARATOR, ACCOUNT_INDEX + 2);

        if (splits.length < ACCOUNT_INDEX + 2) {
            return Optional.empty();
        }

        return Optional.of(splits[ACCOUNT_INDEX]);
    }

    public String getRoleARN() {
        return roleARN;
    }

    public Optional<String> getResourceARN() {
        return Optional.ofNullable(resourceARN);
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

    public Map<String, String> getAttributes() {
        return attributes;
    }

}
