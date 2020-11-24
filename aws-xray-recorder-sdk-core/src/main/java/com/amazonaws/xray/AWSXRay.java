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

package com.amazonaws.xray;

import com.amazonaws.xray.contexts.SegmentContextExecutors;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceID;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static helper class which holds reference to a global client and provides a static interface for invoking methods on the
 * client.
 */
public class AWSXRay {

    private static AWSXRayRecorder globalRecorder = AWSXRayRecorderBuilder.defaultRecorder();

    /**
     * Gets the global {@code AWSXRayRecorder}. This is initialized using {@code AWSXRayRecorderBuilder.defaultRecorder()}.
     *
     * See {@link #setGlobalRecorder(AWSXRayRecorder)}.
     *
     * @return the global AWSXRayRecorder
     */
    public static AWSXRayRecorder getGlobalRecorder() {
        return globalRecorder;
    }

    /**
     * Sets the global {@code AWSXRayRecorder}.
     *
     * See {@link #getGlobalRecorder}.
     *
     * @param globalRecorder
     *  the instance of AWSXRayRecorder to set as global
     */
    public static void setGlobalRecorder(AWSXRayRecorder globalRecorder) {
        AWSXRay.globalRecorder = globalRecorder;
    }

    @Nullable
    public static <R> R createSegment(String name, Function<Segment, @Nullable R> function) {
        return globalRecorder.createSegment(name, function);
    }

    public static void createSegment(String name, Consumer<Segment> consumer) {
        globalRecorder.createSegment(name, consumer);
    }

    @Nullable
    public static <R> R createSegment(String name, Supplier<R> supplier) {
        return globalRecorder.createSegment(name, supplier);
    }

    public static void createSegment(String name, Runnable runnable) {
        globalRecorder.createSegment(name, runnable);
    }

    @Nullable
    public static <R> R createSubsegment(String name, Function<Subsegment, @Nullable R> function) {
        return globalRecorder.createSubsegment(name, function);
    }

    public static void createSubsegment(String name, Consumer<Subsegment> consumer) {
        globalRecorder.createSubsegment(name, consumer);
    }

    @Nullable
    public static <R> R createSubsegment(String name, Supplier<R> supplier) {
        return globalRecorder.createSubsegment(name, supplier);
    }

    public static void createSubsegment(String name, Runnable runnable) {
        globalRecorder.createSubsegment(name, runnable);
    }

    public static Segment beginSegmentWithSampling(String name) {
        return globalRecorder.beginSegmentWithSampling(name);
    }

    public static Segment beginSegment(String name) {
        return globalRecorder.beginSegment(name);
    }

    public static Segment beginSegment(String name, TraceID traceId, String parentId) {
        return globalRecorder.beginSegment(name, traceId, parentId);
    }

    /**
     * @deprecated Use {@code AWSXRay.getGlobalRecorder().beginNoOpSegment() }.
     */
    @Deprecated
    public static Segment beginDummySegment() {
        return globalRecorder.beginNoOpSegment();
    }

    public static void endSegment() {
        globalRecorder.endSegment();
    }

    public static Subsegment beginSubsegment(String name) {
        return globalRecorder.beginSubsegment(name);
    }

    public static void endSubsegment() {
        globalRecorder.endSubsegment();
    }

    public static void endSubsegment(@Nullable Subsegment subsegment) {
        globalRecorder.endSubsegment(subsegment);
    }

    @Nullable
    public String currentEntityId() {
        return globalRecorder.currentEntityId();
    }

    @Nullable
    public TraceID currentTraceId() {
        return globalRecorder.currentTraceId();
    }

    @Nullable
    public static String currentFormattedId() {
        return globalRecorder.currentFormattedId();
    }

    @Nullable
    public static Segment getCurrentSegment() {
        return globalRecorder.getCurrentSegment();
    }

    public static Optional<Segment> getCurrentSegmentOptional() {
        return globalRecorder.getCurrentSegmentOptional();
    }

    @Nullable
    public static Subsegment getCurrentSubsegment() {
        return globalRecorder.getCurrentSubsegment();
    }

    public static Optional<Subsegment> getCurrentSubsegmentOptional() {
        return globalRecorder.getCurrentSubsegmentOptional();
    }

    /**
     * @deprecated use {@link #setTraceEntity(Entity entity)} instead
     */
    @Deprecated
    public static void injectThreadLocal(Entity entity) {
        globalRecorder.injectThreadLocal(entity);
    }

    /**
     * @deprecated use {@link #getTraceEntity()} instead
     */
    @Deprecated
    @Nullable
    public static Entity getThreadLocal() {
        return globalRecorder.getThreadLocal();
    }

    /**
     * @deprecated use {@link #clearTraceEntity()} instead
     */
    @Deprecated
    public static void clearThreadLocal() {
        globalRecorder.clearThreadLocal();
    }

    /**
     * @deprecated Use {@link Entity#run(Runnable)} or methods in {@link SegmentContextExecutors} instead of directly setting
     * the trace entity so it can be restored correctly.
     */
    @Deprecated
    public static void setTraceEntity(Entity entity) {
        globalRecorder.setTraceEntity(entity);
    }

    @Nullable
    public static Entity getTraceEntity() {
        return globalRecorder.getTraceEntity();
    }

    public static void clearTraceEntity() {
        globalRecorder.clearTraceEntity();
    }

    public static boolean sendSegment(Segment segment) {
        return globalRecorder.sendSegment(segment);
    }

    /**
     * @deprecated use {@link #sendSubsegment(Subsegment)} instead
     */
    @Deprecated
    public static boolean sendSubegment(Subsegment subsegment) {
        return AWSXRay.sendSubsegment(subsegment);
    }

    public static boolean sendSubsegment(Subsegment subsegment) {
        return globalRecorder.sendSubsegment(subsegment);
    }

}
