package com.amazonaws.xray.entities;

public final class EntityHeaderKeys {
    private EntityHeaderKeys() {

    }

    public static final class AWS {
        private AWS() {

        }

        public static final String EXTENDED_REQUEST_ID_HEADER = "x-amz-id-2";
    }

    public static final class HTTP {
        private HTTP() {

        }

        public static final String CONTENT_LENGTH_HEADER = "Content-Length";
    }
}
