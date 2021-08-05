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
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Subsegment extends Entity {

    static Subsegment noOp(AWSXRayRecorder recorder, boolean shouldPropagate) {
        return new NoOpSubSegment(Segment.noOp(TraceID.invalid(), recorder), recorder, shouldPropagate);
    }

    static Subsegment noOp(Segment parent, AWSXRayRecorder recorder) {
        return new NoOpSubSegment(parent, recorder);
    }

    /**
     * Ends the subsegment. Sets the end time to the current time. Sets inProgress to false. Decrements its parent segment's
     * segment-reference counter.
     *
     * @return
     *  true if 1) the parent segment now has a ref. count of zero and 2) the parent segment is sampled
     */
    boolean end();

    /**
     * @return the namespace
     */
    @Override
    @Nullable
    String getNamespace();

    /**
     * @param namespace
     *            the namespace to set
     */
    @Override
    void setNamespace(String namespace);

    /**
     * @return the parentSegment
     */
    @Override
    Segment getParentSegment();

    /**
     * @param parentSegment the parentSegment to set
     */
    void setParentSegment(Segment parentSegment);

    /**
     * @return the precursorIds
     */
    Set<String> getPrecursorIds();

    /**
     * @param precursorIds the precursorIds to set
     */
    void setPrecursorIds(Set<String> precursorIds);

    /**
     * @param precursorId the precursor ID to add to the set
     */
    void addPrecursorId(String precursorId);

    /**
     * Determines if this subsegment should propagate its trace context downstream
     * @return true if its trace context should be propagated downstream, false otherwise
     */
    boolean shouldPropagate();

    /**
     * Serializes the subsegment as a standalone String with enough information for the subsegment to be streamed on its own.
     * @return
     *  the string representation of the subsegment with enouogh information for it to be streamed
     */
    String streamSerialize();

    /**
     * Pretty-serializes the subsegment as a standalone String with enough information for the subsegment to be streamed on its
     * own. Only used for debugging.
     *
     * @return
     *  the pretty string representation of the subsegment with enouogh information for it to be streamed
     */
    String prettyStreamSerialize();

    @Override
    void close();
}
