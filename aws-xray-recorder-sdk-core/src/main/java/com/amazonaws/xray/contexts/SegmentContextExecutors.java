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

package com.amazonaws.xray.contexts;

import static com.amazonaws.xray.utils.LooseValidations.checkNotNull;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Segment;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link Executor}s that will mount a segment before running a command. When switching threads, for example when instrumenting an
 * asynchronous application, it is recommended to use one of these {@link Executor}s to make sure callbacks have the trace segment
 * available.
 *
 * <pre>{@code
 * DynamoDbAsyncClient client = DynamoDbAsyncClient.create();
 *
 * client.getItem(request).thenComposeAsync(response -> {
 *     // If we did not provide the segment context executor, this request would not be traced correctly.
 *     return client.getItem(request2);
 * }, SegmentContextExecutors.newSegmentContextExecutor()); *
 * }</pre>
 */
public final class SegmentContextExecutors {

    private SegmentContextExecutors() {
    }

    /**
     * Returns a new {@link Executor} which will run any tasks with the current segment mounted.
     */
    public static Executor newSegmentContextExecutor() {
        return newSegmentContextExecutor(AWSXRay.getCurrentSegmentOptional().orElse(null));
    }

    /**
     * Returns a new {@link Executor} which will run any tasks with the provided {@link Segment} mounted. If {@code segment} is
     * {@code null}, the executor is a no-op.
     */
    public static Executor newSegmentContextExecutor(@Nullable Segment segment) {
        return newSegmentContextExecutor(AWSXRay.getGlobalRecorder(), segment);
    }

    /**
     * Returns a new {@link Executor} which will run any tasks with the provided {@link Segment} mounted in the provided
     * {@link AWSXRayRecorder}. If {@code segment} is {@code null}, the executor is a no-op.
     */
    public static Executor newSegmentContextExecutor(AWSXRayRecorder recorder, @Nullable Segment segment) {
        if (!checkNotNull(recorder, "recorder") || segment == null) {
            return Runnable::run;
        }
        return new SegmentContextExecutor(recorder, segment);
    }

    private static class SegmentContextExecutor implements Executor {
        private final AWSXRayRecorder recorder;
        private final Segment segment;

        private SegmentContextExecutor(AWSXRayRecorder recorder, Segment segment) {
            this.recorder = recorder;
            this.segment = segment;
        }

        @Override
        public void execute(Runnable command) {
            segment.run(command, recorder);
        }
    }
}
