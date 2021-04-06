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

import com.amazonaws.xray.entities.StringValidator;
import java.nio.file.FileSystem;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FSUtils {
    private static final String WINDOWS_PROGRAM_DATA = "ProgramData";

    @Nullable
    public static String getOsSpecificFilePath(FileSystem fs, String linuxPath, String windowsPath) {
        if (fs.getSeparator() == null) {
            return null;
        }

        if (fs.getSeparator().equals("/")) {
            return linuxPath;
        } else if (fs.getSeparator().equals("\\")) {
            String programData = System.getenv(WINDOWS_PROGRAM_DATA);
            if (StringValidator.isNotNullOrBlank(programData)) {
                return programData + windowsPath;
            }
        }

        return null;
    }

    private FSUtils() {
    }
}
