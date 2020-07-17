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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link ReentrantLock} that does nothing.
 */
class NoOpReentrantLock extends ReentrantLock {
    static ReentrantLock get() {
        return INSTANCE;
    }

    private static final NoOpReentrantLock INSTANCE = new NoOpReentrantLock();

    @Override
    public void lock() {
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
    }

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public void unlock() {
    }

    @Override
    public int getHoldCount() {
        return 0;
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return false;
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    // Seems to be wrongly annotated by checker framework, likely since no one is crazy enough to implement a no-op lock like this
    @SuppressWarnings("nullness")
    @Override
    @Nullable
    protected Thread getOwner() {
        return null;
    }

    @Override
    protected Collection<Thread> getQueuedThreads() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasWaiters(Condition condition) {
        return false;
    }

    @Override
    public int getWaitQueueLength(Condition condition) {
        return 0;
    }

    @Override
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        return Collections.emptyList();
    }
}
