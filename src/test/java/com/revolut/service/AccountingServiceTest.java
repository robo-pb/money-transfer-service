package com.revolut.service;

import com.revolut.exceptions.AccountNotFoundException;
import com.revolut.exceptions.InsufficientFundsException;
import com.revolut.persistence.AccountRepository;
import com.revolut.persistence.InMemoryAccountRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AccountingServiceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    private AccountingService accountingService;

    @Before
    public void setup() {
        AccountRepository accountRepository = new InMemoryAccountRepository();
        accountingService = new AccountingServiceImpl(accountRepository);
    }

    @Test
    public void shouldThrowAccountDoesNotExistException() throws AccountNotFoundException {
        final long accountId = 9999;
        expectedException.expect(AccountNotFoundException.class);
        expectedException.expectMessage(String.format("Account %s does not exist", accountId));
        accountingService.getAccount(accountId);
    }

}