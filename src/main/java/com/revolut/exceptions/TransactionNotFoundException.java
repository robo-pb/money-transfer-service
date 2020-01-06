package com.revolut.exceptions;

public class TransactionNotFoundException extends Exception {
    public TransactionNotFoundException(String uuid) {
        super(uuid);
    }
}
