package com.amazonaws.xray;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceID;

/**
 * Static helper class which holds reference to a global client and provides a static interface for invoking methods on the client.
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

    public static <R> R createSegment(String name, Function<Segment, R> function) {
        return globalRecorder.createSegment(name, function);
    }

    public static void createSegment(String name, Consumer<Segment> consumer) {
        globalRecorder.createSegment(name, consumer);
    }

    public static <R> R createSegment(String name, Supplier<R> supplier) {
        return globalRecorder.createSegment(name, supplier);
    }

    public static void createSegment(String name, Runnable runnable) {
        globalRecorder.createSegment(name, runnable);
    }

    public static <R> R createSubsegment(String name, Function<Subsegment, R> function) {
        return globalRecorder.createSubsegment(name, function);
    }

    public static void createSubsegment(String name, Consumer<Subsegment> consumer) {
        globalRecorder.createSubsegment(name, consumer);
    }

    public static <R> R createSubsegment(String name, Supplier<R> supplier) {
        return globalRecorder.createSubsegment(name, supplier);
    }

    public static void createSubsegment(String name, Runnable runnable) {
        globalRecorder.createSubsegment(name, runnable);
    }

    public static Segment beginSegment(String name) {
        return globalRecorder.beginSegment(name);
    }

    public static Segment beginSegment(String name, TraceID traceId, String parentId) {
        return globalRecorder.beginSegment(name, traceId, parentId);
    }

    public static Segment beginDummySegment() {
        return globalRecorder.beginDummySegment();
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

    public String currentEntityId() {
        return globalRecorder.currentEntityId();
    }

    public TraceID currentTraceId() {
        return globalRecorder.currentTraceId();
    }

    public static String currentFormattedId() { return globalRecorder.currentFormattedId(); }

    public static Segment getCurrentSegment() {
        return globalRecorder.getCurrentSegment();
    }

    public static Optional<Segment> getCurrentSegmentOptional() {
        return globalRecorder.getCurrentSegmentOptional();
    }

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

    public static void setTraceEntity(Entity entity) {
        globalRecorder.setTraceEntity(entity);
    }

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
     * @deprecated use {@link #sendSubsegment()} instead
     */
    @Deprecated
    public static boolean sendSubegment(Subsegment subsegment) {
        return AWSXRay.sendSubsegment(subsegment);
    }
    
    public static boolean sendSubsegment(Subsegment subsegment) {
        return globalRecorder.sendSubsegment(subsegment);
    }

}
