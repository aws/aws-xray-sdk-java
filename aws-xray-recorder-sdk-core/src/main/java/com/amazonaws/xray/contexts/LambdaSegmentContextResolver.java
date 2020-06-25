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

package com.amazonaws.xray.contexts;

import com.amazonaws.xray.entities.StringValidator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LambdaSegmentContextResolver implements SegmentContextResolver {
    private static final Log logger = LogFactory.getLog(LambdaSegmentContextResolver.class);

    private static final String LAMBDA_TASK_ROOT_KEY = "LAMBDA_TASK_ROOT";
    private static final String SDK_INITIALIZED_FILE_LOCATION = "/tmp/.aws-xray/initialized";

    static {
        if (StringValidator.isNotNullOrBlank(LambdaSegmentContextResolver.getLambdaTaskRoot())) {
            boolean success = true;
            long now = System.currentTimeMillis();
            File f = new File(SDK_INITIALIZED_FILE_LOCATION);
            File dir = f.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
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

    @Nullable
    private static String getLambdaTaskRoot() {
        return System.getenv(LambdaSegmentContextResolver.LAMBDA_TASK_ROOT_KEY);
    }

    @Override
    @Nullable
    public SegmentContext resolve() {
        String lambdaTaskRootValue = LambdaSegmentContextResolver.getLambdaTaskRoot();
        if (StringValidator.isNotNullOrBlank(lambdaTaskRootValue)) {
            logger.debug(LAMBDA_TASK_ROOT_KEY + " is set. Lambda context detected.");
            return new LambdaSegmentContext();
        }
        return null;
    }
}
