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

/**
 * A representation of what issues caused this (sub)segment to include a failure / error. Can include exceptions or references to other exceptions.
 *
 */
public class Cause {
    private static final Log logger =
        LogFactory.getLog(Cause.class);

    private String workingDirectory;

    private String id;

    private String message;

    private Collection<String> paths;

    private List<ThrowableDescription> exceptions;

    public Cause() {
        //id = Entity.generateId();
        paths = new ArrayList<>();
        exceptions = new ArrayList<>();
    }

    /**
     * @return the workingDirectory
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * @param workingDirectory the workingDirectory to set
     */
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
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
     * @return the paths
     */
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
