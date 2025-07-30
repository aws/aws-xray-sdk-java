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
import com.amazonaws.xray.strategy.sampling.GetSamplingRulesResponse.SamplingRule;
import com.amazonaws.xray.strategy.sampling.SamplingRequest;
import java.util.Collections;
import java.util.Map;

public class Matchers {

    private Map<String, String> attributes;

    private String service;

    private String method;

    private String host;

    private String url;

    private String serviceType;

    public Matchers(SamplingRule r) {
        this.host = r.getHost();
        this.service = r.getServiceName();
        this.method = r.getHttpMethod();
        this.url = r.getUrlPath();
        this.serviceType = r.getServiceType();

        this.attributes = r.getAttributes() == null ? Collections.emptyMap() : r.getAttributes();
    }

    boolean match(SamplingRequest req) {
        // Comparing against the full list of matchers can be expensive. We try to short-circuit the req as quickly
        // as possible by comparing against matchers with high variance and moving down to matchers that are almost
        // always "*".
        Map<String, String> requestAttributes = req.getAttributes();

        // Ensure that each defined attribute in the sampling rule is satisfied by the request. It is okay for the
        // request to have attributes with no corresponding match in the sampling rule.
        for (Map.Entry<String, String> a : attributes.entrySet()) {
            if (!requestAttributes.containsKey(a.getKey())) {
                return false;
            }

            if (!SearchPattern.wildcardMatch(a.getValue(), requestAttributes.get(a.getKey()))) {
                return false;
            }
        }

        // Missing string parameters from the sampling request are replaced with ""s to ensure they match against *
        // matchers.
        return SearchPattern.wildcardMatch(url, req.getUrl().orElse(""))
                && SearchPattern.wildcardMatch(service, req.getService().orElse(""))
                && SearchPattern.wildcardMatch(method, req.getMethod().orElse(""))
                && SearchPattern.wildcardMatch(host, req.getHost().orElse(""))
                && SearchPattern.wildcardMatch(serviceType, req.getServiceType().orElse(""));
    }
}
