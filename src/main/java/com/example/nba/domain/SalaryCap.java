package com.example.nba.domain;

import java.util.Objects;

/** Simple salary cap model. */
public final class SalaryCap {
    private final Money cap;
    private Money committed;

    public SalaryCap(Money cap) {
        this.cap = Objects.requireNonNull(cap);
        this.committed = Money.of(0);
    }

    public Money cap() { return cap; }
    public Money committed() { return committed; }
    public Money remaining() { return cap.minus(committed); }

    public void commit(Money amount) {
        Objects.requireNonNull(amount);
        if (!cap.gte(committed.plus(amount))) throw new IllegalStateException("cap exceeded");
        committed = committed.plus(amount);
    }

    public void uncommit(Money amount) {
        Objects.requireNonNull(amount);
        committed = Money.of(Math.max(0, committed.amount().doubleValue() - amount.amount().doubleValue()));
    }
}
