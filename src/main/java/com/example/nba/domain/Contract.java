package com.example.nba.domain;

import java.time.LocalDate;
import java.util.Objects;

/** Immutable contract. Prefer builder for readability. */
public final class Contract {
    private final Money totalValue;
    private final int years;
    private final LocalDate startDate;

    private Contract(Builder b) {
        this.totalValue = b.totalValue;
        this.years = b.years;
        this.startDate = b.startDate;
    }

    public Money totalValue() { return totalValue; }
    public int years() { return years; }
    public LocalDate startDate() { return startDate; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Money totalValue = Money.of(0);
        private int years = 1;
        private LocalDate startDate = LocalDate.now();

        public Builder totalValue(Money v) { this.totalValue = Objects.requireNonNull(v); return this; }
        public Builder years(int y) { 
            if (y <= 0) throw new IllegalArgumentException("years must be > 0");
            this.years = y; 
            return this; 
        }
        public Builder startDate(LocalDate d) { this.startDate = Objects.requireNonNull(d); return this; }

        public Contract build() { return new Contract(this); }
    }
}
