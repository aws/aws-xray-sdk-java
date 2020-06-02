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

/**
 * Per-Rule statistics maintained by the sampler. Used for making sampling
 * decisions and for reporting rule usage to X-Ray.
 */

public class Statistics {

    private int requests;

    private int sampled;

    private int borrowed;

    public void reset() {
        requests = 0;
        sampled = 0;
        borrowed = 0;
    }

    public void incRequest() {
        requests++;
    }

    public void incSampled() {
        sampled++;
    }

    public void incBorrowed() {
        borrowed++;
    }

    public int getRequests() {
        return requests;
    }

    public int getSampled() {
        return sampled;
    }

    public int getBorrowed() {
        return borrowed;
    }

}
