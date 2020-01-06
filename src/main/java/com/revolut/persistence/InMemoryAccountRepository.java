package com.revolut.persistence;

import com.revolut.domain.Account;
import com.revolut.exceptions.AccountAlreadyExistsException;
import com.revolut.exceptions.AccountNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAccountRepository implements AccountRepository {
    private final Logger log = LoggerFactory.getLogger(InMemoryAccountRepository.class);
    private final Map<Long, Account> accountRegistry = new HashMap<>();
    private final AtomicLong ACCOUNT_NUMBER_COUNTER = new AtomicLong();

    @Override
    public Account createAccount(final Account account) throws AccountAlreadyExistsException {
        final long id = getNextAccountNumber();

        final Account accountWithId = Account.builder()
                .owner(account.getOwner())
                .accountNumber(id)
                .money(account.getMoney())
                .build();

        final Account created = accountRegistry.putIfAbsent(accountWithId.getAccountNumber(), accountWithId);

        if (created != null) {
            final String message = String.format("account %s created already", id);
            log.error("{}", message);
            throw new AccountAlreadyExistsException(message);
        }

        log.info("created account {}", accountWithId);
        return accountWithId;
    }

    @Override
    public Account getAccountByNumber(long accountNumber) throws AccountNotFoundException {
        final AccountNotFoundException exception = new AccountNotFoundException(String.format("Account %s does not exist", accountNumber));
        return Optional.ofNullable(accountRegistry.get(accountNumber))
                .orElseThrow(() -> exception);
    }

    @Override
    public Collection<Account> getAllAccounts() {
        return accountRegistry.values();
    }

    @Override
    public void updateAccount(final Account old, final Account updated) {
        accountRegistry.put(old.getAccountNumber(), updated);
    }

    private long getNextAccountNumber() {
        return ACCOUNT_NUMBER_COUNTER.addAndGet(1L);
    }

}
