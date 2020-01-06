package com.revolut.domain;

import com.revolut.exceptions.InsufficientFundsException;
import com.revolut.exceptions.InvalidDepositException;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AccountTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldThrowInsufficientFunds() throws InsufficientFundsException {
        final long accountNumber = 1;
        expectedException.expect(InsufficientFundsException.class);
        expectedException.expectMessage(String.format("Account %s does not have sufficient funds", accountNumber));
        final Account account = Account.builder().money(Money.of(CurrencyUnit.EUR, 2000)).owner("first").accountNumber(accountNumber).build();
        account.withdraw(Money.of(CurrencyUnit.EUR, 5000));
    }

    @Test
    public void shouldThrowInvalidDepositException() throws InvalidDepositException {
        final Money amount = Money.of(CurrencyUnit.EUR, -5000);

        expectedException.expectMessage(String.format("money %s cannot be deposited", amount));
        expectedException.expect(InvalidDepositException.class);
        final Account account = Account.builder().money(Money.of(CurrencyUnit.EUR, 2000)).owner("first").accountNumber(1).build();
        account.deposit(amount);

    }

}