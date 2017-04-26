package com.ciplogic.statemachine.impl;

public class XyzStateException extends RuntimeException {
    public XyzStateException() {
    }

    public XyzStateException(String message) {
        super(message);
    }

    public XyzStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public XyzStateException(Throwable cause) {
        super(cause);
    }

    public XyzStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
