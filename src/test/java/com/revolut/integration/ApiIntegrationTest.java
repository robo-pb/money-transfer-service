package com.revolut.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revolut.App;
import com.revolut.domain.Account;
import com.revolut.domain.Transaction;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.http.HttpStatus;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

public class ApiIntegrationTest {
    private static final Money INITIAL_AMOUNT = Money.of(CurrencyUnit.EUR, 1000);
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void setUp() {
        configureHost();
        configurePort();
        configureBasePath();
        startServer();
    }

    private static void configureHost() {
        String baseHost = System.getProperty("server.host");
        if (baseHost == null) {
            baseHost = "http://localhost";
        }
        RestAssured.baseURI = baseHost;
    }

    private static void configurePort() {
        String port = System.getProperty("server.port");
        if (port == null) {
            RestAssured.port = Integer.parseInt("7000");
        } else {
            RestAssured.port = Integer.parseInt(port);
        }
    }

    private static void configureBasePath() {
        String basePath = System.getProperty("server.base");
        if (basePath == null) {
            basePath = "/";
        }
        RestAssured.basePath = basePath;
    }

    private static void startServer() {
        App.main(new String[]{});
    }


    @Test
    public void shouldCreateAndGetAccounts() throws JsonProcessingException {
        final String owner1 = "first";
        final String owner2 = "second";

        setUpTwoAccounts(owner1, owner2);

        final List<Account> existingAccounts = getAccountsByOwner(owner1, owner2);
        final Account first = existingAccounts.get(0);
        final Account second = existingAccounts.get(1);
        Assert.assertEquals(first.getMoney(), INITIAL_AMOUNT);
        Assert.assertEquals(second.getMoney(), INITIAL_AMOUNT);

        final Account firstByAccountNumber = getAccountByAccountNumber(first.getAccountNumber());
        final Account secondByAccountNumber = getAccountByAccountNumber(second.getAccountNumber());

        Assert.assertEquals(first, firstByAccountNumber);
        Assert.assertEquals(second, secondByAccountNumber);

    }


    @Test
    public void shouldDoTransfer() throws JsonProcessingException {
        final Pair<Account, Account> accountPair = ensureAccountSetup("nobody1", "somebody1");
        final Account fromAccount = accountPair.getLeft();
        final Account toAccount = accountPair.getRight();
        final Money amount = Money.of(CurrencyUnit.EUR, 1000);
        final String transferRequest = String.format("{\"fromAccountNumber\": %s, \"toAccountNumber\": %s, \"money\" : \"%s\"}",
                fromAccount.getAccountNumber(), toAccount.getAccountNumber(), amount.toString());

        final Account expectedFromAccount = Account.builder().owner(fromAccount.getOwner()).accountNumber(fromAccount.getAccountNumber()).money(fromAccount.getMoney().minus(amount)).build();
        final Account expectedToAccount = Account.builder().owner(toAccount.getOwner()).accountNumber(toAccount.getAccountNumber()).money(toAccount.getMoney().plus(amount)).build();

        final Transaction transactionResult = doTransaction(transferRequest, expectedFromAccount, expectedToAccount);

        verifyAccountBalances(transactionResult, expectedFromAccount, expectedToAccount);

        ensureTransactionsRetrievalWorks(expectedFromAccount, expectedToAccount, amount);

        ensureTransactionPersistedIsSameAsRequested(transactionResult.getId(), fromAccount.getAccountNumber(), toAccount.getAccountNumber(), amount);

    }


    @Test
    public void shouldReturn400_OnInValidDeposit() throws JsonProcessingException {
        final Pair<Account, Account> accountAccountPair = ensureAccountSetup("nobody3", "somebody3");
        final Account first = accountAccountPair.getLeft();
        final Account second = accountAccountPair.getRight();

        int invalidAmount = -100000;

        final String json = String.format("{\"fromAccountNumber\": %s, \"toAccountNumber\": %s, \"money\" : %s}",
                first.getAccountNumber(), second.getAccountNumber(), invalidAmount);

        given().body(json).post("/transactions").then().statusCode(HttpStatus.BAD_REQUEST_400);

        ensureAccountsNotModifiedAfterInvalidOp(first, second);

    }

    @Test
    public void shouldReturnBadRequestOnInsufficientFunds() throws JsonProcessingException {
        final Pair<Account, Account> accountAccountPair = ensureAccountSetup("nobody2", "somebody2");
        final Account first = accountAccountPair.getLeft();
        final Account second = accountAccountPair.getRight();

        int hugeAmount = 100000;

        final String json = String.format("{\"fromAccountNumber\": %s, \"toAccountNumber\": %s, \"money\" : %s}",
                first.getAccountNumber(), second.getAccountNumber(), hugeAmount);

        given().body(json).post("/transactions").then().statusCode(HttpStatus.BAD_REQUEST_400);

        ensureAccountsNotModifiedAfterInvalidOp(first, second);

    }

