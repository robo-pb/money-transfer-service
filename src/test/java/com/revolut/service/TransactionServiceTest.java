package com.revolut.service;

import com.revolut.domain.Account;
import com.revolut.domain.Transaction;
import com.revolut.exceptions.*;
import com.revolut.persistence.AccountRepository;
import com.revolut.persistence.InMemoryAccountRepository;
import com.revolut.persistence.InMemoryTransactionRepository;
import com.revolut.persistence.TransactionRepository;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TransactionServiceTest {

    private static final Money INITIAL_1000_BANK_BALANCE = Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(1000.00));
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TransactionService transactionService;
    private TransactionRepository transactionRepository;
    private AccountingService accountingService;
    private long srcAccountNumber;
    private long dstAccountNumber;

    @Before
    public void setup() throws AccountAlreadyExistsException {
        transactionRepository = new InMemoryTransactionRepository();
        AccountRepository accountRepository = new InMemoryAccountRepository();
        transactionService = new TransactionServiceImpl(accountRepository, transactionRepository);
        accountingService = new AccountingServiceImpl(accountRepository);
        createTwoAccounts();
    }

    @Test
    public void shouldThrowInsufficientFundsException() throws InsufficientFundsException, InvalidDepositException, AccountNotFoundException, SameAccountTransferException {
        expectedException.expect(InsufficientFundsException.class);
        expectedException.expectMessage(String.format("Account %s does not have sufficient funds", srcAccountNumber));
        Money hugeAmount = Money.of(CurrencyUnit.EUR, 2000000.00);
        final Transaction transaction = Transaction.builder().accountFrom(srcAccountNumber).accountTo(dstAccountNumber).money(hugeAmount).build();
        transactionService.transfer(transaction);
    }


    @Test
    public void shouldThrowSameAccountTransferException() throws InsufficientFundsException, InvalidDepositException, AccountNotFoundException, SameAccountTransferException {
        expectedException.expect(SameAccountTransferException.class);
        expectedException.expectMessage(String.format("transfer between the same account %s", srcAccountNumber));

        final Money amount = Money.of(CurrencyUnit.EUR, 20);
        final Transaction transaction = Transaction.builder().accountFrom(srcAccountNumber).accountTo(srcAccountNumber).money(amount).build();
        transactionService.transfer(transaction);
    }


    @Test
    public void shouldThrowInvalidDepositExceptionForNegativeMoney() throws InsufficientFundsException, InvalidDepositException, AccountNotFoundException, SameAccountTransferException {
        final Money negativeAmount = Money.of(CurrencyUnit.EUR, -2000);
        expectedException.expect(InvalidDepositException.class);
        expectedException.expectMessage(String.format("money %s cannot be deposited", negativeAmount));


        final Transaction transaction = Transaction.builder().accountFrom(srcAccountNumber).accountTo(dstAccountNumber).money(negativeAmount).build();
        transactionService.transfer(transaction);
    }

    @Test
    public void shouldThrowAccountDoesNotExistWhenTransferredToNotExistingAccount() throws InsufficientFundsException, InvalidDepositException, AccountNotFoundException, SameAccountTransferException {
        final long unknownAccountNumber = 99999;
        expectedException.expect(AccountNotFoundException.class);
        expectedException.expectMessage(String.format("Account %s does not exist", unknownAccountNumber));
        final Transaction transaction = Transaction.builder().accountFrom(unknownAccountNumber).accountTo(dstAccountNumber).money(Money.of(CurrencyUnit.EUR, 2000.0)).build();
        transactionService.transfer(transaction);
    }


    @Test
    public void transactionIsNotExecutingAfterTransfer() throws InsufficientFundsException, InvalidDepositException, AccountNotFoundException, SameAccountTransferException {
        final Money amount = Money.of(CurrencyUnit.EUR, BigDecimal.valueOf(200.0));
        final Transaction transactionRequest = Transaction.builder().accountFrom(srcAccountNumber).accountTo(dstAccountNumber).money(amount).build();
        Assert.assertTrue(transactionRequest.getIsExecuting().get());
        final Transaction transactionResponse = transactionService.transfer(transactionRequest);
        Assert.assertFalse(transactionRequest.getIsExecuting().get());

        final Account fromAccount = accountingService.getAccount(transactionResponse.getFromAccountNumber());
        final Account toAccount = accountingService.getAccount(transactionResponse.getToAccountNumber());

        Assert.assertEquals(fromAccount.getMoney(), INITIAL_1000_BANK_BALANCE.minus(amount));
        Assert.assertEquals(toAccount.getMoney(), INITIAL_1000_BANK_BALANCE.plus(amount));

        try {
            final Transaction persisted = transactionRepository.getTransaction(transactionRequest.getId());
            Assert.assertFalse(persisted.getIsExecuting().get());
        } catch (TransactionNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void shouldThrowUnSupportedOperation() throws AccountAlreadyExistsException, InsufficientFundsException, InvalidDepositException, AccountNotFoundException, SameAccountTransferException {
        final Money swissMoney = Money.of(CurrencyUnit.CHF, 1000);
        Account third = accountingService.createAccount(Account.builder().owner("third").money(swissMoney).build());

        expectedException.expect(UnsupportedOperationException.class);
        final CurrencyUnit srcCurrency = INITIAL_1000_BANK_BALANCE.getCurrencyUnit();
        final CurrencyUnit dstCurrency = swissMoney.getCurrencyUnit();
        expectedException.expectMessage(String.format("transfer between the different currency %s, %s", srcCurrency, dstCurrency));

        final Money transferredEuro = Money.of(CurrencyUnit.EUR, 20);
        final Transaction transaction = Transaction.builder().accountFrom(srcAccountNumber).accountTo(third.getAccountNumber()).money(transferredEuro).build();
        transactionService.transfer(transaction);

    }


    private void createTwoAccounts() throws AccountAlreadyExistsException {
        accountingService.createAccount(Account.builder().owner("first").money(INITIAL_1000_BANK_BALANCE).build());
        accountingService.createAccount(Account.builder().owner("second").money(INITIAL_1000_BANK_BALANCE).build());

        final List<Account> allAccounts = new ArrayList<>(accountingService.getAllAccounts());
        for (Account allAccount : allAccounts) {
            Assert.assertTrue(allAccount.getMoney().isPositive());
        }

        srcAccountNumber = allAccounts.get(0).getAccountNumber();
        dstAccountNumber = allAccounts.get(1).getAccountNumber();

    }
}
