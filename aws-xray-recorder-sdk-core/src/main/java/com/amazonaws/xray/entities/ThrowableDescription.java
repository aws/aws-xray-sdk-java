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

public class ThrowableDescription {
    private String id;
    private String message;
    private String type;
    private boolean remote;
    private StackTraceElement[] stack;
    private int truncated;
    private int skipped;
    private String cause;
    
    @JsonIgnore
    private Throwable throwable;

    public ThrowableDescription() { }
    public ThrowableDescription(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * @return the id
     */
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
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the type
     */
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
    public StackTraceElement[] getStack() {
        return stack;
    }

    /**
     * @param stack the stack to set
     */
    public void setStack(StackTraceElement[] stack) {
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
    public String getCause() {
        return cause;
    }

    /**
     * @param cause the cause to set
     */
    public void setCause(String cause) {
        this.cause = cause;
    }

    /**
     * @return the throwable
     */
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
