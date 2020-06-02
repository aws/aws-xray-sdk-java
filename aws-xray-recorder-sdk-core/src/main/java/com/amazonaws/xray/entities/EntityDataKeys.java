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

public final class EntityDataKeys {

    public static final class AWS {

        public static final String ACCOUNT_ID_SUBSEGMENT_KEY = "account_id";
        public static final String EXTENDED_REQUEST_ID_KEY = "id_2";
        public static final String OPERATION_KEY = "operation";
        public static final String REGION_KEY = "region";
        public static final String REQUEST_ID_KEY = "request_id";
        public static final String RETRIES_KEY = "retries";

        private AWS() {
        }
    }

    public static final class HTTP {

        public static final String CONTENT_LENGTH_KEY = "content_length";
        public static final String RESPONSE_KEY = "response";
        public static final String STATUS_CODE_KEY = "status";

        private HTTP() {
        }
    }

    private EntityDataKeys() {
    }
}
