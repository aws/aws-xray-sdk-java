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

package com.amazonaws.xray.plugins;

import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.entities.StringValidator;
import com.amazonaws.xray.utils.DockerUtils;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A plugin, for use with the {@link com.amazonaws.xray.AWSXRayRecorderBuilder}, to record log references for dockerized
 * applications using the awslogs driver.
 */
public class DockerPlugin implements Plugin {
    private static final String LINUX_DOCKER_PATH = "/etc/docker/daemon.json";

    private final FileSystem fs;
    private final Set<AWSLogReference> logReferences;

    public DockerPlugin() {
        this(FileSystems.getDefault());
    }

    public DockerPlugin(FileSystem fs) {
        this.fs = fs;
        this.logReferences = new HashSet<>();
    }

    /**
     * @return {@code null} because there is no allow-listed Docker origin for X-Ray segments
     */
    @Override
    @Nullable
    public String getOrigin() {
        return null;
    }

    /**
     * @return name of service for this plugin
     */
    @Override
    public String getServiceName() {
        return "docker";
    }

    /**
     * Determines if the docker metadata can be recorded in X-Ray segments
     * @return true if the Docker daemon config file is present at default location
     */
    @Override
    public boolean isEnabled() {
        return this.fs.getPath(LINUX_DOCKER_PATH).toFile().exists();
    }

    /**
     * @return empty map because this plugin only collects log references.
     */
    @Override
    public Map<String, @Nullable Object> getRuntimeContext() {
        return new HashMap<>();
    }

    /**
     * Parses the docker daemon config file for an AWS Log Group if the awslogs driver is in use
     * @return a set containing the discovered log group if present, otherwise an empty set
     */
    @Override
    public Set<AWSLogReference> getLogReferences() {
        if (this.logReferences.isEmpty()) {
            populateLogReferences();
        }
        return this.logReferences;
    }

    private void populateLogReferences() {
        AWSLogReference reference = DockerUtils.getAwsLogReference(LINUX_DOCKER_PATH, this.fs);
        if (reference != null && StringValidator.isNotNullOrBlank(reference.getLogGroup())) {
            this.logReferences.add(reference);
        }
    }
}
