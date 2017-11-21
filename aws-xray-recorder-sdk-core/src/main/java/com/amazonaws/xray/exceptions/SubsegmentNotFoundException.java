package com.amazonaws.xray.exceptions;

public class SubsegmentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 3598661533525244324L;

    public SubsegmentNotFoundException() {
    }

    public SubsegmentNotFoundException(String message) {
        super(message);
    }

    public SubsegmentNotFoundException(Throwable cause) {
        super(cause);
    }

    public SubsegmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
