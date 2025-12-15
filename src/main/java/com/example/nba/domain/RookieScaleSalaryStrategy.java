package com.example.nba.domain;

/** Example strategy for rookies: clamp to a small cap and add bonus based on rating. */
public final class RookieScaleSalaryStrategy implements SalaryStrategy {
    private final Money maxAnnual;

    public RookieScaleSalaryStrategy(Money maxAnnual) {
        this.maxAnnual = maxAnnual;
    }

    @Override
    public Money annualSalary(Player p, Contract c) {
        Money standard = new StandardSalaryStrategy().annualSalary(p, c);
        double bonus = Math.max(0, p.overallRating() - 70) * 10_000; // simple bonus
        Money withBonus = Money.of(standard.amount().doubleValue() + bonus);
        return maxAnnual.gte(withBonus) ? withBonus : maxAnnual;
    }
}
