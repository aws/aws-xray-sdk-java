package com.amazonaws.xray.plugins;

import com.amazonaws.xray.entities.AWSLogReference;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface Plugin {

    /**
     * Returns the name of the origin associated with this plugin.
     * The {@link com.amazonaws.xray.AWSXRayRecorder} contains a prioritized list of origins from least to most specific.
     *
     * @return the name of the origin associated with this plugin.
     */
    public String getOrigin();
    public String getServiceName();

    /**
     * @return true if an environment inspection determines X-Ray is operating in the correct environment for this plugin OR
     * if X-Ray cannot accurately determine if it's in this plugin's environment
     */
    default boolean isEnabled() {
        return true;
    }

    public Map<String, Object> getRuntimeContext();

    default Set<AWSLogReference> getLogReferences() {
        return Collections.emptySet();
    }

}
