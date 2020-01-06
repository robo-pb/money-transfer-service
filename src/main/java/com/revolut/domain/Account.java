package com.revolut.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.revolut.exceptions.InsufficientFundsException;
import com.revolut.exceptions.InvalidDepositException;
import com.revolut.serializers.MoneyDeSerializer;
import com.revolut.serializers.MoneySerializer;
import org.joda.money.Money;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@JsonIgnoreProperties(value = {
        "lock"
})
public class Account {
    private final transient Lock lock;
    private String owner;
    private long accountNumber;
    @JsonSerialize(using = MoneySerializer.class)
    @JsonDeserialize(using = MoneyDeSerializer.class)
    private Money money;

    private Account() {
        this(builder());
    }

    private Account(final Builder builder) {
        this(builder.owner, builder.accountNumber, builder.balance);
    }

    public Account(final String accountHolder, final long accountNumber, final Money money) {
        this.lock = new ReentrantLock();
        this.owner = accountHolder;
        this.accountNumber = accountNumber;
        this.money = money;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public String getOwner() {
        return owner;
    }

    public Money getMoney() {
        return money;
    }


    public static Builder builder() {
        return new Builder();
    }

    public Lock getLock() {
        return lock;
    }

    public Account deposit(final Money amount) throws InvalidDepositException {
        if (amount.isNegative()) {
            throw new InvalidDepositException(String.format("money %s cannot be deposited", amount));
        }

        return Account.builder()
                .money(this.getMoney().plus(amount))
                .accountNumber(this.getAccountNumber())
                .owner(this.getOwner())
                .build();
    }

    public Account withdraw(final Money amount) throws InsufficientFundsException {
        final Money afterWithDraw = this.getMoney().minus(amount);
        if (afterWithDraw.isNegative()) {
            throw new InsufficientFundsException(String.format("Account %s does not have sufficient funds", this.getAccountNumber()));
        }

        return Account.builder()
                .money(afterWithDraw)
                .accountNumber(this.getAccountNumber())
                .owner(this.getOwner())
                .build();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return accountNumber == account.accountNumber &&
                Objects.equals(owner, account.owner) &&
                Objects.equals(money, account.money);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, accountNumber, money);
    }

    @Override
    public String toString() {
        return "Account{" +
                "'owner='" + owner + '\'' +
                ", accountNumber=" + accountNumber +
                ", money=" + money +
                '}';
    }

    public static class Builder {
        private String owner;
        private long accountNumber;
        private Money balance;

        private Builder() {
        }

        public Builder accountNumber(final long number) {
            this.accountNumber = number;
            return this;
        }

        public Builder owner(final String owner) {
            this.owner = owner;
            return this;
        }

        public Builder money(final Money amount) {
            this.balance = amount;
            return this;
        }


        public Account build() {
            return new Account(this);
        }

    }

    public static Account copy(Account acc) {
        return Account.builder()
                .money(acc.getMoney())
                .accountNumber(acc.getAccountNumber())
                .owner(acc.getOwner())
                .build();
    }

}