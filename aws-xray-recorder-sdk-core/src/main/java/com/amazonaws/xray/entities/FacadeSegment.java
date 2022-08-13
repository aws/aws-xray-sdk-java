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

package com.amazonaws.xray.entities;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.TraceHeader.SampleDecision;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FacadeSegment extends EntityImpl implements Segment {

    private static final String MUTATION_UNSUPPORTED_MESSAGE = "FacadeSegments cannot be mutated.";
    private static final String INFORMATION_UNAVAILABLE_MESSAGE = "This information is unavailable.";

    protected String resourceArn;
    protected String user;
    protected String origin;

    protected Map<String, Object> service;

    private final boolean sampled;

    // TODO(anuraaga): Refactor the entity relationship. There isn't a great reason to use a type hierarchy for data classes and
    // it makes the code to hard to reason about e.g., nullness.
    @SuppressWarnings("nullness")
    public FacadeSegment(
        AWSXRayRecorder recorder, @Nullable TraceID traceId, @Nullable String id, @Nullable SampleDecision sampleDecision) {
        super(recorder, "facade");
        if (traceId != null) {
            super.setTraceId(traceId);
        }
        if (id != null) {
            super.setId(id);
        }
        this.sampled = (SampleDecision.SAMPLED == sampleDecision);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     *
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void close() {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     *
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public boolean end() {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    @Override
    public boolean isRecording() {
        return sampled;
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putService(String key, Object object) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * @return a LongAdder with a value of 0, as the total size of a FacadeSegment is not tracked.
     */
    @Override
    public LongAdder getTotalSize() {
        return totalSize;
    }

    @Override
    public boolean isSampled() {
        return sampled;
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setSampled(boolean sampled) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments are not aware of their resource ARN.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public String getResourceArn() {
        throw new UnsupportedOperationException(INFORMATION_UNAVAILABLE_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setResourceArn(String resourceArn) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments are not aware of their user.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public String getUser() {
        throw new UnsupportedOperationException(INFORMATION_UNAVAILABLE_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setUser(String user) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments are not aware of their origin.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public String getOrigin() {
        throw new UnsupportedOperationException(INFORMATION_UNAVAILABLE_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setOrigin(String origin) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments are not aware of their service.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public Map<String, Object> getService() {
        throw new UnsupportedOperationException(INFORMATION_UNAVAILABLE_MESSAGE);
    }

    @Override
    public Segment getParentSegment() {
        return this;
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setId(String id) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setStartTime(double startTime) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setEndTime(double endTime) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setFault(boolean fault) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setError(boolean error) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setNamespace(String namespace) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setHttp(Map<String, Object> http) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setAws(Map<String, Object> aws) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setSql(Map<String, Object> sql) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setMetadata(Map<String, Map<String, Object>> metadata) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setAnnotations(Map<String, Object> annotations) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setParent(Entity parent) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setCreator(AWSXRayRecorder creator) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setThrottle(boolean throttle) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setInProgress(boolean inProgress) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setTraceId(@Nullable TraceID traceId) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setParentId(@Nullable String parentId) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void addException(Throwable exception) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putHttp(String key, Object value) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putAllHttp(Map<String, Object> all) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putAws(String key, Object value) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putAllAws(Map<String, Object> all) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putSql(String key, Object value) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putAllSql(Map<String, Object> all) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putAnnotation(String key, String value) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putAnnotation(String key, Number value) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putAnnotation(String key, Boolean value) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putMetadata(String key, Object object) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putMetadata(String namespace, String key, Object object) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void putAllService(Map<String, Object> all) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    /**
     * Unsupported as FacadeSegments cannot be mutated.
     * @throws UnsupportedOperationException in all cases
     */
    @Override
    public void setService(Map<String, Object> service) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

    @Override
    public void setRuleName(String name) {
        throw new UnsupportedOperationException(MUTATION_UNSUPPORTED_MESSAGE);
    }

}
