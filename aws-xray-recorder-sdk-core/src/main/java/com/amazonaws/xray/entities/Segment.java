package com.amazonaws.xray.entities;

import java.util.Map;

import com.amazonaws.xray.exceptions.AlreadyEmittedException;

public interface Segment extends Entity {

    /**
     * Ends the segment. Sets the end time to the current time. Sets inProgress to false.
     *
     * @return true if 1) the reference count is less than or equal to zero and 2) sampled is true
     */
    public boolean end();

    /**
     * @return the sampled
     */
    public boolean isSampled();

    /**
     * @param sampled
     *            the sampled to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void setSampled(boolean sampled);

    /**
     * @return the resourceArn
     */
    public String getResourceArn();

    /**
     * @param resourceArn
     *            the resourceArn to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void setResourceArn(String resourceArn);

    /**
     * @return the user
     */
    public String getUser();

    /**
     * @param user
     *            the user to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void setUser(String user);

    /**
     * @return the origin
     */
    public String getOrigin();

    /**
     * @param origin
     *            the origin to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void setOrigin(String origin);

    /**
     * @return the service
     */
    public Map<String, Object> getService();

    /**
     * @param service
     *            the service to set
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void setService(Map<String, Object> service);

    /**
     * @return the annotations
     */
    public Map<String, Object> getAnnotations();

    /**
     * Puts information about this service.
     *
     * @param key
     *            the key under which the service information is stored
     * @param object
     *            the service information
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putService(String key, Object object);

    /**
     * Puts information about this service.
     *
     * @param all
     *            the service information to set.
     *
     * @throws AlreadyEmittedException
     *             if the entity has already been emitted to the X-Ray daemon and the ContextMissingStrategy of the AWSXRayRecorder used to create this entity is configured to throw exceptions
     */
    public void putAllService(Map<String, Object> all);

    public void setRuleName(String name);

    @Override
    public Segment getParentSegment();

    @Override
    public void close();

}
