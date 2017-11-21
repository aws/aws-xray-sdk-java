package com.amazonaws.xray.exceptions;

public class SegmentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -3341201172459643090L;

    public SegmentNotFoundException() {
    }

    public SegmentNotFoundException(String message) {
        super(message);
    }

    public SegmentNotFoundException(Throwable cause) {
        super(cause);
    }

    public SegmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
