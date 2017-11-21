package com.amazonaws.xray.contexts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.entities.StringValidator;

public class LambdaSegmentContextResolver implements SegmentContextResolver {
    private static final Log logger = LogFactory.getLog(LambdaSegmentContextResolver.class);

    private static final String LAMBDA_TASK_ROOT_KEY = "LAMBDA_TASK_ROOT";
    private static final String SDK_INITIALIZED_FILE_LOCATION = "/tmp/.aws-xray/initialized";

    static {
        if (StringValidator.isNotNullOrBlank(LambdaSegmentContextResolver.getLambdaTaskRoot())) {
            boolean success = true;
            long now = System.currentTimeMillis();
            File f = new File(SDK_INITIALIZED_FILE_LOCATION);
            f.getParentFile().mkdirs();
            try {
                OutputStream out = new FileOutputStream(f);
                out.close();
                Files.setAttribute(f.toPath(), "lastAccessTime", FileTime.fromMillis(now));
            } catch (IOException ioe) {
                success = false;
            }
            if (!success || !f.setLastModified(now)) {
                logger.warn("Unable to write to " + SDK_INITIALIZED_FILE_LOCATION + ". Failed to signal SDK initialization.");
            }
        }
    }

    private static String getLambdaTaskRoot() {
        return System.getenv(LambdaSegmentContextResolver.LAMBDA_TASK_ROOT_KEY);
    }

    @Override
    public SegmentContext resolve() {
        String lambdaTaskRootValue = LambdaSegmentContextResolver.getLambdaTaskRoot();
        if (StringValidator.isNotNullOrBlank(lambdaTaskRootValue)) {
            logger.debug(LAMBDA_TASK_ROOT_KEY + " is set. Lambda context detected.");
            return new LambdaSegmentContext();
        }
        return null;
    }
}
