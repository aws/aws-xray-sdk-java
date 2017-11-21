package com.amazonaws.xray.plugins;

import java.util.Map;

public interface Plugin {

    /**
     * Returns the name of the origin associated with this plugin. By default, the {@link com.amazonaws.xray.AWSXRayRecorder} will set the origin of created segments to the name provided by the latest-loaded plugin.
     *
     * @return the name of the origin associated with this plugin.
     */
    public String getOrigin();
    public String getServiceName();

    public Map<String, Object> getRuntimeContext();
}
