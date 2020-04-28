package com.amazonaws.xray.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class to get metadata for dockerized containers
 */
public class DockerUtils {
    private static final Log logger = LogFactory.getLog(DockerUtils.class);

    private static final String CGROUP_PATH = "/proc/self/cgroup";
    private static final int CONTAINER_ID_LENGTH = 64;

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
     * Reads the docker-generated cgroup file that lists the full (untruncated) docker container ID at the end of each line. This method
     * takes advantage of that fact by just reading the 64-character ID from the end of the first line
     *
     * @throws IOException if the file cannot be read
     * @return the untruncated Docker container ID, or null if it can't be read
     */
    public String getContainerId() throws IOException {
        File procFile;

        if (cgroupLocation == null) { return null; }
        try {
            procFile = new File(cgroupLocation.toURI());
        } catch (URISyntaxException e) {
            logger.warn("Failed to read container ID because " + cgroupLocation.toString() + " didn't contain an ID.");
            return null;
        }

        if (procFile.exists()) {
            InputStream inputStream = null;
            BufferedReader reader = null;
            try {
                inputStream = new FileInputStream(procFile);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                do {
                    line = reader.readLine();

                    if (line == null) {
                        logger.warn("Failed to read container ID because " + cgroupLocation.toString() + " didn't contain an ID.");
                    } else if (line.length() > CONTAINER_ID_LENGTH) {
                        return line.substring(line.length() - CONTAINER_ID_LENGTH);
                    }
                } while (line != null);
            } finally {
                if (reader != null) {
                    reader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } else {
            logger.warn("Failed to read container ID because " + cgroupLocation.toString() + " does not exist.");
        }

        return null;
    }
}
