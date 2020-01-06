package com.revolut.persistence;

import com.revolut.domain.Transaction;
import com.revolut.exceptions.TransactionNotFoundException;

import java.util.Collection;
import java.util.UUID;

public interface TransactionRepository {

    void persistTransaction(Transaction transaction);

    Transaction getTransaction(UUID uuid) throws TransactionNotFoundException;

    Collection<Transaction> getAllTransactions();

}
