package com.revolut.service;

import com.revolut.domain.Account;
import com.revolut.exceptions.AccountAlreadyExistsException;
import com.revolut.exceptions.AccountNotFoundException;

import java.util.Collection;

public interface AccountingService {
    Account createAccount(Account account) throws AccountAlreadyExistsException;

    Collection<Account> getAllAccounts();

    Account getAccount(long accountId) throws AccountNotFoundException;
}
