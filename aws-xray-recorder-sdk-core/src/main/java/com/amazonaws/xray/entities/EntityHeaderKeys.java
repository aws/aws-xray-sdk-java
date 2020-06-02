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

public final class EntityHeaderKeys {

    public static final class AWS {
        public static final String EXTENDED_REQUEST_ID_HEADER = "x-amz-id-2";

        private AWS() {
        }
    }

    public static final class HTTP {
        public static final String CONTENT_LENGTH_HEADER = "Content-Length";

        private HTTP() {
        }
    }

    private EntityHeaderKeys() {
    }
}
