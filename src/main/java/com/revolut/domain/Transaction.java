package com.revolut.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.revolut.serializers.MoneyDeSerializer;
import com.revolut.serializers.MoneySerializer;
import org.joda.money.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@JsonIgnoreProperties(value = {"isExecuting"})
public class Transaction {
    private final transient AtomicBoolean isExecuting;
    private final UUID id;
    private final String created;
    private final long fromAccountNumber;
    private final long toAccountNumber;
    @JsonSerialize(using = MoneySerializer.class)
    @JsonDeserialize(using = MoneyDeSerializer.class)
    private final Money money;

    private Transaction() {
        this(builder());
    }

    private Transaction(final Builder builder) {
        this(builder.accountFrom, builder.accountTo, builder.created, builder.amount);
    }

    private Transaction(final long accountFrom, final long accountTo, final String created, final Money money) {
        this.isExecuting = new AtomicBoolean(true);
        this.id = UUID.randomUUID();
        this.created = Instant.now().toString();
        this.fromAccountNumber = accountFrom;
        this.toAccountNumber = accountTo;
        this.money = money;
    }

    public static Builder builder() {
        return new Builder();
    }


    public long getFromAccountNumber() {
        return fromAccountNumber;
    }

    public long getToAccountNumber() {
        return toAccountNumber;
    }

    public Money getMoney() {
        return money;
    }


    public AtomicBoolean getIsExecuting() {
        return isExecuting;
    }

    public UUID getId() {
        return id;
    }

    public String getCreated() {
        return created;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return fromAccountNumber == that.fromAccountNumber &&
                toAccountNumber == that.toAccountNumber &&
                money == that.money &&
                Objects.equals(id, that.id) &&
                Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, created, fromAccountNumber, toAccountNumber, money);
    }


    @Override
    public String toString() {
        return "Transaction{" +
                "isExecuting=" + isExecuting +
                ", id=" + id +
                ", created=" + created +
                ", fromAccountNumber=" + fromAccountNumber +
                ", toAccountNumber=" + toAccountNumber +
                ", money=" + money +
                '}';
    }


    public static class Builder {
        private String created = Instant.now().toString();
        private long accountFrom;
        private long accountTo;
        private Money amount;

        private Builder() {
        }

        public Builder accountFrom(final long accountFrom) {
            this.accountFrom = accountFrom;
            return this;
        }

        public Builder accountTo(final long accountTo) {
            this.accountTo = accountTo;
            return this;
        }

        public Builder money(final Money amount) {
            this.amount = amount;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}
