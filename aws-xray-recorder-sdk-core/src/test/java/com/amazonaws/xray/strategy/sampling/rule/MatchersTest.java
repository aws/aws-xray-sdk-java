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

import com.amazonaws.xray.strategy.sampling.GetSamplingRulesResponse.SamplingRule;
import com.amazonaws.xray.strategy.sampling.SamplingRequest;
import com.amazonaws.xray.strategy.sampling.rule.RuleBuilder;
import com.amazonaws.xray.strategy.sampling.rule.RuleBuilder.RuleParams;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MatchersTest {

    @Test
    void testSimpleMatch() {
        RuleParams params = new RuleParams(null);
        params.attributes = null;
        params.host = "192.168.1.1";
        params.serviceName = "www.foo.com";
        params.httpMethod = "POST";
        params.resourceArn = "arn:aws:service:us-east-1:111111111111:resource";
        params.urlPath = "/bar/123";
        params.serviceType = "AWS::EC2::Instance";
        SamplingRule rule = RuleBuilder.createRule(params);

        SamplingRequest req = new SamplingRequest(
            "role-arn",
            "arn:aws:service:us-east-1:111111111111:resource",
            "www.foo.com",
            "192.168.1.1",
            "POST",
            "/bar/123",
            "AWS::EC2::Instance",
            null
        );

        Matchers m = new Matchers(rule);

        Assertions.assertTrue(m.match(req));
    }

    @Test
    void testSimpleMismatch() {
        RuleParams params = new RuleParams(null);
        params.attributes = null;
        params.host = "192.168.1.1";
        params.serviceName = "www.foo.com";
        params.httpMethod = "POST";
        params.resourceArn = "arn:aws:service:us-east-1:111111111111:resource";
        params.urlPath = "/bar/123";
        params.serviceType = "AWS::EC2::Instance";
        SamplingRule rule = RuleBuilder.createRule(params);

        SamplingRequest req = new SamplingRequest(
            "role-arn",
            "arn:aws:service:us-east-1:111111111111:resource",
            "www.bar.com",
            "192.168.1.1",
            "POST",
            "/bar/123",
            "AWS::EC2::Instance",
            null
        );

        Matchers m = new Matchers(rule);

        Assertions.assertFalse(m.match(req));
    }

    @Test
    void testFullGlobMatch() {
        Map<String, String> ruleAttributes = new HashMap<>();
        ruleAttributes.put("ip", "*");
        ruleAttributes.put("compression", "*");

        Map<String, String> reqAttributes = new HashMap<>();
        reqAttributes.put("ip", "127.0.0.1");
        reqAttributes.put("compression", "gzip");
        reqAttributes.put("encoding", "json");

        RuleParams params = new RuleParams(null);
        params.attributes = ruleAttributes;
        params.host = "*";
        params.serviceName = "*";
        params.httpMethod = "*";
        params.resourceArn = "*";
        params.urlPath = "*";
        params.serviceType = "*";
        SamplingRule rule = RuleBuilder.createRule(params);

        SamplingRequest req = new SamplingRequest(
            "role-arn",
            "arn:aws:service:us-east-1:111111111111:resource",
            "www.foo.com",
            "192.168.1.1",
            "GET",
            "/baz/bar",
            "AWS::EC2::Instance",
            reqAttributes
        );

        Matchers m = new Matchers(rule);

        Assertions.assertTrue(m.match(req));
    }

    @Test
    void testPartialGlobMatch() {
        Map<String, String> ruleAttributes = new HashMap<>();
        ruleAttributes.put("ip", "127.*.1");
        ruleAttributes.put("compression", "*");

        Map<String, String> reqAttributes = new HashMap<>();
        reqAttributes.put("ip", "127.0.0.1");
        reqAttributes.put("compression", "gzip");
        reqAttributes.put("encoding", "json");

        RuleParams params = new RuleParams(null);
        params.attributes = ruleAttributes;
        params.host = "*";
        params.serviceName = "*.foo.*";
        params.httpMethod = "*";
        params.resourceArn = "*";
        params.urlPath = "/bar/*";
        params.serviceType = "AWS::EC2::Instance";
        SamplingRule rule = RuleBuilder.createRule(params);

        SamplingRequest req = new SamplingRequest(
            "role-arn",
            "arn:aws:service:us-east-1:111111111111:resource",
            "www.foo.com",
            "192.168.1.1",
            "GET",
            "/bar/baz",
            "AWS::EC2::Instance",
            reqAttributes
        );

        Matchers m = new Matchers(rule);

        Assertions.assertTrue(m.match(req));
    }

    @Test
    void testPartialGlobMismatch() {
        RuleParams params = new RuleParams(null);
        params.attributes = null;
        params.host = "*";
        params.serviceName = "*.foo.*";
        params.httpMethod = "*";
        params.resourceArn = "*";
        params.urlPath = "/bar/*";
        params.serviceType = "AWS::EC2::Instance";
        SamplingRule rule = RuleBuilder.createRule(params);

        SamplingRequest req = new SamplingRequest(
            "role-arn",
            "arn:aws:service:us-east-1:111111111111:resource",
            "www.bar.com",
            "192.168.1.1",
            "GET",
            "/foo/baz",
            "AWS::EC2::Instance",
            null
        );

        Matchers m = new Matchers(rule);

        Assertions.assertFalse(m.match(req));
    }

    @Test
    void testPartialAttributeGlobMismatch() {
        Map<String, String> ruleAttributes = new HashMap<>();
        ruleAttributes.put("ip", "127.*.0");
        ruleAttributes.put("compression", "*");

        Map<String, String> reqAttributes = new HashMap<>();
        reqAttributes.put("ip", "127.0.0.1");
        reqAttributes.put("compression", "gzip");
        reqAttributes.put("encoding", "json");

        RuleParams params = new RuleParams(null);
        params.attributes = ruleAttributes;
        params.host = "*";
        params.serviceName = "*";
        params.httpMethod = "*";
        params.resourceArn = "*";
        params.urlPath = "*";
        params.serviceType = "AWS::EC2::Instance";
        SamplingRule rule = RuleBuilder.createRule(params);

        SamplingRequest req = new SamplingRequest(
            "role-arn",
            "arn:aws:service:us-east-1:111111111111:resource",
            "www.bar.com",
            "192.168.1.1",
            "GET",
            "/foo/baz",
            "AWS::EC2::Instance",
            reqAttributes
        );

        Matchers m = new Matchers(rule);

        Assertions.assertFalse(m.match(req));
    }

    @Test
    void testPartialRequestMismatch() {
        RuleParams params = new RuleParams(null);
        params.attributes = null;
        params.host = "192.168.1.1";
        params.serviceName = "www.foo.com";
        params.httpMethod = "POST";
        params.resourceArn = "arn:aws:service:us-east-1:111111111111:resource";
        params.urlPath = "/bar/123";
        params.serviceType = "AWS::EC2::Instance";
        SamplingRule rule = RuleBuilder.createRule(params);

        SamplingRequest req = new SamplingRequest(
            "role-arn",
            "arn:aws:service:us-east-1:111111111111:resource",
            "www.bar.com",
            null,
            "POST",
            "/bar/123",
            "AWS::EC2::Instance",
            null
        );

        Matchers m = new Matchers(rule);

        Assertions.assertFalse(m.match(req));
    }

}
