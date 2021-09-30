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

import java.util.regex.Pattern;

/**
 * @deprecated For internal use only.
 */
@Deprecated
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class StringTransform {
    private static final Pattern REGEX = Pattern.compile("([a-z])([A-Z]+)");
    private static final String REPLACE = "$1_$2";

    public static String toSnakeCase(String camelCase) {
        return REGEX.matcher(camelCase).replaceAll(REPLACE).toLowerCase();
    }
}
