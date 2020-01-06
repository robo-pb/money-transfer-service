package com.revolut.service;

import com.revolut.domain.Transaction;
import com.revolut.exceptions.*;

import java.util.Collection;
import java.util.UUID;

public interface TransactionService {
    Transaction getTransaction(UUID uuid) throws TransactionNotFoundException;

    Transaction transfer(Transaction t) throws SameAccountTransferException, InsufficientFundsException, AccountNotFoundException, InvalidDepositException;

    Collection<Transaction> getAllTransactions();
}
