package com.revolut.exceptions;

public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(final String message) {
        super(message);
    }
}
