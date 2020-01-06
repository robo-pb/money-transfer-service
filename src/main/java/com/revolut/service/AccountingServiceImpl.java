package com.revolut.service;

import com.revolut.domain.Account;
import com.revolut.exceptions.AccountAlreadyExistsException;
import com.revolut.exceptions.AccountNotFoundException;
import com.revolut.persistence.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

public class AccountingServiceImpl implements AccountingService {

    private static final Logger log = LoggerFactory.getLogger(AccountingServiceImpl.class);
    private final AccountRepository accountRepository;

    public AccountingServiceImpl(final AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account createAccount(final Account account) throws AccountAlreadyExistsException {
        log.info("creating account {}", account);
        return accountRepository.createAccount(account);
    }

    @Override
    public Collection<Account> getAllAccounts() {
        final Collection<Account> allAccounts = accountRepository.getAllAccounts();
        return allAccounts.stream().map(Account::copy).collect(Collectors.toList());
    }

    @Override
    public Account getAccount(final long accountId) throws AccountNotFoundException {
        final Account orig = accountRepository.getAccountByNumber(accountId);
        return Account.copy(orig);
    }

}
