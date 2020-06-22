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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to get metadata for dockerized containers
 */
public class DockerUtils {
    private static final Log logger = LogFactory.getLog(DockerUtils.class);

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
}
