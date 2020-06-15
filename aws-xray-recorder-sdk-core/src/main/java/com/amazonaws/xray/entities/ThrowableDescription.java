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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ThrowableDescription {
    @Nullable
    private String id;
    @Nullable
    private String message;
    @Nullable
    private String type;
    private boolean remote;
    @Nullable
    private StackTraceElement[] stack;
    private int truncated;
    private int skipped;
    @Nullable
    private String cause;
    
    @JsonIgnore
    @Nullable
    private Throwable throwable;

    // TODO(anuraaga): Investigate why stack is not being treated as nullable
    @SuppressWarnings("nullness:initialization.fields.uninitialized")
    public ThrowableDescription() {
    }

    // TODO(anuraaga): Investigate why stack is not being treated as nullable
    @SuppressWarnings("nullness:initialization.fields.uninitialized")
    public ThrowableDescription(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * @return the id
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the message
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    /**
     * @return the type
     */
    @Nullable
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the remote
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * @param remote the remote to set
     */
    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    /**
     * @return the stack
     */
    @Nullable
    public StackTraceElement[] getStack() {
        return stack;
    }

    /**
     * @param stack the stack to set
     */
    public void setStack(@Nullable StackTraceElement[] stack) {
        this.stack = stack;
    }

    /**
     * @return the truncated
     */
    public int getTruncated() {
        return truncated;
    }

    /**
     * @param truncated the truncated to set
     */
    public void setTruncated(int truncated) {
        this.truncated = truncated;
    }

    /**
     * @return the skipped
     */
    public int getSkipped() {
        return skipped;
    }

    /**
     * @param skipped the skipped to set
     */
    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    /**
     * @return the cause
     */
    @Nullable
    public String getCause() {
        return cause;
    }

    /**
     * @param cause the cause to set
     */
    public void setCause(@Nullable String cause) {
        this.cause = cause;
    }

    /**
     * @return the throwable
     */
    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * @param throwable the throwable to set
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
