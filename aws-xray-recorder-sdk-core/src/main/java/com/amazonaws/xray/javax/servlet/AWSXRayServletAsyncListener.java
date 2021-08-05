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

package com.amazonaws.xray.javax.servlet;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Entity;
import java.io.IOException;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

class AWSXRayServletAsyncListener implements AsyncListener {

    public static final String ENTITY_ATTRIBUTE_KEY = "com.amazonaws.xray.entities.Entity";

    @Nullable
    private AWSXRayRecorder recorder;
    private final AWSXRayServletFilter filter;

    // TODO(anuraaga): Better define lifecycle relationship between this listener and the filter.
    @SuppressWarnings("nullness")
    AWSXRayServletAsyncListener(@UnderInitialization AWSXRayServletFilter filter, @Nullable AWSXRayRecorder recorder) {
        this.filter = filter;
        this.recorder = recorder;
    }

    private void processEvent(AsyncEvent event) throws IOException {
        Entity entity = (Entity) event.getSuppliedRequest().getAttribute(ENTITY_ATTRIBUTE_KEY);
        entity.run(() -> {
            if (event.getThrowable() != null) {
                entity.addException(event.getThrowable());
            }
            filter.postFilter(event.getSuppliedRequest(), event.getSuppliedResponse());
        });
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        processEvent(event);
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        processEvent(event);
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        processEvent(event);
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }
}
