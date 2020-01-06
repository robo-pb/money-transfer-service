package com.revolut.persistence;

import com.revolut.domain.Account;
import com.revolut.exceptions.AccountAlreadyExistsException;
import com.revolut.exceptions.AccountNotFoundException;

import java.util.Collection;

public interface AccountRepository {

    Account createAccount(final Account account) throws AccountAlreadyExistsException;

    Account getAccountByNumber(final long accountNumber) throws AccountNotFoundException;

    Collection<Account> getAllAccounts();

    void updateAccount(Account old, Account updated);

}
