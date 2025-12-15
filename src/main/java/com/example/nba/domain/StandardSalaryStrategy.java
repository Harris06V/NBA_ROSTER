package com.example.nba.domain;

public final class StandardSalaryStrategy implements SalaryStrategy {
    @Override
    public Money annualSalary(Player p, Contract c) {
        // Straightline annual salary
        return c.totalValue().amount().doubleValue() <= 0 ? Money.of(0) : Money.of(
                c.totalValue().amount().doubleValue() / Math.max(1, c.years())
        );
    }
}
