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

package com.amazonaws.xray.utils;

import com.amazonaws.xray.entities.AWSLogReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to get metadata for dockerized containers
 */
public class DockerUtils {
    private static final Log logger = LogFactory.getLog(DockerUtils.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String AWS_LOGS_DRIVER = "awslogs";

    private static final String CGROUP_PATH = "/proc/self/cgroup";
    private static final int CONTAINER_ID_LENGTH = 64;

    @MonotonicNonNull
    private URL cgroupLocation;

    public DockerUtils() {
        try {
            this.cgroupLocation = new File(CGROUP_PATH).toURI().toURL();
        } catch (MalformedURLException e) {
            logger.warn("Failed to read container ID because " + CGROUP_PATH + " does not exist.");
        }
    }

    public DockerUtils(URL cgroupLocation) {
        this.cgroupLocation = cgroupLocation;
    }

    /**
     * Reads the docker-generated cgroup file that lists the full (untruncated) docker container ID at the end of each line. This
     * method takes advantage of that fact by just reading the 64-character ID from the end of the first line.
     *
     * @throws IOException if the file cannot be read
     * @return the untruncated Docker container ID, or null if it can't be read
     */
    @Nullable
    public String getContainerId() throws IOException {
        if (cgroupLocation == null) {
            return null;
        }

        final File procFile;
        try {
            procFile = new File(cgroupLocation.toURI());
        } catch (URISyntaxException e) {
            logger.warn("Failed to read container ID because " + cgroupLocation.toString() + " didn't contain an ID.");
            return null;
        }

        if (procFile.exists()) {
            try (InputStream inputStream = new FileInputStream(procFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                do {
                    line = reader.readLine();

                    if (line == null) {
                        logger.warn("Failed to read container ID because " + cgroupLocation.toString()
                                    + " didn't contain an ID.");
                    } else if (line.length() > CONTAINER_ID_LENGTH) {
                        return line.substring(line.length() - CONTAINER_ID_LENGTH);
                    }
                } while (line != null);
            }
        } else {
            logger.warn("Failed to read container ID because " + cgroupLocation.toString() + " does not exist.");
        }

        return null;
    }

    /**
     * Search a file path for the Docker daemon config file, and if present parse that file for the AWS Log group.
     *
     * Daemon config file: https://docs.docker.com/engine/reference/commandline/dockerd/#daemon-configuration-file
     * awslogs driver format: https://docs.docker.com/config/containers/logging/awslogs/
     *
     * @param filePath file system path to daemon configuration file
     * @param fs file system to search for config file
     * @return an AWSLogReference for the discovered log group, or null if the config file is not present or does not
     *         contain an awslogs driver
     */
    @Nullable
    public static AWSLogReference getAwsLogReference(String filePath, FileSystem fs) {
        URL fileLoc;
        try {
            fileLoc = fs.getPath(filePath).toUri().toURL();
        } catch (IOException e) {
            logger.debug("Docker daemon config file does not exist", e);
            return null;
        }

        try {
            DockerLogConfiguration config = MAPPER.readValue(fileLoc, DockerLogConfiguration.class);
            if (!AWS_LOGS_DRIVER.equals(config.getLogDriver())) {
                return null;  // Only accept AWS log drivers
            }
            return config.getLogReference();
        } catch (IOException e) {
            logger.warn("Failed to parse docker daemon config file", e);
        }

        return null;
    }

    private static class DockerLogConfiguration {
        @Nullable
        private String logDriver;
        @Nullable
        private AWSLogReference logReference;

        DockerLogConfiguration() {
            this.logReference = null;
            this.logDriver = null;
        }

        @JsonProperty("log-driver")
        @Nullable
        String getLogDriver() {
            return logDriver;
        }

        void setLogDriver(@Nullable String logDriver) {
            this.logDriver = logDriver;
        }

        @JsonProperty("log-opts")
        @Nullable
        AWSLogReference getLogReference() {
            return logReference;
        }

        void setLogReference(@Nullable AWSLogReference logReference) {
            this.logReference = logReference;
        }
    }
}
