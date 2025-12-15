package com.example.nba.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Tiny money value object for clarity (USD). */
public final class Money {
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(double dollars) {
        return new Money(BigDecimal.valueOf(dollars));
    }

    public BigDecimal amount() { return amount; }

    public Money plus(Money other) {
        Objects.requireNonNull(other);
        return new Money(this.amount.add(other.amount));
    }

    public Money minus(Money other) {
        Objects.requireNonNull(other);
        return new Money(this.amount.subtract(other.amount));
    }

    public boolean gte(Money other) {
        Objects.requireNonNull(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    @Override public String toString() { return "$" + amount; }

    @Override public boolean equals(Object o) {
        return (o instanceof Money m) && amount.compareTo(m.amount) == 0;
    }

    @Override public int hashCode() { return amount.stripTrailingZeros().hashCode(); }
}
