package org.transactions.digitalwallettraining.exception;

public class MaxRetryExceededException extends RuntimeException {
    public MaxRetryExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
