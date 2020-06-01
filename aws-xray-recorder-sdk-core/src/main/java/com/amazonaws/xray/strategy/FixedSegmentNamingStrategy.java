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

package com.amazonaws.xray.strategy;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FixedSegmentNamingStrategy implements SegmentNamingStrategy {
    private static final Log logger =
        LogFactory.getLog(FixedSegmentNamingStrategy.class);

    private String fixedName;

    /**
     *
     *
     * @param fixedName
     *  the fixed name to use for all segments generated for incoming requests. This will be overriden by the value of the {@code AWS_XRAY_TRACING_NAME} environment variable or {@code com.amazonaws.xray.strategy.tracingName} system property, if either are set to a non-empty value.
     */
    public FixedSegmentNamingStrategy(String fixedName) {
        this.fixedName = fixedName;
        String overrideName = getOverrideName();
        if (null != overrideName) {
            this.fixedName = overrideName;
            if (logger.isInfoEnabled()) {
                logger.info("Environment variable " + NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY + " or system property " + NAME_OVERRIDE_SYSTEM_PROPERTY_KEY + " set. Overriding FixedSegmentNamingStrategy constructor argument. Segments generated with this strategy will be named: " + this.fixedName + ".");
            }
        }
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
