package com.amazonaws.xray.internal;

/**
 * A {@link RuntimeException} thrown when we fail to send HTTP requests to the X-Ray daemon.
 */
class XrayClientException extends RuntimeException {

    private static final long serialVersionUID = -32616082201202518L;

    XrayClientException(String message) {
        super(message);
    }

    XrayClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
