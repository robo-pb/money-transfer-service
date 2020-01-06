package com.revolut;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.revolut.exceptions.*;
import com.revolut.handlers.AccountHandler;
import com.revolut.handlers.TransactionHandler;
import com.revolut.persistence.AccountRepository;
import com.revolut.persistence.InMemoryAccountRepository;
import com.revolut.persistence.InMemoryTransactionRepository;
import com.revolut.persistence.TransactionRepository;
import com.revolut.serializers.MoneyDeSerializer;
import com.revolut.serializers.MoneySerializer;
import com.revolut.service.AccountingService;
import com.revolut.service.AccountingServiceImpl;
import com.revolut.service.TransactionService;
import com.revolut.service.TransactionServiceImpl;
import io.javalin.Javalin;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.plugin.json.JavalinJackson;
import org.eclipse.jetty.http.HttpStatus;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


public class App {
    private static final int DEFAULT_PORT = 7000;
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        final DependencyInjection dependencyInjection = new DependencyInjection().invoke();
        final AccountHandler accountHandler = dependencyInjection.getAccountHandler();
        final TransactionHandler transactionHandler = dependencyInjection.getTransactionHandler();

        JavalinValidation.register(UUID.class, UUID::fromString);

        configureJackson();

        final Javalin restApp = Javalin
                .create(config -> config.requestLogger((context, executionTimeMs) ->
                        LOG.info("{} ms\t {}\t {} {}",
                                executionTimeMs,
                                context.req.getMethod(),
                                context.req.getRequestURI(),
                                context.req.getParameterMap().toString().replaceAll("^.|.$", "")
                        ))
                )
                .events(event -> {
                    event.serverStarted(() -> LOG.info("server has started"));
                    event.serverStartFailed(() -> LOG.error("server start has failed"));
                })
                .start(DEFAULT_PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(restApp::stop));


        restApp.get("/accounts", ctx -> {
            ctx.use(MoneySerializer.class);
            accountHandler.getAll(ctx);
        });
        restApp.get("/accounts/:accountNumber", accountHandler::getAccount);
        restApp.get("/transactions", transactionHandler::getAllTransactions);
        restApp.get("/transactions/:id", transactionHandler::getTransaction);

        restApp.post("/accounts", accountHandler::create);
        restApp.post("/transactions", transactionHandler::transferAmount);

        restApp.exception(AccountNotFoundException.class, (exception, context) -> {
            context.result(exception.getMessage());
            context.status(HttpStatus.NOT_FOUND_404);
            LOG.error("error occurred", exception);
        });


        restApp.exception(AccountAlreadyExistsException.class, (exception, context) -> {
            context.result(exception.getMessage());
            context.status(HttpStatus.NOT_FOUND_404);
            LOG.error("error occurred", exception);
        });

        restApp.exception(TransactionNotFoundException.class, (exception, context) -> {
            context.result(exception.getMessage());
            context.status(HttpStatus.NOT_FOUND_404);
            LOG.error("error occurred", exception);
        });

        restApp.exception(InsufficientFundsException.class, (exception, context) -> {
            if (exception.getMessage() != null) {
                context.result(exception.getMessage());
            }
            context.status(HttpStatus.BAD_REQUEST_400);
            LOG.error("error occurred", exception);
        });

        restApp.exception(InvalidDepositException.class, (exception, context) -> {
            if (exception.getMessage() != null) {
                context.result(exception.getMessage());
            }
            context.status(HttpStatus.BAD_REQUEST_400);
            LOG.error("error occurred", exception);
        });


        restApp.exception(UnsupportedOperationException.class, (exception, context) -> {
            if (exception.getMessage() != null) {
                context.result(exception.getMessage());
            }
            context.status(HttpStatus.NOT_IMPLEMENTED_501);
            LOG.error("error occurred", exception);
        });

        restApp.exception(Exception.class, (exception, context) -> {
            context.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            LOG.error("error occurred", exception);
        });


        restApp.events(event -> {
            event.serverStopping(() -> LOG.info("Stopping server"));
            event.serverStopped(() -> LOG.info("Stopped server"));
        });
    }

    private static void configureJackson() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(Money.class, new MoneySerializer());
        module.addDeserializer(Money.class, new MoneyDeSerializer());
        objectMapper.registerModule(module);

        JavalinJackson.configure(objectMapper);
    }

    private static class DependencyInjection {
        private AccountHandler accountHandler;
        private TransactionHandler transactionHandler;

        AccountHandler getAccountHandler() {
            return accountHandler;
        }

        TransactionHandler getTransactionHandler() {
            return transactionHandler;
        }

        DependencyInjection invoke() {
            final AccountRepository accountRepository = new InMemoryAccountRepository();
            final TransactionRepository transactionRepository = new InMemoryTransactionRepository();
            final TransactionService transactionService = new TransactionServiceImpl(accountRepository, transactionRepository);
            final AccountingService accountingService = new AccountingServiceImpl(accountRepository);
            accountHandler = new AccountHandler(accountingService);
            transactionHandler = new TransactionHandler(transactionService);
            return this;
        }
    }
}
