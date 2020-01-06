package com.revolut.handlers;

import com.revolut.domain.Account;
import com.revolut.exceptions.AccountAlreadyExistsException;
import com.revolut.exceptions.AccountNotFoundException;
import com.revolut.service.AccountingService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class AccountHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountHandler.class);

    private AccountingService accountingService;

    public AccountHandler(final AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    public void getAll(final Context context) {
        final Collection<Account> allAccounts = accountingService.getAllAccounts();
        if (allAccounts.isEmpty()) {
            context.status(HttpStatus.NOT_FOUND_404);
        } else {
            context.json(allAccounts);
        }
    }

    public void create(final Context context) throws AccountAlreadyExistsException {
        final Account accountRequest = validateParamsAndCreateAccount(context);
        final Account created = accountingService.createAccount(accountRequest);
        context.json(created).status(HttpStatus.CREATED_201);
    }

    public void getAccount(final Context context) throws AccountNotFoundException {
        final long accountId = context.pathParam(":accountNumber", Long.class).check(accId -> accId > 0, "Account number cannot be negative number").get();
        final Account account = accountingService.getAccount(accountId);
        context.json(account);
    }


    private Account validateParamsAndCreateAccount(final Context context) {
        try {
            return context.bodyValidator(Account.class)
                    .check(t -> (t.getOwner() != null && !t.getOwner().isEmpty()) && (t.getMoney().isPositive()))
                    .get();
        }
        catch(final BadRequestResponse exception){
            log.error("bad request: {}, reason: {}",  context.body(), exception.getMessage());
            throw new BadRequestResponse(String.format("bad request %s", context.body()));
        }
    }

}
