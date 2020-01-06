package com.revolut.service;

import com.revolut.domain.Account;
import com.revolut.domain.Transaction;
import com.revolut.exceptions.*;
import com.revolut.persistence.AccountRepository;
import com.revolut.persistence.TransactionRepository;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransactionServiceImpl implements TransactionService {
    private static final long LOCK_TIMEOUT = 100;
    private final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(final AccountRepository accountRepository, final TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction transfer(final Transaction transaction)
            throws SameAccountTransferException, InsufficientFundsException, AccountNotFoundException, InvalidDepositException {

        final long srcAccountId = transaction.getFromAccountNumber();
        final long dstAccountId = transaction.getToAccountNumber();
        final Money amount = transaction.getMoney();

        final AtomicBoolean isExecuting = transaction.getIsExecuting();
        log.info("transfer initiated : {}, money: {}, src: {}, dst: {} ", transaction, amount, srcAccountId, dstAccountId);

        while (isExecuting.get()) {
            Account src, dst;
            synchronized (this) {
                src = accountRepository.getAccountByNumber(srcAccountId);
                dst = accountRepository.getAccountByNumber(dstAccountId);
                if (src == dst) {
                    throw new SameAccountTransferException(String.format("transfer between the same account %s", src.getAccountNumber()));
                }
                final CurrencyUnit srcCurrency = src.getMoney().getCurrencyUnit();
                final CurrencyUnit dstCurrency = dst.getMoney().getCurrencyUnit();
                if (srcCurrency != dstCurrency) {
                    throw new UnsupportedOperationException(String.format("transfer between the different currency %s, %s", srcCurrency, dstCurrency));
                }

            }
            try {
                if (src.getLock().tryLock(LOCK_TIMEOUT, TimeUnit.NANOSECONDS)
                        &&
                        dst.getLock().tryLock(LOCK_TIMEOUT, TimeUnit.NANOSECONDS)
                )
                    try {

                        log.debug("current thread {} has lock", Thread.currentThread().getName());
                        final Account srcUpdated = src.withdraw(amount);
                        final Account dstUpdated = dst.deposit(amount);

                        accountRepository.updateAccount(src, srcUpdated);
                        accountRepository.updateAccount(dst, dstUpdated);

                        transactionRepository.persistTransaction(transaction);
                        isExecuting.set(false);
                    } finally {
                        src.getLock().unlock();
                        dst.getLock().unlock();
                        log.debug("current thread {} has unlocked", Thread.currentThread().getName());
                    }
            } catch (final InterruptedException e) {
                log.error("Thread {} is interrupted {}", Thread.currentThread().getName(), e);
                isExecuting.set(false);
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        return transaction;
    }

    @Override
    public Collection<Transaction> getAllTransactions() {
        return transactionRepository.getAllTransactions();
    }

    @Override
    public Transaction getTransaction(UUID uuid) throws TransactionNotFoundException {
        return transactionRepository.getTransaction(uuid);
    }
}