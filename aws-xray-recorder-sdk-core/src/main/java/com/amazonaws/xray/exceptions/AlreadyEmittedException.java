package com.amazonaws.xray.exceptions;

public class AlreadyEmittedException extends RuntimeException {

    private static final long serialVersionUID = 6215061243115294496L;

    public AlreadyEmittedException() {
    }

    public AlreadyEmittedException(String message) {
        super(message);
    }

    public AlreadyEmittedException(Throwable cause) {
        super(cause);
    }

    public AlreadyEmittedException(String message, Throwable cause) {
        super(message, cause);
    }
}
