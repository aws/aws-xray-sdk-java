package com.amazonaws.xray.strategy.sampling;

import org.junit.Assert;
import org.junit.Test;

public class SamplingRequestTest {

    @Test
    public void testSuccessfulAccountIdParsing() {
        SamplingRequest req = new SamplingRequest(
                "arn:aws:iam::123456789123:role/sample-role",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Assert.assertEquals(req.getAccountId().get(), "123456789123");
    }

}