    @Test
    public void shouldReturn404_OnNonExistentAccountsForTransfer() {
        final String json = String.format("{\"fromAccountNumber\": %s, \"toAccountNumber\": %s, \"money\" : \"%s\"}",
                1000000, 999999999, INITIAL_AMOUNT.toString());

        given().body(json).post("/transactions").then().statusCode(HttpStatus.NOT_FOUND_404);

    }


    @Test
    public void shouldThrow400_forTransferBetweenAccountsWithDifferentCurrency() throws JsonProcessingException {
        final String euroJson = String.format("{\"owner\" : \"%s\", \"money\" : \"%s\"}", "euroOwner", INITIAL_AMOUNT.toString());
        Response euroResponse = given().body(euroJson).post("/accounts");
        euroResponse.then().statusCode(HttpStatus.CREATED_201);
        final Account euroAccount = mapper.readValue(euroResponse.asString(), Account.class);

        final String swissJson = String.format("{\"owner\" : \"%s\", \"money\" : \"%s\"}", "swissOwner", Money.of(CurrencyUnit.CHF, 1000));
        Response swissResponse = given().body(swissJson).post("/accounts");
        swissResponse.then().statusCode(HttpStatus.CREATED_201);
        final Account swissAccount = mapper.readValue(swissResponse.asString(), Account.class);

        final String transferJson =  String.format("{\"fromAccountNumber\": %s, \"toAccountNumber\": %s, \"money\" : \"%s\"}",
                euroAccount.getAccountNumber(), swissAccount.getAccountNumber(), INITIAL_AMOUNT.toString());

        given().body(transferJson).post("/transactions").then().statusCode(HttpStatus.NOT_IMPLEMENTED_501);

        ensureAccountsNotModifiedAfterInvalidOp(euroAccount, swissAccount);
    }


    @Test
    public void shouldThrow404_WhenNotExistingAccountIsRequested() {
        long accountNumber = 9999L;
        given().pathParam("accountNumber", accountNumber).when().get("/accounts/{accountNumber}").then().statusCode(HttpStatus.NOT_FOUND_404);
    }


    @Test
    public void shouldThrow400_ForInvalidOwner() {
        final String json = String.format("{\"owner\" : \"%s\", \"money\" : \"%s\"}", "", INITIAL_AMOUNT);
        given().body(json).post("/accounts").then().statusCode(HttpStatus.BAD_REQUEST_400);
    }

    @Test
    public void shouldThrow400_ForInvalidInitialAmount() {
        final String json = String.format("{\"owner\" : \"%s\", \"money\" : \"%s\"}", "owner1", Money.of(CurrencyUnit.EUR, -1000));
        given().body(json).post("/accounts").then().statusCode(HttpStatus.BAD_REQUEST_400);
    }

    @Test
    public void shouldThrow400_ForInvalidMoney(){
        final String json = String.format("{\"owner\" : \"%s\", \"money\" : \"%s\"}", "owner1", "EUR ");
        given().body(json).post("/accounts").then().statusCode(HttpStatus.BAD_REQUEST_400);
    }

    @Test
    public void shouldThrow404_findingNonExistentTransaction() {
        final String nonExistingUUID = UUID.fromString("9c566312-5d7d-4b45-9a74-e27cadf0d0be").toString();
        given().pathParam("id", nonExistingUUID).get("/transactions/{id}").then().statusCode(HttpStatus.NOT_FOUND_404);
    }


    private void ensureTransactionPersistedIsSameAsRequested(final UUID id, final long fromAccountNumber, final long toAccountNumber, final Money amount) throws JsonProcessingException {
        final Response transactionResponse = given().pathParam("id", id.toString()).get("/transactions/{id}");
        transactionResponse.then().statusCode(HttpStatus.OK_200);
        final Transaction transaction = mapper.readValue(transactionResponse.asString(), Transaction.class);

        Assert.assertEquals(transaction.getId(), id);
        Assert.assertEquals(transaction.getFromAccountNumber(), fromAccountNumber);
        Assert.assertEquals(transaction.getToAccountNumber(), toAccountNumber);
        Assert.assertEquals(transaction.getMoney(), amount);
    }


