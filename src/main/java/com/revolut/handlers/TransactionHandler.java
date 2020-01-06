package com.revolut.handlers;

import com.revolut.domain.Transaction;
import com.revolut.exceptions.*;
import com.revolut.service.TransactionService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.UUID;

public class TransactionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHandler.class);

    private final TransactionService transactionService;

    public TransactionHandler(final TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void transferAmount(Context context) throws SameAccountTransferException, InsufficientFundsException, AccountNotFoundException, InvalidDepositException, TransactionNotFoundException {
        final Transaction transactionRequest = validateParamsAndCreateTransaction(context);
        final Transaction response = transactionService.transfer(transactionRequest);
        context.json(response).status(HttpStatus.CREATED_201);
    }

    public void getAllTransactions(final Context context) {
        final Collection<Transaction> allTransactions = transactionService.getAllTransactions();
        if (allTransactions.isEmpty()) {
            context.status(HttpStatus.NOT_FOUND_404);
        } else {
            context.json(allTransactions);
        }
    }

    public void getTransaction(Context context) throws TransactionNotFoundException {
        final UUID id = context.pathParam(":id", UUID.class).get();
        final Transaction transaction = transactionService.getTransaction(id);
        context.json(transaction);
    }

    private Transaction validateParamsAndCreateTransaction(final Context context) {
        try {
            return context.bodyValidator(Transaction.class)
                    .check(t -> (t.getFromAccountNumber() > 0) && (t.getToAccountNumber() > 0) && t.getMoney().isPositive())
                    .get();
        } catch (final BadRequestResponse exception) {
            logger.error("bad request: {}, reason: {}", context.body(), exception.getMessage());
            throw new BadRequestResponse(String.format("bad request %s", context.body()));
        }
    }

}
