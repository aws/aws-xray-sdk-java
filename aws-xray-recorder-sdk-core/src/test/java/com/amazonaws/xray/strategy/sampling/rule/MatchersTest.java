package com.amazonaws.xray.strategy.sampling.rule;

import com.amazonaws.services.xray.model.SamplingRule;
import com.amazonaws.xray.strategy.sampling.SamplingRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MatchersTest {

    @Test
    public void testSimpleMatch() {
        SamplingRule rule = new SamplingRule()
                .withAttributes(null)
                .withHost("192.168.1.1")
                .withServiceName("www.foo.com")
                .withHTTPMethod("POST")
                .withResourceARN("arn:aws:service:us-east-1:111111111111:resource")
                .withURLPath("/bar/123")
                .withServiceType("AWS::EC2::Instance");

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

        Assert.assertTrue(m.match(req));
    }

    @Test
    public void testSimpleMismatch() {
        SamplingRule rule = new SamplingRule()
                .withAttributes(null)
                .withHost("192.168.1.1")
                .withServiceName("www.foo.com")
                .withHTTPMethod("POST")
                .withResourceARN("arn:aws:service:us-east-1:111111111111:resource")
                .withURLPath("/bar/123")
                .withServiceType("AWS::EC2::Instance");

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

        Assert.assertFalse(m.match(req));
    }

    @Test
    public void testFullGlobMatch() {
        Map<String, String> ruleAttributes = new HashMap<>();
        ruleAttributes.put("ip", "*");
        ruleAttributes.put("compression", "*");

        Map<String, String> reqAttributes = new HashMap<>();
        reqAttributes.put("ip", "127.0.0.1");
        reqAttributes.put("compression", "gzip");
        reqAttributes.put("encoding", "json");

        SamplingRule rule = new SamplingRule()
                .withAttributes(ruleAttributes)
                .withHost("*")
                .withServiceName("*")
                .withHTTPMethod("*")
                .withResourceARN("*")
                .withURLPath("*")
                .withServiceType("*");

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

        Assert.assertTrue(m.match(req));
    }

    @Test
    public void testPartialGlobMatch() {
        Map<String, String> ruleAttributes = new HashMap<>();
        ruleAttributes.put("ip", "127.*.1");
        ruleAttributes.put("compression", "*");

        Map<String, String> reqAttributes = new HashMap<>();
        reqAttributes.put("ip", "127.0.0.1");
        reqAttributes.put("compression", "gzip");
        reqAttributes.put("encoding", "json");

        SamplingRule rule = new SamplingRule()
                .withAttributes(ruleAttributes)
                .withHost("*")
                .withServiceName("*.foo.*")
                .withHTTPMethod("*")
                .withResourceARN("*")
                .withURLPath("/bar/*")
                .withServiceType("AWS::EC2::Instance");

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

        Assert.assertTrue(m.match(req));
    }

    @Test
    public void testPartialGlobMismatch() {
        SamplingRule rule = new SamplingRule()
                .withAttributes(null)
                .withHost("*")
                .withServiceName("*.foo.*")
                .withHTTPMethod("*")
                .withResourceARN("*")
                .withURLPath("/bar/*")
                .withServiceType("AWS::EC2::Instance");

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

        Assert.assertFalse(m.match(req));
    }

    @Test
    public void testPartialAttributeGlobMismatch() {
        Map<String, String> ruleAttributes = new HashMap<>();
        ruleAttributes.put("ip", "127.*.0");
        ruleAttributes.put("compression", "*");

        Map<String, String> reqAttributes = new HashMap<>();
        reqAttributes.put("ip", "127.0.0.1");
        reqAttributes.put("compression", "gzip");
        reqAttributes.put("encoding", "json");

        SamplingRule rule = new SamplingRule()
                .withAttributes(ruleAttributes)
                .withHost("*")
                .withServiceName("*")
                .withHTTPMethod("*")
                .withResourceARN("*")
                .withURLPath("*")
                .withServiceType("AWS::EC2::Instance");

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

        Assert.assertFalse(m.match(req));
    }

    @Test
    public void testPartialRequestMismatch() {
         SamplingRule rule = new SamplingRule()
                 .withAttributes(null)
                 .withHost("192.168.1.1")
                 .withServiceName("www.foo.com")
                 .withHTTPMethod("POST")
                 .withResourceARN("arn:aws:service:us-east-1:111111111111:resource")
                 .withURLPath("/bar/123")
                 .withServiceType("AWS::EC2::Instance");

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

         Assert.assertFalse(m.match(req));
    }

}
