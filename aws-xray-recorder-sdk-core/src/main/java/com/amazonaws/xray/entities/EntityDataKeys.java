package com.amazonaws.xray.entities;

public final class EntityDataKeys {

    private EntityDataKeys() {

    }

    public static final class AWS {
        private AWS() {

        }

        public static final String ACCOUNT_ID_SUBSEGMENT_KEY = "account_id";
        public static final String EXTENDED_REQUEST_ID_KEY = "id_2";
        public static final String OPERATION_KEY = "operation";
        public static final String REGION_KEY = "region";
        public static final String REQUEST_ID_KEY = "request_id";
        public static final String RETRIES_KEY = "retries";
    }

    public static final class HTTP {
        private HTTP() {

        }

        public static final String CONTENT_LENGTH_KEY = "content_length";
        public static final String RESPONSE_KEY = "response";
        public static final String STATUS_CODE_KEY = "status";
    }
}
