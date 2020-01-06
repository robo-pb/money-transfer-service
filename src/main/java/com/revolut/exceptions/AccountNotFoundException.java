package com.revolut.exceptions;

public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(final String message) {
        super(message);
    }
}
