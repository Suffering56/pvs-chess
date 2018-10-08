package com.example.chess.exceptions;

public class UnattainablePointException extends RuntimeException {

    public UnattainablePointException() {
    }

    public UnattainablePointException(String message) {
        super(message);
    }

    public UnattainablePointException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnattainablePointException(Throwable cause) {
        super(cause);
    }

    public UnattainablePointException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
