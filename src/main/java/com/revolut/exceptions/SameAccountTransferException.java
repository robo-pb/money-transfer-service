package com.revolut.exceptions;

public class SameAccountTransferException extends Exception {

    public SameAccountTransferException(String message) {
        super(message);
    }
}
