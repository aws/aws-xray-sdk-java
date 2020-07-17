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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A representation of what issues caused this (sub)segment to include a failure / error. Can include exceptions or references to
 * other exceptions.
 *
 */
public class Cause {
    private static final Log logger =
        LogFactory.getLog(Cause.class);

    @Nullable
    private String workingDirectory;

    @Nullable
    private String id;

    @Nullable
    private String message;

    @Nullable
    private Collection<String> paths;

    private final List<ThrowableDescription> exceptions;

    public Cause() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    Cause(List<String> paths, List<ThrowableDescription> exceptions) {
        this.paths = paths;
        this.exceptions = exceptions;
    }

    /**
     * @return the workingDirectory
     */
    @Nullable
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * @param workingDirectory the workingDirectory to set
     */
    public void setWorkingDirectory(@Nullable String workingDirectory) {
        this.workingDirectory = workingDirectory;
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
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the paths
     */
    @Nullable
    public Collection<String> getPaths() {
        return paths;
    }

    /**
     * @param paths the paths to set
     */
    public void setPaths(Collection<String> paths) {
        this.paths = paths;
    }

    /**
     * @return the exceptions
     */
    public List<ThrowableDescription> getExceptions() {
        return exceptions;
    }

    public void addException(ThrowableDescription descriptor) {
        if (exceptions.isEmpty()) {
            addWorkingDirectoryAndPaths();
        }
        exceptions.add(descriptor);
    }

    public void addExceptions(List<ThrowableDescription> descriptors) {
        if (exceptions.isEmpty()) {
            addWorkingDirectoryAndPaths();
        }
        exceptions.addAll(descriptors);
    }


    private void addWorkingDirectoryAndPaths() {
        try {
            setWorkingDirectory(System.getProperty("user.dir"));
        } catch (SecurityException se) {
            logger.warn("Unable to set working directory.", se);
        }
    }
}
