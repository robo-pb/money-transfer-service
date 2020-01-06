package com.revolut.persistence;

import com.revolut.domain.Transaction;
import com.revolut.exceptions.TransactionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InMemoryTransactionRepository implements TransactionRepository {
    private final Logger log = LoggerFactory.getLogger(InMemoryTransactionRepository.class);
    private Map<UUID, Transaction> transactionRegistry = new HashMap<>();

    @Override
    public void persistTransaction(final Transaction transaction) {
        transactionRegistry.put(transaction.getId(), transaction);
        log.info("persisted transaction {}", transaction);
    }


    @Override
    public Transaction getTransaction(final UUID uuid) throws TransactionNotFoundException {
        return Optional.ofNullable(transactionRegistry.get(uuid)).orElseThrow(() -> {
            final String message = String.format("transaction with id:%s not found", uuid);
            return new TransactionNotFoundException(message);
        });
    }

    @Override
    public Collection<Transaction> getAllTransactions() {
        return transactionRegistry.values();
    }
}
