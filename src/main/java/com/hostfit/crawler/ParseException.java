package com.hostfit.crawler;

/** Raised when a page or API response cannot be understood. */
public class ParseException extends Exception {

    private static final long serialVersionUID = 1L;

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
