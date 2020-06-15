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

/**
 * @deprecated Use {@link SegmentNamingStrategy#fixed(String)}.
 */
@Deprecated
public class FixedSegmentNamingStrategy implements SegmentNamingStrategy {
    private static final Log logger =
        LogFactory.getLog(FixedSegmentNamingStrategy.class);

    private final String fixedName;

    /**
     * @deprecated Use {@link SegmentNamingStrategy#fixed(String)}.
     */
    // Instance method is called before the class is initialized. This can cause undefined behavior, e.g., if getOverrideName
    // accesses fixedName. This class doesn't really need to be exposed to users so we suppress for now and will clean up after
    // hiding.
    @SuppressWarnings("nullness")
    @Deprecated
    public FixedSegmentNamingStrategy(String name) {
        String overrideName = getOverrideName();
        if (overrideName != null) {
            this.fixedName = overrideName;
            if (logger.isInfoEnabled()) {
                logger.info("Environment variable " + NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY + " or system property "
                            + NAME_OVERRIDE_SYSTEM_PROPERTY_KEY
                            + " set. Overriding FixedSegmentNamingStrategy constructor argument. Segments generated with this "
                            + "strategy will be named: " + this.fixedName + ".");
            }
        } else {
            this.fixedName = name;
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
