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

package com.amazonaws.xray.strategy.jakarta;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @deprecated Use {@link SegmentNamingStrategy#fixed(String)}.
 */
@Deprecated
public class FixedSegmentNamingStrategy extends com.amazonaws.xray.strategy.interfaces.FixedSegmentNamingStrategy implements SegmentNamingStrategy {

    public FixedSegmentNamingStrategy(String name) {
        super(name);
    }

    /**
     * Returns a segment name for an incoming request.
     *
     * @param request
     *  the incoming request
     * @return
     *  the name for the segment representing the request.
     */
    @Override
    public String nameForRequest(HttpServletRequest request) {
        return fixedName;
    }
}
