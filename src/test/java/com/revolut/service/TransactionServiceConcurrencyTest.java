package com.revolut.service;


import com.revolut.domain.Account;
import com.revolut.domain.Transaction;
import com.revolut.exceptions.*;
import com.revolut.persistence.AccountRepository;
import com.revolut.persistence.InMemoryAccountRepository;
import com.revolut.persistence.InMemoryTransactionRepository;
import com.revolut.persistence.TransactionRepository;
import net.jodah.concurrentunit.Waiter;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionServiceConcurrencyTest {
    private static final int THREAD_COUNT = 3;
    private static final int INITIAL_BANK_BALANCE = 1000;
    private ExecutorService executorService;
    private TransactionService transactionService;
    private AccountRepository accountRepository;
    private long srcAccountNumber;
    private long dstAccountNumber;
    private Waiter waiter;


    @Before
    public void setup() throws AccountAlreadyExistsException {
        waiter = new Waiter();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        final TransactionRepository transactionRepository = new InMemoryTransactionRepository();
        accountRepository = new InMemoryAccountRepository();
        createTwoAccounts(accountRepository);
        transactionService = new TransactionServiceImpl(accountRepository, transactionRepository);
    }

    @After
    public void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void transactionExceptionShouldNotResultInInconsistency() throws InterruptedException, TimeoutException {
        final Runnable runnable = () -> {
            try {
                final Money amountTransferred = Money.of(CurrencyUnit.EUR, 10.00);
                final Account srcAccount = accountRepository.getAccountByNumber(srcAccountNumber);
                final Account dstAccount = accountRepository.getAccountByNumber(dstAccountNumber);

                final Transaction transaction = Transaction.builder().accountFrom(srcAccount.getAccountNumber()).accountTo(dstAccount.getAccountNumber()).money(amountTransferred).build();
                final Transaction transfer = transactionService.transfer(transaction);

                final Account srcAccountAfterTransfer = accountRepository.getAccountByNumber(srcAccountNumber);
                final Account dstAccountAfterTransfer = accountRepository.getAccountByNumber(dstAccountNumber);

                Money expected = Money.of(CurrencyUnit.EUR, INITIAL_BANK_BALANCE * 2);
                Assert.assertEquals(expected, srcAccountAfterTransfer.getMoney().plus(dstAccountAfterTransfer.getMoney()));
                waiter.resume();

            } catch (final InsufficientFundsException | SameAccountTransferException | AccountNotFoundException | InvalidDepositException exception) {
                exception.printStackTrace();
            }
        };

        int numberOfConcurrentExecutions = 5;
        for (int i = 0; i < numberOfConcurrentExecutions; i++) {
            Thread.sleep(ThreadLocalRandom.current().nextInt(2000));
            executorService.execute(runnable);
        }

        waiter.await(10, TimeUnit.SECONDS, numberOfConcurrentExecutions - 1);

        verifyAccountStateWithLockStatus();

    }


    private void verifyAccountStateWithLockStatus() {
        final List<Account> accounts = new ArrayList<>(accountRepository.getAllAccounts());
        final Account currentSrcAccount = accounts.stream().filter(acc -> acc.getAccountNumber() == srcAccountNumber).findFirst().get();
        final Account currentDstAccount = accounts.stream().filter(acc -> acc.getAccountNumber() == dstAccountNumber).findFirst().get();

        final Money srcBalance = currentSrcAccount.getMoney();
        final Money dstAccountBalance = currentDstAccount.getMoney();
        System.out.println("srcBalance: " + srcBalance + " dstBalance: " + dstAccountBalance);

        final Money expected = Money.of(CurrencyUnit.EUR, INITIAL_BANK_BALANCE * 2);
        Assert.assertEquals(expected, srcBalance.plus(dstAccountBalance));

        currentAccountShouldNotBeLockedAfterTransaction(currentSrcAccount, currentDstAccount);
    }


    private void currentAccountShouldNotBeLockedAfterTransaction(Account src, Account dst) {
        final ReentrantLock dstAccountLock = (ReentrantLock) dst.getLock();
        final ReentrantLock srcAccountLock = (ReentrantLock) src.getLock();
        int srcHoldCount = srcAccountLock.getHoldCount();
        int dstHoldCount = dstAccountLock.getHoldCount();

        Assert.assertEquals(0, srcHoldCount);
        Assert.assertEquals(0, dstHoldCount);

    }

    private void createTwoAccounts(final AccountRepository accountRepository) throws AccountAlreadyExistsException {
        final AccountingService accountingService = new AccountingServiceImpl(accountRepository);
        final Money initialBankBalance = Money.of(CurrencyUnit.EUR,INITIAL_BANK_BALANCE);
        accountingService.createAccount(Account.builder().owner("first").money(initialBankBalance).build());
        accountingService.createAccount(Account.builder().owner("second").money(initialBankBalance).build());

        final List<Account> allAccounts = new ArrayList<>(accountingService.getAllAccounts());

        srcAccountNumber = allAccounts.get(0).getAccountNumber();
        dstAccountNumber = allAccounts.get(1).getAccountNumber();

    }


}
