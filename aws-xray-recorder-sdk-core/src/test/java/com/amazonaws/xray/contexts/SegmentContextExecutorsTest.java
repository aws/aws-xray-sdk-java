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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.ThreadLocalStorage;
import com.amazonaws.xray.entities.Segment;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class SegmentContextExecutorsTest {

    private static ExecutorService backgroundExecutor;

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private volatile AWSXRayRecorder recorder;

    @Spy
    private volatile Segment current;

    @Spy
    private volatile Segment manual;

    @Spy
    private volatile Segment previous;

    @BeforeClass
    public static void startBackgroundExecutor() {
        backgroundExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void stopBackgroundExecutor() {
        backgroundExecutor.shutdown();
    }

    @Before
    public void setUp() {
        when(recorder.getTraceEntity()).thenAnswer(invocation -> ThreadLocalStorage.get());
        when(recorder.getCurrentSegmentOptional()).thenAnswer(invocation -> Optional.of(ThreadLocalStorage.get()));
        doAnswer(invocation -> {
            ThreadLocalStorage.set(invocation.getArgument(0));
            return null;
        }).when(recorder).setTraceEntity(any());
        recorder.setTraceEntity(current);
        AWSXRay.setGlobalRecorder(recorder);
    }

    @After
    public void tearDown() {
        recorder.setTraceEntity(null);
    }

    @Test
    public void currentSegmentExecutor() {
        runSegmentExecutorTest(SegmentContextExecutors.newSegmentContextExecutor(), current);
    }

    @Test
    public void segmentExecutor() {
        runSegmentExecutorTest(SegmentContextExecutors.newSegmentContextExecutor(manual), manual);
    }

    @Test
    public void segmentAndRecorderExecutor() {
        runSegmentExecutorTest(SegmentContextExecutors.newSegmentContextExecutor(recorder, manual), manual);
    }

    private void runSegmentExecutorTest(Executor segmentExecutor, Segment mounted) {
        assertThat(recorder.getTraceEntity()).isEqualTo(current);

        CountDownLatch ready = new CountDownLatch(1);

        AtomicReference<Thread> backgroundThread = new AtomicReference<>();
        CompletableFuture<?> future = CompletableFuture
            .supplyAsync(() -> {
                backgroundThread.set(Thread.currentThread());
                recorder.setTraceEntity(previous);
                try {
                    // We need to make sure callbacks are registered before we complete this future or else the callbacks will
                    // be called inline on the main thread (the one that registers them).
                    ready.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new IllegalStateException();
                }
                return null;
            }, backgroundExecutor)
            .thenAcceptAsync(unused -> {
                // Sanity check our test is actually on a different thread.
                assertThat(Thread.currentThread()).isEqualTo(backgroundThread.get());
                assertThat(recorder.getTraceEntity()).isEqualTo(mounted);
            }, segmentExecutor)
            .thenAcceptAsync(unused -> {
                assertThat(Thread.currentThread()).isEqualTo(backgroundThread.get());
                assertThat(recorder.getTraceEntity()).isEqualTo(previous);
            }, backgroundExecutor);

        ready.countDown();
        future.join();

        assertThat(recorder.getTraceEntity()).isEqualTo(current);
    }
}
