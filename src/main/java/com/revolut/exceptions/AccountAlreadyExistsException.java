package com.revolut.exceptions;

public class AccountAlreadyExistsException extends Exception {
    public AccountAlreadyExistsException(final String message) {
        super(message);
    }
}