    private void ensureAccountsNotModifiedAfterInvalidOp(Account first, Account second) throws JsonProcessingException {
        final List<Account> afterTestAccounts = getAccountsByOwner(first.getOwner(), second.getOwner());
        final Account afterTestFirst = afterTestAccounts.get(0);
        final Account afterTestSecond = afterTestAccounts.get(1);

        Assert.assertEquals(afterTestFirst, first);
        Assert.assertEquals(afterTestSecond, second);
    }


    private Transaction doTransaction(String transferRequest, Account firstResult, Account secondResult) throws JsonProcessingException {
        Response post = given().body(transferRequest).post("/transactions");
        post.then().statusCode(HttpStatus.CREATED_201);
        final String postResponse = post.asString();

        final Transaction response = mapper.readValue(postResponse, Transaction.class);
        Assert.assertEquals(response.getFromAccountNumber(), firstResult.getAccountNumber());
        Assert.assertEquals(response.getToAccountNumber(), secondResult.getAccountNumber());
        Assert.assertNotNull(response.getId());

        return response;
    }

    private Pair<Account, Account> ensureAccountSetup(final String owner1, final String owner2) throws JsonProcessingException {
        setUpTwoAccounts(owner1, owner2);
        final List<Account> existingAccounts = getAccountsByOwner(owner1, owner2);
        final Account first = existingAccounts.get(0);
        final Account second = existingAccounts.get(1);
        Assert.assertEquals(first.getMoney(), INITIAL_AMOUNT);
        Assert.assertEquals(second.getMoney(), INITIAL_AMOUNT);

        return Pair.of(first, second);
    }

    private void setUpTwoAccounts(final String owner1, final String owner2) {
        verifyAccountCreation(owner1);
        verifyAccountCreation(owner2);
        final Response response = given().when().get("/accounts");
        response.then()
                .body("owner", hasItems(owner1, owner2))
                .body("money", hasItems(INITIAL_AMOUNT.toString(), INITIAL_AMOUNT.toString()))
                .body("accountNumber", hasItems(1, 2))
                .statusCode(HttpStatus.OK_200);
    }

    private List<Account> getAccountsByOwner(final String owner1, final String owner2) throws JsonProcessingException {
        final Response response = given().when().get("/accounts");
        final TypeReference<List<Account>> mapType = new TypeReference<List<Account>>() {
        };
        final List<Account> accounts = mapper.readValue(response.asString(), mapType);

        return accounts.stream().filter(acc -> acc.getOwner().equals(owner1) || acc.getOwner().equals(owner2)).collect(Collectors.toList());
    }

    private void ensureTransactionsRetrievalWorks(final Account first, final Account second, final Money amount) throws JsonProcessingException {
        final List<Transaction> transactions = getTransactions();

        final List<Transaction> filteredTransaction =
                transactions.stream()
                        .filter(t -> t.getMoney().equals(amount) &&
                                t.getFromAccountNumber() == first.getAccountNumber() &&
                                t.getToAccountNumber() == second.getAccountNumber()
                        ).collect(Collectors.toList());

        Assert.assertEquals(1, filteredTransaction.size());
        final Transaction transaction = filteredTransaction.get(0);
        Assert.assertNotNull(transaction.getId());
    }

    private List<Transaction> getTransactions() throws JsonProcessingException {
        final Response response = given().when().get("/transactions");
        final TypeReference<List<Transaction>> mapType = new TypeReference<List<Transaction>>() {
        };
        return mapper.readValue(response.asString(), mapType);
    }

    private void verifyAccountCreation(final String owner) {
        final String json = String.format("{\"owner\" : \"%s\", \"money\" : \"%s\"}", owner, INITIAL_AMOUNT.toString());
        given().body(json).post("/accounts").then().statusCode(HttpStatus.CREATED_201);
    }

    private void verifyAccountBalances(final Transaction transactionResult, final Account expectedFromAccount, final Account expectedToAccount) throws JsonProcessingException {
        final Account fromAccountResponse = getAccountByAccountNumber(transactionResult.getFromAccountNumber());
        Assert.assertEquals(fromAccountResponse, expectedFromAccount);

        final Account toAccountResponse = getAccountByAccountNumber(transactionResult.getToAccountNumber());
        Assert.assertEquals(toAccountResponse, expectedToAccount);
    }


    private Account getAccountByAccountNumber(final long accountNumber) throws JsonProcessingException {
        final Response fromAccountResponse = given().pathParam("accountNumber", accountNumber).when().get("/accounts/{accountNumber}");
        fromAccountResponse.then().statusCode(HttpStatus.OK_200);
        return mapper.readValue(fromAccountResponse.asString(), Account.class);
    }


}