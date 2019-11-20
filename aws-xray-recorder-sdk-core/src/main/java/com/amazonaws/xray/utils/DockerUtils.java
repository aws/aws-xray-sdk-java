package com.amazonaws.xray.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class to get metadata for dockerized containers
 */
public class DockerUtils {
    private static final Log logger = LogFactory.getLog(DockerUtils.class);

    private static final String CGROUP_PATH = "/proc/self/cgroup";
    private static final int CONTAINER_ID_LENGTH = 64;

    /**
     * Reads the docker-generated cgroup file that lists the full (untruncated) docker container ID at the end of each line. This method
     * takes advantage of that fact by just reading the 64-character ID from the end of the first line
     *
     * @throws IOException if the file cannot be read
     * @return the untruncated Docker container ID, or null if it can't be read
     */
    public static String getContainerId() throws IOException {
        File procFile = new File(CGROUP_PATH);

        if (procFile.exists()) {
            InputStream inputStream = null;
            BufferedReader reader = null;
            try {
                inputStream = new FileInputStream(procFile);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();

                if (line != null) {
                    return line.substring(line.length() - CONTAINER_ID_LENGTH);
                } else {
                    logger.error("Failed to read container ID because " + CGROUP_PATH + " was empty.");
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } else {
            logger.warn("Failed to read container ID because " + CGROUP_PATH + " does not exist.");
        }

        return null;
    }
}
